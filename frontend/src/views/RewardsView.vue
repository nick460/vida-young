<script setup>
import { computed, onMounted, ref } from "vue";
import { Gift, PackageCheck, RefreshCw, Users, Wallet } from "lucide-vue-next";
import { useAuthStore } from "../stores/authStore.js";
import { apiRequest } from "../services/api.js";
import { VyAvatar, VyDonut } from "../components/ui.js";

const authStore = useAuthStore();
const loading = ref(false);
const error = ref("");
const recompensas = ref([]);

const currentPersona = computed(() => authStore.usuario?.persona || null);
const accumulatedRewards = computed(() => recompensas.value.map((item) => ({
  id: item.id,
  persona: item.referido?.persona,
  plan: item.planIngreso,
  fechaUnion: item.referido?.fechaUnion,
  nivel: Number(item.nivelGenerado || 0),
  montoDirecto: Math.max(0, Number(item.montoEfectivo || 0) - Number(item.montoEfectivoRetirado || 0)),
  valorProductos: Math.max(0, Number(item.valorProductos || 0) - Number(item.valorProductosRetirado || 0)),
  cobrable: item.cobrable !== false,
  motivoNoCobrable: item.motivoNoCobrable || "No cobrable porque la membresia no esta activa."
})));

const payableRewards = computed(() => accumulatedRewards.value.filter((item) => item.cobrable));
const lostRewards = computed(() => accumulatedRewards.value.filter((item) => !item.cobrable));
const totalCash = computed(() => payableRewards.value.reduce((sum, item) => sum + item.montoDirecto, 0));
const totalProducts = computed(() => payableRewards.value.reduce((sum, item) => sum + item.valorProductos, 0));
const totalRewards = computed(() => totalCash.value + totalProducts.value);
const lostCash = computed(() => lostRewards.value.reduce((sum, item) => sum + item.montoDirecto, 0));
const lostProducts = computed(() => lostRewards.value.reduce((sum, item) => sum + item.valorProductos, 0));
const lostTotal = computed(() => lostCash.value + lostProducts.value);
const paidRewardCount = computed(() => accumulatedRewards.value.length);
const directRewardCount = computed(() => accumulatedRewards.value.filter((item) => item.nivel === 1).length);
const maxRewardLevel = computed(() => Math.max(0, ...accumulatedRewards.value.map((item) => item.nivel)));
const progressValue = computed(() => Math.min(100, Math.round((paidRewardCount.value / Math.max(1, paidRewardCount.value)) * 100)));

const rewardTypes = [
  { label: "Efectivo acumulado", icon: Wallet, copy: "Monto generado por los ingresos donde apareces dentro del alcance" },
  { label: "Productos canjeables", icon: Gift, copy: "Valor disponible en productos por niveles pagables" },
  { label: "Niveles pagados", icon: Users, copy: "Incluye nivel 1 y niveles profundos segun el plan de ingreso" },
  { label: "Plan de ingreso", icon: PackageCheck, copy: "Cada ingreso paga segun la matriz del plan con el que entra esa persona" }
];

function fullName(persona) {
  return `${persona?.nombres || ""} ${persona?.apellidos || ""}`.trim() || "Persona";
}

function initials(persona) {
  return `${persona?.nombres?.[0] || ""}${persona?.apellidos?.[0] || ""}`.toUpperCase() || "VY";
}

function money(value) {
  return Number(value || 0).toLocaleString("es-CO", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

async function loadRewards() {
  loading.value = true;
  error.value = "";

  try {
    if (!authStore.usuario?.persona?.id) {
      await authStore.cargarPerfil();
    }

    recompensas.value = currentPersona.value?.id
      ? await apiRequest(`/api/recompensas/persona/${currentPersona.value.id}`)
      : [];
  } catch (exception) {
    error.value = "No se pudieron cargar tus recompensas. Verifica que la sesion siga activa.";
  } finally {
    loading.value = false;
  }
}

onMounted(loadRewards);
</script>

<template>
  <div class="vy rewards-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Recompensas y bonos</div>
          <h1>Recompensas acumuladas</h1>
          <p>{{ paidRewardCount }} ingresos generan recompensa para tu cuenta.</p>
        </div>
        <button class="refresh-rewards-button" type="button" :disabled="loading" @click="loadRewards">
          <RefreshCw :size="16" />
          Actualizar
        </button>
      </header>

      <p v-if="error" class="rewards-error">{{ error }}</p>

      <section class="hero-card">
        <div class="bonus-summary">
          <div class="summary-content">
            <span>Total acumulado</span>
            <strong>{{ money(totalRewards) }}</strong>
            <p>
              Incluye {{ money(totalCash) }} en efectivo cobrable y {{ money(totalProducts) }} en valor canjeable por productos.
              <span v-if="lostTotal"> Estas perdiendo {{ money(lostTotal) }} por no estar activo.</span>
            </p>

            <div class="summary-stats">
              <article>
                <small>Cobrables</small>
                <b>{{ payableRewards.length }}</b>
              </article>
              <article>
                <small>Perdidas</small>
                <b>{{ lostRewards.length }}</b>
              </article>
              <article>
                <small>Total beneficios</small>
                <b>{{ paidRewardCount }}</b>
              </article>
              <article>
                <small>Nivel 1</small>
                <b>{{ directRewardCount }}</b>
              </article>
              <article>
                <small>Efectivo</small>
                <b>{{ money(totalCash) }}</b>
              </article>
              <article>
                <small>Productos</small>
                <b>{{ money(totalProducts) }}</b>
              </article>
            </div>
          </div>
        </div>

        <div class="next-bonus">
          <VyDonut :value="progressValue" :size="140" :stroke="14" />
          <div>
            <span>Alcance pagado</span>
            <h2>Nivel maximo {{ maxRewardLevel }}</h2>
            <p>Se suman los ingresos donde tu persona aparece dentro del alcance del plan con el que entraron.</p>
          </div>
        </div>
      </section>

      <section class="content-grid">
        <article class="rewards-card">
            <h2>Ingresos que te generan recompensa</h2>

          <div v-if="loading" class="empty-state">Cargando recompensas...</div>
          <div v-else-if="!currentPersona?.id" class="empty-state">Tu usuario no tiene persona asociada.</div>
          <div v-else-if="!accumulatedRewards.length" class="empty-state">Todavia no tienes recompensas acumuladas.</div>

          <div v-else class="reward-table">
            <article v-for="reward in accumulatedRewards" :key="reward.id" class="reward-row" :class="{ lost: !reward.cobrable }">
              <div class="member-cell">
                <VyAvatar :name="initials(reward.persona)" :size="36" bg="var(--vy-cream)" />
                <span>
                  <strong>{{ fullName(reward.persona) }}</strong>
                  <small>Nivel {{ reward.nivel }} · {{ reward.plan?.nombre || "Sin plan" }}</small>
                  <em v-if="!reward.cobrable">{{ reward.motivoNoCobrable }}</em>
                </span>
              </div>
              <div class="reward-values">
                <span>
                  <small>{{ reward.cobrable ? "Monto directo" : "Efectivo perdido" }}</small>
                  <strong>{{ money(reward.montoDirecto) }}</strong>
                </span>
                <span>
                  <small>{{ reward.cobrable ? "Productos" : "Productos perdidos" }}</small>
                  <strong>{{ money(reward.valorProductos) }}</strong>
                </span>
              </div>
            </article>
          </div>
        </article>

        <article class="reward-types-card">
          <h2>Tipos de recompensa</h2>
          <div class="reward-list">
            <article v-for="reward in rewardTypes" :key="reward.label">
              <span class="reward-icon">
                <component :is="reward.icon" :size="18" stroke-width="2" />
              </span>
              <div>
                <strong>{{ reward.label }}</strong>
                <p>{{ reward.copy }}</p>
              </div>
            </article>
          </div>
        </article>
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

.refresh-rewards-button {
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid rgba(242, 135, 5, 0.34);
  border-radius: 8px;
  background: #fff;
  color: var(--vy-orange-deep);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 900;
  box-shadow: 0 8px 18px rgba(31, 26, 20, 0.05);
}

.refresh-rewards-button:hover:not(:disabled) {
  background: rgba(242, 135, 5, 0.08);
  border-color: rgba(242, 135, 5, 0.62);
  color: var(--vy-ink);
}

.rewards-error,
.empty-state {
  padding: 14px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
}

.rewards-error {
  color: var(--vy-danger);
  background: rgba(196, 69, 42, 0.1);
  margin-bottom: 14px;
}

.empty-state {
  color: var(--vy-ink-2);
  background: var(--vy-surface-2);
}

.hero-card {
  margin-bottom: 18px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  overflow: hidden;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface);
  box-shadow: var(--vy-shadow-sm);
}

.bonus-summary {
  padding: 32px;
  background: linear-gradient(135deg, var(--vy-orange) 0%, var(--vy-orange-deep) 100%);
  color: #fff;
}

.summary-content > span,
.next-bonus span {
  display: block;
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-weight: 800;
  opacity: 0.86;
  margin-bottom: 10px;
}

.summary-content > strong {
  display: block;
  font-family: var(--font-display);
  font-size: 42px;
  font-weight: 800;
  line-height: 1;
}

.summary-content > p {
  margin-top: 14px;
  font-size: 14px;
  line-height: 1.5;
  opacity: 0.92;
}

.summary-stats {
  display: flex;
  gap: 18px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.summary-stats small {
  display: block;
  font-size: 10.5px;
  opacity: 0.82;
  font-weight: 800;
  text-transform: uppercase;
}

.summary-stats b {
  display: block;
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 800;
  margin-top: 2px;
}

.next-bonus {
  padding: 32px;
  display: flex;
  align-items: center;
  gap: 24px;
}

.next-bonus span {
  color: var(--vy-ink-3);
  opacity: 1;
  margin-bottom: 6px;
}

.next-bonus h2 {
  font-size: 20px;
  font-weight: 800;
}

.next-bonus p {
  font-size: 12.5px;
  color: var(--vy-ink-2);
  margin-top: 6px;
  line-height: 1.4;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 1fr);
  gap: 14px;
}

.rewards-card,
.reward-types-card {
  padding: 22px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface);
  box-shadow: var(--vy-shadow-sm);
}

.rewards-card h2,
.reward-types-card h2 {
  font-size: 16px;
  font-weight: 800;
  margin-bottom: 18px;
}

.reward-table,
.reward-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reward-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(260px, auto);
  align-items: center;
  gap: 14px;
  padding: 12px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface-2);
}

.reward-row.lost {
  border-color: rgba(196, 69, 42, 0.24);
  background: rgba(196, 69, 42, 0.06);
}

.member-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.member-cell span {
  min-width: 0;
}

.member-cell strong,
.member-cell small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-cell strong {
  font-size: 13px;
  font-weight: 900;
}

.member-cell small {
  margin-top: 2px;
  color: var(--vy-orange-deep);
  font-size: 11px;
  font-weight: 900;
}

.member-cell em {
  display: block;
  margin-top: 4px;
  color: var(--vy-danger);
  font-size: 11px;
  font-style: normal;
  font-weight: 900;
  white-space: normal;
}

.reward-values {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.reward-values span {
  padding: 10px;
  border-radius: 8px;
  background: #fff;
}

.reward-values small {
  display: block;
  color: var(--vy-ink-3);
  font-size: 10px;
  font-weight: 900;
  text-transform: uppercase;
}

.reward-values strong {
  display: block;
  margin-top: 4px;
  color: var(--vy-ink);
  font-size: 13px;
  font-weight: 900;
}

.reward-row.lost .reward-values span {
  background: #fff7f4;
}

.reward-row.lost .reward-values small,
.reward-row.lost .reward-values strong {
  color: var(--vy-danger);
}

.reward-list article {
  display: flex;
  gap: 14px;
  padding: 14px;
  border-radius: 8px;
  background: var(--vy-surface-2);
}

.reward-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  background: var(--vy-cream);
  color: var(--vy-orange-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.reward-list strong {
  display: block;
  font-weight: 800;
  font-size: 13.5px;
}

.reward-list p {
  font-size: 12px;
  color: var(--vy-ink-2);
  margin-top: 2px;
  line-height: 1.4;
}

@media (max-width: 1040px) {
  .workspace {
    padding: 24px 20px 32px;
  }

  .hero-card,
  .content-grid,
  .reward-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .page-header,
  .next-bonus {
    align-items: flex-start;
    flex-direction: column;
  }

  .reward-values {
    grid-template-columns: 1fr;
  }
}
</style>
