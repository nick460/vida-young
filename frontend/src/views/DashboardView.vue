<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { apiRequest } from "../services/api.js";
import { useAuthStore } from "../stores/authStore.js";
import { VyAvatar, VyDonut, VyIcon, VySparkline } from "../components/ui.js";

const router = useRouter();
const authStore = useAuthStore();
const loadingSummary = ref(false);
const summaryError = ref("");
const walletSummary = ref({
  billetera: null
});
const rewards = ref([]);
const rangos = ref([]);
const showRanksModal = ref(false);

const user = computed(() => ({
  name: authStore.usuario?.username || "Administrador Vidayoung",
  short: authStore.usuario?.username || "Admin",
  avatar: (authStore.usuario?.username || "AV").slice(0, 2).toUpperCase(),
  level: authStore.usuario?.roles?.[0] || "ADMIN"
}));

const sparks = [
  [320, 340, 360, 355, 380, 410, 420, 460, 472, 480, 510, 528],
  [80, 95, 90, 110, 120, 145, 160, 155, 168, 175, 180, 195],
  [12, 18, 16, 24, 22, 30, 36, 32, 40, 44, 46, 48],
  [80, 92, 100, 108, 115, 122, 128, 130, 134, 136, 137, 138]
];

const personaId = computed(() => authStore.usuario?.persona?.id || "");
const wallet = computed(() => walletSummary.value.billetera || {});
const walletMovements = computed(() => walletSummary.value.movimientos || []);
const recentMovements = computed(() => walletMovements.value.slice(0, 5));
const currentQp = computed(() => Number(wallet.value.saldoQp || 0));
const redeemableProducts = computed(() =>
  rewards.value
    .filter((reward) => reward.cobrable !== false)
    .reduce((sum, reward) => sum + Number(reward.valorProductos || 0), 0)
);
const orderedRanks = computed(() =>
  [...rangos.value].sort((left, right) => Number(left.qpMinimo || 0) - Number(right.qpMinimo || 0))
);
const currentRank = computed(() =>
  [...orderedRanks.value].reverse().find((rango) => currentQp.value >= Number(rango.qpMinimo || 0)) || null
);
const nextRank = computed(() =>
  orderedRanks.value.find((rango) => currentQp.value < Number(rango.qpMinimo || 0)) || null
);
const rankProgress = computed(() => {
  if (!orderedRanks.value.length) {
    return 0;
  }

  if (!nextRank.value) {
    return currentRank.value ? 100 : 0;
  }

  const target = Math.max(1, Number(nextRank.value.qpMinimo || 0));
  return Math.min(100, Math.max(0, Math.round((currentQp.value / target) * 100)));
});
const missingQp = computed(() =>
  nextRank.value ? Math.max(0, Number(nextRank.value.qpMinimo || 0) - currentQp.value) : 0
);
const rankSummary = computed(() => {
  if (!orderedRanks.value.length) {
    return "Configura rangos para medir el avance por QP.";
  }

  if (!currentRank.value) {
    return "Aun no alcanzas el primer rango configurado.";
  }

  return `Rango alcanzado con ${money(currentQp.value)} QP acumulados.`;
});
const welcomeRankMessage = computed(() => {
  if (!orderedRanks.value.length) {
    return "Configura rangos para calcular el avance por QP.";
  }

  if (nextRank.value) {
    return `Vas en camino al rango ${nextRank.value.nombre}.`;
  }

  return "Estas en el rango mas alto configurado.";
});

const dashboardKpis = computed(() => [
  {
    label: "Efectivo por cobrar",
    value: `Bs. ${money(wallet.value.saldoDinero)}`,
    hint: "Disponible en tu billetera",
    state: loadingSummary.value ? "Actualizando" : "Disponible"
  },
  {
    label: "Canjeable en productos",
    value: `Bs. ${money(redeemableProducts.value)}`,
    hint: "Valor acumulado para productos",
    state: loadingSummary.value ? "Actualizando" : "Canjeable"
  },
  {
    label: "PV acumulados",
    value: money(wallet.value.saldoPv),
    hint: "Puntos de volumen del periodo",
    state: loadingSummary.value ? "Actualizando" : "PV"
  },
  {
    label: "QP acumulados",
    value: money(wallet.value.saldoQp),
    hint: "Puntos calificables activos",
    state: loadingSummary.value ? "Actualizando" : "QP"
  }
]);

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

function formatMovementAmount(movement) {
  const prefix = movement.tipo === "DINERO" ? "Bs. " : "";
  return `${prefix}${money(movement.monto)}`;
}

function rankProgressFor(rango) {
  const requiredQp = Number(rango?.qpMinimo || 0);

  if (requiredQp <= 0) {
    return 100;
  }

  return Math.min(100, Math.max(0, Math.round((currentQp.value / requiredQp) * 100)));
}

async function loadDashboardSummary() {
  loadingSummary.value = true;
  summaryError.value = "";

  try {
    if (!personaId.value) {
      await authStore.cargarPerfil();
    }

    if (!personaId.value) {
      summaryError.value = "Tu usuario no tiene una persona asociada.";
      return;
    }

    const [walletResponse, rewardsResponse, rangosResponse] = await Promise.all([
      apiRequest(`/api/billeteras/persona/${personaId.value}`),
      apiRequest(`/api/recompensas/persona/${personaId.value}`),
      apiRequest("/api/rangos")
    ]);

    walletSummary.value = walletResponse || { billetera: null };
    rewards.value = Array.isArray(rewardsResponse) ? rewardsResponse : [];
    rangos.value = Array.isArray(rangosResponse) ? rangosResponse : [];
  } catch (exception) {
    summaryError.value = exception.message || "No se pudo cargar el resumen financiero.";
  } finally {
    loadingSummary.value = false;
  }
}

function navigate(name) {
  router.push({ name });
}

onMounted(loadDashboardSummary);
</script>

<template>
  <div class="vy dashboard-view">
    <section class="workspace">
      <header class="topbar">
        <div class="search-box">
          <VyIcon name="search" :size="16" stroke="var(--vy-ink-3)" />
          <span>Buscar productos, miembros, transacciones...</span>
          <kbd>Ctrl K</kbd>
        </div>

        <button class="vy-btn vy-btn-ghost topbar-button" type="button">
          <VyIcon name="cog" :size="16" />
          Ajustes
        </button>
        <button class="icon-button" type="button" aria-label="Notificaciones">
          <VyIcon name="bell" :size="16" />
          <span></span>
        </button>
        <button class="avatar-button" type="button" title="Mi cuenta">
          <VyAvatar :name="user.avatar" :size="40" bg="var(--vy-orange)" color="#fff" />
        </button>
      </header>

      <main class="dashboard-content">
        <section class="welcome-row">
          <div>
            <div class="vy-eyebrow">Hola {{ user.short }}, sábado 9 de mayo</div>
            <h1>Tu bienestar financiero, hoy</h1>
            <p>{{ welcomeRankMessage }}</p>
          </div>
        </section>

        <p v-if="summaryError" class="summary-error">{{ summaryError }}</p>

        <section class="kpi-grid">
          <article v-for="(kpi, index) in dashboardKpis" :key="kpi.label" class="vy-card kpi-card">
            <header>
              <span>{{ kpi.label }}</span>
              <small class="vy-chip vy-chip-success">
                {{ kpi.state }}
              </small>
            </header>
            <strong>{{ kpi.value }}</strong>
            <footer>
              <span>{{ kpi.hint }}</span>
              <VySparkline :data="sparks[index]" :width="80" :height="28" />
            </footer>
          </article>
        </section>

        <section class="dashboard-grid">
          <article class="vy-card rank-card">
            <div class="rank-content">
              <span class="vy-chip vy-chip-orange">Tu rango actual</span>
              <h2>{{ currentRank?.nombre || "Sin rango" }}</h2>
              <p>{{ rankSummary }}</p>
              <div class="rank-progress">
                <div class="rank-donut">
                  <VyDonut :value="rankProgress" :size="136" :stroke="12" />
                </div>
                <div>
                  <strong>{{ nextRank ? `Proximo: ${nextRank.nombre}` : "Rango maximo alcanzado" }}</strong>
                  <span v-if="nextRank">
                    Tienes <b>{{ money(currentQp) }} QP</b>. Faltan <b>{{ money(missingQp) }} QP</b> para llegar a {{ money(nextRank.qpMinimo) }} QP.
                  </span>
                  <span v-else-if="currentRank">
                    Ya alcanzaste el rango mas alto registrado con <b>{{ money(currentQp) }} QP</b>.
                  </span>
                  <span v-else>No hay rangos configurados para calcular el avance.</span>
                  <button class="vy-btn vy-btn-dark" type="button" @click="showRanksModal = true">Ver rangos</button>
                </div>
              </div>
            </div>
          </article>
        </section>

        <section class="vy-card transactions-card">
          <header class="card-header">
            <div>
              <h2>Movimientos recientes</h2>
              <p>Últimas transacciones de tu cuenta</p>
            </div>
            <button type="button" @click="navigate('wallet')">
              Ver todo
              <VyIcon name="arrowR" :size="14" />
            </button>
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
                <tr v-for="movement in recentMovements" :key="movement.id">
                  <td>#{{ movement.id }}</td>
                  <td>{{ movement.concepto }}</td>
                  <td>{{ formatDate(movement.fechaRegistro) }}</td>
                  <td><span class="vy-chip vy-chip-success">{{ movement.tipo }}</span></td>
                  <td>{{ formatMovementAmount(movement) }}</td>
                </tr>
                <tr v-if="!recentMovements.length && !loadingSummary">
                  <td colspan="5">No hay movimientos registrados.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </section>

    <Teleport to="body">
      <div v-if="showRanksModal" class="rank-modal-backdrop" @click.self="showRanksModal = false">
        <section class="rank-modal" role="dialog" aria-modal="true" aria-labelledby="rank-modal-title">
          <header>
            <div>
              <div class="vy-eyebrow">Rangos Vidayoung</div>
              <h2 id="rank-modal-title">Escala de rangos</h2>
              <p>Avance calculado con tus QP acumulados: <b>{{ money(currentQp) }} QP</b>.</p>
            </div>
            <button type="button" aria-label="Cerrar" @click="showRanksModal = false">X</button>
          </header>

          <div v-if="orderedRanks.length" class="rank-modal-list">
            <article
              v-for="rango in orderedRanks"
              :key="rango.id"
              class="rank-modal-row"
              :class="{
                reached: currentQp >= Number(rango.qpMinimo || 0),
                current: currentRank?.id === rango.id
              }"
            >
              <div>
                <strong>{{ rango.nombre }}</strong>
                <span>{{ money(rango.qpMinimo) }} QP requeridos</span>
              </div>
              <div class="rank-modal-bar">
                <span
                  :style="{
                    width: `${rankProgressFor(rango)}%`
                  }"
                ></span>
              </div>
              <em v-if="currentRank?.id === rango.id">Actual</em>
              <em v-else-if="currentQp >= Number(rango.qpMinimo || 0)">Alcanzado</em>
              <em v-else class="pending">{{ rankProgressFor(rango) }}%</em>
            </article>
          </div>
          <p v-else class="empty-ranks">No hay rangos configurados.</p>
        </section>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.workspace {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 32px;
  border-bottom: 1px solid var(--vy-line-2);
  background: rgba(251, 248, 240, 0.85);
  backdrop-filter: blur(8px);
  position: sticky;
  top: 0;
  z-index: 10;
}

.search-box {
  flex: 1;
  max-width: 460px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: var(--vy-surface);
  border-radius: 12px;
  border: 1px solid var(--vy-line);
}

.search-box span {
  flex: 1;
  font-size: 13px;
  color: var(--vy-ink-3);
}

.search-box kbd {
  font-size: 11px;
  font-family: var(--font-mono);
  color: var(--vy-ink-3);
  padding: 2px 6px;
  border: 1px solid var(--vy-line);
  border-radius: 5px;
  background: #fff;
}

.topbar-button {
  padding: 10px 14px;
}

.icon-button {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--vy-surface);
  border: 1px solid var(--vy-line);
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-button span {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 8px;
  height: 8px;
  background: var(--vy-orange);
  border-radius: 50%;
  border: 2px solid var(--vy-surface);
}

.avatar-button {
  border-radius: 50%;
}

.dashboard-content {
  padding: 28px 32px 40px;
  flex: 1;
}

.welcome-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  margin-bottom: 24px;
}

.welcome-row h1 {
  font-size: 30px;
  font-weight: 800;
  margin-top: 8px;
}

.welcome-row p {
  font-size: 14px;
  color: var(--vy-ink-2);
  margin-top: 4px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.summary-error {
  padding: 14px 16px;
  margin-bottom: 14px;
  border-radius: 8px;
  background: rgba(196, 69, 42, 0.1);
  color: var(--vy-danger);
  font-size: 13px;
  font-weight: 800;
}

.kpi-card {
  padding: 18px;
}

.kpi-card header,
.kpi-card footer,
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.kpi-card header > span {
  font-size: 12px;
  color: var(--vy-ink-3);
  font-weight: 600;
}

.kpi-card > strong {
  display: block;
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 800;
  line-height: 1.05;
  margin-top: 10px;
}

.kpi-card footer {
  align-items: flex-end;
  margin-top: 10px;
}

.kpi-card footer > span {
  font-size: 11px;
  color: var(--vy-ink-3);
}

.dashboard-grid {
  margin-bottom: 18px;
}

.rank-card,
.transactions-card {
  padding: 22px;
}

.card-header h2 {
  font-size: 16px;
  font-weight: 800;
}

.card-header p {
  font-size: 12px;
  color: var(--vy-ink-3);
  margin-top: 2px;
}

.rank-card {
  background: linear-gradient(140deg, var(--vy-cream) 0%, #fff 60%);
  position: relative;
  overflow: hidden;
}

.rank-content h2 {
  font-size: 24px;
  font-weight: 800;
  margin-top: 14px;
}

.rank-content > p {
  font-size: 13px;
  color: var(--vy-ink-2);
  margin-top: 4px;
}

.rank-progress {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 18px;
}

.rank-donut {
  position: relative;
  width: 136px;
  height: 136px;
  flex: 0 0 auto;
}

.rank-donut :deep(svg) {
  display: block;
  filter: drop-shadow(0 10px 18px rgba(242, 135, 5, 0.16));
}

.rank-donut :deep(text) {
  color: var(--vy-ink);
  font-size: 26px;
  font-weight: 900;
  letter-spacing: 0;
}

.rank-progress div {
  font-size: 13px;
}

.rank-progress > div:not(.rank-donut) > strong,
.rank-progress span {
  display: block;
}

.rank-progress span {
  color: var(--vy-ink-2);
  line-height: 1.45;
  font-size: 12px;
  margin-top: 6px;
}

.rank-progress button {
  margin-top: 14px;
  padding: 8px 14px;
  font-size: 12px;
}

.transactions-card .card-header button {
  font-size: 13px;
  font-weight: 700;
  color: var(--vy-orange-deep);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.rank-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(31, 26, 20, 0.48);
}

.rank-modal {
  width: min(680px, 100%);
  max-height: min(760px, calc(100vh - 48px));
  overflow: auto;
  padding: 22px;
  border-radius: 10px;
  background: var(--vy-surface);
  border: 1px solid var(--vy-line);
  box-shadow: 0 24px 70px rgba(31, 26, 20, 0.28);
}

.rank-modal header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 16px;
}

.rank-modal header h2 {
  font-size: 22px;
  font-weight: 900;
  margin-top: 6px;
}

.rank-modal header p {
  font-size: 13px;
  line-height: 1.45;
  color: var(--vy-ink-2);
  margin-top: 6px;
}

.rank-modal header button {
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--vy-surface-2);
  border: 1px solid var(--vy-line);
  color: var(--vy-ink-2);
  font-size: 14px;
  font-weight: 900;
}

.rank-modal-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rank-modal-row {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(120px, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 13px;
  border-radius: 10px;
  border: 1px solid var(--vy-line-2);
  background: #fff;
}

.rank-modal-row.reached {
  border-color: rgba(242, 135, 5, 0.42);
  background: #fffaf0;
}

.rank-modal-row.current {
  box-shadow: 0 0 0 2px rgba(242, 135, 5, 0.2);
}

.rank-modal-row strong,
.rank-modal-row span {
  display: block;
}

.rank-modal-row strong {
  font-size: 14px;
  font-weight: 900;
}

.rank-modal-row span {
  margin-top: 3px;
  color: var(--vy-ink-3);
  font-size: 12px;
  font-weight: 800;
}

.rank-modal-bar {
  height: 8px;
  border-radius: 99px;
  background: var(--vy-line-2);
  overflow: hidden;
}

.rank-modal-bar span {
  display: block;
  min-width: 6px;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--vy-orange), var(--vy-orange-deep));
}

.rank-modal-row em {
  padding: 5px 9px;
  border-radius: 99px;
  background: var(--vy-orange);
  color: #fff;
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
  white-space: nowrap;
}

.rank-modal-row em.pending {
  background: var(--vy-surface-2);
  color: var(--vy-ink-2);
}

.empty-ranks {
  padding: 14px;
  border-radius: 8px;
  background: var(--vy-surface-2);
  color: var(--vy-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.table-wrap {
  overflow-x: auto;
  margin-top: 14px;
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

@media (max-width: 1180px) {
  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 860px) {
  .topbar {
    flex-wrap: wrap;
    padding: 16px 20px;
  }

  .search-box {
    order: 2;
    max-width: none;
    flex-basis: 100%;
  }

  .dashboard-content {
    padding: 24px 20px 32px;
  }

  .welcome-row {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 620px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }

  .rank-progress {
    align-items: flex-start;
    flex-direction: column;
  }

  .rank-modal-backdrop {
    align-items: flex-end;
    padding: 12px;
  }

  .rank-modal {
    max-height: calc(100vh - 24px);
    padding: 18px;
  }

  .rank-modal-row {
    grid-template-columns: 1fr;
  }
}
</style>
