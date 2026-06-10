import { defineStore } from "pinia";
import {
  BASE_MENU_ITEMS,
  CUSTOM_MENUS_STORAGE_KEY,
  MENU_PERMISSIONS_STORAGE_KEY,
  getMenuItems,
  mergeRoleMenuPermissions,
  normalizeCustomMenu,
  readStoredCustomMenuItems,
  readStoredRoleMenuPermissions
} from "../navigation/menuConfig.js";
import { apiRequest } from "../services/api.js";

export const useMenuStore = defineStore("menu", {
  state: () => ({
    loadedFromBackend: false,
    backendMenus: [],
    customMenus: readStoredCustomMenuItems(),
    roleMenuPermissions: mergeRoleMenuPermissions(readStoredRoleMenuPermissions())
  }),
  getters: {
    menuItems: (state) => state.backendMenus.length ? state.backendMenus : getMenuItems(state.customMenus)
  },
  actions: {
    applyBackendConfig(config) {
      const menus = Array.isArray(config?.menus) ? config.menus : [];
      const baseMenusById = new Map(BASE_MENU_ITEMS.map((menu) => [menu.id, menu]));
      this.backendMenus = menus.map((menu) => ({
        id: menu.id,
        label: menu.label,
        icon: menu.icon || "PanelTop",
        roles: baseMenusById.get(menu.id)?.roles || (Array.isArray(menu.roles) && menu.roles.length ? menu.roles : []),
        custom: Boolean(menu.custom)
      }));
      this.customMenus = this.backendMenus.filter((menu) => menu.custom);
      this.roleMenuPermissions = mergeRoleMenuPermissions(config?.permissions || {});
      this.loadedFromBackend = true;
    },
    async loadFromBackend(force = false) {
      if (this.loadedFromBackend && !force) {
        return;
      }

      const config = await apiRequest("/api/menus/config");
      this.applyBackendConfig(config);
    },
    setRoleMenuPermissions(permissions) {
      this.roleMenuPermissions = mergeRoleMenuPermissions(permissions);
      localStorage.setItem(MENU_PERMISSIONS_STORAGE_KEY, JSON.stringify(this.roleMenuPermissions));
    },
    async saveRoleMenuPermissions(permissions) {
      await apiRequest("/api/menus/permissions", {
        method: "PUT",
        body: JSON.stringify({ permissions })
      });
      await this.loadFromBackend(true);
    },
    setCustomMenus(menus) {
      this.customMenus = menus.map(normalizeCustomMenu).filter(Boolean);
      localStorage.setItem(CUSTOM_MENUS_STORAGE_KEY, JSON.stringify(this.customMenus));
    },
    async saveCustomMenu(menu) {
      const normalized = normalizeCustomMenu(menu);
      const saved = await apiRequest(normalized.id === menu.id ? `/api/menus/${normalized.id}` : "/api/menus", {
        method: normalized.id === menu.id ? "PUT" : "POST",
        body: JSON.stringify(normalized)
      });
      await this.loadFromBackend(true);
      return saved;
    },
    async deleteCustomMenu(menuId) {
      await apiRequest(`/api/menus/${menuId}`, { method: "DELETE" });
      await this.loadFromBackend(true);
    }
  }
});
