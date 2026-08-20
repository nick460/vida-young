import { defineStore } from "pinia";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import {
  listarMias,
  contarNoLeidas,
  marcarLeida,
  marcarTodasLeidas
} from "../services/notificacionService.js";

const API_URL = import.meta.env.VITE_API_URL || "";

export const useNotificacionesStore = defineStore("notificaciones", {
  state: () => ({
    notificaciones: [],
    noLeidas: 0,
    conectado: false,
    ultimaNotificacion: null,
    client: null
  }),
  actions: {
    async cargar() {
      try {
        const [lista, count] = await Promise.all([listarMias(), contarNoLeidas()]);
        this.notificaciones = lista;
        this.noLeidas = Number(count || 0);
      } catch (error) {
        this.notificaciones = [];
        this.noLeidas = 0;
      }
    },
    conectar() {
      if (this.client) {
        return;
      }

      const token = localStorage.getItem("vy_token");
      if (!token) {
        return;
      }

      const client = new Client({
        webSocketFactory: () =>
          new SockJS(`${API_URL}/ws-notificaciones?token=${encodeURIComponent(token)}`),
        reconnectDelay: 5000,
        onConnect: () => {
          this.conectado = true;
          client.subscribe("/user/queue/notificaciones", (message) =>
            this.recibir(JSON.parse(message.body))
          );
          client.subscribe("/topic/notificaciones", (message) =>
            this.recibir(JSON.parse(message.body))
          );
        },
        onWebSocketClose: () => {
          this.conectado = false;
        }
      });

      client.activate();
      this.client = client;
    },
    desconectar() {
      if (this.client) {
        this.client.deactivate();
        this.client = null;
      }
      this.conectado = false;
    },
    recibir(notificacion) {
      const existe = this.notificaciones.some((item) => item.id === notificacion.id);
      if (!existe) {
        this.notificaciones.unshift(notificacion);
        if (!notificacion.leida) {
          this.noLeidas += 1;
        }
      }
      this.ultimaNotificacion = notificacion;
    },
    async marcarLeida(id) {
      await marcarLeida(id);
      const notificacion = this.notificaciones.find((item) => item.id === id);
      if (notificacion && !notificacion.leida) {
        notificacion.leida = true;
        notificacion.fechaLeida = new Date().toISOString();
        this.noLeidas = Math.max(0, this.noLeidas - 1);
      }
    },
    async marcarTodasLeidas() {
      const marcadas = await marcarTodasLeidas();
      this.notificaciones.forEach((notificacion) => {
        notificacion.leida = true;
        notificacion.fechaLeida = notificacion.fechaLeida || new Date().toISOString();
      });
      this.noLeidas = 0;
      return Number(marcadas || 0);
    }
  }
});