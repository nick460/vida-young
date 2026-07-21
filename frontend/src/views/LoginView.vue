<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { VyIcon } from "../components/ui.js";
import { useAuthStore } from "../stores/authStore.js";
import { apiRequest } from "../services/api.js";
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
const slides = ref([]);
const activeSlideIndex = ref(0);
const carouselTimer = ref(null);
const API_URL = import.meta.env.VITE_API_URL || "";

const defaultSlides = [
  {
    id: "default-1",
    titulo: "Generando bienestar para una nueva generacion.",
    descripcion: "Conoce novedades, productos y oportunidades para construir tu proyecto Vidayoung.",
    imagenUrl: "",
    imagenMobileUrl: ""
  }
];

const carouselSlides = computed(() => slides.value.length ? slides.value : defaultSlides);
const activeSlide = computed(() => carouselSlides.value[activeSlideIndex.value] || carouselSlides.value[0]);

function mediaUrl(path) {
  if (!path) return "";
  if (path.startsWith("http") || path.startsWith("blob:")) return path;
  return `${API_URL}${path.startsWith("/") ? path : `/${path}`}`;
}

const activeDesktopImage = computed(() => activeSlide.value?.imagenUrl || "");
const activeMobileImage = computed(() => activeSlide.value?.imagenMobileUrl || activeSlide.value?.imagenUrl || "");

function goToSlide(index) {
  activeSlideIndex.value = index;
}

function startCarousel() {
  window.clearInterval(carouselTimer.value);

  if (carouselSlides.value.length < 2) {
    return;
  }

  carouselTimer.value = window.setInterval(() => {
    activeSlideIndex.value = (activeSlideIndex.value + 1) % carouselSlides.value.length;
  }, 6000);
}

async function loadCarousel() {
  try {
    slides.value = await apiRequest("/api/public/login-carousel");
    activeSlideIndex.value = 0;
    startCarousel();
  } catch {
    slides.value = [];
  }
}

async function submitLogin() {
  error.value = "";
  loading.value = true;

  try {
    await authStore.login(username.value, password.value);
    await router.push(route.query.redirect?.toString() || { name: "dashboard" });
  } catch (exception) {
    error.value = "Usuario o contrasena incorrectos";
  } finally {
    loading.value = false;
  }
}

onMounted(loadCarousel);

onBeforeUnmount(() => {
  window.clearInterval(carouselTimer.value);
});
</script>

<template>
  <main class="vy login-view">
    <aside class="login-hero">
      <img
        v-if="activeDesktopImage || activeMobileImage"
        class="hero-photo"
        :src="mediaUrl(activeDesktopImage || activeMobileImage)"
        :alt="activeSlide.titulo"
      />
      <img
        v-if="activeMobileImage"
        class="hero-photo-mobile"
        :src="mediaUrl(activeMobileImage)"
        :alt="activeSlide.titulo"
      />
      <div class="hero-background"></div>
      <div class="vy-dotgrid hero-dots"></div>

      <div class="hero-top">
        <div class="hero-brand">
          <img :src="logoMark" alt="Vidayoung" class="hero-logo-mark" />
          <span class="hero-brand-name">Vidayoung</span>
        </div>
      </div>

      <div class="hero-content">
        <h2>{{ activeSlide?.titulo }}</h2>
        <p>{{ activeSlide?.descripcion }}</p>
      </div>

      <div class="hero-progress" aria-label="Carrusel de novedades">
        <button
          v-for="(slide, index) in carouselSlides"
          :key="slide.id || index"
          type="button"
          :class="{ active: index === activeSlideIndex }"
          :aria-label="`Ver novedad ${index + 1}`"
          @click="goToSlide(index)"
        ></button>
      </div>
    </aside>

    <section class="login-panel">
      <div class="login-brand">
        <img :src="logoFull" alt="Vidayoung" class="login-logo-full" />
      </div>

      <div class="login-card">
        <div class="vy-eyebrow login-eyebrow">Bienvenida de regreso</div>
        <h1>Tu bienestar<br />te esta esperando.</h1>

        <form class="login-form" @submit.prevent="submitLogin">
          <label class="field">
            <span>Usuario</span>
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
              <span>Contrasena</span>
              <button type="button">Olvidaste tu contrasena?</button>
            </span>
            <input
              v-model="password"
              type="password"
              autocomplete="current-password"
              placeholder="**********"
              required
            />
          </label>

          <div v-if="error" class="login-error">{{ error }}</div>

          <label class="remember-row">
            <input v-model="remember" type="checkbox" />
            <span>Mantener sesion iniciada en este dispositivo</span>
          </label>

          <button class="vy-btn vy-btn-primary login-submit" type="submit" :disabled="loading">
            <span>{{ loading ? "Ingresando..." : "Entrar a mi panel" }}</span>
            <VyIcon name="arrowR" :size="14" />
          </button>
        </form>
      </div>

      <footer class="login-footer">
        <a>Terminos</a>
        <a>Privacidad</a>
        <a>(c) 2026 Vidayoung</a>
      </footer>
    </section>
  </main>
</template>

<style scoped>
.login-view {
  width: 100%;
  height: 100vh;
  height: 100dvh;
  display: grid;
  grid-template-columns: minmax(520px, 1.05fr) minmax(420px, 1fr);
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
  font-size: 11.5px;
  font-weight: 700;
  text-transform: none;
  border: 0;
  background: none;
  cursor: pointer;
  font-family: inherit;
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
  justify-content: flex-start;
  min-height: 0;
  padding: clamp(28px, 4.8vh, 56px);
  color: #fff;
}

.hero-photo {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.hero-photo {
  object-fit: cover;
  object-position: center center;
}

.hero-photo-mobile {
  display: none;
}

.hero-background {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.25), transparent 50%),
    radial-gradient(circle at 20% 80%, rgba(31, 26, 20, 0.2), transparent 50%);
}

.hero-photo + .hero-background,
.hero-photo ~ .hero-background {
  background:
    linear-gradient(180deg, rgba(31, 26, 20, 0.08) 0%, rgba(31, 26, 20, 0.04) 44%, rgba(31, 26, 20, 0.76) 100%),
    linear-gradient(90deg, rgba(31, 26, 20, 0.42) 0%, rgba(31, 26, 20, 0.06) 44%, rgba(31, 26, 20, 0.34) 100%);
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
  letter-spacing: 0;
  color: #fff;
}

.hero-content {
  margin-top: auto;
  max-width: 560px;
  padding-bottom: 24px;
}

.hero-content h2 {
  font-size: clamp(34px, 6vh, 52px);
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

.hero-progress {
  display: flex;
  gap: 8px;
  margin-top: 18px;
}

.hero-progress button {
  width: 8px;
  height: 4px;
  border: 0;
  border-radius: 99px;
  background: rgba(255, 255, 255, 0.4);
  padding: 0;
  cursor: pointer;
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
    min-height: 260px;
    padding: 20px;
  }

  .login-panel {
    padding: 32px 20px;
    min-height: auto;
    overflow: visible;
  }

  .login-card {
    margin: 34px auto;
  }

  .hero-photo {
    display: none;
  }

  .hero-photo-mobile {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
  }

  .hero-photo-mobile + .hero-background,
  .hero-photo + .hero-photo-mobile + .hero-background {
    background:
      linear-gradient(180deg, rgba(31, 26, 20, 0.28) 0%, rgba(31, 26, 20, 0.18) 48%, rgba(31, 26, 20, 0.72) 100%);
  }

  .hero-brand-name {
    font-size: 18px;
  }

  .hero-content h2 {
    font-size: 28px;
    line-height: 1.08;
  }

  .hero-content > p {
    max-width: 560px;
    font-size: 13px;
    line-height: 1.45;
  }
}

@media (max-width: 560px) {
  .login-hero {
    min-height: 220px;
    padding: 16px;
  }

  .hero-logo-mark {
    height: 30px;
  }

  .hero-content h2 {
    font-size: 24px;
  }

  .hero-content > p {
    font-size: 12.5px;
  }

  .hero-progress {
    gap: 7px;
  }
}
</style>
