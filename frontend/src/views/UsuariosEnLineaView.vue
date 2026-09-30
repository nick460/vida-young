<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { RefreshCw, Search, Users, Wifi } from "lucide-vue-next";
import { contarEnLinea, obtenerEnLinea } from "../services/presenciaService.js";

const loading = ref(false);
const error = ref("");
const usuarios = ref([]);
const total = ref(0);
const query = ref("");
const ventanaMinutos = ref(2);
const ultimaActualizacion = ref(null);

let intervalo = null;

const filtrados = computed(() => {
  const term = query.value.trim().toLowerCase();
  if (!term) return usuarios.value;
  return usuarios.value.filter((u) =>
    `${u.nombres || ""} ${u.apellidos || ""} ${u.username || ""} ${(u.roles || []).join(" ")}`.toLowerCase().includes(term)
  );
});

function haceCuanto(segundos) {
  const s = Number(segundos || 0);
  if (s < 5) return "ahora mismo";
  if (s < 60) return `hace ${s} seg`;
  const m = Math.floor(s / 60);
  if (m < 2) return "hace 1 min";
  return `hace ${m} min`;
}

function formatoFecha(value) {
  if (!value) return "-";
  return new Date(value).toLocaleString("es-BO", {
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    day: "2-digit",
    month: "short"
  });
}

async function cargar() {
  loading.value = true;
  error.value = "";
  try {
    const [lista, conteo] = await Promise.all([
      obtenerEnLinea(ventanaMinutos.value),
      contarEnLinea(ventanaMinutos.value)
    ]);
    usuarios.value = Array.isArray(lista) ? lista : [];
    total.value = Number(conteo?.online ?? usuarios.value.length);
    ultimaActualizacion.value = new Date();
  } catch (e) {
    error.value = "No se pudo cargar los usuarios en línea. Verifica tu sesión de administrador.";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  cargar();
  intervalo = setInterval(cargar, 15000);
});

onUnmounted(() => {
  if (intervalo) clearInterval(intervalo);
});
</script>

<template>
  <main class="online-page">
    <section class="online-header">
      <div>
        <span class="vy-chip vy-chip-green"><Wifi :size="14" /> Tiempo real</span>
        <h1>Usuarios en línea</h1>
        <p>{{ total }} usuario(s) activos en los últimos {{ ventanaMinutos }} min · se actualiza cada 15 seg.</p>
      </div>
      <div class="header-actions">
        <label class="ventana">
          Ventana
          <select v-model.number="ventanaMinutos" @change="cargar">
            <option :value="1">1 min</option>
            <option :value="2">2 min</option>
            <option :value="5">5 min</option>
            <option :value="10">10 min</option>
          </select>
        </label>
        <button type="button" class="refresh-button" :disabled="loading" @click="cargar">
          <RefreshCw :size="16" /> Actualizar
        </button>
      </div>
    </section>

    <p v-if="error" class="online-error">{{ error }}</p>

    <section class="online-panel">
      <div class="toolbar">
        <div class="search-box">
          <Search :size="16" />
          <input v-model.trim="query" type="text" placeholder="Buscar por nombre, usuario o rol..." />
        </div>
        <small v-if="ultimaActualizacion">Última actualización: {{ formatoFecha(ultimaActualizacion) }}</small>
      </div>

      <div v-if="loading && !usuarios.length" class="empty-state">Cargando usuarios...</div>
      <div v-else-if="!filtrados.length" class="empty-state">
        <Users :size="22" />
        <p>Nadie en línea en este momento.</p>
      </div>

      <div v-else class="table-wrap">
        <table class="online-table">
          <thead>
            <tr>
              <th>Usuario</th>
              <th>Roles</th>
              <th>Estado</th>
              <th>Último latido</th>
              <th>Página</th>
              <th>IP</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in filtrados" :key="u.personaId">
              <td>
                <strong>{{ u.nombres }} {{ u.apellidos }}</strong>
                <small>@{{ u.username }}</small>
              </td>
              <td>
                <span v-for="rol in (u.roles || [])" :key="rol" class="role-chip">{{ rol }}</span>
                <span v-if="!(u.roles || []).length" class="muted">-</span>
              </td>
              <td><span class="dot"></span> {{ haceCuanto(u.segundosInactivo) }}</td>
              <td class="muted">{{ formatoFecha(u.ultimoLatido) }}</td>
              <td class="muted">{{ u.rutaActual || "-" }}</td>
              <td class="muted">{{ u.ip || "-" }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </main>
</template>

<style scoped>
.online-page {
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.online-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}
.online-header h1 {
  margin-top: 10px;
  font-size: 30px;
  font-weight: 900;
}
.online-header p {
  margin-top: 6px;
  color: var(--vy-ink-2);
}
.header-actions {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}
.ventana {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  font-weight: 800;
  color: var(--vy-ink-2);
}
.ventana select {
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  padding: 10px 12px;
  background: #fff;
  font: inherit;
}
.refresh-button {
  min-height: 42px;
  padding: 0 16px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 900;
  border: 1px solid rgba(242, 135, 5, 0.34);
  background: #fff;
  color: var(--vy-orange-deep);
  cursor: pointer;
}
.online-error {
  border: 1px solid rgba(196, 69, 42, 0.25);
  border-radius: 8px;
  padding: 12px 14px;
  color: var(--vy-danger);
  background: rgba(196, 69, 42, 0.08);
  font-weight: 800;
}
.online-panel {
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface);
  padding: 18px;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  padding: 0 12px;
  background: #fff;
  min-width: 280px;
}
.search-box input {
  border: 0;
  outline: 0;
  padding: 11px 0;
  width: 100%;
  font: inherit;
}
.table-wrap {
  overflow-x: auto;
}
.online-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.online-table th,
.online-table td {
  text-align: left;
  padding: 10px 12px;
  border-bottom: 1px solid var(--vy-line);
  vertical-align: top;
}
.online-table strong {
  display: block;
  font-weight: 900;
}
.online-table small {
  color: var(--vy-ink-3);
}
.role-chip {
  display: inline-block;
  margin: 0 4px 4px 0;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--vy-cream);
  color: var(--vy-orange-deep);
  font-size: 11px;
  font-weight: 900;
}
.dot {
  display: inline-block;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #22c55e;
  margin-right: 6px;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.18);
}
.muted {
  color: var(--vy-ink-3);
}
.empty-state {
  text-align: center;
  color: var(--vy-ink-3);
  padding: 28px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
</style>
