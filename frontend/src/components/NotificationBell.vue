<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { VyIcon } from "./ui.js";
import { useNotificacionesStore } from "../stores/notificacionesStore.js";
import { useAuthStore } from "../stores/authStore.js";

const router = useRouter();
const notifStore = useNotificacionesStore();
const authStore = useAuthStore();
const open = ref(false);

const TIPO_CLASE = {
  INFO: "info",
  COMPRA: "compra",
  MEMBRESIA: "membresia",
  RECOMPENSA: "recompensa",
  RANGO: "rango",
  SISTEMA: "sistema"
};

const badgeVisible = computed(() => notifStore.noLeidas > 0);
const notificacionesHabilitadas = computed(() => authStore.fcmToken !== null);
const mensajeEstadoNotificaciones = computed(() => {
  if (!authStore.fcmToken) {
    return "Notificaciones no habilitadas";
  }
  if (authStore.permisoNotificaciones === "denied") {
    return "Notificaciones desactivadas en configuración";
  }
  return "Notificaciones habilitadas";
});

function alternar() {
  open.value = !open.value;
}

function cerrar() {
  open.value = false;
}

function irA(notificacion) {
  cerrar();
  if (!notificacion.leida) {
    notifStore.marcarLeida(notificacion.id).catch(() => {});
  }
  if (notificacion.link) {
    if (String(notificacion.link).startsWith("/")) {
      router.push(notificacion.link);
    } else {
      router.push({ name: notificacion.link });
    }
  }
}

function marcarTodas() {
  notifStore.marcarTodasLeidas().catch(() => {});
}

function formatearFecha(iso) {
  if (!iso) {
    return "";
  }
  const fecha = new Date(iso);
  const diff = Date.now() - fecha.getTime();
  const minutos = Math.floor(diff / 60000);
  if (minutos < 1) {
    return "ahora";
  }
  if (minutos < 60) {
    return `hace ${minutos} min`;
  }
  const horas = Math.floor(minutos / 60);
  if (horas < 24) {
    return `hace ${horas} h`;
  }
  const dias = Math.floor(horas / 24);
  if (dias < 7) {
    return `hace ${dias} d`;
  }
  return fecha.toLocaleDateString("es-PE", { day: "2-digit", month: "short" });
}

function clickFuera(event) {
  if (open.value && !event.target.closest(".vy-notif-bell")) {
    cerrar();
  }
}

onMounted(() => {
  notifStore.cargar();
  notifStore.conectar();
  authStore.inicializarNotificaciones();
  document.addEventListener("click", clickFuera);
});

onBeforeUnmount(() => {
  document.removeEventListener("click", clickFuera);
});
</script>

<template>
  <div class="vy-notif-bell">
    <button type="button" class="icon-button" :aria-label="mensajeEstadoNotificaciones" @click="alternar">
      <VyIcon name="bell" :size="16" />
      <span v-if="!notificacionesHabilitadas" class="vy-notif-badge-info">{{ mensajeEstadoNotificaciones }}</span>
      <span v-else-if="badgeVisible" class="vy-notif-badge">{{ notifStore.noLeidas > 99 ? "99+" : notifStore.noLeidas }}</span>
    </button>

    <transition name="vy-notif-fade">
      <div v-if="open" class="vy-notif-panel">
        <header class="vy-notif-header">
          <div>
            <strong>Notificaciones</strong>
            <small v-if="notificacionesHabilitadas">Sin leer: {{ notifStore.noLeidas }}</small>
            <small v-else>Habilite las notificaciones para recibir alertas</small>
          </div>
          <button v-if="notificacionesHabilitadas && notifStore.noLeidas > 0" type="button" class="vy-notif-mark-all" @click="marcarTodas">
            Marcar todas
          </button>
          <button v-else type="button" class="vy-notif-habilitar" @click="authStore.inicializarNotificaciones">
            Habilitar
          </button>
        </header>

        <div class="vy-notif-list">
          <p v-if="!notifStore.notificaciones.length" class="vy-notif-empty">
            No tienes notificaciones.
          </p>

          <button
            v-for="notificacion in notifStore.notificaciones"
            :key="notificacion.id"
            type="button"
            class="vy-notif-item"
            :class="{ unread: !notificacion.leida }"
            @click="irA(notificacion)"
          >
            <span class="vy-notif-dot" :class="TIPO_CLASE[notificacion.tipo] || 'info'"></span>
            <span class="vy-notif-body">
              <strong>{{ notificacion.titulo }}</strong>
              <span class="vy-notif-message">{{ notificacion.mensaje }}</span>
              <small>{{ formatearFecha(notificacion.fechaEnviado) }}</small>
            </span>
          </button>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.vy-notif-bell {
  position: relative;
  display: inline-flex;
}

.icon-button {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--vy-surface);
  border: 1px solid var(--vy-line);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--vy-ink-2);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.icon-button:hover {
  background: var(--vy-ink);
  color: #fff;
  border-color: var(--vy-ink);
}

.vy-notif-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 99px;
  background: var(--vy-orange);
  color: #fff;
  font-size: 10px;
  font-weight: 800;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--vy-surface);
  box-sizing: border-box;
  line-height: 1;
}

.vy-notif-panel {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: 360px;
  max-width: calc(100vw - 24px);
  background: #fff;
  border: 1px solid var(--vy-line);
  border-radius: 16px;
  box-shadow: var(--vy-shadow-lg);
  overflow: hidden;
  z-index: 80;
}

.vy-notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--vy-line-2);
  background: var(--vy-surface-2);
}

.vy-notif-header strong {
  display: block;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.vy-notif-header small {
  display: block;
  margin-top: 2px;
  font-size: 11px;
  font-weight: 700;
  color: var(--vy-orange-deep);
}

.vy-notif-mark-all {
  font-size: 12px;
  font-weight: 700;
  color: var(--vy-orange-deep);
  background: transparent;
  border: 0;
  padding: 6px 8px;
  border-radius: 8px;
  cursor: pointer;
}

.vy-notif-mark-all:hover {
  background: rgba(242, 135, 5, 0.1);
}

.vy-notif-list {
  max-height: 360px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.vy-notif-empty {
  padding: 28px 16px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--vy-ink-3);
}

.vy-notif-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 13px 16px;
  text-align: left;
  background: transparent;
  border: 0;
  border-bottom: 1px solid var(--vy-line-2);
  cursor: pointer;
  transition: background 0.15s;
}

.vy-notif-item:last-child {
  border-bottom: 0;
}

.vy-notif-item:hover {
  background: var(--vy-surface-2);
}

.vy-notif-item.unread {
  background: rgba(242, 135, 5, 0.05);
}

.vy-notif-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
  background: var(--vy-ink-3);
}

.vy-notif-dot.info {
  background: var(--vy-ink-3);
}

.vy-notif-dot.compra {
  background: var(--vy-orange);
}

.vy-notif-dot.membresia {
  background: #2563eb;
}

.vy-notif-dot.recompensa {
  background: #16a34a;
}

.vy-notif-dot.rango {
  background: #b45309;
}

.vy-notif-dot.sistema {
  background: #6b7280;
}

.vy-notif-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.vy-notif-body strong {
  font-size: 13px;
  font-weight: 800;
  color: var(--vy-ink);
}

.vy-notif-item.unread .vy-notif-body strong {
  color: var(--vy-orange-deep);
}

.vy-notif-message {
  font-size: 12px;
  line-height: 1.4;
  color: var(--vy-ink-2);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.vy-notif-body small {
  font-size: 10.5px;
  font-weight: 700;
  color: var(--vy-ink-3);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.vy-notif-fade-enter-active,
.vy-notif-fade-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}

.vy-notif-fade-enter-from,
.vy-notif-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 640px) {
  .vy-notif-bell {
    flex-shrink: 0;
  }

  .icon-button {
    flex-shrink: 0;
    width: 38px;
    height: 38px;
  }

  .vy-notif-badge {
    min-width: 15px;
    height: 15px;
    font-size: 9px;
    top: 5px;
    right: 5px;
  }

  .vy-notif-panel {
    position: fixed;
    top: 64px;
    left: 12px;
    right: 12px;
    width: auto;
    max-width: none;
    max-height: calc(100vh - 80px);
    overflow-x: hidden;
  }

  .vy-notif-list {
    max-height: calc(100vh - 150px);
  }
}
</style>