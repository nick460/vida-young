<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import { CheckCircle2, Eye, EyeOff, Send, UserPlus } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import { useAuthStore } from "../stores/authStore.js";

const authStore = useAuthStore();
const saving = ref(false);
const error = ref("");
const saved = ref(false);
const showPassword = ref(false);
const showConfirmPassword = ref(false);
const checkingUsername = ref(false);
const usernameAvailable = ref(null);
let usernameCheckTimeout;

const form = reactive({ nombres: "", apellidos: "", documento: "", telefono: "", email: "", username: "", password: "", confirmPassword: "" });
const sponsorName = computed(() => [authStore.usuario?.persona?.nombres, authStore.usuario?.persona?.apellidos].filter(Boolean).join(" ") || authStore.usuario?.username || "Usuario actual");
const validationError = computed(() => {
  if (!form.documento.trim() || !form.nombres.trim() || !form.apellidos.trim() || !form.telefono.trim()) return "Completa los datos personales obligatorios.";
  if (!/^[a-zA-Z0-9._-]{4,50}$/.test(form.username.trim())) return "El usuario debe tener entre 4 y 50 caracteres válidos.";
  if (usernameAvailable.value === false) return "El nombre de usuario no está disponible.";
  if (form.password.length < 6) return "La contraseña debe tener al menos 6 caracteres.";
  if (form.password !== form.confirmPassword) return "La confirmación de contraseña no coincide.";
  return "";
});

async function checkUsername() {
  const username = form.username.trim().toLowerCase();
  if (!/^[a-zA-Z0-9._-]{4,50}$/.test(username)) return;
  checkingUsername.value = true;
  try {
    const response = await apiRequest(`/api/public/preinscripciones-referidos/usuarios/${encodeURIComponent(username)}/disponible`);
    usernameAvailable.value = Boolean(response.disponible);
  } finally {
    checkingUsername.value = false;
  }
}

async function submit() {
  error.value = validationError.value;
  if (error.value) return;
  saving.value = true;
  try {
    await apiRequest("/api/preinscripciones-referidos/interna", {
      method: "POST",
      body: JSON.stringify({ ...form })
    });
    saved.value = true;
  } catch (exception) {
    error.value = exception.message || "No se pudo registrar la preinscripción.";
  } finally {
    saving.value = false;
  }
}

function nueva() {
  Object.assign(form, { nombres: "", apellidos: "", documento: "", telefono: "", email: "", username: "", password: "", confirmPassword: "" });
  usernameAvailable.value = null;
  saved.value = false;
}

watch(() => form.username, () => {
  usernameAvailable.value = null;
  clearTimeout(usernameCheckTimeout);
  usernameCheckTimeout = setTimeout(checkUsername, 450);
});

onMounted(async () => {
  if (!authStore.usuario?.persona) await authStore.cargarPerfil();
});
</script>

<template>
  <div class="vy internal-referral-page">
    <main class="workspace">
      <section v-if="saved" class="success-card">
        <CheckCircle2 :size="46" />
        <h1>Preinscripción registrada</h1>
        <p>La solicitud quedó pendiente de validación. El plan se asignará después desde Registro referido.</p>
        <button type="button" class="primary" @click="nueva"><UserPlus :size="17" /> Registrar otro referido</button>
      </section>

      <template v-else>
        <header class="page-header"><div><span class="vy-eyebrow">Registro interno</span><h1>Nuevo referido</h1><p>Registra los datos iniciales sin seleccionar plan.</p></div></header>
        <section class="referral-layout">
          <article class="sponsor-card"><span class="vy-eyebrow"><UserPlus :size="16" /> Patrocinador</span><h2>{{ sponsorName }}</h2><p>El referido quedará vinculado automáticamente a tu cuenta.</p><div class="plan-note"><strong>Plan pendiente</strong><span>Se define cuando ventanilla valide la preinscripción.</span></div></article>
          <form class="form-card" @submit.prevent="submit"><h2>Datos del referido</h2><p v-if="error" class="error-box">{{ error }}</p><div class="form-grid"><label>CI<input v-model.trim="form.documento" required maxlength="30" /></label><label>Celular<input v-model.trim="form.telefono" required maxlength="30" /></label><label>Nombres<input v-model.trim="form.nombres" required maxlength="100" /></label><label>Apellidos<input v-model.trim="form.apellidos" required maxlength="100" /></label><label class="full">Email opcional<input v-model.trim="form.email" type="email" maxlength="120" /></label><label class="full">Nombre de usuario<input v-model.trim="form.username" required maxlength="50" /><small v-if="checkingUsername">Verificando disponibilidad...</small><small v-else-if="usernameAvailable === true" class="available">Usuario disponible.</small></label><label>Contraseña<span class="password-field"><input v-model="form.password" :type="showPassword ? 'text' : 'password'" required minlength="6" /><button type="button" @click="showPassword = !showPassword"><EyeOff v-if="showPassword" :size="17" /><Eye v-else :size="17" /></button></span></label><label>Confirmar contraseña<span class="password-field"><input v-model="form.confirmPassword" :type="showConfirmPassword ? 'text' : 'password'" required minlength="6" /><button type="button" @click="showConfirmPassword = !showConfirmPassword"><EyeOff v-if="showConfirmPassword" :size="17" /><Eye v-else :size="17" /></button></span></label></div><footer><button class="primary" type="submit" :disabled="saving"><Send :size="17" /> {{ saving ? "Guardando..." : "Registrar preinscripción" }}</button></footer></form>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.internal-referral-page { min-height: 100vh; background: var(--vy-bg); color: var(--vy-ink); }.workspace { width: min(1080px, 100%); margin: 0 auto; padding: 28px clamp(16px, 4vw, 40px) 48px; }.page-header h1 { margin: 6px 0; font-size: clamp(30px, 4vw, 42px); }.page-header p, .sponsor-card p { color: var(--vy-ink-3); font-weight: 700; }.referral-layout { display: grid; grid-template-columns: minmax(260px, .8fr) minmax(0, 1.4fr); gap: 18px; margin-top: 22px; }.sponsor-card, .form-card, .success-card { padding: 26px; border: 1px solid var(--vy-line); border-radius: 20px; background: var(--vy-surface); box-shadow: var(--vy-shadow-sm); }.sponsor-card { background: linear-gradient(145deg, #fffaf2, #fff); }.sponsor-card h2 { margin: 18px 0 8px; font-size: 27px; }.plan-note { display: grid; gap: 4px; margin-top: 24px; padding: 14px; border-radius: 13px; background: rgba(242, 135, 5, .1); color: var(--vy-orange-deep); }.plan-note span { font-size: 12px; font-weight: 700; }.form-card h2 { margin-bottom: 18px; }.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; }.form-grid label { display: grid; gap: 7px; color: var(--vy-ink-2); font-size: 12px; font-weight: 900; }.form-grid .full { grid-column: 1 / -1; }.form-grid input { width: 100%; min-height: 43px; padding: 0 12px; border: 1px solid var(--vy-line); border-radius: 11px; background: var(--vy-surface-2); color: var(--vy-ink); font: inherit; }.password-field { display: flex; align-items: center; border: 1px solid var(--vy-line); border-radius: 11px; background: var(--vy-surface-2); }.password-field input { border: 0; background: transparent; }.password-field button { width: 40px; border: 0; background: transparent; color: var(--vy-ink-3); cursor: pointer; }.available { color: #166534; }.error-box { padding: 12px; border-radius: 11px; background: #fff0ef; color: #991b1b; font-size: 13px; font-weight: 750; }.form-card footer { display: flex; justify-content: flex-end; margin-top: 20px; }.primary { min-height: 43px; padding: 0 16px; border: 0; border-radius: 11px; background: var(--vy-orange); color: #1f1a14; display: inline-flex; align-items: center; gap: 8px; font-weight: 900; cursor: pointer; }.primary:disabled { opacity: .6; cursor: wait; }.success-card { max-width: 620px; margin: 70px auto; text-align: center; }.success-card svg { color: #16a34a; }.success-card h1 { margin: 16px 0 8px; }.success-card p { color: var(--vy-ink-3); line-height: 1.5; }.success-card button { margin-top: 20px; }@media (max-width: 760px) { .referral-layout, .form-grid { grid-template-columns: 1fr; }.form-grid .full { grid-column: auto; } }
</style>
