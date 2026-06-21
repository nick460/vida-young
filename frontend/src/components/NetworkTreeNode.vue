<script setup>
import { MoreHorizontal } from "lucide-vue-next";

defineOptions({
  name: "NetworkTreeNode"
});

const API_URL = import.meta.env.VITE_API_URL || "";

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

function photoUrl(persona) {
  const photo = persona?.fotoPerfil || persona?.fotoUrl || persona?.imagenUrl || persona?.photo || "";

  if (!photo) {
    return "";
  }

  if (photo.startsWith("http") || photo.startsWith("blob:")) {
    return photo;
  }

  const normalizedPhoto = photo.startsWith("/") ? photo : `/${photo}`;
  return `${API_URL}${normalizedPhoto}`;
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
        <span class="member-avatar">
          <img v-if="photoUrl(node.persona)" :src="photoUrl(node.persona)" :alt="fullName(node.persona)" />
          <span v-else>{{ initials(node.persona) }}</span>
        </span>
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
  min-width: 236px;
}

.tree-card {
  width: 236px;
  min-height: 116px;
  padding: 13px 13px 11px;
  border-radius: 8px;
  border: 1px solid rgba(214, 204, 188, 0.86);
  background:
    linear-gradient(180deg, #fff 0%, rgba(255, 248, 232, 0.62) 100%);
  box-shadow: 0 10px 22px rgba(31, 26, 20, 0.06);
  position: relative;
  z-index: 1;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.tree-card:hover {
  border-color: rgba(242, 135, 5, 0.5);
  box-shadow: 0 12px 24px rgba(31, 26, 20, 0.08);
  transform: translateY(-1px);
}

.tree-card header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 9px;
}

.member-avatar {
  width: 42px;
  height: 42px;
  flex: 0 0 42px;
  border-radius: 50%;
  padding: 3px;
  background: linear-gradient(135deg, rgba(242, 135, 5, 0.85), rgba(242, 183, 5, 0.55));
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.member-avatar img,
.member-avatar > span {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 2px solid #fff;
}

.member-avatar img {
  object-fit: cover;
  display: block;
  background: var(--vy-surface-2);
}

.member-avatar > span {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--vy-cream);
  color: var(--vy-ink);
  font-size: 13px;
  font-weight: 900;
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
    min-width: 220px;
    align-items: center;
  }

  .tree-card {
    width: 220px;
    min-height: 112px;
  }

  .tree-children {
    flex-direction: row;
    gap: 14px;
    padding: 42px 0 0;
  }

  .tree-children > .tree-item::before,
  .tree-children > .tree-item::after,
  .tree-card:has(+ .tree-children)::after {
    display: block;
  }
}

@media (max-width: 420px) {
  .tree-item {
    min-width: 214px;
  }

  .tree-card {
    width: 214px;
  }
}
</style>
