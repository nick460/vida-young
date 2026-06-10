<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import {
  CircleMinus,
  KeyRound,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  Shield,
  User
} from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import VyAutocompleteSelect from "../components/VyAutocompleteSelect.vue";
import { VyAvatar } from "../components/ui.js";

const loading = ref(false);
const error = ref("");
const personas = ref([]);
const usuarios = ref([]);
const roles = ref([]);
const openMenuId = ref(null);
const searchTerm = ref("");
const currentPage = ref(1);
const pageSize = ref(10);

const personaModalOpen = ref(false);
const usuarioModalOpen = ref(false);
const editingPersonaId = ref(null);
const editingUsuarioId = ref(null);
const selectedPersona = ref(null);

const personaForm = reactive({
  nombres: "",
  apellidos: "",
  documento: "",
  email: "",
  telefono: ""
});

const usuarioForm = reactive({
  username: "",
  password: "",
  rolIds: []
});

const peopleRows = computed(() => personas.value.map((persona) => {
  const usuario = usuarios.value.find((item) => item.persona?.id === persona.id);
  return { persona, usuario };
}));

const filteredPeopleRows = computed(() => {
  const query = normalize(searchTerm.value);

  if (!query) return peopleRows.value;

  return peopleRows.value.filter(({ persona, usuario }) => normalize([
    persona.nombres,
    persona.apellidos,
    persona.documento,
    persona.email,
    persona.telefono,
    usuario?.username,
    userRoles(usuario)
  ].join(" ")).includes(query));
});

const totalPages = computed(() => Math.max(1, Math.ceil(filteredPeopleRows.value.length / Number(pageSize.value || 10))));
const paginatedPeopleRows = computed(() => {
  const start = (currentPage.value - 1) * Number(pageSize.value || 10);
  return filteredPeopleRows.value.slice(start, start + Number(pageSize.value || 10));
});
const paginationStart = computed(() => filteredPeopleRows.value.length ? ((currentPage.value - 1) * Number(pageSize.value || 10)) + 1 : 0);
const paginationEnd = computed(() => Math.min(filteredPeopleRows.value.length, currentPage.value * Number(pageSize.value || 10)));

function navigate(name) {
  router.push({ name });
}

function toggleMenu(id) {
  openMenuId.value = openMenuId.value === id ? null : id;
}

function userRoles(usuario) {
  return usuario?.roles?.map((rol) => rol.nombre).join(", ") || "Sin usuario";
}

function normalize(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

function goToPage(page) {
  currentPage.value = Math.min(totalPages.value, Math.max(1, page));
  openMenuId.value = null;
}

async function loadAll() {
  loading.value = true;
  error.value = "";

  try {
    const [personasData, usuariosData, rolesData] = await Promise.all([
      apiRequest("/api/personas"),
      apiRequest("/api/usuarios"),
      apiRequest("/api/roles")
    ]);
    personas.value = personasData;
    usuarios.value = usuariosData;
    roles.value = rolesData;
  } catch (exception) {
    error.value = "No se pudieron cargar los datos. Verifica que el backend esté activo y la sesión sea válida.";
  } finally {
    loading.value = false;
  }
}

function resetPersonaForm() {
  Object.assign(personaForm, { nombres: "", apellidos: "", documento: "", email: "", telefono: "" });
  editingPersonaId.value = null;
}

function openPersonaModal(persona = null) {
  openMenuId.value = null;

  if (persona) {
    editingPersonaId.value = persona.id;
    Object.assign(personaForm, {
      nombres: persona.nombres || "",
      apellidos: persona.apellidos || "",
      documento: persona.documento || "",
      email: persona.email || "",
      telefono: persona.telefono || ""
    });
  } else {
    resetPersonaForm();
  }

  personaModalOpen.value = true;
}

function closePersonaModal() {
  personaModalOpen.value = false;
  resetPersonaForm();
}

async function savePersona() {
  const isEditing = Boolean(editingPersonaId.value);

  try {
    await apiRequest(isEditing ? `/api/personas/${editingPersonaId.value}` : "/api/personas", {
      method: isEditing ? "PUT" : "POST",
      body: JSON.stringify(personaForm)
    });
    await showSuccess(isEditing ? "Persona editada correctamente." : "Persona guardada correctamente.");
    closePersonaModal();
    await loadAll();
  } catch (exception) {
    await showError(exception.message || "No se pudo guardar la persona.");
  }
}

function resetUsuarioForm() {
  Object.assign(usuarioForm, { username: "", password: "", rolIds: [] });
  editingUsuarioId.value = null;
  selectedPersona.value = null;
}

function openUsuarioModal(persona) {
  openMenuId.value = null;
  selectedPersona.value = persona;

  const usuario = usuarios.value.find((item) => item.persona?.id === persona.id);
  if (usuario) {
    editingUsuarioId.value = usuario.id;
    Object.assign(usuarioForm, {
      username: usuario.username || "",
      password: "",
      rolIds: usuario.roles?.map((rol) => rol.id) || []
    });
  } else {
    Object.assign(usuarioForm, { username: "", password: "", rolIds: [] });
    editingUsuarioId.value = null;
  }

  usuarioModalOpen.value = true;
}

function closeUsuarioModal() {
  usuarioModalOpen.value = false;
  resetUsuarioForm();
}

async function saveUsuario() {
  const existingUser = usuarios.value.find((item) => item.id === editingUsuarioId.value);
  const isEditing = Boolean(editingUsuarioId.value);
  const payload = {
    username: usuarioForm.username,
    password: usuarioForm.password || existingUser?.password,
    activo: true,
    personaId: selectedPersona.value.id,
    rolIds: usuarioForm.rolIds.map((id) => Number(id))
  };

  try {
    await apiRequest(isEditing ? `/api/usuarios/${editingUsuarioId.value}` : "/api/usuarios", {
      method: isEditing ? "PUT" : "POST",
      body: JSON.stringify(payload)
    });
    await showSuccess(isEditing ? "Usuario editado correctamente." : "Usuario creado correctamente.");
    closeUsuarioModal();
    await loadAll();
  } catch (exception) {
    await showError(exception.message || "No se pudo guardar el usuario.");
  }
}

async function removePersona(persona) {
  openMenuId.value = null;

  const result = await Swal.fire({
    title: "Eliminar persona",
    text: "Se eliminará la persona seleccionada y su usuario asociado si existe.",
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Sí, eliminar",
    cancelButtonText: "Cancelar",
    reverseButtons: true,
    background: "#FFFFFF",
    color: "#1F1A14",
    confirmButtonColor: "#C4452A",
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

  if (!result.isConfirmed) {
    return;
  }

  await apiRequest(`/api/personas/${persona.id}`, { method: "DELETE" });
  await showSuccess("Persona eliminada correctamente.");
  await loadAll();
}

function showSuccess(text) {
  return Swal.fire({
    title: "Operacion exitosa",
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

onMounted(loadAll);

watch(searchTerm, () => {
  currentPage.value = 1;
  openMenuId.value = null;
});

watch(pageSize, () => {
  currentPage.value = 1;
  openMenuId.value = null;
});

watch(totalPages, (pages) => {
  if (currentPage.value > pages) {
    currentPage.value = pages;
  }
});
</script>

<template>
  <div class="vy people-view" @click="openMenuId = null">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Administración</div>
          <h1>Personas</h1>
          <p>Gestiona personas y su usuario de acceso desde una sola tabla.</p>
        </div>
        <div class="header-actions">
          <button class="vy-btn vy-btn-ghost" type="button" @click="loadAll">
            <RefreshCw :size="15" stroke-width="2" />
            Actualizar
          </button>
          <button class="vy-btn vy-btn-primary" type="button" @click="openPersonaModal()">
            <Plus :size="15" stroke-width="2.2" />
            Nueva persona
          </button>
        </div>
      </header>

      <section class="summary-grid">
        <article class="vy-card summary-card">
          <div class="summary-icon">
            <User :size="18" stroke-width="1.9" />
          </div>
          <span>Personas</span>
          <strong>{{ personas.length }}</strong>
        </article>
        <article class="vy-card summary-card">
          <div class="summary-icon">
            <KeyRound :size="18" stroke-width="1.9" />
          </div>
          <span>Con usuario</span>
          <strong>{{ usuarios.length }}</strong>
        </article>
        <article class="vy-card summary-card">
          <div class="summary-icon">
            <Shield :size="18" stroke-width="1.9" />
          </div>
          <span>Roles disponibles</span>
          <strong>{{ roles.length }}</strong>
        </article>
      </section>

      <div v-if="error" class="error-box">{{ error }}</div>
      <div v-if="loading" class="loading-box">Cargando datos...</div>

      <section v-else class="vy-card table-card">
        <div class="table-header">
          <div>
            <h2>Personas registradas</h2>
            <p>Crea, edita o elimina personas. Desde el menú de cada fila puedes administrar su usuario.</p>
          </div>
          <div class="table-tools">
            <label class="search-field">
              <Search :size="16" stroke-width="2" />
              <input v-model.trim="searchTerm" type="search" placeholder="Buscar persona, documento, usuario o rol" />
            </label>
            <select v-model.number="pageSize" class="page-size-select">
              <option :value="10">10 por pagina</option>
              <option :value="20">20 por pagina</option>
              <option :value="50">50 por pagina</option>
            </select>
          </div>
        </div>

        <div class="table-meta">
          <span>Mostrando {{ paginationStart }}-{{ paginationEnd }} de {{ filteredPeopleRows.length }}</span>
          <span v-if="searchTerm">Filtro activo</span>
        </div>

        <table v-if="paginatedPeopleRows.length">
          <thead>
            <tr>
              <th>Persona</th>
              <th>Documento</th>
              <th>Contacto</th>
              <th>Usuario</th>
              <th>Roles</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="{ persona, usuario } in paginatedPeopleRows" :key="persona.id">
              <td>
                <div class="person-cell">
                  <VyAvatar :name="`${persona.nombres?.[0] || ''}${persona.apellidos?.[0] || ''}`" :size="34" />
                  <span>
                    <strong>{{ persona.nombres }} {{ persona.apellidos }}</strong>
                    <small>ID {{ persona.id }}</small>
                  </span>
                </div>
              </td>
              <td>{{ persona.documento }}</td>
              <td>
                <span class="contact-cell">
                  <strong>{{ persona.email || "Sin email" }}</strong>
                  <small>{{ persona.telefono || "Sin teléfono" }}</small>
                </span>
              </td>
              <td>
                <span :class="['status-pill', usuario ? 'ok' : 'warn']">
                  {{ usuario?.username || "Sin usuario" }}
                </span>
              </td>
              <td>{{ userRoles(usuario) }}</td>
              <td class="actions-cell">
                <button class="menu-button" type="button" @click.stop="toggleMenu(persona.id)">
                  <span>Acciones</span>
                </button>
                <div v-if="openMenuId === persona.id" class="row-menu" @click.stop>
                  <button type="button" @click="openPersonaModal(persona)">
                    <Pencil :size="14" stroke-width="2" />
                    Editar persona
                  </button>
                  <button type="button" @click="openUsuarioModal(persona)">
                    <KeyRound :size="14" stroke-width="2" />
                    {{ usuario ? "Editar usuario" : "Crear usuario" }}
                  </button>
                  <button type="button" class="danger" @click="removePersona(persona)">
                    <CircleMinus :size="14" stroke-width="2" />
                    Eliminar
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-else class="empty-table">
          No se encontraron personas con ese criterio.
        </div>

        <div v-if="filteredPeopleRows.length" class="pagination-bar">
          <button type="button" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">
            Anterior
          </button>
          <span>Pagina {{ currentPage }} de {{ totalPages }}</span>
          <button type="button" :disabled="currentPage === totalPages" @click="goToPage(currentPage + 1)">
            Siguiente
          </button>
        </div>
      </section>
    </main>

    <Teleport to="body">
      <div v-if="personaModalOpen" class="modal-backdrop" @click.self="closePersonaModal">
        <form class="entity-modal" @submit.prevent="savePersona">
          <header>
            <div>
              <span class="vy-eyebrow">Personas</span>
              <h2>{{ editingPersonaId ? "Editar persona" : "Nueva persona" }}</h2>
            </div>
            <button type="button" aria-label="Cerrar" @click="closePersonaModal">×</button>
          </header>

          <div class="modal-grid">
            <label><span>Nombres</span><input v-model.trim="personaForm.nombres" required /></label>
            <label><span>Apellidos</span><input v-model.trim="personaForm.apellidos" required /></label>
            <label><span>Documento</span><input v-model.trim="personaForm.documento" required /></label>
            <label><span>Email</span><input v-model.trim="personaForm.email" type="email" /></label>
            <label><span>Teléfono</span><input v-model.trim="personaForm.telefono" /></label>
          </div>

          <footer>
            <button class="vy-btn vy-btn-ghost" type="button" @click="closePersonaModal">Cancelar</button>
            <button class="vy-btn vy-btn-primary" type="submit">
              {{ editingPersonaId ? "Guardar cambios" : "Crear persona" }}
            </button>
          </footer>
        </form>
      </div>

      <div v-if="usuarioModalOpen" class="modal-backdrop" @click.self="closeUsuarioModal">
        <form class="entity-modal" @submit.prevent="saveUsuario">
          <header>
            <div>
              <span class="vy-eyebrow">Usuario</span>
              <h2>{{ editingUsuarioId ? "Editar usuario" : "Crear usuario" }}</h2>
              <p v-if="selectedPersona">
                {{ selectedPersona.nombres }} {{ selectedPersona.apellidos }}
              </p>
            </div>
            <button type="button" aria-label="Cerrar" @click="closeUsuarioModal">×</button>
          </header>

          <div class="modal-grid">
            <label><span>Usuario</span><input v-model.trim="usuarioForm.username" required /></label>
            <label>
              <span>{{ editingUsuarioId ? "Nueva contraseña (opcional)" : "Contraseña" }}</span>
              <input v-model="usuarioForm.password" type="password" :required="!editingUsuarioId" />
            </label>
            <label class="full-field">
              <span>Roles</span>
              <VyAutocompleteSelect
                v-model="usuarioForm.rolIds"
                :options="roles"
                label-key="nombre"
                value-key="id"
                placeholder="Buscar y seleccionar roles"
                empty-text="No hay roles disponibles"
              />
            </label>
          </div>

          <footer>
            <button class="vy-btn vy-btn-ghost" type="button" @click="closeUsuarioModal">Cancelar</button>
            <button class="vy-btn vy-btn-primary" type="submit">
              {{ editingUsuarioId ? "Guardar usuario" : "Crear usuario" }}
            </button>
          </footer>
        </form>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.workspace {
  padding: 28px 32px 40px;
  min-width: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 30px;
  font-weight: 800;
  margin-top: 8px;
}

.page-header p,
.table-header p,
.entity-modal header p {
  font-size: 14px;
  color: var(--vy-ink-2);
  margin-top: 4px;
}

.header-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.header-actions .vy-btn {
  min-height: 40px;
  padding: 10px 16px;
  border-radius: 12px;
  gap: 8px;
  font-weight: 800;
  box-shadow: var(--vy-shadow-sm);
}

.header-actions .vy-btn-ghost {
  background: var(--vy-surface);
  border: 1px solid var(--vy-line);
  color: var(--vy-ink-2);
}

.header-actions .vy-btn-ghost:hover {
  background: var(--vy-surface-2);
  color: var(--vy-ink);
}

.header-actions .vy-btn-primary {
  background: var(--vy-orange);
  border: 1px solid var(--vy-orange);
  color: #fff;
  box-shadow: var(--vy-shadow-orange);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.summary-card {
  padding: 18px;
  position: relative;
  overflow: hidden;
}

.summary-card::after {
  content: "";
  position: absolute;
  right: -26px;
  top: -26px;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: rgba(242, 135, 5, 0.08);
}

.summary-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  background: var(--vy-cream);
  color: var(--vy-orange-deep);
}

.summary-card span {
  display: block;
  font-size: 12px;
  color: var(--vy-ink-3);
  font-weight: 700;
  text-transform: uppercase;
}

.summary-card strong {
  display: block;
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 800;
  margin-top: 6px;
  position: relative;
}

.error-box,
.loading-box {
  padding: 14px 16px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 14px;
}

.error-box {
  color: var(--vy-danger);
  background: rgba(196, 69, 42, 0.1);
}

.loading-box {
  color: var(--vy-ink-2);
  background: var(--vy-surface-2);
}

.table-card {
  padding: 18px;
  min-height: 540px;
  overflow: visible;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 18px;
  margin-bottom: 16px;
}

.table-header h2 {
  font-size: 16px;
  font-weight: 800;
}

.table-tools {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.search-field {
  width: min(360px, 100%);
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid var(--vy-line);
  border-radius: 12px;
  background: var(--vy-surface-2);
  color: var(--vy-ink-3);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.search-field input {
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  outline: 0;
}

.search-field input:focus {
  outline: 0;
  border-color: transparent;
}

.page-size-select {
  width: auto;
  min-height: 42px;
  border-radius: 12px;
  background: var(--vy-surface);
  font-weight: 800;
}

.table-meta,
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--vy-ink-3);
  font-size: 12px;
  font-weight: 800;
}

.table-meta {
  margin-bottom: 8px;
}

.pagination-bar {
  margin-top: 16px;
}

.pagination-bar button {
  min-height: 36px;
  padding: 0 13px;
  border: 1px solid var(--vy-line);
  border-radius: 10px;
  background: var(--vy-surface);
  color: var(--vy-ink-2);
  font-size: 12px;
  font-weight: 900;
}

.pagination-bar button:hover:not(:disabled) {
  background: var(--vy-orange);
  border-color: var(--vy-orange);
  color: #fff;
}

.pagination-bar button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.empty-table {
  padding: 28px 16px;
  border: 1px dashed var(--vy-line);
  border-radius: 12px;
  color: var(--vy-ink-3);
  background: var(--vy-surface-2);
  font-size: 13px;
  font-weight: 800;
  text-align: center;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

th {
  text-align: left;
  color: var(--vy-ink-3);
  font-size: 11px;
  text-transform: uppercase;
  padding: 10px 8px;
  white-space: nowrap;
}

td {
  padding: 13px 8px;
  border-top: 1px solid var(--vy-line-2);
  vertical-align: middle;
}

.person-cell,
.contact-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.person-cell span,
.contact-cell {
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.person-cell strong,
.contact-cell strong {
  font-weight: 800;
}

.person-cell small,
.contact-cell small {
  color: var(--vy-ink-3);
  font-size: 11px;
  font-weight: 600;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
}

.status-pill.ok {
  background: rgba(63, 143, 92, 0.12);
  color: var(--vy-success);
}

.status-pill.warn {
  background: var(--vy-cream);
  color: #6b4a12;
}

.actions-cell {
  position: relative;
  text-align: right;
  width: 52px;
}

.menu-button {
  height: 36px;
  min-width: 106px;
  padding: 0 10px 0 12px;
  border-radius: 999px;
  background: var(--vy-cream);
  color: var(--vy-ink-2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 800;
  border: 1px solid rgba(242, 135, 5, 0.18);
}

.menu-button:hover {
  background: var(--vy-orange);
  color: #fff;
  box-shadow: var(--vy-shadow-orange);
}

.row-menu {
  position: absolute;
  right: 8px;
  top: 48px;
  z-index: 20;
  min-width: 210px;
  padding: 6px;
  border-radius: 12px;
  background: var(--vy-surface);
  border: 1px solid var(--vy-line);
  box-shadow: var(--vy-shadow-lg);
}

.row-menu button {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 11px;
  border-radius: 9px;
  color: var(--vy-ink-2);
  font-size: 13px;
  font-weight: 700;
  text-align: left;
}

.row-menu button:hover {
  background: var(--vy-surface-2);
  color: var(--vy-ink);
}

.row-menu button svg {
  width: 28px;
  height: 28px;
  padding: 6px;
  border-radius: 8px;
  background: var(--vy-cream);
  color: var(--vy-orange-deep);
  flex-shrink: 0;
}

.row-menu .danger {
  color: var(--vy-danger);
}

.row-menu .danger svg {
  background: rgba(196, 69, 42, 0.1);
  color: var(--vy-danger);
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(31, 26, 20, 0.42);
  backdrop-filter: blur(5px);
}

.entity-modal {
  width: min(620px, 100%);
  background: var(--vy-surface);
  border: 1px solid var(--vy-line);
  border-radius: 18px;
  box-shadow: var(--vy-shadow-lg);
  padding: 22px;
  font-family: var(--font-sans);
  color: var(--vy-ink);
}

.entity-modal header,
.entity-modal footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.entity-modal header {
  padding-bottom: 18px;
  border-bottom: 1px solid var(--vy-line-2);
  margin-bottom: 18px;
}

.entity-modal h2 {
  font-size: 22px;
  font-weight: 800;
  margin-top: 4px;
}

.entity-modal header > button {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: var(--vy-surface-2);
  color: var(--vy-ink-3);
  font-size: 24px;
  line-height: 1;
}

.entity-modal .vy-btn {
  border-radius: 12px;
  font-weight: 800;
}

.entity-modal .vy-btn-primary {
  background: var(--vy-orange);
  color: #fff;
  box-shadow: var(--vy-shadow-orange);
}

.entity-modal .vy-btn-ghost {
  background: var(--vy-surface);
  border: 1px solid var(--vy-line);
  color: var(--vy-ink-2);
}

.modal-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.full-field {
  grid-column: 1 / -1;
}

label span {
  display: block;
  font-size: 11px;
  color: var(--vy-ink-3);
  font-weight: 800;
  text-transform: uppercase;
  margin-bottom: 6px;
}

input,
select {
  width: 100%;
  padding: 12px 13px;
  border-radius: 10px;
  border: 1px solid var(--vy-line);
  background: var(--vy-surface);
  color: var(--vy-ink);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
}

select[multiple] {
  min-height: 96px;
}

input:focus,
select:focus {
  outline: 2px solid rgba(242, 135, 5, 0.22);
  border-color: var(--vy-orange);
}

.entity-modal footer {
  justify-content: flex-end;
  padding-top: 18px;
  border-top: 1px solid var(--vy-line-2);
  margin-top: 18px;
}

@media (max-width: 1040px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .workspace {
    padding: 24px 20px 32px;
  }
}

@media (max-width: 720px) {
  .page-header,
  .table-header,
  .entity-modal header,
  .entity-modal footer {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions,
  .table-tools,
  .entity-modal footer .vy-btn {
    width: 100%;
  }

  .search-field,
  .page-size-select {
    width: 100%;
  }

  .table-meta,
  .pagination-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .modal-grid {
    grid-template-columns: 1fr;
  }

  table {
    min-width: 860px;
  }

  .table-card {
    overflow-x: auto;
  }
}
</style>
