<script setup>
import { computed, onMounted, ref } from "vue";
import {
  ArrowDownToLine,
  Copy
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

const personaId = computed(() => auth.usuario?.persona?.id || "");
const billetera = computed(() => resumen.value.billetera || {});
const movimientos = computed(() => resumen.value.movimientos || []);
const membresias = computed(() => resumen.value.membresias || []);
const cierresMensuales = computed(() => resumen.value.cierresMensuales || []);
const activationPlans = computed(() => proyeccionActivacion.value?.planes || []);

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
          <h1>Billetera y membresias</h1>
          <p>Consulta dinero disponible, PV, QP e historial mensual de membresias.</p>
        </div>
        <button class="vy-btn vy-btn-primary" type="button">
          <ArrowDownToLine :size="16" stroke-width="2" />
          Retirar fondos
        </button>
      </header>

      <section class="wallet-grid">
        <article class="balance-card">
          <div class="balance-orb"></div>
          <header>
            <div>
              <span>Saldo disponible</span>
              <strong>Bs. {{ money(billetera.saldoDinero) }}</strong>
              <p>Dinero normal acumulado en la billetera.</p>
            </div>
            <b>VY</b>
          </header>
          <footer>
            <button class="vy-btn vy-btn-primary" type="button">
              <ArrowDownToLine :size="15" />
              Retirar fondos
            </button>
            <button class="vy-btn ghost-dark" type="button">
              <Copy :size="15" />
              Actualizado
            </button>
          </footer>
        </article>

        <article class="vy-card points-card">
          <div class="point-row">
            <span>PV</span>
            <strong>{{ money(billetera.saldoPv) }}</strong>
            <small>Puntos de volumen acumulados.</small>
          </div>
          <div class="point-row primary">
            <span>QP</span>
            <strong>{{ money(billetera.saldoQp) }}</strong>
            <small>Puntos calificables de planes y activaciones.</small>
          </div>
        </article>
      </section>

      <div v-if="error" class="error-box">{{ error }}</div>
      <div v-if="loading" class="loading-box">Cargando billetera...</div>

      <section class="vy-card history-card">
        <header class="history-header">
          <div>
            <h2>Movimientos de billetera</h2>
            <p>Dinero, PV y QP registrados en tu cuenta.</p>
          </div>
          <div class="filters">
            <button class="active" type="button">Todos</button>
            <button type="button">Dinero</button>
            <button type="button">PV</button>
            <button type="button">QP</button>
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
              <tr v-for="movimiento in movimientos" :key="movimiento.id">
                <td>#{{ movimiento.id }}</td>
                <td>{{ movimiento.concepto }}</td>
                <td>{{ formatDate(movimiento.fechaRegistro) }}</td>
                <td><span class="vy-chip vy-chip-success">{{ movimiento.tipo }}</span></td>
                <td>{{ movimiento.tipo === "DINERO" ? "Bs. " : "" }}{{ money(movimiento.monto) }}</td>
              </tr>
              <tr v-if="!movimientos.length && !loading">
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
            <p>Historico de dinero enviado a planilla y PV/QP reiniciados por mes.</p>
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
                <td>
                  <span class="rank-pill">{{ cierre.rangoNombre || cierre.rango?.nombre || "Sin rango" }}</span>
                </td>
                <td><span class="vy-chip vy-chip-orange">{{ cierre.estadoPlanilla }}</span></td>
              </tr>
              <tr v-if="!cierresMensuales.length && !loading">
                <td colspan="7">No hay cierres mensuales registrados.</td>
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
.balance-card .vy-btn,
.methods-card button {
  border-radius: 12px;
  font-weight: 800;
}

.wallet-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 1fr);
  gap: 14px;
  margin-bottom: 18px;
}

.balance-card {
  padding: 24px;
  border-radius: var(--r-lg);
  background: linear-gradient(135deg, var(--vy-ink) 0%, #2d2418 100%);
  color: #fff;
  position: relative;
  overflow: hidden;
}

.balance-orb {
  position: absolute;
  right: -40px;
  top: -40px;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(242, 135, 5, 0.4), transparent 70%);
}

.balance-card header,
.balance-card footer {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
}

.balance-card header span {
  display: block;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.6);
  font-weight: 700;
  margin-bottom: 10px;
}

.balance-card header strong {
  display: block;
  font-family: var(--font-display);
  font-size: 40px;
  font-weight: 800;
  line-height: 1;
}

.balance-card header p {
  margin-top: 10px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.balance-card header b {
  font-family: var(--font-display);
  font-weight: 800;
}

.balance-card footer {
  justify-content: flex-start;
  margin-top: 28px;
}

.ghost-dark {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.points-card {
  padding: 22px;
}

.point-row {
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--vy-line);
  margin-bottom: 10px;
}

.point-row.primary {
  border: 2px solid var(--vy-orange);
  background: rgba(242, 135, 5, 0.04);
}

.point-row span {
  display: block;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--vy-ink-3);
  font-weight: 800;
}

.point-row strong {
  display: block;
  margin-top: 8px;
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 800;
}

.point-row small {
  display: block;
  margin-top: 4px;
  color: var(--vy-orange-deep);
  font-size: 12px;
  font-weight: 800;
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
  .wallet-grid {
    grid-template-columns: 1fr;
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
  .balance-card header,
  .balance-card footer {
    align-items: stretch;
    flex-direction: column;
  }

  table {
    min-width: 760px;
  }
}
</style>
