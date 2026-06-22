import { PRODUCTS } from "../data.js";
import { apiRequest } from "./api.js";

const DEFAULT_GRADIENT = "linear-gradient(135deg, #F2E7C4 0%, #F28705 100%)";

export function mapProduct(producto) {
  return {
    id: producto.id,
    sku: producto.sku || `VY-${producto.id}`,
    name: producto.nombre || producto.name,
    cat: producto.categoria || producto.cat || "General",
    price: Number(producto.precio ?? producto.price ?? 0),
    pv: Number(producto.pv || 0),
    qp: Number(producto.qp || 0),
    old: producto.old || null,
    rating: producto.rating || 4.8,
    badge: producto.badge || null,
    img: producto.imagenUrl || producto.img || DEFAULT_GRADIENT,
    listarEnShop: Boolean(producto.listarEnShop ?? true),
    descripcion: producto.descripcion || ""
  };
}

export async function loadProductCatalog({ onlyAvailable = false, fallbackToMock = true } = {}) {
  try {
    const data = await apiRequest("/api/public/productos");
    return data.map(mapProduct);
  } catch (error) {
    try {
      const data = await apiRequest("/api/productos");
      return data.map(mapProduct);
    } catch {
      if (!fallbackToMock) {
        throw error;
      }

      return PRODUCTS.map(mapProduct);
    }
  }
}
