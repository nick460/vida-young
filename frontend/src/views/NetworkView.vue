<script setup>
import { computed, onMounted, ref } from "vue";
import {
  Copy,
  Plus,
  RefreshCw
} from "lucide-vue-next";
import { useAuthStore } from "../stores/authStore.js";
import { apiRequest } from "../services/api.js";
import NetworkTreeNode from "../components/NetworkTreeNode.vue";

const authStore = useAuthStore();
const API_URL = import.meta.env.VITE_API_URL || "";
const loading = ref(false);
const error = ref("");
const referidos = ref([]);
const viewMode = ref("tree");
const copiedInvite = ref(false);

const currentPersona = computed(() => authStore.usuario?.persona || null);
const directChildren = computed(() => childrenOf(currentPersona.value?.id));
const networkTree = computed(() => buildTree(currentPersona.value?.id));
const networkRows = computed(() => flattenTree(networkTree.value));
const mapLevels = computed(() => {
  const levels = new Map();
  networkRows.value.forEach((item) => {
    const rows = levels.get(item.level) || [];
    rows.push(item);
    levels.set(item.level, rows);
  });
  return [...levels.entries()].map(([level, rows]) => ({ level, rows }));
});
const activeCount = computed(() => collectNetwork(currentPersona.value?.id).length);
const maxDepth = computed(() => {
  const planDepths = collectNetwork(currentPersona.value?.id).map((item) => Number(item.plan?.nivelesAlcance || 0));
  return Math.max(0, ...planDepths);
});
const volumeLabel = computed(() => activeCount.value ? `${activeCount.value} miembros` : "Sin red");
const inviteCode = computed(() => authStore.usuario?.username || currentPersona.value?.documento || "VIDAYOUNG");
const inviteLink = computed(() => {
  if (!currentPersona.value?.id) return "";
  const base = `${window.location.origin}${window.location.pathname}`;
  return `${base}#/preinscripcion-referido/${currentPersona.value.id}`;
});

const summary = computed(() => {
  const levels = [1, 2, 3, 4].map((level) => ({
    label: `Nivel ${level}`,
    value: String(nodesAtLevel(currentPersona.value?.id, level).length)
  }));

  return [
    ...levels,
    { label: "Activos", value: String(activeCount.value), active: true },
    { label: "Alcance", value: maxDepth.value ? `${maxDepth.value} niv.` : "0 niv." }
  ];
});

function fullName(persona) {
  return `${persona?.nombres || ""} ${persona?.apellidos || ""}`.trim() || "Persona";
}

function initials(personaOrName) {
  const name = typeof personaOrName === "string" ? personaOrName : fullName(personaOrName);
  return name.split(" ").map((part) => part[0]).join("").slice(0, 2).toUpperCase() || "VY";
}

function photoUrl(persona, fallback = "") {
  const photo = persona?.fotoPerfil || persona?.fotoUrl || persona?.imagenUrl || persona?.photo || fallback || "";

  if (!photo) {
    return "";
  }

  if (photo.startsWith("http") || photo.startsWith("blob:")) {
    return photo;
  }

  const normalizedPhoto = photo.startsWith("/") ? photo : `/${photo}`;
  return `${API_URL}${normalizedPhoto}`;
}

function currentPhotoUrl() {
  return photoUrl(currentPersona.value, authStore.usuario?.fotoPerfil || "");
}

function childrenOf(personaId) {
  if (!personaId) return [];
  return referidos.value.filter((item) => item.patrocinador?.id === personaId);
}

function collectNetwork(personaId, visited = new Set()) {
  if (!personaId || visited.has(personaId)) return [];
  visited.add(personaId);

  return childrenOf(personaId).flatMap((item) => [
    item,
    ...collectNetwork(item.persona?.id, visited)
  ]);
}

function nodesAtLevel(personaId, targetLevel) {
  if (!personaId || targetLevel < 1) return [];

  let current = childrenOf(personaId);
  for (let level = 1; level < targetLevel; level += 1) {
    current = current.flatMap((item) => childrenOf(item.persona?.id));
  }

  return current;
}

function buildTree(rootPersonaId, visited = new Set()) {
  if (!rootPersonaId || visited.has(rootPersonaId)) return [];
  visited.add(rootPersonaId);

  return childrenOf(rootPersonaId).map((item) => ({
    ...item,
    children: buildTree(item.persona?.id, new Set(visited))
  }));
}

function flattenTree(nodes, level = 1) {
  return nodes.flatMap((node) => [
    {
      ...node,
      level,
      directCount: node.children?.length || 0
    },
    ...flattenTree(node.children || [], level + 1)
  ]);
}

async function loadNetwork() {
  loading.value = true;
  error.value = "";

  try {
    if (!authStore.usuario?.persona?.id) {
      await authStore.cargarPerfil();
    }

    referidos.value = await apiRequest("/api/referidos");
  } catch (exception) {
    error.value = "No se pudo cargar tu red. Verifica que la sesion siga activa.";
  } finally {
    loading.value = false;
  }
}

async function copyInviteLink() {
  if (!inviteLink.value) return;
  await navigator.clipboard.writeText(inviteLink.value);
  copiedInvite.value = true;
  window.setTimeout(() => {
    copiedInvite.value = false;
  }, 1800);
}

onMounted(loadNetwork);
</script>

<template>
  <div class="vy network-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Tu red</div>
          <h1>{{ fullName(currentPersona) }}</h1>
          <p>{{ activeCount }} miembros activos en tu estructura. Alcance detectado: {{ maxDepth }} niveles.</p>
        </div>
        <button class="refresh-network-button" type="button" :disabled="loading" @click="loadNetwork">
          <RefreshCw v-if="loading" :size="16" stroke-width="2.2" />
          <Plus v-else :size="16" stroke-width="2.2" />
          Actualizar red
        </button>
      </header>

      <p v-if="error" class="network-error">{{ error }}</p>

      <section class="vy-card invite-card">
        <div class="invite-main">
          <span class="vy-chip vy-chip-cream">Tu enlace de invitacion</span>
          <div class="invite-link">
            <Copy :size="17" stroke-width="2" />
            <strong>{{ inviteLink || inviteCode }}</strong>
            <button class="vy-btn vy-btn-dark" type="button" :disabled="!inviteLink" @click="copyInviteLink">
              {{ copiedInvite ? "Copiado" : "Copiar" }}
            </button>
          </div>
        </div>

        <div class="summary-grid">
          <article v-for="item in summary" :key="item.label" :class="{ active: item.active }">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
      </section>

      <section class="network-card">
        <div class="network-content">
          <header class="network-header">
            <h2>Estructura de red</h2>
            <div class="view-tabs">
              <button :class="{ active: viewMode === 'tree' }" type="button" @click="viewMode = 'tree'">Arbol</button>
              <button :class="{ active: viewMode === 'list' }" type="button" @click="viewMode = 'list'">Lista</button>
              <button :class="{ active: viewMode === 'map' }" type="button" @click="viewMode = 'map'">Mapa</button>
            </div>
          </header>

          <div v-if="viewMode !== 'tree'" class="root-row">
            <article class="root-node">
              <span>Tu</span>
              <div class="root-avatar">
                <img v-if="currentPhotoUrl()" :src="currentPhotoUrl()" :alt="fullName(currentPersona)" />
                <strong v-else>{{ initials(currentPersona) }}</strong>
              </div>
              <div>
                <strong>{{ fullName(currentPersona) }}</strong>
                <small>{{ authStore.usuario?.roles?.[0] || "USUARIO" }} - {{ volumeLabel }}</small>
              </div>
            </article>
          </div>

          <div v-if="loading" class="empty-state">Cargando estructura...</div>
          <div v-else-if="!currentPersona?.id" class="empty-state">Tu usuario no tiene persona asociada.</div>
          <div v-else-if="!directChildren.length" class="empty-state">Todavia no tienes referidos directos.</div>

          <section v-else-if="viewMode === 'tree'" class="tree-stage">
            <div class="tree-canvas">
              <div class="root-row">
                <article class="root-node">
                  <span>Tu</span>
                  <div class="root-avatar">
                    <img v-if="currentPhotoUrl()" :src="currentPhotoUrl()" :alt="fullName(currentPersona)" />
                    <strong v-else>{{ initials(currentPersona) }}</strong>
                  </div>
                  <div>
                    <strong>{{ fullName(currentPersona) }}</strong>
                    <small>{{ authStore.usuario?.roles?.[0] || "USUARIO" }} - {{ volumeLabel }}</small>
                  </div>
                </article>
              </div>

              <ul class="tree-root">
                <NetworkTreeNode
                  v-for="node in networkTree"
                  :key="node.id"
                  :node="node"
                  :level="1"
                />
              </ul>
            </div>
          </section>

          <section v-else-if="viewMode === 'list'" class="network-list-view">
            <article v-for="row in networkRows" :key="row.id" class="network-list-row">
              <div class="member-cell">
                <span class="small-avatar">
                  <img v-if="photoUrl(row.persona)" :src="photoUrl(row.persona)" :alt="fullName(row.persona)" />
                  <strong v-else>{{ initials(row.persona) }}</strong>
                </span>
                <span>
                  <strong>{{ fullName(row.persona) }}</strong>
                  <small>{{ row.plan?.nombre || "Sin plan" }}</small>
                </span>
              </div>
              <span class="level-pill">Nivel {{ row.level }}</span>
              <strong class="direct-count">{{ row.directCount }} directos</strong>
            </article>
          </section>

          <section v-else class="network-map-view">
            <div v-for="level in mapLevels" :key="level.level" class="map-column">
              <header>
                <strong>Nivel {{ level.level }}</strong>
                <span>{{ level.rows.length }}</span>
              </header>
              <div class="map-nodes">
                <article v-for="row in level.rows" :key="row.id" class="map-node">
                  <span class="small-avatar compact">
                    <img v-if="photoUrl(row.persona)" :src="photoUrl(row.persona)" :alt="fullName(row.persona)" />
                    <strong v-else>{{ initials(row.persona) }}</strong>
                  </span>
                  <span>
                    <strong>{{ fullName(row.persona) }}</strong>
                    <small>{{ row.directCount }} directos</small>
                  </span>
                </article>
              </div>
            </div>
          </section>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace {
  padding: 28px 32px 40px;
  min-width: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 30px;
  font-weight: 800;
  margin-top: 8px;
}

.page-header p {
  font-size: 14px;
  color: var(--vy-ink-2);
  margin-top: 4px;
}

.network-error,
.empty-state {
  padding: 14px 16px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 700;
}

.network-error {
  color: var(--vy-danger);
  background: rgba(196, 69, 42, 0.1);
  margin-bottom: 14px;
}

.empty-state {
  color: var(--vy-ink-2);
  background: var(--vy-surface-2);
  text-align: center;
}

.invite-card {
  padding: 22px;
  margin-bottom: 18px;
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(360px, 1fr);
  gap: 24px;
  align-items: center;
}

.invite-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  background: var(--vy-surface-2);
  border: 1px dashed var(--vy-line);
  border-radius: 14px;
  margin-top: 10px;
}

.invite-link svg {
  color: var(--vy-orange-deep);
}

.invite-link strong {
  flex: 1;
  font-family: var(--font-mono);
  font-size: 14px;
}

.invite-link .vy-btn {
  padding: 8px 14px;
  font-size: 12px;
  border-radius: 10px;
}

.refresh-network-button {
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid rgba(242, 135, 5, 0.34);
  border-radius: 8px;
  background: #fff;
  color: var(--vy-orange-deep);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 900;
  box-shadow: 0 8px 18px rgba(31, 26, 20, 0.05);
  transition: background 0.16s ease, border-color 0.16s ease, color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.refresh-network-button:hover:not(:disabled) {
  background: rgba(242, 135, 5, 0.08);
  border-color: rgba(242, 135, 5, 0.62);
  color: var(--vy-ink);
  box-shadow: 0 10px 22px rgba(31, 26, 20, 0.08);
  transform: translateY(-1px);
}

.refresh-network-button:focus-visible {
  outline: 3px solid rgba(242, 135, 5, 0.24);
  outline-offset: 2px;
}

.refresh-network-button:disabled {
  cursor: wait;
  opacity: 0.7;
  transform: none;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.summary-grid article {
  padding: 14px;
  background: var(--vy-surface-2);
  border-radius: 12px;
}

.summary-grid article.active {
  background: var(--vy-orange);
  color: #fff;
}

.summary-grid span {
  font-size: 10px;
  color: var(--vy-ink-3);
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.summary-grid article.active span {
  color: rgba(255, 255, 255, 0.85);
}

.summary-grid strong {
  display: block;
  font-family: var(--font-display);
  font-weight: 800;
  font-size: 20px;
  margin-top: 4px;
}

.network-card {
  padding: 22px 24px 28px;
  background: linear-gradient(180deg, #fff 0%, var(--vy-bg) 100%);
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  box-shadow: var(--vy-shadow-sm);
  position: relative;
  overflow: hidden;
}

.network-content {
  position: relative;
}

.network-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 18px;
  margin-bottom: 20px;
}

.network-header h2 {
  font-size: 16px;
  font-weight: 800;
}

.view-tabs {
  display: flex;
  gap: 6px;
  background: var(--vy-surface);
  padding: 4px;
  border-radius: 99px;
  border: 1px solid var(--vy-line);
}

.view-tabs button {
  padding: 6px 14px;
  border-radius: 99px;
  font-size: 12px;
  font-weight: 800;
  color: var(--vy-ink-2);
}

.view-tabs button.active {
  background: var(--vy-ink);
  color: #fff;
}

.root-row {
  display: flex;
  justify-content: center;
  margin-bottom: 18px;
}

.root-node {
  background: var(--vy-ink);
  color: #fff;
  width: min(420px, 100%);
  min-height: 92px;
  padding: 16px 24px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 18px 42px rgba(31, 26, 20, 0.18);
  position: relative;
  overflow: visible;
}

.root-node > span {
  position: absolute;
  top: -10px;
  right: -10px;
  background: var(--vy-orange);
  color: #fff;
  padding: 4px 10px;
  border-radius: 99px;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.root-node strong,
.node-card strong {
  display: block;
  font-weight: 800;
  font-size: 14px;
}

.root-avatar {
  width: 58px;
  height: 58px;
  flex: 0 0 58px;
  border-radius: 50%;
  padding: 4px;
  background: linear-gradient(135deg, var(--vy-orange), var(--vy-orange-deep));
  box-shadow: 0 10px 22px rgba(242, 135, 5, 0.28);
}

.root-avatar img,
.root-avatar strong {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.92);
}

.root-avatar img {
  display: block;
  object-fit: cover;
  background: var(--vy-surface-2);
}

.root-avatar strong {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--vy-orange);
  color: #fff;
  font-size: 16px;
  font-weight: 900;
}

.root-node small {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
}

.tree-stage {
  overflow-x: auto;
  overflow-y: auto;
  padding: 8px 4px 12px;
  max-height: min(68vh, 720px);
  overscroll-behavior: contain;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: rgba(242, 135, 5, 0.52) rgba(214, 204, 188, 0.35);
}

.tree-stage::-webkit-scrollbar {
  height: 8px;
}

.tree-stage::-webkit-scrollbar-track {
  background: rgba(214, 204, 188, 0.35);
  border-radius: 99px;
}

.tree-stage::-webkit-scrollbar-thumb {
  background: rgba(242, 135, 5, 0.52);
  border-radius: 99px;
}

.tree-canvas {
  width: max-content;
  min-width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.tree-stage .root-row {
  width: 100%;
  min-width: max-content;
  justify-content: center;
  margin-bottom: 28px;
  position: relative;
}

.tree-stage .root-row::after {
  content: "";
  position: absolute;
  left: 50%;
  bottom: -28px;
  height: 22px;
  border-left: 1px solid rgba(242, 135, 5, 0.62);
}

.tree-stage .root-node {
  width: 420px;
}

.tree-root {
  display: flex;
  justify-content: center;
  gap: 16px;
  min-width: max-content;
  margin: 0;
  padding: 24px 0 0;
  list-style: none;
  position: relative;
}

.tree-root::before {
  display: none;
}

.tree-root > :deep(.tree-item)::before {
  content: "";
  position: absolute;
  top: -24px;
  left: 50%;
  height: 24px;
  border-left: 1px solid rgba(242, 135, 5, 0.5);
}

.tree-root > :deep(.tree-item)::after {
  content: "";
  position: absolute;
  top: -24px;
  left: 0;
  right: 0;
  border-top: 1px solid rgba(242, 135, 5, 0.5);
}

.tree-root > :deep(.tree-item:first-child)::after {
  left: 50%;
}

.tree-root > :deep(.tree-item:last-child)::after {
  right: 50%;
}

.tree-root > :deep(.tree-item:only-child)::after {
  display: none;
}

.tree-root:has(> li:only-child)::after {
  display: none;
}

.network-list-view {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 760px;
  margin: 16px auto 0;
}

.network-list-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid rgba(214, 204, 188, 0.86);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 18px rgba(31, 26, 20, 0.04);
}

.member-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.small-avatar {
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  border-radius: 50%;
  padding: 2px;
  background: linear-gradient(135deg, rgba(242, 135, 5, 0.75), rgba(242, 231, 196, 0.95));
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.small-avatar.compact {
  width: 34px;
  height: 34px;
  flex-basis: 34px;
}

.small-avatar img,
.small-avatar strong {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  border: 2px solid #fff;
}

.small-avatar img {
  display: block;
  object-fit: cover;
  background: var(--vy-surface-2);
}

.small-avatar strong {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--vy-cream);
  color: var(--vy-ink);
  font-size: 11px;
  font-weight: 900;
}

.member-cell span {
  min-width: 0;
}

.member-cell strong,
.member-cell small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-cell strong {
  color: var(--vy-ink);
  font-size: 13px;
  font-weight: 900;
}

.member-cell small {
  margin-top: 2px;
  color: var(--vy-orange-deep);
  font-size: 11px;
  font-weight: 900;
}

.level-pill {
  padding: 4px 10px;
  border-radius: 99px;
  background: rgba(242, 135, 5, 0.1);
  color: var(--vy-orange-deep);
  font-size: 11px;
  font-weight: 900;
  white-space: nowrap;
}

.direct-count {
  min-width: 78px;
  color: var(--vy-ink-2);
  font-size: 12px;
  text-align: right;
  white-space: nowrap;
}

.network-map-view {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.map-column {
  min-height: 240px;
  padding: 12px;
  border: 1px solid rgba(214, 204, 188, 0.86);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
}

.map-column header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(214, 204, 188, 0.65);
}

.map-column header strong {
  color: var(--vy-ink);
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}

.map-column header span {
  min-width: 24px;
  height: 24px;
  border-radius: 99px;
  background: var(--vy-orange);
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 900;
}

.map-nodes {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.map-node {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 6px 14px rgba(31, 26, 20, 0.04);
}

.map-node span {
  min-width: 0;
}

.map-node strong,
.map-node small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.map-node strong {
  color: var(--vy-ink);
  font-size: 12px;
  font-weight: 900;
}

.map-node small {
  margin-top: 2px;
  color: var(--vy-ink-3);
  font-size: 10.5px;
  font-weight: 800;
}

@media (max-width: 1120px) {
  .invite-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 980px) {
  .workspace {
    padding: 24px 20px 32px;
  }

  .network-card {
    padding: 18px;
  }
}

@media (max-width: 680px) {
  .page-header,
  .network-header,
  .invite-link {
    align-items: stretch;
    flex-direction: column;
  }

  .summary-grid {
    grid-template-columns: 1fr 1fr;
  }

  .network-card {
    padding: 14px;
  }

  .network-header {
    gap: 12px;
  }

  .view-tabs {
    width: 100%;
    border-radius: 8px;
  }

  .view-tabs button {
    flex: 1;
    border-radius: 7px;
    padding: 8px 10px;
  }

  .root-row {
    justify-content: stretch;
  }

  .root-node {
    width: 100%;
    min-height: 82px;
    padding: 14px;
  }

  .root-avatar {
    width: 50px;
    height: 50px;
    flex-basis: 50px;
  }

  .tree-stage {
    margin: 0 -8px;
    padding: 10px 8px 84px;
    max-height: calc(100dvh - 300px);
    min-height: 360px;
    overflow: auto;
    border: 1px solid rgba(214, 204, 188, 0.72);
    border-radius: 8px;
    background:
      linear-gradient(90deg, rgba(255, 248, 232, 0.92), rgba(255, 255, 255, 0) 24px) left / 42px 100% no-repeat,
      linear-gradient(270deg, rgba(255, 248, 232, 0.92), rgba(255, 255, 255, 0) 24px) right / 42px 100% no-repeat,
      rgba(255, 255, 255, 0.64);
  }

  .tree-root {
    min-width: max-content;
    flex-direction: row;
    padding: 24px 24px 24px;
    justify-content: center;
  }

  .tree-stage .root-row {
    justify-content: center;
  }

  .tree-stage .root-node {
    width: 340px;
    min-height: 82px;
  }

  .network-list-row {
    grid-template-columns: 1fr;
    align-items: stretch;
  }

  .direct-count {
    text-align: left;
  }
}

@media (max-width: 420px) {
  .workspace {
    padding: 18px 12px 28px;
  }

  .invite-card {
    padding: 16px;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .root-node {
    align-items: flex-start;
  }

  .network-map-view {
    grid-template-columns: 1fr;
  }
}
</style>
