<script setup>
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { AlertCircle, FileText, RefreshCw, Search, Terminal } from "lucide-vue-next";
import { getLogFile, listLogFiles } from "../services/logService.js";

const files = ref([]);
const selectedFile = ref("");
const logContent = ref("");
const lines = ref(600);
const query = ref("");
const loadingFiles = ref(false);
const loadingContent = ref(false);
const error = ref("");
const consoleRef = ref(null);

const selectedFileMeta = computed(() => files.value.find((file) => file.name === selectedFile.value) || null);
const filteredLines = computed(() => {
  const sourceLines = logContent.value ? logContent.value.split(/\r?\n/) : [];
  const term = query.value.trim().toLowerCase();

  if (!term) {
    return sourceLines;
  }

  return sourceLines.filter((line) => line.toLowerCase().includes(term));
});

const statusText = computed(() => {
  if (loadingContent.value) {
    return "Cargando";
  }

  if (!selectedFile.value) {
    return "Sin archivo";
  }

  return `${filteredLines.value.length} lineas`;
});

function formatBytes(value) {
  const bytes = Number(value || 0);

  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} KB`;
  }

  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function formatDate(value) {
  if (!value) {
    return "Sin fecha";
  }

  return new Date(value).toLocaleString("es-BO", {
    year: "numeric",
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function lineClass(line) {
  if (line.includes(" ERROR ")) {
    return "line-error";
  }

  if (line.includes(" WARN ")) {
    return "line-warn";
  }

  if (line.includes(" INFO ")) {
    return "line-info";
  }

  return "";
}

async function scrollToBottom() {
  await nextTick();

  if (consoleRef.value) {
    consoleRef.value.scrollTop = consoleRef.value.scrollHeight;
  }
}

async function loadFiles() {
  loadingFiles.value = true;
  error.value = "";

  try {
    const response = await listLogFiles();
    files.value = Array.isArray(response) ? response : [];

    if (!selectedFile.value && files.value.length) {
      selectedFile.value = files.value[0].name;
    }

    if (selectedFile.value && !files.value.some((file) => file.name === selectedFile.value)) {
      selectedFile.value = files.value[0]?.name || "";
    }
  } catch (exception) {
    error.value = exception.message || "No se pudieron cargar los archivos de log.";
  } finally {
    loadingFiles.value = false;
  }
}

async function loadContent() {
  if (!selectedFile.value) {
    logContent.value = "";
    return;
  }

  loadingContent.value = true;
  error.value = "";

  try {
    const response = await getLogFile(selectedFile.value, lines.value);
    logContent.value = response?.content || "";
    await scrollToBottom();
  } catch (exception) {
    error.value = exception.message || "No se pudo leer el archivo seleccionado.";
  } finally {
    loadingContent.value = false;
  }
}

async function refresh() {
  await loadFiles();
  await loadContent();
}

watch(selectedFile, loadContent);
watch(lines, loadContent);

onMounted(refresh);
</script>

<template>
  <div class="vy logs-view">
    <section class="workspace">
      <main class="logs-content">
        <header class="page-header">
          <div>
            <div class="vy-eyebrow">Sistema</div>
            <h1>Consola de logs</h1>
            <p>Lectura diaria de los archivos generados por Spring Boot.</p>
          </div>

          <button class="vy-btn vy-btn-dark" type="button" :disabled="loadingFiles || loadingContent" @click="refresh">
            <RefreshCw :size="16" :class="{ spinning: loadingFiles || loadingContent }" />
            Refrescar
          </button>
        </header>

        <section class="toolbar">
          <label>
            <span>Archivo</span>
            <select v-model="selectedFile" :disabled="loadingFiles || !files.length">
              <option v-for="file in files" :key="file.name" :value="file.name">
                {{ file.name }}
              </option>
            </select>
          </label>

          <label>
            <span>Lineas</span>
            <select v-model.number="lines">
              <option :value="200">200</option>
              <option :value="600">600</option>
              <option :value="1200">1200</option>
              <option :value="2500">2500</option>
              <option :value="5000">5000</option>
            </select>
          </label>

          <label class="search-control">
            <span>Filtro</span>
            <div>
              <Search :size="16" />
              <input v-model="query" type="search" placeholder="Buscar dentro del log" />
            </div>
          </label>
        </section>

        <p v-if="error" class="logs-error">
          <AlertCircle :size="16" />
          {{ error }}
        </p>

        <section class="console-shell">
          <header>
            <div>
              <Terminal :size="16" />
              <strong>{{ selectedFile || "logs" }}</strong>
            </div>
            <div class="console-meta">
              <span>{{ statusText }}</span>
              <span v-if="selectedFileMeta">{{ formatBytes(selectedFileMeta.size) }}</span>
              <span v-if="selectedFileMeta">{{ formatDate(selectedFileMeta.lastModified) }}</span>
            </div>
          </header>

          <div ref="consoleRef" class="console-output" aria-live="polite">
            <div v-if="loadingContent" class="empty-console">Cargando archivo...</div>
            <div v-else-if="!files.length" class="empty-console">
              <FileText :size="18" />
              No hay archivos .txt en la carpeta logs.
            </div>
            <div v-else-if="!filteredLines.length" class="empty-console">No hay lineas para mostrar.</div>
            <ol v-else>
              <li v-for="(line, index) in filteredLines" :key="`${index}-${line}`" :class="lineClass(line)">
                <span class="line-number">{{ index + 1 }}</span>
                <code>{{ line || " " }}</code>
              </li>
            </ol>
          </div>
        </section>
      </main>
    </section>
  </div>
</template>

<style scoped>
.workspace {
  min-width: 0;
  min-height: 100vh;
}

.logs-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
  padding: 28px 32px 40px;
}

.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 18px;
}

.page-header h1 {
  margin-top: 8px;
  font-size: 30px;
  font-weight: 900;
}

.page-header p {
  margin-top: 5px;
  color: var(--vy-ink-2);
  font-size: 14px;
}

.page-header button {
  flex: 0 0 auto;
  padding: 10px 14px;
}

.spinning {
  animation: spin 0.9s linear infinite;
}

.toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) 150px minmax(220px, 0.8fr);
  gap: 12px;
  align-items: end;
}

.toolbar label {
  display: flex;
  flex-direction: column;
  gap: 7px;
  min-width: 0;
}

.toolbar label > span {
  color: var(--vy-ink-3);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.toolbar select,
.search-control div {
  width: 100%;
  min-height: 42px;
  box-sizing: border-box;
  border: 1px solid var(--vy-line);
  border-radius: 8px;
  background: var(--vy-surface);
  color: var(--vy-ink);
}

.toolbar select {
  padding: 0 12px;
  font-weight: 800;
}

.search-control div {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 0 12px;
}

.search-control svg {
  color: var(--vy-ink-3);
  flex: 0 0 auto;
}

.search-control input {
  min-width: 0;
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--vy-ink);
  font: inherit;
  font-size: 13px;
}

.logs-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 8px;
  background: rgba(196, 69, 42, 0.1);
  color: var(--vy-danger);
  font-size: 13px;
  font-weight: 800;
}

.console-shell {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #2e332f;
  border-radius: 8px;
  background: #101412;
  box-shadow: 0 18px 44px rgba(31, 26, 20, 0.18);
}

.console-shell > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 14px;
  border-bottom: 1px solid #293029;
  background: #181e1a;
  color: #d8e8d8;
}

.console-shell > header > div:first-child,
.console-meta {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.console-shell strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--font-mono);
  font-size: 13px;
}

.console-meta {
  flex-wrap: wrap;
  justify-content: flex-end;
  color: #8ca08f;
  font-family: var(--font-mono);
  font-size: 11px;
}

.console-meta span {
  white-space: nowrap;
}

.console-output {
  height: min(68vh, 720px);
  min-height: 420px;
  overflow: auto;
  background: #0c100e;
  color: #cfe3d1;
  font-family: Consolas, "Liberation Mono", "Courier New", monospace;
  font-size: 12px;
  line-height: 1.5;
}

.console-output ol {
  margin: 0;
  padding: 10px 0;
  list-style: none;
}

.console-output li {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 12px;
  padding: 1px 14px 1px 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.console-output li:hover {
  background: rgba(255, 255, 255, 0.045);
}

.line-number {
  color: #5d6c61;
  text-align: right;
  user-select: none;
}

.console-output code {
  min-width: 0;
  font: inherit;
}

.line-error code {
  color: #ff9b8f;
}

.line-warn code {
  color: #ffd27a;
}

.line-info code {
  color: #a7d6ff;
}

.empty-console {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 180px;
  padding: 28px;
  color: #8ca08f;
  font-weight: 800;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 920px) {
  .logs-content {
    padding: 24px 20px 110px;
  }

  .toolbar {
    grid-template-columns: 1fr;
  }

  .console-shell > header {
    align-items: flex-start;
    flex-direction: column;
  }

  .console-meta {
    justify-content: flex-start;
  }
}

@media (max-width: 520px) {
  .logs-content {
    padding-left: 14px;
    padding-right: 14px;
  }

  .console-output {
    min-height: 360px;
    font-size: 11px;
  }

  .console-output li {
    grid-template-columns: 42px minmax(0, 1fr);
    gap: 8px;
    padding-right: 10px;
  }
}
</style>
