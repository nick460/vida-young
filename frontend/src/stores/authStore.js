import { defineStore } from "pinia";
import { login as loginRequest } from "../services/authService.js";
import { obtenerPerfil } from "../services/profileService.js";
import { solicitarPermisoYObtenerToken, registrarServiceWorker } from "../services/fcm-service.js";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    token: localStorage.getItem("vy_token") || "",
    usuario: JSON.parse(localStorage.getItem("vy_usuario") || "null"),
    fcmToken: localStorage.getItem("vy_fcm_token") || null,
    permisoNotificaciones: localStorage.getItem("vy_permiso") || null
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
      this.fcmToken = null;
      this.permisoNotificaciones = null;
      localStorage.removeItem("vy_token");
      localStorage.removeItem("vy_usuario");
      localStorage.removeItem("vy_fcm_token");
      localStorage.removeItem("vy_permiso");
    },
    async inicializarNotificaciones() {
      // Registrar service worker primero
      registrarServiceWorker();
      
      // Solicitar permiso y obtener token FCM
      const token = await solicitarPermisoYObtenerToken();
      if (token) {
        this.fcmToken = token;
        localStorage.setItem("vy_fcm_token", token);
      }
      const permiso = Notification.permission;
      this.permisoNotificaciones = permiso;
      localStorage.setItem("vy_permiso", permiso);
      
      // Vincular token al backend si hay usuario
      if (this.usuario?.id && this.fcmToken) {
        await this.vincularFCMTokenAlBackend(token, this.usuario.id);
      }
    },
    async vincularFCMTokenAlBackend() {
      if (!this.fcmToken || !this.usuario?.id) {
        console.log("⚠️ No hay token FCM o usuario ID para vincular");
        return;
      }
      
      try {
        const response = await fetch("http://localhost:9095/api/dispositivos/vincular", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${this.token}`
          },
          body: JSON.stringify({
            token: this.fcmToken,
            personaId: this.usuario.id
          })
        });
        
        const data = await response.json();
        console.log("✅ Dispositivo vinculado:", data);
        return data;
      } catch (error) {
        console.error("❌ Error vinculando token FCM:", error);
        return null;
      }
    }
  }
});