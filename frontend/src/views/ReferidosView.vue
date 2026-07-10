<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import {
  CircleMinus,
  Network,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  UserPlus,
  Users,
  Wallet
} from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import { VyAvatar } from "../components/ui.js";
import VyAutocompleteSelect from "../components/VyAutocompleteSelect.vue";

const loading = ref(false);
const saving = ref(false);
const error = ref("");
const personas = ref([]);
const planes = ref([]);
const referidos = ref([]);
const editingReferidoId = ref(null);
const useExistingPerson = ref(false);
const esCabezaRed = ref(false);

const form = reactive({
  personaId: "",
  patrocinadorId: "",
  planId: "",
  nombres: "",
  apellidos: "",
  documento: "",
  email: "",
  telefono: ""
});

const registeredPersonaIds = computed(() => new Set(referidos.value.map((item) => item.persona?.id).filter(Boolean)));
const availablePersonas = computed(() => personas.value.filter((persona) => {
  if (editingReferidoId.value && Number(form.personaId) === persona.id) return true;
  return !registeredPersonaIds.value.has(persona.id);
}));
const sponsorOptions = computed(() => personas.value.filter((persona) => persona.id !== Number(form.personaId)));
const availablePersonaSelectOptions = computed(() => availablePersonas.value.map((persona) => ({
  ...persona,
  selectorLabel: personLabel(persona)
})));
const sponsorSelectOptions = computed(() => sponsorOptions.value.map((persona) => ({
  ...persona,
  selectorLabel: personLabel(persona)
})));
const planSelectOptions = computed(() => planes.value.map((plan) => ({
  ...plan,
  selectorLabel: `${plan.nombre} - ${plan.nivelesAlcance} niveles`
})));

const summary = computed(() => ({
  total: referidos.value.length,
  patrocinadores: new Set(referidos.value.map((item) => item.patrocinador?.id).filter(Boolean)).size,
  planes: new Set(referidos.value.map((item) => item.plan?.id).filter(Boolean)).size,
  efectivo: compensationSummary.value.efectivo,
  productos: compensationSummary.value.productos
}));

const compensationSummary = computed(() => referidos.value.reduce((summaryValue, referido) => {
  const rows = compensationRowsForSponsor(referido.patrocinador?.id, referido.plan?.id);
  summaryValue.efectivo += rows.reduce((sum, row) => sum + row.efectivo, 0);
  summaryValue.productos += rows.reduce((sum, row) => sum + row.productos, 0);
  return summaryValue;
}, { efectivo: 0, productos: 0 }));

const previewRows = computed(() => compensationRowsForSponsor(Number(form.patrocinadorId), Number(form.planId)));

function optionalText(value) {
  const normalized = String(value || "").trim();
  return normalized ? normalized : null;
}

function fullName(persona) {
  return `${persona?.nombres || ""} ${persona?.apellidos || ""}`.trim() || "Persona";
}

function personLabel(persona) {
  return `${fullName(persona)}${persona?.documento ? ` - ${persona.documento}` : ""}`;
}

function initials(persona) {
  return `${persona?.nombres?.[0] || ""}${persona?.apellidos?.[0] || ""}`.toUpperCase() || "P";
}

function money(value) {
  return Number(value || 0).toFixed(2);
}

function planById(planId) {
  return planes.value.find((plan) => plan.id === Number(planId)) || null;
}

function referidoByPersonaId(personaId) {
  return referidos.value.find((item) => item.persona?.id === Number(personaId)) || null;
}

function planIngresoByPersonaId(personaId) {
  return referidoByPersonaId(personaId)?.plan || null;
}

function levelReward(plan, level) {
  const nivel = [...(plan?.niveles || [])].find((item) => Number(item.numeroNivel) === Number(level));
  return {
    efectivo: Number(nivel?.porcentajeComision || 0),
    productos: Number(nivel?.valorProductosBeneficio || 0)
  };
}

function compensationRowsForSponsor(sponsorId, entryPlanId) {
  const entryPlan = planById(entryPlanId);

  if (!sponsorId || !entryPlan) return [];

  const rows = [];
  const visited = new Set();
  let currentPersonaId = Number(sponsorId);
  let level = 1;
  const reach = Number(entryPlan.nivelesAlcance || 0);

  while (currentPersonaId && !visited.has(currentPersonaId) && level <= reach) {
    visited.add(currentPersonaId);

    const persona = personas.value.find((item) => item.id === currentPersonaId)
      || referidos.value.find((item) => item.persona?.id === currentPersonaId)?.persona
      || referidos.value.find((item) => item.patrocinador?.id === currentPersonaId)?.patrocinador;
    const sponsorPlan = planIngresoByPersonaId(currentPersonaId);
    const sponsorReach = Number(sponsorPlan?.nivelesAlcance || 0);
    const reward = levelReward(entryPlan, level);

    const pays = sponsorReach >= level;

    rows.push({
      level,
      persona,
      plan: entryPlan,
      alcance: reach,
      efectivo: pays ? reward.efectivo : 0,
      productos: pays ? reward.productos : 0,
      paga: pays,
      motivo: pays ? "" : "No corresponde por nivel del plan"
    });

    const currentReferral = referidoByPersonaId(currentPersonaId);
    currentPersonaId = currentReferral?.patrocinador?.id ? Number(currentReferral.patrocinador.id) : null;
    level += 1;
  }

  return rows;
}

function referralCompensationTotal(referido) {
  const rows = compensationRowsForSponsor(referido.patrocinador?.id, referido.plan?.id);
  return rows.reduce((sum, row) => sum + row.efectivo + row.productos, 0);
}

function resetForm() {
  editingReferidoId.value = null;
  useExistingPerson.value = false;
  esCabezaRed.value = false;
  Object.assign(form, {
    personaId: "",
    patrocinadorId: "",
    planId: "",
    nombres: "",
    apellidos: "",
    documento: "",
    email: "",
    telefono: ""
  });
}

async function loadAll() {
  loading.value = true;
  error.value = "";

  try {
    const [personasData, planesData, referidosData] = await Promise.all([
      apiRequest("/api/personas"),
      apiRequest("/api/planes"),
      apiRequest("/api/referidos")
    ]);
    personas.value = personasData;
    planes.value = planesData;
    referidos.value = referidosData;
  } catch (exception) {
    error.value = "No se pudieron cargar los referidos. Verifica backend y sesion.";
  } finally {
    loading.value = false;
  }
}

function editReferido(referido) {
  editingReferidoId.value = referido.id;
  useExistingPerson.value = true;
  esCabezaRed.value = !referido.patrocinador?.id;
  Object.assign(form, {
    personaId: referido.persona?.id || "",
    patrocinadorId: referido.patrocinador?.id || "",
    planId: referido.plan?.id || "",
    nombres: "",
    apellidos: "",
    documento: "",
    email: "",
    telefono: ""
  });
}

async function createPersonaIfNeeded() {
  if (useExistingPerson.value || editingReferidoId.value) {
    return Number(form.personaId);
  }

  const persona = await apiRequest("/api/personas", {
    method: "POST",
    body: JSON.stringify({
      nombres: form.nombres,
      apellidos: form.apellidos,
      documento: optionalText(form.documento),
      email: optionalText(form.email),
      telefono: optionalText(form.telefono)
    })
  });

  return persona.id;
}

async function saveReferido() {
  const isEditing = Boolean(editingReferidoId.value);

  try {
    if ((useExistingPerson.value || isEditing) && !form.personaId) {
      await showError("Selecciona la persona referida.");
      return;
    }

    if (!esCabezaRed.value && !form.patrocinadorId) {
      await showError("Selecciona el patrocinador.");
      return;
    }

    if (!form.planId) {
      await showError("Selecciona el plan.");
      return;
    }

    saving.value = true;
    const personaId = await createPersonaIfNeeded();
    const payload = {
      personaId,
      patrocinadorId: esCabezaRed.value ? null : Number(form.patrocinadorId),
      planId: Number(form.planId)
    };

    await apiRequest(isEditing ? `/api/referidos/${editingReferidoId.value}` : "/api/referidos", {
      method: isEditing ? "PUT" : "POST",
      body: JSON.stringify(payload)
    });

    await showSuccess(isEditing ? "Referido editado correctamente." : "Referido guardado correctamente.");
    resetForm();
    await loadAll();
  } catch (exception) {
    await showError(exception.message || "No se pudo guardar el referido.");
  } finally {
    saving.value = false;
  }
}

async function removeReferido(referido) {
  const result = await Swal.fire({
    title: "Quitar de la red",
    text: `${fullName(referido.persona)} dejara de figurar como referido activo. Sus referidos directos pasaran a su patrocinador inmediato. Esta accion no se puede revertir.`,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Aceptar y quitar",
    cancelButtonText: "Cancelar",
    confirmButtonColor: "#C4452A",
    cancelButtonColor: "#1F1A14"
  });

  if (!result.isConfirmed) return;

  try {
    await apiRequest(`/api/referidos/${referido.id}`, { method: "DELETE" });
    await showSuccess("Referido quitado correctamente.");
    await loadAll();
  } catch (exception) {
    await showError(exception.message || "No se pudo quitar el referido.");
  }
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

function showError(text) {
  return Swal.fire({
    title: "Revisa los datos",
    text,
    icon: "error",
    confirmButtonText: "Entendido",
    confirmButtonColor: "#F28705"
  });
}

onMounted(loadAll);
</script>

<template>
  <main class="referrals-page">
    <section class="referrals-header">
      <div>
        <span class="vy-chip vy-chip-orange"><Network :size="14" /> Red admin</span>
        <h1>Referidos</h1>
        <p>Registra quien patrocina a cada persona y que plan define su alcance de niveles.</p>
      </div>
      <button type="button" class="vy-btn vy-btn-ghost" :disabled="loading" @click="loadAll">
        <RefreshCw :size="16" /> Actualizar
      </button>
    </section>

    <p v-if="error" class="referrals-error">{{ error }}</p>

    <section class="summary-grid">
      <article class="summary-card">
        <Users :size="18" />
        <span>Referidos activos</span>
        <strong>{{ summary.total }}</strong>
      </article>
      <article class="summary-card">
        <UserPlus :size="18" />
        <span>Patrocinadores</span>
        <strong>{{ summary.patrocinadores }}</strong>
      </article>
      <article class="summary-card">
        <Network :size="18" />
        <span>Planes usados</span>
        <strong>{{ summary.planes }}</strong>
      </article>
      <article class="summary-card">
        <Wallet :size="18" />
        <span>Comisiones proyectadas</span>
        <strong>Bs. {{ money(summary.efectivo + summary.productos) }}</strong>
      </article>
    </section>

    <section class="referrals-layout">
      <form class="referrals-panel" @submit.prevent="saveReferido">
        <h2>{{ editingReferidoId ? "Editar referido" : "Nuevo referido" }}</h2>

        <div v-if="!editingReferidoId" class="mode-switch">
          <button type="button" :class="{ active: !useExistingPerson }" @click="useExistingPerson = false">
            Nueva persona
          </button>
          <button type="button" :class="{ active: useExistingPerson }" @click="useExistingPerson = true">
            Persona existente
          </button>
        </div>

        <label v-if="useExistingPerson || editingReferidoId">
          Persona referida
          <VyAutocompleteSelect
            v-model="form.personaId"
            :multiple="false"
            :options="availablePersonaSelectOptions"
            label-key="selectorLabel"
            value-key="id"
            placeholder="Buscar persona"
            empty-text="No hay personas disponibles"
          />
        </label>

        <div v-else class="person-grid">
          <label>
            Nombres
            <input v-model.trim="form.nombres" required maxlength="100" />
          </label>
          <label>
            Apellidos
            <input v-model.trim="form.apellidos" required maxlength="100" />
          </label>
          <label>
            Documento
            <input v-model.trim="form.documento" maxlength="30" />
          </label>
          <label>
            Email
            <input v-model.trim="form.email" type="email" maxlength="120" />
          </label>
          <label class="full-field">
            Telefono
            <input v-model.trim="form.telefono" maxlength="30" />
          </label>
        </div>

        <label>
          Tipo de ingreso
          <span class="head-toggle">
            <input v-model="esCabezaRed" type="checkbox" @change="form.patrocinadorId = esCabezaRed ? '' : form.patrocinadorId" />
            Cabeza de red, nadie lo refiere
          </span>
        </label>

        <label v-if="!esCabezaRed">
          Patrocinador
          <VyAutocompleteSelect
            v-model="form.patrocinadorId"
            :multiple="false"
            :options="sponsorSelectOptions"
            label-key="selectorLabel"
            value-key="id"
            placeholder="Buscar patrocinador"
            empty-text="No hay patrocinadores disponibles"
          />
        </label>

        <label>
          Plan
          <VyAutocompleteSelect
            v-model="form.planId"
            :multiple="false"
            :options="planSelectOptions"
            label-key="selectorLabel"
            value-key="id"
            placeholder="Buscar plan"
            empty-text="No hay planes disponibles"
          />
        </label>

        <section class="commission-preview">
          <div class="preview-heading">
            <h3>Comisiones segun plan de ingreso</h3>
            <span>Bs. {{ money(previewRows.reduce((sum, row) => sum + row.efectivo + row.productos, 0)) }}</span>
          </div>

          <div v-if="esCabezaRed" class="empty-state">Cabeza de red: no genera comision hacia arriba.</div>
          <div v-else-if="!form.planId" class="empty-state">Selecciona el plan de ingreso para calcular comisiones.</div>
          <div v-else-if="!form.patrocinadorId" class="empty-state">Selecciona un patrocinador para calcular niveles.</div>
          <div v-else-if="!previewRows.length" class="empty-state">No hay cadena de patrocinadores para calcular.</div>
          <div v-else class="commission-list">
            <article
              v-for="row in previewRows"
              :key="`${row.level}-${row.persona?.id || 'sin-persona'}`"
              class="commission-row"
              :class="{ skipped: !row.paga }"
            >
              <span>
                <strong>Nivel {{ row.level }} - {{ fullName(row.persona) }}</strong>
                <small v-if="row.paga">{{ row.plan?.nombre || "Plan de ingreso" }} · Nivel {{ row.level }} de {{ row.alcance || 0 }}</small>
                <small v-else>{{ row.motivo }} · Bs. 0.00</small>
              </span>
              <b>Bs. {{ money(row.efectivo + row.productos) }}</b>
            </article>
          </div>
        </section>

        <div class="form-actions">
          <button type="submit" class="vy-btn vy-btn-primary save-button" :disabled="saving">
            <Save :size="16" /> Guardar
          </button>
          <button v-if="editingReferidoId" type="button" class="vy-btn vy-btn-ghost" @click="resetForm">
            Cancelar
          </button>
        </div>
      </form>

      <section class="referrals-panel table-panel">
        <div class="table-heading">
          <h2>Red registrada</h2>
          <span>{{ referidos.length }} activos</span>
        </div>

        <div v-if="loading" class="empty-state">Cargando referidos...</div>
        <div v-else-if="!referidos.length" class="empty-state">No hay referidos registrados.</div>

        <div v-else class="referral-list">
          <article v-for="referido in referidos" :key="referido.id" class="referral-row">
            <div class="person-cell">
              <VyAvatar :name="initials(referido.persona)" :size="38" />
              <span>
                <strong>{{ fullName(referido.persona) }}</strong>
                <small>{{ referido.patrocinador ? `Referido por ${fullName(referido.patrocinador)}` : "Cabeza de red" }}</small>
              </span>
            </div>
            <div class="plan-cell">
              <strong>{{ referido.plan?.nombre || "Plan" }}</strong>
              <small>{{ referido.plan?.nivelesAlcance || 0 }} niveles de alcance</small>
              <small>Genera Bs. {{ money(referralCompensationTotal(referido)) }}</small>
            </div>
            <div class="row-actions">
              <button type="button" title="Editar referido" @click="editReferido(referido)">
                <Pencil :size="16" />
              </button>
              <button type="button" class="danger" title="Quitar referido" @click="removeReferido(referido)">
                <CircleMinus :size="16" />
              </button>
            </div>
          </article>
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
.referrals-page {
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.referrals-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.referrals-header h1 {
  margin-top: 10px;
  font-size: 30px;
}

.referrals-header p {
  margin-top: 6px;
  color: var(--vy-ink-2);
}

.summary-grid,
.referrals-layout {
  display: grid;
  gap: 16px;
}

.summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.summary-card,
.referrals-panel {
  background: var(--vy-surface);
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  box-shadow: var(--vy-shadow-sm);
}

.summary-card {
  padding: 16px;
}

.summary-card svg {
  color: var(--vy-orange-deep);
  margin-bottom: 10px;
}

.summary-card span {
  display: block;
  color: var(--vy-ink-3);
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

.summary-card strong {
  display: block;
  margin-top: 6px;
  font-size: 26px;
  font-family: var(--font-display);
}

.referrals-layout {
  grid-template-columns: minmax(300px, 0.85fr) minmax(420px, 1.15fr);
}

.referrals-panel {
  padding: 18px;
}

.referrals-panel h2 {
  font-size: 18px;
  margin-bottom: 14px;
}

.referrals-panel label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 800;
  color: var(--vy-ink-2);
}

.referrals-panel input,
.referrals-panel select {
  width: 100%;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  padding: 11px 12px;
  font: inherit;
  color: var(--vy-ink);
  background: #fff;
}

.head-toggle {
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface-2);
  color: var(--vy-ink);
  display: inline-flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 900;
}

.head-toggle input {
  width: 16px;
  height: 16px;
  padding: 0;
  accent-color: var(--vy-orange);
}

.mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
  padding: 4px;
  margin-bottom: 14px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface-2);
}

.mode-switch button {
  min-height: 36px;
  border-radius: 6px;
  font-weight: 800;
  color: var(--vy-ink-2);
}

.mode-switch button.active {
  background: var(--vy-ink);
  color: #fff;
}

.person-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.full-field {
  grid-column: 1 / -1;
}

.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

.commission-preview {
  margin-top: 14px;
  padding: 14px;
  border: 1px solid rgba(242, 135, 5, 0.22);
  border-radius: 8px;
  background: rgba(242, 135, 5, 0.06);
}

.preview-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.preview-heading h3 {
  font-size: 14px;
  font-weight: 900;
}

.preview-heading span {
  color: var(--vy-orange-deep);
  font-size: 13px;
  font-weight: 900;
  white-space: nowrap;
}

.commission-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.commission-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: #fff;
}

.commission-row span {
  min-width: 0;
}

.commission-row strong,
.commission-row small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.commission-row strong {
  font-size: 12px;
  font-weight: 900;
}

.commission-row small {
  margin-top: 3px;
  color: var(--vy-ink-3);
  font-size: 11px;
  font-weight: 800;
}

.commission-row b {
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.commission-row.skipped {
  border-color: rgba(196, 69, 42, 0.22);
  background: rgba(196, 69, 42, 0.05);
}

.commission-row.skipped b,
.commission-row.skipped small {
  color: var(--vy-danger);
}

.save-button {
  min-height: 44px;
  padding: 0 18px;
  font-weight: 900;
}

.table-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.table-heading span,
.empty-state {
  color: var(--vy-ink-3);
  font-size: 13px;
  font-weight: 700;
}

.referral-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.referral-row {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(150px, 0.8fr) auto;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface-2);
}

.person-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.person-cell span,
.plan-cell {
  min-width: 0;
}

.person-cell strong,
.person-cell small,
.plan-cell strong,
.plan-cell small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.person-cell small,
.plan-cell small {
  margin-top: 4px;
  color: var(--vy-ink-3);
  font-size: 12px;
}

.row-actions {
  display: inline-flex;
  gap: 8px;
}

.row-actions button {
  width: 34px;
  height: 34px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: #fff;
  color: var(--vy-ink-2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.row-actions button:hover {
  background: var(--vy-ink);
  color: #fff;
}

.row-actions button.danger {
  color: var(--vy-danger);
}

.row-actions button.danger:hover {
  background: var(--vy-danger);
  border-color: var(--vy-danger);
  color: #fff;
}

.referrals-error {
  border: 1px solid rgba(196, 69, 42, 0.25);
  border-radius: 8px;
  padding: 12px 14px;
  color: var(--vy-danger);
  background: rgba(196, 69, 42, 0.08);
}

@media (max-width: 980px) {
  .referrals-page {
    padding: 18px;
  }

  .referrals-header,
  .form-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .summary-grid,
  .referrals-layout,
  .person-grid {
    grid-template-columns: 1fr;
  }

  .referral-row {
    grid-template-columns: 1fr;
  }
}
</style>
