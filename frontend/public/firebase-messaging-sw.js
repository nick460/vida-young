/* Service Worker de Firebase Cloud Messaging
   Los service workers clasicos no soportan "import", se usa importScripts con el SDK compat */
importScripts("https://www.gstatic.com/firebasejs/10.14.1/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/10.14.1/firebase-messaging-compat.js");

const firebaseConfig = {
  apiKey: "AIzaSyCF8J9BEKa1YWAL68Hl0jHx8wLzgpbeJEE",
  authDomain: "vida-young-7f137.firebaseapp.com",
  projectId: "vida-young-7f137",
  storageBucket: "vida-young-7f137.firebasestorage.app",
  messagingSenderId: "25043404550",
  appId: "1:25043404550:web:60f23bfc57e7b851dd0087",
  measurementId: "G-2QBG34HLGJ"
};

if (!firebase.apps.length) {
  firebase.initializeApp(firebaseConfig);
}

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  console.log("Mensaje FCM recibido en background:", payload);

  const data = payload.data || {};
  const notificationTitle = data.titulo || "Nueva notificacion";
  const notificationOptions = {
    body: data.mensaje || "Tienes una nueva notificacion",
    icon: "/icono.ico",
    badge: "/icono.ico",
    tag: "vy-notificacion-" + (data.tipo || "info"),
    data: {
      link: data.link || "/"
    }
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const link = (event.notification.data && event.notification.data.link) || "/";
  event.waitUntil(self.clients.openWindow(link));
});
