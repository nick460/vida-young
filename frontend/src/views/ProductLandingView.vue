<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { MessageCircle } from "lucide-vue-next";
import { VyLogo, VyProductImage } from "../components/ui.js";
import { addCartItem } from "../services/cartService.js";
import { loadProductCatalog } from "../services/productCatalogService.js";
import { loadProductLandingConfig, resolveReferralFromRoute } from "../services/productLandingService.js";

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const product = ref(null);
const products = ref([]);
const landing = ref(null);
const error = ref("");
const carouselTimer = ref(null);
const referral = ref({
  ref: "",
  name: "Asesor Vidayoung",
  phone: "",
  email: "",
  user: "",
  photo: ""
});

const gallery = computed(() => landing.value?.gallery?.length ? landing.value.gallery : [product.value?.img].filter(Boolean));
const sections = computed(() => landing.value?.sections?.length ? landing.value.sections : []);
const heroSection = computed(() => sections.value.find((section) => section.type === "hero"));
const contentSections = computed(() => sections.value.filter((section) => section.type !== "hero"));
const contactSection = computed(() => sections.value.find((section) => section.type === "contact"));
const advisorName = computed(() => formatAdvisorName(referral.value.name || referral.value.user || contactSection.value?.title || "Asesor Vidayoung"));
const advisorPhoto = computed(() => referral.value.photo || contactSection.value?.imageUrl || gallery.value[0] || "");
const contactFloatingHref = computed(() =>
  contactSection.value ? whatsappHref(contactSection.value.whatsappMessage) : ""
);

async function loadProduct() {
  loading.value = true;
  error.value = "";

  try {
    referral.value = await resolveReferralFromRoute(route);
    products.value = await loadProductCatalog({ onlyAvailable: false });
    product.value = products.value.find((item) => String(item.id) === String(route.params.productId));

    if (!product.value) {
      error.value = "Producto no encontrado.";
    } else {
      landing.value = await loadProductLandingConfig(product.value, { publicEndpoint: true });
    }
  } catch {
    error.value = "No se pudo cargar la landing del producto.";
  } finally {
    loading.value = false;
    startCarouselAutoplay();
  }
}

function startCarouselAutoplay() {
  window.clearInterval(carouselTimer.value);

  nextTick(() => {
    const rows = [...document.querySelectorAll(".product-landing .landing-section.carousel .carousel-row")];
    if (!rows.length) {
      return;
    }

    carouselTimer.value = window.setInterval(() => {
      rows.forEach((row) => {
        if (row.scrollWidth <= row.clientWidth) {
          return;
        }

        scrollCarousel(row, 1, { wrap: true });
      });
    }, 2000);
  });
}

function carouselStep(row) {
  const firstSlide = row?.children?.[0];
  const styles = window.getComputedStyle(row);
  const gap = parseFloat(styles.columnGap || styles.gap || "0") || 0;
  return firstSlide ? firstSlide.getBoundingClientRect().width + gap : row.clientWidth;
}

function scrollCarousel(row, direction, { wrap = false } = {}) {
  if (!row || row.scrollWidth <= row.clientWidth) {
    return;
  }

  const step = carouselStep(row);
  const maxLeft = row.scrollWidth - row.clientWidth;
  let nextLeft = row.scrollLeft + step * direction;

  if (wrap && direction > 0 && row.scrollLeft >= maxLeft - 8) {
    nextLeft = 0;
  } else if (wrap && direction < 0 && row.scrollLeft <= 8) {
    nextLeft = maxLeft;
  } else {
    nextLeft = Math.max(0, Math.min(nextLeft, maxLeft));
  }

  row.scrollTo({ left: nextLeft, behavior: "smooth" });
}

function scrollCarouselFromButton(event, direction) {
  const row = event.currentTarget.closest(".carousel-shell")?.querySelector(".carousel-row");
  scrollCarousel(row, direction, { wrap: true });
}

function addToCart() {
  if (!product.value) {
    return;
  }

  addCartItem(product.value);
  router.push({ name: "cart" });
}

function boliviaWhatsappNumber() {
  if (!referral.value.phone || !product.value) {
    return "";
  }

  const digits = String(referral.value.phone).replace(/\D/g, "");
  if (!digits) {
    return "";
  }

  return digits.startsWith("591") ? digits : `591${digits}`;
}

function whatsappHref(message) {
  if (!product.value) {
    return "";
  }

  const phone = boliviaWhatsappNumber();
  const params = new URLSearchParams({
    text: message || `Hola, quiero informacion sobre ${product.value.name}.`,
    app_absent: "0"
  });

  if (phone) {
    params.set("phone", phone);
    params.set("type", "phone_number");
  }

  return `https://api.whatsapp.com/send/?${params.toString()}`;
}

function formatAdvisorName(value) {
  const clean = String(value || "").replace(/[-_]+/g, " ").replace(/\s+/g, " ").trim();
  if (!clean) {
    return "Asesor Vidayoung";
  }

  return clean
    .split(" ")
    .map((part) => part ? part.charAt(0).toUpperCase() + part.slice(1).toLowerCase() : "")
    .join(" ");
}

function youtubeEmbedUrl(url) {
  const value = String(url || "").trim();
  if (!value) return "";

  const match = value.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([A-Za-z0-9_-]{6,})/);
  const videoId = match?.[1] || "";
  return videoId ? `https://www.youtube.com/embed/${videoId}` : "";
}

onMounted(loadProduct);
onBeforeUnmount(() => window.clearInterval(carouselTimer.value));
</script>

<template>
  <div class="vy product-landing">
    <header class="landing-nav">
      <VyLogo :size="24" />
    </header>

    <main v-if="loading" class="state-box">Cargando producto...</main>
    <main v-else-if="error" class="state-box">{{ error }}</main>

    <main v-else-if="product && landing">
      <section v-if="heroSection" class="hero-section" :class="heroSection.layout">
        <div class="hero-copy">
          <span class="vy-chip vy-chip-orange">{{ product.cat }}</span>
          <h1>{{ heroSection.title }}</h1>
          <p>{{ heroSection.text }}</p>
          <div class="hero-actions">
            <a
              v-if="whatsappHref(heroSection.whatsappMessage)"
              class="vy-btn vy-btn-primary"
              :href="whatsappHref(heroSection.whatsappMessage)"
              target="_blank"
              rel="noreferrer"
            >
              <MessageCircle :size="17" />
              Consultar por WhatsApp
            </a>
            <button class="vy-btn vy-btn-dark" type="button" @click="addToCart">
              {{ heroSection.buttonText || "Comprar ahora" }}
            </button>
          </div>
        </div>
        <div class="hero-image">
          <VyProductImage :grad="heroSection.imageUrl || gallery[0]" :h="460" big />
        </div>
      </section>

      <section class="dynamic-sections">
        <article v-for="(section, index) in contentSections" :key="index" class="landing-section" :class="[section.type, section.layout]">
          <template v-if="section.type === 'benefits'">
            <header>
              <span class="vy-eyebrow">Beneficios</span>
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
            </header>
            <div class="benefits-grid">
              <span v-for="benefit in section.images" :key="benefit">{{ benefit }}</span>
            </div>
          </template>

          <template v-else-if="section.type === 'text'">
            <span class="vy-eyebrow">Seccion</span>
            <h2>{{ section.title }}</h2>
            <p>{{ section.text }}</p>
          </template>

          <template v-if="section.type === 'imageText'">
            <div>
              <span class="vy-eyebrow">Seccion</span>
              <h2>{{ section.title }}</h2>
              <p>{{ section.text }}</p>
            </div>
            <VyProductImage :grad="section.imageUrl || gallery[0]" :h="320" />
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

          <template v-else-if="section.type === 'carousel'">
            <header>
              <span class="vy-eyebrow">Galeria</span>
              <h2>{{ section.title }}</h2>
            </header>
            <div class="carousel-shell">
              <button class="carousel-nav prev" type="button" aria-label="Imagen anterior" @click="scrollCarouselFromButton($event, -1)"></button>
              <div class="carousel-row">
                <VyProductImage v-for="image in section.images" :key="image" :grad="image" :h="240" />
              </div>
              <button class="carousel-nav next" type="button" aria-label="Imagen siguiente" @click="scrollCarouselFromButton($event, 1)"></button>
            </div>
          </template>

          <template v-else-if="section.type === 'gallery'">
            <header>
              <span class="vy-eyebrow">Galeria</span>
              <h2>{{ section.title }}</h2>
            </header>
            <div class="carousel-row">
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

          <template v-else-if="section.type === 'contact'">
            <div class="contact-card">
              <VyProductImage :grad="advisorPhoto" :h="300" />
              <strong>{{ advisorName }}</strong>
              <span v-if="referral.phone">+{{ boliviaWhatsappNumber() }}</span>
              <p>{{ section.text }}</p>
              <a v-if="whatsappHref(section.whatsappMessage)" :href="whatsappHref(section.whatsappMessage)" target="_blank" rel="noreferrer">
                <MessageCircle :size="18" />
                {{ section.buttonText || "Escribeme ahora" }}
              </a>
              <small v-if="section.images?.[0]">{{ section.images[0] }}</small>
            </div>
          </template>
        </article>
      </section>

      <a
        v-if="contactFloatingHref"
        class="floating-whatsapp"
        :href="contactFloatingHref"
        target="_blank"
        rel="noreferrer"
        aria-label="Contacto por WhatsApp"
      >
        <MessageCircle :size="20" />
        Contacto
      </a>
    </main>
  </div>
</template>

<style scoped>
.product-landing { min-height: 100vh; background: linear-gradient(180deg, #fffaf0 0%, var(--vy-bg) 34%, #fff 100%); color: var(--vy-ink); }
.landing-nav { min-height: 72px; padding: 10px clamp(16px, 4vw, 52px); display: flex; align-items: center; justify-content: space-between; gap: 14px; background: rgba(255, 255, 255, 0.9); border-bottom: 1px solid var(--vy-line-2); position: sticky; top: 0; z-index: 20; backdrop-filter: blur(14px); }
.landing-nav button { min-height: 40px; padding: 0 16px; border-radius: 999px; background: var(--vy-surface-2); color: var(--vy-ink-2); font-size: 13px; font-weight: 800; white-space: nowrap; }
.state-box { min-height: calc(100vh - 72px); display: flex; align-items: center; justify-content: center; padding: 24px; color: var(--vy-ink-2); font-weight: 800; text-align: center; }
.hero-section { width: min(100% - 32px, 1280px); margin: 0 auto; display: grid; grid-template-columns: minmax(0, 0.92fr) minmax(320px, 1.08fr); gap: clamp(22px, 5vw, 58px); align-items: center; padding: clamp(24px, 6vw, 68px) 0 clamp(18px, 5vw, 48px); }
.hero-section.imageLeft { grid-template-columns: minmax(340px, 1.1fr) minmax(0, 0.9fr); }
.hero-section.imageLeft .hero-copy { order: 2; }
.hero-section.stacked { grid-template-columns: minmax(0, 1fr); }
.hero-copy { min-width: 0; }
.hero-copy h1 { max-width: 760px; margin-top: 14px; font-size: clamp(34px, 5vw, 66px); line-height: 1; font-weight: 900; overflow-wrap: anywhere; }
.hero-copy p { max-width: 660px; margin-top: 18px; color: var(--vy-ink-2); font-size: clamp(16px, 1.8vw, 20px); line-height: 1.55; text-align: justify; }
.hero-actions { display: flex; gap: 12px; flex-wrap: wrap; margin-top: 28px; }
.hero-actions .vy-btn { min-height: 52px; padding: 0 24px; border-radius: 999px; font-size: 14px; font-weight: 900; letter-spacing: 0.01em; box-shadow: 0 14px 28px rgba(31, 26, 20, 0.12); transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease; display: inline-flex; align-items: center; justify-content: center; gap: 9px; }
.hero-actions .vy-btn:hover { transform: translateY(-1px); box-shadow: 0 18px 34px rgba(31, 26, 20, 0.16); }
.hero-actions .vy-btn:active { transform: translateY(0); box-shadow: 0 8px 18px rgba(31, 26, 20, 0.12); }
.hero-actions .vy-btn-primary { background: linear-gradient(135deg, #25d366 0%, #1eb65a 100%); color: #fff; }
.hero-actions .vy-btn-dark { background: linear-gradient(135deg, var(--vy-ink) 0%, #3a332b 100%); color: #fff; }
.hero-image { min-width: 0; }
.hero-image > :deep(div) { height: clamp(280px, 42vw, 460px) !important; border-radius: 18px !important; }
.price-line { display: flex; align-items: baseline; gap: 12px; flex-wrap: wrap; margin-top: 20px; }
.price-line strong { font-family: var(--font-display); font-size: 30px; font-weight: 900; }
.price-line small { color: var(--vy-orange-deep); font-size: 12px; font-weight: 900; }
.dynamic-sections { width: min(100% - 32px, 1280px); display: grid; gap: clamp(14px, 2.5vw, 22px); margin: 0 auto; padding: 0 0 clamp(42px, 7vw, 76px); }
.landing-section { min-width: 0; padding: clamp(20px, 4vw, 36px); border: 1px solid rgba(31, 26, 20, 0.1); border-radius: 18px; background: rgba(255, 255, 255, 0.86); box-shadow: 0 14px 42px rgba(31, 26, 20, 0.08); overflow: hidden; }
.landing-section.imageText, .landing-section.videoText { display: grid; grid-template-columns: minmax(0, 1fr) minmax(260px, 0.86fr); gap: clamp(18px, 4vw, 36px); align-items: center; }
.landing-section.imageText.imageLeft, .landing-section.videoText.imageLeft { grid-template-columns: minmax(260px, 0.8fr) minmax(0, 1fr); }
.landing-section.imageText.imageLeft > div:first-child, .landing-section.videoText.imageLeft > div:first-child { order: 2; }
.landing-section.imageText.stacked, .landing-section.videoText.stacked { grid-template-columns: 1fr; }
.landing-section.text.centered { max-width: 820px; margin: 0 auto; text-align: center; }
.landing-section h2 { margin-top: 8px; font-size: clamp(24px, 3vw, 36px); font-weight: 900; line-height: 1.12; overflow-wrap: anywhere; }
.landing-section p { margin-top: 12px; color: var(--vy-ink-2); font-size: clamp(15px, 1.4vw, 17px); line-height: 1.68; text-align: justify; }
.landing-section.imageText > :deep(div:last-child) { height: clamp(220px, 32vw, 340px) !important; border-radius: 16px !important; }
.benefits-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: clamp(10px, 2vw, 14px); margin-top: 18px; }
.landing-section.grid2 .benefits-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.landing-section.list .benefits-grid { grid-template-columns: 1fr; }
.benefits-grid span { min-height: 86px; padding: clamp(14px, 2vw, 18px); border: 1px solid rgba(31, 26, 20, 0.1); border-radius: 14px; background: var(--vy-surface-2); color: var(--vy-ink-2); font-size: clamp(14px, 1.3vw, 16px); line-height: 1.52; font-weight: 750; overflow-wrap: anywhere; text-align: justify; }
.carousel-shell { position: relative; margin-top: 18px; padding: 0; }
.video-frame { aspect-ratio: 16 / 9; border-radius: 16px; overflow: hidden; background: #1d1d1f; box-shadow: 0 18px 42px rgba(31, 26, 20, 0.14); }
.video-frame iframe { width: 100%; height: 100%; border: 0; display: block; }
.video-frame span { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; padding: 18px; color: rgba(255, 255, 255, 0.72); font-size: 13px; font-weight: 800; text-align: center; }
.carousel-row { display: grid; grid-auto-flow: column; grid-auto-columns: minmax(240px, 32%); gap: 14px; margin-top: 18px; overflow-x: auto; padding: 2px 2px 8px; scroll-snap-type: x proximity; scrollbar-width: thin; }
.carousel-shell .carousel-row { margin-top: 0; }
.carousel-nav { position: absolute; top: 50%; z-index: 3; width: 40px; height: 58px; transform: translateY(-50%); border-radius: 14px; background: rgba(31, 26, 20, 0.62); box-shadow: 0 12px 26px rgba(31, 26, 20, 0.2); display: inline-flex; align-items: center; justify-content: center; transition: transform 0.18s ease, background 0.18s ease; backdrop-filter: blur(6px); }
.carousel-nav:hover { background: rgba(31, 26, 20, 0.9); }
.carousel-nav:active { transform: translateY(-50%) scale(0.96); }
.carousel-nav.prev { left: 10px; }
.carousel-nav.next { right: 10px; }
.carousel-nav::before { content: ""; width: 0; height: 0; border-top: 11px solid transparent; border-bottom: 11px solid transparent; }
.carousel-nav.prev::before { border-right: 15px solid #fff; margin-left: -2px; }
.carousel-nav.next::before { border-left: 15px solid #fff; margin-right: -2px; }
.landing-section.gallery.grid2 .carousel-row, .landing-section.carousel.grid2 .carousel-row { grid-auto-flow: row; grid-auto-columns: initial; grid-template-columns: repeat(2, minmax(0, 1fr)); overflow: visible; }
.landing-section.gallery.grid3 .carousel-row, .landing-section.carousel.grid3 .carousel-row { grid-auto-flow: row; grid-auto-columns: initial; grid-template-columns: repeat(3, minmax(0, 1fr)); overflow: visible; }
.landing-section.carousel .carousel-row { grid-auto-flow: column; grid-auto-columns: minmax(240px, 32%); grid-template-columns: none; overflow-x: auto; }
.landing-section.carousel .carousel-row {
  display: flex !important;
  grid-template-columns: none !important;
  grid-auto-columns: initial !important;
  grid-auto-flow: initial !important;
  overflow-x: auto !important;
  scroll-snap-type: x mandatory;
}
.landing-section.carousel .carousel-row > * {
  flex: 0 0 calc((100% - 14px) / 2) !important;
  min-width: calc((100% - 14px) / 2) !important;
  max-width: calc((100% - 14px) / 2) !important;
}
.carousel-row > * { scroll-snap-align: start; }
.carousel-row > :deep(div) { height: clamp(280px, 28vw, 380px) !important; border-radius: 16px !important; }
.carousel-row > :deep(div img) { object-fit: cover !important; background: var(--vy-surface-2); }
.landing-section.preguntas { background: #15171b; color: #fff; border-color: rgba(255, 255, 255, 0.08); box-shadow: 0 18px 46px rgba(0, 0, 0, 0.18); }
.faq-section h2 { margin: 0 0 clamp(20px, 4vw, 30px); color: #fff; font-size: clamp(34px, 6vw, 58px); line-height: 1; font-weight: 900; }
.faq-item { border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 14px; background: rgba(255, 255, 255, 0.03); overflow: hidden; }
.faq-item + .faq-item { margin-top: 14px; }
.faq-item summary { min-height: 70px; padding: 0 clamp(18px, 3vw, 24px); display: flex; align-items: center; justify-content: space-between; gap: 16px; cursor: pointer; color: #fff; font-size: clamp(17px, 2vw, 22px); font-weight: 900; list-style: none; }
.faq-item summary::-webkit-details-marker { display: none; }
.faq-item summary::after { content: "+"; flex-shrink: 0; color: #d7bd6a; font-size: 26px; font-weight: 900; line-height: 1; }
.faq-item[open] summary::after { content: "-"; }
.faq-item p { margin: 0; padding: 0 clamp(18px, 3vw, 24px) 24px; color: rgba(255, 255, 255, 0.78); font-size: clamp(16px, 1.8vw, 21px); line-height: 1.55; text-align: justify; }
.landing-section.contact { padding: 0; border: 0; background: transparent; box-shadow: none; }
.contact-card { width: min(100%, 520px); margin: 0 auto; padding: clamp(24px, 5vw, 40px); border-radius: 18px; background: #1d1d1f; border: 1px solid rgba(255, 255, 255, 0.08); color: #fff; display: flex; flex-direction: column; align-items: center; text-align: center; box-shadow: 0 18px 48px rgba(0, 0, 0, 0.2); }
.landing-section.contact.cardWide .contact-card { max-width: 760px; }
.contact-card > div:first-child { width: min(300px, 100%); height: min(300px, calc(100vw - 96px)) !important; aspect-ratio: 1 / 1; margin-bottom: 18px; border-radius: 50% !important; padding: 0 !important; align-items: center !important; }
.contact-card strong { font-size: clamp(22px, 4vw, 28px); font-weight: 900; overflow-wrap: anywhere; }
.contact-card span { margin-top: 10px; color: rgba(255, 255, 255, 0.78); font-size: 16px; overflow-wrap: anywhere; }
.contact-card p { max-width: 440px; margin-top: 24px; color: rgba(255, 255, 255, 0.88); font-size: clamp(15px, 2vw, 17px); line-height: 1.62; text-align: justify; }
.contact-card a { width: min(400px, 100%); min-height: 62px; margin-top: 30px; padding: 0 24px; border-radius: 999px; display: inline-flex; align-items: center; justify-content: center; gap: 10px; background: linear-gradient(135deg, #25d366 0%, #1eb65a 100%); color: #fff; font-size: clamp(16px, 3vw, 18px); font-weight: 900; box-shadow: 0 16px 34px rgba(37, 211, 102, 0.24); transition: transform 0.18s ease, box-shadow 0.18s ease; }
.contact-card a:hover { transform: translateY(-1px); box-shadow: 0 20px 40px rgba(37, 211, 102, 0.3); }
.contact-card a:active { transform: translateY(0); box-shadow: 0 10px 22px rgba(37, 211, 102, 0.22); }
.contact-card small { max-width: 390px; margin-top: 18px; color: rgba(255, 255, 255, 0.52); font-size: 12px; line-height: 1.45; overflow-wrap: anywhere; text-align: justify; }
.floating-whatsapp { position: fixed; right: max(18px, env(safe-area-inset-right)); bottom: max(18px, env(safe-area-inset-bottom)); z-index: 40; min-height: 54px; padding: 0 18px; border-radius: 999px; display: inline-flex; align-items: center; justify-content: center; gap: 9px; background: linear-gradient(135deg, #25d366 0%, #1eb65a 100%); color: #fff; font-size: 14px; font-weight: 900; box-shadow: 0 18px 42px rgba(37, 211, 102, 0.36); }
.floating-whatsapp:hover { transform: translateY(-1px); box-shadow: 0 22px 48px rgba(37, 211, 102, 0.42); }
.floating-whatsapp:active { transform: translateY(0); }
@media (min-width: 1440px) {
  .hero-section, .dynamic-sections { width: min(100% - 80px, 1360px); }
}
@media (max-width: 920px) {
  .hero-section, .hero-section.imageLeft, .landing-section.imageText, .landing-section.imageText.imageLeft, .landing-section.videoText, .landing-section.videoText.imageLeft { width: min(100% - 28px, 760px); grid-template-columns: 1fr; }
  .hero-copy { text-align: left; }
  .hero-section.imageLeft .hero-copy, .landing-section.imageText.imageLeft > div:first-child, .landing-section.videoText.imageLeft > div:first-child { order: initial; }
  .dynamic-sections { width: min(100% - 28px, 760px); }
  .benefits-grid, .landing-section.grid2 .benefits-grid { grid-template-columns: 1fr; }
  .landing-section.gallery.grid2 .carousel-row, .landing-section.gallery.grid3 .carousel-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .landing-section.carousel .carousel-row, .carousel-row { grid-auto-columns: minmax(0, 100%); }
  .carousel-shell { padding: 0; }
  .landing-section.carousel .carousel-row > * {
    flex-basis: 100% !important;
    min-width: 100% !important;
    max-width: 100% !important;
  }
  .carousel-row > :deep(div) { height: clamp(230px, 52vw, 340px) !important; }
}
@media (max-width: 560px) {
  .landing-nav { min-height: 64px; padding: 8px 14px; }
  .landing-nav button { min-height: 36px; padding: 0 12px; font-size: 12px; }
  .hero-section { width: min(100% - 24px, 520px); padding-top: 22px; }
  .hero-copy h1 { font-size: clamp(30px, 10vw, 42px); }
  .hero-copy p { font-size: 15.5px; }
  .hero-actions .vy-btn { width: 100%; }
  .hero-image > :deep(div) { height: clamp(230px, 76vw, 340px) !important; }
  .dynamic-sections { width: min(100% - 24px, 520px); gap: 14px; }
  .landing-section { padding: 18px; border-radius: 16px; }
  .faq-item summary { min-height: 62px; }
  .landing-section.gallery.grid2 .carousel-row, .landing-section.gallery.grid3 .carousel-row { grid-template-columns: 1fr; }
  .landing-section.carousel .carousel-row, .carousel-row { grid-auto-columns: minmax(0, 100%); grid-template-columns: none; overflow-x: auto; }
  .carousel-shell { padding: 0; }
  .carousel-nav { width: 30px; height: 48px; border-radius: 12px; background: rgba(31, 26, 20, 0.6); }
  .carousel-nav.prev { left: 8px; }
  .carousel-nav.next { right: 8px; }
  .carousel-nav::before { border-top-width: 9px; border-bottom-width: 9px; }
  .carousel-nav.prev::before { border-right-width: 12px; }
  .carousel-nav.next::before { border-left-width: 12px; }
  .landing-section.carousel .carousel-row > * {
    flex-basis: 100% !important;
    min-width: 100% !important;
    max-width: 100% !important;
  }
  .carousel-row > :deep(div) { height: clamp(220px, 68vw, 300px) !important; }
  .landing-section.carousel .carousel-row { margin-left: 0; margin-right: 0; }
  .contact-card { border-radius: 16px; }
  .contact-card > div:first-child { width: min(250px, 100%); height: min(250px, calc(100vw - 88px)) !important; }
  .floating-whatsapp { right: 14px; bottom: 14px; min-height: 50px; padding: 0 15px; font-size: 13px; }
}
</style>
