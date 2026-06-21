<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { Eye, Image as ImageIcon, MessageCircle, Plus, RefreshCw, Save, ShoppingCart, Trash2, Upload } from "lucide-vue-next";
import { VyProductImage } from "../components/ui.js";
import { apiRequest } from "../services/api.js";
import { loadProductCatalog } from "../services/productCatalogService.js";
import {
  LANDING_SECTION_TYPES,
  loadProductLandingConfig,
  normalizeLandingSection,
  saveProductLandingConfig
} from "../services/productLandingService.js";

const productos = ref([]);
const selectedProductId = ref("");
const loading = ref(false);
const error = ref("");
const loadingConfig = ref(false);
const previewMode = ref(false);

const form = reactive({
  shareMessage: "",
  sections: []
});

const sectionTypes = LANDING_SECTION_TYPES;

const layoutOptions = {
  hero: [
    { value: "imageRight", label: "Texto izquierda / imagen derecha" },
    { value: "imageLeft", label: "Imagen izquierda / texto derecha" },
    { value: "stacked", label: "Apilado" }
  ],
  imageText: [
    { value: "imageRight", label: "Texto izquierda / imagen derecha" },
    { value: "imageLeft", label: "Imagen izquierda / texto derecha" },
    { value: "stacked", label: "Apilado" }
  ],
  videoText: [
    { value: "imageRight", label: "Texto izquierda / video derecha" },
    { value: "imageLeft", label: "Video izquierda / texto derecha" },
    { value: "stacked", label: "Apilado" }
  ],
  benefits: [
    { value: "grid3", label: "3 columnas" },
    { value: "grid2", label: "2 columnas" },
    { value: "list", label: "Lista" }
  ],
  gallery: [
    { value: "grid3", label: "Grilla 3 columnas" },
    { value: "grid2", label: "Grilla 2 columnas" },
    { value: "carousel", label: "Carrusel horizontal" }
  ],
  carousel: [
    { value: "carousel", label: "Carrusel horizontal" },
    { value: "grid3", label: "Grilla 3 columnas" }
  ],
  contact: [
    { value: "cardCentered", label: "Tarjeta centrada" },
    { value: "cardWide", label: "Tarjeta ancha" }
  ],
  text: [
    { value: "centered", label: "Centrado" },
    { value: "full", label: "Ancho completo" }
  ],
  preguntas: [
    { value: "darkAccordion", label: "Acordeon oscuro" },
    { value: "lightAccordion", label: "Acordeon claro" }
  ]
};

const selectedProduct = computed(() =>
  productos.value.find((product) => String(product.id) === String(selectedProductId.value))
);

const previewSections = computed(() => form.sections.map(sectionToPayload));
const previewGallery = computed(() => {
  const mediaSection = previewSections.value.find((section) => ["hero", "gallery", "carousel", "imageText"].includes(section.type) && (section.imageUrl || section.images.length));
  return mediaSection?.images?.length ? mediaSection.images : [mediaSection?.imageUrl || selectedProduct.value?.img].filter(Boolean);
});
const mediaLibrary = computed(() => {
  const urls = new Map();
  productos.value.forEach((product) => {
    if (product.img?.startsWith("/uploads/") || product.img?.startsWith("http")) {
      urls.set(product.img, product.name);
    }
  });
  form.sections.forEach((section) => {
    if (section.imageUrl?.startsWith("/uploads/") || section.imageUrl?.startsWith("http")) {
      urls.set(section.imageUrl, section.title || "Imagen de seccion");
    }
    section.imagesText.split("\n").map((item) => item.trim()).filter(Boolean).forEach((image) => {
      if (image.startsWith("/uploads/") || image.startsWith("http")) {
        urls.set(image, section.title || "Imagen de seccion");
      }
    });
  });
  return [...urls.entries()].map(([url, label]) => ({ url, label }));
});

async function loadProducts() {
  loading.value = true;
  error.value = "";

  try {
    productos.value = await loadProductCatalog({ onlyAvailable: false });
    if (!selectedProductId.value && productos.value.length) {
      selectedProductId.value = String(productos.value[0].id);
    }
  } catch {
    error.value = "No se pudieron cargar los productos.";
  } finally {
    loading.value = false;
  }
}

async function fillForm(product) {
  if (!product) {
    return;
  }

  loadingConfig.value = true;
  const config = await loadProductLandingConfig(product);
  Object.assign(form, {
    shareMessage: config.shareMessage,
    sections: config.sections.map(normalizeSection)
  });
  loadingConfig.value = false;
}

function normalizeSection(section = {}) {
  const normalized = normalizeLandingSection(section);
  return {
    ...normalized,
    layout: normalized.layout,
    imagesText: normalized.images.join("\n"),
    faqItems: parseFaqItems(normalized.images)
  };
}

function addSection(type = "imageText") {
  const productName = selectedProduct.value?.name || "este producto Vidayoung";
  const templates = {
    hero: {
      title: `${productName} para tu rutina diaria`,
      text: selectedProduct.value?.descripcion || "",
      imageUrl: selectedProduct.value?.img || "",
      buttonText: "Comprar ahora",
      whatsappMessage: `Hola, quiero informacion sobre ${productName}.`,
      layout: "imageRight"
    },
    benefits: {
      title: "Beneficios principales",
      text: "Puntos clave configurables para esta landing.",
      images: [
        "Beneficio configurable del producto.",
        "Ideal para recomendacion personalizada.",
        "Disponible con asesoria de tu referido."
      ],
      layout: "grid3"
    },
    text: { title: "Nueva seccion", text: "Contenido configurable.", layout: "centered" },
    imageText: { title: "Nueva seccion", text: "Contenido configurable.", imageUrl: selectedProduct.value?.img || "", layout: "imageRight" },
    videoText: {
      title: "Video recomendado",
      text: "Escribe aqui el contexto del video y por que ayuda a entender este producto.",
      imageUrl: "https://www.youtube.com/watch?v=",
      layout: "imageRight"
    },
    gallery: { title: "Fotos del producto", images: [selectedProduct.value?.img].filter(Boolean), layout: "grid3" },
    carousel: { title: "Carrusel", images: [selectedProduct.value?.img].filter(Boolean), layout: "carousel" },
    preguntas: {
      title: "Preguntas frecuentes",
      text: "",
      images: [
        "¿Que debo saber sobre este producto?|||Te explicamos los puntos mas importantes para ayudarte a decidir.",
        "¿Como puedo comprarlo?|||Escribenos por WhatsApp y te guiamos con disponibilidad, precio y entrega."
      ],
      layout: "darkAccordion"
    },
    contact: {
      title: "Habla con tu asesor",
      text: "Te explico como funciona este producto, te muestro el producto en detalle y evaluamos si tiene sentido para tu rutina.",
      images: ["Los resultados pueden variar. Este producto no sustituye una alimentacion equilibrada ni la consulta medica."],
      buttonText: "Escribeme ahora",
      whatsappMessage: `Hola, quiero informacion sobre ${productName}.`,
      layout: "cardCentered"
    }
  };

  form.sections.push(normalizeSection({ type, ...(templates[type] || templates.imageText) }));
}

function removeSection(index) {
  form.sections.splice(index, 1);
}

async function saveConfig() {
  if (!selectedProduct.value) {
    return;
  }

  await saveProductLandingConfig(selectedProduct.value.id, {
    shareMessage: form.shareMessage,
    sections: form.sections.map(sectionToPayload)
  });

  await Swal.fire({
    title: "Landing guardada",
    text: "La configuracion del producto se actualizo localmente.",
    icon: "success",
    confirmButtonText: "Listo",
    confirmButtonColor: "#F28705"
  });
}

function sectionToPayload(section) {
  const images = section.type === "preguntas"
    ? section.faqItems.map((item) => serializeFaqItem(item)).filter(Boolean)
    : section.imagesText.split("\n").map((item) => item.trim()).filter(Boolean);

  return normalizeLandingSection({
    type: section.type,
    title: section.title,
    text: section.text,
    imageUrl: section.imageUrl,
    images,
    buttonText: section.buttonText,
    whatsappMessage: section.whatsappMessage,
    layout: section.layout
  });
}

function parseFaqItems(items = []) {
  const parsed = items.map((line) => {
    const [question = "", answer = ""] = String(line).split("|||");
    return { question: question.trim(), answer: answer.trim() };
  }).filter((item) => item.question || item.answer);

  return parsed.length ? parsed : [{ question: "", answer: "" }];
}

function serializeFaqItem(item) {
  const question = String(item.question || "").trim();
  const answer = String(item.answer || "").trim();
  return question || answer ? `${question}|||${answer}` : "";
}

function addFaqItem(section) {
  section.faqItems.push({ question: "", answer: "" });
}

function removeFaqItem(section, index) {
  section.faqItems.splice(index, 1);
  if (!section.faqItems.length) {
    addFaqItem(section);
  }
}

function layoutsFor(section) {
  return layoutOptions[section.type] || layoutOptions.text;
}

function appendImage(section, imageUrl) {
  if (!imageUrl) {
    return;
  }

  if (["gallery", "carousel"].includes(section.type)) {
    const current = section.imagesText.split("\n").map((item) => item.trim()).filter(Boolean);
    if (!current.includes(imageUrl)) {
      current.push(imageUrl);
    }
    section.imagesText = current.join("\n");
    return;
  }

  section.imageUrl = imageUrl;
}

async function uploadSectionImage(event, section) {
  const [file] = event.target.files || [];
  event.target.value = "";

  if (!file) {
    return;
  }

  try {
    const formData = new FormData();
    formData.append("imagen", file);
    const saved = await apiRequest("/api/uploads/landings", {
      method: "POST",
      body: formData
    });
    appendImage(section, saved.url);
  } catch (exception) {
    await Swal.fire({
      title: "No se pudo subir la imagen",
      text: exception.message || "Usa PNG, JPG o WEBP.",
      icon: "error",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
  }
}

function togglePreview() {
  previewMode.value = !previewMode.value;
}

function previewProduct() {
  if (!selectedProduct.value) {
    return;
  }

  window.open(`/producto/${selectedProduct.value.id}`, "_blank", "noopener,noreferrer");
}

function youtubeEmbedUrl(url) {
  const value = String(url || "").trim();
  if (!value) return "";

  const match = value.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([A-Za-z0-9_-]{6,})/);
  const videoId = match?.[1] || "";
  return videoId ? `https://www.youtube.com/embed/${videoId}` : "";
}

onMounted(loadProducts);
watch(selectedProduct, fillForm);
</script>

<template>
  <div class="vy landing-config-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Configuracion</div>
          <h1>Landings por producto</h1>
          <p>Edita textos, beneficios, modo de uso y galeria que veran los clientes.</p>
        </div>
        <div class="header-actions">
          <button class="vy-btn vy-btn-ghost" type="button" @click="loadProducts">
            <RefreshCw :size="15" />
            Actualizar
          </button>
          <button class="vy-btn vy-btn-ghost" type="button" @click="togglePreview">
            <Eye :size="15" />
            {{ previewMode ? "Editar" : "Vista previa" }}
          </button>
          <button class="vy-btn vy-btn-dark" type="button" @click="previewProduct">
            <Eye :size="15" />
            Abrir landing
          </button>
          <button class="vy-btn vy-btn-primary" type="button" @click="saveConfig">
            <Save :size="15" />
            Guardar
          </button>
        </div>
      </header>

      <div v-if="loading" class="loading-box">Cargando productos...</div>
      <div v-if="error" class="error-box">{{ error }}</div>

      <section v-if="productos.length" class="config-grid">
        <aside class="vy-card product-list">
          <h2>Productos</h2>
          <button
            v-for="product in productos"
            :key="product.id"
            type="button"
            :class="{ active: String(product.id) === String(selectedProductId) }"
            @click="selectedProductId = String(product.id)"
          >
            <strong>{{ product.name }}</strong>
            <small>{{ product.sku }} · {{ product.cat }}</small>
          </button>
        </aside>

        <form class="vy-card config-form" @submit.prevent="saveConfig">
          <div v-if="loadingConfig" class="loading-box">Cargando configuracion...</div>
          <section v-if="selectedProduct" class="product-summary">
            <VyProductImage :grad="selectedProduct.img" :h="180" />
            <div>
              <span class="vy-eyebrow">Producto seleccionado</span>
              <h2>{{ selectedProduct.name }}</h2>
              <p>{{ selectedProduct.descripcion || "Sin descripcion base." }}</p>
            </div>
          </section>

          <section v-if="!previewMode" class="share-message-editor">
            <label class="full-field">
              <span>Mensaje personalizado para compartir este producto</span>
              <textarea v-model="form.shareMessage" rows="9" placeholder="Escribe el mensaje que se copiara junto al link. Usa {producto} para insertar el nombre del producto." />
            </label>
            <small>Al copiar desde Herramientas digitales, el link personalizado se agregara automaticamente al final.</small>
          </section>

          <section v-if="!previewMode" class="sections-builder">
            <header>
              <div>
                <span class="vy-eyebrow">Constructor</span>
                <h2>Secciones tipo WordPress</h2>
              </div>
              <div class="section-actions">
                <button class="mini-action" type="button" @click="addSection('hero')">
                  <Plus :size="14" />
                  Hero
                </button>
                <button class="mini-action" type="button" @click="addSection('benefits')">
                  <Plus :size="14" />
                  Beneficios
                </button>
                <button class="mini-action" type="button" @click="addSection('text')">
                  <Plus :size="14" />
                  Texto
                </button>
                <button class="mini-action" type="button" @click="addSection('imageText')">
                  <Plus :size="14" />
                  Imagen y texto
                </button>
                <button class="mini-action" type="button" @click="addSection('videoText')">
                  <Plus :size="14" />
                  Video y texto
                </button>
                <button class="mini-action" type="button" @click="addSection('gallery')">
                  <Plus :size="14" />
                  Galeria
                </button>
                <button class="mini-action" type="button" @click="addSection('carousel')">
                  <Plus :size="14" />
                  Carrusel
                </button>
                <button class="mini-action" type="button" @click="addSection('preguntas')">
                  <Plus :size="14" />
                  Preguntas
                </button>
                <button class="mini-action" type="button" @click="addSection('contact')">
                  <Plus :size="14" />
                  Contacto
                </button>
              </div>
            </header>

            <article v-for="(section, index) in form.sections" :key="index" class="section-editor">
              <div class="section-toolbar">
                <label>
                  <span>Tipo de seccion</span>
                  <select v-model="section.type">
                    <option v-for="type in sectionTypes" :key="type.value" :value="type.value">{{ type.label }}</option>
                  </select>
                </label>
                <label>
                  <span>Distribucion</span>
                  <select v-model="section.layout">
                    <option v-for="layout in layoutsFor(section)" :key="layout.value" :value="layout.value">{{ layout.label }}</option>
                  </select>
                </label>
                <button type="button" aria-label="Eliminar seccion" @click="removeSection(index)">
                  <Trash2 :size="16" />
                </button>
              </div>

              <div class="form-grid">
                <label class="full-field">
                  <span>Titulo</span>
                  <input v-model.trim="section.title" />
                </label>
                <label v-if="!['carousel', 'preguntas'].includes(section.type)" class="full-field">
                  <span>Texto</span>
                  <textarea v-model.trim="section.text" rows="4" />
                </label>
                <label v-if="section.type === 'videoText'" class="full-field">
                  <span>Link de YouTube</span>
                  <input v-model.trim="section.imageUrl" placeholder="https://www.youtube.com/watch?v=..." />
                </label>
                <label v-if="['hero', 'imageText', 'contact'].includes(section.type)" class="full-field">
                  <span>{{ section.type === "contact" ? "Foto de contacto" : "Imagen" }}</span>
                  <input v-model.trim="section.imageUrl" placeholder="/uploads/productos/imagen.webp o https://..." />
                </label>
                <section v-if="['hero', 'imageText', 'gallery', 'carousel', 'contact'].includes(section.type)" class="media-tools full-field">
                  <div class="media-actions">
                    <label class="upload-button">
                      <Upload :size="14" />
                      Subir imagen
                      <input type="file" accept="image/png,image/jpeg,image/webp" @change="uploadSectionImage($event, section)" />
                    </label>
                    <span>o selecciona una ya subida</span>
                  </div>
                  <div class="media-library">
                    <button
                      v-for="image in mediaLibrary"
                      :key="image.url"
                      type="button"
                      :title="image.label"
                      @click="appendImage(section, image.url)"
                    >
                      <VyProductImage :grad="image.url" :h="76" />
                      <small>{{ image.label }}</small>
                    </button>
                    <div v-if="!mediaLibrary.length" class="empty-media">
                      <ImageIcon :size="16" />
                      No hay imagenes subidas todavia.
                    </div>
                  </div>
                </section>
                <section v-if="section.type === 'preguntas'" class="faq-builder full-field">
                  <header>
                    <span>Preguntas y respuestas</span>
                    <button class="mini-action" type="button" @click="addFaqItem(section)">
                      <Plus :size="14" />
                      Pregunta
                    </button>
                  </header>
                  <article v-for="(item, faqIndex) in section.faqItems" :key="faqIndex" class="faq-editor-row">
                    <label>
                      <span>Pregunta</span>
                      <input v-model.trim="item.question" placeholder="¿Qué sabe Black Harmony?" />
                    </label>
                    <label>
                      <span>Respuesta</span>
                      <textarea v-model.trim="item.answer" rows="3" placeholder="Escribe la respuesta que se desplegará al hacer click." />
                    </label>
                    <button type="button" aria-label="Eliminar pregunta" @click="removeFaqItem(section, faqIndex)">
                      <Trash2 :size="15" />
                    </button>
                  </article>
                </section>
                <label v-if="['benefits', 'gallery', 'carousel', 'contact'].includes(section.type)" class="full-field">
                  <span>
                    {{
                      section.type === "benefits"
                        ? "Beneficios, uno por linea"
                        : section.type === "contact"
                          ? "Nota inferior"
                          : "Imagenes, una por linea"
                    }}
                  </span>
                  <textarea v-model="section.imagesText" rows="5" />
                </label>
                <template v-if="['hero', 'contact'].includes(section.type)">
                  <label>
                    <span>Texto boton</span>
                    <input v-model.trim="section.buttonText" placeholder="Escribeme ahora" />
                  </label>
                  <label>
                    <span>Mensaje WhatsApp</span>
                    <textarea v-model.trim="section.whatsappMessage" rows="3" />
                  </label>
                </template>
              </div>
            </article>
          </section>

          <section v-else class="config-preview">
            <article
              v-for="(section, index) in previewSections"
              :key="`${section.type}-${index}`"
              class="preview-section"
              :class="[section.type, section.layout]"
            >
              <template v-if="section.type === 'hero'">
                <div>
                  <span class="vy-chip vy-chip-orange">{{ selectedProduct.cat }}</span>
                  <h1>{{ section.title }}</h1>
                  <p>{{ section.text }}</p>
                  <button type="button">
                    <ShoppingCart :size="16" />
                    {{ section.buttonText || "Comprar ahora" }}
                  </button>
                </div>
                <VyProductImage :grad="section.imageUrl || previewGallery[0]" :h="300" big />
              </template>

              <template v-else-if="section.type === 'benefits'">
                <header>
                  <span class="vy-eyebrow">Beneficios</span>
                  <h2>{{ section.title }}</h2>
                  <p>{{ section.text }}</p>
                </header>
                <div class="preview-benefits">
                  <span v-for="benefit in section.images" :key="benefit">{{ benefit }}</span>
                </div>
              </template>

              <template v-else-if="section.type === 'text'">
                <span class="vy-eyebrow">Texto</span>
                <h2>{{ section.title }}</h2>
                <p>{{ section.text }}</p>
              </template>

              <template v-else-if="section.type === 'imageText'">
                <div>
                  <span class="vy-eyebrow">Seccion</span>
                  <h2>{{ section.title }}</h2>
                  <p>{{ section.text }}</p>
                </div>
                <VyProductImage :grad="section.imageUrl || previewGallery[0]" :h="240" />
              </template>

              <template v-else-if="section.type === 'videoText'">
                <div>
                  <span class="vy-eyebrow">Video</span>
                  <h2>{{ section.title }}</h2>
                  <p>{{ section.text }}</p>
                </div>
                <div class="preview-video-frame">
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
                <div class="preview-media">
                  <VyProductImage v-for="image in section.images" :key="image" :grad="image" :h="180" />
                </div>
              </template>

              <template v-else-if="section.type === 'contact'">
                <div class="preview-contact-card">
                  <VyProductImage :grad="section.imageUrl || previewGallery[0]" :h="220" />
                  <h2>{{ section.title || "Asesor Vidayoung" }}</h2>
                  <strong>+59169096889</strong>
                  <p>{{ section.text }}</p>
                  <button type="button">
                    <MessageCircle :size="17" />
                    {{ section.buttonText || "Escribeme ahora" }}
                  </button>
                  <small>{{ section.images[0] }}</small>
                </div>
              </template>

              <template v-else-if="section.type === 'preguntas'">
                <div class="preview-faq-card">
                  <h2>{{ section.title }}</h2>
                  <details v-for="item in section.images" :key="item" class="preview-faq-item">
                    <summary>{{ item.split("|||")[0] }}</summary>
                    <p>{{ item.split("|||")[1] }}</p>
                  </details>
                </div>
              </template>
            </article>
          </section>
        </form>
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
.header-actions .vy-btn { min-height: 40px; padding: 10px 16px; border-radius: 12px; font-weight: 800; }
.loading-box, .error-box { padding: 14px 16px; border-radius: 12px; font-size: 13px; font-weight: 800; margin-bottom: 14px; }
.loading-box { background: var(--vy-surface-2); color: var(--vy-ink-2); }
.error-box { background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); }
.config-grid { display: grid; grid-template-columns: minmax(260px, 0.32fr) minmax(0, 1fr); gap: 16px; align-items: start; }
.product-list, .config-form { padding: 18px; }
.product-list h2 { font-size: 16px; font-weight: 900; margin-bottom: 12px; }
.product-list { display: grid; gap: 8px; position: sticky; top: 20px; }
.product-list button { padding: 12px; border-radius: 12px; background: var(--vy-surface-2); border: 1px solid var(--vy-line); text-align: left; }
.product-list button.active, .product-list button:hover { background: #fff; border-color: rgba(242, 135, 5, 0.45); box-shadow: var(--vy-shadow-sm); }
.product-list strong, .product-list small { display: block; }
.product-list strong { font-size: 13px; font-weight: 900; }
.product-list small { margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 700; }
.product-summary { display: grid; grid-template-columns: minmax(240px, 0.45fr) minmax(0, 1fr); gap: 16px; align-items: center; margin-bottom: 18px; }
.product-summary h2 { margin-top: 6px; font-size: 24px; font-weight: 900; }
.product-summary p { margin-top: 8px; color: var(--vy-ink-2); font-size: 14px; line-height: 1.45; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.full-field { grid-column: 1 / -1; }
label span { display: block; margin-bottom: 6px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; letter-spacing: 0.06em; text-transform: uppercase; }
input, textarea, select { width: 100%; padding: 12px 13px; border: 1px solid var(--vy-line); border-radius: 10px; background: var(--vy-surface); color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 600; }
input:focus, textarea:focus, select:focus { outline: 2px solid rgba(242, 135, 5, 0.22); border-color: var(--vy-orange); }
.share-message-editor { margin: 0 0 20px; padding: 16px; border: 1px solid var(--vy-line); border-radius: 14px; background: var(--vy-surface-2); }
.share-message-editor textarea { min-height: 190px; background: #fff; line-height: 1.5; }
.share-message-editor small { display: block; margin-top: 8px; color: var(--vy-ink-3); font-size: 12px; font-weight: 700; }
.sections-builder { margin-top: 24px; padding-top: 20px; border-top: 1px solid var(--vy-line-2); }
.sections-builder > header { display: flex; align-items: flex-end; justify-content: space-between; gap: 14px; margin-bottom: 14px; }
.sections-builder h2 { margin-top: 5px; font-size: 18px; font-weight: 900; }
.section-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.mini-action { min-height: 36px; padding: 0 12px; border-radius: 10px; background: var(--vy-ink); color: #fff; display: inline-flex; align-items: center; gap: 7px; font-size: 12px; font-weight: 900; }
.section-editor { padding: 14px; border: 1px solid var(--vy-line); border-radius: 14px; background: var(--vy-surface-2); box-shadow: var(--vy-shadow-sm); }
.section-editor + .section-editor { margin-top: 12px; }
.section-toolbar { display: grid; grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr) auto; gap: 12px; align-items: end; margin-bottom: 12px; }
.section-toolbar button { width: 42px; height: 42px; border-radius: 11px; display: inline-flex; align-items: center; justify-content: center; background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); }
.media-tools { padding: 12px; border: 1px dashed rgba(242, 135, 5, 0.42); border-radius: 12px; background: rgba(242, 135, 5, 0.05); }
.media-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 10px; }
.media-actions > span { color: var(--vy-ink-3); font-size: 12px; font-weight: 800; }
.upload-button { min-height: 36px; padding: 0 12px; border-radius: 10px; background: var(--vy-orange); color: #fff; display: inline-flex; align-items: center; gap: 7px; font-size: 12px; font-weight: 900; cursor: pointer; }
.upload-button input { display: none; }
.media-library { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 10px; }
.media-library button { min-width: 0; padding: 6px; border: 1px solid var(--vy-line); border-radius: 12px; background: #fff; text-align: left; }
.media-library small { display: block; margin-top: 6px; color: var(--vy-ink-3); font-size: 10px; font-weight: 800; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.empty-media { min-height: 76px; padding: 12px; border-radius: 10px; background: #fff; color: var(--vy-ink-3); display: flex; align-items: center; justify-content: center; gap: 8px; font-size: 12px; font-weight: 800; }
.faq-builder { padding: 12px; border: 1px solid var(--vy-line); border-radius: 12px; background: #fff; }
.faq-builder > header { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 12px; }
.faq-builder > header > span { margin: 0; }
.faq-editor-row { display: grid; grid-template-columns: minmax(180px, 0.8fr) minmax(0, 1.2fr) auto; gap: 10px; align-items: end; padding: 10px; border-radius: 12px; background: var(--vy-surface-2); }
.faq-editor-row + .faq-editor-row { margin-top: 10px; }
.faq-editor-row > button { width: 40px; height: 40px; border-radius: 11px; display: inline-flex; align-items: center; justify-content: center; background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); }
.config-preview { display: grid; gap: 14px; margin-top: 20px; }
.preview-section { padding: 18px; border: 1px solid var(--vy-line); border-radius: 14px; background: var(--vy-surface-2); }
.preview-section.hero, .preview-section.imageText, .preview-section.videoText { display: grid; grid-template-columns: minmax(0, 1fr) minmax(240px, 0.8fr); gap: 16px; align-items: center; }
.preview-section.hero.imageLeft, .preview-section.imageText.imageLeft, .preview-section.videoText.imageLeft { grid-template-columns: minmax(240px, 0.8fr) minmax(0, 1fr); }
.preview-section.hero.imageLeft > div:first-child, .preview-section.imageText.imageLeft > div:first-child, .preview-section.videoText.imageLeft > div:first-child { order: 2; }
.preview-section.hero.stacked, .preview-section.imageText.stacked, .preview-section.videoText.stacked { grid-template-columns: 1fr; }
.preview-section.text.centered { max-width: 760px; margin: 0 auto; text-align: center; }
.preview-section h1 { margin-top: 12px; font-size: clamp(30px, 4vw, 48px); line-height: 1; font-weight: 900; }
.preview-section h2 { margin-top: 8px; font-size: 24px; line-height: 1.1; font-weight: 900; }
.preview-section p { margin-top: 10px; color: var(--vy-ink-2); font-size: 14px; line-height: 1.6; }
.preview-section button { min-height: 42px; margin-top: 16px; padding: 0 15px; border-radius: 12px; display: inline-flex; align-items: center; justify-content: center; gap: 8px; background: var(--vy-orange); color: #fff; font-weight: 900; }
.preview-benefits { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; margin-top: 14px; }
.preview-section.grid2 .preview-benefits { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.preview-section.list .preview-benefits { grid-template-columns: 1fr; }
.preview-benefits span { padding: 12px; border-radius: 12px; background: #fff; border: 1px solid var(--vy-line); color: var(--vy-ink-2); font-size: 13px; font-weight: 700; line-height: 1.45; }
.preview-media { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin-top: 14px; }
.preview-video-frame { aspect-ratio: 16 / 9; border-radius: 14px; overflow: hidden; background: #1d1d1f; }
.preview-video-frame iframe { width: 100%; height: 100%; border: 0; display: block; }
.preview-video-frame span { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; padding: 16px; color: rgba(255, 255, 255, 0.72); font-size: 12px; font-weight: 800; text-align: center; }
.preview-section.grid2 .preview-media { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.preview-section.carousel .preview-media { grid-auto-flow: column; grid-auto-columns: minmax(180px, 32%); grid-template-columns: none; overflow-x: auto; padding-bottom: 4px; }
.preview-contact-card { max-width: 500px; margin: 0 auto; padding: 24px; border-radius: 14px; background: #1d1d1f; color: #fff; text-align: center; border: 1px solid rgba(255, 255, 255, 0.08); }
.preview-section.cardWide .preview-contact-card { max-width: 760px; }
.preview-contact-card > div:first-child { width: min(300px, 100%); height: 300px !important; margin: 0 auto 18px; border-radius: 50% !important; padding: 0 !important; align-items: center !important; }
.preview-contact-card h2 { color: #fff; }
.preview-contact-card strong { display: block; margin-top: 8px; color: rgba(255, 255, 255, 0.78); font-size: 16px; }
.preview-contact-card p { color: rgba(255, 255, 255, 0.86); }
.preview-contact-card button { width: min(400px, 100%); background: #25d366; }
.preview-contact-card small { display: block; margin-top: 16px; color: rgba(255, 255, 255, 0.52); font-size: 12px; line-height: 1.45; }
.preview-faq-card { padding: 24px; border-radius: 14px; background: #161719; color: #fff; }
.preview-faq-card h2 { margin: 0 0 18px; color: #fff; font-size: 28px; }
.preview-faq-item { border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 12px; background: rgba(255, 255, 255, 0.03); }
.preview-faq-item + .preview-faq-item { margin-top: 10px; }
.preview-faq-item summary { min-height: 54px; padding: 0 18px; display: flex; align-items: center; justify-content: space-between; gap: 12px; cursor: pointer; font-weight: 900; }
.preview-faq-item summary::after { content: "+"; color: #d7bd6a; font-size: 20px; }
.preview-faq-item[open] summary::after { content: "-"; }
.preview-faq-item p { margin: 0; padding: 0 18px 18px; color: rgba(255, 255, 255, 0.76); text-align: justify; }
@media (max-width: 980px) {
  .config-grid, .product-summary, .preview-section.hero, .preview-section.imageText, .preview-section.videoText { grid-template-columns: 1fr; }
  .product-list { position: static; }
}
@media (max-width: 720px) {
  .workspace { padding: 24px 20px 112px; }
  .page-header { align-items: stretch; flex-direction: column; }
  .header-actions, .header-actions .vy-btn { width: 100%; }
  .form-grid { grid-template-columns: 1fr; }
  .faq-editor-row { grid-template-columns: 1fr; }
  .section-toolbar { grid-template-columns: 1fr; }
  .sections-builder > header { align-items: stretch; flex-direction: column; }
  .section-actions, .mini-action { width: 100%; }
  .preview-benefits, .preview-section.grid2 .preview-benefits, .preview-media, .preview-section.grid2 .preview-media { grid-template-columns: 1fr; }
}
</style>
