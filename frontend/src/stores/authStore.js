import { defineStore } from "pinia";
import { login as loginRequest } from "../services/authService.js";
import { obtenerPerfil } from "../services/profileService.js";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("vy_token") || "",
    usuario: JSON.parse(localStorage.getItem("vy_usuario") || "null")
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.token)
  },
  actions: {
    async login(username, password) {
      const response = await loginRequest(username, password);
      this.token = response.token;
      this.usuario = {
        id: response.usuarioId,
        username: response.username,
        roles: response.roles || []
      };
      localStorage.setItem("vy_token", this.token);
      localStorage.setItem("vy_usuario", JSON.stringify(this.usuario));
      return response;
    },
    async cargarPerfil() {
      const perfil = await obtenerPerfil();
      this.usuario = {
        ...(this.usuario || {}),
        id: perfil.usuarioId,
        username: perfil.username,
        activo: perfil.activo,
        roles: perfil.roles || [],
        persona: perfil.persona,
        referido: perfil.referido,
        fotoPerfil: perfil.fotoPerfil
      };
      localStorage.setItem("vy_usuario", JSON.stringify(this.usuario));
      return perfil;
    },
    actualizarPerfil(perfil) {
      this.usuario = {
        ...(this.usuario || {}),
        id: perfil.usuarioId,
        username: perfil.username,
        activo: perfil.activo,
        roles: perfil.roles || [],
        persona: perfil.persona,
        referido: perfil.referido,
        fotoPerfil: perfil.fotoPerfil
      };
      localStorage.setItem("vy_usuario", JSON.stringify(this.usuario));
    },
    logout() {
      this.token = "";
      this.usuario = null;
      localStorage.removeItem("vy_token");
      localStorage.removeItem("vy_usuario");
    }
  }
});
