<script setup>
import { computed, onMounted, ref } from "vue";
import { Building2, RefreshCw, TrendingDown, TrendingUp, WalletCards } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";

const loading = ref(false);
const error = ref("");
const cartera = ref(null);
const movimientos = ref([]);

const ingresos = computed(() =>
  movimientos.value
    .filter((movimiento) => movimiento.tipo === "INGRESO")
    .reduce((sum, movimiento) => sum + Number(movimiento.monto || 0), 0)
);

const egresos = computed(() =>
  movimientos.value
    .filter((movimiento) => movimiento.tipo === "EGRESO")
    .reduce((sum, movimiento) => sum + Math.abs(Number(movimiento.monto || 0)), 0)
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

async function loadCompanyWallet() {
  loading.value = true;
  error.value = "";

  try {
    const [carteraData, movimientosData] = await Promise.all([
      apiRequest("/api/cartera-empresa"),
      apiRequest("/api/cartera-empresa/movimientos")
    ]);
    cartera.value = carteraData;
    movimientos.value = Array.isArray(movimientosData) ? movimientosData : [];
  } catch (exception) {
    error.value = exception.message || "No se pudo cargar la cartera de la empresa.";
  } finally {
    loading.value = false;
  }
}

onMounted(loadCompanyWallet);
</script>

<template>
  <div class="vy company-wallet-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Administracion</div>
          <h1>Caja empresa</h1>
          <p>Dinero real recibido por planes, ventas internas y ventas publicas validadas.</p>
        </div>
        <button class="vy-btn vy-btn-primary" type="button" :disabled="loading" @click="loadCompanyWallet">
          <RefreshCw :size="16" /> Actualizar
        </button>
      </header>

      <div v-if="error" class="error-box">{{ error }}</div>
      <div v-if="loading" class="loading-box">Cargando caja empresa...</div>

      <section class="summary-grid">
        <article class="balance-card">
          <div class="balance-icon"><Building2 :size="26" /></div>
          <span>Saldo actual en caja</span>
          <strong>Bs. {{ money(cartera?.saldoActual) }}</strong>
          <p>{{ cartera?.nombre || "Caja principal de la empresa" }}</p>
        </article>

        <article class="metric-card">
          <span class="metric-icon income"><TrendingUp :size="20" /></span>
          <div>
            <small>Ingresos registrados</small>
            <strong>Bs. {{ money(ingresos) }}</strong>
          </div>
        </article>

        <article class="metric-card">
          <span class="metric-icon outcome"><TrendingDown :size="20" /></span>
          <div>
            <small>Egresos registrados</small>
            <strong>Bs. {{ money(egresos) }}</strong>
          </div>
        </article>

        <article class="metric-card">
          <span class="metric-icon"><WalletCards :size="20" /></span>
          <div>
            <small>Movimientos</small>
            <strong>{{ movimientos.length }}</strong>
          </div>
        </article>
      </section>

      <section class="vy-card movements-card">
        <header class="section-header">
          <div>
            <h2>Movimientos de caja</h2>
            <p>Auditoria de ingresos y egresos de la cartera principal.</p>
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
              <tr v-for="movimiento in movimientos" :key="movimiento.id">
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
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace { padding: 28px 32px 40px; min-width: 0; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 20px; }
.page-header h1 { margin-top: 8px; font-size: 30px; font-weight: 900; }
.page-header p { margin-top: 5px; color: var(--vy-ink-2); font-size: 14px; }
.summary-grid { display: grid; grid-template-columns: minmax(280px, 1.4fr) repeat(3, minmax(180px, 1fr)); gap: 14px; margin-bottom: 18px; }
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
.error-box, .loading-box { margin-bottom: 14px; padding: 13px 15px; border-radius: 12px; font-size: 13px; font-weight: 800; }
.error-box { color: var(--vy-danger); background: rgba(196, 69, 42, 0.1); }
.loading-box { color: var(--vy-ink-2); background: var(--vy-surface-2); }
@media (max-width: 1120px) {
  .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 720px) {
  .workspace { padding: 24px 20px 32px; }
  .page-header { align-items: stretch; flex-direction: column; }
  .summary-grid { grid-template-columns: 1fr; }
}
</style>
