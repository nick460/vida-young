import { initializeApp } from "firebase/app";
import { getMessaging, onBackgroundMessage } from "firebase/messaging";

// Configuración Firebase - misma que en el frontend
const firebaseConfig = {
  apiKey: "AIzaSyCF8J9BEKa1YWAL68Hl0jHx8wLzgpbeJEE",
  authDomain: "vida-young-7f137.firebaseapp.com",
  projectId: "vida-young-7f137",
  storageBucket: "vida-young-7f137.firebasestorage.app",
  messagingSenderId: "25043404550",
  appId: "1:25043404550:web:60f23bfc57e7b851dd0087",
  measurementId: "G-2QBG34HLGJ"
};

// Inicializar Firebase en el service worker
const app = initializeApp(firebaseConfig);
const messaging = getMessaging(app);

// Escuchar mensajes entrantes en segundo plano
self.addEventListener("backgroundmessage", (payload) => {
  console.log("📲 Mensaje FCM recibido en background:", payload);

  const notificationTitle = payload.notification?.title || "Nueva notificación";
  const notificationOptions = {
    body: payload.notification?.body || "Tienes una nueva notificación",
    icon: "/icono.ico",
    data: {
      link: payload.notification?.link || "/"
    }
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});

// Opcional: manejar notificaciones en foreground cuando el SW está activo
self.addEventListener("message", (event) => {
  const data = event.data;
  if (data && data.type === "firebase-messaging-sync") {
    // Manejo de sincronización posterior si es necesario
  }
});