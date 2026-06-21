<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { BookOpen, Copy, ExternalLink, Link2, RefreshCw, Settings } from "lucide-vue-next";
import { VyProductImage } from "../components/ui.js";
import { useAuthStore } from "../stores/authStore.js";
import { ROLE_ADMIN, hasAnyRole } from "../navigation/menuConfig.js";
import { loadProductCatalog } from "../services/productCatalogService.js";
import {
  buildReferralData,
  encodeReferralQuery,
  getDefaultShareMessage,
  loadProductLandingConfig
} from "../services/productLandingService.js";
import {
  getDefaultDigitalShareMessage,
  loadDigitalLandings
} from "../services/digitalLandingService.js";

const router = useRouter();
const authStore = useAuthStore();
const productos = ref([]);
const digitalLandings = ref([]);
const landingConfigs = ref({});
const loading = ref(false);
const error = ref("");

const canConfigure = computed(() => hasAnyRole(authStore.usuario?.roles, [ROLE_ADMIN]));
const referral = computed(() => buildReferralData(authStore.usuario));

async function loadProducts() {
  loading.value = true;
  error.value = "";

  try {
    const [productsData, topicsData] = await Promise.all([
      loadProductCatalog({ onlyAvailable: false }),
      loadDigitalLandings()
    ]);
    productos.value = productsData;
    digitalLandings.value = topicsData;
    const configs = await Promise.all(productos.value.map(async (product) => [
      product.id,
      await loadProductLandingConfig(product)
    ]));
    landingConfigs.value = Object.fromEntries(configs);
  } catch {
    productos.value = [];
    digitalLandings.value = [];
    landingConfigs.value = {};
    error.value = "No se pudieron cargar las herramientas digitales.";
  } finally {
    loading.value = false;
  }
}

function landingUrl(product) {
  const route = router.resolve({
    name: "producto-landing",
    params: { productId: product.id },
    query: Object.fromEntries(new URLSearchParams(encodeReferralQuery(referral.value)))
  });

  return `${window.location.origin}${window.location.pathname}${route.href}`;
}

function shareMessage(product) {
  const template = landingConfigs.value[product.id]?.shareMessage || getDefaultShareMessage(product);
  return `${template.replaceAll("{producto}", product.name).trim()}\n\n${landingUrl(product)}`;
}

function topicUrl(topic) {
  const route = router.resolve({
    name: "herramienta-landing",
    params: { slug: topic.slug },
    query: Object.fromEntries(new URLSearchParams(encodeReferralQuery(referral.value)))
  });

  return `${window.location.origin}${window.location.pathname}${route.href}`;
}

function topicMessage(topic) {
  const template = topic.shareMessage || getDefaultDigitalShareMessage(topic);
  return `${template.replaceAll("{tema}", topic.title).trim()}\n\n${topicUrl(topic)}`;
}

async function copyLink(product) {
  const url = landingUrl(product);

  try {
    await navigator.clipboard.writeText(url);
    await Swal.fire({
      title: "Link copiado",
      text: `Landing personalizada para ${product.name}.`,
      icon: "success",
      confirmButtonText: "Listo",
      confirmButtonColor: "#F28705"
    });
  } catch {
    await Swal.fire({
      title: "Copia manual",
      text: url,
      icon: "info",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
  }
}

async function copyMessage(product) {
  const message = shareMessage(product);

  try {
    await navigator.clipboard.writeText(message);
    await Swal.fire({
      title: "Mensaje copiado",
      text: `Mensaje personalizado para ${product.name}.`,
      icon: "success",
      confirmButtonText: "Listo",
      confirmButtonColor: "#F28705"
    });
  } catch {
    await Swal.fire({
      title: "Copia manual",
      text: message,
      icon: "info",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
  }
}

function openLanding(product) {
  window.open(landingUrl(product), "_blank", "noopener,noreferrer");
}

async function copyTopicLink(topic) {
  const url = topicUrl(topic);

  try {
    await navigator.clipboard.writeText(url);
    await Swal.fire({
      title: "Link copiado",
      text: `Landing personalizada para ${topic.title}.`,
      icon: "success",
      confirmButtonText: "Listo",
      confirmButtonColor: "#F28705"
    });
  } catch {
    await Swal.fire({
      title: "Copia manual",
      text: url,
      icon: "info",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
  }
}

async function copyTopicMessage(topic) {
  const message = topicMessage(topic);

  try {
    await navigator.clipboard.writeText(message);
    await Swal.fire({
      title: "Mensaje copiado",
      text: `Mensaje personalizado para ${topic.title}.`,
      icon: "success",
      confirmButtonText: "Listo",
      confirmButtonColor: "#F28705"
    });
  } catch {
    await Swal.fire({
      title: "Copia manual",
      text: message,
      icon: "info",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
  }
}

function openTopic(topic) {
  window.open(topicUrl(topic), "_blank", "noopener,noreferrer");
}

onMounted(async () => {
  if (!authStore.usuario?.persona) {
    try {
      await authStore.cargarPerfil();
    } catch {
      // Si el perfil no carga, se mantiene la referencia basica del usuario autenticado.
    }
  }

  await loadProducts();
});
</script>

<template>
  <div class="vy digital-tools-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Herramientas digitales</div>
          <h1>Links personalizados por producto</h1>
          <p>Copia landings con tu referencia para compartir productos con clientes y referidos.</p>
        </div>
        <div class="header-actions">
          <button class="vy-btn vy-btn-ghost" type="button" @click="loadProducts">
            <RefreshCw :size="15" />
            Actualizar
          </button>
          <button v-if="canConfigure" class="vy-btn vy-btn-dark" type="button" @click="router.push({ name: 'landing-productos-config' })">
            <Settings :size="15" />
            Configurar productos
          </button>
          <button v-if="canConfigure" class="vy-btn vy-btn-primary" type="button" @click="router.push({ name: 'landing-temas-config' })">
            <Settings :size="15" />
            Configurar temas
          </button>
        </div>
      </header>

      <section class="ref-card vy-card">
        <div class="ref-icon"><Link2 :size="20" /></div>
        <div>
          <span>Referencia activa</span>
          <strong>{{ referral.name }}</strong>
          <small>{{ referral.phone || "Sin telefono" }} · {{ referral.email || "Sin correo" }}</small>
        </div>
      </section>

      <div v-if="loading" class="loading-box">Cargando herramientas...</div>
      <div v-if="error" class="error-box">{{ error }}</div>

      <section class="section-heading">
        <div>
          <span class="vy-eyebrow">Temas configurables</span>
          <h2>Landings para educar y presentar el negocio</h2>
        </div>
      </section>

      <section class="product-grid">
        <article v-for="topic in digitalLandings" :key="topic.id" class="vy-card product-card topic-card">
          <VyProductImage :grad="topic.imageUrl || 'linear-gradient(135deg, #F2E7C4 0%, #F28705 100%)'" :h="168" />
          <div class="product-body">
            <small>{{ topic.category || "Tema" }}</small>
            <h2>{{ topic.title }}</h2>
            <p>{{ topic.description || "Landing configurable para compartir informacion y captar interesados." }}</p>
            <footer>
              <button class="share-button share-link" type="button" @click="copyTopicLink(topic)">
                <Copy :size="15" />
                Copiar link
              </button>
              <button class="share-button share-message" type="button" @click="copyTopicMessage(topic)">
                <BookOpen :size="15" />
                Copiar mensaje
              </button>
              <button class="icon-button" type="button" aria-label="Abrir landing" @click="openTopic(topic)">
                <ExternalLink :size="17" />
              </button>
            </footer>
          </div>
        </article>
      </section>

      <section class="section-heading products-heading">
        <div>
          <span class="vy-eyebrow">Productos</span>
          <h2>Links personalizados por producto</h2>
        </div>
      </section>

      <section class="product-grid">
        <article v-for="product in productos" :key="product.id" class="vy-card product-card">
          <VyProductImage :grad="product.img" :h="168" />
          <div class="product-body">
            <small>{{ product.cat }}</small>
            <h2>{{ product.name }}</h2>
            <p>{{ product.descripcion || "Landing configurable con informacion ampliada del producto." }}</p>
            <footer>
              <button class="share-button share-link" type="button" @click="copyLink(product)">
                <Copy :size="15" />
                Copiar link
              </button>
              <button class="share-button share-message" type="button" @click="copyMessage(product)">
                <Copy :size="15" />
                Copiar mensaje
              </button>
              <button class="icon-button" type="button" aria-label="Abrir landing" @click="openLanding(product)">
                <ExternalLink :size="17" />
              </button>
            </footer>
          </div>
        </article>
      </section>

      <section v-if="!loading && !productos.length && !digitalLandings.length" class="empty-state vy-card">
        <h2>No hay herramientas disponibles</h2>
        <p>Cuando existan landings de temas o productos apareceran aqui para generar links personalizados.</p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace { min-width: 0; padding: 28px 32px 48px; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.page-header h1 { margin-top: 8px; font-size: 30px; font-weight: 800; }
.page-header p { margin-top: 4px; color: var(--vy-ink-2); font-size: 14px; }
.header-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.header-actions .vy-btn, .product-body .vy-btn { min-height: 40px; padding: 10px 16px; border-radius: 12px; font-weight: 800; }
.ref-card { display: flex; align-items: center; gap: 14px; padding: 16px; margin-bottom: 18px; }
.ref-icon { width: 42px; height: 42px; border-radius: 13px; display: flex; align-items: center; justify-content: center; background: var(--vy-cream); color: var(--vy-orange-deep); }
.ref-card span, .product-body small { display: block; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; letter-spacing: 0.06em; text-transform: uppercase; }
.ref-card strong { display: block; margin-top: 3px; font-size: 16px; font-weight: 900; }
.ref-card small { display: block; margin-top: 2px; color: var(--vy-ink-2); font-size: 12px; }
.loading-box, .error-box { padding: 14px 16px; border-radius: 12px; font-size: 13px; font-weight: 800; margin-bottom: 14px; }
.loading-box { background: var(--vy-surface-2); color: var(--vy-ink-2); }
.error-box { background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); }
.section-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 14px; margin: 22px 0 12px; }
.section-heading h2 { margin-top: 5px; font-size: 20px; font-weight: 900; }
.products-heading { margin-top: 28px; padding-top: 18px; border-top: 1px solid var(--vy-line-2); }
.product-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 14px; }
.product-card { padding: 12px; overflow: hidden; }
.topic-card { background: linear-gradient(180deg, #fff 0%, rgba(255, 248, 232, 0.72) 100%); }
.product-body { padding: 14px 4px 4px; }
.product-body h2 { margin-top: 5px; font-size: 17px; font-weight: 900; line-height: 1.2; }
.product-body p { min-height: 42px; margin-top: 8px; color: var(--vy-ink-2); font-size: 13px; line-height: 1.45; }
.product-body footer { display: flex; align-items: center; gap: 10px; margin-top: 12px; }
.share-button { flex: 1; min-width: 0; min-height: 44px; padding: 0 14px; border-radius: 999px; display: inline-flex; align-items: center; justify-content: center; gap: 8px; font-size: 12px; font-weight: 900; line-height: 1; transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease; }
.share-button:hover { transform: translateY(-1px); }
.share-button:active { transform: translateY(0); }
.share-link { background: #fff; color: var(--vy-ink); border: 1px solid rgba(31, 26, 20, 0.12); box-shadow: 0 10px 22px rgba(31, 26, 20, 0.08); }
.share-link:hover { border-color: rgba(242, 135, 5, 0.45); box-shadow: 0 14px 28px rgba(31, 26, 20, 0.12); }
.share-message { background: linear-gradient(135deg, var(--vy-orange) 0%, #ff9f22 100%); color: #fff; box-shadow: 0 14px 28px rgba(242, 135, 5, 0.25); }
.share-message:hover { box-shadow: 0 18px 34px rgba(242, 135, 5, 0.32); }
.icon-button { width: 42px; height: 42px; border-radius: 12px; display: inline-flex; align-items: center; justify-content: center; background: var(--vy-ink); color: #fff; flex-shrink: 0; }
.empty-state { padding: 38px; text-align: center; color: var(--vy-ink-2); }
.empty-state h2 { color: var(--vy-ink); font-size: 22px; }
.empty-state p { margin-top: 6px; font-size: 14px; }
@media (max-width: 720px) {
  .workspace { padding: 24px 20px 112px; }
  .page-header { align-items: stretch; flex-direction: column; }
  .header-actions, .header-actions .vy-btn { width: 100%; }
  .ref-card { align-items: flex-start; }
  .product-body footer { flex-direction: column; }
  .share-button, .icon-button { width: 100%; }
}
</style>
