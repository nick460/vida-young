<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { Activity, Pencil, Plus, RefreshCw, Save, Trash2 } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";

const loading = ref(false);
const saving = ref(false);
const error = ref("");
const planes = ref([]);
const selectedPlanId = ref("");
const modalOpen = ref(false);
const editingPlanId = ref(null);
const levelRows = ref([]);

const form = reactive({
  nombre: "",
  descripcion: "",
  pvMinimoMensual: 0,
  nivelesAlcance: 3
});

const selectedPlan = computed(() => planes.value.find((plan) => plan.id === Number(selectedPlanId.value)) || null);
const configuredTotal = computed(() => levelRows.value.reduce((sum, nivel) => sum + Number(nivel.montoPorProducto || 0), 0));

function money(value) {
  return Number(value || 0).toFixed(2);
}

async function loadAll() {
  loading.value = true;
  error.value = "";

  try {
    planes.value = await apiRequest("/api/planes-activacion");
    if (!selectedPlanId.value && planes.value.length) {
      selectedPlanId.value = String(planes.value[0].id);
    }
  } catch {
    error.value = "No se pudieron cargar los planes de activacion.";
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  editingPlanId.value = null;
  Object.assign(form, {
    nombre: "",
    descripcion: "",
    pvMinimoMensual: 0,
    nivelesAlcance: 3
  });
  syncLevelRows();
}

function openModal(plan = null) {
  if (plan) {
    editingPlanId.value = plan.id;
    Object.assign(form, {
      nombre: plan.nombre || "",
      descripcion: plan.descripcion || "",
      pvMinimoMensual: plan.pvMinimoMensual || 0,
      nivelesAlcance: plan.nivelesAlcance || 1
    });
    syncLevelRows(plan.niveles || []);
  } else {
    resetForm();
  }

  modalOpen.value = true;
}

function closeModal() {
  modalOpen.value = false;
  resetForm();
}

function syncLevelRows(sourceLevels = []) {
  const levelsByNumber = new Map(sourceLevels.map((nivel) => [Number(nivel.numeroNivel), nivel]));
  const count = Math.max(1, Number(form.nivelesAlcance || 1));

  levelRows.value = Array.from({ length: count }, (_, index) => {
    const numeroNivel = index + 1;
    const savedLevel = levelsByNumber.get(numeroNivel);
    return {
      id: savedLevel?.id || null,
      numeroNivel,
      montoPorProducto: Number(savedLevel?.montoPorProducto || 0)
    };
  });
}

function applyReachToRows() {
  const currentRows = new Map(levelRows.value.map((nivel) => [Number(nivel.numeroNivel), nivel]));
  const count = Math.max(1, Number(form.nivelesAlcance || 1));
  levelRows.value = Array.from({ length: count }, (_, index) => {
    const numeroNivel = index + 1;
    return currentRows.get(numeroNivel) || { id: null, numeroNivel, montoPorProducto: 0 };
  });
}

async function savePlan() {
  saving.value = true;
  const isEditing = Boolean(editingPlanId.value);

  try {
    const saved = await apiRequest(isEditing ? `/api/planes-activacion/${editingPlanId.value}` : "/api/planes-activacion", {
      method: isEditing ? "PUT" : "POST",
      body: JSON.stringify({
        ...form,
        pvMinimoMensual: Number(form.pvMinimoMensual || 0),
        nivelesAlcance: Number(form.nivelesAlcance || 1)
      })
    });

    await Promise.all(levelRows.value.map((nivel) => apiRequest(`/api/planes-activacion/${saved.id}/niveles`, {
      method: "POST",
      body: JSON.stringify({
        numeroNivel: Number(nivel.numeroNivel),
        montoPorProducto: Number(nivel.montoPorProducto || 0)
      })
    })));

    selectedPlanId.value = String(saved.id);
    closeModal();
    await loadAll();
    await Swal.fire("Operacion exitosa", "Plan de activacion guardado correctamente.", "success");
  } catch (exception) {
    await Swal.fire("Revisa los datos", exception.message || "No se pudo guardar el plan de activacion.", "error");
  } finally {
    saving.value = false;
  }
}

async function deletePlan(plan) {
  const result = await Swal.fire({
    title: "Eliminar plan de activacion",
    text: `Se desactivara ${plan.nombre}.`,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Eliminar",
    cancelButtonText: "Cancelar",
    confirmButtonColor: "#C4452A"
  });

  if (!result.isConfirmed) return;

  await apiRequest(`/api/planes-activacion/${plan.id}`, { method: "DELETE" });
  selectedPlanId.value = "";
  await loadAll();
}

onMounted(loadAll);
watch(() => form.nivelesAlcance, applyReachToRows);
</script>

<template>
  <main class="activation-page">
    <section class="activation-header">
      <div>
        <span class="vy-chip vy-chip-orange"><Activity :size="14" /> Activacion mensual</span>
        <h1>Planes de activacion</h1>
        <p>Configura el PV mensual requerido y la ganancia por producto en cada nivel.</p>
      </div>
      <div class="header-actions">
        <button type="button" class="vy-btn vy-btn-ghost" :disabled="loading" @click="loadAll">
          <RefreshCw :size="16" /> Actualizar
        </button>
        <button type="button" class="vy-btn vy-btn-primary" @click="openModal()">
          <Plus :size="16" /> Nuevo plan
        </button>
      </div>
    </section>

    <p v-if="error" class="error-box">{{ error }}</p>

    <section class="activation-layout">
      <article class="vy-card plan-list">
        <h2>Configuraciones activas</h2>
        <button
          v-for="plan in planes"
          :key="plan.id"
          type="button"
          class="plan-row"
          :class="{ active: plan.id === Number(selectedPlanId) }"
          @click="selectedPlanId = String(plan.id)"
        >
          <span>
            <strong>{{ plan.nombre }}</strong>
            <small>{{ money(plan.pvMinimoMensual) }} PV mensuales · {{ plan.nivelesAlcance }} niveles</small>
          </span>
          <span class="row-actions">
            <button type="button" title="Editar" @click.stop="openModal(plan)"><Pencil :size="15" /></button>
            <button type="button" class="danger" title="Eliminar" @click.stop="deletePlan(plan)"><Trash2 :size="15" /></button>
          </span>
        </button>
        <p v-if="!planes.length && !loading" class="empty-state">No hay planes de activacion configurados.</p>
      </article>

      <article class="vy-card detail-card">
        <h2>{{ selectedPlan?.nombre || "Selecciona un plan" }}</h2>
        <p v-if="selectedPlan">{{ selectedPlan.descripcion || "Sin descripcion" }}</p>
        <div v-if="selectedPlan" class="detail-grid">
          <span><b>{{ money(selectedPlan.pvMinimoMensual) }}</b><small>PV requerido</small></span>
          <span><b>{{ selectedPlan.nivelesAlcance }}</b><small>Niveles</small></span>
        </div>
        <div v-if="selectedPlan" class="level-list">
          <article v-for="nivel in selectedPlan.niveles || []" :key="nivel.id">
            <strong>Nivel {{ nivel.numeroNivel }}</strong>
            <span>Bs. {{ money(nivel.montoPorProducto) }} por producto</span>
          </article>
        </div>
      </article>
    </section>

    <Teleport to="body">
      <div v-if="modalOpen" class="modal-backdrop">
        <form class="activation-modal" @submit.prevent="savePlan">
          <header>
            <div>
              <span class="vy-chip vy-chip-orange"><Activity :size="14" /> Plan</span>
              <h2>{{ editingPlanId ? "Editar activacion" : "Nueva activacion" }}</h2>
            </div>
            <button type="button" aria-label="Cerrar" @click="closeModal">x</button>
          </header>

          <div class="modal-grid">
            <label class="full-field">
              Nombre
              <input v-model.trim="form.nombre" required maxlength="100" />
            </label>
            <label class="full-field">
              Descripcion
              <textarea v-model.trim="form.descripcion" maxlength="500" rows="3"></textarea>
            </label>
            <label>
              PV minimo mensual
              <input v-model.number="form.pvMinimoMensual" type="number" min="0" step="0.01" required />
            </label>
            <label>
              Niveles de alcance
              <input v-model.number="form.nivelesAlcance" type="number" min="1" max="20" step="1" required />
            </label>
          </div>

          <section class="levels-editor">
            <div class="levels-header">
              <h3>Ganancia por producto</h3>
              <strong>Bs. {{ money(configuredTotal) }}</strong>
            </div>
            <article v-for="nivel in levelRows" :key="nivel.numeroNivel" class="level-row">
              <span>Nivel {{ nivel.numeroNivel }}</span>
              <label>
                Monto por producto
                <input v-model.number="nivel.montoPorProducto" type="number" min="0" step="0.01" required />
              </label>
            </article>
          </section>

          <footer>
            <button type="button" class="vy-btn vy-btn-ghost" @click="closeModal">Cancelar</button>
            <button type="submit" class="vy-btn vy-btn-primary" :disabled="saving">
              <Save :size="16" /> Guardar
            </button>
          </footer>
        </form>
      </div>
    </Teleport>
  </main>
</template>

<style scoped>
.activation-page { padding: 28px; display: flex; flex-direction: column; gap: 20px; }
.activation-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.activation-header h1 { margin-top: 10px; font-size: 30px; }
.activation-header p { margin-top: 6px; color: var(--vy-ink-2); }
.header-actions { display: inline-flex; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.activation-layout { display: grid; grid-template-columns: minmax(360px, 0.9fr) minmax(420px, 1.1fr); gap: 16px; align-items: start; }
.plan-list, .detail-card { padding: 18px; }
.plan-list h2, .detail-card h2 { font-size: 18px; margin-bottom: 14px; }
.plan-row { width: 100%; display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px; border: 1px solid var(--vy-line); border-radius: 10px; background: var(--vy-surface-2); text-align: left; margin-bottom: 10px; }
.plan-row.active { border-color: var(--vy-orange); background: rgba(242, 135, 5, 0.09); }
.plan-row strong, .plan-row small { display: block; }
.plan-row small, .empty-state, .detail-card p { color: var(--vy-ink-3); margin-top: 4px; }
.row-actions { display: inline-flex; gap: 8px; }
.row-actions button { width: 34px; height: 34px; border-radius: 8px; background: #fff; border: 1px solid var(--vy-line); color: var(--vy-ink-2); display: inline-flex; align-items: center; justify-content: center; }
.row-actions .danger { color: var(--vy-danger); }
.detail-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin: 14px 0; }
.detail-grid span { padding: 14px; border-radius: 10px; background: var(--vy-surface-2); }
.detail-grid b, .detail-grid small { display: block; }
.detail-grid b { font-family: var(--font-display); font-size: 24px; }
.detail-grid small { margin-top: 4px; color: var(--vy-ink-3); font-weight: 800; }
.level-list { display: grid; gap: 8px; }
.level-list article { display: flex; justify-content: space-between; gap: 12px; padding: 12px; border: 1px solid var(--vy-line); border-radius: 10px; }
.error-box { border: 1px solid rgba(196, 69, 42, 0.25); border-radius: 10px; padding: 12px 14px; color: var(--vy-danger); background: rgba(196, 69, 42, 0.08); }
.modal-backdrop { position: fixed; inset: 0; z-index: 80; display: flex; align-items: center; justify-content: center; padding: 24px; background: rgba(31, 26, 20, 0.45); }
.activation-modal { width: min(820px, 100%); max-height: calc(100vh - 48px); overflow-y: auto; padding: 22px; border-radius: 16px; background: var(--vy-surface); box-shadow: var(--vy-shadow-lg); color: var(--vy-ink); font-family: var(--font-sans); }
.activation-modal header, .activation-modal footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.activation-modal header { padding-bottom: 16px; margin-bottom: 16px; border-bottom: 1px solid var(--vy-line-2); }
.activation-modal header h2 { margin-top: 8px; font-size: 22px; }
.activation-modal header > button { width: 36px; height: 36px; border-radius: 10px; background: var(--vy-surface-2); color: var(--vy-ink-3); }
.modal-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }
.full-field { grid-column: 1 / -1; }
.activation-modal label { display: flex; flex-direction: column; gap: 6px; font-size: 12px; font-weight: 900; color: var(--vy-ink-2); }
.activation-modal input, .activation-modal textarea { width: 100%; border: 1px solid var(--vy-line); border-radius: 10px; padding: 12px 13px; font: inherit; }
.levels-editor { margin-top: 16px; padding: 14px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); }
.levels-header { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.level-row { display: grid; grid-template-columns: 90px minmax(0, 1fr); gap: 12px; align-items: end; padding: 10px; border: 1px solid var(--vy-line); border-radius: 10px; background: #fff; margin-bottom: 8px; }
.level-row > span { align-self: center; font-weight: 900; }
.activation-modal footer { margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--vy-line-2); justify-content: flex-end; }
@media (max-width: 900px) {
  .activation-page { padding: 18px; }
  .activation-header, .header-actions, .activation-modal header, .activation-modal footer { align-items: stretch; flex-direction: column; }
  .activation-layout, .modal-grid, .detail-grid, .level-row { grid-template-columns: 1fr; }
  .full-field { grid-column: auto; }
}
</style>
