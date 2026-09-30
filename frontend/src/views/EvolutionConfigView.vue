<script setup>
import { onMounted, reactive, ref } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { MessageCircle, RefreshCw, Save } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";

const loading = ref(false);
const saving = ref(false);
const error = ref("");

const form = reactive({
  habilitado: false,
  apiUrl: "https://evolution.rubina-solutions.tech",
  apiKey: "",
  instanceName: "",
  codigoPais: "591",
  loginUrl: "https://vidayoung.online/login"
});

async function cargar() {
  loading.value = true;
  error.value = "";
  try {
    const data = await apiRequest("/api/evolution-config");
    Object.assign(form, {
      habilitado: Boolean(data.habilitado),
      apiUrl: data.apiUrl || "https://evolution.rubina-solutions.tech",
      apiKey: data.apiKey || "",
      instanceName: data.instanceName || "",
      codigoPais: data.codigoPais || "591",
      loginUrl: data.loginUrl || "https://vidayoung.online/login"
    });
  } catch (exception) {
    error.value = exception.message || "No se pudo cargar la configuración.";
  } finally {
    loading.value = false;
  }
}

async function guardar() {
  saving.value = true;
  try {
    const data = await apiRequest("/api/evolution-config", {
      method: "PUT",
      body: JSON.stringify(form)
    });
    Object.assign(form, data);
    await Swal.fire({
      title: "Configuración guardada",
      text: "Los próximos registros usarán esta configuración sin reiniciar el sistema.",
      icon: "success",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
  } catch (exception) {
    await Swal.fire({
      title: "No se pudo guardar",
      text: exception.message || "Verifica los datos.",
      icon: "error",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
  } finally {
    saving.value = false;
  }
}

onMounted(cargar);
</script>

<template>
  <main class="evolution-page">
    <section class="evolution-header">
      <div>
        <span class="vy-chip vy-chip-orange"><MessageCircle :size="14" /> WhatsApp</span>
        <h1>Evolution API</h1>
        <p>Configura la instancia que enviará las bienvenidas automáticas a nuevos usuarios.</p>
      </div>
      <button type="button" class="refresh-button" :disabled="loading" @click="cargar">
        <RefreshCw :size="16" /> Actualizar
      </button>
    </section>

    <p v-if="error" class="error-box">{{ error }}</p>

    <form class="config-card" @submit.prevent="guardar">
      <label class="toggle-row">
        <input v-model="form.habilitado" type="checkbox" />
        <span>
          <strong>Enviar bienvenida por WhatsApp</strong>
          <small>Si está apagado, el registro continúa normal pero no se envía mensaje.</small>
        </span>
      </label>

      <div class="field-grid">
        <label>
          URL Evolution API
          <input v-model.trim="form.apiUrl" type="url" placeholder="https://evolution.rubina-solutions.tech" />
        </label>
        <label>
          Instancia
          <input v-model.trim="form.instanceName" type="text" placeholder="Nombre de la instancia" />
        </label>
        <label>
          API key
          <input v-model.trim="form.apiKey" type="password" placeholder="apikey de Evolution" autocomplete="new-password" />
        </label>
        <label>
          Código país
          <input v-model.trim="form.codigoPais" type="text" placeholder="591" />
        </label>
        <label class="full">
          Link de login
          <input v-model.trim="form.loginUrl" type="url" placeholder="https://vidayoung.online/login" />
        </label>
      </div>

      <div class="info-box">
        El sistema usará 10 mensajes de bienvenida y los irá rotando en orden para que no se repita la misma versión dos veces seguidas.
      </div>

      <footer>
        <button type="submit" class="save-button" :disabled="saving">
          <Save :size="16" /> {{ saving ? "Guardando..." : "Guardar configuración" }}
        </button>
      </footer>
    </form>
  </main>
</template>

<style scoped>
.evolution-page { padding: 28px; display: flex; flex-direction: column; gap: 18px; }
.evolution-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; flex-wrap: wrap; }
.evolution-header h1 { margin-top: 10px; font-size: 30px; font-weight: 900; }
.evolution-header p { margin-top: 6px; color: var(--vy-ink-2); }
.refresh-button, .save-button { min-height: 42px; padding: 0 16px; border-radius: 8px; display: inline-flex; align-items: center; justify-content: center; gap: 8px; font-size: 13px; font-weight: 900; }
.refresh-button { border: 1px solid rgba(242, 135, 5, 0.34); background: #fff; color: var(--vy-orange-deep); }
.save-button { border: 1px solid var(--vy-orange); background: linear-gradient(135deg, var(--vy-orange), var(--vy-orange-deep)); color: #fff; }
.config-card { border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface); padding: 20px; box-shadow: var(--vy-shadow-sm); }
.toggle-row { display: flex; align-items: flex-start; gap: 12px; padding: 14px; border: 1px solid var(--vy-line); border-radius: 10px; background: var(--vy-surface-2); margin-bottom: 16px; }
.toggle-row input { margin-top: 4px; transform: scale(1.2); }
.toggle-row strong, .toggle-row small { display: block; }
.toggle-row small { margin-top: 3px; color: var(--vy-ink-3); font-size: 12px; }
.field-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.field-grid label { display: flex; flex-direction: column; gap: 6px; color: var(--vy-ink-2); font-size: 13px; font-weight: 800; }
.field-grid .full { grid-column: 1 / -1; }
.field-grid input { width: 100%; border: 1px solid var(--vy-line); border-radius: 8px; padding: 11px 12px; background: #fff; color: var(--vy-ink); font: inherit; }
.info-box, .error-box { margin-top: 16px; padding: 12px 14px; border-radius: 10px; font-size: 13px; font-weight: 800; }
.info-box { border: 1px solid rgba(242, 135, 5, 0.25); background: #fff7e8; color: #8a4a05; }
.error-box { border: 1px solid rgba(196, 69, 42, 0.25); background: rgba(196, 69, 42, 0.08); color: var(--vy-danger); }
footer { display: flex; justify-content: flex-end; margin-top: 18px; }
@media (max-width: 760px) { .field-grid { grid-template-columns: 1fr; } }
</style>
