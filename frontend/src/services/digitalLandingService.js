import { apiRequest } from "./api.js";
import { normalizeLandingSection, slugify } from "./productLandingService.js";

export function getDefaultDigitalShareMessage(topic) {
  return `Te comparto esta guia de {tema}.

Es una pagina corta para entender ideas clave y dar el siguiente paso con acompanamiento.

Entra aqui:`;
}

export function getDigitalLandingDefaults(topic = {}) {
  const title = topic.title || "Nuevo tema";
  const description = topic.description || "Contenido configurable para compartir con clientes, referidos o equipo.";

  return {
    id: topic.id || null,
    slug: topic.slug || slugify(title),
    title,
    category: topic.category || "Tema",
    imageUrl: topic.imageUrl || "",
    description,
    shareMessage: topic.shareMessage || getDefaultDigitalShareMessage(topic),
    sections: topic.sections?.length ? topic.sections.map(normalizeLandingSection) : [
      normalizeLandingSection({
        type: "hero",
        title,
        text: description,
        imageUrl: topic.imageUrl || "",
        buttonText: "Quiero saber mas",
        whatsappMessage: `Hola, quiero informacion sobre ${title}.`,
        layout: "imageRight"
      }),
      normalizeLandingSection({
        type: "benefits",
        title: "Puntos clave",
        text: "Ideas principales para esta landing.",
        images: [
          "Concepto central explicado de forma simple.",
          "Aplicacion practica para tomar mejores decisiones.",
          "Acompanamiento personalizado para avanzar."
        ],
        layout: "grid3"
      }),
      normalizeLandingSection({
        type: "contact",
        title: "Conversemos",
        text: "Te explico los detalles y resolvemos tus preguntas.",
        images: ["Informacion referencial. La decision final depende de tus objetivos y contexto."],
        buttonText: "Escribeme ahora",
        whatsappMessage: `Hola, quiero informacion sobre ${title}.`,
        layout: "cardCentered"
      })
    ],
    updatedAt: topic.updatedAt || ""
  };
}

export async function loadDigitalLandings() {
  const landings = await apiRequest("/api/digital-landings");
  return landings.map(getDigitalLandingDefaults);
}

export async function loadPublicDigitalLanding(slug) {
  const landing = await apiRequest(`/api/public/digital-landings/${slug}`);
  return getDigitalLandingDefaults(landing);
}

export async function saveDigitalLandingConfig(topic) {
  const payload = {
    slug: topic.slug,
    title: topic.title,
    category: topic.category,
    imageUrl: topic.imageUrl,
    description: topic.description,
    sections: Array.isArray(topic.sections) ? topic.sections.map(normalizeLandingSection) : [],
    shareMessage: topic.shareMessage || ""
  };

  const saved = await apiRequest(topic.id ? `/api/digital-landings/${topic.id}` : "/api/digital-landings", {
    method: topic.id ? "PUT" : "POST",
    body: JSON.stringify(payload)
  });

  return getDigitalLandingDefaults(saved);
}

export async function deleteDigitalLanding(id) {
  return apiRequest(`/api/digital-landings/${id}`, { method: "DELETE" });
}
