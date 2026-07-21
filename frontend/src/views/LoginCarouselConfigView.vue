<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { ImagePlus, RefreshCw, Save, Trash2 } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";

const loading = ref(false);
const saving = ref(false);
const uploading = ref(false);
const error = ref("");
const items = ref([]);
const selectedId = ref("");
const imageFile = ref(null);
const mobileImageFile = ref(null);
const imagePreview = ref("");
const mobileImagePreview = ref("");
const API_URL = import.meta.env.VITE_API_URL || "";

const form = reactive({
  id: null,
  titulo: "",
  descripcion: "",
  imagenUrl: "",
  imagenMobileUrl: "",
  orden: 0,
  activo: true
});

const selectedItem = computed(() => items.value.find((item) => item.id === Number(selectedId.value)) || null);

function mediaUrl(path) {
  if (!path) return "";
  if (path.startsWith("http") || path.startsWith("blob:")) return path;
  return `${API_URL}${path.startsWith("/") ? path : `/${path}`}`;
}

function resetForm(item = null) {
  form.id = item?.id || null;
  form.titulo = item?.titulo || "";
  form.descripcion = item?.descripcion || "";
  form.imagenUrl = item?.imagenUrl || "";
  form.imagenMobileUrl = item?.imagenMobileUrl || "";
  form.orden = Number(item?.orden || 0);
  form.activo = item?.activo !== false;
  imageFile.value = null;
  mobileImageFile.value = null;
  imagePreview.value = form.imagenUrl;
  mobileImagePreview.value = form.imagenMobileUrl;
}

async function loadItems() {
  loading.value = true;
  error.value = "";

  try {
    items.value = await apiRequest("/api/login-carousel");

    if (selectedItem.value) {
      resetForm(selectedItem.value);
    } else if (items.value.length) {
      selectedId.value = String(items.value[0].id);
      resetForm(items.value[0]);
    } else {
      selectedId.value = "";
      resetForm();
    }
  } catch (exception) {
    error.value = exception.message || "No se pudieron cargar las novedades.";
  } finally {
    loading.value = false;
  }
}

function newItem() {
  selectedId.value = "";
  resetForm({ orden: items.value.length + 1, activo: true });
}

function selectItem(item) {
  selectedId.value = String(item.id);
  resetForm(item);
}

function onImageSelected(event) {
  const file = event.target.files?.[0] || null;
  imageFile.value = file;
  imagePreview.value = file ? URL.createObjectURL(file) : form.imagenUrl;
}

function onMobileImageSelected(event) {
  const file = event.target.files?.[0] || null;
  mobileImageFile.value = file;
  mobileImagePreview.value = file ? URL.createObjectURL(file) : form.imagenMobileUrl;
}

async function uploadImageIfNeeded(fileRef, currentUrl) {
  if (!fileRef.value) {
    return currentUrl;
  }

  uploading.value = true;

  try {
    const formData = new FormData();
    formData.append("imagen", fileRef.value);
    const saved = await apiRequest("/api/uploads/landings", { method: "POST", body: formData });
    return saved.url;
  } finally {
    uploading.value = false;
  }
}

async function saveItem() {
  saving.value = true;
  error.value = "";

  try {
    const imageUrl = await uploadImageIfNeeded(imageFile, form.imagenUrl);
    const imageMobileUrl = await uploadImageIfNeeded(mobileImageFile, form.imagenMobileUrl);
    const saved = await apiRequest(form.id ? `/api/login-carousel/${form.id}` : "/api/login-carousel", {
      method: form.id ? "PUT" : "POST",
      body: JSON.stringify({
        titulo: form.titulo,
        descripcion: form.descripcion,
        imagenUrl: imageUrl,
        imagenMobileUrl: imageMobileUrl || null,
        orden: Number(form.orden || 0),
        activo: form.activo
      })
    });

    selectedId.value = String(saved.id);
    await loadItems();

    await Swal.fire({
      title: "Novedad guardada",
      icon: "success",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
  } catch (exception) {
    error.value = exception.message || "No se pudo guardar la novedad.";
  } finally {
    saving.value = false;
  }
}

async function deleteItem() {
  if (!form.id) return;

  const result = await Swal.fire({
    title: "Eliminar novedad",
    text: "Esta novedad ya no aparecera en el login.",
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Eliminar",
    cancelButtonText: "Cancelar",
    confirmButtonColor: "#C4452A",
    cancelButtonColor: "#1F1A14"
  });

  if (!result.isConfirmed) return;

  await apiRequest(`/api/login-carousel/${form.id}`, { method: "DELETE" });
  selectedId.value = "";
  await loadItems();
}

onMounted(loadItems);
</script>

<template>
  <main class="login-carousel-config">
    <section class="page-header">
      <div>
        <span class="vy-chip vy-chip-orange">LOGIN</span>
        <h1>Novedades login</h1>
        <p>Configura el carrusel con titulo, descripcion y fotografia para la pantalla de ingreso.</p>
      </div>
      <div class="header-actions">
        <button type="button" class="vy-btn vy-btn-ghost" :disabled="loading" @click="loadItems">
          <RefreshCw :size="15" /> Actualizar
        </button>
        <button type="button" class="vy-btn vy-btn-dark" @click="newItem">
          Nueva novedad
        </button>
      </div>
    </section>

    <p v-if="error" class="error-box">{{ error }}</p>

    <section class="layout-grid">
      <aside class="panel item-list">
        <button
          v-for="item in items"
          :key="item.id"
          type="button"
          class="item-row"
          :class="{ active: item.id === Number(selectedId) }"
          @click="selectItem(item)"
        >
          <img v-if="item.imagenUrl" :src="mediaUrl(item.imagenUrl)" :alt="item.titulo" />
          <span>
            <strong>{{ item.titulo }}</strong>
            <small>Orden {{ item.orden }} - {{ item.activo ? "Activo" : "Inactivo" }}</small>
          </span>
        </button>
        <p v-if="!items.length && !loading" class="empty-state">No hay novedades configuradas.</p>
        <p v-if="loading" class="empty-state">Cargando novedades...</p>
      </aside>

      <form class="panel config-form" @submit.prevent="saveItem">
        <div class="preview-grid">
          <div>
            <span class="preview-label">Imagen principal 1:1</span>
            <div class="image-preview square-preview">
              <img v-if="imagePreview" :src="mediaUrl(imagePreview)" alt="Vista previa principal" />
              <div v-else>
                <ImagePlus :size="36" />
                <span>Sin imagen</span>
              </div>
            </div>
          </div>
          <div>
            <span class="preview-label">Banner movil 4:1</span>
            <div class="image-preview mobile-preview">
              <img v-if="mobileImagePreview" :src="mediaUrl(mobileImagePreview)" alt="Vista previa movil" />
              <div v-else>
                <ImagePlus :size="28" />
                <span>Sin banner</span>
              </div>
            </div>
          </div>
        </div>

        <label>
          Titulo
          <input v-model.trim="form.titulo" required maxlength="140" />
        </label>

        <label>
          Descripcion
          <textarea v-model.trim="form.descripcion" required maxlength="500"></textarea>
        </label>

        <div class="form-grid">
          <label>
            Orden
            <input v-model.number="form.orden" type="number" min="0" />
          </label>
          <label class="toggle-field">
            <span>Activo</span>
            <input v-model="form.activo" type="checkbox" />
          </label>
        </div>

        <label>
          Imagen principal 1:1
          <input type="file" accept="image/png,image/jpeg,image/webp" @change="onImageSelected" />
        </label>

        <label>
          Banner movil 4:1
          <input type="file" accept="image/png,image/jpeg,image/webp" @change="onMobileImageSelected" />
        </label>

        <footer>
          <button v-if="form.id" type="button" class="danger-button" @click="deleteItem">
            <Trash2 :size="15" /> Eliminar
          </button>
          <button type="submit" class="vy-btn vy-btn-primary" :disabled="saving || uploading">
            <Save :size="15" /> {{ saving || uploading ? "Guardando..." : "Guardar" }}
          </button>
        </footer>
      </form>
    </section>
  </main>
</template>

<style scoped>
.login-carousel-config {
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-header,
.header-actions,
.config-form footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.page-header h1 {
  margin-top: 10px;
  font-size: 30px;
  font-weight: 900;
}

.page-header p {
  margin-top: 5px;
  color: var(--vy-ink-2);
}

.layout-grid {
  display: grid;
  grid-template-columns: minmax(280px, 0.8fr) minmax(420px, 1.4fr);
  gap: 16px;
}

.panel {
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface);
  padding: 18px;
  box-shadow: var(--vy-shadow-sm);
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.item-row {
  width: 100%;
  padding: 10px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface-2);
  display: grid;
  grid-template-columns: 62px 1fr;
  gap: 10px;
  text-align: left;
}

.item-row.active {
  border-color: var(--vy-orange);
  background: rgba(242, 135, 5, 0.08);
}

.item-row img {
  width: 62px;
  height: 62px;
  border-radius: 8px;
  object-fit: cover;
}

.item-row strong,
.item-row small {
  display: block;
}

.item-row strong {
  font-size: 13px;
  font-weight: 900;
}

.item-row small {
  margin-top: 4px;
  color: var(--vy-ink-3);
  font-size: 12px;
  font-weight: 800;
}

.config-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.preview-grid {
  display: grid;
  grid-template-columns: minmax(220px, 0.8fr) minmax(260px, 1.2fr);
  gap: 14px;
  align-items: start;
}

.preview-label {
  display: block;
  margin-bottom: 6px;
  color: var(--vy-ink-2);
  font-size: 13px;
  font-weight: 900;
}

.image-preview {
  width: 100%;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  overflow: hidden;
  background: var(--vy-surface-2);
}

.square-preview {
  aspect-ratio: 1 / 1;
}

.mobile-preview {
  aspect-ratio: 4 / 1;
}

.image-preview img,
.image-preview div {
  width: 100%;
  height: 100%;
}

.image-preview img {
  object-fit: cover;
}

.image-preview div {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--vy-ink-3);
  font-weight: 900;
}

.config-form label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--vy-ink-2);
  font-size: 13px;
  font-weight: 900;
}

.config-form input,
.config-form textarea {
  width: 100%;
  min-height: 44px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  padding: 0 12px;
  background: #fff;
  color: var(--vy-ink);
  font: inherit;
}

.config-form textarea {
  min-height: 120px;
  padding: 12px;
  resize: vertical;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.toggle-field {
  justify-content: center;
}

.toggle-field input {
  width: 20px;
  min-height: 20px;
  accent-color: var(--vy-orange);
}

.danger-button {
  min-height: 42px;
  padding: 0 14px;
  border-radius: 8px;
  border: 1px solid rgba(196, 69, 42, 0.28);
  background: rgba(196, 69, 42, 0.08);
  color: var(--vy-danger);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-weight: 900;
}

.error-box,
.empty-state {
  padding: 12px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 800;
}

.error-box {
  color: var(--vy-danger);
  background: rgba(196, 69, 42, 0.1);
}

.empty-state {
  color: var(--vy-ink-3);
  background: var(--vy-surface-2);
}

@media (max-width: 900px) {
  .login-carousel-config {
    padding: 18px;
  }

  .page-header,
  .header-actions,
  .config-form footer {
    align-items: stretch;
    flex-direction: column;
  }

  .layout-grid,
  .preview-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
