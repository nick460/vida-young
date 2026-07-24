<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { ChevronRight, ExternalLink, LogIn } from "lucide-vue-next";
import { VyLogo, VyProductImage } from "../components/ui.js";
import { loadCompanyHome } from "../services/companyHomeService.js";

const STATIC_BANNER_URL = "/banner-principal.png";

const loading = ref(false);
const landing = ref(null);
const scrolled = ref(false);

const sections = computed(() => landing.value?.sections || []);
const heroSection = computed(() => sections.value.find((section) => section.type === "hero"));
const contentSections = computed(() => sections.value.filter((section) => section.type !== "hero"));

async function loadHome() {
  loading.value = true;
  try {
    landing.value = await loadCompanyHome();
    updateSeoMetadata(landing.value);
  } finally {
    loading.value = false;
  }
}

function absoluteUrl(value = "") {
  if (!value) return "";
  if (value.startsWith("http")) return value;
  return `${window.location.origin}${value.startsWith("/") ? value : `/${value}`}`;
}

function setMeta(selector, attributes) {
  let element = document.head.querySelector(selector);
  if (!element) {
    element = document.createElement("meta");
    Object.entries(attributes.identity).forEach(([key, value]) => element.setAttribute(key, value));
    document.head.appendChild(element);
  }

  Object.entries(attributes.values).forEach(([key, value]) => {
    if (value) element.setAttribute(key, value);
  });
}

function setCanonical(url) {
  let element = document.head.querySelector("link[rel='canonical']");
  if (!element) {
    element = document.createElement("link");
    element.setAttribute("rel", "canonical");
    document.head.appendChild(element);
  }
  element.setAttribute("href", url);
}

function updateSeoMetadata(config) {
  const title = config?.title ? `${config.title} - Vidayoung` : "Vidayoung - Generando bienestar";
  const description = config?.description || "Bienestar, comunidad y productos funcionales para acompanar tu crecimiento diario.";
  const url = `${window.location.origin}/`;
  const imageUrl = absoluteUrl(config?.imageUrl || "");

  document.title = title;
  setCanonical(url);
  setMeta("meta[name='description']", {
    identity: { name: "description" },
    values: { content: description }
  });
  setMeta("meta[property='og:title']", {
    identity: { property: "og:title" },
    values: { content: title }
  });
  setMeta("meta[property='og:description']", {
    identity: { property: "og:description" },
    values: { content: description }
  });
  setMeta("meta[property='og:url']", {
    identity: { property: "og:url" },
    values: { content: url }
  });
  setMeta("meta[name='twitter:title']", {
    identity: { name: "twitter:title" },
    values: { content: title }
  });
  setMeta("meta[name='twitter:description']", {
    identity: { name: "twitter:description" },
    values: { content: description }
  });

  if (imageUrl) {
    setMeta("meta[property='og:image']", {
      identity: { property: "og:image" },
      values: { content: imageUrl }
    });
    setMeta("meta[name='twitter:image']", {
      identity: { name: "twitter:image" },
      values: { content: imageUrl }
    });
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

function handleScroll() {
  scrolled.value = window.scrollY > 24;
}

onMounted(() => {
  loadHome();
  handleScroll();
  window.addEventListener("scroll", handleScroll, { passive: true });
});

onBeforeUnmount(() => {
  window.removeEventListener("scroll", handleScroll);
});
</script>

<template>
  <div class="vy company-home">
    <header class="home-nav" :class="{ scrolled }">
      <RouterLink to="/" class="brand-link" aria-label="Ir a la pagina principal">
        <VyLogo :size="30" tagline />
      </RouterLink>
      <nav>
        <a href="#contenido">Empresa</a>
        <a href="#productos">Productos</a>
        <a href="#comunidad">Comunidad</a>
        <RouterLink to="/login" class="login-link"><LogIn :size="17" /> Iniciar sesion</RouterLink>
      </nav>
    </header>

    <main v-if="loading" class="state-box">Cargando pagina principal...</main>
    <main v-else-if="landing">
      <section class="page-banner-shell">
        <section class="page-banner">
          <div class="page-banner-media">
            <VyProductImage
              :grad="STATIC_BANNER_URL"
              :h="720"
              big
            />
          </div>
          <div class="page-banner-overlay"></div>
        </section>
      </section>

      <section class="hero-shell">
        <section class="home-hero">
          <div class="hero-copy">
            <span class="hero-kicker">{{ landing.category || "Empresa" }}</span>
            <h1>{{ heroSection?.title || landing.title }}</h1>
            <p>{{ heroSection?.text || landing.description }}</p>
            <div class="hero-actions">
              <RouterLink to="/login" class="primary-action"><LogIn :size="18" /> Iniciar sesion</RouterLink>
              <a href="#contenido" class="secondary-action">Conocer mas <ChevronRight :size="17" /></a>
            </div>
          </div>
        </section>
      </section>

      <section id="contenido" class="home-sections">
        <article v-for="(section, index) in contentSections" :key="index" class="home-section" :class="[section.type, section.layout]">
          <template v-if="section.type === 'text'">
            <div class="section-copy centered-copy">
              <span class="section-kicker">Empresa</span>
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
            </div>
          </template>

          <template v-else-if="section.type === 'imageText'">
            <div class="section-copy">
              <span class="section-kicker">Vidayoung</span>
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
            </div>
            <div class="section-visual">
              <VyProductImage :grad="section.imageUrl || landing.imageUrl" :h="360" />
            </div>
          </template>

          <template v-else-if="section.type === 'benefits'">
            <header class="section-copy">
              <span class="section-kicker">Diferenciales</span>
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
            </header>
            <div class="benefits-grid">
              <div v-for="item in section.images" :key="item" class="benefit-item">
                <strong>{{ item }}</strong>
                <span>Una propuesta clara, orientada a resultados y mejor experiencia para el usuario.</span>
              </div>
            </div>
          </template>

          <template v-else-if="['gallery', 'carousel'].includes(section.type)">
            <header class="section-copy">
              <span class="section-kicker">Productos</span>
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
            <div id="comunidad" class="faq-wrap">
              <span class="section-kicker">Ayuda</span>
              <h2>{{ section.title || "Preguntas frecuentes" }}</h2>
              <details v-for="item in section.images" :key="item">
                <summary>{{ faqQuestion(item) }}</summary>
                <p>{{ faqAnswer(item) }}</p>
              </details>
            </div>
          </template>

          <template v-else-if="section.type === 'social'">
            <header class="section-copy">
              <span class="section-kicker">Comunidad</span>
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
              <span class="section-kicker">Acceso</span>
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
    radial-gradient(circle at top left, rgba(242, 135, 5, 0.14), transparent 24%),
    linear-gradient(180deg, #fffaf3 0%, #f7efe2 20%, #fff 54%, #fff9f0 100%);
  color: var(--vy-ink);
}

.home-nav {
  min-height: 78px;
  padding: 12px clamp(18px, 5vw, 68px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: rgba(255, 255, 255, 0.14);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  position: sticky;
  top: 0;
  z-index: 30;
  backdrop-filter: blur(6px);
  transition: background-color .24s ease, border-color .24s ease, box-shadow .24s ease, backdrop-filter .24s ease;
}

.home-nav.scrolled {
  background: rgba(255, 249, 241, 0.72);
  border-bottom-color: rgba(194, 168, 133, 0.26);
  box-shadow: 0 12px 34px rgba(31, 26, 20, 0.08);
  backdrop-filter: blur(16px);
}

.brand-link {
  display: inline-flex;
  align-items: center;
}

.home-nav nav,
.hero-ribbon,
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

.home-nav nav > a,
.home-nav nav > :deep(a) {
  transition: background-color .2s ease, color .2s ease, transform .2s ease;
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

.page-banner-shell {
  width: 100%;
  margin: 0;
}

.page-banner {
  min-height: min(70vh, 720px);
  position: relative;
  overflow: hidden;
}

.page-banner-media {
  position: absolute;
  inset: 0;
}

.page-banner-media :deep(> div) {
  height: 100%;
  border-radius: 0 !important;
}

.page-banner-overlay {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(180deg, rgba(20, 15, 11, 0.1) 0%, rgba(20, 15, 11, 0.34) 72%, rgba(20, 15, 11, 0.54) 100%);
}

.hero-shell {
  width: min(100% - 32px, 1280px);
  margin: 0 auto;
  padding: 0 0 28px;
}

.home-hero {
  width: min(100%, 1100px);
  margin: -88px auto 0;
  position: relative;
  z-index: 2;
  padding: clamp(28px, 4vw, 42px);
  border: 1px solid rgba(117, 87, 44, 0.12);
  border-radius: 8px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(255, 247, 236, 0.95));
  box-shadow: 0 24px 64px rgba(31, 26, 20, 0.12);
}

.hero-copy {
  min-width: 0;
}

.hero-kicker,
.section-kicker {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid rgba(242, 135, 5, 0.22);
  border-radius: 999px;
  background: rgba(242, 135, 5, 0.1);
  color: var(--vy-orange-deep);
  font-size: 11px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.hero-copy h1 {
  max-width: 760px;
  margin-top: 20px;
  font-size: clamp(42px, 7vw, 84px);
  line-height: 0.96;
  font-weight: 950;
  overflow-wrap: anywhere;
}

.hero-copy p {
  max-width: 700px;
  margin-top: 20px;
  color: var(--vy-ink-2);
  font-size: clamp(16px, 1.6vw, 20px);
  line-height: 1.68;
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
  transition: transform .2s ease, background-color .2s ease, border-color .2s ease, color .2s ease;
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
  width: 100%;
  margin-top: 34px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.hero-metrics span {
  min-height: 92px;
  padding: 16px;
  border: 1px solid rgba(222, 191, 146, 0.5);
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

.hero-ribbon {
  margin-top: 18px;
  gap: 12px;
  padding: 18px 22px;
  border: 1px solid rgba(117, 87, 44, 0.12);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.86);
  justify-content: space-between;
  flex-wrap: wrap;
}

.hero-ribbon > div {
  min-width: 180px;
  flex: 1 1 0;
}

.hero-ribbon small,
.hero-ribbon strong {
  display: block;
}

.hero-ribbon small {
  color: var(--vy-ink-3);
  font-size: 11px;
  font-weight: 900;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.hero-ribbon strong {
  margin-top: 6px;
  color: var(--vy-ink);
  font-size: 15px;
  font-weight: 900;
}

.home-sections {
  width: min(100% - 32px, 1280px);
  margin: 0 auto;
  padding: 0 0 clamp(54px, 7vw, 92px);
  display: grid;
  gap: 18px;
}

.home-section {
  min-width: 0;
  padding: clamp(26px, 4vw, 48px);
  border: 1px solid rgba(31, 26, 20, 0.08);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 50px rgba(31, 26, 20, 0.06);
  overflow: hidden;
}

.section-copy {
  min-width: 0;
}

.centered-copy {
  width: min(100%, 900px);
  margin: 0 auto;
  text-align: center;
}

.home-section.text.centered {
  background: linear-gradient(180deg, #211b16 0%, #17130f 100%);
  color: #fff;
}

.home-section.imageText {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, 0.84fr);
  gap: clamp(20px, 4vw, 42px);
  align-items: center;
}

.home-section.imageText.imageLeft {
  grid-template-columns: minmax(300px, 0.84fr) minmax(0, 1fr);
}

.home-section.imageText.imageLeft > div:first-child {
  order: 2;
}

.section-visual :deep(> div),
.media-grid :deep(> div) {
  border-radius: 8px !important;
}

.home-section h2 {
  margin-top: 10px;
  font-size: clamp(30px, 4.2vw, 56px);
  line-height: 1.03;
  font-weight: 950;
  overflow-wrap: anywhere;
}

.home-section p {
  max-width: 780px;
  margin-top: 14px;
  color: var(--vy-ink-2);
  font-size: 16px;
  line-height: 1.72;
}

.benefits-grid,
.media-grid,
.social-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 24px;
}

.benefit-item {
  min-height: 144px;
  padding: 22px;
  border: 1px solid rgba(242, 135, 5, 0.14);
  border-radius: 8px;
  background: linear-gradient(180deg, #fff, #fbf4e7);
  box-shadow: 0 12px 30px rgba(31, 26, 20, 0.05);
}

.benefit-item strong,
.benefit-item span {
  display: block;
}

.benefit-item strong {
  color: var(--vy-ink);
  font-size: 16px;
  font-weight: 900;
  line-height: 1.45;
}

.benefit-item span {
  margin-top: 10px;
  color: var(--vy-ink-2);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.55;
}

.home-section.carousel .media-grid {
  grid-auto-flow: column;
  grid-auto-columns: minmax(260px, 32%);
  grid-template-columns: none;
  overflow-x: auto;
  padding-bottom: 8px;
}

.home-section.preguntas {
  background: linear-gradient(180deg, #1d1711 0%, #14100d 100%);
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
  min-height: 88px;
  justify-content: space-between;
  gap: 14px;
  padding: 0 20px;
  border-radius: 8px;
  background: linear-gradient(180deg, #211b16, #16120f);
  color: #fff;
  font-weight: 950;
  border: 1px solid rgba(242, 135, 5, 0.12);
  transition: transform .2s ease, background-color .2s ease;
}

.social-grid a:hover {
  background: var(--vy-orange);
  transform: translateY(-1px);
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
  .hero-shell,
  .home-sections {
    width: min(100% - 24px, 720px);
  }

  .page-banner {
    min-height: 52vh;
  }

  .home-hero {
    margin-top: -58px;
    width: min(100% - 24px, 680px);
  }

  .home-section.imageText,
  .home-section.imageText.imageLeft {
    grid-template-columns: 1fr;
  }

  .home-section.imageText.imageLeft > div:first-child {
    order: initial;
  }

  .hero-ribbon,
  .benefits-grid,
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

  .hero-shell,
  .home-sections {
    width: min(100% - 24px, 520px);
  }

  .page-banner {
    min-height: 40vh;
  }

  .home-hero {
    width: min(100% - 18px, 520px);
    margin-top: -36px;
    padding: 22px 18px;
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

  .hero-ribbon {
    padding: 16px;
  }

  .home-section {
    padding: 20px;
  }
}
</style>
