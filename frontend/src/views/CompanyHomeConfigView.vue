<script setup>
import { onMounted, reactive, ref } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { Eye, Plus, RefreshCw, Save, Trash2, Upload } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import { LANDING_SECTION_TYPES, normalizeLandingSection } from "../services/productLandingService.js";
import { getCompanyHomeDefaults, saveCompanyHomeConfig, loadCompanyHomeConfig } from "../services/companyHomeService.js";

const loading = ref(false);
const saving = ref(false);
const form = reactive(getCompanyHomeDefaults());
const sectionTypes = LANDING_SECTION_TYPES;

function resetForm(config = getCompanyHomeDefaults()) {
  Object.assign(form, {
    id: config.id || null,
    slug: config.slug,
    title: config.title || "",
    category: config.category || "Empresa",
    imageUrl: config.imageUrl || "",
    description: config.description || "",
    sections: (config.sections || []).map(normalizeSection)
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

  return normalizeLandingSection({ ...section, images });
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

function addSection(type = "text") {
  const templates = {
    hero: { category: "Inicio", title: form.title, text: form.description, imageUrl: form.imageUrl, buttonText: "Iniciar sesion", layout: "imageRight" },
    text: { category: "Empresa", title: "Nueva seccion", text: "Contenido editable.", layout: "centered" },
    imageText: { category: "Contenido", title: "Nueva seccion", text: "Contenido editable.", imageUrl: form.imageUrl, layout: "imageRight" },
    benefits: { category: "Beneficios", title: "Beneficios", text: "Describe los beneficios principales.", images: ["Beneficio editable.", "Otro diferencial.", "Siguiente punto clave."], layout: "grid3" },
    gallery: { category: "Productos", title: "Productos", text: "Productos o imagenes destacadas.", images: [], layout: "grid3" },
    carousel: { category: "Galeria", title: "Galeria", images: [], layout: "carousel" },
    preguntas: { category: "Preguntas", title: "Preguntas frecuentes", images: ["Pregunta editable|||Respuesta editable."], layout: "darkAccordion" },
    social: { category: "Redes", title: "Redes sociales", text: "Canales oficiales.", images: ["Instagram|||https://www.instagram.com/"], layout: "grid3" },
    contact: { category: "Contacto", title: "Unete a Vidayoung", text: "Ingresa a tu cuenta para continuar.", buttonText: "Iniciar sesion", layout: "cardCentered" }
  };

  form.sections.push(normalizeSection({ type, ...(templates[type] || templates.text) }));
}

function removeSection(index) {
  form.sections.splice(index, 1);
}

function addFaqItem(section) {
  section.faqItems.push({ question: "", answer: "" });
}

function removeFaqItem(section, index) {
  section.faqItems.splice(index, 1);
  if (!section.faqItems.length) addFaqItem(section);
}

async function uploadImage(event, section = null) {
  const [file] = event.target.files || [];
  event.target.value = "";
  if (!file) return;

  try {
    const formData = new FormData();
    formData.append("imagen", file);
    const saved = await apiRequest("/api/uploads/landings", { method: "POST", body: formData });
    if (section && ["gallery", "carousel"].includes(section.type)) {
      const current = section.imagesText.split("\n").map((item) => item.trim()).filter(Boolean);
      section.imagesText = [...current, saved.url].join("\n");
    } else if (section) {
      section.imageUrl = saved.url;
    } else {
      form.imageUrl = saved.url;
    }
  } catch (exception) {
    await Swal.fire({ title: "No se pudo subir la imagen", text: exception.message || "Usa PNG, JPG o WEBP.", icon: "error", confirmButtonColor: "#F28705" });
  }
}

async function loadConfig() {
  loading.value = true;
  try {
    resetForm(await loadCompanyHomeConfig());
  } finally {
    loading.value = false;
  }
}

async function saveConfig() {
  saving.value = true;
  try {
    const saved = await saveCompanyHomeConfig({
      ...form,
      sections: form.sections.map(sectionToPayload)
    });
    resetForm(saved);
    await Swal.fire({ title: "Pagina principal guardada", text: "La home publica se actualizo correctamente.", icon: "success", confirmButtonColor: "#F28705" });
  } catch (exception) {
    await Swal.fire({ title: "No se pudo guardar", text: exception.message || "Revisa los datos.", icon: "error", confirmButtonColor: "#F28705" });
  } finally {
    saving.value = false;
  }
}

function openHome() {
  window.open("/", "_blank", "noopener,noreferrer");
}

onMounted(loadConfig);
</script>

<template>
  <main class="vy home-config workspace">
    <header class="page-header">
      <div>
        <div class="vy-eyebrow">Sitio publico</div>
        <h1>Pagina principal</h1>
        <p>Configura la home que se muestra en la ruta publica /.</p>
      </div>
      <div class="header-actions">
        <button class="vy-btn vy-btn-ghost" type="button" @click="loadConfig"><RefreshCw :size="15" />Actualizar</button>
        <button class="vy-btn vy-btn-dark" type="button" @click="openHome"><Eye :size="15" />Abrir /</button>
        <button class="vy-btn vy-btn-primary" type="button" :disabled="saving" @click="saveConfig"><Save :size="15" />{{ saving ? "Guardando..." : "Guardar" }}</button>
      </div>
    </header>

    <div v-if="loading" class="loading-box">Cargando configuracion...</div>

    <form v-else class="vy-card config-form" @submit.prevent="saveConfig">
      <section class="base-fields">
        <div class="form-grid">
          <label>
            <span>Titulo principal</span>
            <input v-model.trim="form.title" required maxlength="160" />
          </label>
          <label>
            <span>Categoria</span>
            <input v-model.trim="form.category" maxlength="120" />
          </label>
          <label class="full-field">
            <span>Imagen principal</span>
            <input v-model.trim="form.imageUrl" placeholder="/uploads/landings/imagen.webp o https://..." />
          </label>
          <label class="full-field">
            <span>Descripcion</span>
            <textarea v-model.trim="form.description" rows="4" />
          </label>
        </div>
        <label class="upload-button">
          <Upload :size="14" />
          Subir imagen principal
          <input type="file" accept="image/png,image/jpeg,image/webp" @change="uploadImage" />
        </label>
      </section>

      <section class="sections-builder">
        <header>
          <div>
            <span class="vy-eyebrow">Constructor</span>
            <h2>Secciones dinamicas</h2>
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
              <span>Layout</span>
              <select v-model="section.layout">
                <option value="imageRight">Imagen derecha</option>
                <option value="imageLeft">Imagen izquierda</option>
                <option value="centered">Centrado</option>
                <option value="grid3">3 columnas</option>
                <option value="grid2">2 columnas</option>
                <option value="carousel">Carrusel</option>
                <option value="darkAccordion">Acordeon oscuro</option>
              </select>
            </label>
            <button type="button" aria-label="Eliminar seccion" @click="removeSection(index)"><Trash2 :size="16" /></button>
          </div>

          <div class="form-grid">
            <label class="full-field">
              <span>Categoria</span>
              <input v-model.trim="section.category" maxlength="120" />
            </label>
            <label class="full-field">
              <span>Titulo</span>
              <input v-model.trim="section.title" />
            </label>
            <label v-if="!['carousel', 'preguntas'].includes(section.type)" class="full-field">
              <span>Texto</span>
              <textarea v-model.trim="section.text" rows="4" />
            </label>
            <label v-if="['hero', 'imageText'].includes(section.type)" class="full-field">
              <span>Imagen</span>
              <input v-model.trim="section.imageUrl" />
            </label>
            <label v-if="['benefits', 'gallery', 'carousel', 'social'].includes(section.type)" class="full-field">
              <span>{{ section.type === "social" ? "Redes: Nombre|||https://..." : "Items o imagenes, uno por linea" }}</span>
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
            <label v-if="['gallery', 'carousel', 'hero', 'imageText'].includes(section.type)" class="upload-button full-field">
              <Upload :size="14" />
              Subir imagen para esta seccion
              <input type="file" accept="image/png,image/jpeg,image/webp" @change="uploadImage($event, section)" />
            </label>
          </div>
        </article>
      </section>
    </form>
  </main>
</template>

<style scoped>
.workspace { min-width: 0; padding: 28px 32px 48px; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.page-header h1 { margin-top: 8px; font-size: 30px; font-weight: 800; }
.page-header p { margin-top: 4px; color: var(--vy-ink-2); font-size: 14px; }
.header-actions, .section-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.header-actions .vy-btn { min-height: 40px; padding: 10px 16px; border-radius: 12px; font-weight: 800; }
.loading-box { padding: 14px 16px; border-radius: 12px; background: var(--vy-surface-2); color: var(--vy-ink-2); font-size: 13px; font-weight: 800; }
.config-form { padding: 18px; }
.base-fields { padding-bottom: 18px; border-bottom: 1px solid var(--vy-line-2); }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.full-field { grid-column: 1 / -1; }
label span, .faq-builder > header > span { display: block; margin-bottom: 6px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; letter-spacing: 0.06em; text-transform: uppercase; }
input, textarea, select { width: 100%; padding: 12px 13px; border: 1px solid var(--vy-line); border-radius: 10px; background: var(--vy-surface); color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 600; }
input:focus, textarea:focus, select:focus { outline: 2px solid rgba(242, 135, 5, 0.22); border-color: var(--vy-orange); }
.upload-button, .mini-action { min-height: 36px; padding: 0 12px; border-radius: 10px; display: inline-flex; align-items: center; justify-content: center; gap: 7px; background: var(--vy-ink); color: #fff; font-size: 12px; font-weight: 900; cursor: pointer; }
.upload-button { width: fit-content; margin-top: 12px; }
.upload-button.full-field { width: 100%; margin-top: 0; }
.upload-button input { display: none; }
.sections-builder { margin-top: 20px; }
.sections-builder > header { display: flex; align-items: flex-end; justify-content: space-between; gap: 14px; margin-bottom: 14px; }
.sections-builder h2 { margin-top: 5px; font-size: 18px; font-weight: 900; }
.section-editor { padding: 14px; border: 1px solid var(--vy-line); border-radius: 14px; background: var(--vy-surface-2); box-shadow: var(--vy-shadow-sm); }
.section-editor + .section-editor { margin-top: 12px; }
.section-toolbar { display: grid; grid-template-columns: minmax(0, 0.8fr) minmax(0, 1fr) auto; gap: 12px; align-items: end; margin-bottom: 12px; }
.section-toolbar button, .faq-editor-row button { width: 42px; height: 42px; border-radius: 11px; display: inline-flex; align-items: center; justify-content: center; background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); }
.faq-builder { padding: 12px; border: 1px dashed rgba(242, 135, 5, 0.42); border-radius: 12px; background: rgba(242, 135, 5, 0.05); }
.faq-builder > header { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 12px; }
.faq-editor-row { display: grid; grid-template-columns: minmax(160px, 0.7fr) minmax(0, 1.3fr) auto; gap: 10px; align-items: end; }
.faq-editor-row + .faq-editor-row { margin-top: 10px; }
@media (max-width: 720px) {
  .workspace { padding: 24px 20px 112px; }
  .page-header, .sections-builder > header { align-items: stretch; flex-direction: column; }
  .header-actions, .header-actions .vy-btn, .section-actions, .mini-action, .form-grid, .section-toolbar, .faq-editor-row { width: 100%; grid-template-columns: 1fr; }
}
</style>
