<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { RefreshCw, Send, UserPlus, Users } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import { notificacionService } from "../services/notificacionService.js";
import { useNotificacionesStore } from "../stores/notificacionesStore.js";

const store = useNotificacionesStore();

const loading = ref(false);
const error = ref("");
const enviando = ref(false);
const personas = ref([]);
const modoBroadcast = ref(false);
const personaQuery = ref("");
const personaListaAbierta = ref(false);
const selectedPersona = ref(null);

const TIPOS = [
  { value: "INFO", label: "Informacion" },
  { value: "COMPRA", label: "Compra" },
  { value: "MEMBRESIA", label: "Membresia" },
  { value: "RECOMPENSA", label: "Recompensa" },
  { value: "RANGO", label: "Rango" },
  { value: "SISTEMA", label: "Sistema" }
];

const ENLACES = [
  { value: "", label: "Sin enlace" },
  { value: "dashboard", label: "Dashboard" },
  { value: "wallet", label: "Billetera" },
  { value: "shop", label: "Tienda" },
  { value: "network", label: "Mi red" },
  { value: "rewards", label: "Recompensas" },
  { value: "profile", label: "Perfil" }
];

const TIPO_CLASE = {
  INFO: "info",
  COMPRA: "compra",
  MEMBRESIA: "membresia",
  RECOMPENSA: "recompensa",
  RANGO: "rango",
  SISTEMA: "sistema"
};

const formulario = reactive({
  tipo: "INFO",
  titulo: "",
  mensaje: "",
  link: ""
});

const filteredPersonas = computed(() => {
  const query = normalize(personaQuery.value);
  if (!query) {
    return personas.value.slice(0, 20);
  }
  return personas.value.filter((persona) => normalize([
    persona.nombres,
    persona.apellidos,
    persona.documento,
    persona.email,
    persona.telefono
  ].join(" ")).includes(query)).slice(0, 20);
});

function normalize(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

function personaLabel(persona) {
  return `${persona.nombres} ${persona.apellidos}`;
}

function elegirPersona(persona) {
  selectedPersona.value = persona;
  personaQuery.value = personaLabel(persona);
  personaListaAbierta.value = false;
}

function quitarPersona() {
  selectedPersona.value = null;
  personaQuery.value = "";
}

function alternarPersonaLista() {
  if (!selectedPersona.value) {
    personaListaAbierta.value = !personaListaAbierta.value;
  }
}

function clickFuera(event) {
  if (personaListaAbierta.value && !event.target.closest(".destinatario-picker")) {
    personaListaAbierta.value = false;
  }
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
  return fecha.toLocaleDateString("es-PE", { day: "2-digit", month: "short", year: "numeric" });
}

async function loadAll() {
  loading.value = true;
  error.value = "";
  try {
    const [personasData] = await Promise.all([
      apiRequest("/api/personas"),
      store.cargar()
    ]);
    personas.value = personasData;
  } catch (exception) {
    error.value = "No se pudieron cargar los datos. Verifica que el backend esté activo y la sesión sea válida.";
  } finally {
    loading.value = false;
  }
}

async function enviar() {
  const titulo = formulario.titulo.trim();
  const mensaje = formulario.mensaje.trim();

  if (!titulo || !mensaje) {
    await showError("El titulo y el mensaje son obligatorios.");
    return;
  }

  if (!modoBroadcast.value && !selectedPersona.value) {
    await showError("Selecciona una persona o activa el envio a todos.");
    return;
  }

  const payload = {
    destinatarioId: modoBroadcast.value ? null : selectedPersona.value.id,
    tipo: formulario.tipo,
    titulo,
    mensaje,
    link: formulario.link
  };

  enviando.value = true;
  try {
    await notificacionService.enviar(payload);
    await showSuccess(modoBroadcast.value
      ? "Notificacion enviada a todos los usuarios."
      : `Notificacion enviada a ${personaLabel(selectedPersona.value)}.`);
    quitarPersona();
    Object.assign(formulario, { tipo: "INFO", titulo: "", mensaje: "", link: "" });
    await store.cargar();
  } catch (exception) {
    await showError(exception.message || "No se pudo enviar la notificacion.");
  } finally {
    enviando.value = false;
  }
}

function showSuccess(text) {
  return Swal.fire({
    title: "Notificacion enviada",
    text,
    icon: "success",
    confirmButtonText: "Entendido",
    confirmButtonColor: "#F28705"
  });
}

function showError(text) {
  return Swal.fire({
    title: "Revisa los datos",
    text,
    icon: "error",
    confirmButtonText: "Entendido",
    confirmButtonColor: "#F28705"
  });
}

watch(modoBroadcast, (activado) => {
  if (activado) {
    quitarPersona();
  }
});

onMounted(() => {
  document.addEventListener("click", clickFuera);
  loadAll();
});
</script>

<template>
  <div class="vy notificaciones-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Administración</div>
          <h1>Notificaciones</h1>
          <p>Envía notificaciones a una persona en particular o a todos los usuarios.</p>
        </div>
        <div class="header-actions">
          <button class="vy-btn vy-btn-ghost" type="button" @click="loadAll">
            <RefreshCw :size="15" stroke-width="2" />
            Actualizar
          </button>
        </div>
      </header>

      <div v-if="error" class="error-box">{{ error }}</div>
      <div v-if="loading" class="loading-box">Cargando datos...</div>

      <section class="layout-grid">
        <form class="vy-card send-card" @submit.prevent="enviar">
          <div class="card-heading">
            <h2>Nueva notificación</h2>
            <p>Completa el mensaje y elige quién lo recibirá.</p>
          </div>

          <div class="destinatario-section">
            <p class="field-label">Destinatario</p>
            <div class="segmented">
              <button type="button" :class="{ active: !modoBroadcast }" @click="modoBroadcast = false">
                <UserPlus :size="15" stroke-width="2" />
                Una persona
              </button>
              <button type="button" :class="{ active: modoBroadcast }" @click="modoBroadcast = true">
                <Users :size="15" stroke-width="2" />
                Enviar a todos
              </button>
            </div>

            <div v-if="!modoBroadcast" class="destinatario-picker">
              <label class="field">
                <input
                  v-model="personaQuery"
                  type="search"
                  placeholder="Buscar persona por nombre, documento o email..."
                  :disabled="Boolean(selectedPersona)"
                  @focus="alternarPersonaLista"
                  @input="personaListaAbierta = true"
                />
              </label>

              <div v-if="selectedPersona" class="selected-chip">
                <span>{{ personaLabel(selectedPersona) }}</span>
                <small>ID {{ selectedPersona.id }} · {{ selectedPersona.documento }}</small>
                <button type="button" class="chip-remove" @click="quitarPersona" aria-label="Quitar persona">×</button>
              </div>

              <div v-if="personaListaAbierta && !selectedPersona" class="persona-dropdown">
                <p v-if="!filteredPersonas.length" class="persona-empty">Sin resultados.</p>
                <button
                  v-for="persona in filteredPersonas"
                  :key="persona.id"
                  type="button"
                  class="persona-option"
                  @click="elegirPersona(persona)"
                >
                  <strong>{{ personaLabel(persona) }}</strong>
                  <small>{{ persona.documento }} · {{ persona.email || "Sin email" }}</small>
                </button>
              </div>
            </div>

            <p v-else class="broadcast-note">
              La notificación se enviará a todos los usuarios y aparecerá como no leída para cada uno.
            </p>
          </div>

          <div class="form-row">
            <label class="field">
              <span>Tipo</span>
              <select v-model="formulario.tipo">
                <option v-for="tipo in TIPOS" :key="tipo.value" :value="tipo.value">{{ tipo.label }}</option>
              </select>
            </label>
            <label class="field">
              <span>Enlace al abrir</span>
              <select v-model="formulario.link">
                <option v-for="enlace in ENLACES" :key="enlace.value" :value="enlace.value">{{ enlace.label }}</option>
              </select>
            </label>
          </div>

          <label class="field">
            <span>Título</span>
            <input v-model.trim="formulario.titulo" type="text" maxlength="120" placeholder="Ej. Nuevo beneficio disponible" />
          </label>

          <label class="field">
            <span>Mensaje</span>
            <textarea v-model.trim="formulario.mensaje" rows="4" maxlength="500" placeholder="Escribe el detalle de la notificación..."></textarea>
          </label>

          <button class="vy-btn vy-btn-primary send-button" type="submit" :disabled="enviando">
            <Send :size="15" stroke-width="2.2" />
            {{ enviando ? "Enviando..." : "Enviar notificación" }}
          </button>
        </form>

        <section class="vy-card list-card">
          <div class="card-heading">
            <h2>Recientes</h2>
            <p>Últimas notificaciones enviadas (incluye las que te llegaron a ti).</p>
          </div>

          <div class="recent-list">
            <p v-if="!store.notificaciones.length" class="recent-empty">Aún no hay notificaciones.</p>

            <article v-for="notificacion in store.notificaciones" :key="notificacion.id" class="recent-item">
              <span class="recent-dot" :class="TIPO_CLASE[notificacion.tipo] || 'info'"></span>
              <div class="recent-body">
                <div class="recent-top">
                  <strong>{{ notificacion.titulo }}</strong>
                  <small>{{ formatearFecha(notificacion.fechaEnviado) }}</small>
                </div>
                <p>{{ notificacion.mensaje }}</p>
                <small class="recent-target">
                  {{ notificacion.destinatario
                    ? `Para: ${notificacion.destinatario.nombres} ${notificacion.destinatario.apellidos || ""}`
                    : "Para todos los usuarios" }}
                </small>
              </div>
            </article>
          </div>
        </section>
      </section>
    </main>
  </div>
</template>

<style scoped>
.notificaciones-view .workspace {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.layout-grid {
  display: grid;
  grid-template-columns: minmax(0, 5fr) minmax(0, 6fr);
  gap: 20px;
  align-items: start;
}

@media (max-width: 980px) {
  .layout-grid {
    grid-template-columns: 1fr;
  }
}

.send-card,
.list-card {
  padding: 22px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.card-heading h2 {
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.card-heading p {
  margin-top: 4px;
  font-size: 13px;
  color: var(--vy-ink-3);
}

.destinatario-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--vy-ink-2);
}

.segmented {
  display: flex;
  gap: 6px;
  background: var(--vy-surface-2);
  border: 1px solid var(--vy-line);
  border-radius: 12px;
  padding: 4px;
}

.segmented button {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 9px 12px;
  border-radius: 9px;
  border: 0;
  background: transparent;
  font-size: 13px;
  font-weight: 700;
  color: var(--vy-ink-3);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.segmented button.active {
  background: var(--vy-ink);
  color: #fff;
}

.destinatario-picker {
  position: relative;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field > span {
  font-size: 12px;
  font-weight: 700;
  color: var(--vy-ink-2);
}

.field input,
.field select,
.field textarea {
  width: 100%;
  padding: 11px 13px;
  border-radius: 11px;
  border: 1px solid var(--vy-line);
  background: var(--vy-surface);
  color: var(--vy-ink);
  font-size: 14px;
  font-family: inherit;
  box-sizing: border-box;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.field textarea {
  resize: vertical;
  min-height: 96px;
}

.field input:focus,
.field select:focus,
.field textarea:focus {
  outline: none;
  border-color: var(--vy-orange);
  box-shadow: 0 0 0 3px rgba(242, 135, 5, 0.14);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

@media (max-width: 560px) {
  .form-row {
    grid-template-columns: 1fr;
  }
}

.selected-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 11px;
  background: rgba(242, 135, 5, 0.09);
  border: 1px solid rgba(242, 135, 5, 0.28);
}

.selected-chip span {
  font-weight: 700;
  font-size: 13.5px;
}

.selected-chip small {
  font-size: 12px;
  color: var(--vy-ink-3);
}

.chip-remove {
  margin-left: auto;
  width: 22px;
  height: 22px;
  border-radius: 7px;
  border: 0;
  background: rgba(242, 135, 5, 0.16);
  color: var(--vy-orange-deep);
  font-size: 15px;
  line-height: 1;
  cursor: pointer;
}

.persona-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  max-height: 260px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid var(--vy-line);
  border-radius: 12px;
  box-shadow: var(--vy-shadow-lg);
  z-index: 60;
  padding: 6px;
}

.persona-option {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 10px 12px;
  border-radius: 9px;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.persona-option:hover {
  background: var(--vy-surface-2);
}

.persona-option strong {
  font-size: 13.5px;
  color: var(--vy-ink);
}

.persona-option small {
  font-size: 12px;
  color: var(--vy-ink-3);
}

.persona-empty {
  padding: 14px;
  text-align: center;
  font-size: 13px;
  color: var(--vy-ink-3);
}

.broadcast-note {
  font-size: 12.5px;
  line-height: 1.5;
  color: var(--vy-ink-3);
  background: var(--vy-surface-2);
  border: 1px dashed var(--vy-line);
  border-radius: 11px;
  padding: 10px 12px;
}

.send-button {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.send-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 520px;
  overflow-y: auto;
}

.recent-empty {
  padding: 24px 12px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: var(--vy-ink-3);
}

.recent-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 13px 14px;
  border: 1px solid var(--vy-line-2);
  border-radius: 12px;
  background: var(--vy-surface);
}

.recent-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
  background: var(--vy-ink-3);
}

.recent-dot.info { background: var(--vy-ink-3); }
.recent-dot.compra { background: var(--vy-orange); }
.recent-dot.membresia { background: #2563eb; }
.recent-dot.recompensa { background: #16a34a; }
.recent-dot.rango { background: #b45309; }
.recent-dot.sistema { background: #6b7280; }

.recent-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.recent-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.recent-top strong {
  font-size: 13.5px;
  font-weight: 800;
  color: var(--vy-ink);
}

.recent-top small {
  font-size: 11px;
  font-weight: 700;
  color: var(--vy-ink-3);
  white-space: nowrap;
}

.recent-body p {
  font-size: 13px;
  line-height: 1.45;
  color: var(--vy-ink-2);
}

.recent-target {
  font-size: 11.5px;
  font-weight: 700;
  color: var(--vy-orange-deep);
}
</style>