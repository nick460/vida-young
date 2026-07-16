<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Check, Plus, ShoppingCart } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import { addPublicCartItem, publicCartCount, readPublicCart } from "../services/publicCartService.js";
import { VyProductImage } from "../components/ui.js";

const route = useRoute();
const router = useRouter();
const username = computed(() => String(route.params.username || ""));
const tienda = ref(null);
const productos = ref([]);
const tiposCliente = ref([]);
const tipoCliente = ref("NORMAL");
const activeCategory = ref("Todos");
const cartItems = ref([]);
const loading = ref(false);
const error = ref("");
const recentlyAddedId = ref(null);

const categories = computed(() => ["Todos", ...new Set(productos.value.map((product) => product.categoria).filter(Boolean))]);
const productList = computed(() => activeCategory.value === "Todos"
  ? productos.value
  : productos.value.filter((product) => product.categoria === activeCategory.value));

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function isProductImage(product) {
  const image = product?.imagenUrl || "";
  return image.startsWith("/uploads/") || image.startsWith("http") || image.startsWith("blob:");
}

async function loadStore() {
  loading.value = true;
  error.value = "";
  try {
    const [storeData, tiposData, productsData] = await Promise.all([
      apiRequest(`/api/public/tiendas/${encodeURIComponent(username.value)}`),
      apiRequest("/api/public/tipos-cliente"),
      apiRequest(`/api/public/tiendas/${encodeURIComponent(username.value)}/productos?tipoCliente=${encodeURIComponent(tipoCliente.value)}`)
    ]);
    tienda.value = storeData;
    tiposCliente.value = tiposData;
    productos.value = productsData;
    cartItems.value = readPublicCart(username.value);
  } catch (exception) {
    error.value = exception.message || "No se pudo cargar esta tienda publica.";
    productos.value = [];
  } finally {
    loading.value = false;
  }
}

function refreshCart() {
  cartItems.value = readPublicCart(username.value);
}

function addToCart(product) {
  cartItems.value = addPublicCartItem(username.value, product);
  recentlyAddedId.value = product.id;
  window.setTimeout(() => {
    if (recentlyAddedId.value === product.id) {
      recentlyAddedId.value = null;
    }
  }, 1200);
}

watch(tipoCliente, loadStore);
onMounted(loadStore);
onMounted(() => window.addEventListener("vy-public-cart-updated", refreshCart));
onUnmounted(() => window.removeEventListener("vy-public-cart-updated", refreshCart));
</script>

<template>
  <div class="vy public-store-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Tienda publica</div>
          <h1>{{ tienda ? `${tienda.nombres} ${tienda.apellidos}` : "Distribuidor" }}</h1>
          <p>Productos disponibles con precios calculados segun tu tipo de cliente.</p>
        </div>
        <label class="client-type">
          <span>Tipo de cliente</span>
          <select v-model="tipoCliente">
            <option v-for="tipo in tiposCliente" :key="tipo.id" :value="tipo.codigo">{{ tipo.nombre }}</option>
          </select>
        </label>
      </header>

      <div v-if="loading" class="loading-box">Cargando tienda...</div>
      <div v-if="error" class="error-box">{{ error }}</div>

      <section v-if="productos.length" class="category-row">
        <button v-for="category in categories" :key="category" type="button" :class="{ active: activeCategory === category }" @click="activeCategory = category">
          {{ category }}
        </button>
      </section>

      <section class="product-grid">
        <article v-for="product in productList" :key="product.id" class="vy-card product-card">
          <div class="product-image">
            <img v-if="isProductImage(product)" class="catalog-photo" :src="product.imagenUrl" :alt="product.nombre" />
            <VyProductImage v-else :grad="'linear-gradient(135deg, #F2E7C4 0%, #F28705 100%)'" :h="1" />
            <span v-if="Number(product.descuento || 0) > 0" class="discount-chip">-Bs. {{ money(product.descuento) }}</span>
          </div>
          <div class="product-info">
            <small>{{ product.categoria || "Producto" }}</small>
            <h3>{{ product.nombre }}</h3>
            <footer>
              <div>
                <strong>Bs. {{ money(product.precioFinal) }}</strong>
                <span v-if="Number(product.descuento || 0) > 0">Normal Bs. {{ money(product.precioPublico) }}</span>
              </div>
              <button type="button" class="add-cart-mini" :class="{ added: recentlyAddedId === product.id }" @click="addToCart(product)">
                <Check v-if="recentlyAddedId === product.id" :size="16" />
                <Plus v-else :size="16" />
              </button>
            </footer>
          </div>
        </article>
      </section>

      <button class="floating-cart" type="button" aria-label="Abrir carrito" @click="router.push({ name: 'tienda-publica-carrito', params: { username } })">
        <ShoppingCart :size="22" />
        <span v-if="publicCartCount(cartItems)">{{ publicCartCount(cartItems) }}</span>
      </button>
    </main>
  </div>
</template>

<style scoped>
.workspace { padding: clamp(18px, 3vw, 32px) clamp(16px, 3vw, 32px) 96px; min-width: 0; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; gap: 20px; margin-bottom: 20px; }
.page-header h1 { font-size: 30px; font-weight: 900; margin-top: 8px; }
.page-header p { font-size: 14px; color: var(--vy-ink-2); margin-top: 4px; }
.client-type span { display: block; margin-bottom: 7px; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.client-type select { min-width: 220px; min-height: 42px; padding: 0 12px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface); font-weight: 900; }
.loading-box, .error-box { padding: 14px 16px; border-radius: 12px; font-size: 13px; font-weight: 800; margin-bottom: 14px; }
.loading-box { color: var(--vy-ink-2); background: var(--vy-surface-2); }
.error-box { color: var(--vy-danger); background: rgba(196, 69, 42, 0.1); }
.category-row { display: flex; gap: 8px; margin-bottom: 20px; flex-wrap: wrap; }
.category-row button { padding: 8px 16px; border-radius: 99px; font-size: 13px; font-weight: 800; background: var(--vy-surface); color: var(--vy-ink-2); border: 1px solid var(--vy-line); }
.category-row button.active, .category-row button:hover { background: var(--vy-orange); color: #fff; border-color: var(--vy-orange); }
.product-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px; }
.product-card { padding: 12px; min-width: 0; }
.product-image { position: relative; aspect-ratio: 1 / 1; overflow: hidden; border-radius: 14px; border: 1px solid rgba(214, 204, 188, 0.72); background: #fff; }
.product-image :deep(> div), .catalog-photo { width: 100%; height: 100% !important; }
.catalog-photo { display: block; object-fit: cover; }
.discount-chip { position: absolute; left: 10px; top: 10px; padding: 5px 8px; border-radius: 999px; background: var(--vy-orange); color: #fff; font-size: 11px; font-weight: 900; }
.product-info { padding: 14px 6px 6px; }
.product-info small { color: var(--vy-ink-3); font-size: 12px; font-weight: 800; }
.product-info h3 { font-size: 14px; font-weight: 900; margin-top: 4px; line-height: 1.3; }
.product-info footer { display: flex; justify-content: space-between; align-items: center; gap: 10px; margin-top: 12px; }
.product-info footer strong { display: block; font-family: var(--font-display); font-size: 17px; font-weight: 900; }
.product-info footer span { display: block; margin-top: 3px; color: var(--vy-ink-3); text-decoration: line-through; font-size: 11px; font-weight: 800; }
.add-cart-mini { width: 36px; height: 36px; border-radius: 50%; background: var(--vy-orange); color: #fff; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.floating-cart { position: fixed; right: 28px; bottom: 28px; z-index: 80; width: 58px; height: 58px; border-radius: 50%; background: var(--vy-ink); color: #fff; display: inline-flex; align-items: center; justify-content: center; box-shadow: var(--vy-shadow-lg); }
.floating-cart span { position: absolute; right: -4px; top: -4px; min-width: 22px; height: 22px; padding: 0 6px; border-radius: 999px; background: var(--vy-orange); border: 2px solid var(--vy-surface); font-size: 11px; font-weight: 900; }
@media (max-width: 720px) { .page-header { align-items: stretch; flex-direction: column; } .client-type select { width: 100%; } .product-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 520px) { .product-grid { grid-template-columns: 1fr; } }
</style>
