<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import $ from "jquery";
import select2 from "select2";
import "select2/dist/css/select2.css";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { ArrowDownToLine, Building2, CalendarClock, RefreshCw, TrendingDown, TrendingUp, WalletCards, X } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";

select2($);

const loading = ref(false);
const processing = ref(false);
const error = ref("");
const cartera = ref(null);
const movimientos = ref([]);
const resumenPeriodo = ref(null);
const periodosCaja = ref([]);
const selectedPeriodoId = ref("");
const periodoSelect = ref(null);
const personas = ref([]);
const productos = ref([]);
const retiroModalOpen = ref(false);
const selectedPersonaId = ref("");
const personaSelect = ref(null);
const selectedProductoId = ref("");
const productoSelect = ref(null);
const selectedWallet = ref(null);
const walletLoading = ref(false);
const page = ref(1);
const pageSize = ref(10);
const retiroForm = ref({
  montoDinero: 0,
  productos: [],
  observacion: ""
});

const selectedPeriodo = computed(() =>
  periodosCaja.value.find((periodo) => Number(periodo.id) === Number(selectedPeriodoId.value))
);

const totalPages = computed(() => Math.max(1, Math.ceil(movimientos.value.length / Number(pageSize.value || 10))));
const paginatedMovimientos = computed(() => {
  const size = Number(pageSize.value || 10);
  const start = (page.value - 1) * size;
  return movimientos.value.slice(start, start + size);
});

const selectedPersona = computed(() =>
  personas.value.find((persona) => Number(persona.id) === Number(selectedPersonaId.value))
);

const billeteraSeleccionada = computed(() => selectedWallet.value?.billetera || {});
const efectivoDisponibleRetiro = computed(() =>
  Number(billeteraSeleccionada.value.saldoDinero || 0) + Number(selectedWallet.value?.efectivoRecompensasDisponible || 0)
);
const productosDisponiblesRetiro = computed(() => Number(billeteraSeleccionada.value.saldoProductos || 0));
const selectedProducto = computed(() =>
  productos.value.find((producto) => Number(producto.id) === Number(selectedProductoId.value))
);
const retiroProductosTotal = computed(() =>
  retiroForm.value.productos.reduce((sum, item) => sum + Number(item.precio || 0) * Number(item.cantidad || 0), 0)
);

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function formatDate(value) {
  if (!value) return "Sin fecha";
  return new Date(value).toLocaleString("es-BO", {
    dateStyle: "medium",
    timeStyle: "short"
  });
}

function destroyPeriodoSelect2() {
  if (!periodoSelect.value) return;
  const element = $(periodoSelect.value);
  if (element.hasClass("select2-hidden-accessible")) {
    element.off("change.caja-periodo");
    element.select2("destroy");
  }
}

function destroyPersonaSelect2() {
  if (!personaSelect.value) return;
  const element = $(personaSelect.value);
  if (element.hasClass("select2-hidden-accessible")) {
    element.off("change.caja-persona");
    element.select2("destroy");
  }
}

function destroyProductoSelect2() {
  if (!productoSelect.value) return;
  const element = $(productoSelect.value);
  if (element.hasClass("select2-hidden-accessible")) {
    element.off("change.caja-producto");
    element.select2("destroy");
  }
}

async function initPeriodoSelect2() {
  await nextTick();
  if (!periodoSelect.value) return;

  destroyPeriodoSelect2();
  const element = $(periodoSelect.value);
  element
    .select2({
      width: "100%",
      placeholder: "Selecciona un mes",
      allowClear: false,
      dropdownParent: $(".company-wallet-view"),
      language: {
        noResults: () => "Sin resultados",
        searching: () => "Buscando..."
      }
    })
    .val(selectedPeriodoId.value || null)
    .trigger("change.select2");

  element.on("change.caja-periodo", async () => {
    selectedPeriodoId.value = element.val() || "";
    await loadCompanyWallet();
  });
}

async function initPersonaSelect2() {
  if (!retiroModalOpen.value) return;

  await nextTick();
  if (!personaSelect.value) return;

  destroyPersonaSelect2();
  const element = $(personaSelect.value);
  element
    .select2({
      width: "100%",
      placeholder: "Selecciona una persona",
      allowClear: true,
      dropdownParent: $(".retiro-modal"),
      language: {
        noResults: () => "Sin resultados",
        searching: () => "Buscando..."
      }
    })
    .val(selectedPersonaId.value || null)
    .trigger("change.select2");

  element.on("change.caja-persona", async () => {
    selectedPersonaId.value = element.val() || "";
    await loadSelectedWallet();
  });
}

async function initProductoSelect2() {
  if (!retiroModalOpen.value || !selectedWallet.value) return;

  await nextTick();
  if (!productoSelect.value) return;

  destroyProductoSelect2();
  const element = $(productoSelect.value);
  element
    .select2({
      width: "100%",
      placeholder: "Selecciona producto",
      allowClear: true,
      dropdownParent: $(".retiro-modal"),
      language: {
        noResults: () => "Sin resultados",
        searching: () => "Buscando..."
      }
    })
    .val(selectedProductoId.value || null)
    .trigger("change.select2");

  element.on("change.caja-producto", () => {
    selectedProductoId.value = element.val() || "";
  });
}

async function loadPeriodoOptions() {
  const [activePeriodo, gestionesData] = await Promise.all([
    apiRequest("/api/gestiones/periodos/activo"),
    apiRequest("/api/gestiones")
  ]);
  const periodosPorGestion = await Promise.all(
    (Array.isArray(gestionesData) ? gestionesData : []).map(async (gestion) => {
      const periodos = await apiRequest(`/api/gestiones/${gestion.id}/periodos`);
      return periodos.map((periodo) => ({ ...periodo, gestion: periodo.gestion || gestion }));
    })
  );

  periodosCaja.value = periodosPorGestion
    .flat()
    .sort((left, right) => {
      const leftYear = Number(left.gestion?.anio || 0);
      const rightYear = Number(right.gestion?.anio || 0);
      if (leftYear !== rightYear) return rightYear - leftYear;
      return Number(right.mes || 0) - Number(left.mes || 0);
    });

  if (!selectedPeriodoId.value) {
    selectedPeriodoId.value = String(activePeriodo?.id || periodosCaja.value[0]?.id || "");
  }
}

async function loadCompanyWallet() {
  loading.value = true;
  error.value = "";

  try {
    if (!periodosCaja.value.length) {
      await loadPeriodoOptions();
      await initPeriodoSelect2();
    }
    const query = selectedPeriodoId.value ? `?periodoId=${selectedPeriodoId.value}` : "";
    const [carteraData, movimientosData, resumenData] = await Promise.all([
      apiRequest("/api/cartera-empresa"),
      apiRequest(`/api/cartera-empresa/movimientos${query}`),
      apiRequest(`/api/cartera-empresa/resumen-periodo${query}`)
    ]);
    cartera.value = carteraData;
    movimientos.value = Array.isArray(movimientosData) ? movimientosData : [];
    resumenPeriodo.value = resumenData;
    page.value = Math.min(page.value, totalPages.value);
  } catch (exception) {
    error.value = exception.message || "No se pudo cargar la cartera de la empresa.";
  } finally {
    loading.value = false;
  }
}

async function loadPersonas() {
  try {
    const [personasData, productosData] = await Promise.all([
      apiRequest("/api/personas"),
      apiRequest("/api/productos")
    ]);
    personas.value = personasData;
    productos.value = productosData;
  } catch {
    personas.value = [];
    productos.value = [];
  }
}

async function loadSelectedWallet() {
  selectedWallet.value = null;
  retiroForm.value = { montoDinero: 0, productos: [], observacion: "" };
  if (!selectedPersonaId.value) return;

  walletLoading.value = true;
  try {
    selectedWallet.value = await apiRequest(`/api/billeteras/persona/${selectedPersonaId.value}`);
  } catch (exception) {
    error.value = exception.message || "No se pudo cargar la billetera de la persona.";
  } finally {
    walletLoading.value = false;
  }
}

async function openRetiroModal() {
  retiroModalOpen.value = true;
  if (!personas.value.length) {
    await loadPersonas();
  }
  await initPersonaSelect2();
  await initProductoSelect2();
}

function closeRetiroModal() {
  if (processing.value) return;
  destroyPersonaSelect2();
  destroyProductoSelect2();
  retiroModalOpen.value = false;
  selectedPersonaId.value = "";
  selectedProductoId.value = "";
  selectedWallet.value = null;
  retiroForm.value = { montoDinero: 0, productos: [], observacion: "" };
}

function setMaxRetiro() {
  retiroForm.value.montoDinero = efectivoDisponibleRetiro.value;
}

function addRetiroProducto() {
  if (!selectedProducto.value) return;
  const existente = retiroForm.value.productos.find((item) => Number(item.productoId) === Number(selectedProducto.value.id));
  if (existente) {
    existente.cantidad += 1;
  } else {
    retiroForm.value.productos.push({
      productoId: selectedProducto.value.id,
      nombre: selectedProducto.value.nombre,
      sku: selectedProducto.value.sku,
      precio: Number(selectedProducto.value.precio || 0),
      cantidad: 1
    });
  }
  selectedProductoId.value = "";
  initProductoSelect2();
}

function removeRetiroProducto(item) {
  retiroForm.value.productos = retiroForm.value.productos.filter((current) => Number(current.productoId) !== Number(item.productoId));
}

function updateRetiroProductoCantidad(item, value) {
  item.cantidad = Math.max(1, Number(value || 1));
}

async function registrarRetiro() {
  if (!selectedPersonaId.value || processing.value) return;

  processing.value = true;
  error.value = "";
  try {
    await apiRequest(`/api/billeteras/persona/${selectedPersonaId.value}/retiros`, {
      method: "POST",
      body: JSON.stringify({
        montoDinero: Number(retiroForm.value.montoDinero || 0),
        montoProductos: retiroProductosTotal.value,
        productos: retiroForm.value.productos.map((item) => ({
          productoId: Number(item.productoId),
          cantidad: Number(item.cantidad || 1)
        })),
        observacion: retiroForm.value.observacion || null
      })
    });
    await Swal.fire({
      title: "Retiro procesado",
      text: "Los saldos de la persona y la caja empresa fueron actualizados.",
      icon: "success",
      confirmButtonColor: "#F28705"
    });
    closeRetiroModal();
    await loadCompanyWallet();
  } catch (exception) {
    error.value = exception.message || "No se pudo registrar el retiro.";
  } finally {
    processing.value = false;
  }
}

async function cerrarMes() {
  const result = await Swal.fire({
    title: "Cerrar mes",
    text: "Se enviara a cierre el efectivo, PV, QP y productos pendientes de las billeteras. Esta accion prepara la planilla mensual.",
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Cerrar mes",
    cancelButtonText: "Cancelar",
    confirmButtonColor: "#F28705",
    cancelButtonColor: "#1F1A14"
  });
  if (!result.isConfirmed || processing.value) return;

  processing.value = true;
  error.value = "";
  try {
    const total = await apiRequest("/api/billeteras/cierres-mensuales", { method: "POST" });
    await Swal.fire({
      title: "Cierre realizado",
      text: `Se generaron ${total} cierres mensuales.`,
      icon: "success",
      confirmButtonColor: "#F28705"
    });
    await loadCompanyWallet();
  } catch (exception) {
    error.value = exception.message || "No se pudo cerrar el mes.";
  } finally {
    processing.value = false;
  }
}

function previousPage() {
  page.value = Math.max(1, page.value - 1);
}

function nextPage() {
  page.value = Math.min(totalPages.value, page.value + 1);
}

watch(periodosCaja, () => {
  initPeriodoSelect2();
});

watch(retiroModalOpen, (isOpen) => {
  if (isOpen) {
    initPersonaSelect2();
    initProductoSelect2();
  } else {
    destroyPersonaSelect2();
    destroyProductoSelect2();
  }
});

watch(personas, () => {
  initPersonaSelect2();
});

watch(productos, () => {
  initProductoSelect2();
});

watch(selectedWallet, () => {
  initProductoSelect2();
});

watch(selectedPersonaId, (value) => {
  if (!personaSelect.value) return;
  const element = $(personaSelect.value);
  if (element.hasClass("select2-hidden-accessible") && element.val() !== value) {
    element.val(value || null).trigger("change.select2");
  }
});

watch(selectedProductoId, (value) => {
  if (!productoSelect.value) return;
  const element = $(productoSelect.value);
  if (element.hasClass("select2-hidden-accessible") && element.val() !== value) {
    element.val(value || null).trigger("change.select2");
  }
});

watch(selectedPeriodoId, (value) => {
  if (!periodoSelect.value) return;
  const element = $(periodoSelect.value);
  if (element.hasClass("select2-hidden-accessible") && element.val() !== value) {
    element.val(value || null).trigger("change.select2");
  }
});

onMounted(() => {
  loadCompanyWallet();
  loadPersonas();
});

onBeforeUnmount(() => {
  destroyPersonaSelect2();
  destroyProductoSelect2();
  destroyPeriodoSelect2();
});
</script>

<template>
  <div class="vy company-wallet-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Administracion</div>
          <h1>Caja empresa</h1>
          <p>
            Dinero real recibido por planes, ventas internas y ventas publicas validadas.
            <strong v-if="selectedPeriodo">Mostrando {{ selectedPeriodo.nombre }}.</strong>
          </p>
        </div>
        <div class="header-actions">
          <label class="period-filter">
            <span>Mes</span>
            <select ref="periodoSelect" v-model="selectedPeriodoId">
              <option value="" disabled>Selecciona un mes</option>
              <option v-for="periodo in periodosCaja" :key="periodo.id" :value="periodo.id">
                {{ periodo.nombre }} - Gestion {{ periodo.gestion?.anio || "" }}
              </option>
            </select>
          </label>
          <button class="vy-btn vy-btn-ghost" type="button" :disabled="processing" @click="openRetiroModal">
            <ArrowDownToLine :size="16" /> Detalle / retiro
          </button>
          <button class="vy-btn vy-btn-ghost" type="button" :disabled="processing" @click="cerrarMes">
            <CalendarClock :size="16" /> Cerrar mes
          </button>
          <button class="vy-btn vy-btn-primary" type="button" :disabled="loading" @click="loadCompanyWallet">
            <RefreshCw :size="16" /> Actualizar
          </button>
        </div>
      </header>

      <div v-if="error" class="error-box">{{ error }}</div>
      <div v-if="loading" class="loading-box">Cargando caja empresa...</div>

      <section class="summary-grid">
        <article class="balance-card">
          <div class="balance-icon"><Building2 :size="26" /></div>
          <span>Saldo final del mes</span>
          <strong>Bs. {{ money(resumenPeriodo?.saldoFinal) }}</strong>
          <p>Saldo actual global: Bs. {{ money(cartera?.saldoActual) }}</p>
        </article>

        <article class="metric-card">
          <span class="metric-icon"><WalletCards :size="20" /></span>
          <div>
            <small>Saldo inicial del mes</small>
            <strong>Bs. {{ money(resumenPeriodo?.saldoInicial) }}</strong>
          </div>
        </article>

        <article class="metric-card">
          <span class="metric-icon income"><TrendingUp :size="20" /></span>
          <div>
            <small>Ingresos del mes</small>
            <strong>Bs. {{ money(resumenPeriodo?.ingresos) }}</strong>
          </div>
        </article>

        <article class="metric-card">
          <span class="metric-icon outcome"><TrendingDown :size="20" /></span>
          <div>
            <small>Egresos del mes</small>
            <strong>Bs. {{ money(resumenPeriodo?.egresos) }}</strong>
          </div>
        </article>

        <article class="metric-card">
          <span class="metric-icon"><CalendarClock :size="20" /></span>
          <div>
            <small>Movimientos del mes</small>
            <strong>{{ resumenPeriodo?.cantidadMovimientos || movimientos.length }}</strong>
          </div>
        </article>
      </section>

      <section class="vy-card movements-card">
        <header class="section-header">
          <div>
            <h2>Movimientos de caja</h2>
            <p>Auditoria de ingresos y egresos de {{ selectedPeriodo?.nombre || "el mes seleccionado" }}.</p>
          </div>
        </header>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Concepto</th>
                <th>Referencia</th>
                <th>Fecha</th>
                <th>Tipo</th>
                <th>Monto</th>
                <th>Saldo</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="movimiento in paginatedMovimientos" :key="movimiento.id">
                <td>#{{ movimiento.id }}</td>
                <td>
                  <strong>{{ movimiento.concepto }}</strong>
                </td>
                <td>
                  <span>{{ movimiento.referenciaTipo || "Manual" }}</span>
                  <small v-if="movimiento.referenciaId">#{{ movimiento.referenciaId }}</small>
                </td>
                <td>{{ formatDate(movimiento.fechaRegistro) }}</td>
                <td><span class="status-pill" :class="{ out: movimiento.tipo === 'EGRESO' }">{{ movimiento.tipo }}</span></td>
                <td :class="{ negative: Number(movimiento.monto || 0) < 0 }">Bs. {{ money(movimiento.monto) }}</td>
                <td><strong>Bs. {{ money(movimiento.saldoResultado) }}</strong></td>
              </tr>
              <tr v-if="!movimientos.length && !loading">
                <td colspan="7">No hay movimientos registrados en caja empresa.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <footer v-if="movimientos.length" class="pagination-bar">
          <div>
            <span>Pagina {{ page }} de {{ totalPages }}</span>
            <select v-model.number="pageSize" @change="page = 1">
              <option :value="10">10 por pagina</option>
              <option :value="25">25 por pagina</option>
              <option :value="50">50 por pagina</option>
            </select>
          </div>
          <div class="pagination-actions">
            <button type="button" :disabled="page === 1" @click="previousPage">Anterior</button>
            <button type="button" :disabled="page === totalPages" @click="nextPage">Siguiente</button>
          </div>
        </footer>
      </section>
    </main>

    <Teleport to="body">
      <div v-if="retiroModalOpen" class="retiro-backdrop" @click.self="closeRetiroModal">
        <article class="retiro-modal">
          <header>
            <div>
              <span class="vy-eyebrow">Retiros</span>
              <h2>Detalle y retiro de saldos</h2>
              <p>Selecciona una persona para retirar efectivo inmediato o productos disponibles.</p>
            </div>
            <button type="button" aria-label="Cerrar" @click="closeRetiroModal">
              <X :size="18" />
            </button>
          </header>

          <section class="retiro-body">
            <label class="field">
              <span>Persona</span>
              <select ref="personaSelect" v-model="selectedPersonaId">
                <option value="">Selecciona una persona</option>
                <option v-for="persona in personas" :key="persona.id" :value="persona.id">
                  {{ persona.nombres }} {{ persona.apellidos }} - {{ persona.documento || "Sin documento" }}
                </option>
              </select>
            </label>

            <div v-if="walletLoading" class="loading-box">Cargando saldos de la persona...</div>

            <section v-if="selectedWallet" class="person-wallet-detail">
              <div class="selected-person">
                <small>Persona seleccionada</small>
                <strong>{{ selectedPersona?.nombres }} {{ selectedPersona?.apellidos }}</strong>
                <span>{{ selectedPersona?.documento || "Sin documento" }}</span>
              </div>
              <div class="wallet-values">
                <div>
                  <small>Efectivo inmediato</small>
                  <strong>Bs. {{ money(efectivoDisponibleRetiro) }}</strong>
                  <span>Billetera Bs. {{ money(billeteraSeleccionada.saldoDinero) }} + recompensas Bs. {{ money(selectedWallet?.efectivoRecompensasDisponible) }}</span>
                </div>
                <div>
                  <small>Productos canjeables</small>
                  <strong>Bs. {{ money(productosDisponiblesRetiro) }}</strong>
                  <span>Disponible desde recompensas cobrables. Selecciona productos a precio proveedor.</span>
                </div>
              </div>

              <div class="withdraw-grid">
                <label class="field">
                  <span>Retirar efectivo</span>
                  <input v-model.number="retiroForm.montoDinero" type="number" min="0" step="0.01" />
                </label>
                <div class="field">
                  <span>Total productos</span>
                  <div class="readonly-total">Bs. {{ money(retiroProductosTotal) }}</div>
                </div>
              </div>

              <section class="product-withdrawal">
                <label class="field">
                  <span>Producto a entregar</span>
                  <select ref="productoSelect" v-model="selectedProductoId">
                    <option value="">Selecciona producto</option>
                    <option v-for="producto in productos" :key="producto.id" :value="producto.id">
                      {{ producto.nombre }} - Bs. {{ money(producto.precio) }}
                    </option>
                  </select>
                </label>
                <button class="max-button" type="button" :disabled="!selectedProductoId" @click="addRetiroProducto">
                  Agregar producto
                </button>

                <div v-if="retiroForm.productos.length" class="withdraw-products-list">
                  <article v-for="item in retiroForm.productos" :key="item.productoId">
                    <div>
                      <strong>{{ item.nombre }}</strong>
                      <small>{{ item.sku || "Sin SKU" }} - Precio proveedor Bs. {{ money(item.precio) }}</small>
                    </div>
                    <input
                      :value="item.cantidad"
                      type="number"
                      min="1"
                      @input="updateRetiroProductoCantidad(item, $event.target.value)"
                    />
                    <strong>Bs. {{ money(Number(item.precio || 0) * Number(item.cantidad || 0)) }}</strong>
                    <button type="button" @click="removeRetiroProducto(item)">Quitar</button>
                  </article>
                </div>
                <p v-if="retiroProductosTotal > productosDisponiblesRetiro" class="withdraw-warning">
                  El total de productos supera el saldo canjeable disponible.
                </p>
              </section>

              <label class="field">
                <span>Observacion</span>
                <textarea v-model.trim="retiroForm.observacion" rows="3" placeholder="Ej. Retiro solicitado en caja, canje de productos, pago manual..." />
              </label>

              <button class="max-button" type="button" @click="setMaxRetiro">
                Usar saldo completo disponible
              </button>
            </section>
          </section>

          <footer>
            <button class="vy-btn vy-btn-ghost" type="button" :disabled="processing" @click="closeRetiroModal">Cancelar</button>
            <button
              class="vy-btn vy-btn-primary"
              type="button"
              :disabled="processing || !selectedWallet || retiroProductosTotal > productosDisponiblesRetiro"
              @click="registrarRetiro"
            >
              <ArrowDownToLine :size="16" /> {{ processing ? "Procesando..." : "Procesar retiro" }}
            </button>
          </footer>
        </article>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.workspace { padding: 28px 32px 40px; min-width: 0; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 20px; }
.page-header h1 { margin-top: 8px; font-size: 30px; font-weight: 900; }
.page-header p { margin-top: 5px; color: var(--vy-ink-2); font-size: 14px; }
.page-header p strong { color: var(--vy-orange-deep); font-weight: 900; }
.header-actions { display: flex; align-items: center; justify-content: flex-end; gap: 10px; flex-wrap: wrap; }
.period-filter { display: grid; gap: 6px; min-width: 260px; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.period-filter select { width: 100%; }
.vy-btn-ghost { border: 1px solid var(--vy-line); background: var(--vy-surface); color: var(--vy-ink-2); }
.summary-grid { display: grid; grid-template-columns: minmax(280px, 1.4fr) repeat(4, minmax(170px, 1fr)); gap: 14px; margin-bottom: 18px; }
.balance-card, .metric-card { border: 1px solid var(--vy-line); border-radius: 18px; background: var(--vy-surface); box-shadow: var(--vy-shadow-sm); }
.balance-card { padding: 22px; background: var(--vy-ink); color: #fff; }
.balance-icon { width: 50px; height: 50px; border-radius: 16px; background: rgba(242, 135, 5, 0.18); color: var(--vy-orange); display: inline-flex; align-items: center; justify-content: center; margin-bottom: 18px; }
.balance-card span, .balance-card p { color: rgba(255,255,255,0.72); font-size: 13px; font-weight: 800; }
.balance-card strong { display: block; margin: 8px 0; font-size: 34px; font-weight: 900; }
.metric-card { padding: 18px; display: flex; align-items: center; gap: 14px; }
.metric-icon { width: 44px; height: 44px; border-radius: 14px; background: var(--vy-cream); color: var(--vy-orange-deep); display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; }
.metric-icon.income { background: rgba(63, 143, 92, 0.12); color: var(--vy-success); }
.metric-icon.outcome { background: rgba(196, 69, 42, 0.12); color: var(--vy-danger); }
.metric-card small { display: block; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.metric-card strong { display: block; margin-top: 5px; color: var(--vy-ink); font-size: 20px; font-weight: 900; }
.movements-card { padding: 20px; }
.section-header { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
.section-header h2 { font-size: 18px; font-weight: 900; }
.section-header p { margin-top: 3px; color: var(--vy-ink-3); font-size: 13px; font-weight: 700; }
.table-wrap { overflow-x: auto; }
table { width: 100%; min-width: 920px; border-collapse: collapse; font-size: 13px; }
th { padding: 12px 10px; background: var(--vy-ink); color: #fff; text-align: left; font-size: 11px; font-weight: 900; text-transform: uppercase; white-space: nowrap; }
td { padding: 13px 10px; border-bottom: 1px solid var(--vy-line-2); color: var(--vy-ink-2); vertical-align: middle; }
td strong, td small, td span { display: block; }
td strong { color: var(--vy-ink); font-weight: 900; }
td small { margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; }
.status-pill { display: inline-flex; width: fit-content; align-items: center; min-height: 26px; padding: 0 9px; border-radius: 999px; background: rgba(63, 143, 92, 0.12); color: var(--vy-success); font-size: 11px; font-weight: 900; }
.status-pill.out { background: rgba(196, 69, 42, 0.12); color: var(--vy-danger); }
.negative { color: var(--vy-danger); font-weight: 900; }
.pagination-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 16px; padding-top: 14px; border-top: 1px solid var(--vy-line-2); }
.pagination-bar > div { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.pagination-bar span { color: var(--vy-ink-3); font-size: 12px; font-weight: 900; }
.pagination-bar select { min-height: 36px; padding: 0 10px; border: 1px solid var(--vy-line); border-radius: 10px; background: var(--vy-surface-2); color: var(--vy-ink); font-weight: 800; }
.pagination-actions button { min-height: 36px; padding: 0 12px; border-radius: 10px; background: var(--vy-ink); color: #fff; font-size: 12px; font-weight: 900; }
.pagination-actions button:disabled { opacity: 0.45; cursor: not-allowed; }
.retiro-backdrop { position: fixed; inset: 0; z-index: 120; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(31, 26, 20, 0.55); backdrop-filter: blur(7px); }
.retiro-modal { width: min(780px, 100%); max-height: calc(100vh - 40px); padding: 20px; border-radius: 22px; border: 1px solid var(--vy-line); background: var(--vy-surface); box-shadow: var(--vy-shadow-lg); color: var(--vy-ink); overflow: hidden; display: flex; flex-direction: column; }
.retiro-modal > header, .retiro-modal > footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.retiro-modal > header { padding-bottom: 14px; border-bottom: 1px solid var(--vy-line-2); }
.retiro-modal h2 { margin-top: 4px; font-size: 22px; font-weight: 900; }
.retiro-modal p { margin-top: 3px; color: var(--vy-ink-3); font-size: 13px; font-weight: 800; }
.retiro-modal > header button { width: 38px; height: 38px; border-radius: 12px; background: var(--vy-surface-2); color: var(--vy-ink-2); display: inline-flex; align-items: center; justify-content: center; }
.retiro-body { margin: 16px 0; overflow: auto; }
.field { display: block; }
.field > span { display: block; margin-bottom: 7px; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.field select, .field input, .field textarea { width: 100%; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 800; outline: 0; }
.field select, .field input { min-height: 42px; padding: 0 12px; }
.field textarea { padding: 12px; resize: vertical; }
.readonly-total { min-height: 42px; padding: 0 12px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); color: var(--vy-ink); display: flex; align-items: center; font-size: 13px; font-weight: 900; }
:deep(.select2-container--default .select2-selection--single) { min-height: 42px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); display: flex; align-items: center; }
:deep(.select2-container--default .select2-selection--single .select2-selection__rendered) { padding-left: 12px; padding-right: 34px; color: var(--vy-ink); font-size: 13px; font-weight: 800; line-height: 42px; }
:deep(.select2-container--default .select2-selection--single .select2-selection__placeholder) { color: var(--vy-ink-3); }
:deep(.select2-container--default .select2-selection--single .select2-selection__arrow) { height: 42px; right: 8px; }
:deep(.select2-container--default.select2-container--open .select2-selection--single) { border-color: var(--vy-orange); }
:deep(.select2-dropdown) { border: 1px solid var(--vy-line); border-radius: 12px; overflow: hidden; color: var(--vy-ink); }
:deep(.select2-search--dropdown) { padding: 8px; }
:deep(.select2-container--default .select2-search--dropdown .select2-search__field) { min-height: 36px; border: 1px solid var(--vy-line); border-radius: 9px; outline: 0; padding: 0 10px; }
:deep(.select2-results__option) { padding: 9px 12px; font-size: 13px; font-weight: 800; }
:deep(.select2-container--default .select2-results__option--highlighted.select2-results__option--selectable) { background: var(--vy-orange); color: #fff; }
.person-wallet-detail { margin-top: 14px; display: grid; gap: 14px; }
.selected-person, .wallet-values div { padding: 14px; border: 1px solid var(--vy-line); border-radius: 14px; background: var(--vy-surface-2); }
.selected-person small, .wallet-values small { display: block; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.selected-person strong, .wallet-values strong { display: block; margin-top: 5px; color: var(--vy-ink); font-size: 16px; font-weight: 900; }
.selected-person span { display: block; margin-top: 4px; color: var(--vy-ink-2); font-size: 12px; font-weight: 800; }
.wallet-values span { display: block; margin-top: 4px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; line-height: 1.35; }
.wallet-values, .withdraw-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.product-withdrawal { display: grid; gap: 10px; }
.withdraw-products-list { display: grid; gap: 8px; }
.withdraw-products-list article { display: grid; grid-template-columns: minmax(0, 1fr) 78px 110px auto; align-items: center; gap: 10px; padding: 10px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); }
.withdraw-products-list strong, .withdraw-products-list small { display: block; }
.withdraw-products-list small { margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; }
.withdraw-products-list input { width: 78px; min-height: 36px; padding: 0 8px; border: 1px solid var(--vy-line); border-radius: 10px; background: #fff; color: var(--vy-ink); font-weight: 900; }
.withdraw-products-list button { min-height: 34px; padding: 0 10px; border-radius: 10px; background: rgba(196, 69, 42, 0.08); color: var(--vy-danger); border: 1px solid rgba(196, 69, 42, 0.22); font-size: 12px; font-weight: 900; }
.withdraw-warning { margin: 0; padding: 10px 12px; border-radius: 10px; background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); font-size: 12px; font-weight: 900; }
.max-button { width: fit-content; min-height: 38px; padding: 0 12px; border-radius: 10px; background: #fffaf0; color: var(--vy-orange-deep); border: 1px solid rgba(242, 135, 5, 0.28); font-size: 12px; font-weight: 900; }
.retiro-modal > footer { justify-content: flex-end; padding-top: 14px; border-top: 1px solid var(--vy-line-2); }
.error-box, .loading-box { margin-bottom: 14px; padding: 13px 15px; border-radius: 12px; font-size: 13px; font-weight: 800; }
.error-box { color: var(--vy-danger); background: rgba(196, 69, 42, 0.1); }
.loading-box { color: var(--vy-ink-2); background: var(--vy-surface-2); }
@media (max-width: 1120px) {
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 720px) {
  .workspace { padding: 24px 20px 32px; }
  .page-header { align-items: stretch; flex-direction: column; }
  .header-actions, .header-actions .vy-btn, .period-filter { width: 100%; }
  .header-actions { align-items: stretch; flex-direction: column; }
  .period-filter { min-width: 0; }
  .summary-grid { grid-template-columns: 1fr; }
  .pagination-bar, .retiro-modal > header, .retiro-modal > footer { align-items: stretch; flex-direction: column; }
  .pagination-actions, .pagination-actions button, .retiro-modal > footer .vy-btn { width: 100%; }
  .wallet-values, .withdraw-grid { grid-template-columns: 1fr; }
  .withdraw-products-list article { grid-template-columns: 1fr; }
  .withdraw-products-list input, .withdraw-products-list button { width: 100%; }
}
</style>
