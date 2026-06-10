<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";
import { PanelTop } from "lucide-vue-next";
import { useMenuStore } from "../stores/menuStore.js";

const route = useRoute();
const menuStore = useMenuStore();

const menu = computed(() =>
  menuStore.menuItems.find((item) => item.id === route.name || item.id === route.params.menuId)
);
</script>

<template>
  <div class="vy custom-menu-view">
    <main class="workspace">
      <section class="empty-state vy-card">
        <div class="empty-icon">
          <PanelTop :size="24" stroke-width="1.9" />
        </div>
        <div>
          <div class="vy-eyebrow">Menu personalizado</div>
          <h1>{{ menu?.label || "Menu" }}</h1>
          <p>Este modulo ya esta disponible en la navegacion y listo para conectar una pantalla especifica.</p>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace {
  min-width: 0;
  padding: 28px 32px 40px;
}

.empty-state {
  min-height: 420px;
  padding: 28px;
  display: flex;
  align-items: center;
  gap: 18px;
}

.empty-icon {
  width: 54px;
  height: 54px;
  border-radius: 16px;
  background: var(--vy-cream);
  color: var(--vy-orange-deep);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

h1 {
  margin-top: 8px;
  font-size: 30px;
  font-weight: 800;
}

p {
  margin-top: 6px;
  color: var(--vy-ink-2);
  font-size: 14px;
}
</style>
