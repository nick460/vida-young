<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import $ from "jquery";
import select2 from "select2";
import "select2/dist/css/select2.css";
import { CalendarClock, CheckCircle2, Loader2, Lock, Pencil, Plus, RefreshCw, X } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";

select2($);

const loading = ref(false);
const saving = ref(false);
const error = ref("");
const activePeriod = ref(null);
const gestiones = ref([]);
const periodos = ref([]);
const selectedGestionId = ref("");
const editingGestionId = ref(null);
const gestionSelect = ref(null);
const mesSelect = ref(null);
const gestionForm = ref({
  anio: new Date().getFullYear(),
  nombre: ""
});
const periodoForm = ref({
  mes: new Date().getMonth() + 1,
  nombre: ""
});

const selectedGestion = computed(() =>
  gestiones.value.find((gestion) => Number(gestion.id) === Number(selectedGestionId.value))
);
const editingGestion = computed(() =>
  gestiones.value.find((gestion) => Number(gestion.id) === Number(editingGestionId.value))
);

const mesesDisponibles = [
  { value: 1, label: "Enero" },
  { value: 2, label: "Febrero" },
  { value: 3, label: "Marzo" },
  { value: 4, label: "Abril" },
  { value: 5, label: "Mayo" },
  { value: 6, label: "Junio" },
  { value: 7, label: "Julio" },
  { value: 8, label: "Agosto" },
  { value: 9, label: "Septiembre" },
  { value: 10, label: "Octubre" },
  { value: 11, label: "Noviembre" },
  { value: 12, label: "Diciembre" }
];

function formatDate(value) {
  if (!value) return "Sin fecha";
  return new Date(`${value}T00:00:00`).toLocaleDateString("es-BO", {
    day: "2-digit",
    month: "short",
    year: "numeric"
  });
}

function periodLabel(periodo) {
  if (!periodo) return "Sin periodo activo";
  return `${periodo.nombre} - Gestion ${periodo.gestion?.anio || ""}`;
}

function destroySelect2(selectRef, namespace) {
  if (!selectRef.value) return;
  const element = $(selectRef.value);
  if (element.hasClass("select2-hidden-accessible")) {
    element.off(`change.${namespace}`);
    element.select2("destroy");
  }
}

function syncSelect2Value(selectRef, value) {
  if (!selectRef.value) return;
  const element = $(selectRef.value);
  if (element.hasClass("select2-hidden-accessible") && element.val() !== String(value || "")) {
    element.val(value || null).trigger("change.select2");
  }
}

async function initGestionSelect2() {
  await nextTick();
  if (!gestionSelect.value) return;

  destroySelect2(gestionSelect, "gestiones");
  const element = $(gestionSelect.value);
  element
    .select2({
      width: "100%",
      placeholder: "Selecciona una gestion",
      allowClear: false,
      dropdownParent: $(".gestion-page"),
      language: {
        noResults: () => "Sin resultados",
        searching: () => "Buscando..."
      }
    })
    .val(selectedGestionId.value || null)
    .trigger("change.select2");

  element.on("change.gestiones", async () => {
    selectedGestionId.value = element.val() || "";
    await loadPeriodos();
  });
}

async function initMesSelect2() {
  await nextTick();
  if (!mesSelect.value) return;

  destroySelect2(mesSelect, "meses");
  const element = $(mesSelect.value);
  element
    .select2({
      width: "100%",
      placeholder: "Selecciona un mes",
      allowClear: false,
      dropdownParent: $(".gestion-page"),
      language: {
        noResults: () => "Sin resultados",
        searching: () => "Buscando..."
      }
    })
    .val(String(periodoForm.value.mes || ""))
    .trigger("change.select2");

  element.on("change.meses", () => {
    periodoForm.value.mes = Number(element.val() || new Date().getMonth() + 1);
  });
}

async function initSelect2Controls() {
  await initGestionSelect2();
  await initMesSelect2();
}

async function loadAll() {
  loading.value = true;
  error.value = "";
  try {
    const [gestionesData, activeData] = await Promise.all([
      apiRequest("/api/gestiones"),
      apiRequest("/api/gestiones/periodos/activo")
    ]);
    gestiones.value = Array.isArray(gestionesData) ? gestionesData : [];
    activePeriod.value = activeData;
    if (!selectedGestionId.value) {
      selectedGestionId.value = String(activeData?.gestion?.id || gestiones.value[0]?.id || "");
    }
    await loadPeriodos();
    await initSelect2Controls();
  } catch (exception) {
    error.value = exception.message || "No se pudieron cargar las gestiones.";
  } finally {
    loading.value = false;
  }
}

async function loadPeriodos() {
  periodos.value = [];
  if (!selectedGestionId.value) return;
  periodos.value = await apiRequest(`/api/gestiones/${selectedGestionId.value}/periodos`);
}

async function createGestion() {
  if (saving.value) return;
  saving.value = true;
  error.value = "";
  try {
    const isEditing = Boolean(editingGestionId.value);
    const gestion = await apiRequest(isEditing ? `/api/gestiones/${editingGestionId.value}` : "/api/gestiones", {
      method: isEditing ? "PUT" : "POST",
      body: JSON.stringify(gestionForm.value)
    });
    selectedGestionId.value = String(gestion.id);
    resetGestionForm();
    await loadAll();
  } catch (exception) {
    error.value = exception.message || "No se pudo guardar la gestion.";
  } finally {
    saving.value = false;
  }
}

function editGestion(gestion) {
  editingGestionId.value = gestion.id;
  gestionForm.value = {
    anio: gestion.anio,
    nombre: gestion.nombre || ""
  };
}

function resetGestionForm() {
  editingGestionId.value = null;
  gestionForm.value = {
    anio: new Date().getFullYear(),
    nombre: ""
  };
}

async function createPeriodo() {
  if (!selectedGestionId.value || saving.value) return;
  saving.value = true;
  error.value = "";
  try {
    await apiRequest(`/api/gestiones/${selectedGestionId.value}/periodos`, {
      method: "POST",
      body: JSON.stringify(periodoForm.value)
    });
    periodoForm.value.nombre = "";
    await loadPeriodos();
  } catch (exception) {
    error.value = exception.message || "No se pudo crear el periodo.";
  } finally {
    saving.value = false;
  }
}

async function activatePeriodo(periodo) {
  if (saving.value || periodo.estadoPeriodo === "ACTIVO") return;
  saving.value = true;
  error.value = "";
  try {
    await apiRequest(`/api/gestiones/periodos/${periodo.id}/activar`, { method: "PUT" });
    await loadAll();
  } catch (exception) {
    error.value = exception.message || "No se pudo activar el periodo.";
  } finally {
    saving.value = false;
  }
}

async function closeActivePeriodo() {
  if (saving.value || !activePeriod.value) return;
  saving.value = true;
  error.value = "";
  try {
    await apiRequest("/api/gestiones/periodos/activo/cerrar", { method: "PUT" });
    await loadAll();
  } catch (exception) {
    error.value = exception.message || "No se pudo cerrar el periodo activo.";
  } finally {
    saving.value = false;
  }
}

watch(gestiones, initGestionSelect2, { deep: true });
watch(selectedGestionId, (value) => syncSelect2Value(gestionSelect, value));
watch(() => periodoForm.value.mes, (value) => syncSelect2Value(mesSelect, value));

onMounted(loadAll);

onBeforeUnmount(() => {
  destroySelect2(gestionSelect, "gestiones");
  destroySelect2(mesSelect, "meses");
});
</script>

<template>
  <section class="gestion-page">
    <header class="gestion-header">
      <div>
        <p class="eyebrow">Control operativo</p>
        <h1>Gestiones y meses</h1>
        <p>Las ventas, activaciones, recompensas y movimientos se registran contra el mes activo.</p>
      </div>
      <button class="icon-button" type="button" :disabled="loading" @click="loadAll" title="Actualizar">
        <RefreshCw :size="18" :class="{ spin: loading }" />
      </button>
    </header>

    <p v-if="error" class="alert">{{ error }}</p>

    <div class="active-band">
      <CalendarClock :size="28" />
      <div>
        <span>Periodo activo</span>
        <strong>{{ periodLabel(activePeriod) }}</strong>
        <small v-if="activePeriod">
          {{ formatDate(activePeriod.fechaInicio) }} al {{ formatDate(activePeriod.fechaFin) }}
        </small>
      </div>
      <button type="button" class="secondary-button" :disabled="saving || !activePeriod" @click="closeActivePeriodo">
        <Lock :size="16" />
        Cerrar activo
      </button>
    </div>

    <div class="forms-grid">
      <form class="tool-panel" @submit.prevent="createGestion">
        <div class="panel-title">
          <h2>{{ editingGestionId ? "Editar gestion" : "Nueva gestion" }}</h2>
          <button
            v-if="editingGestionId"
            class="icon-button small"
            type="button"
            :disabled="saving"
            title="Cancelar edicion"
            @click="resetGestionForm"
          >
            <X :size="16" />
          </button>
        </div>
        <p v-if="editingGestion" class="helper-text">Editando {{ editingGestion.nombre }}.</p>
        <label>
          Anio
          <input v-model.number="gestionForm.anio" type="number" min="2020" max="2100" required />
        </label>
        <label>
          Nombre
          <input v-model.trim="gestionForm.nombre" type="text" placeholder="Gestion 2026" />
        </label>
        <button type="submit" :disabled="saving">
          <Loader2 v-if="saving" :size="16" class="spin" />
          <Pencil v-else-if="editingGestionId" :size="16" />
          <Plus v-else :size="16" />
          {{ editingGestionId ? "Actualizar gestion" : "Guardar gestion" }}
        </button>
      </form>

      <form class="tool-panel" @submit.prevent="createPeriodo">
        <h2>Nuevo mes/proceso</h2>
        <label>
          Gestion
          <select ref="gestionSelect" v-model="selectedGestionId" required>
            <option value="" disabled>Selecciona una gestion</option>
            <option v-for="gestion in gestiones" :key="gestion.id" :value="gestion.id">
              {{ gestion.nombre }} ({{ gestion.anio }})
            </option>
          </select>
        </label>
        <label>
          Mes
          <select ref="mesSelect" v-model.number="periodoForm.mes" required>
            <option v-for="mes in mesesDisponibles" :key="mes.value" :value="mes.value">
              {{ mes.label }}
            </option>
          </select>
        </label>
        <label>
          Nombre
          <input v-model.trim="periodoForm.nombre" type="text" placeholder="Proceso Julio 2026" />
        </label>
        <button type="submit" :disabled="saving || !selectedGestionId">
          <Loader2 v-if="saving" :size="16" class="spin" />
          <Plus v-else :size="16" />
          Guardar mes
        </button>
      </form>
    </div>

    <section class="periods-section">
      <div class="section-title">
        <div>
          <h2>Meses de {{ selectedGestion?.nombre || "la gestion" }}</h2>
          <p>Solo un mes puede estar activo. Al activar otro, el activo anterior queda cerrado.</p>
        </div>
        <button
          v-if="selectedGestion"
          type="button"
          class="secondary-button"
          :disabled="saving"
          @click="editGestion(selectedGestion)"
        >
          <Pencil :size="16" />
          Editar gestion
        </button>
      </div>

      <div class="period-table">
        <div class="table-row table-head">
          <span>Mes</span>
          <span>Rango</span>
          <span>Estado</span>
          <span>Accion</span>
        </div>
        <div v-if="loading" class="empty-state">
          <Loader2 :size="20" class="spin" />
          Cargando periodos
        </div>
        <div v-else-if="!periodos.length" class="empty-state">
          No hay meses creados para esta gestion.
        </div>
        <div v-for="periodo in periodos" v-else :key="periodo.id" class="table-row">
          <strong>{{ periodo.nombre }}</strong>
          <span>{{ formatDate(periodo.fechaInicio) }} al {{ formatDate(periodo.fechaFin) }}</span>
          <span class="status" :class="periodo.estadoPeriodo.toLowerCase()">
            <CheckCircle2 v-if="periodo.estadoPeriodo === 'ACTIVO'" :size="15" />
            {{ periodo.estadoPeriodo }}
          </span>
          <button
            type="button"
            class="secondary-button compact"
            :disabled="saving || periodo.estadoPeriodo === 'ACTIVO'"
            @click="activatePeriodo(periodo)"
          >
            Activar
          </button>
        </div>
      </div>
    </section>
  </section>
</template>

<style scoped>
.gestion-page {
  display: grid;
  gap: 1rem;
  padding: 1rem;
}

.gestion-header,
.active-band,
.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}

.gestion-header h1,
.tool-panel h2,
.section-title h2 {
  margin: 0;
  color: #172033;
}

.gestion-header p,
.section-title p,
.active-band small {
  margin: 0.25rem 0 0;
  color: #64748b;
}

.eyebrow {
  margin: 0;
  color: #0f766e;
  font-size: 0.78rem;
  font-weight: 800;
  text-transform: uppercase;
}

.icon-button,
button {
  border: none;
  cursor: pointer;
}

.icon-button {
  display: inline-grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #e2e8f0;
  color: #172033;
}

.icon-button.small {
  width: 32px;
  height: 32px;
}

.active-band,
.tool-panel,
.periods-section {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.active-band {
  padding: 1rem;
}

.active-band > div {
  display: grid;
  gap: 0.15rem;
  flex: 1;
}

.active-band span {
  color: #64748b;
  font-size: 0.82rem;
}

.active-band strong {
  color: #172033;
  font-size: 1.2rem;
}

.forms-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.tool-panel {
  display: grid;
  gap: 0.8rem;
  padding: 1rem;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}

.helper-text {
  margin: -0.35rem 0 0;
  color: #64748b;
  font-size: 0.84rem;
}

label {
  display: grid;
  gap: 0.35rem;
  color: #334155;
  font-size: 0.85rem;
  font-weight: 700;
}

input,
select {
  min-height: 42px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 0.65rem 0.75rem;
  color: #172033;
  font: inherit;
}

:deep(.select2-container--default .select2-selection--single) {
  min-height: 42px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  display: flex;
  align-items: center;
}

:deep(.select2-container--default .select2-selection--single .select2-selection__rendered) {
  padding-left: 0.75rem;
  padding-right: 2rem;
  color: #172033;
  font-size: 0.95rem;
  line-height: 42px;
}

:deep(.select2-container--default .select2-selection--single .select2-selection__arrow) {
  height: 42px;
  right: 0.45rem;
}

:deep(.select2-container--default.select2-container--open .select2-selection--single) {
  border-color: #0f766e;
}

:deep(.select2-dropdown) {
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  overflow: hidden;
}

:deep(.select2-search--dropdown) {
  padding: 0.5rem;
}

:deep(.select2-container--default .select2-search--dropdown .select2-search__field) {
  min-height: 36px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  outline: 0;
  padding: 0 0.65rem;
}

:deep(.select2-results__option) {
  padding: 0.55rem 0.75rem;
}

:deep(.select2-container--default .select2-results__option--highlighted.select2-results__option--selectable) {
  background: #0f766e;
  color: #ffffff;
}

button:not(.icon-button) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  min-height: 40px;
  border-radius: 8px;
  padding: 0.65rem 0.9rem;
  background: #0f766e;
  color: #ffffff;
  font-weight: 800;
}

.secondary-button {
  background: #e2e8f0 !important;
  color: #172033 !important;
}

.compact {
  min-height: 34px !important;
  padding: 0.45rem 0.65rem !important;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.periods-section {
  padding: 1rem;
}

.period-table {
  display: grid;
  gap: 0.45rem;
  margin-top: 0.8rem;
}

.table-row {
  display: grid;
  grid-template-columns: 1.2fr 1.5fr 0.8fr 0.7fr;
  align-items: center;
  gap: 0.75rem;
  min-height: 54px;
  border-bottom: 1px solid #e2e8f0;
  color: #334155;
}

.table-head {
  min-height: 36px;
  color: #64748b;
  font-size: 0.78rem;
  font-weight: 800;
  text-transform: uppercase;
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 0.3rem;
  width: fit-content;
  border-radius: 999px;
  padding: 0.25rem 0.55rem;
  background: #f1f5f9;
  color: #475569;
  font-size: 0.78rem;
  font-weight: 800;
}

.status.activo {
  background: #dcfce7;
  color: #166534;
}

.status.cerrado {
  background: #fee2e2;
  color: #991b1b;
}

.alert {
  margin: 0;
  border-radius: 8px;
  background: #fee2e2;
  color: #991b1b;
  padding: 0.8rem 1rem;
  font-weight: 700;
}

.empty-state {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem 0;
  color: #64748b;
}

.spin {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 760px) {
  .gestion-header,
  .active-band,
  .section-title {
    align-items: stretch;
    flex-direction: column;
  }

  .forms-grid {
    grid-template-columns: 1fr;
  }

  .table-row {
    grid-template-columns: 1fr;
    align-items: start;
    gap: 0.35rem;
    padding: 0.75rem 0;
  }

  .table-head {
    display: none;
  }
}
</style>
