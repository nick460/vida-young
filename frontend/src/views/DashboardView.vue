<script setup>
import { computed } from "vue";
import { useRouter } from "vue-router";
import { KPIS, TRANSACTIONS } from "../data.js";
import { useAuthStore } from "../stores/authStore.js";
import { VyAvatar, VyBarChart, VyDonut, VyIcon, VyMoney, VySparkline } from "../components/ui.js";

const router = useRouter();
const authStore = useAuthStore();

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

const chartData = [
  { l: "Ene", v: 4.2 },
  { l: "Feb", v: 5.1 },
  { l: "Mar", v: 4.8 },
  { l: "Abr", v: 6.4 },
  { l: "May", v: 7.8 },
  { l: "Jun", v: 8.6 },
  { l: "Jul", v: 9.2 },
  { l: "Ago", v: 10.4 },
  { l: "Sep", v: 11.1 },
  { l: "Oct", v: 12.0 },
  { l: "Nov", v: 11.8 },
  { l: "Dic", v: 12.4 }
];

function navigate(name) {
  router.push({ name });
}
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
            <p>Has crecido 24% este mes. Vas en camino al rango Diamante Élite.</p>
          </div>
          <div class="welcome-actions">
            <button class="vy-btn vy-btn-ghost" type="button">
              <VyIcon name="arrowDn" :size="14" />
              Retirar
            </button>
            <button class="vy-btn vy-btn-primary" type="button">
              <VyIcon name="plus" :size="14" />
              Invitar embajador
            </button>
          </div>
        </section>

        <section class="kpi-grid">
          <article v-for="(kpi, index) in KPIS" :key="kpi.label" class="vy-card kpi-card">
            <header>
              <span>{{ kpi.label }}</span>
              <small class="vy-chip vy-chip-success">
                <VyIcon :name="kpi.up ? 'arrowUp' : 'arrowDn'" :size="10" />
                {{ kpi.delta }}
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
          <article class="vy-card chart-card">
            <header class="card-header">
              <div>
                <h2>Ganancias acumuladas</h2>
                <p>Últimos 12 meses · valor en millones COP</p>
              </div>
              <div class="period-tabs">
                <button class="active" type="button">Año</button>
                <button type="button">6M</button>
                <button type="button">3M</button>
              </div>
            </header>
            <div class="chart-scroll">
              <VyBarChart :data="chartData" :width="640" :height="180" />
            </div>
          </article>

          <article class="vy-card rank-card">
            <div class="rank-orb"></div>
            <div class="rank-content">
              <span class="vy-chip vy-chip-orange">Tu nivel actual</span>
              <h2>Líder Diamante</h2>
              <p>Top 8% de embajadores en Colombia</p>
              <div class="rank-progress">
                <VyDonut :value="68" :size="124" :stroke="11" />
                <div>
                  <strong>Próximo: Diamante Élite</strong>
                  <span>Cierre de mes el 31 de mayo. Faltan <b>$ 1,2M</b> en volumen para alcanzarlo.</span>
                  <button class="vy-btn vy-btn-dark" type="button">Ver requisitos</button>
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
                  <th>Estado</th>
                  <th>Monto</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="transaction in TRANSACTIONS.slice(0, 5)" :key="transaction.id">
                  <td>{{ transaction.id }}</td>
                  <td>{{ transaction.type }}</td>
                  <td>{{ transaction.date }}</td>
                  <td><span class="vy-chip vy-chip-success">● {{ transaction.status }}</span></td>
                  <td><VyMoney :v="transaction.amt" sign /></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </section>
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

.welcome-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
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
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(360px, 1fr);
  gap: 14px;
  margin-bottom: 18px;
}

.chart-card,
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

.period-tabs {
  display: flex;
  gap: 6px;
  background: var(--vy-cream);
  padding: 4px;
  border-radius: 99px;
}

.period-tabs button {
  padding: 6px 14px;
  border-radius: 99px;
  font-size: 12px;
  font-weight: 700;
  color: var(--vy-ink-2);
}

.period-tabs button.active {
  background: var(--vy-surface);
  box-shadow: var(--vy-shadow-sm);
  color: var(--vy-ink);
}

.chart-scroll {
  margin-top: 18px;
  overflow-x: auto;
}

.rank-card {
  background: linear-gradient(140deg, var(--vy-cream) 0%, #fff 60%);
  position: relative;
  overflow: hidden;
}

.rank-orb {
  right: -30px;
  bottom: -30px;
  top: auto;
  width: 160px;
  height: 160px;
  background: rgba(242, 135, 5, 0.18);
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

.rank-progress div {
  font-size: 13px;
}

.rank-progress strong,
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
}
</style>

