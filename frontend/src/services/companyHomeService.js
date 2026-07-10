import { apiRequest } from "./api.js";
import { normalizeLandingSection } from "./productLandingService.js";

export const COMPANY_HOME_SLUG = "pagina-principal";

export function getCompanyHomeDefaults(config = {}) {
  const title = config.title || "Vidayoung";
  const description = config.description || "Bienestar, comunidad y productos funcionales para acompanar tu crecimiento diario.";

  return {
    id: config.id || null,
    slug: COMPANY_HOME_SLUG,
    title,
    category: config.category || "Empresa",
    imageUrl: config.imageUrl || "",
    description,
    shareMessage: config.shareMessage || "",
    sections: config.sections?.length ? config.sections.map(normalizeLandingSection) : defaultHomeSections(title, description, config.imageUrl),
    updatedAt: config.updatedAt || ""
  };
}

export function defaultHomeSections(title, description, imageUrl = "") {
  return [
    normalizeLandingSection({
      type: "hero",
      title,
      text: description,
      imageUrl,
      buttonText: "Iniciar sesion",
      layout: "imageRight"
    }),
    normalizeLandingSection({
      type: "text",
      title: "Una empresa construida alrededor del bienestar",
      text: "Creamos una experiencia que conecta productos, tecnologia y acompanamiento para personas que quieren vivir mejor y construir una red con proposito.",
      layout: "centered"
    }),
    normalizeLandingSection({
      type: "benefits",
      title: "Lo que encuentras en Vidayoung",
      text: "Un ecosistema simple para descubrir productos, registrarte, comprar y hacer seguimiento a tu crecimiento.",
      images: [
        "Productos funcionales con informacion clara.",
        "Herramientas digitales para compartir y educar.",
        "Una comunidad enfocada en bienestar y crecimiento."
      ],
      layout: "grid3"
    }),
    normalizeLandingSection({
      type: "gallery",
      title: "Productos destacados",
      text: "Agrega imagenes o enlaces de productos para mostrarlos en la pagina principal.",
      images: [],
      layout: "grid3"
    }),
    normalizeLandingSection({
      type: "preguntas",
      title: "Preguntas frecuentes",
      images: [
        "¿Como puedo comprar?|||Inicia sesion o contacta a un asesor para recibir orientacion.",
        "¿Puedo formar parte de la comunidad?|||Si. Un asesor puede explicarte los planes y primeros pasos.",
        "¿La pagina se puede editar?|||Si. El administrador puede configurar secciones, textos, imagenes y redes."
      ],
      layout: "darkAccordion"
    }),
    normalizeLandingSection({
      type: "social",
      title: "Redes sociales",
      text: "Siguenos y conoce novedades, contenidos y experiencias de la comunidad.",
      images: [
        "Instagram|||https://www.instagram.com/",
        "Facebook|||https://www.facebook.com/",
        "TikTok|||https://www.tiktok.com/"
      ],
      layout: "grid3"
    })
  ];
}

export async function loadCompanyHome() {
  try {
    const landing = await apiRequest(`/api/public/digital-landings/${COMPANY_HOME_SLUG}`);
    return getCompanyHomeDefaults(landing);
  } catch {
    return getCompanyHomeDefaults();
  }
}

export async function loadCompanyHomeConfig() {
  try {
    const landings = await apiRequest("/api/digital-landings");
    const home = landings.find((landing) => landing.slug === COMPANY_HOME_SLUG);
    return getCompanyHomeDefaults(home);
  } catch {
    return getCompanyHomeDefaults();
  }
}

export async function saveCompanyHomeConfig(config) {
  const current = await loadCompanyHomeConfig();
  const payload = {
    slug: COMPANY_HOME_SLUG,
    title: config.title,
    category: config.category || "Empresa",
    imageUrl: config.imageUrl,
    description: config.description,
    sections: Array.isArray(config.sections) ? config.sections.map(normalizeLandingSection) : [],
    shareMessage: config.shareMessage || ""
  };

  const saved = await apiRequest(current.id ? `/api/digital-landings/${current.id}` : "/api/digital-landings", {
    method: current.id ? "PUT" : "POST",
    body: JSON.stringify(payload)
  });

  return getCompanyHomeDefaults(saved);
}
