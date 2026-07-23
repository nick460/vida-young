<script setup>
import { onMounted, ref } from "vue";
import { Bot, Loader2, RotateCcw, Save, Settings } from "lucide-vue-next";
import { loadAssistantConfig, saveAssistantConfig } from "../services/asistenteService.js";

const loading = ref(false);
const saving = ref(false);
const error = ref("");
const success = ref("");
const systemInstruction = ref("");

async function loadConfig() {
  loading.value = true;
  error.value = "";
  success.value = "";

  try {
    const config = await loadAssistantConfig();
    systemInstruction.value = config.systemInstruction || "";
  } catch (exception) {
    error.value = exception.message || "No se pudo cargar la configuracion.";
  } finally {
    loading.value = false;
  }
}

async function saveConfig() {
  saving.value = true;
  error.value = "";
  success.value = "";

  try {
    const config = await saveAssistantConfig(systemInstruction.value);
    systemInstruction.value = config.systemInstruction || "";
    success.value = "Reglas guardadas correctamente.";
  } catch (exception) {
    error.value = exception.message || "No se pudieron guardar las reglas.";
  } finally {
    saving.value = false;
  }
}

function restoreDefault() {
  systemInstruction.value = "";
  success.value = "";
  error.value = "";
}

onMounted(loadConfig);
</script>

<template>
  <div class="vy assistant-config-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Configuracion</div>
          <h1>Reglas del asistente</h1>
          <p>Define la instruccion del sistema que Gemini usara como comportamiento base.</p>
        </div>
        <button class="vy-btn vy-btn-ghost" type="button" :disabled="loading" @click="loadConfig">
          <RotateCcw :size="15" />
          Recargar
        </button>
      </header>

      <section class="config-shell">
        <div class="config-heading">
          <span class="config-icon"><Bot :size="22" /></span>
          <div>
            <h2>System instruction</h2>
            <p>Estas reglas se aplican antes de cada mensaje enviado desde el chat.</p>
          </div>
        </div>

        <textarea
          v-model="systemInstruction"
          class="instruction-input"
          rows="16"
          :disabled="loading || saving"
          placeholder="Ejemplo: Responde como asesor de Vidayoung. Usa un tono claro, breve y profesional. No inventes precios ni promesas medicas. Si falta informacion, pide contexto."
        ></textarea>

        <div v-if="loading" class="status-box loading">
          <Loader2 :size="16" class="spin" />
          Cargando reglas...
        </div>
        <div v-if="error" class="status-box error">{{ error }}</div>
        <div v-if="success" class="status-box success">{{ success }}</div>

        <footer class="actions">
          <button class="vy-btn vy-btn-ghost" type="button" :disabled="saving" @click="restoreDefault">
            <Settings :size="15" />
            Limpiar
          </button>
          <button class="vy-btn vy-btn-dark" type="button" :disabled="saving || loading" @click="saveConfig">
            <Loader2 v-if="saving" :size="15" class="spin" />
            <Save v-else :size="15" />
            Guardar reglas
          </button>
        </footer>
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace { min-width: 0; padding: 28px 32px 48px; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.page-header h1 { margin-top: 8px; font-size: 30px; font-weight: 900; }
.page-header p { margin-top: 4px; color: var(--vy-ink-2); font-size: 14px; }
.page-header .vy-btn { min-height: 40px; padding: 10px 16px; border-radius: 12px; font-weight: 800; }
.config-shell { display: flex; flex-direction: column; gap: 16px; max-width: 980px; border: 1px solid var(--vy-line); border-radius: 8px; background: #fff; padding: 18px; box-shadow: 0 18px 44px rgba(31, 26, 20, 0.08); box-sizing: border-box; }
.config-heading { display: flex; align-items: flex-start; gap: 13px; }
.config-icon { width: 44px; height: 44px; border-radius: 8px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; background: var(--vy-cream); color: var(--vy-orange-deep); }
.config-heading h2 { margin: 0; font-size: 20px; font-weight: 900; }
.config-heading p { margin: 4px 0 0; color: var(--vy-ink-2); font-size: 13px; line-height: 1.45; }
.instruction-input { width: 100%; min-height: 420px; resize: vertical; border: 1px solid var(--vy-line); border-radius: 8px; padding: 14px; color: var(--vy-ink); font: inherit; font-size: 14px; line-height: 1.55; outline: none; box-sizing: border-box; }
.instruction-input:focus { border-color: var(--vy-orange); box-shadow: 0 0 0 3px rgba(242, 135, 5, 0.12); }
.status-box { display: flex; align-items: center; gap: 8px; padding: 11px 13px; border-radius: 8px; font-size: 13px; font-weight: 800; }
.status-box.loading { background: var(--vy-surface-2); color: var(--vy-ink-2); }
.status-box.error { background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); }
.status-box.success { background: rgba(70, 142, 90, 0.12); color: #2f7447; }
.actions { display: flex; justify-content: flex-end; gap: 10px; flex-wrap: wrap; }
.actions .vy-btn { min-height: 42px; padding: 10px 16px; border-radius: 8px; font-weight: 900; }
.actions .vy-btn:disabled { opacity: 0.58; cursor: not-allowed; }
.spin { animation: spin 0.9s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 860px) {
  .workspace { padding: 22px 14px 112px !important; }
  .page-header { align-items: stretch; flex-direction: column; }
  .config-shell { padding: 14px; }
  .instruction-input { min-height: 360px; }
  .actions .vy-btn { width: 100%; }
}
</style>
