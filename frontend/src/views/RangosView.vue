<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { Pencil, Plus, RefreshCw, Save, Trash2, Trophy } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";

const loading = ref(false);
const saving = ref(false);
const error = ref("");
const rangos = ref([]);
const editingRangoId = ref(null);

const rangoForm = reactive({
  nombre: "",
  qpMinimo: 0
});

const orderedRangos = computed(() =>
  [...rangos.value].sort((left, right) => Number(left.qpMinimo || 0) - Number(right.qpMinimo || 0))
);

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

async function loadRangos() {
  loading.value = true;
  error.value = "";

  try {
    rangos.value = await apiRequest("/api/rangos");
  } catch (exception) {
    error.value = "No se pudieron cargar los rangos. Verifica que el backend este activo y la sesion sea valida.";
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  editingRangoId.value = null;
  Object.assign(rangoForm, {
    nombre: "",
    qpMinimo: 0
  });
}

function editRango(rango) {
  editingRangoId.value = rango.id;
  Object.assign(rangoForm, {
    nombre: rango.nombre || "",
    qpMinimo: Number(rango.qpMinimo || 0)
  });
}

async function saveRango() {
  saving.value = true;
  const isEditing = Boolean(editingRangoId.value);

  try {
    await apiRequest(isEditing ? `/api/rangos/${editingRangoId.value}` : "/api/rangos", {
      method: isEditing ? "PUT" : "POST",
      body: JSON.stringify({
        nombre: rangoForm.nombre,
        qpMinimo: Number(rangoForm.qpMinimo || 0)
      })
    });

    await Swal.fire({
      title: "Operacion exitosa",
      text: isEditing ? "Rango actualizado correctamente." : "Rango guardado correctamente.",
      icon: "success",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });

    resetForm();
    await loadRangos();
  } catch (exception) {
    await Swal.fire({
      title: "Revisa los datos",
      text: exception.message || "No se pudo guardar el rango.",
      icon: "error",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
  } finally {
    saving.value = false;
  }
}

async function deleteRango(rango) {
  const result = await Swal.fire({
    title: "Eliminar rango",
    text: `Se desactivara ${rango.nombre}.`,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Eliminar",
    cancelButtonText: "Cancelar",
    confirmButtonColor: "#C4452A",
    cancelButtonColor: "#1F1A14"
  });

  if (!result.isConfirmed) return;

  await apiRequest(`/api/rangos/${rango.id}`, { method: "DELETE" });
  if (editingRangoId.value === rango.id) {
    resetForm();
  }
  await loadRangos();
}

onMounted(loadRangos);
</script>

<template>
  <main class="rangos-page">
    <section class="rangos-header">
      <div>
        <span class="vy-chip vy-chip-orange"><Trophy :size="14" /> Configuracion</span>
        <h1>Rangos</h1>
        <p>Registra los rangos de carrera y el QP requerido para alcanzarlos.</p>
      </div>
      <button type="button" class="refresh-button" :disabled="loading" @click="loadRangos">
        <RefreshCw :size="16" /> Actualizar
      </button>
    </section>

    <p v-if="error" class="rangos-error">{{ error }}</p>

    <section class="rangos-layout">
      <form class="rangos-panel rango-form" @submit.prevent="saveRango">
        <h2>{{ editingRangoId ? "Editar rango" : "Nuevo rango" }}</h2>
        <label>
          Nombre del rango
          <input v-model.trim="rangoForm.nombre" type="text" maxlength="100" required placeholder="Ej. Plata" />
        </label>
        <label>
          QP a alcanzar
          <input v-model.number="rangoForm.qpMinimo" type="number" min="0" step="0.01" required />
        </label>
        <div class="form-actions">
          <button v-if="editingRangoId" type="button" class="ghost-button" @click="resetForm">Cancelar</button>
          <button type="submit" class="save-button" :disabled="saving">
            <Save v-if="editingRangoId" :size="16" />
            <Plus v-else :size="16" />
            {{ editingRangoId ? "Guardar cambios" : "Crear rango" }}
          </button>
        </div>
      </form>

      <section class="rangos-panel rangos-list">
        <header>
          <div>
            <h2>Rangos configurados</h2>
            <p>{{ orderedRangos.length }} rangos activos ordenados por QP.</p>
          </div>
        </header>

        <div class="rank-stack">
          <article v-for="(rango, index) in orderedRangos" :key="rango.id" class="rank-row">
            <span class="rank-position">{{ index + 1 }}</span>
            <div class="rank-main">
              <strong>{{ rango.nombre }}</strong>
              <small>Requiere {{ money(rango.qpMinimo) }} QP</small>
            </div>
            <div class="rank-actions">
              <button type="button" class="icon-action" title="Editar rango" @click="editRango(rango)">
                <Pencil :size="16" />
              </button>
              <button type="button" class="icon-action danger" title="Eliminar rango" @click="deleteRango(rango)">
                <Trash2 :size="16" />
              </button>
            </div>
          </article>

          <p v-if="!orderedRangos.length && !loading" class="empty-state">No hay rangos configurados.</p>
          <p v-if="loading" class="empty-state">Cargando rangos...</p>
        </div>
      </section>
    </section>
  </main>
</template>

<style scoped>
.rangos-page {
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.rangos-header,
.rangos-list header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.rangos-header h1 {
  margin-top: 10px;
  font-size: 30px;
  font-weight: 900;
}

.rangos-header p,
.rangos-list header p {
  margin-top: 6px;
  color: var(--vy-ink-2);
}

.refresh-button,
.save-button,
.ghost-button {
  min-height: 42px;
  padding: 0 16px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 900;
  transition: transform 0.16s ease, box-shadow 0.16s ease, background 0.16s ease;
}

.refresh-button,
.ghost-button {
  border: 1px solid rgba(242, 135, 5, 0.34);
  background: #fff;
  color: var(--vy-orange-deep);
}

.save-button {
  border: 1px solid var(--vy-orange);
  background: linear-gradient(135deg, var(--vy-orange), var(--vy-orange-deep));
  color: #fff;
  box-shadow: 0 12px 22px rgba(242, 135, 5, 0.22);
}

.refresh-button:hover:not(:disabled),
.ghost-button:hover,
.save-button:hover:not(:disabled) {
  transform: translateY(-1px);
}

.rangos-layout {
  display: grid;
  grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.rangos-panel {
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface);
  padding: 18px;
  box-shadow: var(--vy-shadow-sm);
}

.rangos-panel h2 {
  font-size: 18px;
  font-weight: 900;
  margin-bottom: 14px;
}

.rango-form label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
  color: var(--vy-ink-2);
  font-size: 13px;
  font-weight: 800;
}

.rango-form input {
  width: 100%;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  padding: 11px 12px;
  background: #fff;
  color: var(--vy-ink);
  font: inherit;
}

.form-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.rank-stack {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rank-row {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface-2);
}

.rank-position {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: var(--vy-cream);
  color: var(--vy-orange-deep);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 900;
}

.rank-main strong,
.rank-main small {
  display: block;
}

.rank-main strong {
  font-size: 15px;
  font-weight: 900;
}

.rank-main small,
.empty-state {
  margin-top: 4px;
  color: var(--vy-ink-3);
  font-size: 12px;
  font-weight: 800;
}

.rank-actions {
  display: inline-flex;
  gap: 8px;
}

.icon-action {
  width: 34px;
  height: 34px;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: #fff;
  color: var(--vy-ink-2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.icon-action:hover {
  background: var(--vy-ink);
  border-color: var(--vy-ink);
  color: #fff;
}

.icon-action.danger {
  color: var(--vy-danger);
}

.icon-action.danger:hover {
  background: var(--vy-danger);
  border-color: var(--vy-danger);
  color: #fff;
}

.rangos-error {
  border: 1px solid rgba(196, 69, 42, 0.25);
  border-radius: 8px;
  padding: 12px 14px;
  color: var(--vy-danger);
  background: rgba(196, 69, 42, 0.08);
  font-weight: 800;
}

@media (max-width: 900px) {
  .rangos-page {
    padding: 18px;
  }

  .rangos-header,
  .rangos-layout,
  .rangos-list header {
    grid-template-columns: 1fr;
    align-items: stretch;
    flex-direction: column;
  }

  .rank-row {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .rank-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
}
</style>
