<script setup>
import { ref, computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import * as LucideIcons from "lucide-vue-next";
import { useAuthStore } from "../stores/authStore.js";
import { getAllowedMenuItems } from "../navigation/menuConfig.js";
import { useMenuStore } from "../stores/menuStore.js";
import { VyAvatar } from "./ui.js";
import logoFull from "../assets/logoFull.png";
import logoMark from "../assets/logoMark.png";

const collapsed = ref(false);

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const menuStore = useMenuStore();
const {
  BarChart3,
  ChevronLeft,
  ChevronRight,
  Gift,
  Home,
  LogOut,
  Shield,
  ShoppingBag,
  User,
  Users,
  Wallet
} = LucideIcons;

const user = computed(() => ({
  name: authStore.usuario?.username || "Administrador Vidayoung",
  avatar: (authStore.usuario?.username || "AV").slice(0, 2).toUpperCase()
}));

const staticMenuItems = [
  { id: "dashboard", label: "Dashboard", icon: Home },
  { id: "personas", label: "Personas", icon: User },
  { id: "wallet", label: "Billetera", icon: Wallet },
  { id: "shop", label: "Tienda", icon: ShoppingBag },
  { id: "network", label: "Mi red", icon: Users, badge: "138" },
  { id: "rewards", label: "Recompensas", icon: Gift, badge: "3" },
  { id: "stats", label: "Estadísticas", icon: BarChart3 },
  { id: "profile", label: "Perfil", icon: User }
];

const iconMap = {
  BarChart3,
  Gift,
  Home,
  Shield,
  ShoppingBag,
  User,
  Users,
  Wallet
};

const menuItems = computed(() =>
  getAllowedMenuItems(authStore.usuario?.roles, menuStore.roleMenuPermissions, menuStore.menuItems).map((item) => ({
    ...item,
    icon: LucideIcons[item.icon] || iconMap[item.icon] || Home
  }))
);

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
</script>

<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="sidebar-logo">
      <img v-if="!collapsed" :src="logoFull" alt="Vidayoung" class="logo-full" />
      <img v-else :src="logoMark" alt="Vidayoung" class="logo-mark" />
      <button type="button" class="toggle-btn" :title="collapsed ? 'Expandir' : 'Colapsar'" @click="collapsed = !collapsed">
        <ChevronLeft v-if="!collapsed" :size="14" stroke-width="2.5" />
        <ChevronRight v-else :size="14" stroke-width="2.5" />
      </button>
    </div>

    <nav class="sidebar-nav" aria-label="Principal">
      <p v-if="!collapsed">General</p>
      <button
        v-for="item in menuItems"
        :key="item.id"
        type="button"
        :class="{ active: item.id === route.name }"
        :title="collapsed ? item.label : undefined"
        @click="router.push({ name: item.id })"
      >
        <span class="nav-icon"><component :is="item.icon" :size="18" stroke-width="1.8" /></span>
        <span v-if="!collapsed">{{ item.label }}</span>
        <small v-if="item.badge && !collapsed">{{ item.badge }}</small>
      </button>
    </nav>

    <div class="sidebar-footer" aria-label="Cuenta">
      <div class="user-footer-card" :title="collapsed ? user.name : undefined">
        <VyAvatar :name="user.avatar" :size="32" bg="var(--vy-orange)" color="#fff" />
        <span v-if="!collapsed">
          <strong>{{ user.name }}</strong>
        </span>
      </div>
      <button type="button" class="logout-item" :title="collapsed ? 'Cerrar sesión' : undefined" @click="logout">
        <span class="nav-icon"><LogOut :size="18" stroke-width="1.8" /></span>
        <span v-if="!collapsed">Cerrar sesión</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  background: var(--vy-surface);
  border-right: 1px solid var(--vy-line);
  padding: 24px 14px;
  display: flex;
  flex-direction: column;
  gap: 22px;
  position: sticky;
  top: 0;
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: none;
  -ms-overflow-style: none;
  width: 248px;
  box-sizing: border-box;
  transition: width 0.22s ease, padding 0.22s ease;
}

.sidebar::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.sidebar.collapsed {
  width: 72px;
  padding: 24px 10px;
}

/* ---- Logo ---- */
.sidebar-logo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 36px;
  padding: 0 4px;
}

.logo-full {
  height: 32px;
  width: auto;
  max-width: 150px;
  object-fit: contain;
  flex: 1;
  min-width: 0;
}

.logo-mark {
  height: 32px;
  width: auto;
  object-fit: contain;
}

.toggle-btn {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: var(--vy-surface-2);
  border: 1px solid var(--vy-line);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--vy-ink-3);
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.toggle-btn:hover {
  background: var(--vy-ink);
  color: #fff;
  border-color: var(--vy-ink);
}

.sidebar.collapsed .sidebar-logo {
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

/* ---- Nav ---- */
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sidebar-nav p {
  font-size: 10px;
  color: var(--vy-ink-3);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  padding: 4px 10px;
  margin-bottom: 6px;
  font-weight: 700;
  white-space: nowrap;
}

.sidebar-nav button {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 8px;
  border-radius: 10px;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--vy-ink-2);
  transition: background 0.16s ease, color 0.16s ease, transform 0.16s ease;
  width: 100%;
}

.sidebar.collapsed .sidebar-nav button {
  justify-content: center;
  gap: 0;
  padding: 8px;
}

.sidebar-nav button:hover,
.sidebar-nav button.active {
  background: var(--vy-ink);
  color: #fff;
  transform: translateX(2px);
}

.sidebar.collapsed .sidebar-nav button:hover,
.sidebar.collapsed .sidebar-nav button.active {
  transform: none;
}

.nav-icon {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  color: var(--vy-ink-3);
  background: var(--vy-surface-2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.sidebar-nav button:hover .nav-icon,
.sidebar-nav button.active .nav-icon {
  color: var(--vy-orange);
  background: rgba(242, 135, 5, 0.14);
}

.sidebar-nav button > span:nth-child(2) {
  flex: 1;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
}

.sidebar-nav small {
  padding: 1px 8px;
  border-radius: 99px;
  font-size: 11px;
  font-weight: 700;
  background: var(--vy-cream);
  color: #6b4a12;
  white-space: nowrap;
}

/* ---- Footer ---- */
.sidebar-footer {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--vy-line-2);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sidebar-footer .user-footer-card {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 10px;
  background: var(--vy-surface-2);
  border: 1px solid var(--vy-line);
  border-radius: 12px;
  box-sizing: border-box;
}

.sidebar.collapsed .user-footer-card {
  justify-content: center;
  gap: 0;
  padding: 8px;
}

.user-footer-card span {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.user-footer-card strong {
  font-size: 13px;
  font-weight: 800;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-footer .logout-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 8px;
  border-radius: 10px;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--vy-danger);
  transition: background 0.16s ease, color 0.16s ease, transform 0.16s ease;
}

.sidebar.collapsed .logout-item {
  justify-content: center;
  gap: 0;
  padding: 9px 8px;
}

.sidebar-footer .logout-item:hover {
  background: rgba(196, 69, 42, 0.1);
  color: var(--vy-danger);
  transform: translateX(2px);
}

.sidebar.collapsed .logout-item:hover {
  transform: none;
}

.sidebar-footer .logout-item:hover .nav-icon {
  color: var(--vy-danger);
}

@media (max-width: 860px) {
  .sidebar {
    position: fixed;
    inset: auto 10px 10px 10px;
    z-index: 70;
    width: auto;
    height: 68px;
    padding: 8px;
    border: 1px solid rgba(233, 226, 210, 0.92);
    border-radius: 22px;
    background: rgba(255, 255, 255, 0.94);
    box-shadow: 0 18px 44px rgba(31, 26, 20, 0.18);
    backdrop-filter: blur(14px);
    display: block;
    overflow: hidden;
    border-bottom: 1px solid rgba(242, 135, 5, 0.18);
  }

  .sidebar.collapsed {
    width: auto;
    padding: 8px;
  }

  .sidebar-logo,
  .sidebar-footer,
  .sidebar-nav p {
    display: none;
  }

  .sidebar-nav {
    height: 100%;
    display: flex;
    flex-direction: row;
    align-items: center;
    gap: 6px;
    padding: 0 2px;
    overflow-x: auto;
    overflow-y: hidden;
    scrollbar-width: none;
    scroll-snap-type: x proximity;
  }

  .sidebar-nav::-webkit-scrollbar {
    display: none;
  }

  .sidebar-nav button,
  .sidebar.collapsed .sidebar-nav button {
    flex: 0 0 64px;
    width: 64px;
    min-width: 64px;
    height: 52px;
    padding: 6px 4px;
    border-radius: 16px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 4px;
    scroll-snap-align: center;
    font-size: 10px;
    font-weight: 800;
    color: var(--vy-ink-3);
    transform: none;
    position: relative;
  }

  .sidebar-nav button:hover,
  .sidebar-nav button.active,
  .sidebar.collapsed .sidebar-nav button:hover,
  .sidebar.collapsed .sidebar-nav button.active {
    background: linear-gradient(180deg, var(--vy-ink), #34281d);
    color: #fff;
    transform: none;
  }

  .sidebar-nav button.active::after {
    content: "";
    position: absolute;
    bottom: 4px;
    left: 50%;
    width: 18px;
    height: 3px;
    border-radius: 999px;
    background: var(--vy-orange);
    transform: translateX(-50%);
  }

  .nav-icon {
    width: 28px;
    height: 28px;
    border-radius: 10px;
  }

  .sidebar-nav button > span:nth-child(2) {
    display: block;
    width: 100%;
    flex: 0;
    text-align: center;
    font-size: 10px;
    line-height: 1.05;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .sidebar-nav small {
    display: none;
  }
}

@media (max-width: 420px) {
  .sidebar {
    inset: auto 8px 8px 8px;
    height: 64px;
    border-radius: 20px;
  }

  .sidebar-nav button,
  .sidebar.collapsed .sidebar-nav button {
    flex-basis: 58px;
    width: 58px;
    min-width: 58px;
  }

  .sidebar-nav button > span:nth-child(2) {
    font-size: 9px;
  }
}
</style>
