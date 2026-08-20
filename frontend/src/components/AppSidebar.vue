<script setup>
import { ref, computed, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import * as LucideIcons from "lucide-vue-next";
import { useAuthStore } from "../stores/authStore.js";
import { getAllowedMenuItems } from "../navigation/menuConfig.js";
import { useMenuStore } from "../stores/menuStore.js";
import logoFull from "../assets/logoFull.png";
import logoMark from "../assets/logoMark.png";

const collapsed = ref(false);
const mobileOpen = ref(false);

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const menuStore = useMenuStore();

const mobileQuery = window.matchMedia("(max-width: 860px)");

function applyResponsive() {
  if (mobileQuery.matches) {
    collapsed.value = false;
  }
}

mobileQuery.addEventListener("change", applyResponsive);
applyResponsive();
const {
  ChevronLeft,
  ChevronRight,
  Home,
  Menu,
  X
} = LucideIcons;

watch(
  () => route.fullPath,
  () => {
    mobileOpen.value = false;
  }
);

function irA(id) {
  router.push({ name: id });
  mobileOpen.value = false;
}

const menuItems = computed(() =>
  getAllowedMenuItems(authStore.usuario?.roles, menuStore.roleMenuPermissions, menuStore.menuItems).map((item) => ({
    ...item,
    icon: LucideIcons[item.icon] || Home
  }))
);
</script>

<template>
  <div class="sidebar-wrap">
    <button
      class="mobile-menu-toggle"
      type="button"
      v-show="!mobileOpen"
      :aria-label="'Abrir menú'"
      @click="mobileOpen = true"
    >
      <Menu :size="18" stroke-width="2.2" />
    </button>

    <div v-if="mobileOpen" class="sidebar-backdrop" @click="mobileOpen = false"></div>

    <aside class="sidebar" :class="{ collapsed, 'is-open': mobileOpen }">
      <div class="sidebar-logo">
        <img v-if="!collapsed" :src="logoFull" alt="Vidayoung" class="logo-full" />
        <img v-else :src="logoMark" alt="Vidayoung" class="logo-mark" />
        <button type="button" class="toggle-btn" :title="collapsed ? 'Expandir' : 'Colapsar'" @click="collapsed = !collapsed">
          <ChevronLeft v-if="!collapsed" :size="14" stroke-width="2.5" />
          <ChevronRight v-else :size="14" stroke-width="2.5" />
        </button>
        <button type="button" class="drawer-close" aria-label="Cerrar menú" @click="mobileOpen = false">
          <X :size="16" stroke-width="2.2" />
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
          @click="irA(item.id)"
        >
          <span class="nav-icon"><component :is="item.icon" :size="18" stroke-width="1.8" /></span>
          <span v-if="!collapsed">{{ item.label }}</span>
          <small v-if="item.badge && !collapsed">{{ item.badge }}</small>
        </button>
      </nav>
    </aside>
  </div>
</template>

<style scoped>
.sidebar-wrap {
  position: relative;
}

.mobile-menu-toggle {
  display: none;
}

.drawer-close {
  display: none;
}

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

@media (max-width: 860px) {
  .mobile-menu-toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    position: fixed;
    top: 12px;
    left: 14px;
    z-index: 85;
    width: 38px;
    height: 38px;
    border-radius: 11px;
    background: var(--vy-surface);
    border: 1px solid var(--vy-line);
    color: var(--vy-ink-2);
    cursor: pointer;
    transition: background 0.15s, color 0.15s;
  }

  .mobile-menu-toggle:hover {
    background: var(--vy-ink);
    color: #fff;
    border-color: var(--vy-ink);
  }

  .drawer-close {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 30px;
    height: 30px;
    border-radius: 9px;
    background: var(--vy-surface-2);
    border: 1px solid var(--vy-line);
    color: var(--vy-ink-2);
    cursor: pointer;
    margin-left: auto;
    transition: background 0.15s, color 0.15s;
  }

  .drawer-close:hover {
    background: var(--vy-ink);
    color: #fff;
    border-color: var(--vy-ink);
  }

  .sidebar-backdrop {
    position: fixed;
    inset: 0;
    z-index: 79;
    background: rgba(31, 26, 20, 0.45);
    backdrop-filter: blur(2px);
  }

  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: 248px;
    height: 100vh;
    padding: 24px 14px;
    z-index: 80;
    transform: translateX(-100%);
    transition: transform 0.25s ease, box-shadow 0.25s ease;
    border-right: 1px solid var(--vy-line);
    overflow-y: auto;
    overflow-x: hidden;
  }

  .sidebar.is-open {
    transform: translateX(0);
    box-shadow: var(--vy-shadow-lg);
  }

  .sidebar.collapsed,
  .sidebar.collapsed.is-open {
    width: 248px;
    padding: 24px 14px;
  }

  .sidebar-logo,
  .sidebar-nav p {
    display: flex;
  }

  .toggle-btn {
    display: none;
  }

  .sidebar-nav {
    height: auto;
    flex-direction: column;
    gap: 2px;
    overflow: visible;
  }

  .sidebar-nav button,
  .sidebar.collapsed .sidebar-nav button {
    flex: none;
    width: 100%;
    height: auto;
    min-width: 0;
    padding: 8px 8px;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: flex-start;
    gap: 12px;
    border-radius: 10px;
    font-size: 13.5px;
    font-weight: 600;
    color: var(--vy-ink-2);
    transform: none;
  }

  .sidebar-nav button > span:nth-child(2),
  .sidebar.collapsed .sidebar-nav button > span:nth-child(2) {
    display: block;
    flex: 1;
    text-align: left;
    font-size: 13.5px;
    line-height: inherit;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .sidebar-nav button:hover,
  .sidebar-nav button.active,
  .sidebar.collapsed .sidebar-nav button:hover,
  .sidebar.collapsed .sidebar-nav button.active {
    background: var(--vy-ink);
    color: #fff;
    transform: translateX(2px);
  }

  .sidebar-nav button.active::after {
    display: none;
  }

  .nav-icon {
    width: 30px;
    height: 30px;
    border-radius: 9px;
    flex-shrink: 0;
  }

  .sidebar-nav small {
    display: block;
  }
}
</style>
