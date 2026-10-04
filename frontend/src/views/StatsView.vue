<script setup>
import { computed, onMounted, ref } from "vue";
import { RefreshCw, TrendingUp, Users } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import { useAuthStore } from "../stores/authStore.js";
import { VyBarChart, VyDonut } from "../components/ui.js";

const auth = useAuthStore();
const loading = ref(false);
const error = ref("");
const periodos = ref([]);
const selectedPeriodoId = ref("");
const stats = ref(null);

const personaId = computed(() => auth.usuario?.persona?.id || "");
const selectedPeriodo = computed(() =>
  periodos.value.find((periodo) => Number(periodo.id) === Number(selectedPeriodoId.value))
);
const resumen = computed(() => stats.value?.resumen || {});
const brazos = computed(() => stats.value?.brazos || []);
const porNivel = computed(() => stats.value?.porNivel || []);
const topAportantes = computed(() => stats.value?.topAportantes || []);
const menosAportan = computed(() => stats.value?.menosAportan || []);
const inactivos = computed(() => stats.value?.inactivos || []);
const evolucion = computed(() => stats.value?.evolucion || []);

const brazosChart = computed(() =>
  brazos.value.slice(0, 8).map((brazo) => ({
    l: shortName(brazo.nombre),
    v: Number(brazo.monto || 0)
  }))
);
const evolucionChart = computed(() =>
  evolucion.value.map((punto) => ({
    l: shortPeriodo(punto.periodoNombre),
    v: Number(punto.monto || 0)
  }))
);
const nivelChart = computed(() =>
  porNivel.value.map((nivel) => ({
    l: `N${nivel.nivel}`,
    v: Number(nivel.aporte || 0)
  }))
);
const maxBrazo = computed(() =>
  brazos.value.reduce((max, brazo) => Math.max(max, Number(brazo.monto || 0)), 0)
);
const totalRed = computed(() => Number(resumen.value.totalRed || 0));
const pctActivos = computed(() => Math.round(Number(resumen.value.pctActivos || 0)));
const pieStyle = computed(() => {
  const activos = pctActivos.value;
  return {
    background: `conic-gradient(var(--vy-orange) 0 ${activos}%, var(--vy-cream) ${activos}% 100%)`
  };
});

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function shortName(value) {
  if (!value) return "-";
  const parts = String(value).trim().split(/\s+/);
  if (parts.length === 1) return parts[0].slice(0, 8);
  return `${parts[0].slice(0, 6)} ${parts[1].charAt(0)}.`;
}

function shortPeriodo(nombre) {
  if (!nombre) return "-";
  return String(nombre).split(" ").slice(0, 2).join(" ");
}

async function loadPeriodos() {
  const [activePeriodo, gestiones] = await Promise.all([
    apiRequest("/api/gestiones/periodos/activo"),
    apiRequest("/api/gestiones")
  ]);
  const periodosPorGestion = await Promise.all(
    gestiones.map(async (gestion) => {
      const items = await apiRequest(`/api/gestiones/${gestion.id}/periodos`);
      return items.map((periodo) => ({ ...periodo, gestion: periodo.gestion || gestion }));
    })
  );
  periodos.value = periodosPorGestion
    .flat()
    .sort((a, b) => {
      const gestionA = Number(a.gestion?.anio || 0);
      const gestionB = Number(b.gestion?.anio || 0);
      if (gestionA !== gestionB) return gestionB - gestionA;
      return Number(b.mes || 0) - Number(a.mes || 0);
    });
  if (!selectedPeriodoId.value) {
    selectedPeriodoId.value = String(activePeriodo?.id || periodos.value[0]?.id || "");
  }
}

async function loadStats() {
  loading.value = true;
  error.value = "";
  try {
    if (!periodos.value.length) {
      await loadPeriodos();
    }
    if (!personaId.value) {
      await auth.cargarPerfil();
    }
    if (!personaId.value) {
      error.value = "Tu usuario no tiene una persona asociada.";
      return;
    }
    const query = selectedPeriodoId.value ? `?personaId=${personaId.value}&periodoId=${selectedPeriodoId.value}` : `?personaId=${personaId.value}`;
    stats.value = await apiRequest(`/api/estadisticas/mi-red${query}`);
  } catch (exception) {
    error.value = exception.message || "No se pudieron cargar las estadisticas.";
  } finally {
    loading.value = false;
  }
}

onMounted(loadStats);
</script>

<template>
  <div class="vy stats-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Estadisticas</div>
          <h1>Mi red mes a mes</h1>
          <p>Quienes mas te aportan, quienes menos, inactivos e ingresos por brazo.</p>
          <strong v-if="selectedPeriodo">Mostrando {{ selectedPeriodo.nombre }}.</strong>
        </div>
        <div class="header-actions">
          <label class="period-filter">
            <span>Mes</span>
            <select v-model="selectedPeriodoId" @change="loadStats">
              <option value="" disabled>Selecciona un mes</option>
              <option v-for="periodo in periodos" :key="periodo.id" :value="periodo.id">
                {{ periodo.nombre }} - Gestion {{ periodo.gestion?.anio || "" }}
              </option>
            </select>
          </label>
          <button class="refresh-button" type="button" :disabled="loading" @click="loadStats">
            <RefreshCw :class="{ spinning: loading }" :size="16" stroke-width="2.3" />
            <span>{{ loading ? "Actualizando" : "Actualizar" }}</span>
          </button>
        </div>
      </header>

      <div v-if="error" class="error-box">{{ error }}</div>
      <div v-if="loading" class="loading-box">Cargando estadisticas...</div>

      <section class="kpi-grid">
        <article class="vy-card kpi-card">
          <span><Users :size="13" /> Miembros en red</span>
          <div><strong>{{ resumen.totalRed || 0 }}</strong><small>{{ resumen.directos || 0 }} directos</small></div>
        </article>
        <article class="vy-card kpi-card">
          <span>Activos del mes</span>
          <div><strong>{{ resumen.activos || 0 }}</strong><small>{{ pctActivos }}% · {{ resumen.inactivos || 0 }} inactivos</small></div>
        </article>
        <article class="vy-card kpi-card highlight">
          <span>Ingreso de la red (Bs.)</span>
          <div><strong>{{ money(resumen.total) }}</strong><small><TrendingUp :size="12" /> efect. {{ money(resumen.totalEfectivo) }}</small></div>
        </article>
        <article class="vy-card kpi-card">
          <span>Brazos / Profundidad</span>
          <div><strong>{{ brazos.length }}</strong><small>{{ resumen.profundidad || 0 }} niveles</small></div>
        </article>
      </section>

      <section class="charts-grid">
        <article class="vy-card chart-card">
          <h2>Ingresos por brazo</h2>
          <p>Cada directo es un brazo con todo su subtree.</p>
          <div v-if="brazosChart.length && maxBrazo > 0" class="chart-scroll">
            <VyBarChart :data="brazosChart" :width="560" :height="200" />
          </div>
          <p v-else class="empty">Sin ingresos este mes.</p>
          <div class="brazo-list">
            <div v-for="brazo in brazos" :key="brazo.personaId" class="brazo-row">
              <header><span>{{ brazo.nombre }}</span><strong>Bs. {{ money(brazo.monto) }}</strong></header>
              <div class="track"><span :style="{ width: `${maxBrazo ? (Number(brazo.monto || 0) / maxBrazo) * 100 : 0}%` }"></span></div>
              <small>{{ brazo.miembros }} miembros · {{ brazo.activos }} activos</small>
            </div>
            <p v-if="!brazos.length" class="empty">Aun no tienes directos.</p>
          </div>
        </article>

        <article class="vy-card mix-card">
          <h2>Activos vs inactivos</h2>
          <p>Salud de tu red en el mes.</p>
          <div class="donut-row">
            <VyDonut :value="pctActivos" :size="130" />
            <div class="pie" :style="pieStyle"></div>
          </div>
          <ul class="legend">
            <li><i class="dot active"></i>Activos: {{ resumen.activos || 0 }}</li>
            <li><i class="dot inactive"></i>Inactivos: {{ resumen.inactivos || 0 }}</li>
          </ul>
          <h2 class="sub">Aporte por nivel</h2>
          <div v-if="nivelChart.length" class="chart-scroll">
            <VyBarChart :data="nivelChart" :width="320" :height="140" />
          </div>
          <ul class="nivel-list">
            <li v-for="nivel in porNivel" :key="nivel.nivel">
              <span>Nivel {{ nivel.nivel }} · {{ nivel.miembros }} pers.</span>
              <strong>Bs. {{ money(nivel.aporte) }}</strong>
            </li>
          </ul>
        </article>
      </section>

      <section class="vy-card chart-card">
        <h2>Evolucion de ingresos (ultimos 6 periodos)</h2>
        <p>Para analizar como te va yendo mes a mes.</p>
        <div v-if="evolucionChart.length" class="chart-scroll">
          <VyBarChart :data="evolucionChart" :width="640" :height="180" />
        </div>
        <p v-else class="empty">Sin historial suficiente.</p>
      </section>

      <section class="tables-grid">
        <article class="vy-card">
          <h2>Top: quienes mas te aportan</h2>
          <div class="table-wrap"><table>
            <thead><tr><th>Miembro</th><th>Niv.</th><th>Aporte</th></tr></thead>
            <tbody>
              <tr v-for="miembro in topAportantes" :key="miembro.personaId">
                <td><strong>{{ miembro.nombre }}</strong><small>{{ miembro.activo ? "Activo" : "Inactivo" }}</small></td>
                <td>{{ miembro.nivel }}</td>
                <td class="amount">Bs. {{ money(miembro.aporte) }}</td>
              </tr>
              <tr v-if="!topAportantes.length"><td colspan="3" class="empty">Sin datos.</td></tr>
            </tbody>
          </table></div>
        </article>
        <article class="vy-card">
          <h2>Quienes menos aportan</h2>
          <div class="table-wrap"><table>
            <thead><tr><th>Miembro</th><th>Niv.</th><th>Aporte</th></tr></thead>
            <tbody>
              <tr v-for="miembro in menosAportan" :key="miembro.personaId">
                <td><strong>{{ miembro.nombre }}</strong><small>{{ miembro.activo ? "Activo" : "Inactivo" }}</small></td>
                <td>{{ miembro.nivel }}</td>
                <td class="amount">Bs. {{ money(miembro.aporte) }}</td>
              </tr>
              <tr v-if="!menosAportan.length"><td colspan="3" class="empty">Sin datos.</td></tr>
            </tbody>
          </table></div>
        </article>
        <article class="vy-card">
          <h2>Inactivos del mes</h2>
          <p class="card-sub">Para reactivar o dar seguimiento.</p>
          <div class="table-wrap"><table>
            <thead><tr><th>Miembro</th><th>Niv.</th><th>Doc.</th></tr></thead>
            <tbody>
              <tr v-for="miembro in inactivos" :key="miembro.personaId">
                <td><strong>{{ miembro.nombre }}</strong></td>
                <td>{{ miembro.nivel }}</td>
                <td>{{ miembro.documento || "-" }}</td>
              </tr>
              <tr v-if="!inactivos.length"><td colspan="3" class="empty">Todos activos. Buen trabajo.</td></tr>
            </tbody>
          </table></div>
        </article>
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace { padding: 28px 32px 40px; min-width: 0; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; gap: 20px; margin-bottom: 24px; }
.page-header h1 { font-size: 30px; font-weight: 800; margin-top: 8px; }
.page-header p { font-size: 14px; color: var(--vy-ink-2); margin-top: 4px; }
.page-header strong { display: block; margin-top: 8px; color: var(--vy-orange-deep); font-size: 13px; font-weight: 900; }
.header-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.period-filter { min-width: 230px; display: grid; gap: 6px; }
.period-filter span { color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.period-filter select { min-height: 42px; padding: 0 12px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface); font-weight: 800; outline: none; }
.refresh-button { min-height: 42px; padding: 0 16px; border-radius: 12px; background: linear-gradient(135deg, var(--vy-orange) 0%, var(--vy-orange-deep) 100%); color: #fff; display: inline-flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 900; }
.refresh-button:disabled { opacity: 0.72; cursor: wait; }
.spinning { animation: refresh-spin 0.8s linear infinite; }
@keyframes refresh-spin { to { transform: rotate(360deg); } }
.error-box, .loading-box { padding: 14px 16px; border-radius: 12px; font-size: 13px; font-weight: 800; margin-bottom: 14px; }
.error-box { color: var(--vy-danger); background: rgba(196, 69, 42, 0.1); }
.loading-box { color: var(--vy-ink-2); background: var(--vy-surface-2); }
.kpi-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin-bottom: 18px; }
.kpi-card { padding: 18px; }
.kpi-card.highlight { border-color: var(--vy-orange); background: rgba(242, 135, 5, 0.05); }
.kpi-card > span { font-size: 11px; color: var(--vy-ink-3); font-weight: 800; text-transform: uppercase; display: flex; align-items: center; gap: 6px; }
.kpi-card div { display: flex; align-items: baseline; gap: 8px; margin-top: 8px; flex-wrap: wrap; }
.kpi-card strong { font-size: 24px; font-weight: 800; }
.kpi-card small { font-size: 11px; font-weight: 800; color: var(--vy-ink-2); background: var(--vy-gray); padding: 3px 8px; border-radius: 999px; }
.charts-grid { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(320px, 1fr); gap: 14px; margin-bottom: 18px; }
.chart-card, .mix-card { padding: 22px; margin-bottom: 18px; }
.chart-card h2, .mix-card h2 { font-size: 16px; font-weight: 800; }
.chart-card p, .mix-card p, .card-sub { font-size: 12px; color: var(--vy-ink-3); margin-top: 2px; }
.chart-scroll { margin-top: 14px; overflow-x: auto; }
.empty { color: var(--vy-ink-3); font-size: 13px; font-weight: 700; margin-top: 10px; }
.brazo-list { display: grid; gap: 10px; margin-top: 14px; }
.brazo-row header { display: flex; justify-content: space-between; font-size: 13px; font-weight: 800; margin-bottom: 5px; }
.track { height: 8px; border-radius: 999px; background: var(--vy-line-2); }
.track span { display: block; height: 100%; border-radius: 999px; background: var(--vy-orange); }
.brazo-row small { color: var(--vy-ink-3); font-size: 11px; font-weight: 700; }
.donut-row { display: flex; align-items: center; gap: 18px; margin-top: 14px; }
.pie { width: 90px; height: 90px; border-radius: 50%; }
.legend { list-style: none; margin: 12px 0 0; padding: 0; display: grid; gap: 6px; font-size: 13px; font-weight: 800; }
.dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 8px; }
.dot.active { background: var(--vy-orange); }
.dot.inactive { background: var(--vy-cream); border: 1px solid var(--vy-line); }
.sub { margin-top: 18px; }
.nivel-list { list-style: none; margin: 10px 0 0; padding: 0; display: grid; gap: 6px; }
.nivel-list li { display: flex; justify-content: space-between; font-size: 13px; font-weight: 800; background: var(--vy-surface-2); border-radius: 10px; padding: 8px 10px; }
.tables-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.tables-grid .vy-card { padding: 18px; }
.tables-grid h2 { font-size: 15px; font-weight: 800; margin-bottom: 10px; }
.table-wrap { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; font-size: 13px; }
thead tr { text-align: left; color: var(--vy-ink-3); font-size: 11px; text-transform: uppercase; }
th, td { padding: 10px 6px; }
tbody tr { border-top: 1px solid var(--vy-line-2); }
td small { display: block; color: var(--vy-ink-3); font-size: 11px; }
.amount { text-align: right; font-weight: 900; white-space: nowrap; }
@media (max-width: 1100px) { .kpi-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .charts-grid, .tables-grid { grid-template-columns: 1fr; } .workspace { padding: 24px 20px 32px; } }
@media (max-width: 680px) { .page-header { flex-direction: column; align-items: stretch; } .kpi-grid { grid-template-columns: 1fr; } }
</style>
