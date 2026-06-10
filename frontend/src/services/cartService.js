const CART_STORAGE_KEY = "vy_cart_items";

export function readCartItems() {
  try {
    const items = JSON.parse(localStorage.getItem(CART_STORAGE_KEY) || "[]");
    return Array.isArray(items) ? items : [];
  } catch {
    return [];
  }
}

export function writeCartItems(items) {
  localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(items));
  window.dispatchEvent(new Event("vy-cart-updated"));
}

export function addCartItem(product, quantity = 1) {
  const items = readCartItems();
  const productId = Number(product.id);
  const existing = items.find((item) => Number(item.id) === productId);

  if (existing) {
    existing.quantity = Number(existing.quantity || 0) + Number(quantity || 1);
  } else {
    items.push({
      id: productId,
      sku: product.sku,
      name: product.name,
      cat: product.cat,
      price: Number(product.price || 0),
      pv: Number(product.pv || 0),
      qp: Number(product.qp || 0),
      stockDisponible: Number(product.stockDisponible || 0),
      img: product.img,
      quantity: Number(quantity || 1)
    });
  }

  writeCartItems(items);
  return items;
}

export function clearCart() {
  writeCartItems([]);
}

export function cartCount(items = readCartItems()) {
  return items.reduce((total, item) => total + Number(item.quantity || 0), 0);
}
