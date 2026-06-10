<script setup>
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { VyIcon, VyMark } from "../components/ui.js";
import { useAuthStore } from "../stores/authStore.js";
import logoFull from "../assets/logoFull.png";
import logoMark from "../assets/logoMark.png";

const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();

const username = ref("");
const password = ref("");
const remember = ref(true);
const loading = ref(false);
const error = ref("");

async function submitLogin() {
  error.value = "";
  loading.value = true;

  try {
    await authStore.login(username.value, password.value);
    await router.push(route.query.redirect?.toString() || { name: "dashboard" });
  } catch (exception) {
    error.value = "Usuario o contraseña incorrectos";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <main class="vy login-view">
    <section class="login-panel">
      <div class="login-brand">
        <img :src="logoFull" alt="Vidayoung" class="login-logo-full" />
      </div>

      <div class="login-card">
        <div class="vy-eyebrow login-eyebrow">Bienvenida de regreso</div>
        <h1>Tu bienestar<br />te está esperando.</h1>
        <p>
          Ingresa con tu correo o tu código de embajador para acceder a tu panel.
        </p>

        <form class="login-form" @submit.prevent="submitLogin">
          <label class="field">
            <span>Correo o código embajador</span>
            <input
              v-model.trim="username"
              type="text"
              autocomplete="username"
              placeholder="carolina.m@vidayoung.com"
              required
            />
          </label>

          <label class="field">
            <span class="field-row">
              <span>Contraseña</span>
              <button type="button">¿Olvidaste tu contraseña?</button>
            </span>
            <input
              v-model="password"
              type="password"
              autocomplete="current-password"
              placeholder="••••••••••"
              required
            />
          </label>

          <div v-if="error" class="login-error">{{ error }}</div>

          <label class="remember-row">
            <input v-model="remember" type="checkbox" />
            <span>Mantener sesión iniciada en este dispositivo</span>
          </label>

          <button class="vy-btn vy-btn-primary login-submit" type="submit" :disabled="loading">
            <span>{{ loading ? "Ingresando..." : "Entrar a mi panel" }}</span>
            <VyIcon name="arrowR" :size="14" />
          </button>

        </form>

      </div>

      <footer class="login-footer">
        <a>Términos</a>
        <a>Privacidad</a>
        <a>© 2026 Vidayoung</a>
      </footer>
    </section>

    <aside class="login-hero">
      <div class="hero-background"></div>
      <div class="vy-dotgrid hero-dots"></div>

      <div class="hero-top">
        <div class="hero-brand">
          <img :src="logoMark" alt="Vidayoung" class="hero-logo-mark" />
          <span class="hero-brand-name">Vidayoung</span>
        </div>
        <span class="hero-badge">Plataforma 2.6</span>
      </div>

      <div class="hero-content">
        <h2>Generando bienestar para una nueva generación.</h2>
        <p>
          Más de 12.000 embajadores latinoamericanos construyen un proyecto de vida
          con productos premium de cuidado personal y un plan de compensación
          transparente.
        </p>

        <article class="testimonial">
          <p>
            "En 14 meses pasé de iniciadora a Líder Diamante. Vidayoung me dio una
            comunidad real, comisiones puntuales y productos que mis clientas aman.
            Cambió mi vida financiera."
          </p>
          <div class="testimonial-user">
            <div>VN</div>
            <span>
              <strong>Valeria Núñez</strong>
              <small>Líder Diamante · Medellín</small>
            </span>
          </div>
        </article>
      </div>

      <div class="hero-progress" aria-hidden="true">
        <span class="active"></span>
        <span></span>
        <span></span>
      </div>
    </aside>
  </main>
</template>

<style scoped>
.login-view {
  width: 100%;
  height: 100vh;
  height: 100dvh;
  display: grid;
  grid-template-columns: minmax(420px, 1fr) minmax(520px, 1.05fr);
  background: var(--vy-bg);
  overflow: hidden;
}

.login-panel {
  min-height: 0;
  padding: clamp(28px, 4.8vh, 56px);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-width: 0;
  overflow: hidden;
}

.login-brand {
  display: flex;
  align-items: center;
}

.login-logo-full {
  height: 38px;
  width: auto;
  object-fit: contain;
}

.login-card {
  width: 100%;
  max-width: 380px;
  margin: clamp(18px, 4vh, 48px) auto;
}

.login-eyebrow {
  margin-bottom: 14px;
}

.login-card h1 {
  font-size: clamp(30px, 4.2vh, 36px);
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: 0;
}

.login-card > p {
  color: var(--vy-ink-2);
  margin-top: 14px;
  font-size: 14.5px;
  line-height: 1.5;
}

.login-form {
  margin-top: clamp(22px, 3.5vh, 36px);
  display: flex;
  flex-direction: column;
  gap: clamp(10px, 1.6vh, 14px);
}

.field {
  display: block;
}

.field span,
.field-row {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
  color: var(--vy-ink-3);
  display: block;
  margin-bottom: 6px;
}

.field-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.field-row button {
  color: var(--vy-orange-deep);
  font-weight: 700;
  border: 0;
  background: none;
  cursor: pointer;
  font-family: inherit;
}

.field-row button {
  font-size: 11.5px;
  text-transform: none;
}

.field input {
  width: 100%;
  padding: clamp(11px, 1.7vh, 14px) 16px;
  background: var(--vy-surface);
  border: 1px solid var(--vy-line);
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  color: var(--vy-ink);
}

.field input:focus {
  outline: 2px solid rgba(242, 135, 5, 0.22);
  border-color: var(--vy-orange);
}

.login-error {
  padding: 10px 12px;
  border-radius: 10px;
  background: rgba(196, 69, 42, 0.1);
  color: var(--vy-danger);
  font-size: 12px;
  font-weight: 700;
}

.remember-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--vy-ink-2);
  padding: 6px 0;
}

.remember-row input {
  width: 16px;
  height: 16px;
  accent-color: var(--vy-orange);
}

.login-submit {
  width: 100%;
  padding: clamp(11px, 1.7vh, 14px);
  font-size: 14px;
}

.login-submit:disabled {
  opacity: 0.68;
  cursor: not-allowed;
  transform: none;
}

.login-footer {
  font-size: 11.5px;
  color: var(--vy-ink-3);
  display: flex;
  gap: 16px;
  font-weight: 600;
}

.login-hero {
  background: linear-gradient(135deg, var(--vy-orange) 0%, var(--vy-orange-deep) 100%);
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 0;
  padding: clamp(28px, 4.8vh, 56px);
  color: #fff;
}

.hero-background {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.25), transparent 50%),
    radial-gradient(circle at 20% 80%, rgba(31, 26, 20, 0.2), transparent 50%);
}

.hero-dots {
  position: absolute;
  inset: 0;
  opacity: 0.18;
}

.hero-top,
.hero-content,
.hero-progress {
  position: relative;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hero-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hero-logo-mark {
  height: 36px;
  width: auto;
  object-fit: contain;
  filter: brightness(0) invert(1);
}

.hero-brand-name {
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.03em;
  color: #fff;
}

.hero-badge {
  background: rgba(255, 255, 255, 0.18);
  backdrop-filter: blur(10px);
  padding: 6px 14px;
  border-radius: 99px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.hero-content {
  max-width: 520px;
}

.hero-content h2 {
  font-size: clamp(34px, 6vh, 48px);
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.05;
  margin-top: 0;
}

.hero-content > p {
  font-size: clamp(14px, 1.9vh, 16px);
  line-height: 1.55;
  margin-top: clamp(12px, 2vh, 18px);
  opacity: 0.92;
}

.testimonial {
  margin-top: clamp(18px, 3.4vh, 36px);
  padding: clamp(14px, 2.2vh, 20px);
  background: rgba(255, 255, 255, 0.14);
  backdrop-filter: blur(12px);
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.testimonial > p {
  font-size: 13.5px;
  line-height: 1.5;
  font-style: italic;
}

.testimonial-user {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
}

.testimonial-user > div {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
}

.testimonial-user span {
  display: flex;
  flex-direction: column;
}

.testimonial-user strong {
  font-weight: 800;
  font-size: 13px;
}

.testimonial-user small {
  font-size: 11px;
  opacity: 0.8;
}

.hero-progress {
  display: flex;
  gap: 8px;
}

.hero-progress span {
  width: 8px;
  height: 4px;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.4);
}

.hero-progress .active {
  width: 24px;
  background: #fff;
}

@media (max-width: 980px) {
  .login-view {
    grid-template-columns: 1fr;
    min-height: 100vh;
    min-height: 100dvh;
    height: auto;
    overflow: auto;
  }

  .login-hero {
    display: none;
  }

  .login-panel {
    padding: 32px 20px;
    min-height: 100vh;
    min-height: 100dvh;
    overflow: visible;
  }

  .login-card {
    margin: 40px auto;
  }
}
</style>
