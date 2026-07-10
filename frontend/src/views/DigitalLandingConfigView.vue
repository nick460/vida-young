<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { Eye, Image as ImageIcon, Plus, RefreshCw, Save, Trash2, Upload } from "lucide-vue-next";
import { VyProductImage } from "../components/ui.js";
import { apiRequest } from "../services/api.js";
import {
  LANDING_SECTION_TYPES,
  normalizeLandingSection,
  slugify
} from "../services/productLandingService.js";
import {
  deleteDigitalLanding,
  getDefaultDigitalShareMessage,
  getDigitalLandingDefaults,
  loadDigitalLandings,
  saveDigitalLandingConfig
} from "../services/digitalLandingService.js";

const landings = ref([]);
const selectedId = ref("");
const loading = ref(false);
const saving = ref(false);
const previewMode = ref(false);
const error = ref("");

const form = reactive(getDigitalLandingDefaults());
const sectionTypes = LANDING_SECTION_TYPES;

const selectedLanding = computed(() =>
  landings.value.find((item) => String(item.id) === String(selectedId.value))
);

const mediaLibrary = computed(() => {
  const urls = new Map();
  landings.value.forEach((landing) => {
    if (landing.imageUrl) {
      urls.set(landing.imageUrl, landing.title);
    }
    landing.sections.forEach((section) => {
      if (section.imageUrl) {
        urls.set(section.imageUrl, section.title || landing.title);
      }
      section.images.forEach((image) => urls.set(image, section.title || landing.title));
    });
  });
  return [...urls.entries()].filter(([url]) => url.startsWith("/uploads/") || url.startsWith("http")).map(([url, label]) => ({ url, label }));
});

function resetForm(landing = getDigitalLandingDefaults()) {
  Object.assign(form, {
    id: landing.id || null,
    slug: landing.slug || "",
    title: landing.title || "",
    category: landing.category || "",
    imageUrl: landing.imageUrl || "",
    description: landing.description || "",
    shareMessage: landing.shareMessage || getDefaultDigitalShareMessage(landing),
    sections: (landing.sections || []).map(normalizeSection)
  });
}

function normalizeSection(section = {}) {
  const normalized = normalizeLandingSection(section);
  return {
    ...normalized,
    imagesText: normalized.images.join("\n"),
    faqItems: parseFaqItems(normalized.images)
  };
}

function sectionToPayload(section) {
  const images = section.type === "preguntas"
    ? section.faqItems.map(serializeFaqItem).filter(Boolean)
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

async function loadLandings() {
  loading.value = true;
  error.value = "";
  try {
    landings.value = await loadDigitalLandings();
    if (!selectedId.value && landings.value.length) {
      selectedId.value = String(landings.value[0].id);
    }
  } catch {
    error.value = "No se pudieron cargar las landings digitales.";
  } finally {
    loading.value = false;
  }
}

function newLanding() {
  selectedId.value = "";
  resetForm(getDigitalLandingDefaults({
    title: "Nuevo tema",
    category: "Formacion",
    description: "Describe aqui el tema que deseas compartir."
  }));
}

function addSection(type = "text") {
  const topic = form.title || "este tema";
  const templates = {
    hero: {
      title: topic,
      text: form.description,
      imageUrl: form.imageUrl,
      buttonText: "Quiero saber mas",
      whatsappMessage: `Hola, quiero informacion sobre ${topic}.`,
      layout: "imageRight"
    },
    benefits: {
      title: "Puntos clave",
      text: "Ideas principales para esta landing.",
      images: ["Punto clave editable.", "Aplicacion practica.", "Siguiente paso recomendado."],
      layout: "grid3"
    },
    text: { title: "Nueva seccion", text: "Contenido editable.", layout: "centered" },
    imageText: { title: "Nueva seccion", text: "Contenido editable.", imageUrl: form.imageUrl, layout: "imageRight" },
    videoText: {
      title: "Video recomendado",
      text: "Escribe aqui el contexto del video y que deberia aprender la persona al verlo.",
      imageUrl: "https://www.youtube.com/watch?v=",
      layout: "imageRight"
    },
    gallery: { title: "Galeria", images: [form.imageUrl].filter(Boolean), layout: "grid3" },
    carousel: { title: "Carrusel", images: [form.imageUrl].filter(Boolean), layout: "carousel" },
    social: {
      title: "Redes sociales",
      text: "Canales oficiales o recomendados.",
      images: ["Instagram|||https://www.instagram.com/"],
      layout: "grid3"
    },
    preguntas: {
      title: "Preguntas frecuentes",
      images: ["¿Que debo saber?|||Aqui puedes escribir una respuesta clara.", "¿Como avanzo?|||Escribenos para recibir orientacion."],
      layout: "darkAccordion"
    },
    contact: {
      title: "Habla con tu asesor",
      text: "Te explico este tema y resolvemos tus preguntas.",
      images: ["Informacion referencial."],
      buttonText: "Escribeme ahora",
      whatsappMessage: `Hola, quiero informacion sobre ${topic}.`,
      layout: "cardCentered"
    }
  };

  form.sections.push(normalizeSection({ type, ...(templates[type] || templates.text) }));
}

function removeSection(index) {
  form.sections.splice(index, 1);
}

function appendImage(section, imageUrl) {
  if (!imageUrl) return;
  if (["gallery", "carousel"].includes(section.type)) {
    const current = section.imagesText.split("\n").map((item) => item.trim()).filter(Boolean);
    if (!current.includes(imageUrl)) current.push(imageUrl);
    section.imagesText = current.join("\n");
    return;
  }
  section.imageUrl = imageUrl;
}

async function uploadImage(event, section = null) {
  const [file] = event.target.files || [];
  event.target.value = "";
  if (!file) return;

  try {
    const formData = new FormData();
    formData.append("imagen", file);
    const saved = await apiRequest("/api/uploads/landings", { method: "POST", body: formData });
    if (section) {
      appendImage(section, saved.url);
    } else {
      form.imageUrl = saved.url;
    }
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

async function saveLanding() {
  saving.value = true;
  try {
    const saved = await saveDigitalLandingConfig({
      ...form,
      slug: form.slug || slugify(form.title),
      sections: form.sections.map(sectionToPayload)
    });
    await loadLandings();
    selectedId.value = String(saved.id);
    await Swal.fire({
      title: "Landing guardada",
      text: "La landing digital se actualizo correctamente.",
      icon: "success",
      confirmButtonText: "Listo",
      confirmButtonColor: "#F28705"
    });
  } catch (exception) {
    await Swal.fire({
      title: "No se pudo guardar",
      text: exception.message || "Revisa los datos.",
      icon: "error",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
  } finally {
    saving.value = false;
  }
}

async function removeLanding() {
  if (!form.id) return;
  const result = await Swal.fire({
    title: "Eliminar landing",
    text: `Se quitara ${form.title} de herramientas digitales.`,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Eliminar",
    cancelButtonText: "Cancelar",
    confirmButtonColor: "#C4452A"
  });
  if (!result.isConfirmed) return;
  await deleteDigitalLanding(form.id);
  selectedId.value = "";
  await loadLandings();
}

function openLanding() {
  if (!form.slug) return;
  window.open(`/herramienta/${form.slug}`, "_blank", "noopener,noreferrer");
}

function youtubeEmbedUrl(url) {
  const value = String(url || "").trim();
  if (!value) return "";

  const match = value.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/|youtube\.com\/embed\/)([A-Za-z0-9_-]{6,})/);
  const videoId = match?.[1] || "";
  return videoId ? `https://www.youtube.com/embed/${videoId}` : "";
}

watch(selectedLanding, (landing) => {
  if (landing) resetForm(landing);
}, { immediate: true });

watch(() => form.title, (title) => {
  if (!form.id && title) {
    form.slug = slugify(title);
  }
});

onMounted(loadLandings);
</script>

<template>
  <div class="vy digital-landing-config-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Herramientas digitales</div>
          <h1>Landings por tema</h1>
          <p>Crea paginas configurables para educacion financiera, claves del negocio o cualquier tema.</p>
        </div>
        <div class="header-actions">
          <button class="vy-btn vy-btn-ghost" type="button" @click="loadLandings"><RefreshCw :size="15" />Actualizar</button>
          <button class="vy-btn vy-btn-ghost" type="button" @click="previewMode = !previewMode"><Eye :size="15" />{{ previewMode ? "Editar" : "Vista previa" }}</button>
          <button class="vy-btn vy-btn-dark" type="button" :disabled="!form.slug" @click="openLanding"><Eye :size="15" />Abrir landing</button>
          <button class="vy-btn vy-btn-primary" type="button" :disabled="saving" @click="saveLanding"><Save :size="15" />{{ saving ? "Guardando..." : "Guardar" }}</button>
        </div>
      </header>

      <div v-if="loading" class="loading-box">Cargando landings...</div>
      <div v-if="error" class="error-box">{{ error }}</div>

      <section class="config-grid">
        <aside class="vy-card topic-list">
          <button class="new-topic" type="button" @click="newLanding"><Plus :size="15" />Nuevo tema</button>
          <button
            v-for="landing in landings"
            :key="landing.id"
            type="button"
            :class="{ active: String(landing.id) === String(selectedId) }"
            @click="selectedId = String(landing.id)"
          >
            <strong>{{ landing.title }}</strong>
            <small>{{ landing.category || "Tema" }} · /{{ landing.slug }}</small>
          </button>
        </aside>

        <form class="vy-card config-form" @submit.prevent="saveLanding">
          <section class="base-fields">
            <div class="form-grid">
              <label>
                <span>Titulo</span>
                <input v-model.trim="form.title" required maxlength="160" />
              </label>
              <label>
                <span>Slug publico</span>
                <input v-model.trim="form.slug" required maxlength="120" />
              </label>
              <label>
                <span>Categoria</span>
                <input v-model.trim="form.category" maxlength="120" placeholder="Formacion, negocio, finanzas..." />
              </label>
              <label>
                <span>Imagen principal</span>
                <input v-model.trim="form.imageUrl" placeholder="/uploads/landings/imagen.webp o https://..." />
              </label>
              <label class="full-field">
                <span>Descripcion</span>
                <textarea v-model.trim="form.description" rows="4" />
              </label>
              <label class="full-field">
                <span>Mensaje para compartir</span>
                <textarea v-model="form.shareMessage" rows="6" placeholder="Usa {tema} para insertar el titulo." />
              </label>
            </div>
            <div class="media-actions">
              <label class="upload-button">
                <Upload :size="14" />
                Subir imagen principal
                <input type="file" accept="image/png,image/jpeg,image/webp" @change="uploadImage" />
              </label>
              <button v-if="form.id" class="danger-button" type="button" @click="removeLanding"><Trash2 :size="14" />Eliminar landing</button>
            </div>
          </section>

          <section v-if="!previewMode" class="sections-builder">
            <header>
              <div>
                <span class="vy-eyebrow">Constructor</span>
                <h2>Secciones</h2>
              </div>
              <div class="section-actions">
                <button v-for="type in sectionTypes" :key="type.value" class="mini-action" type="button" @click="addSection(type.value)">
                  <Plus :size="14" />{{ type.label }}
                </button>
              </div>
            </header>

            <article v-for="(section, index) in form.sections" :key="index" class="section-editor">
              <div class="section-toolbar">
                <label>
                  <span>Tipo</span>
                  <select v-model="section.type">
                    <option v-for="type in sectionTypes" :key="type.value" :value="type.value">{{ type.label }}</option>
                  </select>
                </label>
                <label>
                  <span>Distribucion</span>
                  <select v-model="section.layout">
                    <option value="imageRight">Imagen derecha</option>
                    <option value="imageLeft">Imagen izquierda</option>
                    <option value="stacked">Apilado</option>
                    <option value="grid3">3 columnas</option>
                    <option value="grid2">2 columnas</option>
                    <option value="list">Lista</option>
                    <option value="centered">Centrado</option>
                    <option value="cardCentered">Tarjeta centrada</option>
                    <option value="darkAccordion">Acordeon oscuro</option>
                  </select>
                </label>
                <button type="button" aria-label="Eliminar seccion" @click="removeSection(index)"><Trash2 :size="16" /></button>
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
                  <span>Imagen</span>
                  <input v-model.trim="section.imageUrl" />
                </label>
                <section v-if="['hero', 'imageText', 'gallery', 'carousel', 'contact'].includes(section.type)" class="media-tools full-field">
                  <div class="media-actions">
                    <label class="upload-button">
                      <Upload :size="14" />
                      Subir imagen
                      <input type="file" accept="image/png,image/jpeg,image/webp" @change="uploadImage($event, section)" />
                    </label>
                  </div>
                  <div class="media-library">
                    <button v-for="image in mediaLibrary" :key="image.url" type="button" @click="appendImage(section, image.url)">
                      <VyProductImage :grad="image.url" :h="70" />
                      <small>{{ image.label }}</small>
                    </button>
                    <div v-if="!mediaLibrary.length" class="empty-media"><ImageIcon :size="15" />Sin imagenes</div>
                  </div>
                </section>
                <label v-if="['benefits', 'gallery', 'carousel', 'contact', 'social'].includes(section.type)" class="full-field">
                  <span>{{ section.type === "benefits" ? "Items, uno por linea" : "Imagenes/notas, una por linea" }}</span>
                  <textarea v-model="section.imagesText" rows="5" />
                </label>
                <section v-if="section.type === 'preguntas'" class="faq-builder full-field">
                  <header>
                    <span>Preguntas y respuestas</span>
                    <button class="mini-action" type="button" @click="addFaqItem(section)"><Plus :size="14" />Pregunta</button>
                  </header>
                  <article v-for="(item, faqIndex) in section.faqItems" :key="faqIndex" class="faq-editor-row">
                    <input v-model.trim="item.question" placeholder="Pregunta" />
                    <textarea v-model.trim="item.answer" rows="3" placeholder="Respuesta" />
                    <button type="button" @click="removeFaqItem(section, faqIndex)"><Trash2 :size="14" /></button>
                  </article>
                </section>
                <template v-if="['hero', 'contact'].includes(section.type)">
                  <label>
                    <span>Texto boton</span>
                    <input v-model.trim="section.buttonText" />
                  </label>
                  <label>
                    <span>Mensaje WhatsApp</span>
                    <textarea v-model.trim="section.whatsappMessage" rows="3" />
                  </label>
                </template>
              </div>
            </article>
          </section>

          <section v-else class="preview-list">
            <article v-for="(section, index) in form.sections.map(sectionToPayload)" :key="index" class="preview-section" :class="[section.type, section.layout]">
              <div v-if="section.type === 'videoText'" class="preview-video-frame">
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
              <VyProductImage v-else-if="section.imageUrl" :grad="section.imageUrl" :h="220" />
              <div>
                <span class="vy-eyebrow">{{ section.type }}</span>
                <h2>{{ section.title }}</h2>
                <p>{{ section.text }}</p>
              </div>
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
.config-grid { display: grid; grid-template-columns: minmax(250px, 0.3fr) minmax(0, 1fr); gap: 16px; align-items: start; }
.topic-list, .config-form { padding: 18px; }
.topic-list { display: grid; gap: 8px; position: sticky; top: 20px; }
.topic-list button { padding: 12px; border-radius: 12px; background: var(--vy-surface-2); border: 1px solid var(--vy-line); text-align: left; }
.topic-list button.active, .topic-list button:hover { background: #fff; border-color: rgba(242, 135, 5, 0.45); box-shadow: var(--vy-shadow-sm); }
.topic-list strong, .topic-list small { display: block; }
.topic-list strong { font-size: 13px; font-weight: 900; }
.topic-list small { margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 700; }
.new-topic { display: inline-flex; align-items: center; gap: 8px; color: var(--vy-orange-deep); font-weight: 900; }
.base-fields { padding-bottom: 18px; border-bottom: 1px solid var(--vy-line-2); }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.full-field { grid-column: 1 / -1; }
label span, .faq-builder > header > span { display: block; margin-bottom: 6px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; letter-spacing: 0.06em; text-transform: uppercase; }
input, textarea, select { width: 100%; padding: 12px 13px; border: 1px solid var(--vy-line); border-radius: 10px; background: var(--vy-surface); color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 600; }
input:focus, textarea:focus, select:focus { outline: 2px solid rgba(242, 135, 5, 0.22); border-color: var(--vy-orange); }
.media-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-top: 12px; }
.upload-button, .mini-action, .danger-button { min-height: 36px; padding: 0 12px; border-radius: 10px; display: inline-flex; align-items: center; gap: 7px; font-size: 12px; font-weight: 900; cursor: pointer; }
.upload-button, .mini-action { background: var(--vy-ink); color: #fff; }
.upload-button input { display: none; }
.danger-button { background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); }
.sections-builder { margin-top: 20px; }
.sections-builder > header { display: flex; align-items: flex-end; justify-content: space-between; gap: 14px; margin-bottom: 14px; }
.sections-builder h2 { margin-top: 5px; font-size: 18px; font-weight: 900; }
.section-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.section-editor { padding: 14px; border: 1px solid var(--vy-line); border-radius: 14px; background: var(--vy-surface-2); box-shadow: var(--vy-shadow-sm); }
.section-editor + .section-editor { margin-top: 12px; }
.section-toolbar { display: grid; grid-template-columns: minmax(0, 0.8fr) minmax(0, 1fr) auto; gap: 12px; align-items: end; margin-bottom: 12px; }
.section-toolbar button, .faq-editor-row button { width: 42px; height: 42px; border-radius: 11px; display: inline-flex; align-items: center; justify-content: center; background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); }
.media-tools, .faq-builder { padding: 12px; border: 1px dashed rgba(242, 135, 5, 0.42); border-radius: 12px; background: rgba(242, 135, 5, 0.05); }
.media-library { display: grid; grid-template-columns: repeat(auto-fill, minmax(120px, 1fr)); gap: 10px; }
.media-library button { padding: 6px; border: 1px solid var(--vy-line); border-radius: 12px; background: #fff; text-align: left; }
.media-library small { display: block; margin-top: 6px; color: var(--vy-ink-3); font-size: 10px; font-weight: 800; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.empty-media { min-height: 70px; display: flex; align-items: center; justify-content: center; gap: 8px; color: var(--vy-ink-3); font-size: 12px; font-weight: 800; }
.faq-builder > header { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 12px; }
.faq-editor-row { display: grid; grid-template-columns: minmax(160px, 0.7fr) minmax(0, 1.3fr) auto; gap: 10px; align-items: end; }
.faq-editor-row + .faq-editor-row { margin-top: 10px; }
.preview-list { display: grid; gap: 12px; margin-top: 20px; }
.preview-section { padding: 16px; border: 1px solid var(--vy-line); border-radius: 14px; background: var(--vy-surface-2); display: grid; grid-template-columns: minmax(220px, 0.36fr) minmax(0, 1fr); gap: 14px; align-items: center; }
.preview-section h2 { margin-top: 6px; font-size: 22px; font-weight: 900; }
.preview-section p { margin-top: 8px; color: var(--vy-ink-2); line-height: 1.55; }
.preview-video-frame { aspect-ratio: 16 / 9; border-radius: 14px; overflow: hidden; background: #1d1d1f; }
.preview-video-frame iframe { width: 100%; height: 100%; border: 0; display: block; }
.preview-video-frame span { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; padding: 16px; color: rgba(255, 255, 255, 0.72); font-size: 12px; font-weight: 800; text-align: center; }
@media (max-width: 980px) {
  .config-grid, .preview-section { grid-template-columns: 1fr; }
  .topic-list { position: static; }
}
@media (max-width: 720px) {
  .workspace { padding: 24px 20px 112px; }
  .page-header, .sections-builder > header { align-items: stretch; flex-direction: column; }
  .header-actions, .header-actions .vy-btn, .section-actions, .mini-action { width: 100%; }
  .form-grid, .section-toolbar, .faq-editor-row { grid-template-columns: 1fr; }
}
</style>
