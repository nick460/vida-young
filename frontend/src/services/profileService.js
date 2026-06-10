import { apiRequest } from "./api.js";

export function obtenerPerfil() {
  return apiRequest("/api/auth/me");
}

export function subirFotoPerfil(file) {
  const formData = new FormData();
  formData.append("foto", file);

  return apiRequest("/api/auth/me/foto", {
    method: "POST",
    body: formData
  });
}
