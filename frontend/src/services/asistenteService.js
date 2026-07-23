import { apiRequest } from "./api.js";

export function sendAssistantMessage(message, history = []) {
  return apiRequest("/api/asistente/chat", {
    method: "POST",
    body: JSON.stringify({
      message,
      history: history.map((item) => ({
        role: item.role,
        text: item.text
      }))
    })
  });
}
