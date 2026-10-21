import { getMessaging, getToken, onMessage } from "firebase/messaging";
import { initializeApp } from "firebase/app";

// Configuración Firebase
const firebaseConfig = {
  apiKey: "AIzaSyCF8J9BEKa1YWAL68Hl0jHx8wLzgpbeJEE",
  authDomain: "vida-young-7f137.firebaseapp.com",
  projectId: "vida-young-7f137",
  storageBucket: "vida-young-7f137.firebasestorage.app",
  messagingSenderId: "25043404550",
  appId: "1:25043404550:web:60f23bfc57e7b851dd0087",
  measurementId: "G-2QBG34HLGJ"
};

const app = initializeApp(firebaseConfig);
const messaging = getMessaging(app);
let serviceWorkerRegistration;

// Solicitar permiso y obtener token FCM
export async function solicitarPermisoYObtenerToken() {
  try {
    const permiso = await Notification.requestPermission();
    if (permiso === "granted") {
      let registro = null;
      try {
        registro = await navigator.serviceWorker.ready;
      } catch (e) {
        console.warn("⚠️ Service Worker no disponible:", e);
      }
      try {
        const suscripcion = registro ? await registro.pushManager.getSubscription() : null;
        console.info("FCM: solicitando token", {
          permission: Notification.permission,
          secureContext: window.isSecureContext,
          serviceWorker: registro ? {
            scope: registro.scope,
            active: registro.active?.state || null,
            waiting: registro.waiting?.state || null,
            existingPushSubscription: Boolean(suscripcion),
            pushEndpoint: suscripcion?.endpoint ? new URL(suscripcion.endpoint).origin : null
          } : null
        });

        const token = await getToken(messaging, {
          vapidKey: "BFNiZSzfqIJOpcdnJ7dSojccHc0CJcifiDyt6aJxexoMW1Aecgol-TK_TOrfW_thlpWwz--YeJGog6Ne0b3-pzc",
          ...(registro ? { serviceWorkerRegistration: registro } : {})
        });
        console.info("FCM: token obtenido", { length: token?.length || 0, prefix: token?.slice(0, 12) || null });
        return token;
      } catch (error) {
        console.error("FCM: no se pudo registrar la suscripción Push", {
          name: error?.name,
          message: error?.message,
          code: error?.code || null,
          permission: Notification.permission,
          secureContext: window.isSecureContext,
          serviceWorkerScope: registro?.scope || null,
          activeServiceWorker: registro?.active?.state || null
        }, error);
        return null;
      }
    } else if (permiso === "denied") {
      console.log("⚠️ Permiso de notificación denegado");
      return null;
    } else {
      console.log("ℹ️ El usuario aún no ha decidido sobre el permiso");
      return null;
    }
  } catch (error) {
    console.error("FCM: no se pudo solicitar el permiso de notificaciones", error);
    return null;
  }
}

// Obtener token actual (si ya hay permiso)
export function obtenerTokenActual() {
  return getToken(messaging, {
    vapidKey: "BFNiZSzfqIJOpcdnJ7dSojccHc0CJcifiDyt6aJxexoMW1Aecgol-TK_TOrfW_thlpWwz--YeJGog6Ne0b3-pzc"
  }).then(token => token || null).catch(() => null);
}

// Escuchar mensajes en foreground
export function onMensajeForeground(callback) {
  onMessage(app, callback);
}

// Registrar el service worker en el navegador
export function registrarServiceWorker() {
  if (serviceWorkerRegistration) {
    return serviceWorkerRegistration;
  }
  if ("serviceWorker" in navigator) {
    serviceWorkerRegistration = navigator.serviceWorker.register("/firebase-messaging-sw.js").then((reg) => {
      console.log("✅ Service Worker registrado:", reg.scope);
      return reg;
    }).catch((err) => {
      serviceWorkerRegistration = null;
      console.warn("⚠️ No se pudo registrar el Service Worker:", err);
      return null;
    });
    return serviceWorkerRegistration;
  }
  return Promise.resolve(null);
}
