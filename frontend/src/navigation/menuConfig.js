export const ROLE_ADMIN = "ADMIN";
export const MENU_PERMISSIONS_STORAGE_KEY = "vy_role_menu_permissions";
export const CUSTOM_MENUS_STORAGE_KEY = "vy_custom_menus";

export const BASE_MENU_ITEMS = [
  { id: "dashboard", label: "Dashboard", icon: "Home", roles: ["*"] },
  { id: "roles-menus", label: "Roles y menus", icon: "Shield", roles: [ROLE_ADMIN] },
  { id: "personas", label: "Personas", icon: "User", roles: [ROLE_ADMIN] },
  { id: "rangos", label: "Rangos", icon: "Trophy", roles: [ROLE_ADMIN] },
  { id: "planes", label: "Planes", icon: "BadgePercent", roles: [ROLE_ADMIN] },
  { id: "planes-activacion", label: "Activaciones", icon: "Activity", roles: [ROLE_ADMIN] },
  { id: "referidos", label: "Referidos", icon: "Network", roles: [ROLE_ADMIN] },
  { id: "inventario", label: "Inventario", icon: "PackageSearch", roles: [ROLE_ADMIN] },
  { id: "ventanilla", label: "Ventanilla", icon: "Store", roles: [ROLE_ADMIN] },
  { id: "registro-referido", label: "Registro referido", icon: "UserPlus", roles: [ROLE_ADMIN] },
  { id: "herramientas-digitales", label: "Herramientas digitales", icon: "Wrench", roles: [ROLE_ADMIN, "EMBAJADOR", "USUARIO"] },
  { id: "wallet", label: "Finanzas", icon: "Wallet", roles: [ROLE_ADMIN, "EMBAJADOR", "USUARIO"] },
  { id: "shop", label: "Tienda", icon: "ShoppingBag", roles: [ROLE_ADMIN, "EMBAJADOR", "USUARIO", "CLIENTE"] },
  { id: "network", label: "Mi red", icon: "Users", badge: "138", roles: [ROLE_ADMIN, "EMBAJADOR"] },
  { id: "rewards", label: "Recompensas", icon: "Gift", badge: "3", roles: [ROLE_ADMIN, "EMBAJADOR"] },
  { id: "stats", label: "Estadisticas", icon: "BarChart3", roles: [ROLE_ADMIN] },
  { id: "profile", label: "Perfil", icon: "User", roles: ["*"] }
];

export const MENU_ITEMS = BASE_MENU_ITEMS;

export const DEFAULT_ROLE_MENU_PERMISSIONS = BASE_MENU_ITEMS.reduce((permissions, item) => {
  if (item.roles.includes("*")) {
    return permissions;
  }

  item.roles.forEach((role) => {
    const normalizedRole = normalizeRole(role);
    permissions[normalizedRole] = [...new Set([...(permissions[normalizedRole] || []), item.id])];
  });

  return permissions;
}, {});

export function normalizeRole(role) {
  const value = typeof role === "string" ? role : role?.nombre || role?.name || "";
  return value.replace(/^ROLE_/i, "").trim().toUpperCase();
}

export function normalizeRoles(roles = []) {
  return roles.map(normalizeRole).filter(Boolean);
}

export function hasAnyRole(userRoles = [], allowedRoles = []) {
  if (!allowedRoles.length || allowedRoles.includes("*")) {
    return true;
  }

  const normalizedUserRoles = normalizeRoles(userRoles);
  const normalizedAllowedRoles = normalizeRoles(allowedRoles);

  return normalizedUserRoles.some((role) => normalizedAllowedRoles.includes(role));
}

export function readStoredRoleMenuPermissions() {
  try {
    return JSON.parse(localStorage.getItem(MENU_PERMISSIONS_STORAGE_KEY) || "null") || {};
  } catch {
    return {};
  }
}

export function normalizeMenuId(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

export function readStoredCustomMenuItems() {
  try {
    const menus = JSON.parse(localStorage.getItem(CUSTOM_MENUS_STORAGE_KEY) || "[]");
    return Array.isArray(menus) ? menus.map(normalizeCustomMenu).filter(Boolean) : [];
  } catch {
    return [];
  }
}

export function normalizeCustomMenu(menu) {
  const label = String(menu?.label || "").trim();
  const id = normalizeMenuId(menu?.id || label);

  if (!id || !label) {
    return null;
  }

  return {
    id,
    label,
    icon: menu?.icon || "PanelTop",
    roles: Array.isArray(menu?.roles) && menu.roles.length ? menu.roles : [ROLE_ADMIN],
    custom: true
  };
}

export function getMenuItems(customMenus = readStoredCustomMenuItems()) {
  const normalizedCustomMenus = customMenus.map(normalizeCustomMenu).filter(Boolean);
  const baseIds = new Set(BASE_MENU_ITEMS.map((item) => item.id));
  return [
    ...BASE_MENU_ITEMS,
    ...normalizedCustomMenus.filter((item) => !baseIds.has(item.id))
  ];
}

export function mergeRoleMenuPermissions(permissions = {}) {
  return {
    ...DEFAULT_ROLE_MENU_PERMISSIONS,
    ...Object.fromEntries(
      Object.entries(permissions).map(([role, menuIds]) => [
        normalizeRole(role),
        Array.isArray(menuIds) ? menuIds : []
      ])
    )
  };
}

export function canAccessMenu(
  userRoles = [],
  menuId,
  permissions = readStoredRoleMenuPermissions(),
  menuItems = getMenuItems()
) {
  const item = menuItems.find((menuItem) => menuItem.id === menuId);

  if (!item) {
    return false;
  }

  if (item.roles.includes("*")) {
    return true;
  }

  const normalizedUserRoles = normalizeRoles(userRoles);
  const mergedPermissions = mergeRoleMenuPermissions(permissions);

  return normalizedUserRoles.some((role) => mergedPermissions[role]?.includes(menuId));
}

export function getAllowedMenuItems(
  userRoles = [],
  permissions = readStoredRoleMenuPermissions(),
  menuItems = getMenuItems()
) {
  return menuItems.filter((item) => canAccessMenu(userRoles, item.id, permissions, menuItems));
}

export function getDefaultRouteName(
  userRoles = [],
  permissions = readStoredRoleMenuPermissions(),
  menuItems = getMenuItems()
) {
  return getAllowedMenuItems(userRoles, permissions, menuItems)[0]?.id || "profile";
}
