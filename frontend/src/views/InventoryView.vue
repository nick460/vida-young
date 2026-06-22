<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import {
  Boxes,
  CircleMinus,
  Eye,
  EyeOff,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Search
} from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import { VyProductImage } from "../components/ui.js";

const loading = ref(false);
const error = ref("");
const productos = ref([]);
const query = ref("");
const productModalOpen = ref(false);
const editingProductId = ref(null);
const productImageFile = ref(null);
const productImagePreview = ref("");

const alertClasses = {
  popup: "vy-swal-popup",
  title: "vy-swal-title",
  htmlContainer: "vy-swal-text",
  confirmButton: "vy-swal-confirm",
  cancelButton: "vy-swal-cancel"
};

const productForm = reactive({
  sku: "",
  nombre: "",
  descripcion: "",
  categoria: "",
  precio: 0,
  pv: 0,
  qp: 0,
  imagenUrl: "",
  listarEnShop: false
});

const productErrors = reactive({});
const productSubmitted = ref(false);

const filteredProducts = computed(() => {
  const text = query.value.trim().toLowerCase();
  if (!text) {
    return productos.value;
  }

  return productos.value.filter((producto) => (
    producto.nombre?.toLowerCase().includes(text) ||
    producto.sku?.toLowerCase().includes(text) ||
    producto.categoria?.toLowerCase().includes(text)
  ));
});

const shopProductsCount = computed(() =>
  productos.value.filter((producto) => Boolean(producto.listarEnShop)).length
);

const categoriesCount = computed(() =>
  new Set(productos.value.map((producto) => producto.categoria).filter(Boolean)).size
);

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function showSuccess(title, text) {
  return Swal.fire({
    title,
    text,
    icon: "success",
    confirmButtonText: "Listo",
    background: "#FFFFFF",
    color: "#1F1A14",
    confirmButtonColor: "#F28705",
    iconColor: "#3F8F5C",
    customClass: alertClasses
  });
}

function showError(title, text) {
  return Swal.fire({
    title,
    text,
    icon: "error",
    confirmButtonText: "Entendido",
    background: "#FFFFFF",
    color: "#1F1A14",
    confirmButtonColor: "#F28705",
    iconColor: "#C4452A",
    customClass: alertClasses
  });
}

function confirmDanger(title, text) {
  return Swal.fire({
    title,
    text,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Si, eliminar",
    cancelButtonText: "Cancelar",
    reverseButtons: true,
    background: "#FFFFFF",
    color: "#1F1A14",
    confirmButtonColor: "#C4452A",
    cancelButtonColor: "#1F1A14",
    iconColor: "#F28705",
    customClass: alertClasses
  });
}

function setErrors(target, errors) {
  Object.keys(target).forEach((key) => delete target[key]);
  Object.assign(target, errors);
}

function validateProductForm() {
  const errors = {};

  if (!productForm.sku.trim()) {
    errors.sku = "Ingresa el SKU del producto.";
  }

  if (!productForm.nombre.trim()) {
    errors.nombre = "Ingresa el nombre del producto.";
  }

  if (Number(productForm.precio) <= 0) {
    errors.precio = "El precio debe ser mayor a cero.";
  }

  if (Number(productForm.pv) < 0) {
    errors.pv = "El PV no puede ser negativo.";
  }

  if (Number(productForm.qp) < 0) {
    errors.qp = "El QP no puede ser negativo.";
  }

  setErrors(productErrors, errors);
  return Object.keys(errors).length === 0;
}

async function loadAll() {
  loading.value = true;
  error.value = "";

  try {
    productos.value = await apiRequest("/api/productos");
  } catch {
    error.value = "No se pudo cargar inventario. Verifica que el backend este activo y la sesion sea valida.";
  } finally {
    loading.value = false;
  }
}

function resetProductForm() {
  editingProductId.value = null;
  productImageFile.value = null;
  productImagePreview.value = "";
  productSubmitted.value = false;
  setErrors(productErrors, {});
  Object.assign(productForm, {
    sku: "",
    nombre: "",
    descripcion: "",
    categoria: "",
    precio: 0,
    pv: 0,
    qp: 0,
    imagenUrl: "",
    listarEnShop: false
  });
}

function openProductModal(producto = null) {
  if (producto) {
    editingProductId.value = producto.id;
    productImageFile.value = null;
    productImagePreview.value = producto.imagenUrl || "";
    Object.assign(productForm, {
      sku: producto.sku || "",
      nombre: producto.nombre || "",
      descripcion: producto.descripcion || "",
      categoria: producto.categoria || "",
      precio: producto.precio || 0,
      pv: producto.pv || 0,
      qp: producto.qp || 0,
      imagenUrl: producto.imagenUrl || "",
      listarEnShop: Boolean(producto.listarEnShop)
    });
  } else {
    resetProductForm();
  }
  productModalOpen.value = true;
}

function handleProductImage(event) {
  const [file] = event.target.files || [];
  productImageFile.value = file || null;
  productImagePreview.value = file ? URL.createObjectURL(file) : productForm.imagenUrl;
}

function closeProductModal() {
  productModalOpen.value = false;
  resetProductForm();
}

async function saveProduct() {
  productSubmitted.value = true;
  if (!validateProductForm()) {
    return;
  }

  try {
    const isEditing = Boolean(editingProductId.value);
    const savedProduct = await apiRequest(isEditing ? `/api/productos/${editingProductId.value}` : "/api/productos", {
      method: isEditing ? "PUT" : "POST",
      body: JSON.stringify({
        ...productForm,
        precio: Number(productForm.precio || 0),
        pv: Number(productForm.pv || 0),
        qp: Number(productForm.qp || 0),
        listarEnShop: Boolean(productForm.listarEnShop)
      })
    });

    if (productImageFile.value) {
      const formData = new FormData();
      formData.append("imagen", productImageFile.value);
      await apiRequest(`/api/productos/${savedProduct.id}/imagen`, {
        method: "POST",
        body: formData
      });
    }

    closeProductModal();
    await loadAll();
    await showSuccess("Producto guardado", "El producto se registro correctamente.");
  } catch (exception) {
    await showError("No se pudo guardar", exception.message || "Intenta nuevamente.");
  }
}

async function removeProduct(producto) {
  const result = await confirmDanger("Eliminar producto", `Se eliminara ${producto.nombre} del inventario.`);

  if (!result.isConfirmed) {
    return;
  }

  try {
    await apiRequest(`/api/productos/${producto.id}`, { method: "DELETE" });
    await loadAll();
    await showSuccess("Producto eliminado", "El producto se elimino correctamente.");
  } catch (exception) {
    await showError("No se pudo eliminar", exception.message || "Intenta nuevamente.");
  }
}

onMounted(loadAll);

watch(productForm, () => {
  if (productSubmitted.value) {
    validateProductForm();
  }
}, { deep: true });
</script>

<template>
  <div class="vy inventory-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Inventario</div>
          <h1>Productos</h1>
          <p>Administra el catalogo y la visibilidad de productos en la tienda.</p>
        </div>
        <div class="header-actions">
          <button class="vy-btn vy-btn-ghost" type="button" @click="loadAll">
            <RefreshCw :size="15" stroke-width="2" />
            Actualizar
          </button>
          <button class="vy-btn vy-btn-primary" type="button" @click="openProductModal()">
            <Plus :size="15" stroke-width="2.2" />
            Producto
          </button>
        </div>
      </header>

      <section class="summary-grid">
        <article class="vy-card summary-card">
          <div class="summary-icon"><Boxes :size="18" stroke-width="1.9" /></div>
          <span>Productos</span>
          <strong>{{ productos.length }}</strong>
        </article>
        <article class="vy-card summary-card">
          <div class="summary-icon"><Eye :size="18" stroke-width="1.9" /></div>
          <span>En tienda</span>
          <strong>{{ shopProductsCount }}</strong>
        </article>
        <article class="vy-card summary-card">
          <div class="summary-icon"><Search :size="18" stroke-width="1.9" /></div>
          <span>Categorias</span>
          <strong>{{ categoriesCount }}</strong>
        </article>
      </section>

      <div v-if="error" class="error-box">{{ error }}</div>
      <div v-if="loading" class="loading-box">Cargando inventario...</div>

      <section v-else class="vy-card products-card">
        <div class="card-header">
          <div>
            <h2>Catalogo de productos</h2>
            <p>Busca, edita, publica u oculta productos de la tienda.</p>
          </div>
          <label class="search-field">
            <Search :size="15" stroke-width="2" />
            <input v-model.trim="query" placeholder="Buscar producto, SKU o categoria" />
          </label>
        </div>

        <div class="product-list">
          <article v-for="producto in filteredProducts" :key="producto.id" class="product-row">
            <VyProductImage
              class="product-thumb"
              :grad="producto.imagenUrl || 'linear-gradient(135deg, #F2E7C4 0%, #F28705 100%)'"
              :h="58"
            />
            <div class="product-main">
              <strong>{{ producto.nombre }}</strong>
              <small>{{ producto.sku }} · {{ producto.categoria || "Sin categoria" }}</small>
            </div>
            <span class="shop-state" :class="{ disabled: !producto.listarEnShop }">
              <Eye v-if="producto.listarEnShop" :size="14" />
              <EyeOff v-else :size="14" />
              {{ producto.listarEnShop ? "Shop" : "Oculto" }}
            </span>
            <span class="price">
              Bs. {{ money(producto.precio) }}
              <small>PV {{ money(producto.pv) }} · QP {{ money(producto.qp) }}</small>
            </span>
            <span class="row-actions">
              <button type="button" aria-label="Editar" @click="openProductModal(producto)">
                <Pencil :size="15" stroke-width="2" />
              </button>
              <button type="button" class="danger" aria-label="Eliminar" @click="removeProduct(producto)">
                <CircleMinus :size="15" stroke-width="2" />
              </button>
            </span>
          </article>
        </div>
      </section>
    </main>

    <Teleport to="body">
      <div v-if="productModalOpen" class="modal-backdrop" @click.self="closeProductModal">
        <form class="entity-modal" novalidate @submit.prevent="saveProduct">
          <header>
            <div>
              <span class="vy-eyebrow">Producto</span>
              <h2>{{ editingProductId ? "Editar producto" : "Nuevo producto" }}</h2>
            </div>
            <button type="button" aria-label="Cerrar" @click="closeProductModal">x</button>
          </header>
          <div class="modal-grid">
            <label :class="{ invalid: productErrors.sku }">
              <span>SKU</span>
              <input v-model.trim="productForm.sku" />
              <small v-if="productErrors.sku">{{ productErrors.sku }}</small>
            </label>
            <label :class="{ invalid: productErrors.nombre }">
              <span>Nombre</span>
              <input v-model.trim="productForm.nombre" />
              <small v-if="productErrors.nombre">{{ productErrors.nombre }}</small>
            </label>
            <label>
              <span>Categoria</span>
              <input v-model.trim="productForm.categoria" />
            </label>
            <label :class="{ invalid: productErrors.precio }">
              <span>Precio</span>
              <input v-model.number="productForm.precio" type="number" min="0" step="0.01" />
              <small v-if="productErrors.precio">{{ productErrors.precio }}</small>
            </label>
            <label :class="{ invalid: productErrors.pv }">
              <span>PV</span>
              <input v-model.number="productForm.pv" type="number" min="0" step="0.01" />
              <small v-if="productErrors.pv">{{ productErrors.pv }}</small>
            </label>
            <label :class="{ invalid: productErrors.qp }">
              <span>QP</span>
              <input v-model.number="productForm.qp" type="number" min="0" step="0.01" />
              <small v-if="productErrors.qp">{{ productErrors.qp }}</small>
            </label>
            <label class="toggle-field full-field">
              <input v-model="productForm.listarEnShop" type="checkbox" />
              <span>Listar este producto en /shop</span>
            </label>
            <section class="image-field full-field">
              <label>
                <span>Imagen</span>
                <input type="file" accept="image/png,image/jpeg,image/webp" @change="handleProductImage" />
              </label>
              <VyProductImage :grad="productImagePreview || productForm.imagenUrl || 'linear-gradient(135deg, #F2E7C4 0%, #F28705 100%)'" :h="120" />
            </section>
            <label class="full-field">
              <span>Descripcion</span>
              <textarea v-model.trim="productForm.descripcion" rows="3" />
            </label>
          </div>
          <footer>
            <button class="vy-btn vy-btn-ghost" type="button" @click="closeProductModal">Cancelar</button>
            <button class="vy-btn vy-btn-primary" type="submit"><Save :size="15" /> Guardar</button>
          </footer>
        </form>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.workspace { min-width: 0; padding: 28px 32px 40px; }
.page-header, .card-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; }
.page-header { margin-bottom: 20px; }
.page-header h1 { font-size: 30px; font-weight: 800; margin-top: 8px; }
.page-header p, .card-header p { margin-top: 4px; color: var(--vy-ink-2); font-size: 14px; }
.header-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.header-actions .vy-btn, .entity-modal .vy-btn { min-height: 40px; padding: 10px 16px; border-radius: 12px; font-weight: 800; }
.vy-btn-primary { background: var(--vy-orange); color: #fff; box-shadow: var(--vy-shadow-orange); }
.vy-btn-ghost { background: var(--vy-surface); border: 1px solid var(--vy-line); color: var(--vy-ink-2); }
.summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; margin-bottom: 18px; }
.summary-card { padding: 18px; }
.summary-icon { width: 38px; height: 38px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-bottom: 12px; background: var(--vy-cream); color: var(--vy-orange-deep); }
.summary-card span { display: block; font-size: 12px; color: var(--vy-ink-3); font-weight: 700; text-transform: uppercase; }
.summary-card strong { display: block; font-family: var(--font-display); font-size: 28px; font-weight: 800; margin-top: 6px; }
.products-card { padding: 18px; overflow: hidden; }
.card-header { margin-bottom: 16px; }
.card-header h2 { font-size: 16px; font-weight: 800; }
.search-field { min-width: 280px; height: 40px; padding: 0 12px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); display: flex; align-items: center; gap: 8px; color: var(--vy-ink-3); }
.search-field input { width: 100%; border: 0; outline: 0; background: transparent; font: inherit; font-size: 13px; font-weight: 700; color: var(--vy-ink); }
.product-list { display: grid; gap: 8px; max-height: 650px; overflow: auto; padding-right: 2px; }
.product-row { min-height: 78px; padding: 10px 12px; border: 1px solid var(--vy-line); border-radius: 13px; background: var(--vy-surface-2); display: grid; grid-template-columns: 58px minmax(0, 1fr) auto 110px auto; align-items: center; gap: 10px; }
.product-row:hover { border-color: rgba(242, 135, 5, 0.42); background: #fff; box-shadow: var(--vy-shadow-sm); }
.product-thumb { border-radius: 10px; overflow: hidden; }
.product-main strong, .product-main small { display: block; }
.product-main strong { font-size: 13px; font-weight: 900; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-main small { margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 700; }
.shop-state { padding: 4px 9px; border-radius: 999px; font-size: 11px; font-weight: 900; white-space: nowrap; display: inline-flex; align-items: center; gap: 5px; background: rgba(63, 143, 92, 0.12); color: var(--vy-success); }
.shop-state.disabled { background: rgba(31, 26, 20, 0.08); color: var(--vy-ink-3); }
.price { font-size: 13px; font-weight: 900; text-align: right; }
.price small { display: block; margin-top: 3px; color: var(--vy-ink-3); font-size: 10px; font-weight: 800; white-space: nowrap; }
.row-actions { display: inline-flex; gap: 6px; }
.row-actions button { width: 30px; height: 30px; border: 1px solid var(--vy-line); border-radius: 9px; background: #fff; color: var(--vy-orange-deep); display: inline-flex; align-items: center; justify-content: center; }
.row-actions .danger { color: var(--vy-danger); }
.error-box, .loading-box { padding: 14px 16px; border-radius: 12px; font-size: 13px; font-weight: 700; margin-bottom: 14px; }
.error-box { color: var(--vy-danger); background: rgba(196, 69, 42, 0.1); }
.loading-box { color: var(--vy-ink-2); background: var(--vy-surface-2); }
.modal-backdrop { position: fixed; inset: 0; z-index: 100; display: flex; align-items: center; justify-content: center; padding: 24px; background: rgba(31, 26, 20, 0.42); backdrop-filter: blur(5px); }
.entity-modal { width: min(680px, 100%); max-height: calc(100vh - 48px); overflow-y: auto; padding: 22px; border-radius: 18px; border: 1px solid var(--vy-line); background: var(--vy-surface); box-shadow: var(--vy-shadow-lg); font-family: var(--font-sans); color: var(--vy-ink); }
.entity-modal header, .entity-modal footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.entity-modal header { padding-bottom: 18px; margin-bottom: 18px; border-bottom: 1px solid var(--vy-line-2); }
.entity-modal h2 { margin-top: 4px; font-size: 22px; font-weight: 800; }
.entity-modal header > button { width: 36px; height: 36px; border-radius: 10px; background: var(--vy-surface-2); color: var(--vy-ink-3); font-size: 18px; }
.modal-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.full-field { grid-column: 1 / -1; }
label span { display: block; margin-bottom: 6px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; text-transform: uppercase; }
label small { display: block; margin-top: 6px; color: var(--vy-danger); font-size: 11px; font-weight: 800; }
input, textarea { width: 100%; padding: 12px 13px; border: 1px solid var(--vy-line); border-radius: 10px; background: var(--vy-surface); color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 600; }
input[type="file"] { padding: 10px; }
input:focus, textarea:focus { outline: 2px solid rgba(242, 135, 5, 0.22); border-color: var(--vy-orange); }
label.invalid > span { color: var(--vy-danger); }
label.invalid input, label.invalid textarea { border-color: var(--vy-danger); background: rgba(196, 69, 42, 0.04); }
.toggle-field { min-height: 46px; padding: 12px 13px; border: 1px solid var(--vy-line); border-radius: 10px; background: var(--vy-surface-2); display: flex; align-items: center; gap: 10px; }
.toggle-field input { width: 18px; height: 18px; padding: 0; accent-color: var(--vy-orange); }
.toggle-field span { margin: 0; color: var(--vy-ink); font-size: 13px; text-transform: none; }
.image-field { display: grid; grid-template-columns: minmax(220px, 0.85fr) minmax(260px, 1.15fr); gap: 14px; align-items: end; }
.entity-modal footer { justify-content: flex-end; padding-top: 18px; margin-top: 18px; border-top: 1px solid var(--vy-line-2); }
@media (max-width: 1180px) { .product-row { grid-template-columns: 58px minmax(0, 1fr) auto auto; } .price { display: none; } }
@media (max-width: 720px) { .workspace { padding: 24px 20px 32px; } .page-header, .card-header, .entity-modal header, .entity-modal footer { align-items: stretch; flex-direction: column; } .header-actions, .header-actions .vy-btn { width: 100%; } .summary-grid, .modal-grid, .image-field { grid-template-columns: 1fr; } .product-row { grid-template-columns: 1fr; } .search-field { min-width: 0; width: 100%; } .price { text-align: left; } .row-actions button { width: 100%; } }
</style>
