<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import AppSidebar from "./components/AppSidebar.vue";

const route = useRoute();
const router = useRouter();

const showSidebar = computed(() => route.meta.sidebar === true);

function nav(id) {
  router.push({ name: id });
  window.scrollTo({ top: 0, behavior: "instant" });
}
</script>

<template>
  <div class="vy app-layout" :class="{ 'with-sidebar': showSidebar }">
    <AppSidebar v-if="showSidebar" />
    <router-view v-slot="{ Component }">
      <component :is="Component" @navigate="nav" />
    </router-view>
  </div>
</template>

<style>
body {
  margin: 0;
  background: var(--vy-bg);
  overflow-x: hidden;
}

.app-layout {
  width: 100%;
  min-width: 0;
  min-height: 100vh;
  background: var(--vy-bg);
}

.app-layout.with-sidebar {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
}

.app-layout.with-sidebar > :not(.sidebar) {
  min-width: 0;
}

@media (max-width: 860px) {
  .app-layout.with-sidebar {
    display: block;
    padding-bottom: 92px;
  }

  .workspace {
    padding: 22px 14px 108px !important;
  }

  .page-header,
  .card-header,
  .table-header {
    align-items: stretch !important;
    flex-direction: column !important;
    gap: 12px !important;
  }

  .page-header h1 {
    font-size: clamp(24px, 8vw, 30px) !important;
    line-height: 1.05 !important;
  }

  .page-header p {
    font-size: 13px !important;
    line-height: 1.45 !important;
  }
}

@media (max-width: 480px) {
  .workspace {
    padding-left: 10px !important;
    padding-right: 10px !important;
  }
}

.vy-swal-popup {
  border-radius: 18px;
  border: 1px solid var(--vy-line);
  box-shadow: var(--vy-shadow-lg);
  font-family: var(--font-sans);
}

.vy-swal-title {
  font-family: var(--font-display);
  font-weight: 800;
  letter-spacing: 0;
}

.vy-swal-text {
  color: var(--vy-ink-2);
  font-size: 14px;
}

.vy-swal-confirm,
.vy-swal-cancel {
  border-radius: 999px !important;
  padding: 11px 18px !important;
  font-weight: 800 !important;
  box-shadow: none !important;
}

.vy-swal-confirm:focus,
.vy-swal-cancel:focus {
  box-shadow: 0 0 0 3px rgba(242, 135, 5, 0.22) !important;
}
</style>
