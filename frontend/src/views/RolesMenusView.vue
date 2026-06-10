<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import * as LucideIcons from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import {
  BASE_MENU_ITEMS,
  ROLE_ADMIN,
  mergeRoleMenuPermissions,
  normalizeMenuId,
  normalizeRole
} from "../navigation/menuConfig.js";
import { useMenuStore } from "../stores/menuStore.js";
import CustomMenuView from "./CustomMenuView.vue";

const {
  Check,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Search,
  Shield,
  Trash2
} = LucideIcons;

const router = useRouter();
const menuStore = useMenuStore();

const loading = ref(false);
const savingPermissions = ref(false);
const error = ref("");
const roles = ref([]);
const editingRoleId = ref(null);
const editingMenuId = ref(null);
const iconSearch = ref("");
const rolePermissions = ref({});

const roleForm = reactive({
  nombre: "",
  descripcion: ""
});

const menuForm = reactive({
  label: "",
  icon: "PanelTop"
});

const iconOptions = computed(() => {
  const query = iconSearch.value.trim().toLowerCase();
  const iconNames = Object.keys(LucideIcons).filter((name) => (
    /^[A-Z]/.test(name) &&
    typeof LucideIcons[name] === "function" &&
    !(name.endsWith("Icon") && LucideIcons[name.replace(/Icon$/, "")])
  ));

  return iconNames
    .filter((name) => !query || name.toLowerCase().includes(query))
    .sort((a, b) => a.localeCompare(b));
});

const visibleIconOptions = computed(() => iconOptions.value.slice(0, 140));
const editableMenus = computed(() => menuStore.menuItems.filter((item) => !item.roles.includes("*")));
const customMenus = computed(() => menuStore.customMenus);
const sortedRoles = computed(() => [...roles.value].sort((a, b) => a.nombre.localeCompare(b.nombre)));
const selectedRoleCount = computed(() => Object.keys(rolePermissions.value).length);
const assignedCells = computed(() =>
  Object.values(rolePermissions.value).reduce((count, menuIds) => count + menuIds.length, 0)
);

function registerCustomMenuRoute(menu) {
  if (!menu.custom || router.hasRoute(menu.id)) {
    return;
  }

  router.addRoute({
    path: `/${menu.id}`,
    name: menu.id,
    component: CustomMenuView,
    meta: { sidebar: true, customMenu: true }
  });
}

function resetRoleForm() {
  editingRoleId.value = null;
  Object.assign(roleForm, { nombre: "", descripcion: "" });
}

function resetMenuForm() {
  editingMenuId.value = null;
  Object.assign(menuForm, { label: "", icon: "PanelTop" });
}

function fillMissingRolePermissions() {
  const merged = mergeRoleMenuPermissions(menuStore.roleMenuPermissions);
  const adminMenus = editableMenus.value.map((item) => item.id);

  roles.value.forEach((role) => {
    const roleName = normalizeRole(role.nombre);
    merged[roleName] = roleName === ROLE_ADMIN ? adminMenus : merged[roleName] || [];
  });

  rolePermissions.value = merged;
}

async function loadRoles() {
  loading.value = true;
  error.value = "";

  try {
    await menuStore.loadFromBackend(true);
    menuStore.menuItems.forEach(registerCustomMenuRoute);
    roles.value = await apiRequest("/api/roles");
    fillMissingRolePermissions();
  } catch {
    error.value = "No se pudieron cargar los roles. Verifica que el backend este activo y la sesion sea valida.";
  } finally {
    loading.value = false;
  }
}

function editRole(role) {
  editingRoleId.value = role.id;
  Object.assign(roleForm, {
    nombre: role.nombre || "",
    descripcion: role.descripcion || ""
  });
}

async function saveRole() {
  const isEditing = Boolean(editingRoleId.value);
  const payload = {
    nombre: normalizeRole(roleForm.nombre),
    descripcion: roleForm.descripcion
  };

  await apiRequest(isEditing ? `/api/roles/${editingRoleId.value}` : "/api/roles", {
    method: isEditing ? "PUT" : "POST",
    body: JSON.stringify(payload)
  });

  resetRoleForm();
  await loadRoles();
}

async function removeRole(role) {
  const result = await Swal.fire({
    title: "Eliminar rol",
    text: `Se eliminara el rol ${role.nombre}.`,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Si, eliminar",
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

  await apiRequest(`/api/roles/${role.id}`, { method: "DELETE" });
  delete rolePermissions.value[normalizeRole(role.nombre)];
  await menuStore.saveRoleMenuPermissions(rolePermissions.value);
  await loadRoles();
}

function selectIcon(iconName) {
  menuForm.icon = iconName;
}

function editMenu(menu) {
  editingMenuId.value = menu.id;
  Object.assign(menuForm, {
    label: menu.label,
    icon: menu.icon
  });
}

async function saveMenu() {
  const label = menuForm.label.trim();
  const id = editingMenuId.value || normalizeMenuId(label);

  if (!label || !id) {
    return;
  }

  const baseIds = new Set(BASE_MENU_ITEMS.map((item) => item.id));
  if (baseIds.has(id)) {
    Swal.fire({
      title: "Menu reservado",
      text: "Ese nombre ya pertenece a un menu base del sistema.",
      icon: "warning",
      background: "#FFFFFF",
      color: "#1F1A14",
      confirmButtonColor: "#F28705"
    });
    return;
  }

  const menu = await menuStore.saveCustomMenu({ id, label, icon: menuForm.icon, roles: [ROLE_ADMIN], custom: true });
  registerCustomMenuRoute({ id: menu.menuId || id, label: menu.label || label, icon: menu.icon || menuForm.icon, roles: [ROLE_ADMIN], custom: true });
  rolePermissions.value = {
    ...rolePermissions.value,
    [ROLE_ADMIN]: [...new Set([...(rolePermissions.value[ROLE_ADMIN] || []), id])]
  };
  await menuStore.saveRoleMenuPermissions(rolePermissions.value);
  fillMissingRolePermissions();
  resetMenuForm();
}

async function removeMenu(menu) {
  await menuStore.deleteCustomMenu(menu.id);
  rolePermissions.value = Object.fromEntries(
    Object.entries(rolePermissions.value).map(([role, menuIds]) => [
      role,
      menuIds.filter((menuId) => menuId !== menu.id)
    ])
  );
  await menuStore.saveRoleMenuPermissions(rolePermissions.value);
  fillMissingRolePermissions();
}

function hasMenu(roleName, menuId) {
  const normalizedRole = normalizeRole(roleName);
  return rolePermissions.value[normalizedRole]?.includes(menuId) || false;
}

function toggleMenu(roleName, menuId) {
  const normalizedRole = normalizeRole(roleName);

  if (normalizedRole === ROLE_ADMIN) {
    return;
  }

  const currentMenus = new Set(rolePermissions.value[normalizedRole] || []);
  currentMenus.has(menuId) ? currentMenus.delete(menuId) : currentMenus.add(menuId);

  rolePermissions.value = {
    ...rolePermissions.value,
    [normalizedRole]: [...currentMenus]
  };
}

async function savePermissions() {
  savingPermissions.value = true;

  try {
    rolePermissions.value = {
      ...rolePermissions.value,
      [ROLE_ADMIN]: editableMenus.value.map((item) => item.id)
    };
    await menuStore.saveRoleMenuPermissions(rolePermissions.value);
    await Swal.fire({
      title: "Permisos guardados",
      text: "Los menus visibles por rol se actualizaron correctamente.",
      icon: "success",
      timer: 1400,
      showConfirmButton: false,
      background: "#FFFFFF",
      color: "#1F1A14",
      confirmButtonColor: "#F28705"
    });
  } finally {
    savingPermissions.value = false;
  }
}

onMounted(() => {
  loadRoles();
});
</script>

<template>
  <div class="vy roles-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Administracion</div>
          <h1>Roles y menus</h1>
          <p>Configura roles, crea menus y asigna acceso visual por rol.</p>
        </div>
        <div class="header-actions">
          <button class="vy-btn vy-btn-ghost" type="button" @click="loadRoles">
            <RefreshCw :size="15" stroke-width="2" />
            Actualizar
          </button>
          <button class="vy-btn vy-btn-primary" type="button" :disabled="savingPermissions" @click="savePermissions">
            <Save :size="15" stroke-width="2.2" />
            Guardar asignacion
          </button>
        </div>
      </header>

      <section class="summary-grid">
        <article class="vy-card summary-card">
          <div class="summary-icon"><Shield :size="18" stroke-width="1.9" /></div>
          <span>Roles</span>
          <strong>{{ roles.length }}</strong>
        </article>
        <article class="vy-card summary-card">
          <div class="summary-icon"><Check :size="18" stroke-width="1.9" /></div>
          <span>Menus</span>
          <strong>{{ editableMenus.length }}</strong>
        </article>
        <article class="vy-card summary-card">
          <div class="summary-icon"><Save :size="18" stroke-width="1.9" /></div>
          <span>Asignaciones</span>
          <strong>{{ assignedCells }}</strong>
        </article>
      </section>

      <div v-if="error" class="error-box">{{ error }}</div>
      <div v-if="loading" class="loading-box">Cargando roles...</div>

      <section v-else class="content-grid">
        <aside class="side-stack">
          <article class="vy-card panel-card">
            <div class="section-title">
              <h2>{{ editingRoleId ? "Editar rol" : "Nuevo rol" }}</h2>
              <p>Los nombres se normalizan en mayusculas.</p>
            </div>

            <form class="compact-form" @submit.prevent="saveRole">
              <label>
                <span>Nombre</span>
                <input v-model.trim="roleForm.nombre" placeholder="EMBAJADOR" required />
              </label>
              <label>
                <span>Descripcion</span>
                <textarea v-model.trim="roleForm.descripcion" rows="3" placeholder="Acceso operativo" />
              </label>
              <footer>
                <button v-if="editingRoleId" class="vy-btn vy-btn-ghost" type="button" @click="resetRoleForm">
                  Cancelar
                </button>
                <button class="vy-btn vy-btn-primary" type="submit">
                  <Save v-if="editingRoleId" :size="15" stroke-width="2.2" />
                  <Plus v-else :size="15" stroke-width="2.2" />
                  {{ editingRoleId ? "Guardar" : "Crear rol" }}
                </button>
              </footer>
            </form>

            <div class="roles-list">
              <button v-for="role in sortedRoles" :key="role.id" class="role-row" type="button" @click="editRole(role)">
                <span>
                  <strong>{{ role.nombre }}</strong>
                  <small>{{ role.descripcion || "Sin descripcion" }}</small>
                </span>
                <span class="row-actions">
                  <Pencil :size="15" stroke-width="2" />
                  <Trash2
                    v-if="normalizeRole(role.nombre) !== ROLE_ADMIN"
                    :size="15"
                    stroke-width="2"
                    @click.stop="removeRole(role)"
                  />
                </span>
              </button>
            </div>
          </article>

          <article class="vy-card panel-card">
            <div class="section-title">
              <h2>{{ editingMenuId ? "Editar menu" : "Nuevo menu" }}</h2>
              <p>El menu creado aparece en el sidebar y en la matriz.</p>
            </div>

            <form class="compact-form" @submit.prevent="saveMenu">
              <label>
                <span>Nombre del menu</span>
                <input v-model.trim="menuForm.label" placeholder="Reportes" required />
              </label>
              <div class="selected-icon">
                <component :is="LucideIcons[menuForm.icon]" :size="20" stroke-width="2" />
                <strong>{{ menuForm.icon }}</strong>
              </div>
              <label>
                <span>Buscar icono</span>
                <div class="search-field">
                  <Search :size="15" stroke-width="2" />
                  <input v-model.trim="iconSearch" placeholder="Search, Wallet, Users..." />
                </div>
              </label>
              <div class="icon-grid" aria-label="Iconos disponibles">
                <button
                  v-for="iconName in visibleIconOptions"
                  :key="iconName"
                  type="button"
                  :class="{ active: menuForm.icon === iconName }"
                  :title="iconName"
                  @click="selectIcon(iconName)"
                >
                  <component :is="LucideIcons[iconName]" :size="18" stroke-width="1.9" />
                  <span>{{ iconName }}</span>
                </button>
              </div>
              <small class="icon-count">
                Mostrando {{ visibleIconOptions.length }} de {{ iconOptions.length }} iconos disponibles.
              </small>
              <footer>
                <button v-if="editingMenuId" class="vy-btn vy-btn-ghost" type="button" @click="resetMenuForm">
                  Cancelar
                </button>
                <button class="vy-btn vy-btn-primary" type="submit">
                  <Plus :size="15" stroke-width="2.2" />
                  {{ editingMenuId ? "Guardar menu" : "Crear menu" }}
                </button>
              </footer>
            </form>

            <div v-if="customMenus.length" class="menus-list">
              <button v-for="menu in customMenus" :key="menu.id" class="menu-row" type="button" @click="editMenu(menu)">
                <span class="menu-row-icon">
                  <component :is="LucideIcons[menu.icon] || LucideIcons.PanelTop" :size="17" stroke-width="2" />
                </span>
                <span>
                  <strong>{{ menu.label }}</strong>
                  <small>{{ menu.id }}</small>
                </span>
                <Trash2 :size="15" stroke-width="2" @click.stop="removeMenu(menu)" />
              </button>
            </div>
          </article>
        </aside>

        <article class="vy-card assignment-card">
          <div class="section-title assignment-title">
            <div>
              <h2>Asignacion de menus</h2>
              <p>Selecciona un rol y activa los menus que debe ver en el sidebar.</p>
            </div>
          </div>

          <div class="role-permission-list">
            <section v-for="role in sortedRoles" :key="role.id" class="permission-block">
              <header>
                <span class="role-pill">{{ normalizeRole(role.nombre) }}</span>
                <small>{{ normalizeRole(role.nombre) === ROLE_ADMIN ? "Acceso completo" : "Acceso configurable" }}</small>
              </header>
              <div class="permission-grid">
                <button
                  v-for="menu in editableMenus"
                  :key="`${role.id}-${menu.id}`"
                  type="button"
                  class="permission-card"
                  :class="{ active: hasMenu(role.nombre, menu.id), locked: normalizeRole(role.nombre) === ROLE_ADMIN }"
                  @click="toggleMenu(role.nombre, menu.id)"
                >
                  <span class="permission-icon">
                    <component :is="LucideIcons[menu.icon] || LucideIcons.PanelTop" :size="18" stroke-width="1.9" />
                  </span>
                  <span>
                    <strong>{{ menu.label }}</strong>
                    <small>{{ menu.custom ? "Personalizado" : "Base" }}</small>
                  </span>
                  <Check v-if="hasMenu(role.nombre, menu.id)" class="permission-check" :size="16" stroke-width="2.5" />
                </button>
              </div>
            </section>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace {
  min-width: 0;
  padding: 28px 32px 40px;
}

.page-header,
.assignment-title {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 30px;
  font-weight: 800;
  margin-top: 8px;
}

.page-header p,
.section-title p {
  margin-top: 4px;
  color: var(--vy-ink-2);
  font-size: 14px;
}

.header-actions,
.compact-form footer {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.vy-btn {
  min-height: 40px;
  padding: 10px 16px;
  border-radius: 12px;
  font-weight: 800;
  transition: transform 0.16s ease, box-shadow 0.16s ease, background 0.16s ease, border-color 0.16s ease;
}

.vy-btn:hover:not(:disabled) {
  transform: translateY(-1px);
}

.vy-btn:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.vy-btn-primary {
  background: var(--vy-orange);
  border: 1px solid var(--vy-orange);
  color: #fff;
  box-shadow: var(--vy-shadow-orange);
}

.vy-btn-primary:hover:not(:disabled) {
  background: var(--vy-orange-deep);
  border-color: var(--vy-orange-deep);
}

.vy-btn-ghost {
  background: var(--vy-surface);
  border: 1px solid var(--vy-line);
  color: var(--vy-ink-2);
}

.vy-btn-ghost:hover:not(:disabled) {
  background: var(--vy-surface-2);
  border-color: rgba(242, 135, 5, 0.34);
  color: var(--vy-ink);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.summary-card,
.panel-card,
.assignment-card {
  padding: 18px;
}

.summary-icon,
.menu-row-icon,
.permission-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--vy-cream);
  color: var(--vy-orange-deep);
}

.summary-icon {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  margin-bottom: 12px;
}

.summary-card span {
  display: block;
  color: var(--vy-ink-3);
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
}

.summary-card strong {
  display: block;
  margin-top: 6px;
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 800;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(320px, 380px) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.side-stack {
  display: grid;
  gap: 18px;
}

.section-title {
  margin-bottom: 16px;
}

.section-title h2 {
  font-size: 16px;
  font-weight: 800;
}

.compact-form {
  display: grid;
  gap: 14px;
}

label span {
  display: block;
  margin-bottom: 6px;
  color: var(--vy-ink-3);
  font-size: 11px;
  font-weight: 800;
  text-transform: uppercase;
}

input,
textarea {
  width: 100%;
  padding: 12px 13px;
  border: 1px solid var(--vy-line);
  border-radius: 10px;
  background: var(--vy-surface);
  color: var(--vy-ink);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  resize: vertical;
}

input:focus,
textarea:focus {
  outline: 2px solid rgba(242, 135, 5, 0.22);
  border-color: var(--vy-orange);
}

.roles-list,
.menus-list {
  display: grid;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--vy-line-2);
}

.role-row,
.menu-row {
  width: 100%;
  min-height: 58px;
  padding: 10px 10px 10px 12px;
  border: 1px solid var(--vy-line);
  border-radius: 12px;
  background: var(--vy-surface-2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  text-align: left;
  box-shadow: 0 1px 0 rgba(31, 26, 20, 0.02);
  transition: transform 0.16s ease, border-color 0.16s ease, background 0.16s ease, box-shadow 0.16s ease;
}

.role-row:hover,
.menu-row:hover {
  border-color: rgba(242, 135, 5, 0.42);
  background: #fff;
  box-shadow: var(--vy-shadow-sm);
  transform: translateY(-1px);
}

.role-row:focus-visible,
.menu-row:focus-visible,
.icon-grid button:focus-visible,
.permission-card:focus-visible,
.vy-btn:focus-visible {
  outline: 3px solid rgba(242, 135, 5, 0.24);
  outline-offset: 2px;
}

.role-row strong,
.role-row small,
.menu-row strong,
.menu-row small {
  display: block;
}

.role-row strong,
.menu-row strong {
  font-size: 13px;
  font-weight: 900;
}

.role-row small,
.menu-row small {
  margin-top: 3px;
  color: var(--vy-ink-3);
  font-size: 11px;
  font-weight: 700;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--vy-ink-3);
  flex-shrink: 0;
}

.row-actions svg,
.menu-row > svg {
  width: 30px;
  height: 30px;
  padding: 7px;
  border-radius: 9px;
  background: #fff;
  border: 1px solid var(--vy-line);
  transition: background 0.16s ease, color 0.16s ease, border-color 0.16s ease, transform 0.16s ease;
}

.row-actions svg:first-child {
  color: var(--vy-orange-deep);
}

.row-actions svg:hover,
.menu-row > svg:hover {
  transform: translateY(-1px);
}

.row-actions svg:first-child:hover {
  background: rgba(242, 135, 5, 0.12);
  border-color: rgba(242, 135, 5, 0.28);
}

.row-actions svg:last-child:hover,
.menu-row > svg:hover {
  background: rgba(196, 69, 42, 0.1);
  border-color: rgba(196, 69, 42, 0.22);
  color: var(--vy-danger);
}

.menu-row-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  flex-shrink: 0;
}

.selected-icon {
  min-height: 44px;
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--vy-surface-2);
  border: 1px solid var(--vy-line);
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--vy-orange-deep);
}

.selected-icon strong {
  color: var(--vy-ink);
  font-size: 13px;
}

.search-field {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 12px;
  border: 1px solid var(--vy-line);
  border-radius: 10px;
  background: var(--vy-surface);
  color: var(--vy-ink-3);
}

.search-field input {
  border: 0;
  padding-left: 0;
}

.search-field input:focus {
  outline: 0;
}

.icon-grid {
  max-height: 246px;
  overflow: auto;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.icon-grid button {
  min-width: 0;
  height: 40px;
  padding: 0 10px;
  border: 1px solid var(--vy-line);
  border-radius: 10px;
  background: var(--vy-surface-2);
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--vy-ink-2);
  transition: transform 0.15s ease, background 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;
}

.icon-grid button:hover {
  border-color: rgba(242, 135, 5, 0.36);
  background: #fff;
  transform: translateY(-1px);
}

.icon-grid button.active {
  border-color: var(--vy-orange);
  background: var(--vy-orange);
  color: #fff;
  box-shadow: 0 8px 18px rgba(242, 135, 5, 0.22);
}

.icon-grid span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  font-weight: 800;
}

.icon-count {
  color: var(--vy-ink-3);
  font-size: 11px;
  font-weight: 700;
}

.role-permission-list {
  display: grid;
  gap: 16px;
}

.permission-block {
  padding: 14px;
  border: 1px solid var(--vy-line);
  border-radius: 14px;
  background: var(--vy-surface);
}

.permission-block header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.permission-block header small {
  color: var(--vy-ink-3);
  font-size: 12px;
  font-weight: 800;
}

.role-pill {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  border-radius: 999px;
  background: var(--vy-cream);
  color: #6b4a12;
  font-size: 12px;
  font-weight: 900;
}

.permission-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(168px, 1fr));
  gap: 10px;
}

.permission-card {
  min-height: 64px;
  padding: 10px;
  border: 1px solid var(--vy-line);
  border-radius: 12px;
  background: var(--vy-surface-2);
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  text-align: left;
  position: relative;
  overflow: hidden;
  box-shadow: 0 1px 0 rgba(31, 26, 20, 0.02);
  transition: transform 0.16s ease, border-color 0.16s ease, background 0.16s ease, box-shadow 0.16s ease;
}

.permission-card::before {
  content: "";
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  background: transparent;
  transition: background 0.16s ease;
}

.permission-card:hover {
  border-color: rgba(242, 135, 5, 0.34);
  background: #fff;
  box-shadow: var(--vy-shadow-sm);
  transform: translateY(-1px);
}

.permission-card.active {
  border-color: rgba(242, 135, 5, 0.52);
  background: linear-gradient(90deg, rgba(242, 135, 5, 0.14), rgba(255, 255, 255, 0.92));
  box-shadow: 0 8px 20px rgba(242, 135, 5, 0.12);
}

.permission-card.active::before {
  background: var(--vy-orange);
}

.permission-card.locked {
  cursor: default;
}

.permission-card.locked:hover {
  transform: none;
}

.permission-icon {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  transition: background 0.16s ease, color 0.16s ease;
}

.permission-card.active .permission-icon {
  background: var(--vy-orange);
  color: #fff;
}

.permission-card strong,
.permission-card small {
  display: block;
}

.permission-card strong {
  overflow: hidden;
  color: var(--vy-ink);
  font-size: 13px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.permission-card small {
  margin-top: 2px;
  color: var(--vy-ink-3);
  font-size: 11px;
  font-weight: 700;
}

.permission-check {
  width: 26px;
  height: 26px;
  padding: 5px;
  border-radius: 999px;
  color: #fff;
  background: var(--vy-orange);
  box-shadow: 0 6px 14px rgba(242, 135, 5, 0.24);
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

@media (max-width: 1180px) {
  .content-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .workspace {
    padding: 24px 20px 32px;
  }

  .page-header,
  .assignment-title {
    align-items: stretch;
    flex-direction: column;
  }

  .header-actions,
  .header-actions .vy-btn,
  .compact-form footer .vy-btn {
    width: 100%;
  }

  .permission-grid,
  .icon-grid {
    grid-template-columns: 1fr;
  }
}
</style>
