<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import $ from "jquery";
import select2 from "select2";
import "select2/dist/css/select2.css";
import { RefreshCw, Search } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";

select2($);

const router = useRouter();
const loading = ref(false);
const error = ref("");
const personas = ref([]);
const selectedPersonaId = ref("");
const personaSelect = ref(null);

const selectedPersona = computed(() =>
  personas.value.find((persona) => Number(persona.id) === Number(selectedPersonaId.value)) || null
);

function fullName(persona) {
  if (!persona) return "Sin persona";
  return `${persona.nombres || ""} ${persona.apellidos || ""}`.trim() || "Sin nombre";
}

function personaLabel(persona) {
  return `${fullName(persona)}${persona.documento ? ` - CI ${persona.documento}` : ""}${persona.telefono ? ` - ${persona.telefono}` : ""}`;
}

function destroyPersonaSelect2() {
  if (!personaSelect.value) return;
  const element = $(personaSelect.value);
  if (element.hasClass("select2-hidden-accessible")) {
    element.off("change.consulta-persona");
    element.select2("destroy");
  }
}

async function initPersonaSelect2() {
  await nextTick();
  if (!personaSelect.value) return;

  destroyPersonaSelect2();
  const element = $(personaSelect.value);
  element
    .select2({
      width: "100%",
      placeholder: "Selecciona una persona",
      allowClear: true,
      dropdownParent: $(".consult-search-view"),
      language: {
        noResults: () => "Sin resultados",
        searching: () => "Buscando..."
      }
    })
    .val(selectedPersonaId.value || null)
    .trigger("change.select2");

  element.on("change.consulta-persona", () => {
    selectedPersonaId.value = element.val() || "";
  });
}

async function loadPersonas() {
  loading.value = true;
  error.value = "";

  try {
    const data = await apiRequest("/api/personas");
    personas.value = Array.isArray(data) ? data : [];
    await initPersonaSelect2();
  } catch (exception) {
    error.value = exception.message || "No se pudieron cargar las personas.";
  } finally {
    loading.value = false;
  }
}

function buscarPersona() {
  if (!selectedPersonaId.value) {
    error.value = "Selecciona una persona para consultar.";
    return;
  }

  router.push({ name: "consultar-detalle", params: { personaId: selectedPersonaId.value } });
}

watch(personas, initPersonaSelect2, { deep: true });
watch(selectedPersonaId, (value) => {
  if (!personaSelect.value) return;
  const element = $(personaSelect.value);
  if (element.hasClass("select2-hidden-accessible") && element.val() !== String(value || "")) {
    element.val(value || null).trigger("change.select2");
  }
});

onMounted(loadPersonas);
onBeforeUnmount(destroyPersonaSelect2);
</script>

<template>
  <div class="vy consult-search-view">
    <main class="workspace">
      <section class="search-shell vy-card">
        <header>
          <div class="search-icon"><Search :size="24" /></div>
          <div>
            <span class="vy-eyebrow">Consultar</span>
            <h1>Buscar persona</h1>
            <p>Selecciona una persona para abrir su vista completa por mes.</p>
          </div>
        </header>

        <form class="search-form" @submit.prevent="buscarPersona">
          <label>
            <span>Persona</span>
            <select ref="personaSelect" v-model="selectedPersonaId">
              <option value=""></option>
              <option v-for="persona in personas" :key="persona.id" :value="persona.id">
                {{ personaLabel(persona) }}
              </option>
            </select>
          </label>

          <button class="vy-btn vy-btn-primary" type="submit" :disabled="loading">
            <Search :size="16" />
            Buscar
          </button>
        </form>

        <div v-if="selectedPersona" class="preview-box">
          <small>Vista previa</small>
          <strong>{{ fullName(selectedPersona) }}</strong>
          <span>{{ selectedPersona.documento || "Sin documento" }} - {{ selectedPersona.email || "Sin correo" }} - {{ selectedPersona.telefono || "Sin telefono" }}</span>
        </div>

        <div v-if="error" class="error-box">{{ error }}</div>

        <button class="refresh-button" type="button" :disabled="loading" @click="loadPersonas">
          <RefreshCw :class="{ spinning: loading }" :size="15" />
          {{ loading ? "Cargando..." : "Actualizar lista" }}
        </button>
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace { min-height: calc(100vh - 1px); padding: 28px 32px 48px; display: grid; place-items: center; }
.search-shell { width: min(680px, 100%); padding: 24px; display: grid; gap: 18px; }
.search-shell header { display: flex; align-items: center; gap: 14px; }
.search-icon { width: 52px; height: 52px; border-radius: 14px; background: var(--vy-ink); color: #fff; display: flex; align-items: center; justify-content: center; }
h1 { margin-top: 6px; font-size: 28px; font-weight: 900; }
p { margin-top: 4px; color: var(--vy-ink-2); font-size: 14px; }
.search-form { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 12px; align-items: end; }
label span, .preview-box small { display: block; margin-bottom: 6px; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
select { width: 100%; min-height: 42px; }
.vy-btn { min-height: 42px; padding: 10px 18px; border-radius: 12px; font-weight: 900; display: inline-flex; align-items: center; justify-content: center; gap: 8px; }
.vy-btn-primary { background: var(--vy-orange); color: #fff; box-shadow: var(--vy-shadow-orange); }
.preview-box { padding: 14px; border: 1px solid rgba(242, 135, 5, 0.35); border-radius: 12px; background: #fff8e8; }
.preview-box strong { display: block; margin-top: 4px; font-size: 16px; font-weight: 900; }
.preview-box span { display: block; margin-top: 4px; color: var(--vy-ink-2); font-size: 13px; font-weight: 800; overflow-wrap: anywhere; }
.refresh-button { width: fit-content; display: inline-flex; align-items: center; gap: 7px; color: var(--vy-ink-3); font-size: 12px; font-weight: 900; }
.error-box { padding: 14px 16px; border-radius: 12px; background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); font-size: 13px; font-weight: 800; }
.spinning { animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
::deep(.select2-container--default .select2-selection--single) { min-height: 42px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); display: flex; align-items: center; }
::deep(.select2-container--default .select2-selection--single .select2-selection__rendered) { padding-left: 12px; padding-right: 34px; color: var(--vy-ink); font-size: 13px; font-weight: 800; line-height: 42px; }
::deep(.select2-container--default .select2-selection--single .select2-selection__placeholder) { color: var(--vy-ink-3); }
::deep(.select2-container--default .select2-selection--single .select2-selection__arrow) { height: 42px; right: 8px; }
::deep(.select2-container--default.select2-container--open .select2-selection--single) { border-color: var(--vy-orange); }
::deep(.select2-dropdown) { border: 1px solid var(--vy-line); border-radius: 12px; overflow: hidden; color: var(--vy-ink); }
::deep(.select2-search--dropdown) { padding: 8px; }
::deep(.select2-container--default .select2-search--dropdown .select2-search__field) { min-height: 36px; border: 1px solid var(--vy-line); border-radius: 9px; outline: 0; padding: 0 10px; }
::deep(.select2-results__option) { padding: 9px 12px; font-size: 13px; font-weight: 800; }
::deep(.select2-container--default .select2-results__option--highlighted.select2-results__option--selectable) { background: var(--vy-orange); color: #fff; }
@media (max-width: 720px) {
  .workspace { padding: 24px 20px 112px; place-items: start; }
  .search-form { grid-template-columns: 1fr; }
  .vy-btn, .refresh-button { width: 100%; }
}
</style>
