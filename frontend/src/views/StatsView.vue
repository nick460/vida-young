<script setup>
import { TrendingUp } from "lucide-vue-next";
import { VyBarChart } from "../components/ui.js";

const kpis = [
  { label: "Ventas totales", value: "$12,4M", delta: "+18%" },
  { label: "Volumen equipo", value: "$48,9M", delta: "+24%" },
  { label: "Retención red", value: "87%", delta: "+3pp" },
  { label: "Tasa conversión", value: "4,2%", delta: "+0,8pp" }
];

const salesByMonth = [
  { l: "Ene", v: 4.2 },
  { l: "Feb", v: 5.1 },
  { l: "Mar", v: 4.8 },
  { l: "Abr", v: 6.4 },
  { l: "May", v: 7.8 },
  { l: "Jun", v: 8.6 }
];

const cohort = [
  { label: "Sem 1", value: 92 },
  { label: "Sem 2", value: 78 },
  { label: "Sem 3", value: 64 },
  { label: "Sem 4", value: 56 },
  { label: "Sem 5", value: 49 },
  { label: "Sem 6", value: 44 },
  { label: "Sem 7", value: 41 }
];

const categories = [
  { label: "Cuidado facial", value: 38, color: "var(--vy-orange)" },
  { label: "Suplementos", value: 28, color: "var(--vy-beige)" },
  { label: "Aromaterapia", value: 18, color: "var(--vy-cream)" },
  { label: "Nutrición", value: 16, color: "var(--vy-ink-2)" }
];

</script>

<template>
  <div class="vy stats-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Estadísticas</div>
          <h1>Rendimiento y crecimiento</h1>
          <p>Indicadores de ventas, retención de equipo y mix de productos.</p>
        </div>
      </header>

      <section class="kpi-grid">
        <article v-for="kpi in kpis" :key="kpi.label" class="vy-card kpi-card">
          <span>{{ kpi.label }}</span>
          <div>
            <strong>{{ kpi.value }}</strong>
            <small><TrendingUp :size="12" /> {{ kpi.delta }}</small>
          </div>
        </article>
      </section>

      <section class="charts-grid">
        <article class="vy-card chart-card">
          <h2>Ventas por mes</h2>
          <p>Valores expresados en millones COP</p>
          <div class="chart-scroll">
            <VyBarChart :data="salesByMonth" :width="560" :height="200" />
          </div>
        </article>

        <article class="vy-card mix-card">
          <h2>Mix de categorías</h2>
          <div class="category-list">
            <div v-for="category in categories" :key="category.label">
              <header>
                <span>{{ category.label }}</span>
                <strong>{{ category.value }}%</strong>
              </header>
              <div class="track">
                <span :style="{ width: `${category.value}%`, background: category.color }"></span>
              </div>
            </div>
          </div>
        </article>
      </section>

      <section class="vy-card cohort-card">
        <h2>Retención de cohorte</h2>
        <p>Cohorte de embajadores activados en marzo 2026 (% activos por semana)</p>
        <div class="cohort-grid">
          <article v-for="item in cohort" :key="item.label">
            <div :style="{ opacity: item.value / 100 }">{{ item.value }}%</div>
            <span>{{ item.label }}</span>
          </article>
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
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 30px;
  font-weight: 800;
  margin-top: 8px;
}

.page-header p,
.chart-card p,
.cohort-card p {
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

.kpi-card {
  padding: 18px;
}

.kpi-card > span {
  font-size: 11px;
  color: var(--vy-ink-3);
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.kpi-card div {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 8px;
}

.kpi-card strong {
  font-family: var(--font-display);
  font-size: 24px;
  font-weight: 800;
}

.kpi-card small {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border-radius: 999px;
  background: rgba(63, 143, 92, 0.12);
  color: var(--vy-success);
  font-size: 10px;
  font-weight: 800;
}

.charts-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 1fr);
  gap: 14px;
  margin-bottom: 18px;
}

.chart-card,
.mix-card,
.cohort-card {
  padding: 22px;
}

.chart-card h2,
.mix-card h2,
.cohort-card h2 {
  font-size: 16px;
  font-weight: 800;
}

.chart-scroll {
  margin-top: 18px;
  overflow-x: auto;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 13px;
  margin-top: 18px;
}

.category-list header {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  font-weight: 800;
  margin-bottom: 6px;
}

.category-list strong {
  font-family: var(--font-mono);
}

.track {
  height: 8px;
  border-radius: 999px;
  background: var(--vy-line-2);
}

.track span {
  display: block;
  height: 100%;
  border-radius: 999px;
}

.cohort-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 8px;
  margin-top: 18px;
}

.cohort-grid article {
  text-align: center;
}

.cohort-grid div {
  height: 80px;
  border-radius: 10px;
  background: var(--vy-orange);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 800;
  font-family: var(--font-display);
}

.cohort-grid span {
  display: block;
  font-size: 11px;
  color: var(--vy-ink-3);
  font-weight: 700;
  margin-top: 6px;
}

@media (max-width: 1100px) {
  .workspace {
    padding: 24px 20px 32px;
  }

  .kpi-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .charts-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .kpi-grid,
  .cohort-grid {
    grid-template-columns: 1fr;
  }
}
</style>

