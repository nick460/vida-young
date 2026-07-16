import { createApp } from "vue";
import { createPinia } from "pinia";
import { createRouter, createWebHistory } from "vue-router";
import App from "./App.vue";
import "./styles/brand.css";
import DashboardView from "./views/DashboardView.vue";
import LoginView from "./views/LoginView.vue";
import NetworkView from "./views/NetworkView.vue";
import PersonasView from "./views/PersonasView.vue";
import ProfileView from "./views/ProfileView.vue";
import RewardsView from "./views/RewardsView.vue";
import ShopView from "./views/ShopView.vue";
import StatsView from "./views/StatsView.vue";
import WalletView from "./views/WalletView.vue";
import CompanyWalletView from "./views/CompanyWalletView.vue";
import CartView from "./views/CartView.vue";
import RolesMenusView from "./views/RolesMenusView.vue";
import CustomMenuView from "./views/CustomMenuView.vue";
import InventoryView from "./views/InventoryView.vue";
import PlansView from "./views/PlansView.vue";
import RangosView from "./views/RangosView.vue";
import ActivationPlansView from "./views/ActivationPlansView.vue";
import ReferidosView from "./views/ReferidosView.vue";
import AdminSalesView from "./views/AdminSalesView.vue";
import RegistroReferidoView from "./views/RegistroReferidoView.vue";
import DigitalToolsView from "./views/DigitalToolsView.vue";
import DigitalLandingConfigView from "./views/DigitalLandingConfigView.vue";
import DigitalLandingView from "./views/DigitalLandingView.vue";
import CompanyHomeConfigView from "./views/CompanyHomeConfigView.vue";
import CompanyHomeView from "./views/CompanyHomeView.vue";
import ProductLandingConfigView from "./views/ProductLandingConfigView.vue";
import ProductLandingView from "./views/ProductLandingView.vue";
import PreinscripcionReferidoPublicView from "./views/PreinscripcionReferidoPublicView.vue";
import PublicStoreView from "./views/PublicStoreView.vue";
import PublicCartView from "./views/PublicCartView.vue";
import { ScreenLanding } from "./screens/publico.js";
import { ScreenAdmin } from "./screens/admin.js";
import { useAuthStore } from "./stores/authStore.js";
import { ROLE_ADMIN, canAccessMenu, getDefaultRouteName, hasAnyRole } from "./navigation/menuConfig.js";
import { useMenuStore } from "./stores/menuStore.js";

const GoogleVerificationView = {
  template: `<pre style="margin:0;padding:0;font:16px monospace;background:#fff;color:#000">google-site-verification: google31f2ae7a40d7a38f.html</pre>`
};

const routes = [
  { path: "/", name: "home-publica", component: CompanyHomeView, meta: { public: true } },
  { path: "/landing", name: "landing", component: ScreenLanding, meta: { public: true } },
  { path: "/producto/:productId/:ref?", name: "producto-landing", component: ProductLandingView, meta: { public: true } },
  { path: "/herramienta/:slug", name: "herramienta-landing", component: DigitalLandingView, meta: { public: true } },
  { path: "/referido/:username", name: "referido-publico", component: PreinscripcionReferidoPublicView, meta: { public: true } },
  { path: "/tienda/:username", name: "tienda-publica", component: PublicStoreView, meta: { public: true } },
  { path: "/tienda/:username/carrito", name: "tienda-publica-carrito", component: PublicCartView, meta: { public: true } },
  { path: "/preinscripcion-referido/:patrocinadorId", name: "preinscripcion-referido", component: PreinscripcionReferidoPublicView, meta: { public: true } },
  { path: "/login", name: "login", component: LoginView, meta: { public: true } },
  { path: "/dashboard", name: "dashboard", component: DashboardView, meta: { sidebar: true, roles: ["*"] } },
  { path: "/roles-menus", name: "roles-menus", component: RolesMenusView, meta: { sidebar: true, roles: [ROLE_ADMIN] } },
  { path: "/personas", name: "personas", component: PersonasView, meta: { sidebar: true, roles: [ROLE_ADMIN] } },
  { path: "/rangos", name: "rangos", component: RangosView, meta: { sidebar: true, roles: [ROLE_ADMIN] } },
  { path: "/planes", name: "planes", component: PlansView, meta: { sidebar: true, roles: [ROLE_ADMIN] } },
  { path: "/planes-activacion", name: "planes-activacion", component: ActivationPlansView, meta: { sidebar: true, roles: [ROLE_ADMIN] } },
  { path: "/referidos", name: "referidos", component: ReferidosView, meta: { sidebar: true, roles: [ROLE_ADMIN] } },
  { path: "/inventario", name: "inventario", component: InventoryView, meta: { sidebar: true, roles: [ROLE_ADMIN] } },
  { path: "/ventanilla", name: "ventanilla", component: AdminSalesView, meta: { sidebar: true, roles: [ROLE_ADMIN] } },
  { path: "/registro-referido", name: "registro-referido", component: RegistroReferidoView, meta: { sidebar: true, roles: [ROLE_ADMIN], parentMenu: "ventanilla" } },
  { path: "/herramientas-digitales", name: "herramientas-digitales", component: DigitalToolsView, meta: { sidebar: true, roles: [ROLE_ADMIN, "EMBAJADOR", "USUARIO"] } },
  { path: "/landings-productos-config", name: "landing-productos-config", component: ProductLandingConfigView, meta: { sidebar: true, roles: [ROLE_ADMIN], parentMenu: "herramientas-digitales" } },
  { path: "/landings-temas-config", name: "landing-temas-config", component: DigitalLandingConfigView, meta: { sidebar: true, roles: [ROLE_ADMIN], parentMenu: "herramientas-digitales" } },
  { path: "/pagina-principal-config", name: "pagina-principal-config", component: CompanyHomeConfigView, meta: { sidebar: true, roles: [ROLE_ADMIN] } },
  { path: "/caja-empresa", name: "caja-empresa", component: CompanyWalletView, meta: { sidebar: true, roles: [ROLE_ADMIN] } },
  { path: "/wallet", name: "wallet", component: WalletView, meta: { sidebar: true, roles: [ROLE_ADMIN, "EMBAJADOR", "USUARIO"] } },
  { path: "/shop", name: "shop", component: ShopView, meta: { sidebar: true, roles: [ROLE_ADMIN, "EMBAJADOR", "USUARIO", "CLIENTE"] } },
  { path: "/cart", name: "cart", component: CartView, meta: { sidebar: true, roles: ["*"], parentMenu: "shop" } },
  { path: "/network", name: "network", component: NetworkView, meta: { sidebar: true, roles: [ROLE_ADMIN, "EMBAJADOR"] } },
  { path: "/profile", name: "profile", component: ProfileView, meta: { sidebar: true, roles: ["*"] } },
  { path: "/rewards", name: "rewards", component: RewardsView, meta: { sidebar: true, roles: [ROLE_ADMIN, "EMBAJADOR"] } },
  { path: "/stats", name: "stats", component: StatsView, meta: { sidebar: true, roles: [ROLE_ADMIN] } },
  { path: "/admin", name: "admin", component: ScreenAdmin, meta: { roles: [ROLE_ADMIN] } },
  { path: "/google31f2ae7a40d7a38f.html", name: "google-verification", component: GoogleVerificationView, meta: { public: true } },
  { path: "/:pathMatch(.*)*", redirect: "/" }
];

const pinia = createPinia();

const router = createRouter({
  history: createWebHistory(),
  routes
});

const menuStore = useMenuStore(pinia);

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

menuStore.menuItems.forEach(registerCustomMenuRoute);

function defaultRouteOrAbort(auth, to) {
  const fallback = getDefaultRouteName(auth.usuario?.roles, menuStore.roleMenuPermissions, menuStore.menuItems);
  return fallback && fallback !== to.name ? { name: fallback } : false;
}

function canEnterRoute(auth, to) {
  const isMenuRoute = menuStore.menuItems.some((item) => item.id === to.name);

  if (isMenuRoute) {
    return canAccessMenu(auth.usuario?.roles, to.name, menuStore.roleMenuPermissions, menuStore.menuItems);
  }

  if (to.meta.parentMenu) {
    return canAccessMenu(auth.usuario?.roles, to.meta.parentMenu, menuStore.roleMenuPermissions, menuStore.menuItems)
      || hasAnyRole(auth.usuario?.roles, to.meta.roles);
  }

  return hasAnyRole(auth.usuario?.roles, to.meta.roles);
}

router.beforeEach((to) => {
  const auth = useAuthStore(pinia);

  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: "login", query: { redirect: to.fullPath } };
  }

  if (!to.meta.public) {
    return menuStore.loadFromBackend()
      .then(() => {
        menuStore.menuItems.forEach(registerCustomMenuRoute);

        const canEnter = canEnterRoute(auth, to);

        if (!canEnter) {
          return defaultRouteOrAbort(auth, to);
        }
      })
      .catch(() => {
        const canEnter = canEnterRoute(auth, to);

        if (!canEnter) {
          return defaultRouteOrAbort(auth, to);
        }
      });
  }

  const canEnter = canEnterRoute(auth, to);

  if (!to.meta.public && !canEnter) {
    return defaultRouteOrAbort(auth, to);
  }
});

createApp(App)
  .use(pinia)
  .use(router)
  .mount("#app");
