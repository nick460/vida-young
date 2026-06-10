<script setup>
import { computed, onMounted, ref } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import {
  Camera,
  Check,
  Settings,
  Sparkles,
  Star,
  UploadCloud
} from "lucide-vue-next";
import { USER } from "../data.js";
import { useAuthStore } from "../stores/authStore.js";
import { subirFotoPerfil } from "../services/profileService.js";
import { VyAvatar } from "../components/ui.js";

const authStore = useAuthStore();
const fileInput = ref(null);
const loadingProfile = ref(false);
const uploadingPhoto = ref(false);
const API_URL = import.meta.env.VITE_API_URL || "";
const MAX_PHOTO_SIZE = 5 * 1024 * 1024;

const account = computed(() => ({
  name: nombreCompleto.value || authStore.usuario?.username || USER.name,
  email: authStore.usuario?.persona?.email || USER.email,
  phone: authStore.usuario?.persona?.telefono || "Sin teléfono",
  document: authStore.usuario?.persona?.documento || "Sin documento",
  avatar: iniciales.value,
  role: authStore.usuario?.roles?.[0] || USER.level,
  photo: authStore.usuario?.fotoPerfil || ""
}));

const nombreCompleto = computed(() => {
  const persona = authStore.usuario?.persona;
  return [persona?.nombres, persona?.apellidos].filter(Boolean).join(" ").trim();
});

const iniciales = computed(() => {
  const source = nombreCompleto.value || authStore.usuario?.username || USER.avatar;
  return source
    .split(" ")
    .filter(Boolean)
    .slice(0, 2)
    .map((item) => item[0])
    .join("")
    .toUpperCase();
});

const photoUrl = computed(() => {
  if (!account.value.photo) {
    return "";
  }

  if (account.value.photo.startsWith("http")) {
    return account.value.photo;
  }

  return `${API_URL}${account.value.photo}`;
});

const stats = [
  { label: "Red", value: "138" },
  { label: "Ventas", value: "$12M" },
  { label: "Bonos", value: "24" }
];

const personalInfo = computed(() => [
  { label: "Nombre", value: account.value.name },
  { label: "Correo", value: account.value.email },
  { label: "Documento", value: account.value.document },
  { label: "Teléfono", value: account.value.phone },
  { label: "Usuario", value: authStore.usuario?.username || "Sin usuario", mono: true },
  { label: "Código embajador", value: USER.code, mono: true },
  { label: "Miembro desde", value: USER.joined },
  { label: "Patrocinador", value: "Vidayoung Founders" }
]);

const achievements = [
  { label: "Primera venta", date: "Mar 2024", done: true },
  { label: "10 referidos directos", date: "Jul 2024", done: true },
  { label: "Líder Plata", date: "Sep 2024", done: true },
  { label: "Líder Oro", date: "Nov 2024", done: true },
  { label: "Líder Diamante", date: "Mar 2025", done: true },
  { label: "Líder Diamante Élite", date: "Pendiente", done: false }
];

onMounted(async () => {
  loadingProfile.value = true;
  try {
    await authStore.cargarPerfil();
  } catch (error) {
    await Swal.fire({
      title: "No se pudo cargar el perfil",
      text: error.message || "Intenta nuevamente.",
      icon: "error",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
  } finally {
    loadingProfile.value = false;
  }
});

function openPhotoPicker() {
  fileInput.value?.click();
}

async function handlePhotoChange(event) {
  const [file] = event.target.files || [];
  event.target.value = "";

  if (!file) {
    return;
  }

  if (!file.type.startsWith("image/")) {
    await Swal.fire({
      title: "Formato no válido",
      text: "Selecciona una imagen JPG, PNG o WEBP.",
      icon: "warning",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
    return;
  }

  if (file.size > MAX_PHOTO_SIZE) {
    await Swal.fire({
      title: "Imagen demasiado grande",
      text: "La foto no debe superar los 5 MB.",
      icon: "warning",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
    return;
  }

  uploadingPhoto.value = true;
  try {
    const perfil = await subirFotoPerfil(file);
    authStore.actualizarPerfil(perfil);
    await Swal.fire({
      title: "Foto actualizada",
      text: "Tu foto de perfil se guardó correctamente.",
      icon: "success",
      confirmButtonText: "Listo",
      confirmButtonColor: "#F28705"
    });
  } catch (error) {
    await Swal.fire({
      title: "No se pudo subir la foto",
      text: error.message || "Intenta nuevamente.",
      icon: "error",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
  } finally {
    uploadingPhoto.value = false;
  }
}

</script>

<template>
  <div class="vy profile-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Perfil</div>
          <h1>Tu perfil</h1>
          <p>Datos personales, configuración de cuenta y trayectoria en Vidayoung.</p>
        </div>
      </header>

      <section class="profile-grid">
        <article class="vy-card profile-card">
          <div class="avatar-wrap">
            <img v-if="photoUrl" class="profile-photo" :src="photoUrl" :alt="account.name" />
            <VyAvatar v-else :name="account.avatar" :size="92" bg="var(--vy-orange)" color="#fff" />
            <button type="button" aria-label="Cambiar foto" :disabled="uploadingPhoto" @click="openPhotoPicker">
              <Camera v-if="!uploadingPhoto" :size="14" stroke-width="2" />
              <Settings v-else :size="14" stroke-width="2" />
            </button>
            <input
              ref="fileInput"
              class="photo-input"
              type="file"
              accept="image/png,image/jpeg,image/webp"
              @change="handlePhotoChange"
            />
          </div>

          <h2>{{ account.name }}</h2>
          <p>{{ account.email }}</p>
          <span class="rank-chip">
            <Star :size="13" fill="currentColor" stroke-width="1.8" />
            {{ account.role }}
          </span>

          <div class="stats-grid">
            <article v-for="item in stats" :key="item.label">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>

          <button class="profile-action-button" type="button" :disabled="uploadingPhoto" @click="openPhotoPicker">
            <UploadCloud :size="17" stroke-width="2" />
            <span>{{ uploadingPhoto ? "Guardando foto..." : "Cambiar foto" }}</span>
          </button>
        </article>

        <div class="detail-column">
          <article class="vy-card info-card">
            <h2>Información personal</h2>
            <div class="info-grid">
              <div v-for="item in personalInfo" :key="item.label">
                <span>{{ item.label }}</span>
                <strong :class="{ 'vy-mono': item.mono }">{{ item.value }}</strong>
              </div>
            </div>
          </article>

          <article class="vy-card timeline-card">
            <header>
              <h2>Trayectoria</h2>
              <span class="vy-chip vy-chip-success">5 de 6 desbloqueados</span>
            </header>

            <div class="timeline-list">
              <article v-for="achievement in achievements" :key="achievement.label" class="timeline-item">
                <span class="timeline-icon" :class="{ pending: !achievement.done }">
                  <Check v-if="achievement.done" :size="16" stroke-width="2.2" />
                  <Sparkles v-else :size="16" stroke-width="2" />
                </span>
                <div>
                  <strong :class="{ pending: !achievement.done }">{{ achievement.label }}</strong>
                  <small>{{ achievement.date }}</small>
                </div>
                <em v-if="!achievement.done">En progreso</em>
              </article>
            </div>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace {
  padding: 28px 32px 40px;
  min-width: 0;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 30px;
  font-weight: 800;
  margin-top: 8px;
}

.page-header p {
  font-size: 14px;
  color: var(--vy-ink-2);
  margin-top: 4px;
}

.profile-grid {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(0, 1.6fr);
  gap: 14px;
}

.profile-card {
  padding: 28px;
  text-align: center;
  background: linear-gradient(180deg, var(--vy-cream) 0%, var(--vy-surface) 50%);
  border-color: transparent;
}

.avatar-wrap {
  position: relative;
  display: inline-block;
}

.profile-photo {
  width: 92px;
  height: 92px;
  border-radius: 50%;
  object-fit: cover;
  display: block;
  border: 4px solid var(--vy-surface);
  box-shadow: 0 12px 22px rgba(31, 26, 20, 0.16);
}

.photo-input {
  display: none;
}

.avatar-wrap button {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--vy-ink), #000);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid var(--vy-surface);
  box-shadow: 0 8px 18px rgba(31, 26, 20, 0.22);
  transition: transform 0.16s ease, background 0.16s ease, box-shadow 0.16s ease;
}

.avatar-wrap button:hover:not(:disabled) {
  background: linear-gradient(135deg, var(--vy-orange), var(--vy-orange-deep));
  transform: translateY(-1px) scale(1.04);
  box-shadow: 0 10px 22px rgba(242, 135, 5, 0.32);
}

.avatar-wrap button:disabled {
  cursor: wait;
  opacity: 0.75;
}

.profile-card h2 {
  font-size: 22px;
  font-weight: 800;
  margin-top: 16px;
}

.profile-card > p {
  font-size: 12px;
  color: var(--vy-ink-3);
  margin-top: 4px;
}

.rank-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  padding: 5px 11px;
  border-radius: 999px;
  background: rgba(242, 135, 5, 0.12);
  color: var(--vy-orange-deep);
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 24px;
  text-align: left;
}

.stats-grid article {
  padding: 12px;
  background: var(--vy-surface);
  border-radius: 10px;
  border: 1px solid var(--vy-line);
}

.stats-grid span,
.info-grid span {
  display: block;
  font-size: 10px;
  color: var(--vy-ink-3);
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.stats-grid strong {
  display: block;
  font-family: var(--font-display);
  font-weight: 800;
  font-size: 18px;
  margin-top: 2px;
}

.profile-action-button {
  margin-top: 20px;
  width: 100%;
  min-height: 44px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 9px;
  background: linear-gradient(135deg, var(--vy-ink), #000);
  color: #fff;
  font-size: 13.5px;
  font-weight: 800;
  box-shadow: 0 12px 24px rgba(31, 26, 20, 0.18);
  transition: transform 0.16s ease, box-shadow 0.16s ease, background 0.16s ease;
}

.profile-action-button:hover:not(:disabled) {
  background: linear-gradient(135deg, var(--vy-orange), var(--vy-orange-deep));
  transform: translateY(-1px);
  box-shadow: 0 14px 30px rgba(242, 135, 5, 0.28);
}

.profile-action-button:disabled {
  cursor: wait;
  opacity: 0.74;
}

.detail-column {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.info-card,
.timeline-card {
  padding: 22px;
}

.info-card h2,
.timeline-card h2 {
  font-size: 16px;
  font-weight: 800;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 16px;
  font-size: 13px;
}

.info-grid strong {
  display: block;
  font-weight: 700;
  margin-top: 4px;
}

.timeline-card header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.timeline-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--vy-orange);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.timeline-icon.pending {
  background: var(--vy-surface-2);
  color: var(--vy-ink-3);
  border: 2px dashed var(--vy-line);
}

.timeline-item div {
  flex: 1;
}

.timeline-item strong {
  display: block;
  font-weight: 800;
  font-size: 13.5px;
}

.timeline-item strong.pending {
  color: var(--vy-ink-3);
}

.timeline-item small {
  display: block;
  font-size: 11px;
  color: var(--vy-ink-3);
  margin-top: 2px;
}

.timeline-item em {
  font-style: normal;
  font-size: 11px;
  color: var(--vy-orange-deep);
  font-weight: 800;
}

@media (max-width: 1040px) {
  .workspace {
    padding: 24px 20px 32px;
  }

  .profile-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .stats-grid,
  .info-grid {
    grid-template-columns: 1fr;
  }

  .timeline-card header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>

