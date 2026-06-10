<script setup>
import { MoreHorizontal } from "lucide-vue-next";
import { VyAvatar } from "./ui.js";

defineOptions({
  name: "NetworkTreeNode"
});

defineProps({
  node: {
    type: Object,
    required: true
  },
  level: {
    type: Number,
    default: 1
  }
});

function fullName(persona) {
  return `${persona?.nombres || ""} ${persona?.apellidos || ""}`.trim() || "Persona";
}

function initials(personaOrName) {
  const name = typeof personaOrName === "string" ? personaOrName : fullName(personaOrName);
  return name.split(" ").map((part) => part[0]).join("").slice(0, 2).toUpperCase() || "VY";
}

function progressWidth(node) {
  const children = node.children?.length || 0;
  const percent = Math.min(100, 35 + children * 18);
  return `${percent}%`;
}
</script>

<template>
  <li class="tree-item">
    <article class="tree-card">
      <header>
        <VyAvatar :name="initials(node.persona)" :size="34" bg="var(--vy-cream)" />
        <div>
          <strong>{{ fullName(node.persona) }}</strong>
          <small>{{ node.plan?.nombre || "Sin plan" }}</small>
        </div>
        <MoreHorizontal :size="15" stroke-width="2" />
      </header>

      <div class="volume-track">
        <span :style="{ width: progressWidth(node) }"></span>
      </div>

      <footer>
        <span>Nivel {{ level }}</span>
        <strong>{{ node.children?.length || 0 }} directos</strong>
      </footer>
    </article>

    <ul v-if="node.children?.length" class="tree-children">
      <NetworkTreeNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :level="level + 1"
      />
    </ul>
  </li>
</template>

<style scoped>
.tree-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  min-width: 214px;
}

.tree-card {
  width: 214px;
  min-height: 108px;
  padding: 12px 12px 10px;
  border-radius: 8px;
  border: 1px solid rgba(214, 204, 188, 0.86);
  background: #fff;
  box-shadow: 0 8px 18px rgba(31, 26, 20, 0.05);
  position: relative;
  z-index: 1;
}

.tree-card:hover {
  border-color: rgba(242, 135, 5, 0.5);
  box-shadow: 0 12px 24px rgba(31, 26, 20, 0.08);
}

.tree-card header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 9px;
}

.tree-card header > div {
  flex: 1;
  min-width: 0;
}

.tree-card header strong,
.tree-card header small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-card header strong {
  color: var(--vy-ink);
  font-size: 12px;
  font-weight: 900;
}

.tree-card header small {
  margin-top: 2px;
  color: var(--vy-orange-deep);
  font-size: 10px;
  font-weight: 900;
}

.volume-track {
  height: 4px;
  border-radius: 99px;
  background: var(--vy-line-2);
  margin: 8px 0 9px;
}

.volume-track span {
  display: block;
  height: 100%;
  border-radius: 99px;
  background: var(--vy-orange);
}

.tree-card footer {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding-top: 7px;
  border-top: 1px dashed var(--vy-line);
  font-size: 10px;
  font-weight: 900;
  color: var(--vy-ink-3);
}

.tree-card footer strong {
  color: var(--vy-ink-2);
}

.tree-children {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 44px 0 0;
  margin: 0;
  list-style: none;
  position: relative;
}

.tree-card:has(+ .tree-children)::after {
  content: "";
  position: absolute;
  left: 50%;
  bottom: -22px;
  height: 22px;
  border-left: 1px solid rgba(242, 135, 5, 0.62);
}

.tree-children > .tree-item::before {
  content: "";
  position: absolute;
  top: -22px;
  left: 50%;
  height: 22px;
  border-left: 1px solid rgba(242, 135, 5, 0.5);
}

.tree-children > .tree-item::after {
  content: "";
  position: absolute;
  top: -22px;
  left: 0;
  right: 0;
  border-top: 1px solid rgba(242, 135, 5, 0.5);
}

.tree-children > .tree-item:first-child::after {
  left: 50%;
}

.tree-children > .tree-item:last-child::after {
  right: 50%;
}

.tree-children > .tree-item:only-child::after {
  display: none;
}

@media (max-width: 900px) {
  .tree-item {
    min-width: 100%;
    align-items: stretch;
  }

  .tree-card {
    width: 100%;
  }

  .tree-children {
    flex-direction: column;
    gap: 12px;
    padding-left: 18px;
  }

  .tree-children::before {
    top: 0;
    left: 8px;
    width: 0;
    height: 100%;
    border-top: 0;
    border-left: 2px solid rgba(242, 135, 5, 0.32);
    transform: none;
  }

  .tree-children > .tree-item::before,
  .tree-children > .tree-item::after,
  .tree-card:has(+ .tree-children)::after {
    display: none;
  }
}
</style>
