<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import $ from "jquery";
import select2 from "select2";
import "select2/dist/css/select2.css";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { ArrowDownToLine, Gift, PackageCheck, Plus, RefreshCw, Search, WalletCards, X } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";

select2($);

const loading = ref(false);
const processing = ref(false);
const error = ref("");
const recompensas = ref([]);
const productos = ref([]);
const searchTerm = ref("");
const selected = ref(null);
const selectedProductoId = ref("");
const productoSelect = ref(null);
const form = ref({ montoDinero: 0, productos: [], observacion: "" });

const filteredRecompensas = computed(() => {
  const term = searchTerm.value.trim().toLowerCase();
  if (!term) return recompensas.value;
  return recompensas.value.filter((item) =>
    `${fullName(item.beneficiario)} ${fullName(item.referido?.persona)} ${item.planIngreso?.nombre || ""}`.toLowerCase().includes(term)
  );
});

const totals = computed(() =>
  filteredRecompensas.value.reduce(
    (acc, item) => ({
      efectivo: acc.efectivo + efectivoDisponible(item),
      productos: acc.productos + productosDisponible(item)
    }),
    { efectivo: 0, productos: 0 }
  )
);

const retiroProductosTotal = computed(() =>
  form.value.productos.reduce((sum, item) => sum + Number(item.precio || 0) * Number(item.cantidad || 0), 0)
);
const selectedProducto = computed(() =>
  productos.value.find((producto) => Number(producto.id) === Number(selectedProductoId.value)) || null
);
const efectivoSeleccionadoDisponible = computed(() => efectivoDisponible(selected.value));
const productosSeleccionadoDisponible = computed(() => productosDisponible(selected.value));
const retiroExcedeEfectivo = computed(() => Number(form.value.montoDinero || 0) > efectivoSeleccionadoDisponible.value);
const retiroExcedeProductos = computed(() => retiroProductosTotal.value > productosSeleccionadoDisponible.value);
const retiroTieneMonto = computed(() => Number(form.value.montoDinero || 0) > 0 || retiroProductosTotal.value > 0);
const retiroTieneMontosValidos = computed(() => Number(form.value.montoDinero || 0) >= 0 && retiroProductosTotal.value >= 0);
const canRegistrarRetiro = computed(() =>
  Boolean(selected.value)
    && retiroTieneMonto.value
    && retiroTieneMontosValidos.value
    && !retiroExcedeEfectivo.value
    && !retiroExcedeProductos.value
    && !processing.value
);

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function fullName(persona) {
  return `${persona?.nombres || ""} ${persona?.apellidos || ""}`.trim() || "Persona";
}

function efectivoDisponible(recompensa) {
  return Math.max(0, Number(recompensa?.montoEfectivo || 0) - Number(recompensa?.montoEfectivoRetirado || 0));
}

function productosDisponible(recompensa) {
  return Math.max(0, Number(recompensa?.valorProductos || 0) - Number(recompensa?.valorProductosRetirado || 0));
}

async function loadAll() {
  loading.value = true;
  error.value = "";
  try {
    const [recompensasData, productosData] = await Promise.all([
      apiRequest("/api/recompensas/nivel-1"),
      apiRequest("/api/productos")
    ]);
    recompensas.value = Array.isArray(recompensasData) ? recompensasData : [];
    productos.value = Array.isArray(productosData) ? productosData : [];
  } catch (exception) {
    error.value = exception.message || "No se pudieron cargar los retiros de nivel 1.";
  } finally {
    loading.value = false;
  }
}

async function initProductoSelect2() {
  if (!selected.value) return;
  await nextTick();
  if (!productoSelect.value) return;

  destroyProductoSelect2();
  const element = $(productoSelect.value);
  element
    .select2({
      width: "100%",
      placeholder: "Busca y selecciona producto",
      allowClear: true,
      dropdownParent: $(".retiro-modal"),
      language: {
        noResults: () => "Sin resultados",
        searching: () => "Buscando..."
      }
    })
    .val(selectedProductoId.value || null)
    .trigger("change.select2");

  element.on("change.retiros-nivel-1", () => {
    selectedProductoId.value = element.val() || "";
  });
}

function destroyProductoSelect2() {
  if (!productoSelect.value) return;
  const element = $(productoSelect.value);
  element.off("change.retiros-nivel-1");
  if (element.hasClass("select2-hidden-accessible")) {
    element.select2("destroy");
  }
}

async function openModal(recompensa) {
  selected.value = recompensa;
  form.value = { montoDinero: efectivoDisponible(recompensa), productos: [], observacion: "" };
  selectedProductoId.value = "";
  await initProductoSelect2();
}

function closeModal() {
  if (processing.value) return;
  destroyProductoSelect2();
  selected.value = null;
  selectedProductoId.value = "";
  form.value = { montoDinero: 0, productos: [], observacion: "" };
}

function addProducto() {
  const producto = selectedProducto.value;
  if (!producto) return;
  const siguienteTotal = retiroProductosTotal.value + Number(producto.precio || 0);
  if (siguienteTotal > productosSeleccionadoDisponible.value) {
    error.value = "El producto seleccionado supera el credito disponible de la recompensa.";
    return;
  }
  error.value = "";
  const existente = form.value.productos.find((item) => Number(item.productoId) === Number(producto.id));
  if (existente) {
    existente.cantidad += 1;
    selectedProductoId.value = "";
    initProductoSelect2();
    return;
  }
  form.value.productos.push({
    productoId: producto.id,
    nombre: producto.nombre,
    sku: producto.sku,
    precio: Number(producto.precio || 0),
    cantidad: 1
  });
  selectedProductoId.value = "";
  initProductoSelect2();
}

function removeProducto(item) {
  form.value.productos = form.value.productos.filter((current) => Number(current.productoId) !== Number(item.productoId));
}

function updateCantidad(item, value) {
  item.cantidad = Math.max(1, Number(value || 1));
}

async function registrarRetiro() {
  if (!selected.value || processing.value) return;
  if (!canRegistrarRetiro.value) {
    error.value = "Revisa los montos: no pueden superar el efectivo o credito disponible.";
    return;
  }
  processing.value = true;
  error.value = "";
  try {
    await apiRequest(`/api/recompensas/nivel-1/${selected.value.id}/retiro`, {
      method: "POST",
      body: JSON.stringify({
        montoDinero: Number(form.value.montoDinero || 0),
        montoProductos: retiroProductosTotal.value,
        productos: form.value.productos.map((item) => ({
          productoId: Number(item.productoId),
          cantidad: Number(item.cantidad || 1)
        })),
        observacion: form.value.observacion || null
      })
    });
    await Swal.fire({
      title: "Retiro procesado",
      text: "La recompensa de nivel 1 fue actualizada.",
      icon: "success",
      confirmButtonColor: "#F28705"
    });
    closeModal();
    await loadAll();
  } catch (exception) {
    error.value = exception.message || "No se pudo registrar el retiro.";
  } finally {
    processing.value = false;
  }
}

watch(selectedProductoId, (value) => {
  if (!productoSelect.value) return;
  const element = $(productoSelect.value);
  if (element.hasClass("select2-hidden-accessible") && element.val() !== value) {
    element.val(value || null).trigger("change.select2");
  }
});

watch(productos, () => {
  initProductoSelect2();
});

onMounted(loadAll);

onBeforeUnmount(() => {
  destroyProductoSelect2();
});
</script>

<template>
  <div class="vy level-one-withdrawals-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Recompensas inmediatas</div>
          <h1>Retiros nivel 1</h1>
          <p>Retiros individuales de efectivo inmediato y credito en producto del patrocinador directo.</p>
        </div>
        <button class="refresh-action" type="button" :disabled="loading" @click="loadAll">
          <RefreshCw :size="16" /> Actualizar
        </button>
      </header>

      <div v-if="error" class="error-box">{{ error }}</div>

      <section class="summary-grid">
        <article class="balance-card">
          <div class="balance-icon"><WalletCards :size="26" /></div>
          <span>Efectivo inmediato</span>
          <strong>Bs. {{ money(totals.efectivo) }}</strong>
          <p>{{ filteredRecompensas.length }} recompensas pendientes.</p>
        </article>
        <article class="metric-card">
          <span class="metric-icon"><PackageCheck :size="20" /></span>
          <div>
            <small>Credito en producto</small>
            <strong>Bs. {{ money(totals.productos) }}</strong>
          </div>
        </article>
      </section>

      <section class="vy-card table-card">
        <header class="section-header">
          <div>
            <h2>Beneficiarios nivel 1</h2>
            <p>Selecciona una recompensa para procesar su retiro inmediato.</p>
          </div>
          <label class="search-box">
            <Search :size="16" />
            <input v-model.trim="searchTerm" type="search" placeholder="Buscar beneficiario o referido" />
          </label>
        </header>

        <div v-if="loading" class="loading-box">Cargando recompensas...</div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Beneficiario</th>
                <th>Nuevo referido</th>
                <th>Plan</th>
                <th>Efectivo</th>
                <th>Producto</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="recompensa in filteredRecompensas" :key="recompensa.id">
                <td>
                  <strong>{{ fullName(recompensa.beneficiario) }}</strong>
                  <small>{{ recompensa.beneficiario?.documento || "Sin documento" }}</small>
                </td>
                <td>{{ fullName(recompensa.referido?.persona) }}</td>
                <td>{{ recompensa.planIngreso?.nombre || "Plan" }}</td>
                <td>Bs. {{ money(efectivoDisponible(recompensa)) }}</td>
                <td>Bs. {{ money(productosDisponible(recompensa)) }}</td>
                <td class="actions-cell">
                  <button class="row-withdraw-button" type="button" @click="openModal(recompensa)">
                    <Gift :size="15" /> Retirar
                  </button>
                </td>
              </tr>
              <tr v-if="!filteredRecompensas.length && !loading">
                <td colspan="6">No hay recompensas de nivel 1 pendientes.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>

    <Teleport to="body">
      <div v-if="selected" class="retiro-backdrop">
        <article class="retiro-modal">
          <header>
            <div>
              <span class="vy-eyebrow">Nivel 1</span>
              <h2>Retiro inmediato</h2>
              <p>{{ fullName(selected.beneficiario) }}</p>
            </div>
            <button type="button" aria-label="Cerrar" @click="closeModal">
              <X :size="18" />
            </button>
          </header>

          <section class="retiro-body">
            <div class="modal-context">
              <div>
                <small>Beneficiario</small>
                <strong>{{ fullName(selected.beneficiario) }}</strong>
              </div>
              <div>
                <small>Nuevo referido</small>
                <strong>{{ fullName(selected.referido?.persona) }}</strong>
              </div>
              <div>
                <small>Plan</small>
                <strong>{{ selected.planIngreso?.nombre || "Plan" }}</strong>
              </div>
            </div>

            <div class="wallet-values">
              <div>
                <small>Efectivo disponible</small>
                <strong>Bs. {{ money(efectivoSeleccionadoDisponible) }}</strong>
              </div>
              <div>
                <small>Credito producto</small>
                <strong>Bs. {{ money(productosSeleccionadoDisponible) }}</strong>
              </div>
            </div>

            <div class="withdraw-grid">
              <label class="field" :class="{ invalid: retiroExcedeEfectivo }">
                <span>Retirar efectivo</span>
                <input v-model.number="form.montoDinero" type="number" min="0" step="0.01" />
                <small v-if="retiroExcedeEfectivo">Supera el efectivo disponible.</small>
              </label>
              <div class="field" :class="{ invalid: retiroExcedeProductos }">
                <span>Total productos</span>
                <div class="readonly-total">Bs. {{ money(retiroProductosTotal) }}</div>
                <small v-if="retiroExcedeProductos">Supera el credito disponible.</small>
              </div>
            </div>

            <section class="product-select-panel">
              <label class="field">
                <span>Producto a entregar</span>
                <div class="select-action">
                  <select ref="productoSelect" v-model="selectedProductoId">
                    <option value="">Selecciona producto</option>
                    <option v-for="producto in productos" :key="producto.id" :value="producto.id">
                      {{ producto.nombre }} - {{ producto.sku || "Sin SKU" }} - Bs. {{ money(producto.precio) }}
                    </option>
                  </select>
                  <button type="button" :disabled="!selectedProducto" @click="addProducto">
                    <Plus :size="16" /> Agregar
                  </button>
                </div>
              </label>
              <div class="credit-meter" :class="{ danger: retiroExcedeProductos }">
                <span>Credito usado</span>
                <strong>Bs. {{ money(retiroProductosTotal) }} / Bs. {{ money(productosSeleccionadoDisponible) }}</strong>
              </div>
            </section>

            <div v-if="form.productos.length" class="withdraw-products-list">
              <article v-for="item in form.productos" :key="item.productoId">
                <div>
                  <strong>{{ item.nombre }}</strong>
                  <small>{{ item.sku || "Sin SKU" }} - Bs. {{ money(item.precio) }}</small>
                </div>
                <input :value="item.cantidad" type="number" min="1" @input="updateCantidad(item, $event.target.value)" />
                <span>Bs. {{ money(Number(item.precio || 0) * Number(item.cantidad || 0)) }}</span>
                <button type="button" @click="removeProducto(item)">Quitar</button>
              </article>
            </div>

            <label class="field">
              <span>Observacion</span>
              <textarea v-model.trim="form.observacion" rows="3" placeholder="Detalle interno del retiro"></textarea>
            </label>
          </section>

          <footer>
            <button class="vy-btn vy-btn-ghost" type="button" :disabled="processing" @click="closeModal">Cancelar</button>
            <button class="withdraw-submit" type="button" :disabled="!canRegistrarRetiro" @click="registrarRetiro">
              <ArrowDownToLine :size="18" />
              {{ processing ? "Procesando..." : "Procesar retiro" }}
            </button>
          </footer>
        </article>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.level-one-withdrawals-view { min-height: 100vh; background: var(--vy-bg); color: var(--vy-ink); }
.workspace { width: min(1180px, calc(100% - 32px)); margin: 0 auto; padding: 28px 0 40px; }
.page-header, .section-header, .retiro-modal > header, .retiro-modal > footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-header { margin-bottom: 18px; }
.page-header h1 { margin-top: 6px; font-size: 34px; font-weight: 900; }
.page-header p, .section-header p { margin-top: 5px; color: var(--vy-ink-3); font-weight: 700; }
.refresh-action { min-height: 42px; padding: 0 16px; border: 1px solid rgba(242, 135, 5, 0.32); border-radius: 8px; background: #fff7e8; color: #1f1a14; display: inline-flex; align-items: center; justify-content: center; gap: 8px; font-size: 14px; font-weight: 950; box-shadow: 0 10px 24px rgba(242, 135, 5, 0.14); }
.refresh-action:hover:not(:disabled) { background: #f28705; color: #1f1a14; transform: translateY(-1px); }
.refresh-action:disabled { cursor: not-allowed; opacity: 0.55; box-shadow: none; }
.summary-grid { display: grid; grid-template-columns: minmax(260px, 1.4fr) minmax(220px, 0.8fr); gap: 14px; margin-bottom: 16px; }
.balance-card, .metric-card { padding: 18px; border: 1px solid var(--vy-line); border-radius: 8px; background: var(--vy-surface); box-shadow: var(--vy-shadow-sm); }
.balance-card span, .metric-card small { color: var(--vy-ink-3); font-size: 12px; font-weight: 900; text-transform: uppercase; }
.balance-card strong, .metric-card strong { display: block; margin-top: 6px; font-size: 24px; font-weight: 900; }
.balance-card p { margin-top: 5px; color: var(--vy-ink-3); font-size: 13px; font-weight: 700; }
.table-card { padding: 18px; }
.search-box { display: flex; align-items: center; gap: 8px; min-width: 260px; padding: 10px 12px; border: 1px solid var(--vy-line); border-radius: 8px; background: var(--vy-surface-2); }
.search-box input { width: 100%; border: 0; outline: 0; background: transparent; color: var(--vy-ink); font-weight: 700; }
.table-wrap { overflow: auto; margin-top: 14px; }
table { width: 100%; border-collapse: collapse; min-width: 760px; }
th, td { padding: 12px; border-bottom: 1px solid var(--vy-line-2); text-align: left; vertical-align: top; }
th { color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
td strong, td small { display: block; }
td small { margin-top: 3px; color: var(--vy-ink-3); font-size: 12px; font-weight: 700; }
.actions-cell { text-align: right; }
.row-withdraw-button { min-height: 36px; padding: 0 12px; border: 1px solid rgba(31, 26, 20, 0.12); border-radius: 8px; background: #1f1a14; color: #fff; display: inline-flex; align-items: center; justify-content: center; gap: 7px; font-size: 13px; font-weight: 950; white-space: nowrap; box-shadow: 0 8px 18px rgba(31, 26, 20, 0.16); }
.row-withdraw-button:hover { background: #f28705; color: #1f1a14; transform: translateY(-1px); }
.error-box, .loading-box { margin-bottom: 14px; padding: 13px 15px; border-radius: 8px; font-size: 13px; font-weight: 800; }
.error-box { color: #8a2c1c; background: #fff1ec; border: 1px solid #ffd0c2; }
.loading-box { color: var(--vy-ink-2); background: var(--vy-surface-2); }
.retiro-backdrop { position: fixed; inset: 0; z-index: 80; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(31, 26, 20, 0.42); backdrop-filter: blur(8px); }
.retiro-modal { width: min(820px, 100%); max-height: calc(100vh - 40px); padding: 20px; border-radius: 8px; border: 1px solid var(--vy-line); background: var(--vy-surface); box-shadow: var(--vy-shadow-lg); overflow: hidden; display: flex; flex-direction: column; }
.retiro-modal header { padding-bottom: 14px; border-bottom: 1px solid var(--vy-line-2); }
.retiro-modal h2 { margin-top: 4px; font-size: 22px; font-weight: 900; }
.retiro-modal header button { width: 38px; height: 38px; border-radius: 8px; background: var(--vy-surface-2); color: var(--vy-ink-2); display: inline-flex; align-items: center; justify-content: center; }
.retiro-body { padding: 16px 2px; overflow: auto; }
.modal-context { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; margin-bottom: 12px; }
.modal-context > div { padding: 11px 12px; border: 1px solid var(--vy-line); border-radius: 8px; background: #fff; }
.modal-context small { display: block; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.modal-context strong { display: block; margin-top: 4px; font-size: 14px; font-weight: 900; }
.wallet-values, .withdraw-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.wallet-values > div, .readonly-total { padding: 12px; border: 1px solid var(--vy-line); border-radius: 8px; background: var(--vy-surface-2); }
.wallet-values small, .field span { display: block; color: var(--vy-ink-3); font-size: 12px; font-weight: 900; text-transform: uppercase; }
.wallet-values strong { display: block; margin-top: 5px; font-size: 22px; font-weight: 900; }
.field input, .field textarea, .field select { width: 100%; margin-top: 7px; padding: 11px 12px; border: 1px solid var(--vy-line); border-radius: 8px; background: #fff; color: var(--vy-ink); font-weight: 800; }
.field.invalid input, .field.invalid .readonly-total { border-color: #c4452a; background: #fff4ef; }
.field > small { display: block; margin-top: 6px; color: #c4452a; font-size: 12px; font-weight: 900; }
.product-select-panel { display: grid; grid-template-columns: 1fr 210px; gap: 12px; align-items: end; margin: 12px 0 14px; padding: 12px; border: 1px solid var(--vy-line); border-radius: 8px; background: var(--vy-surface-2); }
.select-action { display: grid; grid-template-columns: 1fr auto; gap: 8px; align-items: end; }
.select-action button { height: 42px; padding: 0 14px; border-radius: 8px; background: var(--vy-ink); color: #fff; display: inline-flex; align-items: center; gap: 7px; font-size: 13px; font-weight: 900; }
.select-action button:disabled { cursor: not-allowed; opacity: 0.45; }
:deep(.select-action .select2-container) { margin-top: 7px; }
:deep(.select-action .select2-container--default .select2-selection--single) { height: 42px; border: 1px solid var(--vy-line); border-radius: 8px; background: #fff; display: flex; align-items: center; }
:deep(.select-action .select2-container--default .select2-selection--single .select2-selection__rendered) { width: 100%; padding-left: 12px; padding-right: 34px; color: var(--vy-ink); font-size: 14px; font-weight: 850; line-height: 42px; }
:deep(.select-action .select2-container--default .select2-selection--single .select2-selection__placeholder) { color: var(--vy-ink-3); }
:deep(.select-action .select2-container--default .select2-selection--single .select2-selection__arrow) { height: 40px; right: 8px; }
:deep(.select2-dropdown) { border: 1px solid var(--vy-line); border-radius: 8px; overflow: hidden; box-shadow: var(--vy-shadow-lg); }
:deep(.select2-search--dropdown) { padding: 8px; background: var(--vy-surface-2); }
:deep(.select2-container--default .select2-search--dropdown .select2-search__field) { min-height: 36px; border: 1px solid var(--vy-line); border-radius: 8px; padding: 7px 9px; outline: 0; }
:deep(.select2-results__option) { padding: 9px 12px; font-size: 13px; font-weight: 800; }
:deep(.select2-container--default .select2-results__option--highlighted.select2-results__option--selectable) { background: #f28705; color: #1f1a14; }
.credit-meter { padding: 11px 12px; border: 1px solid var(--vy-line); border-radius: 8px; background: #fff; }
.credit-meter span { display: block; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.credit-meter strong { display: block; margin-top: 5px; color: var(--vy-ink); font-size: 14px; font-weight: 900; }
.credit-meter.danger { border-color: #c4452a; background: #fff4ef; }
.credit-meter.danger strong { color: #c4452a; }
.withdraw-products-list { display: grid; gap: 8px; margin-bottom: 14px; }
.withdraw-products-list article { display: grid; grid-template-columns: 1fr 76px 110px auto; align-items: center; gap: 10px; padding: 10px; border: 1px solid var(--vy-line); border-radius: 8px; }
.withdraw-products-list input { width: 76px; padding: 8px; border: 1px solid var(--vy-line); border-radius: 8px; }
.retiro-modal footer { justify-content: flex-end; padding-top: 14px; border-top: 1px solid var(--vy-line-2); }
.withdraw-submit { min-height: 42px; padding: 0 18px; border-radius: 8px; background: #1f1a14; color: #fff; display: inline-flex; align-items: center; justify-content: center; gap: 8px; font-size: 14px; font-weight: 950; box-shadow: 0 12px 28px rgba(31, 26, 20, 0.18); }
.withdraw-submit:hover:not(:disabled) { background: #f28705; color: #1f1a14; transform: translateY(-1px); }
.withdraw-submit:disabled { cursor: not-allowed; opacity: 0.45; box-shadow: none; }
@media (max-width: 760px) {
  .summary-grid, .modal-context, .wallet-values, .withdraw-grid, .product-select-panel, .select-action { grid-template-columns: 1fr; }
  .page-header, .section-header, .retiro-modal > header, .retiro-modal > footer { align-items: stretch; flex-direction: column; }
  .search-box, .retiro-modal footer .vy-btn, .withdraw-submit { width: 100%; }
}
</style>
