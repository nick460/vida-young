<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { ExternalLink, MessageCircle } from "lucide-vue-next";
import { VyLogo, VyProductImage } from "../components/ui.js";
import { loadPublicDigitalLanding } from "../services/digitalLandingService.js";
import { readReferralFromRoute } from "../services/productLandingService.js";

const route = useRoute();
const loading = ref(false);
const error = ref("");
const landing = ref(null);
const referral = computed(() => readReferralFromRoute(route));
const sections = computed(() => landing.value?.sections || []);
const heroSection = computed(() => sections.value.find((section) => section.type === "hero"));
const contentSections = computed(() => sections.value.filter((section) => section.type !== "hero"));
const contactSection = computed(() => sections.value.find((section) => section.type === "contact"));
const advisorName = computed(() => referral.value.name || contactSection.value?.title || "Asesor Vidayoung");
const advisorPhoto = computed(() => referral.value.photo || contactSection.value?.imageUrl || landing.value?.imageUrl || "");
const floatingHref = computed(() => contactSection.value ? whatsappHref(contactSection.value.whatsappMessage) : "");

async function loadLanding() {
  loading.value = true;
  error.value = "";
  try {
    landing.value = await loadPublicDigitalLanding(route.params.slug);
  } catch {
    error.value = "No se pudo cargar esta landing.";
  } finally {
    loading.value = false;
  }
}

function boliviaWhatsappNumber() {
  const digits = String(referral.value.phone || "").replace(/\D/g, "");
  if (!digits) return "";
  return digits.startsWith("591") ? digits : `591${digits}`;
}

function whatsappHref(message) {
  const phone = boliviaWhatsappNumber();
  const params = new URLSearchParams({
    text: message || `Hola, quiero informacion sobre ${landing.value?.title || "este tema"}.`,
    app_absent: "0"
  });

  if (phone) {
    params.set("phone", phone);
    params.set("type", "phone_number");
  }

  return `https://api.whatsapp.com/send/?${params.toString()}`;
}

function youtubeEmbedUrl(url) {
  const value = String(url || "").trim();
  if (!value) return "";

  const match = value.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([A-Za-z0-9_-]{6,})/);
  const videoId = match?.[1] || "";
  return videoId ? `https://www.youtube.com/embed/${videoId}` : "";
}

function socialLabel(item) {
  return String(item || "").split("|||")[0] || "Red social";
}

function socialUrl(item) {
  return String(item || "").split("|||")[1] || "#";
}

onMounted(loadLanding);
</script>

<template>
  <div class="vy digital-landing-page">
    <header class="landing-nav">
      <VyLogo :size="24" />
    </header>

    <main v-if="loading" class="state-box">Cargando landing...</main>
    <main v-else-if="error" class="state-box">{{ error }}</main>

    <main v-else-if="landing">
      <section v-if="heroSection" class="hero-section" :class="heroSection.layout">
        <div class="hero-copy">
          <span class="vy-chip vy-chip-orange">{{ landing.category || "Tema" }}</span>
          <h1>{{ heroSection.title || landing.title }}</h1>
          <p>{{ heroSection.text || landing.description }}</p>
          <a v-if="whatsappHref(heroSection.whatsappMessage)" :href="whatsappHref(heroSection.whatsappMessage)" target="_blank" rel="noreferrer">
            <MessageCircle :size="18" />
            {{ heroSection.buttonText || "Quiero saber mas" }}
          </a>
        </div>
        <VyProductImage :grad="heroSection.imageUrl || landing.imageUrl || 'linear-gradient(135deg, #F2E7C4 0%, #F28705 100%)'" :h="460" big />
      </section>

      <section class="dynamic-sections">
        <article v-for="(section, index) in contentSections" :key="index" class="landing-section" :class="[section.type, section.layout]">
          <template v-if="section.type === 'benefits'">
            <header>
              <span class="vy-eyebrow">Puntos clave</span>
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
            </header>
            <div class="benefits-grid">
              <span v-for="benefit in section.images" :key="benefit">{{ benefit }}</span>
            </div>
          </template>

          <template v-else-if="section.type === 'text'">
            <span class="vy-eyebrow">Contenido</span>
            <h2>{{ section.title }}</h2>
            <p>{{ section.text }}</p>
          </template>

          <template v-else-if="section.type === 'imageText'">
            <div>
              <span class="vy-eyebrow">Contenido</span>
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
            </div>
            <VyProductImage :grad="section.imageUrl || landing.imageUrl" :h="320" />
          </template>

          <template v-else-if="section.type === 'videoText'">
            <div>
              <span class="vy-eyebrow">Video</span>
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
            </div>
            <div class="video-frame">
              <iframe
                v-if="youtubeEmbedUrl(section.imageUrl)"
                :src="youtubeEmbedUrl(section.imageUrl)"
                :title="section.title"
                loading="lazy"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                allowfullscreen
              ></iframe>
              <span v-else>Agrega un link valido de YouTube.</span>
            </div>
          </template>

          <template v-else-if="['gallery', 'carousel'].includes(section.type)">
            <header>
              <span class="vy-eyebrow">Galeria</span>
              <h2>{{ section.title }}</h2>
            </header>
            <div class="media-row">
              <VyProductImage v-for="image in section.images" :key="image" :grad="image" :h="240" />
            </div>
          </template>

          <template v-else-if="section.type === 'preguntas'">
            <div class="faq-section">
              <h2>{{ section.title || "Preguntas frecuentes" }}</h2>
              <details v-for="item in section.images" :key="item" class="faq-item">
                <summary>{{ item.split("|||")[0] }}</summary>
                <p>{{ item.split("|||")[1] }}</p>
              </details>
            </div>
          </template>

          <template v-else-if="section.type === 'social'">
            <header>
              <span class="vy-eyebrow">Redes</span>
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
            <div class="contact-card">
              <VyProductImage :grad="advisorPhoto" :h="280" />
              <strong>{{ advisorName }}</strong>
              <span v-if="referral.phone">+{{ boliviaWhatsappNumber() }}</span>
              <p>{{ section.text }}</p>
              <a :href="whatsappHref(section.whatsappMessage)" target="_blank" rel="noreferrer">
                <MessageCircle :size="18" />
                {{ section.buttonText || "Escribeme ahora" }}
              </a>
              <small v-if="section.images?.[0]">{{ section.images[0] }}</small>
            </div>
          </template>
        </article>
      </section>

      <a v-if="floatingHref" class="floating-whatsapp" :href="floatingHref" target="_blank" rel="noreferrer">
        <MessageCircle :size="20" />
        Contacto
      </a>
    </main>
  </div>
</template>

<style scoped>
.digital-landing-page { min-height: 100vh; background: linear-gradient(180deg, #fffaf0 0%, var(--vy-bg) 36%, #fff 100%); color: var(--vy-ink); }
.landing-nav { min-height: 72px; padding: 10px clamp(16px, 4vw, 52px); display: flex; align-items: center; background: rgba(255, 255, 255, 0.9); border-bottom: 1px solid var(--vy-line-2); position: sticky; top: 0; z-index: 20; backdrop-filter: blur(14px); }
.state-box { min-height: calc(100vh - 72px); display: flex; align-items: center; justify-content: center; padding: 24px; color: var(--vy-ink-2); font-weight: 800; text-align: center; }
.hero-section { width: min(100% - 32px, 1280px); margin: 0 auto; display: grid; grid-template-columns: minmax(0, 0.9fr) minmax(320px, 1fr); gap: clamp(22px, 5vw, 58px); align-items: center; padding: clamp(24px, 6vw, 68px) 0 clamp(18px, 5vw, 48px); }
.hero-section.imageLeft { grid-template-columns: minmax(320px, 1fr) minmax(0, 0.9fr); }
.hero-section.imageLeft .hero-copy { order: 2; }
.hero-section.stacked { grid-template-columns: 1fr; }
.hero-copy h1 { max-width: 760px; margin-top: 14px; font-size: clamp(36px, 6vw, 72px); line-height: 1; font-weight: 900; overflow-wrap: anywhere; }
.hero-copy p { max-width: 680px; margin-top: 18px; color: var(--vy-ink-2); font-size: clamp(16px, 1.8vw, 20px); line-height: 1.58; text-align: justify; }
.hero-copy a, .contact-card a, .floating-whatsapp { display: inline-flex; align-items: center; justify-content: center; gap: 10px; background: linear-gradient(135deg, #25d366 0%, #1eb65a 100%); color: #fff; font-weight: 900; }
.hero-copy a { min-height: 52px; margin-top: 28px; padding: 0 24px; border-radius: 999px; box-shadow: 0 14px 28px rgba(37, 211, 102, 0.22); }
.dynamic-sections { width: min(100% - 32px, 1280px); display: grid; gap: clamp(14px, 2.5vw, 22px); margin: 0 auto; padding: 0 0 clamp(42px, 7vw, 76px); }
.landing-section { min-width: 0; padding: clamp(20px, 4vw, 36px); border: 1px solid rgba(31, 26, 20, 0.1); border-radius: 18px; background: rgba(255, 255, 255, 0.88); box-shadow: 0 14px 42px rgba(31, 26, 20, 0.08); overflow: hidden; }
.landing-section.imageText, .landing-section.videoText { display: grid; grid-template-columns: minmax(0, 1fr) minmax(260px, 0.85fr); gap: clamp(18px, 4vw, 36px); align-items: center; }
.landing-section.imageText.imageLeft, .landing-section.videoText.imageLeft { grid-template-columns: minmax(260px, 0.85fr) minmax(0, 1fr); }
.landing-section.imageText.imageLeft > div:first-child, .landing-section.videoText.imageLeft > div:first-child { order: 2; }
.landing-section.text.centered { max-width: 820px; margin: 0 auto; text-align: center; }
.landing-section h2 { margin-top: 8px; font-size: clamp(24px, 3vw, 38px); font-weight: 900; line-height: 1.12; overflow-wrap: anywhere; }
.landing-section p { margin-top: 12px; color: var(--vy-ink-2); font-size: clamp(15px, 1.4vw, 17px); line-height: 1.68; text-align: justify; }
.benefits-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; margin-top: 18px; }
.landing-section.grid2 .benefits-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.landing-section.list .benefits-grid { grid-template-columns: 1fr; }
.benefits-grid span { min-height: 86px; padding: 16px; border: 1px solid rgba(31, 26, 20, 0.1); border-radius: 14px; background: var(--vy-surface-2); color: var(--vy-ink-2); font-weight: 750; line-height: 1.52; text-align: justify; }
.media-row { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; margin-top: 18px; }
.social-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; margin-top: 18px; }
.social-grid a { min-height: 72px; padding: 0 18px; border-radius: 14px; background: var(--vy-ink); color: #fff; display: inline-flex; align-items: center; justify-content: space-between; gap: 12px; font-weight: 900; }
.video-frame { aspect-ratio: 16 / 9; border-radius: 16px; overflow: hidden; background: #1d1d1f; box-shadow: 0 18px 42px rgba(31, 26, 20, 0.14); }
.video-frame iframe { width: 100%; height: 100%; border: 0; display: block; }
.video-frame span { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; padding: 18px; color: rgba(255, 255, 255, 0.72); font-size: 13px; font-weight: 800; text-align: center; }
.landing-section.carousel .media-row { grid-auto-flow: column; grid-auto-columns: minmax(240px, 32%); grid-template-columns: none; overflow-x: auto; padding-bottom: 8px; }
.landing-section.preguntas { background: #15171b; color: #fff; }
.faq-section h2 { margin: 0 0 22px; color: #fff; font-size: clamp(34px, 6vw, 58px); line-height: 1; font-weight: 900; }
.faq-item { border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 14px; background: rgba(255, 255, 255, 0.03); overflow: hidden; }
.faq-item + .faq-item { margin-top: 12px; }
.faq-item summary { min-height: 66px; padding: 0 20px; display: flex; align-items: center; justify-content: space-between; cursor: pointer; color: #fff; font-size: 18px; font-weight: 900; list-style: none; }
.faq-item summary::-webkit-details-marker { display: none; }
.faq-item summary::after { content: "+"; color: #d7bd6a; font-size: 24px; }
.faq-item[open] summary::after { content: "-"; }
.faq-item p { margin: 0; padding: 0 20px 20px; color: rgba(255, 255, 255, 0.78); }
.landing-section.contact { padding: 0; border: 0; background: transparent; box-shadow: none; }
.contact-card { width: min(100%, 520px); margin: 0 auto; padding: clamp(24px, 5vw, 40px); border-radius: 18px; background: #1d1d1f; color: #fff; display: flex; flex-direction: column; align-items: center; text-align: center; box-shadow: 0 18px 48px rgba(0, 0, 0, 0.2); }
.contact-card > div:first-child { width: min(280px, 100%); height: min(280px, calc(100vw - 96px)) !important; aspect-ratio: 1 / 1; margin-bottom: 18px; border-radius: 50% !important; padding: 0 !important; }
.contact-card strong { font-size: clamp(22px, 4vw, 28px); font-weight: 900; }
.contact-card span { margin-top: 10px; color: rgba(255, 255, 255, 0.78); }
.contact-card p { max-width: 440px; color: rgba(255, 255, 255, 0.88); }
.contact-card a { width: min(400px, 100%); min-height: 58px; margin-top: 24px; padding: 0 22px; border-radius: 999px; }
.contact-card small { max-width: 390px; margin-top: 16px; color: rgba(255, 255, 255, 0.52); font-size: 12px; line-height: 1.45; }
.floating-whatsapp { position: fixed; right: 18px; bottom: 18px; z-index: 40; min-height: 54px; padding: 0 18px; border-radius: 999px; box-shadow: 0 18px 42px rgba(37, 211, 102, 0.36); }
@media (max-width: 920px) {
  .hero-section, .hero-section.imageLeft, .landing-section.imageText, .landing-section.imageText.imageLeft, .landing-section.videoText, .landing-section.videoText.imageLeft { width: min(100% - 28px, 760px); grid-template-columns: 1fr; }
  .hero-section.imageLeft .hero-copy, .landing-section.imageText.imageLeft > div:first-child, .landing-section.videoText.imageLeft > div:first-child { order: initial; }
  .dynamic-sections { width: min(100% - 28px, 760px); }
  .benefits-grid, .landing-section.grid2 .benefits-grid, .media-row, .social-grid { grid-template-columns: 1fr; }
}
@media (max-width: 560px) {
  .landing-nav { min-height: 64px; padding: 8px 14px; }
  .hero-section { width: min(100% - 24px, 520px); padding-top: 22px; }
  .hero-copy h1 { font-size: clamp(30px, 10vw, 42px); }
  .hero-copy a { width: 100%; }
  .dynamic-sections { width: min(100% - 24px, 520px); gap: 14px; }
  .landing-section { padding: 18px; border-radius: 16px; }
  .floating-whatsapp { right: 14px; bottom: 14px; min-height: 50px; font-size: 13px; }
}
</style>
