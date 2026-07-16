<script setup>
import { computed, onMounted, ref } from "vue";
import {
  ArrowDownToLine,
  BadgeCheck,
  Box,
  Copy,
  Gem,
  RefreshCw,
  WalletCards
} from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import { useAuthStore } from "../stores/authStore.js";

const auth = useAuthStore();
const loading = ref(false);
const error = ref("");
const resumen = ref({
  billetera: null,
  movimientos: [],
  membresias: [],
  cierresMensuales: []
});
const proyeccionActivacion = ref(null);
const movementFilter = ref("TODOS");

const personaId = computed(() => auth.usuario?.persona?.id || "");
const billetera = computed(() => resumen.value.billetera || {});
const movimientos = computed(() => resumen.value.movimientos || []);
const membresias = computed(() => resumen.value.membresias || []);
const cierresMensuales = computed(() => resumen.value.cierresMensuales || []);
const activationPlans = computed(() => proyeccionActivacion.value?.planes || []);
const activeMembership = computed(() =>
  membresias.value.find((membresia) => membresia.estadoMembresia === "ACTIVA")
);
const filteredMovimientos = computed(() => {
  if (movementFilter.value === "TODOS") return movimientos.value;
  return movimientos.value.filter((movimiento) => movimiento.tipo === movementFilter.value);
});
const movementFilters = [
  { id: "TODOS", label: "Todos" },
  { id: "DINERO", label: "Dinero" },
  { id: "PRODUCTOS", label: "Productos" },
  { id: "PV", label: "PV" },
  { id: "QP", label: "QP" },
  { id: "CR", label: "CR" }
];

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function formatDate(value) {
  if (!value) return "Sin fecha";
  return new Date(value).toLocaleDateString("es-BO", {
    year: "numeric",
    month: "short",
    day: "2-digit"
  });
}

function movementAmount(movimiento) {
  const prefix = movimiento.tipo === "DINERO" || movimiento.tipo === "PRODUCTOS" ? "Bs. " : "";
  return `${prefix}${money(movimiento.monto)}`;
}

async function loadWallet() {
  loading.value = true;
  error.value = "";

  try {
    if (!personaId.value) {
      await auth.cargarPerfil();
    }

    if (!personaId.value) {
      error.value = "Tu usuario no tiene una persona asociada.";
      return;
    }

    resumen.value = await apiRequest(`/api/billeteras/persona/${personaId.value}`);
    proyeccionActivacion.value = await apiRequest(`/api/planes-activacion/persona/${personaId.value}/proyeccion`);
  } catch (exception) {
    error.value = exception.message || "No se pudo cargar la billetera.";
  } finally {
    loading.value = false;
  }
}

onMounted(loadWallet);
</script>

<template>
  <div class="vy wallet-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Finanzas</div>
          <h1>Mi cartera</h1>
          <p>Consulta saldos disponibles, puntos de red, membresia y movimientos de tu cuenta.</p>
        </div>
        <button class="vy-btn vy-btn-primary" type="button" :disabled="loading" @click="loadWallet">
          <RefreshCw :size="16" stroke-width="2" />
          Actualizar
        </button>
      </header>

      <section class="wallet-overview">
        <article class="balance-card cash-card">
          <header>
            <span class="card-icon"><WalletCards :size="24" /></span>
            <div>
              <span>Efectivo disponible</span>
              <strong>Bs. {{ money(billetera.saldoDinero) }}</strong>
              <p>Dinero listo para solicitar retiro cuando administracion lo habilite.</p>
            </div>
          </header>
        </article>

        <article class="balance-card product-card">
          <header>
            <span class="card-icon"><Box :size="24" /></span>
            <div>
              <span>Productos canjeables</span>
              <strong>Bs. {{ money(billetera.saldoProductos) }}</strong>
              <p>Saldo disponible para canjear por productos de la tienda.</p>
            </div>
          </header>
        </article>

        <article class="vy-card membership-summary">
          <span class="card-icon"><BadgeCheck :size="22" /></span>
          <div>
            <span>Membresia actual</span>
            <strong>{{ activeMembership?.plan?.nombre || "Sin membresia activa" }}</strong>
            <small>{{ activeMembership ? `Vigente hasta ${formatDate(activeMembership.fechaFin)}` : "Activa un plan para habilitar beneficios." }}</small>
          </div>
        </article>
      </section>

      <section class="points-grid">
        <article class="point-card">
          <span>PV</span>
          <strong>{{ money(billetera.saldoPv) }}</strong>
          <small>Volumen personal acumulado.</small>
        </article>
        <article class="point-card primary">
          <span>QP</span>
          <strong>{{ money(billetera.saldoQp) }}</strong>
          <small>Puntos calificables para rangos.</small>
        </article>
        <article class="point-card">
          <span>CR</span>
          <strong>{{ money(billetera.saldoCr) }}</strong>
          <small>Credito independiente de compras.</small>
        </article>
        <article class="point-card">
          <span>Productos</span>
            <strong>{{ money(billetera.saldoProductos) }}</strong>
          <small>Canjeable en catalogo.</small>
        </article>
      </section>

      <div v-if="error" class="error-box">{{ error }}</div>
      <div v-if="loading" class="loading-box">Cargando billetera...</div>

      <section class="vy-card history-card">
        <header class="history-header">
          <div>
            <h2>Movimientos de billetera</h2>
            <p>Entradas, salidas y ajustes de dinero, productos, PV, QP y CR.</p>
          </div>
          <div class="filters">
            <button
              v-for="filter in movementFilters"
              :key="filter.id"
              :class="{ active: movementFilter === filter.id }"
              type="button"
              @click="movementFilter = filter.id"
            >
              {{ filter.label }}
            </button>
          </div>
        </header>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Concepto</th>
                <th>Fecha</th>
                <th>Tipo</th>
                <th>Monto</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="movimiento in filteredMovimientos" :key="movimiento.id">
                <td>#{{ movimiento.id }}</td>
                <td>{{ movimiento.concepto }}</td>
                <td>{{ formatDate(movimiento.fechaRegistro) }}</td>
                <td><span class="vy-chip vy-chip-success">{{ movimiento.tipo }}</span></td>
                <td>{{ movementAmount(movimiento) }}</td>
              </tr>
              <tr v-if="!filteredMovimientos.length && !loading">
                <td colspan="5">No hay movimientos registrados.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="vy-card history-card membership-card">
        <header class="history-header">
          <div>
            <h2>Proyeccion de activacion</h2>
            <p>Con tu PV mensual actual puedes ver cuanto ganarias por cada producto comprado en tu red.</p>
          </div>
          <span class="pv-badge">PV actual {{ money(proyeccionActivacion?.pvMensualActual) }}</span>
        </header>

        <div class="activation-projection-grid">
          <article v-for="plan in activationPlans" :key="plan.planActivacionId" class="activation-projection-card" :class="{ active: plan.activable }">
            <header>
              <span>{{ plan.activable ? "Activable" : `Faltan ${money(plan.pvFaltante)} PV` }}</span>
              <h3>{{ plan.nombre }}</h3>
              <p>{{ money(plan.pvMinimoMensual) }} PV · {{ plan.nivelesAlcance }} niveles</p>
            </header>
            <strong>Bs. {{ money(plan.gananciaMaximaPorProducto) }}</strong>
            <small>Potencial por producto si toda la red alcanzada compra una unidad.</small>
            <div class="projection-levels">
              <div v-for="nivel in plan.niveles" :key="nivel.numeroNivel">
                <span>Nivel {{ nivel.numeroNivel }}</span>
                <b>{{ nivel.personasEnNivel }} pers. · Bs. {{ money(nivel.montoPorProducto) }}</b>
              </div>
            </div>
          </article>
          <p v-if="!activationPlans.length && !loading" class="empty-row">No hay planes de activacion configurados.</p>
        </div>
      </section>

      <section class="vy-card history-card membership-card">
        <header class="history-header">
          <div>
            <h2>Historico de membresias</h2>
            <p>Afiliacion inicial y activaciones mensuales por plan.</p>
          </div>
        </header>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Tipo</th>
                <th>Plan</th>
                <th>Inicio</th>
                <th>Fin</th>
                <th>Estado</th>
                <th>QP</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="membresia in membresias" :key="membresia.id">
                <td>{{ membresia.tipo }}</td>
                <td>{{ membresia.plan?.nombre || "Plan" }}</td>
                <td>{{ formatDate(membresia.fechaInicio) }}</td>
                <td>{{ formatDate(membresia.fechaFin) }}</td>
                <td><span class="vy-chip" :class="membresia.estadoMembresia === 'ACTIVA' ? 'vy-chip-success' : 'vy-chip-orange'">{{ membresia.estadoMembresia }}</span></td>
                <td>{{ money(membresia.qpPlan) }}</td>
              </tr>
              <tr v-if="!membresias.length && !loading">
                <td colspan="6">No hay membresias registradas.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="vy-card history-card membership-card">
        <header class="history-header">
          <div>
            <h2>Cierres mensuales</h2>
            <p>Historico de dinero enviado a planilla y PV/QP/CR reiniciados por mes.</p>
          </div>
        </header>

        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Periodo</th>
                <th>Fecha cierre</th>
                <th>Dinero planilla</th>
                <th>PV</th>
                <th>QP</th>
                <th>CR</th>
                <th>Productos</th>
                <th>Rango</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="cierre in cierresMensuales" :key="cierre.id">
                <td>{{ cierre.periodo }}</td>
                <td>{{ formatDate(cierre.fechaCierre) }}</td>
                <td>Bs. {{ money(cierre.saldoDinero) }}</td>
                <td>{{ money(cierre.saldoPv) }}</td>
                <td>{{ money(cierre.saldoQp) }}</td>
                <td>{{ money(cierre.saldoCr) }}</td>
                <td>{{ money(cierre.saldoProductos) }}</td>
                <td>
                  <span class="rank-pill">{{ cierre.rangoNombre || cierre.rango?.nombre || "Sin rango" }}</span>
                </td>
                <td><span class="vy-chip vy-chip-orange">{{ cierre.estadoPlanilla }}</span></td>
              </tr>
              <tr v-if="!cierresMensuales.length && !loading">
                <td colspan="9">No hay cierres mensuales registrados.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace {
  padding: 28px 32px 40px;
  min-width: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 30px;
  font-weight: 800;
  margin-top: 8px;
}

.page-header p {
  font-size: 14px;
  color: var(--vy-ink-2);
  margin-top: 4px;
}

.page-header .vy-btn,
.methods-card button {
  border-radius: 12px;
  font-weight: 800;
}

.wallet-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.balance-card {
  padding: 22px;
  border-radius: 18px;
  border: 1px solid var(--vy-line);
  background: var(--vy-ink);
  color: #fff;
  box-shadow: var(--vy-shadow-sm);
}

.balance-card header {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.card-icon {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: rgba(242, 135, 5, 0.18);
  color: var(--vy-orange);
}

.balance-card header span:not(.card-icon),
.membership-summary > div > span {
  display: block;
  font-size: 11px;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.68);
  font-weight: 900;
}

.balance-card header strong {
  display: block;
  margin-top: 7px;
  font-size: 30px;
  font-weight: 900;
}

.balance-card header p {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.4;
  color: rgba(255, 255, 255, 0.72);
  font-weight: 800;
}

.product-card {
  background: #fffaf0;
  color: var(--vy-ink);
}

.product-card .card-icon {
  background: rgba(63, 143, 92, 0.12);
  color: var(--vy-success);
}

.product-card header span:not(.card-icon),
.product-card header p {
  color: var(--vy-ink-3);
}

.product-card header strong {
  color: var(--vy-ink);
}

.membership-summary {
  padding: 20px;
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.membership-summary .card-icon {
  background: var(--vy-cream);
  color: var(--vy-orange-deep);
}

.membership-summary > div > span {
  color: var(--vy-ink-3);
}

.membership-summary strong {
  display: block;
  margin-top: 7px;
  color: var(--vy-ink);
  font-size: 18px;
  font-weight: 900;
}

.membership-summary small {
  display: block;
  margin-top: 5px;
  color: var(--vy-ink-2);
  font-size: 12px;
  font-weight: 800;
  line-height: 1.35;
}

.points-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.point-card {
  padding: 16px;
  border-radius: 14px;
  border: 1px solid var(--vy-line);
  background: var(--vy-surface);
  box-shadow: var(--vy-shadow-sm);
}

.point-card.primary {
  border-color: var(--vy-orange);
  background: rgba(242, 135, 5, 0.04);
}

.point-card span {
  display: block;
  font-size: 11px;
  text-transform: uppercase;
  color: var(--vy-ink-3);
  font-weight: 900;
}

.point-card strong {
  display: block;
  margin-top: 7px;
  color: var(--vy-ink);
  font-size: 24px;
  font-weight: 900;
}

.point-card small {
  display: block;
  margin-top: 4px;
  color: var(--vy-ink-3);
  font-size: 12px;
  font-weight: 800;
  line-height: 1.35;
}

.history-card {
  padding: 22px;
}

.membership-card {
  margin-top: 18px;
}

.pv-badge {
  padding: 8px 12px;
  border-radius: 999px;
  background: var(--vy-cream);
  color: #6b4a12;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.rank-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(242, 135, 5, 0.1);
  color: var(--vy-orange-deep);
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.activation-projection-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.activation-projection-card {
  padding: 16px;
  border: 1px solid var(--vy-line);
  border-radius: 14px;
  background: var(--vy-surface-2);
}

.activation-projection-card.active {
  border-color: var(--vy-orange);
  background: rgba(242, 135, 5, 0.07);
}

.activation-projection-card header span {
  display: inline-flex;
  margin-bottom: 8px;
  padding: 4px 9px;
  border-radius: 999px;
  background: #fff;
  color: var(--vy-orange-deep);
  font-size: 11px;
  font-weight: 900;
}

.activation-projection-card h3 {
  font-size: 16px;
  font-weight: 900;
}

.activation-projection-card p,
.activation-projection-card small {
  display: block;
  margin-top: 4px;
  color: var(--vy-ink-3);
  font-size: 12px;
  font-weight: 700;
}

.activation-projection-card > strong {
  display: block;
  margin-top: 14px;
  color: var(--vy-orange-deep);
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 900;
}

.projection-levels {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.projection-levels div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 10px;
  border-radius: 10px;
  background: #fff;
  font-size: 12px;
}

.projection-levels b {
  color: var(--vy-ink);
  white-space: nowrap;
}

.empty-row {
  color: var(--vy-ink-3);
  font-size: 13px;
  font-weight: 800;
}

.error-box,
.loading-box {
  padding: 14px 16px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 800;
  margin-bottom: 14px;
}

.error-box {
  color: var(--vy-danger);
  background: rgba(196, 69, 42, 0.1);
}

.loading-box {
  color: var(--vy-ink-2);
  background: var(--vy-surface-2);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  margin-bottom: 14px;
}

.history-header h2 {
  font-size: 16px;
  font-weight: 800;
}

.history-header p {
  font-size: 12px;
  color: var(--vy-ink-3);
  margin-top: 2px;
}

.filters {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filters button {
  padding: 6px 11px;
  border-radius: 999px;
  background: var(--vy-gray);
  color: var(--vy-ink-2);
  font-size: 12px;
  font-weight: 800;
}

.filters button.active {
  background: var(--vy-cream);
  color: #6b4a12;
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

thead tr {
  text-align: left;
  color: var(--vy-ink-3);
  font-weight: 600;
  font-size: 11px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

th,
td {
  padding: 14px 8px;
}

th:first-child,
td:first-child {
  padding-left: 0;
}

th:last-child,
td:last-child {
  text-align: right;
  padding-right: 0;
}

tbody tr {
  border-top: 1px solid var(--vy-line-2);
}

td:first-child {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--vy-ink-3);
}

td:nth-child(2) {
  font-weight: 700;
}

td:nth-child(3) {
  color: var(--vy-ink-2);
}

@media (max-width: 1040px) {
  .wallet-overview,
  .points-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .activation-projection-grid {
    grid-template-columns: 1fr;
  }

  .workspace {
    padding: 24px 20px 32px;
  }
}

@media (max-width: 720px) {
  .page-header,
  .history-header,
  .wallet-overview,
  .points-grid {
    align-items: stretch;
    flex-direction: column;
    grid-template-columns: 1fr;
  }

  table {
    min-width: 760px;
  }
}
</style>
