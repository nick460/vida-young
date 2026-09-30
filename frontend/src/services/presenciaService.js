import { apiRequest } from "./api.js";

export async function enviarLatido(rutaActual = "") {
  try {
    await apiRequest("/api/presencia/latido", {
      method: "POST",
      body: JSON.stringify({ rutaActual: String(rutaActual || "").slice(0, 200) })
    });
  } catch {
    // Silencioso: no interrumpir la navegación si falla el latido
  }
}

export async function obtenerEnLinea(minutos = 2) {
  return apiRequest(`/api/presencia/online?minutos=${Number(minutos) || 2}`);
}

export async function contarEnLinea(minutos = 2) {
  return apiRequest(`/api/presencia/count?minutos=${Number(minutos) || 2}`);
}

export async function marcarSalida() {
  try {
    await apiRequest("/api/presencia/salir", { method: "DELETE" });
  } catch {
    // Silencioso
  }
}
