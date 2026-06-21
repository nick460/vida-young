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
  font-family: var(--font-sans);
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

.swal2-container,
.swal2-popup,
.swal2-title,
.swal2-html-container,
.swal2-actions,
.swal2-confirm,
.swal2-cancel,
.swal2-deny,
.swal2-input,
.swal2-textarea,
.swal2-select {
  font-family: var(--font-sans) !important;
}

.swal2-popup,
.vy-swal-popup {
  border-radius: 8px !important;
  border: 1px solid var(--vy-line);
  box-shadow: var(--vy-shadow-lg);
  color: var(--vy-ink);
}

.swal2-title,
.vy-swal-title {
  font-family: var(--font-display) !important;
  color: var(--vy-ink) !important;
  font-weight: 900 !important;
  letter-spacing: -0.025em !important;
}

.swal2-html-container,
.vy-swal-text {
  color: var(--vy-ink-2) !important;
  font-size: 14px !important;
  line-height: 1.5 !important;
}

.swal2-confirm,
.swal2-cancel,
.vy-swal-confirm,
.vy-swal-cancel {
  border-radius: 8px !important;
  padding: 11px 18px !important;
  font-weight: 900 !important;
  letter-spacing: 0 !important;
  box-shadow: none !important;
}

.swal2-confirm,
.vy-swal-confirm {
  background: var(--vy-orange) !important;
}

.swal2-confirm:hover,
.vy-swal-confirm:hover {
  background: var(--vy-orange-deep) !important;
}

.swal2-cancel,
.vy-swal-cancel {
  background: var(--vy-ink) !important;
}

.swal2-input,
.swal2-textarea,
.swal2-select {
  border: 1px solid var(--vy-line) !important;
  border-radius: 8px !important;
  color: var(--vy-ink) !important;
  box-shadow: none !important;
}

.swal2-input:focus,
.swal2-textarea:focus,
.swal2-select:focus {
  border-color: var(--vy-orange) !important;
  box-shadow: 0 0 0 3px rgba(242, 135, 5, 0.18) !important;
}

.swal2-confirm:focus,
.swal2-cancel:focus,
.vy-swal-confirm:focus,
.vy-swal-cancel:focus {
  box-shadow: 0 0 0 3px rgba(242, 135, 5, 0.22) !important;
}
</style>
