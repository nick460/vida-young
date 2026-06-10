<script setup>
import { computed, onMounted, onUnmounted, ref } from "vue";
import { useRouter } from "vue-router";
import {
  Check,
  Heart,
  Plus,
  ShoppingCart
} from "lucide-vue-next";
import { VyProductImage } from "../components/ui.js";
import { apiRequest } from "../services/api.js";
import { addCartItem, cartCount, readCartItems } from "../services/cartService.js";

const router = useRouter();
const activeCategory = ref("Todos");
const productos = ref([]);
const loading = ref(false);
const error = ref("");
const cartItems = ref(readCartItems());
const recentlyAddedId = ref(null);

const categories = computed(() => ["Todos", ...new Set(productos.value.map((product) => product.cat).filter(Boolean))]);
const hero = computed(() => productos.value[0]);
const productList = computed(() => {
  if (activeCategory.value === "Todos") {
    return productos.value.slice(1);
  }

  return productos.value.filter((product) => product.cat === activeCategory.value);
});

function navigate(name) {
  router.push({ name });
}

function productGradient(product) {
  return product.img || "linear-gradient(135deg, #F2E7C4 0%, #F28705 100%)";
}

function mapProducto(producto) {
  return {
    id: producto.id,
    sku: producto.sku,
    name: producto.nombre,
    cat: producto.categoria || "General",
    price: Number(producto.precio || 0),
    pv: Number(producto.pv || 0),
    qp: Number(producto.qp || 0),
    old: null,
    badge: Number(producto.stockDisponible || 0) <= Number(producto.stockMinimo || 0) ? "Stock bajo" : null,
    img: producto.imagenUrl || "linear-gradient(135deg, #F2E7C4 0%, #F28705 100%)",
    stockDisponible: Number(producto.stockDisponible || 0),
    descripcion: producto.descripcion
  };
}

async function loadProducts() {
  loading.value = true;
  error.value = "";

  try {
    const data = await apiRequest("/api/productos");
    productos.value = data.map(mapProducto).filter((product) => product.stockDisponible > 0);
  } catch {
    productos.value = [];
    error.value = "No se pudieron cargar los productos reales desde inventario.";
  } finally {
    loading.value = false;
  }
}

function refreshCart() {
  cartItems.value = readCartItems();
}

function addToCart(product) {
  cartItems.value = addCartItem(product);
  recentlyAddedId.value = product.id;
  window.setTimeout(() => {
    if (recentlyAddedId.value === product.id) {
      recentlyAddedId.value = null;
    }
  }, 1200);
}

onMounted(loadProducts);
onMounted(() => window.addEventListener("vy-cart-updated", refreshCart));
onUnmounted(() => window.removeEventListener("vy-cart-updated", refreshCart));
</script>

<template>
  <div class="vy shop-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Tienda Vidayoung</div>
          <h1>Productos para tu bienestar</h1>
        </div>
      </header>

      <div v-if="loading" class="loading-box">Cargando productos...</div>
      <div v-if="error" class="error-box">{{ error }}</div>

      <section v-if="productos.length" class="category-row" aria-label="Categorías">
        <button
          v-for="category in categories"
          :key="category"
          type="button"
          :class="{ active: activeCategory === category }"
          @click="activeCategory = category"
        >
          {{ category }}
        </button>
      </section>

      <section v-if="hero" class="vy-card featured-product">
        <VyProductImage :grad="productGradient(hero)" :h="380" big />
        <div class="featured-info">
          <span class="vy-chip vy-chip-orange">{{ hero.badge }}</span>
          <small>{{ hero.cat }}</small>
          <h2>{{ hero.name }}</h2>
          <p>
            {{ hero.descripcion || "Producto Vidayoung registrado en inventario con disponibilidad calculada por lotes activos." }}
          </p>

          <div class="price-row">
            <strong>Bs. {{ hero.price.toLocaleString("es-BO") }}</strong>
            <span v-if="hero.old">Bs. {{ hero.old.toLocaleString("es-BO") }}</span>
            <b v-if="hero.old" class="vy-chip vy-chip-orange">−14%</b>
          </div>
          <p class="volume-line">PV {{ hero.pv.toLocaleString("es-BO") }} · QP {{ hero.qp.toLocaleString("es-BO") }}</p>

          <div class="featured-actions">
            <button
              class="add-cart-featured"
              :class="{ added: recentlyAddedId === hero.id }"
              type="button"
              @click="addToCart(hero)"
            >
              <span class="add-cart-icon">
                <Check v-if="recentlyAddedId === hero.id" :size="17" />
                <ShoppingCart v-else :size="17" />
              </span>
              <span>{{ recentlyAddedId === hero.id ? "Agregado al carrito" : "Agregar al carrito" }}</span>
            </button>
            <button class="favorite-button" type="button" aria-label="Favorito">
              <Heart :size="18" />
            </button>
          </div>
        </div>
      </section>

      <section class="product-grid">
        <article v-for="product in productList" :key="product.id" class="vy-card product-card">
          <div class="product-image">
            <VyProductImage :grad="productGradient(product)" :h="180" />
            <span v-if="product.badge" class="vy-chip vy-chip-ink">{{ product.badge }}</span>
            <button type="button" aria-label="Favorito">
              <Heart :size="15" />
            </button>
          </div>

          <div class="product-info">
            <small>{{ product.cat }}</small>
            <h3>{{ product.name }}</h3>

            <footer>
              <div>
                <strong>Bs. {{ product.price.toLocaleString("es-BO") }}</strong>
                <small>PV {{ product.pv.toLocaleString("es-BO") }} · QP {{ product.qp.toLocaleString("es-BO") }}</small>
                <span v-if="product.old">Bs. {{ product.old.toLocaleString("es-BO") }}</span>
              </div>
              <button
                type="button"
                class="add-cart-mini"
                :class="{ added: recentlyAddedId === product.id }"
                :aria-label="recentlyAddedId === product.id ? 'Agregado' : 'Agregar'"
                @click="addToCart(product)"
              >
                <Check v-if="recentlyAddedId === product.id" :size="16" stroke-width="2.5" />
                <Plus v-else :size="16" stroke-width="2.5" />
              </button>
            </footer>
          </div>
        </article>
      </section>

      <section v-if="!loading && !error && !productos.length" class="empty-shop vy-card">
        <ShoppingCart :size="28" />
        <h2>No hay productos disponibles</h2>
        <p>La tienda solo muestra productos reales con stock disponible en inventario.</p>
      </section>

      <button class="floating-cart" type="button" aria-label="Abrir carrito" @click="navigate('cart')">
        <ShoppingCart :size="22" stroke-width="2.2" />
        <span v-if="cartCount(cartItems)">{{ cartCount(cartItems) }}</span>
      </button>
    </main>
  </div>
</template>

<style scoped>
.workspace {
  padding: clamp(18px, 3vw, 32px) clamp(16px, 3vw, 32px) 96px;
  min-width: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 20px;
  margin-bottom: 20px;
}

.page-header h1 {
  font-size: 30px;
  font-weight: 800;
  margin-top: 8px;
}

.page-header p {
  font-size: 14px;
  color: var(--vy-ink-2);
  margin-top: 4px;
}

.loading-box {
  padding: 14px 16px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 14px;
  color: var(--vy-ink-2);
  background: var(--vy-surface-2);
}

.error-box {
  padding: 14px 16px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 800;
  margin-bottom: 14px;
  color: var(--vy-danger);
  background: rgba(196, 69, 42, 0.1);
}

.empty-shop {
  min-height: 280px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  text-align: center;
  color: var(--vy-ink-2);
}

.empty-shop h2 {
  color: var(--vy-ink);
  font-size: 22px;
}

.floating-cart {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 80;
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--vy-ink) 0%, #3a2b1c 100%);
  color: #fff;
  box-shadow: 0 18px 36px rgba(31, 26, 20, 0.24), inset 0 1px 0 rgba(255, 255, 255, 0.16);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.floating-cart:hover {
  transform: translateY(-3px) scale(1.04);
  box-shadow: 0 22px 42px rgba(31, 26, 20, 0.3), inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.floating-cart span {
  position: absolute;
  right: -4px;
  top: -4px;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--vy-orange);
  color: #fff;
  border: 2px solid var(--vy-surface);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 900;
  box-shadow: 0 8px 18px rgba(242, 135, 5, 0.34);
}

.category-row {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.category-row button {
  padding: 8px 16px;
  border-radius: 99px;
  font-size: 13px;
  font-weight: 800;
  background: var(--vy-surface);
  color: var(--vy-ink-2);
  border: 1px solid var(--vy-line);
}

.category-row button.active,
.category-row button:hover {
  background: var(--vy-ink);
  color: #fff;
  border-color: var(--vy-ink);
}

.featured-product {
  padding: 0;
  margin-bottom: 18px;
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(320px, 0.92fr);
  gap: 0;
  overflow: hidden;
}

.featured-info {
  min-width: 0;
  padding: clamp(22px, 3.2vw, 36px);
}

.featured-info > small,
.product-info small {
  display: block;
  font-size: 11px;
  color: var(--vy-ink-3);
  font-weight: 800;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  margin-top: 14px;
}

.featured-info h2 {
  font-size: clamp(26px, 3vw, 32px);
  font-weight: 800;
  margin-top: 8px;
  line-height: 1.1;
}

.featured-info p {
  font-size: 14px;
  color: var(--vy-ink-2);
  margin-top: 14px;
  line-height: 1.5;
}

.price-row,
.featured-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 18px;
  flex-wrap: wrap;
}

.price-row {
  align-items: baseline;
  margin-top: 24px;
}

.price-row strong {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 800;
}

.price-row > span {
  text-decoration: line-through;
  color: var(--vy-ink-3);
  font-size: 14px;
}

.featured-actions .vy-btn,
.add-cart-featured {
  flex: 1;
  min-width: min(220px, 100%);
  border-radius: 12px;
  font-weight: 800;
}

.add-cart-featured {
  min-height: 48px;
  padding: 0 18px;
  border: 1px solid rgba(255, 255, 255, 0.26);
  background: linear-gradient(135deg, var(--vy-ink) 0%, #3a2b1c 100%);
  color: #fff;
  box-shadow: 0 14px 28px rgba(31, 26, 20, 0.16), inset 0 1px 0 rgba(255, 255, 255, 0.16);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  position: relative;
  overflow: hidden;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.add-cart-featured::before {
  content: "";
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.18), transparent);
  transform: translateX(-120%);
  transition: transform 0.55s ease;
}

.add-cart-featured:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 34px rgba(31, 26, 20, 0.2), inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.add-cart-featured:hover::before {
  transform: translateX(120%);
}

.add-cart-featured.added {
  background: linear-gradient(135deg, var(--vy-orange) 0%, var(--vy-orange-deep) 100%);
  box-shadow: 0 16px 30px rgba(242, 135, 5, 0.26);
}

.add-cart-icon {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.favorite-button {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: var(--vy-surface);
  border: 1px solid var(--vy-line);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.product-card {
  padding: 12px;
  min-width: 0;
  transition: transform 0.16s ease, box-shadow 0.16s ease;
}

.product-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--vy-shadow);
}

.product-image {
  position: relative;
}

.product-image .vy-chip {
  position: absolute;
  top: 10px;
  left: 10px;
  font-size: 10px;
}

.product-image button {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
}

.product-info {
  padding: 14px 6px 6px;
}

.product-info small {
  margin-top: 0;
}

.product-info h3 {
  font-size: 14px;
  font-weight: 800;
  margin-top: 4px;
  line-height: 1.3;
}

.product-info footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  min-width: 0;
}

.product-info footer > div {
  min-width: 0;
}

.product-info footer strong {
  display: block;
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 800;
}

.product-info footer span {
  display: block;
  text-decoration: line-through;
  color: var(--vy-ink-3);
  font-size: 11px;
}

.product-info footer small,
.volume-line {
  display: block;
  margin-top: 3px;
  color: var(--vy-orange-deep);
  font-size: 11px;
  font-weight: 900;
}

.product-info footer button,
.add-cart-mini {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 20%, #4a3826, var(--vy-ink));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 10px 20px rgba(31, 26, 20, 0.18);
  transition: transform 0.16s ease, box-shadow 0.16s ease, background 0.16s ease;
}

.add-cart-mini:hover {
  transform: translateY(-2px) scale(1.04);
  box-shadow: 0 14px 24px rgba(31, 26, 20, 0.22);
}

.add-cart-mini.added {
  background: linear-gradient(135deg, var(--vy-orange) 0%, var(--vy-orange-deep) 100%);
  box-shadow: 0 12px 22px rgba(242, 135, 5, 0.28);
}

@media (min-width: 1440px) {
  .product-grid {
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  }
}

@media (max-width: 980px) {
  .featured-product {
    grid-template-columns: 1fr;
  }

  .featured-info {
    padding: 24px;
  }
}

@media (max-width: 720px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
  }

  .category-row {
    flex-wrap: nowrap;
    margin-inline: -16px;
    padding-inline: 16px;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .category-row::-webkit-scrollbar {
    display: none;
  }

  .category-row button {
    flex: 0 0 auto;
  }

  .floating-cart {
    right: 18px;
    bottom: 92px;
    width: 54px;
    height: 54px;
  }

  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }
}

@media (max-width: 520px) {
  .workspace {
    padding-inline: 14px;
  }

  .featured-info {
    padding: 20px;
  }

  .featured-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .add-cart-featured,
  .favorite-button {
    width: 100%;
  }

  .favorite-button {
    height: 46px;
    border-radius: 12px;
  }

  .product-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 380px) {
  .price-row strong {
    font-size: 26px;
  }

  .product-info footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .add-cart-mini {
    width: 100%;
    border-radius: 12px;
  }
}
</style>
