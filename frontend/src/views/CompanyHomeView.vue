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
        <a href="#contenido">Empresa</a>
        <a href="#productos">Productos</a>
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
          <div class="hero-metrics" aria-label="Resumen Vidayoung">
            <span><strong>Bienestar</strong><small>Productos funcionales</small></span>
            <span><strong>Comunidad</strong><small>Acompanamiento cercano</small></span>
            <span><strong>Digital</strong><small>Herramientas simples</small></span>
          </div>
        </div>
        <div class="hero-visual">
          <VyProductImage
            :grad="heroSection?.imageUrl || landing.imageUrl || 'linear-gradient(135deg, #F2E7C4 0%, #F28705 48%, #1F1A14 100%)'"
            :h="500"
            big
          />
        </div>
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
            <div id="productos" class="media-grid">
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
          <a href="#productos">Productos</a>
          <RouterLink to="/login">Iniciar sesion</RouterLink>
          <a href="mailto:info@vidayoung.com">Contacto</a>
        </nav>
        <small>Vidayoung © {{ new Date().getFullYear() }}. Todos los derechos reservados.</small>
      </footer>
    </main>
  </div>
</template>

<style scoped>
.company-home {
  min-height: 100vh;
  background:
    linear-gradient(180deg, #fffaf2 0%, var(--vy-bg) 42%, #fff 100%);
  color: var(--vy-ink);
}

.home-nav {
  min-height: 78px;
  padding: 12px clamp(18px, 5vw, 68px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: rgba(255, 255, 255, 0.88);
  border-bottom: 1px solid rgba(233, 226, 210, 0.72);
  position: sticky;
  top: 0;
  z-index: 30;
  backdrop-filter: blur(18px);
}

.home-nav nav,
.hero-actions,
.login-link,
.primary-action,
.secondary-action,
.social-grid a {
  display: inline-flex;
  align-items: center;
}

.home-nav nav {
  gap: 8px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.home-nav nav > a:not(.login-link) {
  min-height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  color: var(--vy-ink-2);
  font-size: 13px;
  font-weight: 900;
}

.home-nav nav > a:not(.login-link):hover {
  background: var(--vy-surface-2);
  color: var(--vy-ink);
}

.login-link {
  min-height: 42px;
  justify-content: center;
  gap: 9px;
  padding: 0 16px;
  border-radius: 999px;
  background: var(--vy-ink);
  color: #fff;
  font-weight: 900;
  box-shadow: 0 10px 26px rgba(31, 26, 20, 0.14);
}

.state-box {
  min-height: calc(100vh - 78px);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
  color: var(--vy-ink-2);
}

.home-hero {
  width: min(100% - 32px, 1240px);
  min-height: calc(100vh - 104px);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(330px, 1fr);
  gap: clamp(28px, 5vw, 68px);
  align-items: center;
  padding: clamp(34px, 6vw, 76px) 0 clamp(28px, 5vw, 56px);
}

.home-hero.imageLeft {
  grid-template-columns: minmax(330px, 1fr) minmax(0, 0.92fr);
}

.home-hero.imageLeft .hero-copy {
  order: 2;
}

.hero-copy {
  min-width: 0;
}

.hero-copy h1 {
  max-width: 800px;
  margin-top: 18px;
  font-size: clamp(44px, 7.6vw, 92px);
  line-height: 0.94;
  font-weight: 950;
  overflow-wrap: anywhere;
}

.hero-copy p {
  max-width: 690px;
  margin-top: 22px;
  color: var(--vy-ink-2);
  font-size: clamp(16px, 1.7vw, 21px);
  line-height: 1.62;
}

.hero-actions {
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 32px;
}

.primary-action,
.secondary-action {
  min-height: 54px;
  justify-content: center;
  gap: 9px;
  padding: 0 24px;
  border-radius: 999px;
  font-weight: 950;
}

.primary-action {
  background: var(--vy-orange);
  color: #fff;
  box-shadow: 0 18px 38px rgba(242, 135, 5, 0.3);
}

.primary-action:hover {
  background: var(--vy-orange-deep);
  transform: translateY(-1px);
}

.secondary-action {
  border: 1px solid rgba(31, 26, 20, 0.12);
  background: rgba(255, 255, 255, 0.82);
  color: var(--vy-ink);
}

.hero-metrics {
  width: min(100%, 700px);
  margin-top: 34px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.hero-metrics span {
  min-height: 82px;
  padding: 14px;
  border: 1px solid rgba(242, 135, 5, 0.18);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 14px 36px rgba(31, 26, 20, 0.06);
}

.hero-metrics strong,
.hero-metrics small {
  display: block;
}

.hero-metrics strong {
  color: var(--vy-ink);
  font-size: 15px;
  font-weight: 950;
}

.hero-metrics small {
  margin-top: 6px;
  color: var(--vy-ink-3);
  font-size: 12px;
  font-weight: 800;
  line-height: 1.35;
}

.hero-visual {
  min-width: 0;
  padding: clamp(10px, 2vw, 18px);
  border: 1px solid rgba(242, 135, 5, 0.2);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.78), rgba(250, 245, 232, 0.84));
  box-shadow: 0 24px 70px rgba(31, 26, 20, 0.12);
}

.hero-visual :deep(> div) {
  border-radius: 8px !important;
}

.home-sections {
  width: min(100% - 32px, 1240px);
  margin: 0 auto;
  padding: clamp(8px, 2vw, 18px) 0 clamp(54px, 7vw, 92px);
  display: grid;
  gap: clamp(18px, 2.5vw, 28px);
}

.home-section {
  min-width: 0;
  padding: clamp(24px, 4vw, 46px);
  border: 1px solid rgba(31, 26, 20, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18px 54px rgba(31, 26, 20, 0.08);
  overflow: hidden;
}

.home-section.text.centered {
  width: min(100%, 900px);
  margin: 0 auto;
  text-align: center;
  background: var(--vy-ink);
  color: #fff;
}

.home-section.text.centered .vy-eyebrow {
  color: var(--vy-orange);
}

.home-section.text.centered p {
  color: rgba(255, 255, 255, 0.78);
}

.home-section.imageText {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 0.82fr);
  gap: clamp(20px, 4vw, 42px);
  align-items: center;
}

.home-section.imageText.imageLeft {
  grid-template-columns: minmax(280px, 0.82fr) minmax(0, 1fr);
}

.home-section.imageText.imageLeft > div:first-child {
  order: 2;
}

.home-section.imageText :deep(> div:last-child),
.media-grid :deep(> div) {
  border-radius: 8px !important;
}

.home-section h2 {
  margin-top: 10px;
  font-size: clamp(30px, 4.2vw, 54px);
  line-height: 1.02;
  font-weight: 950;
  overflow-wrap: anywhere;
}

.home-section p {
  max-width: 760px;
  margin-top: 14px;
  color: var(--vy-ink-2);
  font-size: 16px;
  line-height: 1.72;
}

.card-grid,
.media-grid,
.social-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 24px;
}

.info-card {
  min-height: 128px;
  padding: 20px;
  border: 1px solid rgba(242, 135, 5, 0.18);
  border-radius: 8px;
  background: linear-gradient(180deg, #fff, var(--vy-surface-2));
  color: var(--vy-ink-2);
  font-weight: 850;
  line-height: 1.55;
  box-shadow: 0 12px 30px rgba(31, 26, 20, 0.05);
}

.home-section.carousel .media-grid {
  grid-auto-flow: column;
  grid-auto-columns: minmax(250px, 32%);
  grid-template-columns: none;
  overflow-x: auto;
  padding-bottom: 8px;
}

.home-section.preguntas {
  background: #17130f;
  color: #fff;
}

.faq-wrap h2 {
  color: #fff;
}

.faq-wrap details {
  margin-top: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.04);
}

.faq-wrap summary {
  min-height: 66px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  cursor: pointer;
  list-style: none;
  color: #fff;
  font-size: 17px;
  font-weight: 950;
}

.faq-wrap summary::-webkit-details-marker {
  display: none;
}

.faq-wrap summary::after {
  content: "+";
  flex: 0 0 auto;
  color: var(--vy-orange);
  font-size: 24px;
}

.faq-wrap details[open] summary::after {
  content: "-";
}

.faq-wrap details p {
  margin: 0;
  padding: 0 20px 20px;
  color: rgba(255, 255, 255, 0.78);
}

.social-grid a {
  min-height: 82px;
  justify-content: space-between;
  gap: 14px;
  padding: 0 20px;
  border-radius: 8px;
  background: var(--vy-ink);
  color: #fff;
  font-weight: 950;
}

.social-grid a:hover {
  background: var(--vy-orange);
}

.contact-block {
  max-width: 760px;
  margin: 0 auto;
  text-align: center;
}

.contact-block .primary-action {
  margin-top: 24px;
}

.home-footer {
  padding: clamp(32px, 5vw, 52px) clamp(18px, 5vw, 68px);
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: start;
  background: var(--vy-ink);
  color: #fff;
}

.home-footer p {
  max-width: 580px;
  margin-top: 14px;
  color: rgba(255, 255, 255, 0.68);
  line-height: 1.62;
}

.home-footer nav {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.home-footer a {
  min-height: 38px;
  display: inline-flex;
  align-items: center;
  padding: 0 12px;
  border-radius: 999px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 14px;
  font-weight: 900;
}

.home-footer a:hover {
  background: rgba(255, 255, 255, 0.08);
  color: var(--vy-orange);
}

.home-footer small {
  grid-column: 1 / -1;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.5);
  font-size: 12px;
  font-weight: 800;
}

@media (max-width: 920px) {
  .home-hero,
  .home-hero.imageLeft,
  .home-section.imageText,
  .home-section.imageText.imageLeft {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .home-hero.imageLeft .hero-copy,
  .home-section.imageText.imageLeft > div:first-child {
    order: initial;
  }

  .card-grid,
  .media-grid,
  .social-grid {
    grid-template-columns: 1fr;
  }

  .home-footer {
    grid-template-columns: 1fr;
  }

  .home-footer nav {
    justify-content: flex-start;
  }
}

@media (max-width: 680px) {
  .home-nav {
    min-height: 70px;
    padding: 10px 14px;
  }

  .home-nav :deep(.vy-mark + div span:last-child),
  .home-nav nav > a:not(.login-link) {
    display: none;
  }

  .login-link {
    min-height: 38px;
    padding: 0 12px;
    font-size: 13px;
  }

  .home-hero,
  .home-sections {
    width: min(100% - 24px, 520px);
  }

  .hero-copy h1 {
    font-size: clamp(34px, 12vw, 50px);
  }

  .hero-actions,
  .primary-action,
  .secondary-action {
    width: 100%;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
  }

  .hero-visual {
    padding: 8px;
  }

  .home-section {
    padding: 20px;
  }
}
</style>
