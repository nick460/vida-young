import { apiRequest } from "./api.js";

export const LANDING_SECTION_TYPES = [
  { value: "hero", label: "Hero principal" },
  { value: "benefits", label: "Beneficios" },
  { value: "text", label: "Texto" },
  { value: "imageText", label: "Imagen y texto" },
  { value: "videoText", label: "Video y texto" },
  { value: "gallery", label: "Galeria" },
  { value: "carousel", label: "Carrusel" },
  { value: "social", label: "Redes sociales" },
  { value: "contact", label: "Contacto personalizado" },
  { value: "preguntas", label: "Preguntas frecuentes" }
];

export function slugify(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

export async function loadProductLandingConfig(product, { publicEndpoint = false } = {}) {
  if (!product?.id) {
    return getProductLandingDefaults(product);
  }

  try {
    const path = publicEndpoint ? `/api/public/productos/${product.id}/landing` : `/api/productos/${product.id}/landing`;
    const config = await apiRequest(path);
    return mergeProductLandingConfig(product, config);
  } catch {
    return getProductLandingDefaults(product);
  }
}

export async function saveProductLandingConfig(productId, config) {
  const sections = Array.isArray(config.sections) ? config.sections.map(normalizeLandingSection) : [];
  const hero = sections.find((section) => section.type === "hero") || {};
  const story = sections.find((section) => ["imageText", "text"].includes(section.type)) || {};
  const benefits = sections.find((section) => section.type === "benefits")?.images || [];
  const gallery = sections.find((section) => ["gallery", "carousel"].includes(section.type))?.images || [];

  const saved = await apiRequest(`/api/productos/${productId}/landing`, {
    method: "PUT",
    body: JSON.stringify({
      headline: hero.title || config.headline || "",
      subtitle: hero.text || config.subtitle || "",
      story: story.text || config.story || "",
      usage: config.usage || "",
      ingredients: config.ingredients || "",
      benefits: benefits.length ? benefits : (Array.isArray(config.benefits) ? config.benefits.filter(Boolean) : []),
      gallery: gallery.length ? gallery : (Array.isArray(config.gallery) ? config.gallery.filter(Boolean) : []),
      sections,
      shareMessage: config.shareMessage || ""
    })
  });

  return mergeProductLandingConfig(saved.producto || saved.product || {}, saved);
}

export function getProductLandingDefaults(product) {
  const description = product?.descripcion || "Producto Vidayoung disponible con asesoria personalizada.";

  return {
    headline: `${product?.name || "Producto Vidayoung"} para tu rutina diaria`,
    subtitle: description,
    story: description,
    usage: "Consulta con tu referido la forma recomendada de uso y disponibilidad actual.",
    ingredients: "Informacion configurable por producto.",
    benefits: [],
    gallery: [product?.img].filter(Boolean),
    shareMessage: getDefaultShareMessage(product),
    sections: [
      {
        type: "hero",
        title: `${product?.name || "Producto Vidayoung"} para tu rutina diaria`,
        text: description,
        imageUrl: product?.img || "",
        images: [],
        buttonText: "Comprar ahora",
        whatsappMessage: `Hola, quiero informacion sobre ${product?.name || "este producto Vidayoung"}.`,
        layout: "imageRight"
      }
    ],
    updatedAt: ""
  };
}

export function mergeProductLandingConfig(product, config = {}) {
  const defaults = getProductLandingDefaults(product);

  return {
    headline: config.headline || defaults.headline,
    subtitle: config.subtitle || defaults.subtitle,
    story: config.story || defaults.story,
    usage: config.usage || defaults.usage,
    ingredients: config.ingredients || defaults.ingredients,
    benefits: config.benefits?.length ? config.benefits : defaults.benefits,
    gallery: config.gallery?.length ? config.gallery : defaults.gallery,
    shareMessage: config.shareMessage || defaults.shareMessage,
    sections: buildLandingSections(defaults, config),
    updatedAt: config.updatedAt || ""
  };
}

export function getDefaultShareMessage(product) {
  return `Te comparto algo que me esta cambiando el ritual del cafe.

Es {producto} — una experiencia funcional premium inspirada en la Amazonia que NO es solo un producto comun, sino una experiencia completa con ingredientes pensados para:

✅ Energia equilibrada
✅ Enfoque consciente
✅ Bienestar diario

De la Amazonia a tu mesa.

Entra aqui y descubre la experiencia:`;
}

function buildLandingSections(defaults, config = {}) {
  const incoming = config.sections?.length ? config.sections.map(normalizeLandingSection) : [];

  if (!incoming.length) {
    return defaults.sections.map(normalizeLandingSection);
  }

  const sections = [...incoming];
  const hasType = (type) => sections.some((section) => section.type === type);

  if (!hasType("hero")) {
    sections.unshift(normalizeLandingSection({
      type: "hero",
      title: config.headline || defaults.headline,
      text: config.subtitle || defaults.subtitle,
      imageUrl: config.gallery?.[0] || defaults.gallery?.[0] || "",
      buttonText: "Comprar ahora",
      whatsappMessage: "",
      layout: "imageRight"
    }));
  }

  return sections;
}

export function normalizeLandingSection(section = {}) {
  return {
    type: section.type || "imageText",
    category: section.category || "",
    title: section.title || "",
    text: section.text || "",
    imageUrl: section.imageUrl || "",
    images: Array.isArray(section.images) ? section.images.filter(Boolean) : [],
    buttonText: section.buttonText || "",
    whatsappMessage: section.whatsappMessage || "",
    layout: section.layout || defaultLayoutForSection(section.type)
  };
}

function defaultLayoutForSection(type) {
  if (type === "hero" || type === "imageText" || type === "videoText") {
    return "imageRight";
  }
  if (type === "benefits" || type === "gallery") {
    return "grid3";
  }
  if (type === "carousel") {
    return "carousel";
  }
  if (type === "social") {
    return "grid3";
  }
  if (type === "contact") {
    return "cardCentered";
  }
  if (type === "preguntas") {
    return "darkAccordion";
  }
  return "centered";
}

export function buildReferralData(usuario) {
  const persona = usuario?.persona || {};
  const name = [persona.nombres, persona.apellidos].filter(Boolean).join(" ").trim() || usuario?.username || "Asesor Vidayoung";
  return {
    ref: slugify(usuario?.username || usuario?.id || name || "vidayoung"),
    name,
    phone: persona.telefono || "",
    email: persona.email || "",
    user: usuario?.username || "",
    photo: usuario?.fotoPerfil || persona.fotoPerfil || persona.fotoUrl || ""
  };
}

export function encodeReferralQuery(referral) {
  const params = new URLSearchParams();
  Object.entries(referral).forEach(([key, value]) => {
    if (value) {
      params.set(key, value);
    }
  });
  return params.toString();
}

export function readReferralFromRoute(route) {
  return {
    ref: route.query.ref || route.params.ref || "",
    name: route.query.name || "Asesor Vidayoung",
    phone: route.query.phone || "",
    email: route.query.email || "",
    user: route.query.user || "",
    photo: route.query.photo || ""
  };
}

export async function resolveReferralFromRoute(route) {
  const fallback = readReferralFromRoute(route);
  const username = String(route.params.ref || route.query.user || route.query.ref || "").trim();

  if (!username || route.query.name || route.query.phone || route.query.email || route.query.photo) {
    return fallback;
  }

  try {
    const advisor = await apiRequest(`/api/public/tiendas/${encodeURIComponent(username)}`);
    return {
      ref: username,
      user: advisor?.username || username,
      name: [advisor?.nombres, advisor?.apellidos].filter(Boolean).join(" ").trim() || fallback.name,
      phone: advisor?.telefono || "",
      email: advisor?.email || "",
      photo: advisor?.fotoPerfil || "",
    };
  } catch {
    return fallback;
  }
}
