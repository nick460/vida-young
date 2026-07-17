const PUBLIC_CART_KEY = "vy_public_cart_items";
const PUBLIC_CART_STORE_KEY = "vy_public_cart_store";

export function readPublicCart(username = "") {
  try {
    const store = localStorage.getItem(PUBLIC_CART_STORE_KEY) || "";
    if (store !== username) {
      return [];
    }
    const items = JSON.parse(localStorage.getItem(PUBLIC_CART_KEY) || "[]");
    return Array.isArray(items) ? items : [];
  } catch {
    return [];
  }
}

export function writePublicCart(username, items) {
  localStorage.setItem(PUBLIC_CART_STORE_KEY, username || "");
  localStorage.setItem(PUBLIC_CART_KEY, JSON.stringify(items));
  window.dispatchEvent(new Event("vy-public-cart-updated"));
}

export function addPublicCartItem(username, product, quantity = 1) {
  const items = readPublicCart(username);
  const productId = Number(product.id);
  const existing = items.find((item) => Number(item.id) === productId);

  if (existing) {
    existing.quantity = Number(existing.quantity || 0) + Number(quantity || 1);
  } else {
    items.push({
      id: productId,
      sku: product.sku,
      name: product.nombre,
      cat: product.categoria,
      price: Number(product.precioFinal || 0),
      publicPrice: Number(product.precioPublico || 0),
      distributorPrice: Number(product.precioDistribuidor || 0),
      discount: Number(product.descuento || 0),
      img: product.imagenPublicaUrl || product.imagenUrl,
      quantity: Number(quantity || 1)
    });
  }

  writePublicCart(username, items);
  return items;
}

export function clearPublicCart(username) {
  writePublicCart(username, []);
}

export function publicCartCount(items = []) {
  return items.reduce((total, item) => total + Number(item.quantity || 0), 0);
}
