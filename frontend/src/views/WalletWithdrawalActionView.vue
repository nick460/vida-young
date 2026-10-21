<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { ArrowLeft, ArrowDownToLine, RefreshCw } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";

const route = useRoute();
const router = useRouter();
const loading = ref(true);
const processing = ref(false);
const error = ref("");
const wallet = ref(null);
const retiro = ref(null);
const observacion = ref("");
const nivelFiltro = ref("");

const personaId = computed(() => Number(route.params.personaId));
const periodoId = computed(() => route.query.periodoId ? Number(route.query.periodoId) : null);
const esRetiro = computed(() => route.params.modo === "retiro");
const esRetiroProcesado = computed(() => Boolean(route.query.retiroId));
const billetera = computed(() => wallet.value?.billetera || {});
const detalleEfectivo = computed(() => wallet.value?.detalleEfectivoMensual || []);
const efectivoDisponible = computed(() =>
  Number(billetera.value.saldoDinero || 0) + Number(wallet.value?.efectivoRecompensasDisponible || 0)
);
const movimientos = computed(() => wallet.value?.movimientos || []);
const nivelesDisponibles = computed(() => [...new Set([
  ...detalleEfectivo.value.map((item) => Number(item.nivelGenerado)),
  ...movimientos.value.map((item) => Number(item.nivelGenerado))
].filter((nivel) => nivel >= 2))].sort((left, right) => left - right));
const patrocinioFiltrado = computed(() => detalleEfectivo.value.filter((item) =>
  !nivelFiltro.value || Number(item.nivelGenerado) === Number(nivelFiltro.value)
));
const dineroPatrocinioFiltrado = computed(() => patrocinioFiltrado.value
  .reduce((total, item) => total + Number(item.montoDisponible || 0), 0));
const comprasConBeneficio = computed(() => movimientos.value.filter((movimiento) => {
  if (!movimiento.compra || Number(movimiento.monto || 0) <= 0) return false;
  return !nivelFiltro.value || Number(movimiento.nivelGenerado) === Number(nivelFiltro.value);
}));
const persona = computed(() => ({
  nombres: route.query.nombres || retiro.value?.nombres || "",
  apellidos: route.query.apellidos || retiro.value?.apellidos || "",
  documento: route.query.documento || retiro.value?.documento || ""
}));

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatDateTime(value) {
  return value ? new Date(value).toLocaleString("es-BO", {
    day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit"
  }) : "-";
}

function volver() {
  router.push({ name: "retiros-billeteras", query: periodoId.value ? { periodoId: periodoId.value } : {} });
}

async function cargar() {
  loading.value = true;
  error.value = "";
  try {
    const query = periodoId.value ? `?periodoId=${periodoId.value}` : "";
    wallet.value = await apiRequest(`/api/billeteras/persona/${personaId.value}${query}`);
    if (esRetiroProcesado.value) {
      const retiros = await apiRequest(`/api/billeteras/retiros${query}`);
      retiro.value = (Array.isArray(retiros) ? retiros : []).find((item) => Number(item.id) === Number(route.query.retiroId)) || null;
    }
  } catch (exception) {
    error.value = exception.message || "No se pudo cargar la información de la billetera.";
  } finally {
    loading.value = false;
  }
}

async function registrarRetiro() {
  if (processing.value || efectivoDisponible.value <= 0) return;
  const result = await Swal.fire({
    title: "Registrar retiro",
    text: `Se registrará un retiro total de Bs. ${money(efectivoDisponible.value)}.`,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Sí, registrar",
    cancelButtonText: "Cancelar",
    confirmButtonColor: "#F28705"
  });
  if (!result.isConfirmed) return;

  processing.value = true;
  try {
    const procesado = await apiRequest(`/api/billeteras/persona/${personaId.value}/retiros`, {
      method: "POST",
      body: JSON.stringify({
        montoDinero: efectivoDisponible.value,
        montoProductos: 0,
        periodoId: periodoId.value,
        productos: [],
        observacion: observacion.value || null
      })
    });
    await Swal.fire("Retiro procesado", "Los saldos de la persona fueron actualizados.", "success");
    router.replace({ name: "retiros-billeteras-accion", params: { personaId: personaId.value, modo: "detalle" }, query: { periodoId: periodoId.value, retiroId: procesado.id } });
    await cargar();
  } catch (exception) {
    error.value = exception.message || "No se pudo registrar el retiro.";
  } finally {
    processing.value = false;
  }
}

onMounted(cargar);
</script>

<template>
  <div class="vy withdrawal-action-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <span class="vy-eyebrow">Retiros de billeteras</span>
          <h1>{{ esRetiro ? "Registrar retiro" : "Detalle de billetera" }}</h1>
          <p>{{ persona.nombres }} {{ persona.apellidos }} <template v-if="persona.documento">· {{ persona.documento }}</template></p>
        </div>
        <div class="actions">
          <button class="secondary" type="button" :disabled="loading" @click="cargar"><RefreshCw :size="16" /> Actualizar</button>
          <button class="secondary" type="button" @click="volver"><ArrowLeft :size="16" /> Volver</button>
        </div>
      </header>

      <div v-if="error" class="error-box">{{ error }}</div>
      <div v-else-if="loading" class="loading-box">Cargando información de la billetera...</div>

      <template v-else>
        <section class="summary-grid">
          <article><small>Efectivo disponible</small><strong>Bs. {{ money(efectivoDisponible) }}</strong><span>Compras y recompensas mensuales</span></article>
          <article><small>Patrocinio niveles 2+</small><strong>Bs. {{ money(dineroPatrocinioFiltrado) }}</strong><span>{{ nivelFiltro ? `Solo nivel ${nivelFiltro}` : "Todos los niveles" }}</span></article>
          <article><small>Productos nivel 1</small><strong>Bs. {{ money(wallet?.productosRecompensasDisponible) }}</strong><span>Se retiran desde su módulo específico</span></article>
          <article v-if="retiro"><small>Retiro procesado</small><strong>Bs. {{ money(retiro.montoDinero) }}</strong><span>{{ formatDateTime(retiro.fechaRetiro) }}</span></article>
        </section>

        <section class="vy-card content-card">
          <header><div><span class="vy-eyebrow">{{ esRetiro ? "Confirmación" : "Movimientos" }}</span><h2>{{ esRetiro ? "Monto a pagar" : esRetiroProcesado ? `Retiro #${retiro?.id || route.query.retiroId}` : "Origen de los saldos" }}</h2></div></header>

          <template v-if="esRetiro">
            <div class="total-box"><span>Retiro total obligatorio</span><strong>Bs. {{ money(efectivoDisponible) }}</strong></div>
            <section class="detail-section">
              <header class="section-heading"><div><small>Dinero que recibirá</small><h3>Desglose antes de registrar</h3></div><label>Filtrar patrocinio<select v-model="nivelFiltro"><option value="">Todos los niveles</option><option v-for="nivel in nivelesDisponibles" :key="nivel" :value="nivel">Nivel {{ nivel }}</option></select></label></header>
              <div class="breakdown"><span><b>Dinero por compras y activaciones</b><small>Saldo directo disponible de la billetera.</small></span><b>Bs. {{ money(billetera.saldoDinero) }}</b></div>
              <div class="table-wrap"><table><thead><tr><th>Compra</th><th>Realizada por</th><th>Nivel</th><th>Dinero recibido</th></tr></thead><tbody><tr v-for="movimiento in comprasConBeneficio" :key="movimiento.id"><td>#{{ movimiento.compra.id }}<small>{{ formatDateTime(movimiento.compra.fechaCompra || movimiento.fechaRegistro) }}</small></td><td>{{ movimiento.compra.personaNombres }} {{ movimiento.compra.personaApellidos }}<small>{{ movimiento.compra.personaDocumento || "Sin documento" }}</small></td><td>{{ movimiento.nivelGenerado ? `Nivel ${movimiento.nivelGenerado}` : "Directo" }}</td><td>Bs. {{ money(movimiento.monto) }}</td></tr><tr v-if="!comprasConBeneficio.length"><td colspan="4">No hay compras para el filtro seleccionado.</td></tr></tbody></table></div>
              <div class="breakdown"><span><b>Dinero por patrocinio, niveles 2+</b><small>Recompensas disponibles de los referidos.</small></span><b>Bs. {{ money(dineroPatrocinioFiltrado) }}</b></div>
              <div v-for="item in patrocinioFiltrado" :key="item.recompensaId" class="breakdown detail-row"><span><b>Nivel {{ item.nivelGenerado }} · {{ item.referidoNombre || "Referido" }}</b><small>{{ item.planIngreso || "Plan" }}<template v-if="item.referidoDocumento"> · CI {{ item.referidoDocumento }}</template></small></span><b>Bs. {{ money(item.montoDisponible) }}</b></div>
            </section>
            <label>Observación interna<textarea v-model.trim="observacion" rows="4" placeholder="Detalle interno del retiro"></textarea></label>
            <footer><button class="primary" type="button" :disabled="processing || efectivoDisponible <= 0" @click="registrarRetiro"><ArrowDownToLine :size="17" /> {{ processing ? "Procesando..." : "Registrar retiro" }}</button></footer>
          </template>

          <template v-else-if="esRetiroProcesado && retiro">
            <div class="breakdown"><span><b>Dinero de compras</b><small>Beneficios de compras y activaciones.</small></span><b>Bs. {{ money(retiro.montoDesdeBilletera) }}</b></div>
            <div class="breakdown"><span><b>Dinero por niveles de referidos</b><small>Recompensas mensuales de nivel 2 en adelante.</small></span><b>Bs. {{ money(retiro.montoDesdeRecompensas) }}</b></div>
            <div v-if="retiro.observacion" class="note"><b>Observación</b><br>{{ retiro.observacion }}</div>
          </template>

          <template v-else>
            <header class="section-heading"><div><small>Filtro de patrocinio</small><h3>Detalle de dinero recibido</h3></div><label>Mostrar<select v-model="nivelFiltro"><option value="">Todos los niveles</option><option v-for="nivel in nivelesDisponibles" :key="nivel" :value="nivel">Nivel {{ nivel }}</option></select></label></header>
            <section class="detail-section"><h3>Dinero por compras y activaciones</h3><div class="breakdown"><span><b>Saldo directo</b><small>Beneficios de compras y activaciones del período.</small></span><b>Bs. {{ money(billetera.saldoDinero) }}</b></div><div class="table-wrap"><table><thead><tr><th>Compra</th><th>Realizada por</th><th>Nivel</th><th>Dinero recibido</th></tr></thead><tbody><tr v-for="movimiento in comprasConBeneficio" :key="movimiento.id"><td>#{{ movimiento.compra.id }}<small>{{ formatDateTime(movimiento.compra.fechaCompra || movimiento.fechaRegistro) }}</small></td><td>{{ movimiento.compra.personaNombres }} {{ movimiento.compra.personaApellidos }}<small>{{ movimiento.compra.personaDocumento || "Sin documento" }}</small></td><td>{{ movimiento.nivelGenerado ? `Nivel ${movimiento.nivelGenerado}` : "Directo" }}</td><td>Bs. {{ money(movimiento.monto) }}</td></tr><tr v-if="!comprasConBeneficio.length"><td colspan="4">No hay compras para el filtro seleccionado.</td></tr></tbody></table></div></section>
            <section class="detail-section"><h3>Dinero por patrocinio, niveles 2+</h3><div class="breakdown"><span><b>Total de patrocinio</b><small>Recompensas disponibles para el período.</small></span><b>Bs. {{ money(dineroPatrocinioFiltrado) }}</b></div><div v-for="item in patrocinioFiltrado" :key="item.recompensaId" class="breakdown detail-row"><span><b>Nivel {{ item.nivelGenerado }} · {{ item.referidoNombre || "Referido" }}</b><small>{{ item.planIngreso || "Plan" }}<template v-if="item.referidoDocumento"> · CI {{ item.referidoDocumento }}</template></small></span><b>Bs. {{ money(item.montoDisponible) }}</b></div><div v-if="!patrocinioFiltrado.length" class="empty">No hay recompensas de patrocinio para el filtro seleccionado.</div></section>
          </template>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.withdrawal-action-view { min-height: 100vh; background: var(--vy-bg); color: var(--vy-ink); }
.workspace { width: min(1180px, 100%); margin: 0 auto; padding: 28px clamp(16px, 4vw, 42px) 48px; }
.page-header, .actions, .summary-grid, .breakdown, .content-card > header, footer { display: flex; align-items: center; gap: 14px; }
.page-header, .content-card > header { justify-content: space-between; }.page-header h1 { margin: 5px 0; font-size: clamp(28px, 4vw, 42px); }.page-header p, small, span { color: var(--vy-ink-3); font-weight: 700; }
.actions { flex-wrap: wrap; justify-content: flex-end; }.secondary, .primary { min-height: 42px; padding: 0 15px; border-radius: 12px; display: inline-flex; align-items: center; gap: 8px; font-weight: 900; }.secondary { border: 1px solid var(--vy-line); background: var(--vy-surface); }.primary { border: 0; background: var(--vy-orange); color: #1f1a14; }.primary:disabled { opacity: .55; cursor: not-allowed; }
.error-box, .loading-box { margin-top: 20px; padding: 16px; border-radius: 14px; font-weight: 800; }.error-box { background: #fff0ef; color: #991b1b; }.loading-box { background: var(--vy-surface); }
.summary-grid { align-items: stretch; margin: 22px 0; }.summary-grid article { flex: 1; min-width: 0; padding: 20px; border: 1px solid var(--vy-line); border-radius: 18px; background: var(--vy-surface); }.summary-grid small, .summary-grid strong, .summary-grid span { display: block; }.summary-grid strong { margin: 6px 0; font-size: 26px; }
.content-card { padding: 24px; border: 1px solid var(--vy-line); border-radius: 20px; background: var(--vy-surface); }.content-card h2 { margin: 4px 0 0; }.total-box { margin: 22px 0; padding: 22px; border-radius: 16px; background: #fff3df; }.total-box span, .total-box strong { display: block; }.total-box strong { margin-top: 5px; font-size: 30px; }label { display: grid; gap: 8px; font-weight: 900; }textarea { width: 100%; padding: 12px; border: 1px solid var(--vy-line); border-radius: 12px; font: inherit; resize: vertical; }footer { justify-content: flex-end; margin-top: 22px; }.breakdown { justify-content: space-between; padding: 16px 0; border-bottom: 1px solid var(--vy-line-2); }.breakdown b, .breakdown small { display: block; }.note { margin-top: 18px; padding: 15px; border-radius: 12px; background: var(--vy-surface-2); }.table-wrap { overflow-x: auto; margin-top: 22px; }table { width: 100%; border-collapse: collapse; min-width: 620px; }th, td { padding: 12px; border-bottom: 1px solid var(--vy-line-2); text-align: left; }th { font-size: 11px; text-transform: uppercase; color: var(--vy-ink-3); }
@media (max-width: 700px) { .page-header, .summary-grid { flex-direction: column; align-items: stretch; }.actions { justify-content: stretch; }.actions button { flex: 1; justify-content: center; }.content-card { padding: 17px; } }
</style>
