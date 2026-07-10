<script setup>
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { ChevronRight, ExternalLink, LogIn } from "lucide-vue-next";
import { VyLogo, VyProductImage } from "../components/ui.js";
import { loadCompanyHome } from "../services/companyHomeService.js";

const loading = ref(false);
const landing = ref(null);

const sections = computed(() => landing.value?.sections || []);
const heroSection = computed(() => sections.value.find((section) => section.type === "hero"));
const contentSections = computed(() => sections.value.filter((section) => section.type !== "hero"));

async function loadHome() {
  loading.value = true;
  try {
    landing.value = await loadCompanyHome();
  } finally {
    loading.value = false;
  }
}

function faqQuestion(item) {
  return String(item || "").split("|||")[0] || "";
}

function faqAnswer(item) {
  return String(item || "").split("|||")[1] || "";
}

function socialLabel(item) {
  return String(item || "").split("|||")[0] || "Red social";
}

function socialUrl(item) {
  return String(item || "").split("|||")[1] || "#";
}

onMounted(loadHome);
</script>

<template>
  <div class="vy company-home">
    <header class="home-nav">
      <VyLogo :size="28" tagline />
      <nav>
        <RouterLink to="/login" class="login-link"><LogIn :size="17" /> Iniciar sesion</RouterLink>
      </nav>
    </header>

    <main v-if="loading" class="state-box">Cargando pagina principal...</main>
    <main v-else-if="landing">
      <section class="home-hero" :class="heroSection?.layout || 'imageRight'">
        <div class="hero-copy">
          <span class="vy-chip vy-chip-orange">{{ landing.category || "Empresa" }}</span>
          <h1>{{ heroSection?.title || landing.title }}</h1>
          <p>{{ heroSection?.text || landing.description }}</p>
          <div class="hero-actions">
            <RouterLink to="/login" class="primary-action"><LogIn :size="18" /> Iniciar sesion</RouterLink>
            <a href="#contenido" class="secondary-action">Conocer mas <ChevronRight :size="17" /></a>
          </div>
        </div>
        <VyProductImage
          :grad="heroSection?.imageUrl || landing.imageUrl || 'linear-gradient(135deg, #F2E7C4 0%, #F28705 48%, #1F1A14 100%)'"
          :h="500"
          big
        />
      </section>

      <section id="contenido" class="home-sections">
        <article v-for="(section, index) in contentSections" :key="index" class="home-section" :class="[section.type, section.layout]">
          <template v-if="section.type === 'text'">
            <span class="vy-eyebrow">Empresa</span>
            <h2>{{ section.title }}</h2>
            <p>{{ section.text }}</p>
          </template>

          <template v-else-if="section.type === 'imageText'">
            <div>
              <span class="vy-eyebrow">Vidayoung</span>
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
            </div>
            <VyProductImage :grad="section.imageUrl || landing.imageUrl" :h="330" />
          </template>

          <template v-else-if="section.type === 'benefits'">
            <header>
              <span class="vy-eyebrow">Diferenciales</span>
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
            </header>
            <div class="card-grid">
              <div v-for="item in section.images" :key="item" class="info-card">{{ item }}</div>
            </div>
          </template>

          <template v-else-if="['gallery', 'carousel'].includes(section.type)">
            <header>
              <span class="vy-eyebrow">Productos</span>
              <h2>{{ section.title }}</h2>
              <p v-if="section.text">{{ section.text }}</p>
            </header>
            <div class="media-grid">
              <VyProductImage
                v-for="image in section.images"
                :key="image"
                :grad="image"
                :label="image.startsWith('http') || image.startsWith('/uploads/') ? '' : image"
                :h="260"
              />
              <VyProductImage
                v-if="!section.images.length"
                grad="linear-gradient(135deg, #FFF4D8 0%, #F28705 100%)"
                label="Producto Vidayoung"
                :h="260"
              />
            </div>
          </template>

          <template v-else-if="section.type === 'preguntas'">
            <div class="faq-wrap">
              <span class="vy-eyebrow">Ayuda</span>
              <h2>{{ section.title || "Preguntas frecuentes" }}</h2>
              <details v-for="item in section.images" :key="item">
                <summary>{{ faqQuestion(item) }}</summary>
                <p>{{ faqAnswer(item) }}</p>
              </details>
            </div>
          </template>

          <template v-else-if="section.type === 'social'">
            <header>
              <span class="vy-eyebrow">Comunidad</span>
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
            </header>
            <div class="social-grid">
              <a v-for="item in section.images" :key="item" :href="socialUrl(item)" target="_blank" rel="noreferrer">
                <span>{{ socialLabel(item) }}</span>
                <ExternalLink :size="17" />
              </a>
            </div>
          </template>

          <template v-else-if="section.type === 'contact'">
            <div class="contact-block">
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
              <RouterLink to="/login" class="primary-action"><LogIn :size="18" /> Iniciar sesion</RouterLink>
            </div>
          </template>
        </article>
      </section>

      <footer class="home-footer">
        <div>
          <VyLogo :size="24" dark />
          <p>{{ landing.description }}</p>
        </div>
        <nav>
          <a href="#contenido">Empresa</a>
          <RouterLink to="/login">Iniciar sesion</RouterLink>
          <a href="mailto:info@vidayoung.com">Contacto</a>
        </nav>
        <small>Vidayoung © {{ new Date().getFullYear() }}. Todos los derechos reservados.</small>
      </footer>
    </main>
  </div>
</template>

<style scoped>
.company-home { min-height: 100vh; background: #fffaf2; color: var(--vy-ink); }
.home-nav { min-height: 76px; padding: 12px clamp(18px, 5vw, 64px); display: flex; align-items: center; justify-content: space-between; gap: 18px; background: rgba(255, 255, 255, 0.92); border-bottom: 1px solid var(--vy-line-2); position: sticky; top: 0; z-index: 30; backdrop-filter: blur(14px); }
.login-link, .primary-action, .secondary-action, .social-grid a { display: inline-flex; align-items: center; justify-content: center; gap: 9px; font-weight: 900; }
.login-link { min-height: 42px; padding: 0 16px; border-radius: 999px; background: var(--vy-ink); color: #fff; }
.state-box { min-height: calc(100vh - 76px); display: flex; align-items: center; justify-content: center; font-weight: 800; color: var(--vy-ink-2); }
.home-hero { width: min(100% - 32px, 1240px); min-height: calc(100vh - 112px); margin: 0 auto; display: grid; grid-template-columns: minmax(0, 0.9fr) minmax(320px, 1fr); gap: clamp(24px, 5vw, 62px); align-items: center; padding: clamp(26px, 6vw, 68px) 0; }
.home-hero.imageLeft { grid-template-columns: minmax(320px, 1fr) minmax(0, 0.9fr); }
.home-hero.imageLeft .hero-copy { order: 2; }
.hero-copy h1 { max-width: 780px; margin-top: 16px; font-size: clamp(42px, 7vw, 84px); line-height: 0.96; font-weight: 900; overflow-wrap: anywhere; }
.hero-copy p { max-width: 680px; margin-top: 20px; color: var(--vy-ink-2); font-size: clamp(16px, 1.8vw, 21px); line-height: 1.58; }
.hero-actions { display: flex; gap: 12px; flex-wrap: wrap; margin-top: 30px; }
.primary-action { min-height: 52px; padding: 0 23px; border-radius: 999px; background: var(--vy-orange); color: #fff; box-shadow: 0 16px 34px rgba(242, 135, 5, 0.28); }
.secondary-action { min-height: 52px; padding: 0 21px; border-radius: 999px; border: 1px solid var(--vy-line); background: #fff; color: var(--vy-ink); }
.home-sections { width: min(100% - 32px, 1240px); margin: 0 auto; padding: 0 0 clamp(46px, 7vw, 82px); display: grid; gap: clamp(16px, 2vw, 24px); }
.home-section { padding: clamp(22px, 4vw, 42px); border: 1px solid rgba(31, 26, 20, 0.1); border-radius: 8px; background: rgba(255, 255, 255, 0.9); box-shadow: 0 16px 44px rgba(31, 26, 20, 0.07); overflow: hidden; }
.home-section.text.centered { max-width: 900px; margin: 0 auto; text-align: center; }
.home-section.imageText { display: grid; grid-template-columns: minmax(0, 1fr) minmax(280px, 0.8fr); gap: clamp(18px, 4vw, 36px); align-items: center; }
.home-section.imageText.imageLeft { grid-template-columns: minmax(280px, 0.8fr) minmax(0, 1fr); }
.home-section.imageText.imageLeft > div:first-child { order: 2; }
.home-section h2 { margin-top: 8px; font-size: clamp(28px, 4vw, 50px); line-height: 1.04; font-weight: 900; overflow-wrap: anywhere; }
.home-section p { margin-top: 12px; color: var(--vy-ink-2); font-size: 16px; line-height: 1.68; }
.card-grid, .media-grid, .social-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; margin-top: 22px; }
.info-card { min-height: 112px; padding: 18px; border: 1px solid var(--vy-line); border-radius: 8px; background: var(--vy-surface-2); color: var(--vy-ink-2); font-weight: 800; line-height: 1.5; }
.home-section.carousel .media-grid { grid-auto-flow: column; grid-auto-columns: minmax(250px, 32%); grid-template-columns: none; overflow-x: auto; padding-bottom: 8px; }
.home-section.preguntas { background: #171717; color: #fff; }
.faq-wrap h2 { color: #fff; }
.faq-wrap details { margin-top: 12px; border: 1px solid rgba(255, 255, 255, 0.12); border-radius: 8px; background: rgba(255, 255, 255, 0.04); }
.faq-wrap summary { min-height: 62px; padding: 0 18px; display: flex; align-items: center; justify-content: space-between; cursor: pointer; list-style: none; font-size: 17px; font-weight: 900; }
.faq-wrap summary::-webkit-details-marker { display: none; }
.faq-wrap summary::after { content: "+"; color: var(--vy-orange); font-size: 24px; }
.faq-wrap details[open] summary::after { content: "-"; }
.faq-wrap details p { margin: 0; padding: 0 18px 18px; color: rgba(255, 255, 255, 0.78); }
.social-grid a { min-height: 78px; padding: 0 18px; border-radius: 8px; background: var(--vy-ink); color: #fff; justify-content: space-between; }
.contact-block { max-width: 720px; margin: 0 auto; text-align: center; }
.home-footer { padding: clamp(28px, 5vw, 44px) clamp(18px, 5vw, 64px); display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 22px; align-items: start; background: var(--vy-ink); color: #fff; }
.home-footer p { max-width: 560px; margin-top: 12px; color: rgba(255, 255, 255, 0.68); line-height: 1.6; }
.home-footer nav { display: flex; gap: 16px; flex-wrap: wrap; justify-content: flex-end; }
.home-footer a { color: rgba(255, 255, 255, 0.82); font-size: 14px; font-weight: 800; }
.home-footer a:hover { color: var(--vy-orange); }
.home-footer small { grid-column: 1 / -1; padding-top: 18px; border-top: 1px solid rgba(255, 255, 255, 0.12); color: rgba(255, 255, 255, 0.5); font-size: 12px; font-weight: 700; }
@media (max-width: 920px) {
  .home-hero, .home-hero.imageLeft, .home-section.imageText, .home-section.imageText.imageLeft { grid-template-columns: 1fr; min-height: auto; }
  .home-hero.imageLeft .hero-copy, .home-section.imageText.imageLeft > div:first-child { order: initial; }
  .card-grid, .media-grid, .social-grid { grid-template-columns: 1fr; }
  .home-footer { grid-template-columns: 1fr; }
  .home-footer nav { justify-content: flex-start; }
}
@media (max-width: 560px) {
  .home-nav { min-height: 68px; padding: 10px 14px; }
  .home-nav :deep(.vy-mark + div span:last-child) { display: none; }
  .login-link { min-height: 38px; padding: 0 12px; font-size: 13px; }
  .home-hero, .home-sections { width: min(100% - 24px, 520px); }
  .hero-copy h1 { font-size: clamp(34px, 12vw, 48px); }
  .hero-actions, .primary-action, .secondary-action { width: 100%; }
  .home-section { padding: 20px; }
}
</style>
