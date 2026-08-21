<script setup>
import { computed, onMounted, ref, watch } from "vue";
import {
  ClipboardList,
  Pencil,
  RefreshCw,
  Search,
  Trash2,
  Plus,
  Minus,
  X
} from "lucide-vue-next";
import { apiRequest } from "../services/api.js";

const compras = ref([]);
const productos = ref([]);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const searchQuery = ref("");
const estadoFilter = ref("TODOS");
const page = ref(1);
const pageSize = 10;

const editModalOpen = ref(false);
const editingCompra = ref(null);
const editItems = ref([]);
const editProductQuery = ref("");
const editDiscountAmount = ref("");
const editDiscountConcept = ref("");

const detalleModalCompra = ref(null);
const detalleMovimientos = ref([]);
const detalleLoading = ref(false);
const detalleError = ref("");

const filteredProductosEdit = computed(() => {
  const text = editProductQuery.value.trim().toLowerCase();
  if (!text) return productos.value;
  return productos.value.filter((p) =>
    [p.nombre, p.sku, p.categoria].some((v) => String(v || "").toLowerCase().includes(text))
  );
});

const editSaleTotal = computed(() =>
  editItems.value.reduce((acc, it) => acc + Number(it.precio || 0) * Number(it.cantidad || 0), 0)
);
const editSalePv = computed(() =>
  editItems.value.reduce((acc, it) => acc + Number(it.pv || 0) * Number(it.cantidad || 0), 0)
);
const editSaleQp = computed(() =>
  editItems.value.reduce((acc, it) => acc + Number(it.qp || 0) * Number(it.cantidad || 0), 0)
);
const editSaleCr = computed(() =>
  editItems.value.reduce((acc, it) => acc + Number(it.cr || 0) * Number(it.cantidad || 0), 0)
);
const editDiscountError = computed(() => {
  const monto = Number(editDiscountAmount.value || 0);
  if (monto < 0) return "El descuento no puede ser negativo.";
  if (monto > editSaleTotal.value) return "El descuento no puede ser mayor al subtotal.";
  if (monto > 0 && !editDiscountConcept.value.trim()) return "Debe ingresar el concepto del descuento.";
  return "";
});

const visibleCompras = computed(() => {
  const text = searchQuery.value.trim().toLowerCase();
  return compras.value.filter((compra) => {
    if (estadoFilter.value !== "TODOS" && compra.estadoCompra !== estadoFilter.value) return false;
    if (!text) return true;
    const persona = compra.persona || {};
    return [
      compra.id,
      compra.estadoCompra,
      compra.codigoPago,
      compra.metodoPago,
      persona.nombres,
      persona.apellidos,
      persona.documento,
      persona.email
    ].some((v) => String(v ?? "").toLowerCase().includes(text));
  });
});

const totalPages = computed(() => Math.max(1, Math.ceil(visibleCompras.value.length / pageSize)));
const paginatedCompras = computed(() => {
  const start = (page.value - 1) * pageSize;
  return visibleCompras.value.slice(start, start + pageSize);
});

watch(searchQuery, () => (page.value = 1));
watch(estadoFilter, () => (page.value = 1));
watch(visibleCompras, () => {
  if (page.value > totalPages.value) page.value = totalPages.value;
});

function fullName(persona) {
  if (!persona) return "Sin persona";
  const name = `${persona.nombres || ""} ${persona.apellidos || ""}`.trim();
  return name || persona.username || "Sin nombre";
}
function money(value) {
  const num = Number(value || 0);
  return num.toLocaleString("es-BO", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
function formatDateTime(value) {
  if (!value) return "Sin registro";
  return new Date(value).toLocaleString("es-BO", { dateStyle: "medium", timeStyle: "short" });
}

async function loadAll() {
  loading.value = true;
  error.value = "";
  try {
    const [comprasData, productosData] = await Promise.all([
      apiRequest("/api/compras/admin/todas"),
      apiRequest("/api/productos")
    ]);
    compras.value = Array.isArray(comprasData) ? comprasData : [];
    productos.value = Array.isArray(productosData) ? productosData : [];
  } catch (e) {
    error.value = e.message || "No se pudo cargar las compras.";
  } finally {
    loading.value = false;
  }
}

function openEditModal(compra) {
  editingCompra.value = compra;
  editItems.value = (compra.detalles || []).map((d) => ({
    id: d.producto?.id,
    nombre: d.producto?.nombre || "Producto",
    sku: d.producto?.sku || "",
    precio: Number(d.precioUnitario ?? d.producto?.precio ?? 0),
    pv: Number(d.pvUnitario ?? d.producto?.pv ?? 0),
    qp: Number(d.qpUnitario ?? d.producto?.qp ?? 0),
    cr: Number(d.crUnitario ?? d.producto?.cr ?? 0),
    cantidad: Number(d.cantidad || 1)
  })).filter((it) => it.id);
  editDiscountAmount.value = compra.descuentoMonto != null ? String(compra.descuentoMonto) : "";
  editDiscountConcept.value = compra.descuentoConcepto || "";
  editProductQuery.value = "";
  editModalOpen.value = true;
}
function closeEditModal() {
  editModalOpen.value = false;
  editingCompra.value = null;
}
function addEditProduct(producto) {
  const found = editItems.value.find((it) => Number(it.id) === Number(producto.id));
  if (found) { found.cantidad += 1; return; }
  editItems.value.push({
    id: producto.id,
    nombre: producto.nombre,
    sku: producto.sku,
    precio: Number(producto.precio || 0),
    pv: Number(producto.pv || 0),
    qp: Number(producto.qp || 0),
    cr: Number(producto.cr || 0),
    cantidad: 1
  });
}
function removeEditItem(item) {
  editItems.value = editItems.value.filter((it) => Number(it.id) !== Number(item.id));
}
function changeEditQuantity(item, value) {
  const next = Math.max(1, Number(value || 1));
  item.cantidad = next;
}
async function saveEdit() {
  if (!editingCompra.value) return;
  if (!editItems.value.length) {
    error.value = "Agrega al menos un producto.";
    return;
  }
  if (editDiscountError.value) {
    error.value = editDiscountError.value;
    return;
  }
  const confirmMsg = `Se anulará lógicamente la compra #${editingCompra.value.id} (quedará ANULADA para auditoría) y se creará una nueva con misma fecha/periodo/validación pero con productos actualizados. Las recompensas antiguas serán revertidas y regeneradas. ¿Continuar?`;
  if (!window.confirm(confirmMsg)) return;

  saving.value = true;
  error.value = "";
  try {
    const payload = {
      items: editItems.value.map((it) => ({ productoId: it.id, cantidad: it.cantidad })),
      descuentoMonto: editDiscountAmount.value === "" ? null : Number(editDiscountAmount.value),
      descuentoConcepto: editDiscountConcept.value || null
    };
    await apiRequest(`/api/compras/${editingCompra.value.id}/admin-reconstruir`, {
      method: "PUT",
      body: JSON.stringify(payload)
    });
    closeEditModal();
    await loadAll();
  } catch (e) {
    error.value = e.message || "No se pudo guardar la edición.";
  } finally {
    saving.value = false;
  }
}

// Detalles
async function openDetallesModal(compra) {
  detalleModalCompra.value = compra;
  detalleMovimientos.value = [];
  detalleError.value = "";
  detalleLoading.value = true;
  try {
    const data = await apiRequest(`/api/compras/${compra.id}/movimientos`);
    detalleMovimientos.value = Array.isArray(data) ? data : [];
  } catch (e) {
    detalleError.value = e.message || "No se pudieron cargar los movimientos.";
  } finally {
    detalleLoading.value = false;
  }
}
function closeDetallesModal() {
  detalleModalCompra.value = null;
  detalleMovimientos.value = [];
  detalleError.value = "";
}
const MOV_LABELS = {
  VOLUMEN_COMPRADOR: "Volumen del comprador",
  BONO_REFERIDO: "QP bono referido (hacia arriba)",
  BENEFICIO_ACTIVACION: "Beneficios de activacion",
  AJUSTE_BENEFICIO: "Ajustes retroactivos",
  CARTERA_EMPRESA: "Cartera de la empresa",
  ANULACION: "Anulaciones"
};
const MOV_ORDEN = ["VOLUMEN_COMPRADOR","BONO_REFERIDO","BENEFICIO_ACTIVACION","AJUSTE_BENEFICIO","CARTERA_EMPRESA","ANULACION"];
const movimientosAgrupados = computed(() => {
  const grupos = [];
  for (const origen of MOV_ORDEN) {
    const items = detalleMovimientos.value.filter((m) => m.origen === origen);
    if (!items.length) continue;
    grupos.push({ origen, label: MOV_LABELS[origen] || origen, items, total: items.reduce((a,m)=>a+Number(m.monto||0),0) });
  }
  const otros = detalleMovimientos.value.filter((m)=>!MOV_ORDEN.includes(m.origen));
  if (otros.length) grupos.push({ origen:"OTROS", label:"Otros", items: otros, total: otros.reduce((a,m)=>a+Number(m.monto||0),0) });
  return grupos;
});

onMounted(loadAll);
</script>

<template>
  <div class="vy admin-compras-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Administración</div>
          <h1>Compras</h1>
          <p>Todas las compras del sistema. Editar anula lógicamente la original (ANULADA) y crea una nueva con misma fecha/periodo.</p>
        </div>
        <div class="header-actions">
          <label class="period-filter search-filter">
            <span>Buscar</span>
            <div class="input-icon">
              <Search :size="15" />
              <input v-model.trim="searchQuery" placeholder="Compra, cliente, documento, estado..." />
            </div>
          </label>
          <label class="period-filter">
            <span>Estado</span>
            <select v-model="estadoFilter">
              <option value="TODOS">Todos</option>
              <option value="PENDIENTE">PENDIENTE</option>
              <option value="VALIDADA">VALIDADA</option>
              <option value="RECHAZADA">RECHAZADA</option>
              <option value="ANULADA">ANULADA</option>
              <option value="CONFIRMADA">CONFIRMADA</option>
            </select>
          </label>
          <button class="refresh-action" type="button" :disabled="loading" @click="loadAll">
            <RefreshCw :class="{ spinning: loading }" :size="15" /> {{ loading ? "Actualizando" : "Actualizar" }}
          </button>
        </div>
      </header>

      <div v-if="error" class="error-box">{{ error }}</div>

      <section v-if="loading" class="vy-card">
        <p style="padding:18px">Cargando compras...</p>
      </section>

      <section v-else class="vy-card sales-table-card">
        <div class="card-title">
          <span class="icon-box"><ClipboardList :size="18" /></span>
          <div>
            <h2>Todas las compras</h2>
            <p>{{ visibleCompras.length }} registros</p>
          </div>
        </div>
        <div class="sales-table-wrap">
          <table class="sales-table">
            <thead>
              <tr>
                <th>Compra</th>
                <th>Cliente</th>
                <th>Fecha</th>
                <th>Periodo</th>
                <th>Estado</th>
                <th>Total</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="compra in paginatedCompras" :key="compra.id">
                <td><strong>#{{ compra.id }}</strong><small v-if="compra.codigoPago">Caja {{ compra.codigoPago }}</small></td>
                <td><strong>{{ fullName(compra.persona) }}</strong><small>{{ compra.persona?.documento || "Sin documento" }}</small></td>
                <td>{{ formatDateTime(compra.fechaCompra) }}</td>
                <td>{{ compra.periodo?.nombre || compra.periodo?.id || "-" }}</td>
                <td><span class="status-pill" :class="{ anulada: compra.estadoCompra==='ANULADA' }">{{ compra.estadoCompra }}</span></td>
                <td>
                  <strong>Bs. {{ money(compra.subtotal) }}</strong>
                  <small>PV {{ money(compra.totalPv) }} · QP {{ money(compra.totalQp) }}</small>
                  <small v-if="Number(compra.descuentoMonto||0)>0">Desc. Bs. {{ money(compra.descuentoMonto) }}</small>
                </td>
                <td>
                  <div style="display:flex;gap:6px;flex-wrap:wrap">
                    <button class="vy-btn vy-btn-ghost" type="button" @click="openDetallesModal(compra)"><ClipboardList :size="14" /> Detalles</button>
                    <button class="vy-btn vy-btn-primary" type="button" :disabled="compra.estadoCompra==='ANULADA'" title="Editar" @click="openEditModal(compra)"><Pencil :size="14" /> Editar</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="visibleCompras.length > pageSize" class="pagination-bar">
            <span>{{ (page-1)*pageSize+1 }}-{{ Math.min(page*pageSize, visibleCompras.length) }} de {{ visibleCompras.length }}</span>
            <div>
              <button type="button" :disabled="page<=1" @click="page--">Anterior</button>
              <strong>{{ page }} / {{ totalPages }}</strong>
              <button type="button" :disabled="page>=totalPages" @click="page++">Siguiente</button>
            </div>
          </div>
          <div v-if="!visibleCompras.length" class="empty-state">
            <ClipboardList :size="28" />
            <strong>No hay compras</strong>
          </div>
        </div>
      </section>
    </main>

    <!-- Modal edición -->
    <Teleport to="body">
      <div v-if="editModalOpen" class="receipt-modal-backdrop" @click.self="closeEditModal">
        <article class="receipt-modal detalle-modal">
          <header>
            <div>
              <span class="vy-eyebrow">Edición administrativa</span>
              <h2>Compra #{{ editingCompra?.id }}</h2>
              <p>{{ fullName(editingCompra?.persona) }} · {{ formatDateTime(editingCompra?.fechaCompra) }} · {{ editingCompra?.estadoCompra }}</p>
            </div>
            <button type="button" aria-label="Cerrar" @click="closeEditModal"><X :size="18" /></button>
          </header>
          <section class="detalle-modal-body">
            <div class="alert-warn">Se anulará lógicamente la compra #{{ editingCompra?.id }} (ANULADA) y se creará una nueva con misma fecha y periodo. Las recompensas se destruirán y regenerarán. El saldo ya utilizado bloqueará la operación.</div>

            <div class="detalle-block">
              <h3>Productos</h3>
              <label class="field">
                <span>Buscar producto</span>
                <div class="input-icon"><Search :size="15" /><input v-model.trim="editProductQuery" placeholder="Nombre, SKU o categoria" /></div>
              </label>
              <div class="product-picker" style="margin-top:10px;max-height:240px;overflow:auto;display:grid;gap:8px">
                <button v-for="p in filteredProductosEdit.slice(0,8)" :key="p.id" type="button" @click="addEditProduct(p)" style="padding:10px 12px;border:1px solid var(--vy-line);border-radius:12px;background:var(--vy-surface-2);text-align:left;display:flex;align-items:center;gap:10px">
                  <span style="flex:1;min-width:0"><strong style="display:block">{{ p.nombre }}</strong><small style="color:var(--vy-ink-3)">{{ p.sku }} · PV {{ money(p.pv) }} QP {{ money(p.qp) }}</small></span>
                  <b>Bs. {{ money(p.precio) }}</b><Plus :size="16" />
                </button>
              </div>
              <div style="margin-top:14px;display:grid;gap:8px">
                <div v-for="it in editItems" :key="it.id" style="display:grid;grid-template-columns:minmax(0,1fr) 110px 100px 34px;gap:8px;align-items:center;padding:8px 0;border-top:1px solid var(--vy-line-2)">
                  <div><strong style="display:block;font-size:13px">{{ it.nombre }}</strong><small style="color:var(--vy-ink-3)">Bs. {{ money(it.precio) }} · PV {{ money(it.pv) }}</small></div>
                  <div style="display:flex;align-items:center;gap:6px"><button type="button" @click="it.cantidad=Math.max(1,it.cantidad-1)" style="width:28px;height:28px;border-radius:50%;background:#fff;border:1px solid var(--vy-line)"><Minus :size="12" /></button><input :value="it.cantidad" type="number" min="1" style="width:48px;text-align:center;border:1px solid var(--vy-line);border-radius:8px;padding:4px" @input="changeEditQuantity(it, $event.target.value)" /><button type="button" @click="it.cantidad++" style="width:28px;height:28px;border-radius:50%;background:#fff;border:1px solid var(--vy-line)"><Plus :size="12" /></button></div>
                  <b style="text-align:right">Bs. {{ money(it.precio*it.cantidad) }}</b>
                  <button type="button" @click="removeEditItem(it)" style="width:32px;height:32px;border-radius:8px;background:rgba(196,69,42,.1);color:var(--vy-danger);display:inline-flex;align-items:center;justify-content:center"><Trash2 :size="14" /></button>
                </div>
              </div>
            </div>

            <div class="detalle-block">
              <h3>Descuento</h3>
              <div style="display:grid;grid-template-columns:180px minmax(0,1fr);gap:12px">
                <label class="field"><span>Monto</span><input type="number" min="0" step="0.01" v-model="editDiscountAmount" placeholder="0.00" /></label>
                <label class="field"><span>Concepto</span><input v-model.trim="editDiscountConcept" placeholder="Motivo del descuento" /></label>
              </div>
              <small v-if="editDiscountError" style="color:var(--vy-danger);font-weight:800;margin-top:6px;display:block">{{ editDiscountError }}</small>
            </div>

            <div class="detalle-block" style="display:flex;justify-content:space-between;gap:12px;flex-wrap:wrap;padding:14px;border:1px solid var(--vy-line);border-radius:12px;background:#fffaf0">
              <div><small>Subtotal</small><br /><strong>Bs. {{ money(editSaleTotal) }}</strong></div>
              <div><small>PV</small><br /><strong>{{ money(editSalePv) }}</strong></div>
              <div><small>QP</small><br /><strong>{{ money(editSaleQp) }}</strong></div>
              <div><small>Total a pagar</small><br /><strong>Bs. {{ money(Math.max(0, editSaleTotal - Number(editDiscountAmount||0))) }}</strong></div>
            </div>
          </section>
          <footer>
            <button class="vy-btn vy-btn-ghost" type="button" @click="closeEditModal">Cancelar</button>
            <button class="vy-btn vy-btn-primary" type="button" :disabled="saving || !!editDiscountError || !editItems.length" @click="saveEdit">{{ saving ? "Guardando..." : "Guardar cambios" }}</button>
          </footer>
        </article>
      </div>

      <!-- Modal detalles -->
      <div v-if="detalleModalCompra" class="receipt-modal-backdrop" @click.self="closeDetallesModal">
        <article class="receipt-modal detalle-modal">
          <header>
            <div><span class="vy-eyebrow">Detalle completo</span><h2>Compra #{{ detalleModalCompra.id }}</h2><p>{{ detalleModalCompra.estadoCompra }} · {{ fullName(detalleModalCompra.persona) }} · {{ formatDateTime(detalleModalCompra.fechaCompra) }}</p></div>
            <button type="button" aria-label="Cerrar" @click="closeDetallesModal"><X :size="18" /></button>
          </header>
          <section class="detalle-modal-body">
            <div class="detalle-block">
              <h3>Productos</h3>
              <div class="receipt-table-wrap"><table><thead><tr><th>Producto</th><th>SKU</th><th>Cant.</th><th>Precio</th><th>Volumen</th><th>Subtotal</th></tr></thead><tbody><tr v-for="d in detalleModalCompra.detalles || []" :key="d.id"><td>{{ d.producto?.nombre || "Producto" }}</td><td>{{ d.producto?.sku || "" }}</td><td>{{ d.cantidad }}</td><td>Bs. {{ money(d.precioUnitario) }}</td><td>PV {{ money(d.pvUnitario) }} / QP {{ money(d.qpUnitario) }} / CR {{ money(d.crUnitario) }}</td><td>Bs. {{ money(d.subtotal) }}</td></tr></tbody></table></div>
            </div>
            <div class="detalle-block">
              <h3>Movimientos que genero</h3>
              <p v-if="detalleLoading" class="detalle-loading">Cargando...</p>
              <div v-else-if="detalleError" class="detalle-error">{{ detalleError }}</div>
              <div v-else-if="!detalleMovimientos.length" class="detalle-empty">Sin movimientos.</div>
              <template v-else>
                <section v-for="g in movimientosAgrupados" :key="g.origen" class="detalle-grupo">
                  <header><strong>{{ g.label }}</strong><span>Total {{ money(g.total) }}</span></header>
                  <div class="receipt-table-wrap"><table><thead><tr><th>Persona</th><th>Tipo</th><th>Concepto</th><th>Monto</th><th>Saldo</th><th>Fecha</th></tr></thead><tbody><tr v-for="(m,i) in g.items" :key="`${g.origen}-${i}`"><td><strong>{{ m.personaNombres }} {{ m.personaApellidos }}</strong><small v-if="m.nivel">Nivel {{ m.nivel }}</small></td><td>{{ m.tipo }}</td><td>{{ m.concepto }}</td><td>{{ money(m.monto) }}</td><td>{{ money(m.saldoResultado) }}</td><td>{{ formatDateTime(m.fechaRegistro) }}</td></tr></tbody></table></div>
                </section>
              </template>
            </div>
          </section>
          <footer><button class="vy-btn vy-btn-primary" type="button" @click="closeDetallesModal">Cerrar</button></footer>
        </article>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.workspace{padding:28px 32px 110px;min-width:0}
.page-header{display:flex;align-items:flex-end;justify-content:space-between;gap:18px;margin-bottom:20px}
.page-header h1{margin-top:8px;font-size:30px;font-weight:900}
.page-header p{margin-top:5px;color:var(--vy-ink-2);font-size:14px}
.header-actions{display:flex;align-items:center;gap:10px;flex-wrap:wrap;justify-content:flex-end}
.period-filter{display:grid;gap:6px;min-width:220px;color:var(--vy-ink-3);font-size:11px;font-weight:900;text-transform:uppercase}
.search-filter{min-width:320px;flex:1 1 320px}
.period-filter select{width:100%}
.refresh-action{min-height:42px;padding:0 15px;border:1px solid var(--vy-line);border-radius:12px;background:var(--vy-surface);color:var(--vy-ink-2);box-shadow:var(--vy-shadow-sm);display:inline-flex;align-items:center;justify-content:center;gap:8px;font-size:13px;font-weight:900}
.refresh-action:disabled{cursor:wait;opacity:.7}
.refresh-action .spinning{animation:refresh-spin .8s linear infinite}
@keyframes refresh-spin{to{transform:rotate(360deg)}}
.vy-card{background:var(--vy-surface);border:1px solid var(--vy-line);border-radius:18px;box-shadow:var(--vy-shadow-sm);overflow:hidden}
.card-title{display:flex;align-items:center;gap:12px;padding:16px 18px;border-bottom:1px solid var(--vy-line-2)}
.card-title h2{font-size:16px;font-weight:900}
.card-title p{color:var(--vy-ink-3);font-size:12px;font-weight:800}
.icon-box{width:36px;height:36px;border-radius:12px;background:var(--vy-surface-2);display:inline-flex;align-items:center;justify-content:center;color:var(--vy-orange)}
.sales-table-wrap{overflow:auto}
.sales-table{width:100%;border-collapse:collapse;font-size:13px}
.sales-table th{padding:12px 10px;background:var(--vy-surface-2);text-align:left;font-size:11px;text-transform:uppercase;white-space:nowrap;color:var(--vy-ink-3)}
.sales-table td{padding:12px 10px;border-top:1px solid var(--vy-line-2);vertical-align:top}
.status-pill{padding:4px 8px;border-radius:999px;background:#fff3df;color:var(--vy-orange-deep);font-size:11px;font-weight:900}
.status-pill.anulada{background:rgba(196,69,42,.12);color:var(--vy-danger)}
.pagination-bar{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:12px 14px;border-top:1px solid var(--vy-line-2)}
.vy-btn{min-height:36px;padding:0 14px;border-radius:10px;font-size:13px;font-weight:900;display:inline-flex;align-items:center;gap:6px}
.vy-btn-primary{background:var(--vy-orange);color:#fff}
.vy-btn-ghost{background:var(--vy-surface-2);border:1px solid var(--vy-line)}
.input-icon{min-height:42px;padding:0 12px;border:1px solid var(--vy-line);border-radius:12px;background:var(--vy-surface-2);display:flex;align-items:center;gap:8px;color:var(--vy-ink-3)}
.input-icon input{width:100%;border:0;outline:0;background:transparent;color:var(--vy-ink);font:inherit;font-size:13px;font-weight:800}
.field{display:grid;gap:6px}
.field span{color:var(--vy-ink-3);font-size:11px;font-weight:900;text-transform:uppercase}
.field input{width:100%;min-height:42px;padding:0 12px;border:1px solid var(--vy-line);border-radius:12px;background:var(--vy-surface-2);font-size:13px;font-weight:800}
.error-box{margin-bottom:14px;padding:13px 15px;border-radius:12px;background:rgba(196,69,42,.1);color:var(--vy-danger);font-weight:800}
.empty-state{min-height:180px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;color:var(--vy-ink-3)}
.alert-warn{padding:12px 14px;border-radius:12px;background:#fff3df;border:1px solid rgba(242,135,5,.3);color:#7a4a00;font-size:13px;font-weight:800}
.receipt-modal-backdrop{position:fixed;inset:0;z-index:125;display:flex;align-items:center;justify-content:center;padding:20px;background:rgba(31,26,20,.55);backdrop-filter:blur(7px)}
.receipt-modal{width:min(1040px,100%);max-height:calc(100vh - 40px);padding:20px;border-radius:22px;border:1px solid var(--vy-line);background:var(--vy-surface);box-shadow:var(--vy-shadow-lg);color:var(--vy-ink);overflow:hidden;display:flex;flex-direction:column}
.receipt-modal>header,.receipt-modal>footer{display:flex;align-items:center;justify-content:space-between;gap:14px}
.receipt-modal>header{padding-bottom:14px;border-bottom:1px solid var(--vy-line-2)}
.receipt-modal h2{margin-top:4px;font-size:22px;font-weight:900}
.receipt-modal p{margin-top:3px;color:var(--vy-ink-3);font-size:13px;font-weight:800}
.receipt-modal>header button{width:38px;height:38px;border-radius:12px;background:var(--vy-surface-2);color:var(--vy-ink-2);display:inline-flex;align-items:center;justify-content:center}
.detalle-modal{width:min(1040px,100%)}
.detalle-modal-body{margin:16px 0;padding-right:4px;overflow:auto;display:grid;gap:20px}
.detalle-block h3{margin-bottom:10px;font-size:15px;font-weight:900;color:var(--vy-ink)}
.detalle-grupo{border:1px solid var(--vy-line);border-radius:16px;overflow:hidden;background:var(--vy-surface-2)}
.detalle-grupo>header{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:12px 14px;background:#fffaf0;border-bottom:1px solid var(--vy-line-2)}
.detalle-grupo>header strong{font-size:14px;font-weight:900;color:var(--vy-ink)}
.detalle-grupo>header span{font-size:12px;font-weight:900;color:var(--vy-orange-deep)}
.detalle-grupo .receipt-table-wrap{margin-top:0;background:#fff}
.receipt-table-wrap{overflow:auto}
.receipt-table-wrap table{width:100%;border-collapse:collapse;font-size:13px}
.receipt-table-wrap th{padding:11px 9px;background:var(--vy-ink);color:#fff;text-align:left;font-size:11px;text-transform:uppercase;white-space:nowrap}
.receipt-table-wrap td{padding:12px 9px;border-bottom:1px solid var(--vy-line-2);color:var(--vy-ink-2);vertical-align:top}
.detalle-loading,.detalle-empty{padding:18px;color:var(--vy-ink-3);font-weight:800}
.detalle-error{padding:18px;color:var(--vy-danger);font-weight:800}
</style>
