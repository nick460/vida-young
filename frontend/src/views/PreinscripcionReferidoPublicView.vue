<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import { CheckCircle2, Eye, EyeOff, Mail, Phone, Send, ShieldCheck, UserPlus } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";

const route = useRoute();
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const saved = ref(false);
const patrocinador = ref(null);
const planes = ref([]);
const selectedPlanId = ref("");
const showPassword = ref(false);
const showConfirmPassword = ref(false);
const API_URL = import.meta.env.VITE_API_URL || "";

const patrocinadorId = computed(() => Number(route.params.patrocinadorId || 0));
const username = computed(() => String(route.params.username || "").trim());

const sponsorName = computed(() => {
  const persona = patrocinador.value || {};
  return persona.nombreCompleto
    || [persona.nombres, persona.apellidos].filter(Boolean).join(" ").trim()
    || "Asesor Vidayoung";
});

const sponsorInitials = computed(() => sponsorName.value
  .split(" ")
  .filter(Boolean)
  .slice(0, 2)
  .map((item) => item[0])
  .join("")
  .toUpperCase());

const sponsorPhoto = computed(() => {
  const photo = patrocinador.value?.fotoPerfil
    || patrocinador.value?.fotoUrl
    || patrocinador.value?.imagenUrl
    || patrocinador.value?.photo
    || "";

  if (!photo) {
    return "";
  }

  if (photo.startsWith("http") || photo.startsWith("blob:")) {
    return photo;
  }

  const normalizedPhoto = photo.startsWith("/") ? photo : `/${photo}`;
  return `${API_URL}${normalizedPhoto}`;
});

const selectedPlan = computed(() => planes.value.find((plan) => plan.id === Number(selectedPlanId.value)) || null);

const sortedPlanes = computed(() => [...planes.value].sort((left, right) => Number(left.precio || 0) - Number(right.precio || 0)));

const form = reactive({
  nombres: "",
  apellidos: "",
  documento: "",
  telefono: "",
  email: "",
  username: "",
  password: "",
  confirmPassword: ""
});

const touched = reactive({
  nombres: false,
  apellidos: false,
  documento: false,
  telefono: false,
  email: false,
  username: false,
  password: false,
  confirmPassword: false
});

const validationErrors = computed(() => {
  const errors = {};
  const trimmed = {
    nombres: form.nombres.trim(),
    apellidos: form.apellidos.trim(),
    documento: form.documento.trim(),
    telefono: form.telefono.trim(),
    email: form.email.trim(),
    username: form.username.trim()
  };

  if (!trimmed.documento) {
    errors.documento = "El CI es obligatorio.";
  }

  if (!trimmed.nombres) {
    errors.nombres = "Los nombres son obligatorios.";
  }

  if (!trimmed.apellidos) {
    errors.apellidos = "Los apellidos son obligatorios.";
  }

  if (!trimmed.telefono) {
    errors.telefono = "El numero de celular es obligatorio.";
  } else if (trimmed.telefono.length < 6) {
    errors.telefono = "Ingresa un numero de celular valido.";
  }

  if (trimmed.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmed.email)) {
    errors.email = "Ingresa un email valido.";
  }

  if (!trimmed.username) {
    errors.username = "El nombre de usuario es obligatorio.";
  } else if (!/^[a-zA-Z0-9._-]{4,50}$/.test(trimmed.username)) {
    errors.username = "Usa 4 a 50 caracteres: letras, numeros, punto, guion o guion bajo.";
  }

  if (!form.password) {
    errors.password = "La contrasena es obligatoria.";
  } else if (form.password.length < 6) {
    errors.password = "La contrasena debe tener al menos 6 caracteres.";
  }

  if (!form.confirmPassword) {
    errors.confirmPassword = "Confirma tu contrasena.";
  } else if (form.confirmPassword !== form.password) {
    errors.confirmPassword = "La confirmacion de contrasena no coincide.";
  }

  return errors;
});

const isFormValid = computed(() => Object.keys(validationErrors.value).length === 0);

function markTouched(field) {
  touched[field] = true;
}

function markAllTouched() {
  Object.keys(touched).forEach(markTouched);
}

function fieldError(field) {
  return touched[field] ? validationErrors.value[field] : "";
}

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function sortedLevels(plan) {
  return [...(plan?.niveles || [])]
    .filter((nivel) => nivel.estado !== "ELIMINADO")
    .sort((left, right) => Number(left.numeroNivel || 0) - Number(right.numeroNivel || 0));
}

async function loadPlanes() {
  planes.value = await apiRequest("/api/planes/public");
}

async function loadPatrocinador() {
  try {
    const endpoint = username.value
      ? `/api/public/preinscripciones-referidos/patrocinadores/usuario/${encodeURIComponent(username.value)}`
      : `/api/public/preinscripciones-referidos/patrocinadores/${patrocinadorId.value}`;
    patrocinador.value = await apiRequest(endpoint);
  } catch (exception) {
    error.value = "El enlace de referido no es valido o la persona que refiere no existe.";
  }
}

async function loadInitialData() {
  loading.value = true;
  error.value = "";

  try {
    await Promise.all([loadPatrocinador(), loadPlanes()]);
  } catch (exception) {
    error.value = exception.message || "No se pudo cargar la informacion del enlace.";
  } finally {
    loading.value = false;
  }
}

function selectPlan(plan) {
  selectedPlanId.value = String(plan.id);
}

function assetUrl(path) {
  if (!path) {
    return "";
  }

  if (path.startsWith("http") || path.startsWith("blob:")) {
    return path;
  }

  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${API_URL}${normalizedPath}`;
}

async function submitForm() {
  markAllTouched();
  saving.value = true;
  error.value = "";

  try {
    if (!isFormValid.value) {
      throw new Error("Revisa los campos marcados antes de enviar.");
    }

    if (form.password !== form.confirmPassword) {
      throw new Error("La confirmacion de contrasena no coincide.");
    }

    await apiRequest("/api/public/preinscripciones-referidos", {
      method: "POST",
      body: JSON.stringify({
        patrocinadorId: patrocinador.value?.id || patrocinadorId.value,
        planId: selectedPlan.value?.id,
        ...form
      })
    });
    saved.value = true;
  } catch (exception) {
    error.value = exception.message || "No se pudo registrar tu preinscripcion.";
  } finally {
    saving.value = false;
  }
}

onMounted(loadInitialData);
</script>

<template>
  <main class="public-referral-page">
    <section class="referral-shell" :class="{ 'plans-step': !saved && !selectedPlan }">
      <article v-if="selectedPlan && !saved" class="intro-panel">
        <span class="eyebrow"><UserPlus :size="16" /> Preinscripcion Vidayoung</span>
        <div class="sponsor-hero">
          <div class="sponsor-photo-wrap">
            <img v-if="sponsorPhoto" class="sponsor-photo" :src="sponsorPhoto" :alt="sponsorName" />
            <span v-else class="sponsor-initials">{{ sponsorInitials }}</span>
          </div>
          <div>
            <small>Invitacion personal de</small>
            <strong>{{ sponsorName }}</strong>
          </div>
        </div>

        <h1>Unete a la red Vidayoung</h1>
        <p>
          Completa tus datos para que tu solicitud quede vinculada con la persona que te invito. En ventanilla validaran tu informacion y el plan de ingreso.
        </p>
        <div class="sponsor-box">
          <div>
            <ShieldCheck :size="18" />
            <span>Referente verificado</span>
          </div>
          <strong>{{ patrocinador ? sponsorName : "Cargando..." }}</strong>
          <small v-if="patrocinador?.telefono"><Phone :size="13" /> {{ patrocinador.telefono }}</small>
          <small v-if="patrocinador?.email"><Mail :size="13" /> {{ patrocinador.email }}</small>
        </div>
      </article>

      <section v-if="!saved && !selectedPlan" class="plans-panel">
        <header>
          <h2>Elige tu plan</h2>
          <p>Selecciona el plan con el que quieres ingresar a la red. Despues completaras tus datos de registro.</p>
        </header>

        <p v-if="error" class="error-box">{{ error }}</p>
        <p v-if="loading" class="loading-box">Cargando planes...</p>

        <div class="plans-grid">
          <article v-for="plan in sortedPlanes" :key="plan.id" class="plan-card">
            <img v-if="plan.imagenUrl" class="plan-image" :src="assetUrl(plan.imagenUrl)" :alt="plan.nombre" />
            <div>
              <span class="plan-chip">{{ plan.nivelesAlcance }} niveles</span>
              <h3>{{ plan.nombre }}</h3>
              <p>{{ plan.descripcion || "Plan de ingreso Vidayoung" }}</p>
            </div>

            <strong class="plan-price">Bs. {{ money(plan.precio) }}</strong>

            <ul class="level-list">
              <li v-for="nivel in sortedLevels(plan)" :key="nivel.id || nivel.numeroNivel">
                <span>Nivel {{ nivel.numeroNivel }}</span>
                <strong>Bs. {{ money(nivel.porcentajeComision) }}</strong>
              </li>
            </ul>

            <button type="button" class="select-plan-button" :disabled="loading || !patrocinador" @click="selectPlan(plan)">
              <CheckCircle2 :size="16" />
              Seleccionar
            </button>
          </article>
        </div>

        <p v-if="!sortedPlanes.length && !loading" class="empty-box">No hay planes disponibles por el momento.</p>
      </section>

      <form v-else-if="!saved && selectedPlan" class="referral-form" novalidate @submit.prevent="submitForm">
        <button type="button" class="change-plan-button" @click="selectedPlanId = ''">
          Cambiar plan
        </button>
        <div class="selected-plan-box">
          <span>Plan seleccionado</span>
          <strong>{{ selectedPlan.nombre }} - Bs. {{ money(selectedPlan.precio) }}</strong>
        </div>
        <h2>Datos personales</h2>
        <p v-if="error" class="error-box">{{ error }}</p>
        <p v-if="loading" class="loading-box">Validando enlace...</p>

        <label>
          CI
          <input
            v-model.trim="form.documento"
            type="text"
            required
            maxlength="30"
            autocomplete="off"
            :class="{ invalid: fieldError('documento') }"
            @blur="markTouched('documento')"
            @input="markTouched('documento')"
          />
          <small v-if="fieldError('documento')" class="field-error">{{ fieldError("documento") }}</small>
        </label>
        <label>
          Nombres
          <input
            v-model.trim="form.nombres"
            type="text"
            required
            maxlength="100"
            autocomplete="given-name"
            :class="{ invalid: fieldError('nombres') }"
            @blur="markTouched('nombres')"
            @input="markTouched('nombres')"
          />
          <small v-if="fieldError('nombres')" class="field-error">{{ fieldError("nombres") }}</small>
        </label>
        <label>
          Apellidos
          <input
            v-model.trim="form.apellidos"
            type="text"
            required
            maxlength="100"
            autocomplete="family-name"
            :class="{ invalid: fieldError('apellidos') }"
            @blur="markTouched('apellidos')"
            @input="markTouched('apellidos')"
          />
          <small v-if="fieldError('apellidos')" class="field-error">{{ fieldError("apellidos") }}</small>
        </label>
        <label>
          Numero de celular
          <input
            v-model.trim="form.telefono"
            type="tel"
            required
            maxlength="30"
            autocomplete="tel"
            :class="{ invalid: fieldError('telefono') }"
            @blur="markTouched('telefono')"
            @input="markTouched('telefono')"
          />
          <small v-if="fieldError('telefono')" class="field-error">{{ fieldError("telefono") }}</small>
        </label>
        <label>
          Email opcional
          <input
            v-model.trim="form.email"
            type="email"
            maxlength="120"
            autocomplete="email"
            :class="{ invalid: fieldError('email') }"
            @blur="markTouched('email')"
            @input="markTouched('email')"
          />
          <small v-if="fieldError('email')" class="field-error">{{ fieldError("email") }}</small>
        </label>
        <label>
          Nombre de usuario
          <input
            v-model.trim="form.username"
            type="text"
            required
            minlength="4"
            maxlength="50"
            autocomplete="username"
            :class="{ invalid: fieldError('username') }"
            @blur="markTouched('username')"
            @input="markTouched('username')"
          />
          <small v-if="fieldError('username')" class="field-error">{{ fieldError("username") }}</small>
        </label>
        <label>
          Contrasena
          <span class="password-field">
            <input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              required
              minlength="6"
              maxlength="80"
              autocomplete="new-password"
              :class="{ invalid: fieldError('password') }"
              @blur="markTouched('password')"
              @input="markTouched('password')"
            />
            <button
              type="button"
              class="password-toggle"
              :aria-label="showPassword ? 'Ocultar contrasena' : 'Mostrar contrasena'"
              @click="showPassword = !showPassword"
            >
              <EyeOff v-if="showPassword" :size="18" />
              <Eye v-else :size="18" />
            </button>
          </span>
          <small v-if="fieldError('password')" class="field-error">{{ fieldError("password") }}</small>
        </label>
        <label>
          Confirmar contrasena
          <span class="password-field">
            <input
              v-model="form.confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              required
              minlength="6"
              maxlength="80"
              autocomplete="new-password"
              :class="{ invalid: fieldError('confirmPassword') }"
              @blur="markTouched('confirmPassword')"
              @input="markTouched('confirmPassword')"
            />
            <button
              type="button"
              class="password-toggle"
              :aria-label="showConfirmPassword ? 'Ocultar confirmacion' : 'Mostrar confirmacion'"
              @click="showConfirmPassword = !showConfirmPassword"
            >
              <EyeOff v-if="showConfirmPassword" :size="18" />
              <Eye v-else :size="18" />
            </button>
          </span>
          <small v-if="fieldError('confirmPassword')" class="field-error">{{ fieldError("confirmPassword") }}</small>
        </label>

        <button type="submit" class="submit-button" :disabled="saving || loading || !patrocinador || !isFormValid">
          <Send :size="17" />
          {{ saving ? "Enviando..." : "Enviar preinscripcion" }}
        </button>
      </form>

      <article v-else class="success-panel">
        <CheckCircle2 :size="44" />
        <h2>Preinscripcion enviada</h2>
        <p>Tu solicitud fue registrada correctamente. La empresa validara tus datos para completar el ingreso a la red.</p>
      </article>
    </section>
  </main>
</template>

<style scoped>
.public-referral-page {
  min-height: 100vh;
  padding: 32px;
  display: flex;
  align-items: center;
  background:
    linear-gradient(135deg, rgba(31, 26, 20, 0.08) 0%, rgba(255, 255, 255, 0) 34%),
    linear-gradient(135deg, #fff8e8 0%, #ffffff 58%, #f7efe1 100%);
  color: var(--vy-ink);
}

.referral-shell {
  width: min(1040px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(320px, 430px);
  gap: 22px;
  align-items: stretch;
}

.referral-shell.plans-step {
  width: min(1180px, 100%);
  grid-template-columns: 1fr;
}

.intro-panel,
.referral-form,
.plans-panel,
.success-panel {
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 22px 54px rgba(31, 26, 20, 0.1);
}

.intro-panel {
  position: relative;
  overflow: hidden;
  padding: 34px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.94) 0%, rgba(255, 248, 232, 0.96) 100%);
}

.intro-panel::after {
  content: "";
  position: absolute;
  right: -92px;
  bottom: -92px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  border: 42px solid rgba(242, 135, 5, 0.1);
  pointer-events: none;
}

.eyebrow {
  width: fit-content;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 11px;
  border-radius: 999px;
  background: rgba(242, 135, 5, 0.12);
  color: var(--vy-orange-deep);
  font-size: 12px;
  font-weight: 900;
}

.intro-panel h1 {
  margin-top: 18px;
  font-size: clamp(34px, 4.4vw, 54px);
  line-height: 0.98;
  font-weight: 900;
  position: relative;
  z-index: 1;
}

.intro-panel p {
  max-width: 620px;
  margin-top: 18px;
  color: var(--vy-ink-2);
  font-size: 16px;
  line-height: 1.55;
  position: relative;
  z-index: 1;
}

.sponsor-hero {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 28px;
  padding: 14px;
  border: 1px solid rgba(242, 135, 5, 0.24);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 18px 42px rgba(31, 26, 20, 0.08);
}

.sponsor-photo-wrap {
  width: 86px;
  height: 86px;
  flex: 0 0 86px;
  border-radius: 50%;
  padding: 4px;
  background: linear-gradient(135deg, var(--vy-orange), var(--vy-orange-deep));
  box-shadow: 0 14px 28px rgba(242, 135, 5, 0.28);
}

.sponsor-photo,
.sponsor-initials {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 3px solid #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.sponsor-photo {
  object-fit: cover;
  background: var(--vy-surface-2);
}

.sponsor-initials {
  background: var(--vy-ink);
  color: #fff;
  font-size: 26px;
  font-weight: 900;
}

.sponsor-hero small {
  display: block;
  color: var(--vy-ink-3);
  font-size: 11px;
  font-weight: 900;
  text-transform: uppercase;
}

.sponsor-hero strong {
  display: block;
  margin-top: 5px;
  font-size: 22px;
  line-height: 1.1;
  font-weight: 900;
}

.sponsor-box {
  position: relative;
  z-index: 1;
  width: fit-content;
  min-width: 260px;
  margin-top: 26px;
  padding: 14px 16px;
  border: 1px solid rgba(242, 135, 5, 0.28);
  border-radius: 8px;
  background: rgba(242, 135, 5, 0.07);
}

.sponsor-box div {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--vy-orange-deep);
  font-size: 12px;
  font-weight: 900;
}

.sponsor-box small,
.sponsor-box strong {
  display: flex;
  align-items: center;
  gap: 6px;
}

.sponsor-box small {
  margin-top: 8px;
  color: var(--vy-ink-3);
  font-size: 12px;
  font-weight: 800;
}

.sponsor-box strong {
  margin-top: 8px;
  font-size: 17px;
  font-weight: 900;
}

.referral-form,
.plans-panel,
.success-panel {
  padding: 24px;
}

.referral-form h2,
.plans-panel h2,
.success-panel h2 {
  font-size: 22px;
  font-weight: 900;
  margin-bottom: 16px;
}

.plans-panel header p {
  margin-top: -8px;
  margin-bottom: 16px;
  color: var(--vy-ink-2);
  font-size: 14px;
  line-height: 1.45;
}

.plans-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
  align-items: stretch;
}

.plan-card {
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  padding: 14px;
  background: #fff;
  display: grid;
  grid-template-rows: auto auto auto 1fr auto;
  gap: 12px;
  box-shadow: 0 12px 24px rgba(31, 26, 20, 0.07);
}

.plan-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 8px;
  object-fit: cover;
  background: var(--vy-surface-2);
}

.plan-chip {
  width: fit-content;
  display: inline-flex;
  padding: 5px 9px;
  border-radius: 999px;
  background: rgba(242, 135, 5, 0.12);
  color: var(--vy-orange-deep);
  font-size: 11px;
  font-weight: 900;
}

.plan-card h3 {
  margin-top: 8px;
  font-size: 20px;
  line-height: 1.1;
  font-weight: 900;
}

.plan-card p {
  margin-top: 6px;
  color: var(--vy-ink-2);
  font-size: 13px;
  line-height: 1.4;
}

.plan-price {
  font-size: 28px;
  line-height: 1;
  font-weight: 900;
  color: var(--vy-ink);
}

.level-list {
  display: grid;
  gap: 6px;
  width: 100%;
  max-height: 150px;
  margin: 0;
  padding: 0;
  list-style: none;
  overflow: auto;
}

.level-list li {
  min-height: 34px;
  padding: 7px 9px;
  border: 1px solid rgba(242, 135, 5, 0.18);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--vy-ink-2);
  font-size: 12px;
  font-weight: 900;
}

.level-list strong {
  color: var(--vy-orange-deep);
  white-space: nowrap;
}

.select-plan-button,
.change-plan-button {
  min-height: 44px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 900;
}

.select-plan-button {
  width: 100%;
  background: var(--vy-ink);
  color: #fff;
}

.change-plan-button {
  width: fit-content;
  margin-bottom: 12px;
  padding: 0 13px;
  border: 1px solid rgba(242, 135, 5, 0.32);
  background: #fff;
  color: var(--vy-orange-deep);
}

.selected-plan-box {
  margin-bottom: 16px;
  padding: 12px;
  border: 1px solid rgba(242, 135, 5, 0.28);
  border-radius: 8px;
  background: rgba(242, 135, 5, 0.08);
}

.selected-plan-box span,
.selected-plan-box strong {
  display: block;
}

.selected-plan-box span {
  color: var(--vy-orange-deep);
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}

.selected-plan-box strong {
  margin-top: 4px;
  font-size: 16px;
  font-weight: 900;
}

.referral-form label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  color: var(--vy-ink-2);
  font-size: 13px;
  font-weight: 900;
}

.referral-form input {
  width: 100%;
  min-height: 44px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  padding: 0 12px;
  background: #fff;
  color: var(--vy-ink);
  font: inherit;
}

.password-field {
  position: relative;
  display: block;
}

.password-field input {
  padding-right: 46px;
}

.password-toggle {
  position: absolute;
  top: 50%;
  right: 8px;
  width: 32px;
  height: 32px;
  transform: translateY(-50%);
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--vy-ink-3);
  background: transparent;
}

.password-toggle:hover {
  background: var(--vy-surface-2);
  color: var(--vy-ink);
}

.referral-form input.invalid {
  border-color: rgba(196, 69, 42, 0.74);
  background: rgba(196, 69, 42, 0.04);
  box-shadow: 0 0 0 3px rgba(196, 69, 42, 0.08);
}

.field-error {
  color: var(--vy-danger);
  font-size: 12px;
  line-height: 1.35;
  font-weight: 800;
}

.submit-button {
  width: 100%;
  min-height: 48px;
  margin-top: 8px;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--vy-orange), var(--vy-orange-deep));
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  font-size: 14px;
  font-weight: 900;
  box-shadow: 0 14px 28px rgba(242, 135, 5, 0.24);
}

.submit-button:disabled {
  opacity: 0.68;
  cursor: wait;
}

.error-box,
.loading-box,
.empty-box {
  padding: 12px;
  border-radius: 8px;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 800;
}

.error-box {
  color: var(--vy-danger);
  background: rgba(196, 69, 42, 0.1);
}

.loading-box {
  color: var(--vy-ink-2);
  background: var(--vy-surface-2);
}

.empty-box {
  color: var(--vy-ink-3);
  background: var(--vy-surface-2);
}

.success-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  text-align: center;
}

.success-panel svg {
  margin: 0 auto 14px;
  color: var(--vy-orange-deep);
}

.success-panel p {
  color: var(--vy-ink-2);
  line-height: 1.5;
}

@media (max-width: 860px) {
  .public-referral-page {
    min-height: 100dvh;
    padding: 18px;
    align-items: flex-start;
  }

  .referral-shell {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .intro-panel {
    padding: 24px;
  }

  .intro-panel h1 {
    max-width: 620px;
    font-size: 40px;
    line-height: 1;
  }

  .referral-form,
  .plans-panel,
  .success-panel {
    padding: 22px;
  }
}

@media (max-width: 520px) {
  .public-referral-page {
    padding: 10px;
  }

  .intro-panel,
  .referral-form,
  .plans-panel,
  .success-panel {
    border-radius: 8px;
    box-shadow: 0 14px 30px rgba(31, 26, 20, 0.08);
  }

  .intro-panel {
    padding: 18px;
  }

  .eyebrow {
    max-width: 100%;
    font-size: 11px;
  }

  .sponsor-hero {
    align-items: center;
    gap: 12px;
    margin-top: 18px;
    padding: 12px;
  }

  .sponsor-photo-wrap {
    width: 68px;
    height: 68px;
    flex-basis: 68px;
  }

  .sponsor-initials {
    font-size: 20px;
  }

  .sponsor-hero strong {
    font-size: 17px;
    line-height: 1.18;
  }

  .intro-panel h1 {
    margin-top: 18px;
    font-size: 33px;
    line-height: 1.02;
  }

  .intro-panel p {
    margin-top: 14px;
    font-size: 14px;
  }

  .sponsor-box {
    width: 100%;
    min-width: 0;
    margin-top: 18px;
  }

  .referral-form,
  .plans-panel,
  .success-panel {
    padding: 18px;
  }

  .referral-form h2,
  .plans-panel h2,
  .success-panel h2 {
    font-size: 20px;
    margin-bottom: 14px;
  }

  .referral-form label {
    margin-bottom: 10px;
  }

  .referral-form input {
    min-height: 46px;
    font-size: 16px;
  }

  .submit-button {
    min-height: 50px;
    font-size: 14px;
  }

}

@media (max-width: 360px) {
  .sponsor-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .intro-panel h1 {
    font-size: 29px;
  }
}
</style>
