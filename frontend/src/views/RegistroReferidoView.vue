<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { CheckCircle2, RefreshCw, UserCheck, XCircle } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";

const loading = ref(false);
const saving = ref(false);
const error = ref("");
const preinscripciones = ref([]);
const personas = ref([]);
const planes = ref([]);
const referidos = ref([]);
const selectedId = ref("");

const selected = computed(() => preinscripciones.value.find((item) => item.id === Number(selectedId.value)) || null);
const pendingCount = computed(() => preinscripciones.value.filter((item) => item.estadoPreinscripcion === "PENDIENTE").length);

const form = reactive({
  patrocinadorId: "",
  planId: "",
  nombres: "",
  apellidos: "",
  documento: "",
  telefono: "",
  email: ""
});

function fullName(persona) {
  return `${persona?.nombres || ""} ${persona?.apellidos || ""}`.trim() || "Persona";
}

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

async function loadAll() {
  loading.value = true;
  error.value = "";

  try {
    const [preinsData, personasData, planesData, referidosData] = await Promise.all([
      apiRequest("/api/preinscripciones-referidos?estado=PENDIENTE"),
      apiRequest("/api/personas"),
      apiRequest("/api/planes"),
      apiRequest("/api/referidos")
    ]);
    preinscripciones.value = preinsData;
    personas.value = personasData;
    planes.value = planesData;
    referidos.value = referidosData;

    if (!selectedId.value && preinscripciones.value.length) {
      selectPreinscripcion(preinscripciones.value[0]);
    } else if (selected.value) {
      selectPreinscripcion(selected.value);
    }
  } catch (exception) {
    error.value = exception.message || "No se pudieron cargar las preinscripciones.";
  } finally {
    loading.value = false;
  }
}

function selectPreinscripcion(item) {
  selectedId.value = String(item.id);
  Object.assign(form, {
    patrocinadorId: item.patrocinador?.id || "",
    planId: item.plan?.id || "",
    nombres: item.nombres || "",
    apellidos: item.apellidos || "",
    documento: item.documento || "",
    telefono: item.telefono || "",
    email: item.email || ""
  });
}

function planById(planId) {
  return planes.value.find((plan) => plan.id === Number(planId)) || null;
}

function personaById(personaId) {
  return personas.value.find((persona) => persona.id === Number(personaId)) || null;
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

    const persona = personaById(currentPersonaId)
      || referidoByPersonaId(currentPersonaId)?.persona
      || referidos.value.find((item) => item.patrocinador?.id === currentPersonaId)?.patrocinador;
    const sponsorPlan = planIngresoByPersonaId(currentPersonaId);
    const sponsorReach = Number(sponsorPlan?.nivelesAlcance || 0);
    const reward = levelReward(entryPlan, level);
    const pays = sponsorReach >= level;

    rows.push({
      level,
      persona,
      efectivo: pays ? reward.efectivo : 0,
      productos: pays ? reward.productos : 0,
      paga: pays,
      motivo: pays ? "" : "No corresponde por nivel del plan"
    });

    currentPersonaId = referidoByPersonaId(currentPersonaId)?.patrocinador?.id || null;
    level += 1;
  }

  return rows;
}

function escapeHtml(value) {
  return String(value || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function confirmationHtml() {
  const plan = planById(form.planId);
  const sponsor = personaById(form.patrocinadorId);
  const rows = compensationRowsForSponsor(form.patrocinadorId, form.planId);
  const qp = Number(plan?.qp || 0);
  const precioPlan = Number(plan?.precio || 0);
  const rewardRows = rows.map((row) => {
    const total = Number(row.efectivo || 0) + Number(row.productos || 0);
    const detail = row.paga
      ? `Efectivo Bs. ${money(row.efectivo)} · Productos Bs. ${money(row.productos)}`
      : row.motivo;

    return `
      <div style="display:flex;justify-content:space-between;gap:12px;padding:10px;border:1px solid #eadfcd;border-radius:8px;margin-top:8px;text-align:left">
        <span>
          <b>Nivel ${row.level} · ${escapeHtml(fullName(row.persona))}</b>
          <small style="display:block;color:#766b5d;margin-top:3px">${escapeHtml(detail)}</small>
        </span>
        <b style="white-space:nowrap;color:${row.paga ? "#1f1a14" : "#c4452a"}">Bs. ${money(total)}</b>
      </div>
    `;
  }).join("");

  return `
    <div style="text-align:left">
      <div style="padding:12px;border-radius:8px;background:#fff7e7;border:1px solid #f3d6a4;margin-bottom:10px">
        <b>Ingreso a registrar</b>
        <div style="margin-top:6px;color:#4d4337">${escapeHtml(form.nombres)} ${escapeHtml(form.apellidos)} entrara con el plan <b>${escapeHtml(plan?.nombre || "Plan")}</b>.</div>
        <div style="margin-top:6px;color:#4d4337">Caja empresa recibira <b>Bs. ${money(precioPlan)}</b> por el costo del plan.</div>
      </div>
      <div style="padding:12px;border-radius:8px;background:#f7f3ea;border:1px solid #eadfcd;margin-bottom:10px">
        <b>Movimiento QP</b>
        <div style="margin-top:6px;color:#4d4337">Se acreditaran <b>${money(qp)} QP</b> a ${escapeHtml(fullName(sponsor))} por la afiliacion.</div>
      </div>
      <b>Recompensas por niveles</b>
      ${rewardRows || '<p style="margin-top:8px;color:#766b5d">No se generaran recompensas hacia arriba.</p>'}
    </div>
  `;
}

async function validateSelected() {
  if (!selected.value) return;

  try {
    const confirmation = await Swal.fire({
      title: "Confirmar validacion",
      html: confirmationHtml(),
      icon: "info",
      showCancelButton: true,
      confirmButtonText: "Si, validar",
      cancelButtonText: "Cancelar",
      confirmButtonColor: "#F28705",
      cancelButtonColor: "#1F1A14",
      width: 680
    });

    if (!confirmation.isConfirmed) return;

    saving.value = true;

    await apiRequest(`/api/preinscripciones-referidos/${selected.value.id}/validar`, {
      method: "POST",
      body: JSON.stringify({
        patrocinadorId: Number(form.patrocinadorId),
        planId: Number(form.planId),
        nombres: form.nombres,
        apellidos: form.apellidos,
        documento: form.documento,
        telefono: form.telefono,
        email: form.email
      })
    });

    await Swal.fire({
      title: "Referido registrado",
      text: "La preinscripcion fue validada y la persona ya entro a la red.",
      icon: "success",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });

    selectedId.value = "";
    await loadAll();
  } catch (exception) {
    await Swal.fire({
      title: "No se pudo validar",
      text: exception.message || "Revisa los datos de la preinscripcion.",
      icon: "error",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
  } finally {
    saving.value = false;
  }
}

async function rejectSelected() {
  if (!selected.value) return;

  const result = await Swal.fire({
    title: "Rechazar preinscripcion",
    input: "textarea",
    inputLabel: "Observacion",
    inputPlaceholder: "Motivo del rechazo",
    showCancelButton: true,
    confirmButtonText: "Rechazar",
    cancelButtonText: "Cancelar",
    confirmButtonColor: "#C4452A",
    cancelButtonColor: "#1F1A14"
  });

  if (!result.isConfirmed) return;

  await apiRequest(`/api/preinscripciones-referidos/${selected.value.id}/rechazar`, {
    method: "POST",
    body: JSON.stringify({ observacion: result.value || "" })
  });

  selectedId.value = "";
  await loadAll();
}

onMounted(loadAll);
</script>

<template>
  <main class="registro-referido-page">
    <section class="page-header">
      <div>
        <span class="vy-chip vy-chip-orange"><UserCheck :size="14" /> VENTANILLA</span>
        <h1>Registro referido</h1>
        <p>Valida las preinscripciones publicas y completa el ingreso a la red con el plan correcto.</p>
      </div>
      <button type="button" class="refresh-button" :disabled="loading" @click="loadAll">
        <RefreshCw :size="16" />
        Actualizar
      </button>
    </section>

    <p v-if="error" class="error-box">{{ error }}</p>

    <section class="layout-grid">
      <aside class="panel request-list">
        <header>
          <h2>Solicitudes pendientes</h2>
          <span>{{ pendingCount }}</span>
        </header>

        <button
          v-for="item in preinscripciones"
          :key="item.id"
          type="button"
          class="request-row"
          :class="{ active: item.id === Number(selectedId) }"
          @click="selectPreinscripcion(item)"
        >
          <strong>{{ item.nombres }} {{ item.apellidos }}</strong>
          <small>CI {{ item.documento }} - {{ item.telefono }}</small>
          <small>Refiere {{ fullName(item.patrocinador) }} - {{ formatDate(item.fechaRegistro) }}</small>
        </button>

        <p v-if="!preinscripciones.length && !loading" class="empty-state">No hay preinscripciones pendientes.</p>
        <p v-if="loading" class="empty-state">Cargando solicitudes...</p>
      </aside>

      <form v-if="selected" class="panel validation-form" @submit.prevent="validateSelected">
        <header>
          <div>
            <h2>Validar datos</h2>
            <p>Solicitud #{{ selected.id }}</p>
          </div>
          <span class="status-pill">{{ selected.estadoPreinscripcion }}</span>
        </header>

        <div class="form-grid">
          <label>
            CI
            <input v-model.trim="form.documento" required maxlength="30" />
          </label>
          <label>
            Celular
            <input v-model.trim="form.telefono" required maxlength="30" />
          </label>
          <label>
            Nombres
            <input v-model.trim="form.nombres" required maxlength="100" />
          </label>
          <label>
            Apellidos
            <input v-model.trim="form.apellidos" required maxlength="100" />
          </label>
          <label class="full-field">
            Email
            <input v-model.trim="form.email" type="email" maxlength="120" />
          </label>
          <label class="full-field">
            Persona que refiere
            <select v-model="form.patrocinadorId" required>
              <option value="">Selecciona patrocinador</option>
              <option v-for="persona in personas" :key="persona.id" :value="persona.id">
                {{ fullName(persona) }} - {{ persona.documento }}
              </option>
            </select>
          </label>
          <label class="full-field">
            Plan de ingreso
            <select v-model="form.planId" required>
              <option value="">Selecciona plan</option>
              <option v-for="plan in planes" :key="plan.id" :value="plan.id">
                {{ plan.nombre }} - QP {{ Number(plan.qp || 0).toFixed(2) }}
              </option>
            </select>
          </label>
        </div>

        <footer>
          <button type="button" class="reject-button" :disabled="saving" @click="rejectSelected">
            <XCircle :size="16" />
            Rechazar
          </button>
          <button type="submit" class="validate-button" :disabled="saving">
            <CheckCircle2 :size="16" />
            {{ saving ? "Validando..." : "Validar y registrar" }}
          </button>
        </footer>
      </form>

      <section v-else class="panel empty-panel">
        <UserCheck :size="36" />
        <h2>Selecciona una solicitud</h2>
        <p>Cuando haya preinscripciones pendientes, selecciona una para validar datos y asignar plan.</p>
      </section>
    </section>
  </main>
</template>

<style scoped>
.registro-referido-page {
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header,
.validation-form header,
.request-list header,
.validation-form footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-header h1 {
  margin-top: 10px;
  font-size: 30px;
  font-weight: 900;
}

.page-header p,
.validation-form header p {
  margin-top: 5px;
  color: var(--vy-ink-2);
}

.refresh-button,
.validate-button,
.reject-button {
  min-height: 42px;
  padding: 0 16px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 900;
}

.refresh-button {
  border: 1px solid rgba(242, 135, 5, 0.34);
  background: #fff;
  color: var(--vy-orange-deep);
}

.layout-grid {
  display: grid;
  grid-template-columns: minmax(300px, 0.9fr) minmax(420px, 1.4fr);
  gap: 16px;
  align-items: start;
}

.panel {
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface);
  padding: 18px;
  box-shadow: var(--vy-shadow-sm);
}

.request-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.request-list h2,
.validation-form h2,
.empty-panel h2 {
  font-size: 18px;
  font-weight: 900;
}

.request-list header span,
.status-pill {
  padding: 5px 10px;
  border-radius: 999px;
  background: var(--vy-cream);
  color: var(--vy-orange-deep);
  font-size: 12px;
  font-weight: 900;
}

.request-row {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface-2);
  text-align: left;
}

.request-row.active {
  border-color: var(--vy-orange);
  background: rgba(242, 135, 5, 0.08);
}

.request-row strong,
.request-row small {
  display: block;
}

.request-row small {
  margin-top: 4px;
  color: var(--vy-ink-3);
  font-size: 12px;
  font-weight: 800;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 16px;
}

.full-field {
  grid-column: 1 / -1;
}

.validation-form label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--vy-ink-2);
  font-size: 13px;
  font-weight: 900;
}

.validation-form input,
.validation-form select {
  width: 100%;
  min-height: 44px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  padding: 0 12px;
  background: #fff;
  color: var(--vy-ink);
  font: inherit;
}

.validation-form footer {
  margin-top: 18px;
}

.reject-button {
  border: 1px solid rgba(196, 69, 42, 0.28);
  background: rgba(196, 69, 42, 0.08);
  color: var(--vy-danger);
}

.validate-button {
  border: 1px solid var(--vy-orange);
  background: linear-gradient(135deg, var(--vy-orange), var(--vy-orange-deep));
  color: #fff;
  box-shadow: 0 12px 24px rgba(242, 135, 5, 0.22);
}

.error-box,
.empty-state {
  padding: 12px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 800;
}

.error-box {
  color: var(--vy-danger);
  background: rgba(196, 69, 42, 0.1);
}

.empty-state {
  color: var(--vy-ink-3);
  background: var(--vy-surface-2);
}

.empty-panel {
  min-height: 340px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.empty-panel svg {
  color: var(--vy-orange-deep);
  margin-bottom: 12px;
}

.empty-panel p {
  max-width: 360px;
  margin-top: 8px;
  color: var(--vy-ink-2);
}

@media (max-width: 960px) {
  .registro-referido-page {
    padding: 18px;
  }

  .page-header,
  .layout-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .page-header,
  .validation-form footer {
    align-items: stretch;
    flex-direction: column;
  }

  .full-field {
    grid-column: auto;
  }
}
</style>
