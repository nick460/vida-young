<script setup>
import { computed, nextTick, ref } from "vue";
import { Bot, Loader2, RotateCcw, Send, User } from "lucide-vue-next";
import { sendAssistantMessage } from "../services/asistenteService.js";

const input = ref("");
const loading = ref(false);
const error = ref("");
const messages = ref([
  {
    role: "model",
    text: "Hola, soy el asistente de Vidayoung. Preguntame lo que necesites.",
    local: true
  }
]);
const chatBody = ref(null);

const canSend = computed(() => input.value.trim().length > 0 && !loading.value);

function scrollToBottom() {
  nextTick(() => {
    if (chatBody.value) {
      chatBody.value.scrollTop = chatBody.value.scrollHeight;
    }
  });
}

function resetChat() {
  error.value = "";
  input.value = "";
  messages.value = [
    {
      role: "model",
      text: "Hola, soy el asistente de Vidayoung. Preguntame lo que necesites.",
      local: true
    }
  ];
  scrollToBottom();
}

async function submitMessage() {
  const message = input.value.trim();

  if (!message || loading.value) {
    return;
  }

  error.value = "";
  input.value = "";
  messages.value.push({ role: "user", text: message });
  loading.value = true;
  scrollToBottom();

  try {
    const history = messages.value
      .filter((item) => !item.local)
      .slice(-20);
    const response = await sendAssistantMessage(message, history.slice(0, -1));
    messages.value.push({ role: "model", text: response.text });
  } catch (exception) {
    error.value = exception.message || "No se pudo conectar con el asistente.";
  } finally {
    loading.value = false;
    scrollToBottom();
  }
}
</script>

<template>
  <div class="vy assistant-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Asistente</div>
          <h1>Asistente Vidayoung</h1>
          <p>Conversa con Gemini desde la plataforma.</p>
        </div>
        <button class="vy-btn vy-btn-ghost" type="button" @click="resetChat">
          <RotateCcw :size="15" />
          Reiniciar
        </button>
      </header>

      <section class="assistant-shell">
        <div ref="chatBody" class="chat-body" aria-live="polite">
          <article
            v-for="(message, index) in messages"
            :key="index"
            class="message-row"
            :class="message.role === 'user' ? 'from-user' : 'from-model'"
          >
            <div class="message-avatar">
              <User v-if="message.role === 'user'" :size="18" />
              <Bot v-else :size="18" />
            </div>
            <div class="message-bubble">
              <span>{{ message.role === "user" ? "Tu" : "Asistente" }}</span>
              <p>{{ message.text }}</p>
            </div>
          </article>

          <article v-if="loading" class="message-row from-model">
            <div class="message-avatar">
              <Bot :size="18" />
            </div>
            <div class="message-bubble thinking">
              <span>Asistente</span>
              <p><Loader2 :size="16" class="spin" /> Procesando...</p>
            </div>
          </article>
        </div>

        <div v-if="error" class="error-box">{{ error }}</div>

        <form class="composer" @submit.prevent="submitMessage">
          <textarea
            v-model="input"
            rows="2"
            placeholder="Escribe tu mensaje..."
            :disabled="loading"
            @keydown.enter.exact.prevent="submitMessage"
          ></textarea>
          <button class="send-button" type="submit" :disabled="!canSend" aria-label="Enviar mensaje">
            <Loader2 v-if="loading" :size="18" class="spin" />
            <Send v-else :size="18" />
          </button>
        </form>
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace { min-width: 0; min-height: 100vh; padding: 28px 32px 24px; display: flex; flex-direction: column; box-sizing: border-box; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.page-header h1 { margin-top: 8px; font-size: 30px; font-weight: 900; }
.page-header p { margin-top: 4px; color: var(--vy-ink-2); font-size: 14px; }
.page-header .vy-btn { min-height: 40px; padding: 10px 16px; border-radius: 12px; font-weight: 800; }
.assistant-shell { flex: 1; min-height: 0; display: flex; flex-direction: column; border: 1px solid var(--vy-line); border-radius: 8px; background: var(--vy-surface); overflow: hidden; box-shadow: 0 18px 44px rgba(31, 26, 20, 0.08); }
.chat-body { flex: 1; min-height: 0; overflow-y: auto; padding: 22px; display: flex; flex-direction: column; gap: 14px; background: linear-gradient(180deg, #fff 0%, #fffaf0 100%); }
.message-row { display: flex; gap: 10px; max-width: min(760px, 92%); }
.from-user { align-self: flex-end; flex-direction: row-reverse; }
.from-model { align-self: flex-start; }
.message-avatar { width: 36px; height: 36px; border-radius: 8px; display: inline-flex; align-items: center; justify-content: center; flex-shrink: 0; background: var(--vy-ink); color: #fff; }
.from-user .message-avatar { background: var(--vy-orange); }
.message-bubble { padding: 12px 14px; border-radius: 8px; background: #fff; border: 1px solid rgba(31, 26, 20, 0.08); box-shadow: 0 10px 24px rgba(31, 26, 20, 0.06); }
.from-user .message-bubble { background: var(--vy-ink); color: #fff; }
.message-bubble span { display: block; margin-bottom: 5px; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.from-user .message-bubble span { color: rgba(255, 255, 255, 0.72); }
.message-bubble p { white-space: pre-wrap; line-height: 1.55; font-size: 14px; margin: 0; }
.thinking p { display: inline-flex; align-items: center; gap: 8px; color: var(--vy-ink-2); font-weight: 800; }
.error-box { margin: 0 16px 12px; padding: 12px 14px; border-radius: 8px; background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); font-size: 13px; font-weight: 800; }
.composer { flex-shrink: 0; display: grid; grid-template-columns: 1fr 46px; gap: 10px; padding: 14px; border-top: 1px solid var(--vy-line); background: #fff; }
.composer textarea { width: 100%; resize: none; border: 1px solid var(--vy-line); border-radius: 8px; padding: 12px 14px; color: var(--vy-ink); font: inherit; line-height: 1.4; outline: none; box-sizing: border-box; }
.composer textarea:focus { border-color: var(--vy-orange); box-shadow: 0 0 0 3px rgba(242, 135, 5, 0.12); }
.send-button { width: 46px; height: 46px; border-radius: 8px; display: inline-flex; align-items: center; justify-content: center; align-self: end; background: var(--vy-orange); color: #fff; transition: background 0.16s ease, transform 0.16s ease; }
.send-button:hover:not(:disabled) { background: var(--vy-orange-deep); transform: translateY(-1px); }
.send-button:disabled { cursor: not-allowed; opacity: 0.55; }
.spin { animation: spin 0.9s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 860px) {
  .assistant-view { position: fixed; inset: 0 0 84px 0; z-index: 1; min-height: 0; overflow: hidden; background: var(--vy-bg); }
  .workspace { height: 100%; min-height: 0; padding: 12px 10px 8px !important; overflow: hidden; }
  .page-header { flex-shrink: 0; align-items: stretch; flex-direction: column; gap: 8px; margin-bottom: 10px; }
  .page-header h1 { margin-top: 4px; font-size: 24px !important; }
  .page-header p { margin-top: 2px; }
  .page-header .vy-btn { align-self: center; min-height: 38px; padding: 9px 13px; }
  .chat-body { padding: 16px; }
  .message-row { max-width: 100%; }
  .composer { padding: 12px; }
  .composer textarea { min-height: 46px; max-height: 92px; }
}
</style>
