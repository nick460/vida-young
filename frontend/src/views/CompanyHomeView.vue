<script setup>
import { computed, inject, onBeforeUnmount, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import { ArrowRight, LogIn, Menu, MessageCircle, Music2, X, Youtube } from "lucide-vue-next";
import { VyLogo, VyProductImage } from "../components/ui.js";
import { loadCompanyHome } from "../services/companyHomeService.js";
import logoMark from "../assets/logoMark.png";
import blackHarmonyImage from "../assets/products/wheel/03_Black_Harmony_Coffee_1200x1200.webp";
import collagenImage from "../assets/products/wheel/02_Colageno_Hidrolizado_1200x1200.webp";
import mentolFreshImage from "../assets/products/wheel/01_MentolFresh_1200x1200.webp";
import chocolateProShakeImage from "../assets/products/wheel/Chocolate_Pro_Shake_1200x1200.webp";
import detoxsenImage from "../assets/products/wheel/Detoxsen_VidaYoung_1200x1200.webp";
import guaranaEnergyImage from "../assets/products/wheel/Guarana_Energy_VidaYoung_1200x1200.webp";

const HERO_PRODUCTS = [
  { src: blackHarmonyImage, name: "Black Harmony Coffee Gourmet" },
  { src: collagenImage, name: "Colágeno Hidrolizado" },
  { src: mentolFreshImage, name: "Mentol Fresh" },
  { src: chocolateProShakeImage, name: "Chocolate Pro Shake" },
  { src: detoxsenImage, name: "Detoxsen" },
  { src: guaranaEnergyImage, name: "Guaraná Energy" }
];

const loading = ref(false);
const landing = ref(null);
const scrolled = ref(false);
const loadError = ref("");
const mobileMenuOpen = ref(false);
const currentProductIndex = ref(0);
const publicIntroReady = inject("publicIntroReady", ref(true));
let productWheelTimer = null;
let revealObserver = null;

function observeRevealElement(element) {
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (reduceMotion || !("IntersectionObserver" in window)) {
    element.classList.add("is-visible");
    return;
  }

  if (!revealObserver) {
    revealObserver = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          entry.target.classList.add("is-visible");
          revealObserver?.unobserve(entry.target);
        });
      },
      { threshold: 0.12, rootMargin: "0px 0px -8% 0px" }
    );
  }

  revealObserver.observe(element);
}

const vReveal = {
  mounted: observeRevealElement,
  unmounted(element) {
    revealObserver?.unobserve(element);
  }
};

const sections = computed(() => landing.value?.sections || []);
const heroSection = computed(() => sections.value.find((section) => section.type === "hero"));
const contentSections = computed(() => sections.value.filter((section) => !["hero", "social"].includes(section.type)));
const navSections = computed(() =>
  contentSections.value.reduce((items, section, index) => {
    if (items.length >= 4) return items;
    const label = sectionBadge(section, index);
    if (!label || items.some((item) => item.label.toLowerCase() === label.toLowerCase())) {
      return items;
    }

    items.push({
      id: sectionAnchorId(section, index),
      label
    });
    return items;
  }, [])
);
const primarySectionId = computed(() => {
  const index = contentSections.value.findIndex((section) => ["benefits", "gallery", "carousel"].includes(section.type));
  const targetIndex = index >= 0 ? index : 0;
  const section = contentSections.value[targetIndex];
  return section ? sectionAnchorId(section, targetIndex) : "";
});
const currentHeroProduct = computed(() => HERO_PRODUCTS[currentProductIndex.value]);

async function loadHome() {
  loading.value = true;
  loadError.value = "";
  try {
    landing.value = await loadCompanyHome();
    updateSeoMetadata(landing.value);
  } catch {
    loadError.value = "No pudimos cargar la página en este momento.";
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

function visibleFaqItems(items = []) {
  return items.filter((item) => !faqQuestion(item).toLowerCase().includes("pagina se puede editar"));
}

function sectionDescription(section) {
  const text = String(section?.text || "").trim();
  if (text.toLowerCase().startsWith("agrega imagenes")) {
    return "Conoce una selección de productos pensados para acompañar tu bienestar cada día.";
  }
  return text;
}

function benefitDescription(index) {
  return [
    "Fórmulas pensadas para acompañar tus objetivos y sumar bienestar a tu rutina.",
    "Recursos prácticos para aprender, compartir y desarrollar tu proyecto digital.",
    "Una red cercana para crecer con acompañamiento, propósito y experiencias compartidas."
  ][index % 3];
}

function slugify(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

function sectionAnchorId(section, index) {
  const base = slugify(section?.title || section?.type || `seccion-${index + 1}`);
  return base || `seccion-${index + 1}`;
}

function navSectionLabel(section, index) {
  const title = String(section?.title || "").trim();
  if (title) {
    return title;
  }

  const typeLabels = {
    text: "Informacion",
    imageText: "Contenido",
    benefits: "Beneficios",
    gallery: "Galeria",
    carousel: "Galeria",
    preguntas: "Preguntas",
    social: "Redes",
    contact: "Contacto"
  };

  return typeLabels[section?.type] || `Seccion ${index + 1}`;
}

function sectionBadge(section, index) {
  return String(section?.category || "").trim() || navSectionLabel(section, index);
}

function handleScroll() {
  scrolled.value = window.scrollY > 24;
}

function closeMobileMenu() {
  mobileMenuOpen.value = false;
}

function stopProductWheel() {
  if (productWheelTimer) window.clearInterval(productWheelTimer);
  productWheelTimer = null;
}

function startProductWheel() {
  stopProductWheel();
  if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
  productWheelTimer = window.setInterval(() => {
    currentProductIndex.value = (currentProductIndex.value + 1) % HERO_PRODUCTS.length;
  }, 3400);
}

onMounted(() => {
  loadHome();
  startProductWheel();
  handleScroll();
  window.addEventListener("scroll", handleScroll, { passive: true });
});

onBeforeUnmount(() => {
  stopProductWheel();
  revealObserver?.disconnect();
  revealObserver = null;
  window.removeEventListener("scroll", handleScroll);
});
</script>

<template>
  <div class="vy company-home">
    <header class="home-nav" :class="{ scrolled }">
      <RouterLink to="/" class="brand-link" aria-label="Ir a la pagina principal">
        <VyLogo :size="30" tagline />
      </RouterLink>
      <div class="header-right">
        <nav class="desktop-nav" aria-label="Navegación principal">
          <a v-for="section in navSections" :key="section.id" :href="`#${section.id}`" @click="closeMobileMenu">{{ section.label }}</a>
        </nav>
        <button
          class="public-menu-toggle"
          type="button"
          :aria-expanded="mobileMenuOpen"
          aria-controls="mobile-navigation"
          :aria-label="mobileMenuOpen ? 'Cerrar navegación' : 'Abrir navegación'"
          @click="mobileMenuOpen = !mobileMenuOpen"
        >
          <X v-if="mobileMenuOpen" :size="20" />
          <Menu v-else :size="20" />
        </button>
      </div>
    </header>

    <Teleport to="body">
      <Transition name="menu-fade">
        <button
          v-if="mobileMenuOpen"
          class="mobile-menu-backdrop"
          type="button"
          aria-label="Cerrar navegación"
          @click="closeMobileMenu"
        ></button>
      </Transition>
      <Transition name="menu-drawer">
        <aside
          v-if="mobileMenuOpen"
          id="mobile-navigation"
          class="mobile-nav-drawer"
          role="dialog"
          aria-modal="true"
          aria-label="Navegación móvil"
        >
          <div class="mobile-nav-head">
            <div>
              <small>Vidayoung</small>
              <strong>Explora nuestro espacio</strong>
            </div>
            <button type="button" aria-label="Cerrar navegación" @click="closeMobileMenu">
              <X :size="20" />
            </button>
          </div>
          <nav class="mobile-drawer-links" aria-label="Secciones">
            <a v-for="section in navSections" :key="section.id" :href="`#${section.id}`" @click="closeMobileMenu">{{ section.label }}</a>
          </nav>
          <div class="mobile-nav-foot">
            <span>Generando bienestar</span>
            <i></i>
          </div>
        </aside>
      </Transition>
    </Teleport>

    <main v-if="loading" class="state-box">Cargando pagina principal...</main>
    <main v-else-if="loadError" class="state-box state-error">
      <strong>{{ loadError }}</strong>
      <button type="button" class="primary-action" @click="loadHome">Intentar nuevamente</button>
    </main>
    <main v-else-if="landing">
      <section class="home-hero" :class="{ 'intro-ready': publicIntroReady }">
        <img :src="logoMark" class="hero-watermark" alt="" aria-hidden="true" />
        <div class="hero-composition">
          <div class="hero-copy">
            <span class="hero-kicker">{{ landing.category || "Bienestar con propósito" }}</span>
            <h1>{{ heroSection?.title || landing.title }}</h1>
            <p>{{ heroSection?.text || landing.description }}</p>
            <div class="hero-actions">
              <a v-if="primarySectionId" class="primary-action" :href="`#${primarySectionId}`">
                Explorar Vidayoung <ArrowRight :size="18" />
              </a>
              <RouterLink to="/login" class="secondary-action">Ya soy parte</RouterLink>
            </div>
          </div>

          <div class="hero-visual" aria-hidden="true">
            <span class="visual-glow"></span>
            <span class="visual-orbit orbit-large"></span>
            <span class="visual-orbit orbit-small"></span>
            <span class="visual-dot dot-one"></span>
            <span class="visual-dot dot-two"></span>
            <span class="visual-dot dot-three"></span>

            <div class="product-showcase" @mouseenter="stopProductWheel" @mouseleave="startProductWheel">
              <Transition name="product-wheel" appear>
                <figure :key="currentHeroProduct.src" class="wheel-product">
                  <img :src="currentHeroProduct.src" alt="" />
                </figure>
              </Transition>
              <div class="wheel-progress">
                <span
                  v-for="(_, productIndex) in HERO_PRODUCTS"
                  :key="productIndex"
                  :class="{ active: productIndex === currentProductIndex }"
                ></span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="home-sections">
        <article
          v-for="(section, index) in contentSections"
          v-reveal
          :id="sectionAnchorId(section, index)"
          :key="index"
          class="home-section scroll-reveal"
          :class="[section.type, section.layout]"
        >
          <template v-if="section.type === 'text'">
            <div class="section-copy centered-copy">
              <span class="section-kicker">{{ sectionBadge(section, index) }}</span>
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
            </div>
          </template>

          <template v-else-if="section.type === 'imageText'">
            <div class="section-copy">
              <span class="section-kicker">{{ sectionBadge(section, index) }}</span>
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
            </div>
            <div class="section-visual">
              <VyProductImage :grad="section.imageUrl || landing.imageUrl" :h="360" />
            </div>
          </template>

          <template v-else-if="section.type === 'benefits'">
            <header class="section-copy">
              <span class="section-kicker">{{ sectionBadge(section, index) }}</span>
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
            </header>
            <div class="benefits-grid">
              <div v-for="(item, benefitIndex) in section.images" :key="item" class="benefit-item">
                <strong>{{ item }}</strong>
                <span>{{ benefitDescription(benefitIndex) }}</span>
              </div>
            </div>
          </template>

          <template v-else-if="['gallery', 'carousel'].includes(section.type)">
            <header class="section-copy">
              <span class="section-kicker">{{ sectionBadge(section, index) }}</span>
              <h2>{{ section.title }}</h2>
              <p v-if="sectionDescription(section)">{{ sectionDescription(section) }}</p>
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
              <span class="section-kicker">{{ sectionBadge(section, index) }}</span>
              <h2>{{ section.title || "Preguntas frecuentes" }}</h2>
              <details v-for="item in visibleFaqItems(section.images)" :key="item">
                <summary>{{ faqQuestion(item) }}</summary>
                <p>{{ faqAnswer(item) }}</p>
              </details>
            </div>
          </template>

          <template v-else-if="section.type === 'contact'">
            <div class="contact-block">
              <span class="section-kicker">{{ sectionBadge(section, index) }}</span>
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
              <RouterLink to="/login" class="primary-action"><LogIn :size="18" /> Iniciar sesion</RouterLink>
            </div>
          </template>
        </article>
      </section>

      <section v-reveal class="final-cta scroll-reveal">
        <span class="section-kicker">Tu siguiente paso</span>
        <h2>Bienestar que crece en comunidad.</h2>
        <p>Descubre la propuesta Vidayoung y encuentra una forma más cercana de avanzar hacia tus objetivos.</p>
        <div class="final-cta-actions">
          <RouterLink to="/landing" class="primary-action">Conocer la oportunidad <ArrowRight :size="18" /></RouterLink>
          <RouterLink to="/login" class="final-login">Ya tengo una cuenta</RouterLink>
        </div>
      </section>

      <footer v-reveal class="home-footer scroll-reveal">
        <div class="footer-brand">
          <VyLogo :size="24" dark />
          <p>{{ landing.description }}</p>
          <div class="footer-socials" aria-label="Canales oficiales Vidayoung">
            <a href="https://www.youtube.com/@vidayoungoficial" target="_blank" rel="noreferrer" aria-label="YouTube Vidayoung">
              <Youtube :size="18" />
              <span>YouTube</span>
            </a>
            <a href="https://www.tiktok.com/@vidayoung.bolivia?_r=1&_t=ZS-98JLs3EFWpb" target="_blank" rel="noreferrer" aria-label="TikTok Vidayoung">
              <Music2 :size="18" />
              <span>TikTok</span>
            </a>
            <a href="https://wa.me/59168880318" target="_blank" rel="noreferrer" aria-label="WhatsApp Vidayoung">
              <MessageCircle :size="18" />
              <span>WhatsApp</span>
            </a>
          </div>
        </div>
        <small>
          Vidayoung © {{ new Date().getFullYear() }}. Todos los derechos reservados.
          <a href="https://rubina-solutions.tech/" target="_blank" rel="noreferrer">Desarrollado por NRS System Solutions</a>
        </small>
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

.header-right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  min-width: 0;
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
  gap: 4px;
  justify-content: flex-end;
  flex-wrap: wrap;
  min-width: 0;
}

.home-nav nav > a,
.home-nav nav > :deep(a) {
  transition: color .2s ease, transform .2s ease, border-color .2s ease, background-color .2s ease;
}

.home-nav nav > a:not(.login-link) {
  min-height: 38px;
  padding: 0 12px;
  border-radius: 10px;
  border: 1px solid transparent;
  background: transparent;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  font-weight: 800;
}

.home-nav nav > a:not(.login-link):hover {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.14);
  color: #fff;
  transform: translateY(-1px);
}

.login-link {
  min-height: 44px;
  justify-content: center;
  gap: 9px;
  padding: 0 18px;
  border-radius: 999px;
  border: 1px solid rgba(242, 135, 5, 0.34);
  background: linear-gradient(135deg, rgba(242, 135, 5, 0.98), rgba(210, 101, 0, 0.96));
  color: #fff;
  font-weight: 900;
  box-shadow: 0 12px 28px rgba(184, 97, 10, 0.3);
}

.login-link:hover {
  transform: translateY(-1px);
  box-shadow: 0 16px 32px rgba(184, 97, 10, 0.34);
}

.home-nav.scrolled nav > a:not(.login-link) {
  border-color: transparent;
  background: transparent;
  color: var(--vy-ink-2);
}

.home-nav.scrolled nav > a:not(.login-link):hover {
  background: rgba(242, 135, 5, 0.08);
  border-color: rgba(242, 135, 5, 0.12);
  color: var(--vy-ink);
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
  background: #1a1511;
}

.page-banner-media picture {
  display: block;
  width: 100%;
  height: 100%;
}

.page-banner-image {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  object-position: center 22%;
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
  width: 100%;
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
  font-size: clamp(40px, 6vw, 78px);
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

.footer-brand {
  display: grid;
  gap: 16px;
}

.footer-socials {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.footer-socials a {
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.88);
  font-size: 13px;
  font-weight: 900;
  transition: background-color .2s ease, color .2s ease, transform .2s ease;
}

.footer-socials a:hover {
  background: rgba(255, 255, 255, 0.14);
  color: var(--vy-orange);
  transform: translateY(-1px);
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

.home-footer small a {
  min-height: auto;
  margin-left: 10px;
  padding: 0;
  border-radius: 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 12px;
  font-weight: 900;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.home-footer small a:hover {
  background: transparent;
  color: var(--vy-orange);
}

@media (max-width: 920px) {
  .hero-shell,
  .home-sections {
    width: min(100% - 24px, 720px);
  }

  .page-banner {
    min-height: 52vh;
  }

  .page-banner-image {
    object-position: center top;
  }

  .home-hero {
    margin-top: -58px;
    width: 100%;
    padding: 26px 24px;
  }

  .hero-copy h1 {
    font-size: clamp(38px, 7vw, 62px);
  }

  .hero-copy p {
    max-width: 100%;
    font-size: 15px;
    line-height: 1.58;
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

  .header-right {
    gap: 10px;
    flex-wrap: wrap;
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

  .header-right {
    margin-left: auto;
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
    min-height: 34vh;
  }

  .page-banner-image {
    object-fit: cover;
    object-position: center top;
  }

  .home-hero {
    width: 100%;
    margin-top: -36px;
    padding: 22px 18px;
  }

  .hero-copy h1 {
    font-size: clamp(32px, 11vw, 44px);
  }

  .hero-copy p {
    font-size: 14px;
    line-height: 1.54;
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

  .section-visual :deep(> div),
  .media-grid :deep(> div) {
    background: #fff !important;
  }

  .section-visual :deep(img),
  .media-grid :deep(img) {
    object-fit: contain !important;
    object-position: center center !important;
    padding: 6px;
    background: #fff;
  }
}

/* Composición pública simplificada */
.company-home {
  background: #fffdf8;
}

.home-nav,
.home-nav.scrolled {
  min-height: 72px;
  padding: 10px clamp(18px, 4vw, 56px);
  border-bottom: 1px solid rgba(31, 26, 20, 0.08);
  background: rgba(255, 253, 248, 0.9);
  box-shadow: none;
  backdrop-filter: blur(18px);
}

.home-nav.scrolled {
  box-shadow: 0 8px 30px rgba(31, 26, 20, 0.07);
}

.home-nav nav > a:not(.login-link),
.home-nav.scrolled nav > a:not(.login-link) {
  min-height: 42px;
  color: var(--vy-ink-2);
}

.home-nav nav > a:not(.login-link):hover,
.home-nav.scrolled nav > a:not(.login-link):hover {
  border-color: transparent;
  color: var(--vy-orange-deep);
  background: rgba(242, 135, 5, 0.08);
}

.login-link {
  min-height: 42px;
  padding: 0 16px;
  box-shadow: none;
}

.public-menu-toggle {
  display: none;
  width: 42px;
  height: 42px;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--vy-line);
  border-radius: 12px;
  background: #fff;
  color: var(--vy-ink);
}

.mobile-nav-head,
.mobile-nav-foot,
.mobile-menu-backdrop,
.mobile-nav-drawer {
  display: none;
}

.home-hero {
  position: relative;
  width: 100%;
  height: calc(100svh - 72px);
  min-height: 0;
  margin: 0;
  padding: clamp(24px, 3.5vw, 52px);
  display: flex;
  align-items: center;
  overflow: hidden;
  border: 0;
  border-radius: 0;
  background:
    radial-gradient(circle at 72% 45%, rgba(242, 135, 5, 0.1), transparent 30%),
    linear-gradient(112deg, #120f0c, #1b1611 52%, #120f0c);
  box-shadow: none;
}

.hero-watermark {
  position: absolute;
  left: -4%;
  bottom: -24%;
  width: clamp(300px, 34vw, 540px);
  height: auto;
  opacity: 0.035;
  filter: grayscale(1) brightness(2.4);
  pointer-events: none;
  user-select: none;
}

.hero-composition {
  position: relative;
  z-index: 1;
  width: min(100%, 1420px);
  height: 100%;
  min-height: 0;
  margin: 0 auto;
  padding: clamp(34px, 4vw, 62px);
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(430px, 0.98fr);
  align-items: center;
  gap: clamp(20px, 3vw, 52px);
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 38px;
  background:
    radial-gradient(circle at 69% 48%, rgba(242, 135, 5, 0.15), transparent 30%),
    radial-gradient(circle at 18% 32%, rgba(242, 184, 133, 0.055), transparent 28%),
    linear-gradient(118deg, rgba(255, 255, 255, 0.035), rgba(255, 255, 255, 0.012));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.05), 0 28px 80px rgba(0, 0, 0, 0.16);
}

.home-hero.intro-ready .hero-composition {
  animation: hero-stage-arrive 0.9s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.home-hero.intro-ready .hero-visual {
  animation: hero-visual-arrive 1s 0.12s cubic-bezier(0.16, 1, 0.3, 1) both;
}

.home-hero.intro-ready .hero-copy > * {
  animation: hero-copy-arrive 0.72s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.home-hero.intro-ready .hero-copy > :nth-child(1) { animation-delay: 0.16s; }
.home-hero.intro-ready .hero-copy > :nth-child(2) { animation-delay: 0.24s; }
.home-hero.intro-ready .hero-copy > :nth-child(3) { animation-delay: 0.32s; }
.home-hero.intro-ready .hero-copy > :nth-child(4) { animation-delay: 0.4s; }

.scroll-reveal {
  opacity: 0;
  transform: translateY(42px);
  transition: opacity 0.72s ease, transform 0.88s cubic-bezier(0.16, 1, 0.3, 1);
  will-change: opacity, transform;
}

.scroll-reveal.is-visible {
  opacity: 1;
  transform: translateY(0);
  will-change: auto;
}

.scroll-reveal .section-copy > *,
.scroll-reveal .centered-copy > *,
.scroll-reveal .faq-wrap > *,
.scroll-reveal .contact-block > *,
.scroll-reveal.final-cta > *,
.scroll-reveal .footer-brand > *,
.scroll-reveal.home-footer > small,
.scroll-reveal .section-visual,
.scroll-reveal .benefit-item,
.scroll-reveal .media-grid > *,
.scroll-reveal .footer-socials > a {
  transition: opacity 0.58s ease, transform 0.72s cubic-bezier(0.22, 1, 0.36, 1);
}

.scroll-reveal:not(.is-visible) .section-copy > *,
.scroll-reveal:not(.is-visible) .centered-copy > *,
.scroll-reveal:not(.is-visible) .faq-wrap > *,
.scroll-reveal:not(.is-visible) .contact-block > *,
.scroll-reveal.final-cta:not(.is-visible) > *,
.scroll-reveal:not(.is-visible) .footer-brand > *,
.scroll-reveal.home-footer:not(.is-visible) > small,
.scroll-reveal:not(.is-visible) .benefit-item,
.scroll-reveal:not(.is-visible) .media-grid > *,
.scroll-reveal:not(.is-visible) .footer-socials > a {
  opacity: 0;
  transform: translateY(20px);
}

.scroll-reveal:not(.is-visible) .section-visual {
  opacity: 0;
  transform: translateX(32px) scale(0.98);
}

.scroll-reveal .section-copy > :nth-child(1),
.scroll-reveal .centered-copy > :nth-child(1),
.scroll-reveal .faq-wrap > :nth-child(1),
.scroll-reveal .contact-block > :nth-child(1),
.scroll-reveal.final-cta > :nth-child(1) { transition-delay: 0.06s; }

.scroll-reveal .section-copy > :nth-child(2),
.scroll-reveal .centered-copy > :nth-child(2),
.scroll-reveal .faq-wrap > :nth-child(2),
.scroll-reveal .contact-block > :nth-child(2),
.scroll-reveal.final-cta > :nth-child(2) { transition-delay: 0.12s; }

.scroll-reveal .section-copy > :nth-child(3),
.scroll-reveal .centered-copy > :nth-child(3),
.scroll-reveal .faq-wrap > :nth-child(3),
.scroll-reveal .contact-block > :nth-child(3),
.scroll-reveal.final-cta > :nth-child(3) { transition-delay: 0.18s; }

.scroll-reveal .section-copy > :nth-child(4),
.scroll-reveal .centered-copy > :nth-child(4),
.scroll-reveal .faq-wrap > :nth-child(4),
.scroll-reveal .contact-block > :nth-child(4),
.scroll-reveal.final-cta > :nth-child(4),
.scroll-reveal .section-visual { transition-delay: 0.24s; }

.scroll-reveal .benefit-item:nth-child(1),
.scroll-reveal .media-grid > :nth-child(1),
.scroll-reveal .footer-socials > :nth-child(1) { transition-delay: 0.14s; }
.scroll-reveal .benefit-item:nth-child(2),
.scroll-reveal .media-grid > :nth-child(2),
.scroll-reveal .footer-socials > :nth-child(2) { transition-delay: 0.2s; }
.scroll-reveal .benefit-item:nth-child(3),
.scroll-reveal .media-grid > :nth-child(3),
.scroll-reveal .footer-socials > :nth-child(3) { transition-delay: 0.26s; }
.scroll-reveal .benefit-item:nth-child(4),
.scroll-reveal .media-grid > :nth-child(4) { transition-delay: 0.32s; }
.scroll-reveal .media-grid > :nth-child(5) { transition-delay: 0.38s; }
.scroll-reveal .media-grid > :nth-child(6) { transition-delay: 0.44s; }

.hero-composition::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: 0;
  opacity: 0.11;
  background-image: radial-gradient(rgba(242, 184, 133, 0.72) 1px, transparent 1px);
  background-size: 23px 23px;
  mask-image: radial-gradient(ellipse at 62% 50%, #000 0%, rgba(0, 0, 0, 0.75) 45%, transparent 88%);
  pointer-events: none;
}

.hero-visual {
  position: relative;
  z-index: 1;
  width: 100%;
  height: min(56vh, 530px);
  overflow: visible;
  background: transparent;
}

.visual-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 420px;
  height: 420px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(242, 135, 5, 0.24), rgba(242, 135, 5, 0.04) 48%, transparent 70%);
  transform: translate(-50%, -50%);
  animation: visual-breathe 3.4s ease-in-out infinite alternate;
}

.visual-orbit {
  position: absolute;
  top: 50%;
  left: 50%;
  border: 1px solid rgba(242, 184, 133, 0.2);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.orbit-large {
  width: min(39vw, 520px);
  height: min(39vw, 520px);
  animation: orbit-spin 28s linear infinite;
}

.orbit-large::before,
.orbit-large::after,
.orbit-small::before {
  content: "";
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--vy-orange);
  box-shadow: 0 0 0 7px rgba(242, 135, 5, 0.1);
}

.orbit-large::before { top: 10%; left: 14%; }
.orbit-large::after { right: -5px; top: 48%; }

.orbit-small {
  width: min(29vw, 370px);
  height: min(29vw, 370px);
  border-style: dashed;
  animation: orbit-spin-reverse 22s linear infinite;
}

.orbit-small::before { left: 18%; bottom: 5%; width: 7px; height: 7px; }

.visual-dot {
  position: absolute;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--vy-beige);
  box-shadow: 0 0 18px currentColor;
}

.dot-one { top: 20%; left: 22%; }
.dot-two { top: 29%; right: 13%; }
.dot-three { bottom: 18%; right: 25%; }

.product-showcase {
  position: absolute;
  top: 50%;
  left: 50%;
  width: min(100%, 560px);
  height: min(54vh, 500px);
  display: grid;
  place-items: center;
  perspective: 1100px;
  transform: translate(-50%, -50%);
}

.wheel-product {
  position: absolute;
  inset: 0 0 28px;
  margin: 0;
  display: grid;
  place-items: center;
  transform-style: preserve-3d;
}

.wheel-product img {
  width: auto;
  max-width: 88%;
  height: auto;
  max-height: 100%;
  display: block;
  object-fit: contain;
  filter: drop-shadow(0 32px 28px rgba(0, 0, 0, 0.38));
}

.wheel-progress {
  position: absolute;
  left: 50%;
  bottom: 2px;
  display: flex;
  align-items: center;
  gap: 8px;
  transform: translateX(-50%);
}

.wheel-progress span {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
  transition: width 0.3s ease, background-color 0.3s ease;
}

.wheel-progress span.active {
  width: 26px;
  background: var(--vy-orange);
}

.product-wheel-enter-active,
.product-wheel-leave-active {
  transition: opacity 0.58s ease, transform 0.68s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: opacity, transform;
}

.product-wheel-enter-active {
  z-index: 2;
}

.product-wheel-leave-active {
  z-index: 1;
}

.product-wheel-enter-from {
  opacity: 0;
  transform: scale(0.94);
}

.product-wheel-leave-to {
  opacity: 0;
  transform: scale(1.035);
}

.product-scene {
  position: absolute;
  top: 51%;
  left: 52%;
  width: min(38vw, 500px);
  height: 390px;
  transform: translate(-50%, -50%);
  animation: product-float 4.6s ease-in-out infinite alternate;
}

.product-shadow {
  position: absolute;
  left: 50%;
  bottom: 14px;
  width: 270px;
  height: 42px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.5);
  filter: blur(18px);
  transform: translateX(-50%);
}

.product-bottle {
  position: absolute;
  z-index: 3;
  bottom: 35px;
  left: 50%;
  width: 138px;
  height: 285px;
  transform: translateX(-50%);
  filter: drop-shadow(0 28px 30px rgba(0, 0, 0, 0.34));
}

.bottle-cap {
  position: absolute;
  top: 0;
  left: 43px;
  width: 52px;
  height: 28px;
  border-radius: 8px 8px 4px 4px;
  background: linear-gradient(90deg, #15120f, #4d4235 50%, #17130f);
}

.bottle-neck {
  position: absolute;
  top: 23px;
  left: 50px;
  width: 38px;
  height: 24px;
  background: #efe8d9;
}

.bottle-body {
  position: absolute;
  inset: 42px 0 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 24px 24px 30px 30px;
  border: 1px solid rgba(255, 255, 255, 0.68);
  background: linear-gradient(110deg, #d8d0c3 0%, #fff 25%, #f6f0e7 72%, #cfc4b4 100%);
  box-shadow: inset -10px 0 22px rgba(99, 75, 45, 0.13), inset 8px 0 18px rgba(255, 255, 255, 0.9);
}

.bottle-body img {
  width: 42px;
  height: 42px;
}

.bottle-body strong {
  margin-top: 12px;
  color: var(--vy-orange-deep);
  font-size: 14px;
  font-weight: 900;
}

.bottle-body small {
  margin-top: 5px;
  color: #6f6559;
  font-size: 7px;
  font-weight: 800;
  letter-spacing: 0.13em;
  text-transform: uppercase;
}

.product-pack {
  position: absolute;
  bottom: 42px;
  width: 132px;
  height: 196px;
  padding: 24px 18px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  border-radius: 18px;
  box-shadow: 0 24px 42px rgba(0, 0, 0, 0.3);
}

.product-pack strong {
  font-size: 18px;
  font-weight: 950;
  letter-spacing: 0.04em;
}

.product-pack small {
  margin-top: 6px;
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.product-pack-left {
  left: 54px;
  color: #fff;
  background: linear-gradient(155deg, #f7a025, #d56800 65%, #743300);
  transform: rotate(-8deg);
}

.pack-seal {
  margin-bottom: auto;
  color: rgba(255, 255, 255, 0.8);
  font-size: 11px;
  font-weight: 950;
}

.product-pack-right {
  right: 50px;
  color: var(--vy-cream);
  background: linear-gradient(155deg, #453a2c, #17130f 68%);
  transform: rotate(8deg);
}

.product-pack-right svg {
  margin-bottom: auto;
  color: var(--vy-orange);
}

.visual-card {
  position: absolute;
  z-index: 5;
  min-width: 170px;
  min-height: 64px;
  padding: 10px 14px;
  display: flex;
  align-items: center;
  gap: 11px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  color: #fff;
  background: rgba(35, 28, 21, 0.78);
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(14px);
  animation: card-drift 3.5s ease-in-out infinite alternate;
}

.visual-card > span {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  border-radius: 11px;
  color: var(--vy-orange);
  background: rgba(242, 135, 5, 0.12);
}

.visual-card small,
.visual-card strong {
  display: block;
}

.visual-card small {
  color: rgba(255, 255, 255, 0.52);
  font-size: 8px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.visual-card strong {
  margin-top: 4px;
  font-size: 11px;
  font-weight: 850;
}

.card-wellness { top: 16%; left: 4%; }
.card-community { top: 25%; right: 3%; animation-delay: -1.4s; }
.card-growth { right: 8%; bottom: 13%; animation-delay: -2.4s; }

@keyframes product-float {
  from { transform: translate(-50%, -49%); }
  to { transform: translate(-50%, -53%); }
}

@keyframes visual-breathe {
  from { opacity: 0.55; transform: translate(-50%, -50%) scale(0.9); }
  to { opacity: 1; transform: translate(-50%, -50%) scale(1.08); }
}

@keyframes orbit-spin {
  from { transform: translate(-50%, -50%) rotate(0); }
  to { transform: translate(-50%, -50%) rotate(360deg); }
}

@keyframes orbit-spin-reverse {
  from { transform: translate(-50%, -50%) rotate(360deg); }
  to { transform: translate(-50%, -50%) rotate(0); }
}

@keyframes card-drift {
  from { transform: translateY(0); }
  to { transform: translateY(-9px); }
}

.home-hero .hero-copy {
  position: relative;
  z-index: 2;
  width: 100%;
}

.home-hero .hero-kicker {
  color: #fff4df;
  border-color: rgba(255, 255, 255, 0.24);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(8px);
}

.home-hero .hero-copy h1 {
  max-width: 700px;
  color: #fff;
  font-size: clamp(46px, 6.4vw, 88px);
  line-height: 0.94;
  text-wrap: balance;
}

.home-hero .hero-copy p {
  max-width: 620px;
  color: rgba(255, 255, 255, 0.8);
  font-size: clamp(16px, 1.45vw, 20px);
  line-height: 1.6;
}

.home-hero .secondary-action {
  border-color: rgba(255, 255, 255, 0.28);
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.home-sections {
  width: min(100% - 40px, 1160px);
  padding: clamp(64px, 9vw, 120px) 0;
  gap: 0;
}

.home-section {
  padding: clamp(64px, 8vw, 104px) 0;
  border: 0;
  border-bottom: 1px solid rgba(31, 26, 20, 0.1);
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  scroll-margin-top: 92px;
}

.home-section:first-child {
  padding-top: 0;
}

.home-section:last-child {
  border-bottom: 0;
}

.home-section.text.centered,
.home-section.preguntas,
.home-section.contact {
  margin: clamp(34px, 5vw, 64px) 0;
  padding: clamp(42px, 6vw, 72px);
  border: 0;
  border-radius: 28px;
  box-shadow: 0 24px 70px rgba(31, 26, 20, 0.12);
}

.home-section.text.centered p {
  color: rgba(255, 255, 255, 0.72);
}

.home-section h2 {
  max-width: 820px;
  font-size: clamp(32px, 4vw, 54px);
  text-wrap: balance;
}

.home-section p {
  max-width: 720px;
  font-size: clamp(15px, 1.4vw, 18px);
}

.home-section.imageText,
.home-section.imageText.imageLeft {
  grid-template-columns: minmax(0, 0.95fr) minmax(360px, 1.05fr);
  gap: clamp(40px, 7vw, 88px);
}

.section-visual :deep(> div),
.media-grid :deep(> div) {
  overflow: hidden;
  border: 0 !important;
  border-radius: 22px !important;
  box-shadow: 0 20px 60px rgba(31, 26, 20, 0.1);
}

.benefits-grid {
  gap: 18px;
  margin-top: 40px;
}

.benefit-item {
  min-height: 190px;
  padding: 28px;
  border-color: rgba(242, 135, 5, 0.16);
  border-radius: 18px;
  background: #fffaf1;
  box-shadow: none;
}

.benefit-item strong {
  font-size: 18px;
}

.benefit-item span {
  margin-top: 14px;
  font-weight: 600;
}

.home-section.gallery .media-grid,
.home-section.carousel .media-grid {
  margin-top: 40px;
}

.home-section.contact {
  color: #fff;
  background: linear-gradient(135deg, var(--vy-orange-deep), var(--vy-orange));
}

.home-section.contact .section-kicker {
  color: #fff;
  border-color: rgba(255, 255, 255, 0.28);
  background: rgba(255, 255, 255, 0.12);
}

.home-section.contact p {
  color: rgba(255, 255, 255, 0.82);
}

.home-section.contact .primary-action {
  color: var(--vy-ink);
  background: #fff;
  box-shadow: none;
}

.state-error {
  flex-direction: column;
  gap: 20px;
  text-align: center;
}

.state-error .primary-action {
  width: auto;
  min-height: 48px;
  padding: 0 22px;
}

.final-cta {
  width: min(100% - 40px, 1160px);
  margin: 0 auto clamp(70px, 9vw, 120px);
  padding: clamp(48px, 7vw, 84px);
  overflow: hidden;
  position: relative;
  border-radius: 30px;
  color: #fff;
  background:
    radial-gradient(circle at 88% 20%, rgba(242, 184, 133, 0.38), transparent 26%),
    linear-gradient(135deg, #201912, #120f0c);
  box-shadow: 0 26px 80px rgba(31, 26, 20, 0.18);
}

.final-cta h2 {
  max-width: 720px;
  margin-top: 18px;
  font-size: clamp(38px, 5.5vw, 72px);
  line-height: 0.98;
  font-weight: 950;
  text-wrap: balance;
}

.final-cta p {
  max-width: 600px;
  margin-top: 20px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 17px;
  line-height: 1.65;
}

.final-cta-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 18px;
  margin-top: 32px;
}

.final-login {
  min-height: 48px;
  display: inline-flex;
  align-items: center;
  padding: 0 8px;
  color: rgba(255, 255, 255, 0.82);
  font-size: 14px;
  font-weight: 850;
}

.final-login:hover {
  color: var(--vy-orange);
}

@media (max-width: 920px) {
  .home-section.imageText,
  .home-section.imageText.imageLeft {
    grid-template-columns: 1fr;
  }

  .home-sections {
    width: min(100% - 32px, 720px);
  }

  .home-hero {
    height: calc(100svh - 72px);
    min-height: 0;
    margin: 0;
    padding: 16px;
  }

  .hero-composition {
    width: 100%;
    height: 100%;
    min-height: 0;
    padding: 24px;
    grid-template-columns: minmax(0, 0.92fr) minmax(320px, 1.08fr);
    grid-template-rows: 1fr;
    gap: 12px;
  }

  .hero-visual {
    grid-row: auto;
    height: min(58vh, 480px);
  }

  .home-hero .hero-copy {
    grid-row: auto;
  }
}

@media (max-width: 680px) {
  .home-nav,
  .home-nav.scrolled {
    min-height: 68px;
    padding: 9px 14px;
  }

  .home-nav :deep(.vy-mark + div span:last-child) {
    display: none;
  }

  .header-right {
    flex-wrap: nowrap;
  }

  .public-menu-toggle {
    display: inline-flex;
  }

  .desktop-nav {
    display: none !important;
  }

  .mobile-nav-drawer {
    position: fixed;
    inset: 0 0 0 auto;
    z-index: 52;
    width: min(86vw, 360px);
    padding: 24px 20px 20px;
    display: flex;
    flex-direction: column;
    align-items: stretch;
    justify-content: flex-start;
    gap: 5px;
    overflow-y: auto;
    border-left: 1px solid rgba(242, 135, 5, 0.16);
    background:
      radial-gradient(circle at 100% 0%, rgba(242, 135, 5, 0.13), transparent 28%),
      #fffdf8;
    box-shadow: -24px 0 70px rgba(31, 26, 20, 0.2);
    transform-origin: right center;
    will-change: transform, opacity;
  }

  .mobile-drawer-links {
    display: flex;
    flex-direction: column;
    gap: 5px;
  }

  .mobile-drawer-links > a {
    display: flex;
    align-items: center;
    width: 100%;
    justify-content: flex-start;
    min-height: 54px;
    padding: 0 16px;
    border: 0;
    border-bottom: 1px solid rgba(31, 26, 20, 0.08);
    border-radius: 12px;
    color: var(--vy-ink-2);
    font-size: 15px;
    font-weight: 800;
    animation: menu-link-in 0.42s cubic-bezier(0.22, 1, 0.36, 1) both;
    transition: color 0.2s ease, background-color 0.2s ease;
  }

  .mobile-drawer-links > a:nth-of-type(1) { animation-delay: 0.08s; }
  .mobile-drawer-links > a:nth-of-type(2) { animation-delay: 0.12s; }
  .mobile-drawer-links > a:nth-of-type(3) { animation-delay: 0.16s; }
  .mobile-drawer-links > a:nth-of-type(4) { animation-delay: 0.2s; }

  .mobile-drawer-links > a:hover,
  .mobile-drawer-links > a:focus-visible {
    color: var(--vy-orange-deep);
    background: rgba(242, 135, 5, 0.08);
  }

  .mobile-menu-backdrop {
    position: fixed;
    inset: 0;
    z-index: 51;
    display: block;
    border: 0;
    background: rgba(18, 15, 12, 0.52);
    backdrop-filter: blur(4px);
    -webkit-backdrop-filter: blur(4px);
  }

  .menu-fade-enter-active,
  .menu-fade-leave-active {
    transition: opacity 0.34s ease, backdrop-filter 0.34s ease;
  }

  .menu-drawer-enter-active {
    transition: transform 0.46s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.28s ease;
  }

  .menu-drawer-leave-active {
    transition: transform 0.34s cubic-bezier(0.4, 0, 1, 1), opacity 0.26s ease;
  }

  .menu-fade-enter-from,
  .menu-fade-leave-to {
    opacity: 0;
    backdrop-filter: blur(0);
    -webkit-backdrop-filter: blur(0);
  }

  .menu-drawer-enter-from,
  .menu-drawer-leave-to {
    opacity: 0;
    transform: translate3d(104%, 0, 0) scale(0.985);
  }

  .mobile-nav-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 18px;
    padding: 4px 2px 20px;
    border-bottom: 1px solid var(--vy-line);
  }

  .mobile-nav-head div {
    min-width: 0;
  }

  .mobile-nav-head small,
  .mobile-nav-head strong {
    display: block;
  }

  .mobile-nav-head small {
    color: var(--vy-orange-deep);
    font-size: 9px;
    font-weight: 900;
    letter-spacing: 0.18em;
    text-transform: uppercase;
  }

  .mobile-nav-head strong {
    margin-top: 5px;
    color: var(--vy-ink);
    font-size: 17px;
    line-height: 1.2;
  }

  .mobile-nav-head button {
    width: 42px;
    height: 42px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 auto;
    border: 1px solid var(--vy-line);
    border-radius: 12px;
    background: #fff;
    color: var(--vy-ink);
  }

  .mobile-nav-foot {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: auto;
    padding: 22px 4px 4px;
    color: var(--vy-ink-3);
    font-size: 10px;
    font-weight: 850;
    letter-spacing: 0.13em;
    text-transform: uppercase;
  }

  .mobile-nav-foot i {
    height: 1px;
    flex: 1;
    background: linear-gradient(90deg, var(--vy-orange), transparent);
  }

  .login-link {
    min-height: 40px;
    padding: 0 12px;
    font-size: 12px;
  }

  .home-hero {
    height: calc(100svh - 68px);
    min-height: 0;
    padding: 10px;
    align-items: stretch;
  }

  .hero-composition {
    height: 100%;
    padding: 12px 18px 18px;
    grid-template-columns: 1fr;
    grid-template-rows: minmax(0, 44%) minmax(0, 56%);
    gap: 0;
    border-radius: 24px;
  }

  .hero-visual {
    grid-row: 1;
    width: 100%;
    height: 100%;
  }

  .visual-glow {
    top: 68%;
    width: 520px;
    height: 520px;
    opacity: 0.72;
  }

  .hero-visual::after {
    display: none;
  }

  .product-showcase {
    top: 50%;
    width: min(92vw, 400px);
    height: min(35svh, 280px);
  }

  .wheel-product img {
    max-width: 82%;
  }

  .orbit-large {
    width: 360px;
    height: 360px;
  }

  .orbit-small {
    width: 270px;
    height: 270px;
  }

  .home-hero .hero-copy h1 {
    margin-top: 12px;
    font-size: clamp(34px, 10vw, 46px);
    line-height: 0.96;
  }

  .home-hero .hero-copy {
    grid-row: 2;
    width: 100%;
    z-index: 3;
    margin-top: -18px;
    padding: 22px 4px 0;
    background: linear-gradient(180deg, transparent 0%, rgba(24, 20, 15, 0.18) 22%, transparent 72%);
  }

  .home-hero .hero-copy p {
    margin-top: 12px;
    font-size: 14px;
    line-height: 1.45;
  }

  .home-hero .hero-kicker {
    min-height: 28px;
    padding: 0 10px;
    font-size: 10px;
  }

  .home-hero .hero-actions {
    margin-top: 18px;
  }

  .home-hero .primary-action {
    min-height: 48px;
    padding: 0 18px;
    font-size: 13px;
  }

  .home-hero .secondary-action {
    display: none;
  }

  .hero-watermark {
    left: -18%;
    bottom: -5%;
    width: 390px;
  }

  .home-sections {
    width: min(100% - 28px, 520px);
    padding: 56px 0;
  }

  .home-section {
    padding: 56px 0;
  }

  .home-section.text.centered,
  .home-section.preguntas,
  .home-section.contact {
    margin: 24px 0;
    padding: 32px 22px;
    border-radius: 22px;
  }

  .benefits-grid {
    grid-template-columns: 1fr;
  }

  .benefit-item {
    min-height: 0;
    padding: 24px;
  }

  .final-cta {
    width: min(100% - 28px, 520px);
    margin-bottom: 56px;
    padding: 38px 24px;
    border-radius: 24px;
  }

  .final-cta p {
    font-size: 15px;
  }

  .final-cta-actions,
  .final-cta .primary-action,
  .final-login {
    width: 100%;
  }

  .final-login {
    justify-content: center;
  }
}

@media (prefers-reduced-motion: reduce) {
  .scroll-reveal,
  .scroll-reveal * {
    opacity: 1 !important;
    transform: none !important;
    transition: none !important;
  }

  .home-hero.intro-ready .hero-composition,
  .home-hero.intro-ready .hero-visual,
  .home-hero.intro-ready .hero-copy > * {
    animation: none !important;
  }

  .hero-visual *,
  .hero-visual *::before,
  .hero-visual *::after {
    animation: none !important;
  }

  .mobile-nav-drawer,
  .mobile-menu-backdrop,
  .mobile-drawer-links > a {
    animation: none !important;
    transition: none !important;
  }
}

@keyframes hero-stage-arrive {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.985);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes hero-visual-arrive {
  from {
    opacity: 0;
    transform: translateX(22px) scale(0.94);
  }
  to {
    opacity: 1;
    transform: translateX(0) scale(1);
  }
}

@keyframes hero-copy-arrive {
  from {
    opacity: 0;
    transform: translateY(22px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes menu-link-in {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
</style>
