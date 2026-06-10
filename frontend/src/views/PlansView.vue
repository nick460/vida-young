<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import {
  BadgePercent,
  ImagePlus,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Trash2
} from "lucide-vue-next";
import { apiRequest } from "../services/api.js";

const loading = ref(false);
const saving = ref(false);
const error = ref("");
const planes = ref([]);
const selectedPlanId = ref("");
const editingPlanId = ref(null);
const planImageFile = ref(null);
const planImagePreview = ref("");
const planModalOpen = ref(false);
const modalLevelRows = ref([]);

const planForm = reactive({
  nombre: "",
  descripcion: "",
  precio: 0,
  qp: 0,
  bonificacionDirecta: 0,
  valorProductosBeneficio: 0,
  imagenUrl: "",
  nivelesAlcance: 1
});

const selectedPlan = computed(() => planes.value.find((plan) => plan.id === Number(selectedPlanId.value)) || null);
const modalCashTotal = computed(() => modalLevelRows.value.reduce((sum, nivel) => sum + Number(nivel.porcentajeComision || 0), 0));
const modalProductTotal = computed(() => modalLevelRows.value.reduce((sum, nivel) => sum + Number(nivel.valorProductosBeneficio || 0), 0));
const modalConfiguredTotal = computed(() => modalCashTotal.value + modalProductTotal.value);

async function loadAll() {
  loading.value = true;
  error.value = "";

  try {
    const planesData = await apiRequest("/api/planes");
    planes.value = planesData;

    if (!selectedPlanId.value && planes.value.length) {
      selectedPlanId.value = String(planes.value[0].id);
    }
  } catch (exception) {
    error.value = "No se pudieron cargar los planes. Verifica que el backend este activo y la sesion sea valida.";
  } finally {
    loading.value = false;
  }
}

function resetPlanForm() {
  editingPlanId.value = null;
  planImageFile.value = null;
  planImagePreview.value = "";
  Object.assign(planForm, {
    nombre: "",
    descripcion: "",
    precio: 0,
    qp: 0,
    bonificacionDirecta: 0,
    valorProductosBeneficio: 0,
    imagenUrl: "",
    nivelesAlcance: 1
  });
  syncModalLevelRows();
}

function openPlanModal(plan = null) {
  if (plan) {
    editPlan(plan);
  } else {
    resetPlanForm();
  }

  planModalOpen.value = true;
}

function closePlanModal() {
  planModalOpen.value = false;
  resetPlanForm();
}

function editPlan(plan) {
  editingPlanId.value = plan.id;
  planImageFile.value = null;
  planImagePreview.value = plan.imagenUrl || "";
  Object.assign(planForm, {
    nombre: plan.nombre || "",
    descripcion: plan.descripcion || "",
    precio: plan.precio || 0,
    qp: plan.qp || 0,
    bonificacionDirecta: plan.bonificacionDirecta || 0,
    valorProductosBeneficio: plan.valorProductosBeneficio || 0,
    imagenUrl: plan.imagenUrl || "",
    nivelesAlcance: plan.nivelesAlcance || 1
  });
  syncModalLevelRows(plan.niveles || []);
}

function handlePlanImage(event) {
  const [file] = event.target.files || [];
  planImageFile.value = file || null;
  planImagePreview.value = file ? URL.createObjectURL(file) : planForm.imagenUrl;
}

async function savePlan() {
  saving.value = true;
  const isEditing = Boolean(editingPlanId.value);
  const firstLevel = modalLevelRows.value.find((nivel) => Number(nivel.numeroNivel) === 1) || {};

  try {
    const saved = await apiRequest(isEditing ? `/api/planes/${editingPlanId.value}` : "/api/planes", {
      method: isEditing ? "PUT" : "POST",
      body: JSON.stringify({
        ...planForm,
        precio: Number(planForm.precio),
        qp: Number(planForm.qp || 0),
        bonificacionDirecta: Number(firstLevel.porcentajeComision || 0),
        valorProductosBeneficio: Number(firstLevel.valorProductosBeneficio || 0),
        nivelesAlcance: Number(planForm.nivelesAlcance)
      })
    });

    if (planImageFile.value) {
      const formData = new FormData();
      formData.append("imagen", planImageFile.value);
      await apiRequest(`/api/planes/${saved.id}/imagen`, {
        method: "POST",
        body: formData
      });
    }

    selectedPlanId.value = String(saved.id);
    await saveModalLevels(saved.id);
    await showSuccess(isEditing ? "Plan editado correctamente." : "Plan guardado correctamente.");
    closePlanModal();
    await loadAll();
  } catch (exception) {
    await showError(exception.message || "No se pudo guardar el plan.");
  } finally {
    saving.value = false;
  }
}

function syncModalLevelRows(sourceLevels = []) {
  const levelsByNumber = new Map(sourceLevels.map((nivel) => [Number(nivel.numeroNivel), nivel]));
  const count = Math.max(1, Number(planForm.nivelesAlcance || 1));

  modalLevelRows.value = Array.from({ length: count }, (_, index) => {
    const numeroNivel = index + 1;
    const savedLevel = levelsByNumber.get(numeroNivel);

    return {
      id: savedLevel?.id || null,
      numeroNivel,
      porcentajeComision: Number(savedLevel?.porcentajeComision ?? (numeroNivel === 1 ? planForm.bonificacionDirecta : 0) ?? 0),
      valorProductosBeneficio: Number(savedLevel?.valorProductosBeneficio ?? (numeroNivel === 1 ? planForm.valorProductosBeneficio : 0) ?? 0)
    };
  });
}

function applyReachToModalRows() {
  const count = Math.max(1, Number(planForm.nivelesAlcance || 1));
  const currentRows = new Map(modalLevelRows.value.map((nivel) => [Number(nivel.numeroNivel), nivel]));

  modalLevelRows.value = Array.from({ length: count }, (_, index) => {
    const numeroNivel = index + 1;
    return currentRows.get(numeroNivel) || {
      id: null,
      numeroNivel,
      porcentajeComision: 0,
      valorProductosBeneficio: 0
    };
  });
}

async function saveModalLevels(planId) {
  const rows = modalLevelRows.value.slice(0, Number(planForm.nivelesAlcance || 1));

  await Promise.all(rows.map((nivel) => apiRequest(`/api/planes/${planId}/niveles`, {
    method: "POST",
    body: JSON.stringify({
      numeroNivel: Number(nivel.numeroNivel),
      porcentajeComision: Number(nivel.porcentajeComision || 0),
      valorProductosBeneficio: Number(nivel.valorProductosBeneficio || 0)
    })
  })));

  if (selectedPlan.value?.id === Number(planId)) {
    const reach = Number(planForm.nivelesAlcance || 1);
    const levelsToRemove = (selectedPlan.value.niveles || []).filter((nivel) => Number(nivel.numeroNivel) > reach);
    await Promise.all(levelsToRemove.map((nivel) => apiRequest(`/api/planes/niveles/${nivel.id}`, { method: "DELETE" })));
  }
}

async function deletePlan(plan) {
  const result = await Swal.fire({
    title: "Eliminar plan",
    text: `Se desactivara ${plan.nombre}.`,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Eliminar",
    cancelButtonText: "Cancelar",
    confirmButtonColor: "#C4452A",
    cancelButtonColor: "#1F1A14"
  });

  if (!result.isConfirmed) return;

  await apiRequest(`/api/planes/${plan.id}`, { method: "DELETE" });
  selectedPlanId.value = "";
  await loadAll();
}

function imageSource(url) {
  return url || "";
}

function showError(text) {
  return Swal.fire({
    title: "Revisa los datos",
    text,
    icon: "error",
    confirmButtonText: "Entendido",
    confirmButtonColor: "#F28705"
  });
}

function showSuccess(text) {
  return Swal.fire({
    title: "Operacion exitosa",
    text,
    icon: "success",
    confirmButtonText: "Entendido",
    confirmButtonColor: "#F28705"
  });
}

onMounted(loadAll);
onUnmounted(() => {
  document.body.style.overflow = "";
});

watch(() => planForm.nivelesAlcance, applyReachToModalRows);
watch(planModalOpen, (isOpen) => {
  document.body.style.overflow = isOpen ? "hidden" : "";
});
</script>

<template>
  <main class="plans-page">
    <section class="plans-header">
      <div>
        <span class="vy-chip vy-chip-orange"><BadgePercent :size="14" /> Configuracion</span>
        <h1>Planes</h1>
        <p>Registra cada plan y configura su matriz de recompensas por profundidad.</p>
      </div>
      <div class="header-actions">
        <button type="button" class="refresh-plans-button" :disabled="loading" @click="loadAll">
          <RefreshCw :size="16" /> Actualizar
        </button>
        <button type="button" class="new-plan-button" @click="openPlanModal()">
          <Plus :size="16" /> Nuevo plan
        </button>
      </div>
    </section>

    <p v-if="error" class="plans-error">{{ error }}</p>

    <section class="plans-layout">
      <section class="plans-panel plans-list">
        <h2>Planes activos</h2>
        <button
          v-for="plan in planes"
          :key="plan.id"
          type="button"
          class="plan-row"
          :class="{ active: plan.id === Number(selectedPlanId) }"
          @click="selectedPlanId = String(plan.id)"
        >
          <span>
            <span class="plan-thumb">
              <img v-if="plan.imagenUrl" :src="imageSource(plan.imagenUrl)" alt="" />
              <BadgePercent v-else :size="16" />
            </span>
            <strong>{{ plan.nombre }}</strong>
            <small>Precio Bs. {{ Number(plan.precio).toFixed(2) }} - QP {{ Number(plan.qp || 0).toFixed(2) }} - {{ plan.nivelesAlcance }} niveles</small>
            <small class="benefit-summary">
              Directo Bs. {{ Number(plan.bonificacionDirecta || 0).toFixed(2) }} - Productos Bs. {{ Number(plan.valorProductosBeneficio || 0).toFixed(2) }}
            </small>
          </span>
          <span class="plan-row-actions">
            <button type="button" class="icon-action" title="Editar plan" @click.stop="openPlanModal(plan)">
              <Pencil :size="16" />
            </button>
            <button type="button" class="icon-action danger" title="Eliminar plan" @click.stop="deletePlan(plan)">
              <Trash2 :size="16" />
            </button>
          </span>
        </button>
        <p v-if="!planes.length && !loading" class="empty-state">No hay planes configurados.</p>
      </section>
    </section>

    <Teleport to="body">
      <div v-if="planModalOpen" class="modal-backdrop" @click.self="closePlanModal">
        <form class="plan-modal" @submit.prevent="savePlan">
          <header>
            <div>
              <span class="vy-chip vy-chip-orange"><BadgePercent :size="14" /> Plan</span>
              <h2>{{ editingPlanId ? "Editar plan" : "Nuevo plan" }}</h2>
            </div>
            <button type="button" aria-label="Cerrar" @click="closePlanModal">×</button>
          </header>

          <div class="plan-modal-grid">
            <section class="plan-editor-column">
              <div class="plan-image-field">
                <div class="plan-image-preview">
                  <img v-if="planImagePreview || planForm.imagenUrl" :src="imageSource(planImagePreview || planForm.imagenUrl)" alt="" />
                  <BadgePercent v-else :size="34" />
                </div>
                <label class="file-button">
                  <ImagePlus :size="16" />
                  <span>Imagen del plan</span>
                  <input type="file" accept="image/png,image/jpeg,image/webp" @change="handlePlanImage" />
                </label>
              </div>

              <div class="modal-grid">
                <label class="full-field">
                  Nombre
                  <input v-model.trim="planForm.nombre" type="text" required maxlength="100" />
                </label>
                <label class="full-field">
                  Descripcion
                  <textarea v-model.trim="planForm.descripcion" maxlength="500" rows="3"></textarea>
                </label>
                <label>
                  Precio
                  <input v-model.number="planForm.precio" type="number" min="0" step="0.01" required />
                </label>
                <label>
                  QP
                  <input v-model.number="planForm.qp" type="number" min="0" step="0.01" required />
                </label>
                <label>
                  Niveles de alcance
                  <input v-model.number="planForm.nivelesAlcance" type="number" min="1" max="20" step="1" required />
                </label>
              </div>

            </section>

            <section class="level-editor-column">
              <div class="level-editor-header">
                <div>
                  <h3>Matriz de recompensas</h3>
                  <p>{{ modalLevelRows.length }} niveles generados por alcance</p>
                </div>
                <strong>Bs. {{ modalConfiguredTotal.toFixed(2) }}</strong>
              </div>

              <div class="modal-level-list">
                <article v-for="nivel in modalLevelRows" :key="nivel.numeroNivel" class="modal-level-row">
                  <span>Nivel {{ nivel.numeroNivel }}</span>
                  <label>
                    Efectivo / valor
                    <input v-model.number="nivel.porcentajeComision" type="number" min="0" step="0.01" required />
                  </label>
                  <label>
                    Productos
                    <input v-model.number="nivel.valorProductosBeneficio" type="number" min="0" step="0.01" required />
                  </label>
                  <strong>Bs. {{ (Number(nivel.porcentajeComision || 0) + Number(nivel.valorProductosBeneficio || 0)).toFixed(2) }}</strong>
                </article>
              </div>
            </section>
          </div>

          <footer>
            <button type="button" class="vy-btn vy-btn-ghost" @click="closePlanModal">Cancelar</button>
            <button type="submit" class="vy-btn vy-btn-primary plan-save-button" :disabled="saving">
              <Save :size="16" /> Guardar
            </button>
          </footer>
        </form>
      </div>
    </Teleport>
  </main>
</template>

<style scoped>
.plans-page {
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.plans-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.plans-header h1 {
  margin-top: 10px;
  font-size: 30px;
}

.plans-header p {
  margin-top: 6px;
  color: var(--vy-ink-2);
}

.plans-layout {
  display: grid;
  grid-template-columns: 1fr;
}

.header-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.refresh-plans-button,
.new-plan-button {
  min-height: 42px;
  padding: 0 16px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 900;
  box-shadow: 0 8px 18px rgba(31, 26, 20, 0.05);
  transition: background 0.16s ease, border-color 0.16s ease, color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.refresh-plans-button {
  border: 1px solid rgba(242, 135, 5, 0.34);
  background: #fff;
  color: var(--vy-orange-deep);
}

.new-plan-button {
  border: 1px solid var(--vy-orange);
  background: var(--vy-orange);
  color: #fff;
}

.refresh-plans-button:hover:not(:disabled),
.new-plan-button:hover:not(:disabled) {
  box-shadow: 0 10px 22px rgba(31, 26, 20, 0.08);
  transform: translateY(-1px);
}

.refresh-plans-button:hover:not(:disabled) {
  background: rgba(242, 135, 5, 0.08);
  border-color: rgba(242, 135, 5, 0.62);
  color: var(--vy-ink);
}

.new-plan-button:hover:not(:disabled) {
  background: var(--vy-orange-deep);
  border-color: var(--vy-orange-deep);
}

.refresh-plans-button:focus-visible,
.new-plan-button:focus-visible {
  outline: 3px solid rgba(242, 135, 5, 0.24);
  outline-offset: 2px;
}

.refresh-plans-button:disabled {
  cursor: wait;
  opacity: 0.7;
  transform: none;
}

.plans-panel {
  background: var(--vy-surface);
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  padding: 18px;
  box-shadow: var(--vy-shadow-sm);
}

.plans-panel h2 {
  font-size: 18px;
  margin-bottom: 14px;
}

.plans-panel label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 800;
  color: var(--vy-ink-2);
}

.plans-panel input,
.plans-panel textarea,
.plans-panel select,
.plan-modal input,
.plan-modal textarea,
.plan-modal select {
  width: 100%;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  padding: 11px 12px;
  font: inherit;
  color: var(--vy-ink);
  background: #fff;
}

.plans-form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.modal-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.full-field {
  grid-column: 1 / -1;
}

.benefit-box {
  margin-top: 4px;
  margin-bottom: 12px;
  padding: 14px;
  border: 1px solid rgba(242, 135, 5, 0.2);
  border-radius: 8px;
  background: rgba(242, 135, 5, 0.06);
}

.benefit-box h3 {
  margin-bottom: 10px;
  color: var(--vy-ink);
  font-size: 13px;
  font-weight: 900;
}

.benefit-box label {
  margin-bottom: 0;
}

.plans-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 50;
  background: rgba(31, 26, 20, 0.46);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 22px;
  font-family: var(--font-sans);
  color: var(--vy-ink);
}

.plan-modal {
  width: min(1120px, 100%);
  max-height: calc(100vh - 44px);
  overflow-y: auto;
  border-radius: 8px;
  border: 1px solid var(--vy-line);
  background: var(--vy-surface);
  padding: 20px;
  box-shadow: 0 24px 64px rgba(31, 26, 20, 0.24);
  font-family: var(--font-sans);
  color: var(--vy-ink);
}

.plan-modal h2,
.plan-modal h3 {
  font-family: var(--font-display);
  letter-spacing: -0.025em;
}

.plan-modal-grid {
  display: grid;
  grid-template-columns: minmax(340px, 0.9fr) minmax(420px, 1.1fr);
  gap: 18px;
  align-items: start;
}

.plan-editor-column,
.level-editor-column {
  min-width: 0;
}

.level-editor-column {
  border: 1px solid rgba(214, 204, 188, 0.85);
  border-radius: 8px;
  background: var(--vy-surface-2);
  padding: 14px;
}

.level-editor-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.level-editor-header h3 {
  color: var(--vy-ink);
  font-size: 15px;
  font-weight: 900;
}

.level-editor-header p {
  margin-top: 3px;
  color: var(--vy-ink-3);
  font-size: 12px;
  font-weight: 800;
}

.level-editor-header strong {
  color: var(--vy-orange-deep);
  font-size: 15px;
  font-weight: 900;
  white-space: nowrap;
}

.modal-level-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 430px;
  overflow-y: auto;
  padding-right: 4px;
}

.modal-level-row {
  display: grid;
  grid-template-columns: 76px minmax(120px, 1fr) minmax(120px, 1fr) auto;
  align-items: end;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: #fff;
}

.modal-level-row > span {
  align-self: center;
  color: var(--vy-ink);
  font-size: 13px;
  font-weight: 900;
}

.modal-level-row label {
  margin-bottom: 0;
}

.modal-level-row strong {
  align-self: center;
  color: var(--vy-ink);
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.plan-modal header,
.plan-modal footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.plan-modal header {
  margin-bottom: 16px;
}

.plan-modal header h2 {
  margin-top: 10px;
  font-size: 22px;
}

.plan-modal header > button {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: var(--vy-surface-2);
  color: var(--vy-ink-2);
  font-size: 22px;
  line-height: 1;
}

.plan-modal footer {
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--vy-line-2);
}

.plan-modal label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 800;
  color: var(--vy-ink-2);
}

.plan-save-button {
  min-height: 44px;
  padding: 0 18px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 12px;
  background: linear-gradient(135deg, var(--vy-orange) 0%, var(--vy-orange-deep) 100%);
  box-shadow: 0 12px 24px rgba(242, 135, 5, 0.24), inset 0 1px 0 rgba(255, 255, 255, 0.3);
  font-weight: 900;
}

.plan-save-button:hover:not(:disabled) {
  background: linear-gradient(135deg, #f59a22 0%, #e86900 100%);
  box-shadow: 0 14px 28px rgba(242, 116, 5, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.34);
  transform: translateY(-1px);
}

.plan-save-button:focus-visible {
  outline: 3px solid rgba(242, 135, 5, 0.24);
  outline-offset: 2px;
}

.plan-save-button:disabled {
  cursor: wait;
  opacity: 0.68;
  transform: none;
  box-shadow: none;
}

.plan-image-field {
  display: grid;
  grid-template-columns: 112px 1fr;
  gap: 14px;
  align-items: center;
  margin-bottom: 16px;
}

.plan-image-preview {
  width: 112px;
  aspect-ratio: 1 / 1;
  border-radius: 8px;
  border: 1px solid var(--vy-line);
  background: linear-gradient(135deg, rgba(242, 231, 196, 0.8), rgba(242, 135, 5, 0.18));
  color: var(--vy-orange-deep);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.plan-image-preview img,
.plan-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.file-button {
  min-height: 44px;
  margin: 0;
  padding: 0 14px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface-2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-direction: row;
  gap: 8px;
  color: var(--vy-ink);
  cursor: pointer;
}

.file-button input {
  display: none;
}

.plans-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.plan-row {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface-2);
  text-align: left;
  transition: border-color 0.16s ease, background 0.16s ease, transform 0.16s ease;
}

.plan-row:hover {
  border-color: rgba(242, 135, 5, 0.55);
  transform: translateY(-1px);
}

.plan-row.active {
  border-color: var(--vy-orange);
  background: rgba(242, 135, 5, 0.09);
}

.plan-row > span:first-child {
  display: grid;
  grid-template-columns: 44px 1fr;
  column-gap: 12px;
  align-items: center;
  min-width: 0;
}

.plan-thumb {
  grid-row: span 2;
  width: 44px;
  aspect-ratio: 1 / 1;
  border-radius: 8px;
  background: #fff;
  border: 1px solid var(--vy-line);
  color: var(--vy-orange-deep);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.plan-row strong,
.plan-row small {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.benefit-summary {
  grid-column: 2;
  font-weight: 800;
  color: var(--vy-orange-deep);
}

.plan-row small,
.empty-state {
  color: var(--vy-ink-3);
  margin-top: 4px;
}

.plan-row-actions {
  display: inline-flex;
  gap: 8px;
}

.icon-action {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  border: 1px solid var(--vy-line);
  background: #fff;
  color: var(--vy-ink-2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background 0.16s ease, color 0.16s ease, border-color 0.16s ease;
}

.icon-action:hover {
  background: var(--vy-ink);
  border-color: var(--vy-ink);
  color: #fff;
}

.icon-action.danger {
  color: var(--vy-danger);
}

.icon-action.danger:hover {
  background: var(--vy-danger);
  border-color: var(--vy-danger);
  color: #fff;
}

.plans-error {
  border: 1px solid rgba(196, 69, 42, 0.25);
  border-radius: 8px;
  padding: 12px 14px;
  color: var(--vy-danger);
  background: rgba(196, 69, 42, 0.08);
}

@media (max-width: 900px) {
  .plans-page {
    padding: 18px;
  }

  .plans-header,
  .header-actions,
  .plans-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .plan-save-button {
    width: 100%;
  }

  .plans-layout {
    grid-template-columns: 1fr;
  }

  .modal-grid,
  .plan-modal-grid,
  .plan-modal .plans-form-grid {
    grid-template-columns: 1fr;
  }

  .modal-level-list {
    max-height: none;
  }

  .modal-level-row {
    grid-template-columns: 1fr;
  }

  .full-field {
    grid-column: auto;
  }

  .plan-image-field {
    grid-template-columns: 1fr;
  }

  .plan-image-preview {
    width: 100%;
    max-width: 220px;
  }

  .plan-modal footer {
    align-items: stretch;
    flex-direction: column-reverse;
  }
}
</style>
