<script setup>
import { computed, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import Swal from "sweetalert2";
import { LogOut } from "lucide-vue-next";
import AppSidebar from "./components/AppSidebar.vue";
import NotificationBell from "./components/NotificationBell.vue";
import { VyAvatar } from "./components/ui.js";
import { useAuthStore } from "./stores/authStore.js";
import { useNotificacionesStore } from "./stores/notificacionesStore.js";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const notificacionesStore = useNotificacionesStore();

const showSidebar = computed(() => route.meta.sidebar === true);

const user = computed(() => ({
  name: authStore.usuario?.username || "Administrador Vidayoung",
  avatar: (authStore.usuario?.username || "AV").slice(0, 2).toUpperCase(),
  rol: authStore.usuario?.roles?.[0] || "ADMIN"
}));

async function logout() {
  const result = await Swal.fire({
    title: "Cerrar sesión",
    text: "¿Estás seguro de que quieres salir de tu cuenta?",
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Sí, cerrar sesión",
    cancelButtonText: "Cancelar",
    reverseButtons: true,
    background: "#FFFFFF",
    color: "#1F1A14",
    confirmButtonColor: "#F28705",
    cancelButtonColor: "#1F1A14",
    iconColor: "#F28705",
    customClass: {
      popup: "vy-swal-popup",
      title: "vy-swal-title",
      htmlContainer: "vy-swal-text",
      confirmButton: "vy-swal-confirm",
      cancelButton: "vy-swal-cancel"
    }
  });

  if (!result.isConfirmed) return;

  authStore.logout();
  router.push({ name: "login" });
}

let ultimoToastId = null;

function mostrarToastNotificacion(notificacion) {
  if (!notificacion || notificacion.id === ultimoToastId) {
    return;
  }
  ultimoToastId = notificacion.id;
  Swal.fire({
    toast: true,
    position: "top-end",
    showConfirmButton: false,
    timer: 5000,
    timerProgressBar: true,
    icon: "info",
    title: notificacion.titulo,
    text: notificacion.mensaje,
    background: "#FFFFFF",
    color: "#1F1A14",
    customClass: {
      popup: "vy-swal-popup",
      title: "vy-swal-title",
      htmlContainer: "vy-swal-text"
    }
  });
}

watch(
  () => notificacionesStore.ultimaNotificacion,
  (notificacion) => mostrarToastNotificacion(notificacion)
);

watch(
  () => authStore.isAuthenticated,
  (autenticado) => {
    if (autenticado) {
      notificacionesStore.cargar();
      notificacionesStore.conectar();
    } else {
      notificacionesStore.desconectar();
    }
  },
  { immediate: true }
);

function nav(id) {
  router.push({ name: id });
  window.scrollTo({ top: 0, behavior: "instant" });
}
</script>

<template>
  <div class="vy app-layout" :class="{ 'with-sidebar': showSidebar }">
    <AppSidebar v-if="showSidebar" />
    <div v-if="showSidebar" class="app-main">
      <header class="app-topbar">
        <NotificationBell />
        <div class="topbar-user">
          <button class="avatar-button" type="button" title="Mi cuenta" @click="nav('profile')">
            <VyAvatar :name="user.avatar" :size="40" bg="var(--vy-orange)" color="#fff" />
          </button>
          <div class="user-meta">
            <strong>{{ user.name }}</strong>
            <small>{{ user.rol }}</small>
          </div>
          <button class="logout-button" type="button" title="Cerrar sesión" @click="logout">
            <LogOut :size="17" stroke-width="2" />
          </button>
        </div>
      </header>
      <router-view v-slot="{ Component }">
        <component :is="Component" @navigate="nav" />
      </router-view>
    </div>
    <router-view v-else v-slot="{ Component }">
      <component :is="Component" @navigate="nav" />
    </router-view>
  </div>
</template>

<style>
body {
  margin: 0;
  background: var(--vy-bg);
  overflow-x: hidden;
  font-family: var(--font-sans);
}

.app-layout {
  width: 100%;
  min-width: 0;
  min-height: 100vh;
  background: var(--vy-bg);
}

.app-layout.with-sidebar {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
}

.app-layout.with-sidebar > :not(.sidebar) {
  min-width: 0;
}

.app-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.app-topbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  padding: 12px 32px;
  border-bottom: 1px solid var(--vy-line-2);
  background: rgba(251, 248, 240, 0.85);
  backdrop-filter: blur(8px);
  position: sticky;
  top: 0;
  z-index: 20;
}

.avatar-button {
  border-radius: 50%;
  border: 0;
  padding: 0;
  background: transparent;
  cursor: pointer;
  line-height: 0;
}

.topbar-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 6px 8px 6px 6px;
  border: 1px solid var(--vy-line);
  border-radius: 14px;
  background: var(--vy-surface);
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
  min-width: 0;
}

.user-meta strong {
  font-size: 13px;
  font-weight: 800;
  color: var(--vy-ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}

.user-meta small {
  font-size: 11px;
  font-weight: 700;
  color: var(--vy-ink-3);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.logout-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  border: 0;
  background: transparent;
  color: var(--vy-danger);
  cursor: pointer;
  transition: background 0.15s;
}

.logout-button:hover {
  background: rgba(196, 69, 42, 0.12);
}

@media (max-width: 860px) {
  .app-topbar {
    padding: 10px 16px;
  }

  .user-meta {
    display: none;
  }

  .topbar-user {
    padding: 5px;
  }
}

@media (max-width: 860px) {
  .app-layout.with-sidebar {
    display: block;
  }

  .workspace {
    padding: 22px 14px 40px !important;
  }

  .page-header,
  .card-header,
  .table-header {
    align-items: stretch !important;
    flex-direction: column !important;
    gap: 12px !important;
  }

  .page-header h1 {
    font-size: clamp(24px, 8vw, 30px) !important;
    line-height: 1.05 !important;
  }

  .page-header p {
    font-size: 13px !important;
    line-height: 1.45 !important;
  }
}

@media (max-width: 480px) {
  .workspace {
    padding-left: 10px !important;
    padding-right: 10px !important;
  }
}

.swal2-container,
.swal2-popup,
.swal2-title,
.swal2-html-container,
.swal2-actions,
.swal2-confirm,
.swal2-cancel,
.swal2-deny,
.swal2-input,
.swal2-textarea,
.swal2-select {
  font-family: var(--font-sans) !important;
}

.swal2-popup,
.vy-swal-popup {
  border-radius: 8px !important;
  border: 1px solid var(--vy-line);
  box-shadow: var(--vy-shadow-lg);
  color: var(--vy-ink);
}

.swal2-title,
.vy-swal-title {
  font-family: var(--font-display) !important;
  color: var(--vy-ink) !important;
  font-weight: 900 !important;
  letter-spacing: -0.025em !important;
}

.swal2-html-container,
.vy-swal-text {
  color: var(--vy-ink-2) !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
}

.swal2-confirm,
.swal2-cancel,
.vy-swal-confirm,
.vy-swal-cancel {
  border-radius: 8px !important;
  padding: 11px 18px !important;
  font-weight: 900 !important;
  letter-spacing: 0 !important;
  box-shadow: none !important;
}

.swal2-confirm,
.vy-swal-confirm {
  background: var(--vy-orange) !important;
}

.swal2-confirm:hover,
.vy-swal-confirm:hover {
  background: var(--vy-orange-deep) !important;
}

.swal2-cancel,
.vy-swal-cancel {
  background: var(--vy-ink) !important;
}

.swal2-input,
.swal2-textarea,
.swal2-select {
  border: 1px solid var(--vy-line) !important;
  border-radius: 8px !important;
  color: var(--vy-ink) !important;
  box-shadow: none !important;
}

.swal2-input:focus,
.swal2-textarea:focus,
.swal2-select:focus {
  border-color: var(--vy-orange) !important;
  box-shadow: 0 0 0 3px rgba(242, 135, 5, 0.18) !important;
}

.swal2-confirm:focus,
.swal2-cancel:focus,
.vy-swal-confirm:focus,
.vy-swal-cancel:focus {
  box-shadow: 0 0 0 3px rgba(242, 135, 5, 0.22) !important;
}
</style>
