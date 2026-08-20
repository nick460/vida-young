import { apiRequest } from "./api.js";

export function listarMias() {
  return apiRequest("/api/notificaciones");
}

export function contarNoLeidas() {
  return apiRequest("/api/notificaciones/no-leidas");
}

export function marcarLeida(id) {
  return apiRequest(`/api/notificaciones/${id}/leida`, { method: "POST" });
}

export function marcarTodasLeidas() {
  return apiRequest("/api/notificaciones/marcar-todas-leidas", { method: "POST" });
}

export function enviar(payload) {
  return apiRequest("/api/notificaciones/enviar", {
    method: "POST",
    body: JSON.stringify(payload)
  });
}

export const notificacionService = {
  listarMias,
  contarNoLeidas,
  marcarLeida,
  marcarTodasLeidas,
  enviar
};