<script setup>
import { computed, onMounted, reactive, ref, watch } from "vue";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import {
  Boxes,
  CalendarClock,
  CircleMinus,
  PackagePlus,
  Pencil,
  Plus,
  RefreshCw,
  Save,
  Search,
  TriangleAlert
} from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import VyAutocompleteSelect from "../components/VyAutocompleteSelect.vue";
import { VyProductImage } from "../components/ui.js";

const loading = ref(false);
const error = ref("");
const productos = ref([]);
const lotes = ref([]);
const query = ref("");
const selectedProductId = ref("");
const productModalOpen = ref(false);
const lotModalOpen = ref(false);
const editingProductId = ref(null);
const editingLotId = ref(null);
const productImageFile = ref(null);
const productImagePreview = ref("");

const alertClasses = {
  popup: "vy-swal-popup",
  title: "vy-swal-title",
  htmlContainer: "vy-swal-text",
  confirmButton: "vy-swal-confirm",
  cancelButton: "vy-swal-cancel"
};

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

const productForm = reactive({
  sku: "",
  nombre: "",
  descripcion: "",
  categoria: "",
  precio: 0,
  pv: 0,
  qp: 0,
  imagenUrl: "",
  stockMinimo: 0
});

const lotForm = reactive({
  productoId: "",
  numeroLote: "",
  fechaVencimiento: "",
  cantidadInicial: 0,
  cantidadDisponible: 0,
  costoUnitario: 0,
  observacion: ""
});

const productErrors = reactive({});
const lotErrors = reactive({});
const productSubmitted = ref(false);
const lotSubmitted = ref(false);

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

const selectedProduct = computed(() =>
  productos.value.find((producto) => producto.id === Number(selectedProductId.value))
);

const visibleLots = computed(() => {
  if (!selectedProductId.value) {
    return lotes.value;
  }

  return lotes.value.filter((lote) => lote.producto?.id === Number(selectedProductId.value));
});

const totalStock = computed(() =>
  productos.value.reduce((total, producto) => total + Number(producto.stockDisponible || 0), 0)
);

const lowStockCount = computed(() =>
  productos.value.filter((producto) => Number(producto.stockDisponible || 0) <= Number(producto.stockMinimo || 0)).length
);

const expiringLotsCount = computed(() => {
  const today = new Date();
  const limit = new Date();
  limit.setDate(today.getDate() + 45);

  return lotes.value.filter((lote) => {
    if (!lote.fechaVencimiento) {
      return false;
    }
    const date = new Date(`${lote.fechaVencimiento}T00:00:00`);
    return date >= today && date <= limit;
  }).length;
});

const selectedLotProductIds = computed({
  get: () => lotForm.productoId ? [lotForm.productoId] : [],
  set: (value) => {
    lotForm.productoId = value.at(-1) || "";
  }
});

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function stockStatus(producto) {
  const stock = Number(producto.stockDisponible || 0);
  const minimo = Number(producto.stockMinimo || 0);
  if (stock <= 0) {
    return { label: "Sin stock", class: "danger" };
  }
  if (stock <= minimo) {
    return { label: "Stock bajo", class: "warn" };
  }
  return { label: "Disponible", class: "ok" };
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

  if (Number(productForm.stockMinimo) < 0) {
    errors.stockMinimo = "El stock minimo no puede ser negativo.";
  }

  setErrors(productErrors, errors);
  return Object.keys(errors).length === 0;
}

function validateLotForm() {
  const errors = {};

  if (!lotForm.productoId) {
    errors.productoId = "Selecciona el producto del lote.";
  }

  if (!lotForm.numeroLote.trim()) {
    errors.numeroLote = "Ingresa el numero de lote.";
  }

  if (Number(lotForm.cantidadInicial) < 0) {
    errors.cantidadInicial = "La cantidad inicial no puede ser negativa.";
  }

  if (Number(lotForm.cantidadDisponible) < 0) {
    errors.cantidadDisponible = "La cantidad disponible no puede ser negativa.";
  }

  if (Number(lotForm.cantidadDisponible) > Number(lotForm.cantidadInicial)) {
    errors.cantidadDisponible = "No puede superar la cantidad inicial.";
  }

  if (Number(lotForm.costoUnitario) < 0) {
    errors.costoUnitario = "El costo unitario no puede ser negativo.";
  }

  setErrors(lotErrors, errors);
  return Object.keys(errors).length === 0;
}

async function loadAll() {
  loading.value = true;
  error.value = "";

  try {
    const [productosData, lotesData] = await Promise.all([
      apiRequest("/api/productos"),
      apiRequest("/api/lotes-productos")
    ]);
    productos.value = productosData;
    lotes.value = lotesData;
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
    stockMinimo: 0
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
      stockMinimo: producto.stockMinimo || 0
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
        stockMinimo: Number(productForm.stockMinimo || 0)
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

function resetLotForm() {
  editingLotId.value = null;
  lotSubmitted.value = false;
  setErrors(lotErrors, {});
  Object.assign(lotForm, {
    productoId: selectedProductId.value || "",
    numeroLote: "",
    fechaVencimiento: "",
    cantidadInicial: 0,
    cantidadDisponible: 0,
    costoUnitario: 0,
    observacion: ""
  });
}

function openLotModal(lote = null) {
  if (lote) {
    editingLotId.value = lote.id;
    Object.assign(lotForm, {
      productoId: lote.producto?.id || "",
      numeroLote: lote.numeroLote || "",
      fechaVencimiento: lote.fechaVencimiento || "",
      cantidadInicial: lote.cantidadInicial || 0,
      cantidadDisponible: lote.cantidadDisponible || 0,
      costoUnitario: lote.costoUnitario || 0,
      observacion: lote.observacion || ""
    });
  } else {
    resetLotForm();
  }
  lotModalOpen.value = true;
}

function closeLotModal() {
  lotModalOpen.value = false;
  resetLotForm();
}

async function saveLot() {
  lotSubmitted.value = true;
  if (!validateLotForm()) {
    return;
  }

  try {
    const isEditing = Boolean(editingLotId.value);
    await apiRequest(isEditing ? `/api/lotes-productos/${editingLotId.value}` : "/api/lotes-productos", {
      method: isEditing ? "PUT" : "POST",
      body: JSON.stringify({
        producto: { id: Number(lotForm.productoId) },
        numeroLote: lotForm.numeroLote,
        fechaVencimiento: lotForm.fechaVencimiento || null,
        cantidadInicial: Number(lotForm.cantidadInicial || 0),
        cantidadDisponible: Number(lotForm.cantidadDisponible || 0),
        costoUnitario: Number(lotForm.costoUnitario || 0),
        observacion: lotForm.observacion
      })
    });
    closeLotModal();
    await loadAll();
    await showSuccess("Lote guardado", "El lote se registro correctamente.");
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

async function removeLot(lote) {
  const result = await confirmDanger("Eliminar lote", `Se eliminara el lote ${lote.numeroLote}.`);

  if (!result.isConfirmed) {
    return;
  }

  try {
    await apiRequest(`/api/lotes-productos/${lote.id}`, { method: "DELETE" });
    await loadAll();
    await showSuccess("Lote eliminado", "El lote se elimino correctamente.");
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

watch(lotForm, () => {
  if (lotSubmitted.value) {
    validateLotForm();
  }
}, { deep: true });
</script>

<template>
  <div class="vy inventory-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Inventario</div>
          <h1>Productos y lotes</h1>
          <p>Administra catalogo, entradas por lote, vencimientos y stock disponible.</p>
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
          <button class="vy-btn vy-btn-dark" type="button" @click="openLotModal()">
            <PackagePlus :size="15" stroke-width="2.2" />
            Lote
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
          <div class="summary-icon"><PackagePlus :size="18" stroke-width="1.9" /></div>
          <span>Stock total</span>
          <strong>{{ totalStock }}</strong>
        </article>
        <article class="vy-card summary-card">
          <div class="summary-icon"><TriangleAlert :size="18" stroke-width="1.9" /></div>
          <span>Stock bajo</span>
          <strong>{{ lowStockCount }}</strong>
        </article>
        <article class="vy-card summary-card">
          <div class="summary-icon"><CalendarClock :size="18" stroke-width="1.9" /></div>
          <span>Vencen pronto</span>
          <strong>{{ expiringLotsCount }}</strong>
        </article>
      </section>

      <div v-if="error" class="error-box">{{ error }}</div>
      <div v-if="loading" class="loading-box">Cargando inventario...</div>

      <section v-else class="content-grid">
        <article class="vy-card products-card">
          <div class="card-header">
            <div>
              <h2>Catalogo de productos</h2>
              <p>Busca, edita o elimina productos registrados.</p>
            </div>
            <label class="search-field">
              <Search :size="15" stroke-width="2" />
              <input v-model.trim="query" placeholder="Buscar producto, SKU o categoria" />
            </label>
          </div>

          <div class="product-list">
            <button
              v-for="producto in filteredProducts"
              :key="producto.id"
              type="button"
              class="product-row"
              :class="{ active: selectedProductId === String(producto.id) }"
              @click="selectedProductId = String(producto.id)"
            >
              <span class="product-main">
                <strong>{{ producto.nombre }}</strong>
                <small>{{ producto.sku }} · {{ producto.categoria || "Sin categoria" }}</small>
              </span>
              <span :class="['stock-pill', stockStatus(producto).class]">{{ stockStatus(producto).label }}</span>
              <span class="stock-number">{{ producto.stockDisponible || 0 }}</span>
              <span class="price">
                $ {{ money(producto.precio) }}
                <small>PV {{ money(producto.pv) }} · QP {{ money(producto.qp) }}</small>
              </span>
              <span class="row-actions">
                <Pencil :size="15" stroke-width="2" @click.stop="openProductModal(producto)" />
                <CircleMinus :size="15" stroke-width="2" @click.stop="removeProduct(producto)" />
              </span>
            </button>
          </div>
        </article>

        <article class="vy-card lots-card">
          <div class="card-header">
            <div>
              <h2>Lotes</h2>
              <p>{{ selectedProduct ? selectedProduct.nombre : "Todos los productos" }}</p>
            </div>
            <button class="mini-action" type="button" @click="openLotModal()">
              <Plus :size="15" stroke-width="2.2" />
              Nuevo lote
            </button>
          </div>

          <table>
            <thead>
              <tr>
                <th>Lote</th>
                <th>Producto</th>
                <th>Vence</th>
                <th>Inicial</th>
                <th>Disponible</th>
                <th>Costo</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="lote in visibleLots" :key="lote.id">
                <td><strong>{{ lote.numeroLote }}</strong></td>
                <td>{{ lote.producto?.nombre }}</td>
                <td>{{ lote.fechaVencimiento || "Sin fecha" }}</td>
                <td>{{ lote.cantidadInicial }}</td>
                <td>{{ lote.cantidadDisponible }}</td>
                <td>$ {{ money(lote.costoUnitario) }}</td>
                <td class="table-actions">
                  <button type="button" @click="openLotModal(lote)"><Pencil :size="14" /></button>
                  <button type="button" class="danger" @click="removeLot(lote)"><CircleMinus :size="14" /></button>
                </td>
              </tr>
            </tbody>
          </table>
        </article>
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
            <label :class="{ invalid: productErrors.sku }"><span>SKU</span><input v-model.trim="productForm.sku" /><small v-if="productErrors.sku">{{ productErrors.sku }}</small></label>
            <label :class="{ invalid: productErrors.nombre }"><span>Nombre</span><input v-model.trim="productForm.nombre" /><small v-if="productErrors.nombre">{{ productErrors.nombre }}</small></label>
            <label><span>Categoria</span><input v-model.trim="productForm.categoria" /></label>
            <label :class="{ invalid: productErrors.precio }"><span>Precio</span><input v-model.number="productForm.precio" type="number" min="0" step="0.01" /><small v-if="productErrors.precio">{{ productErrors.precio }}</small></label>
            <label :class="{ invalid: productErrors.pv }"><span>PV</span><input v-model.number="productForm.pv" type="number" min="0" step="0.01" /><small v-if="productErrors.pv">{{ productErrors.pv }}</small></label>
            <label :class="{ invalid: productErrors.qp }"><span>QP</span><input v-model.number="productForm.qp" type="number" min="0" step="0.01" /><small v-if="productErrors.qp">{{ productErrors.qp }}</small></label>
            <label class="full-field" :class="{ invalid: productErrors.stockMinimo }"><span>Stock minimo</span><input v-model.number="productForm.stockMinimo" type="number" min="0" /><small v-if="productErrors.stockMinimo">{{ productErrors.stockMinimo }}</small></label>
            <section class="image-field full-field">
              <label>
                <span>Imagen</span>
                <input type="file" accept="image/png,image/jpeg,image/webp" @change="handleProductImage" />
              </label>
              <VyProductImage :grad="productImagePreview || productForm.imagenUrl || 'linear-gradient(135deg, #F2E7C4 0%, #F28705 100%)'" :h="120" />
            </section>
            <label class="full-field"><span>Descripcion</span><textarea v-model.trim="productForm.descripcion" rows="3" /></label>
          </div>
          <footer>
            <button class="vy-btn vy-btn-ghost" type="button" @click="closeProductModal">Cancelar</button>
            <button class="vy-btn vy-btn-primary" type="submit"><Save :size="15" /> Guardar</button>
          </footer>
        </form>
      </div>

      <div v-if="lotModalOpen" class="modal-backdrop" @click.self="closeLotModal">
        <form class="entity-modal" novalidate @submit.prevent="saveLot">
          <header>
            <div>
              <span class="vy-eyebrow">Lote</span>
              <h2>{{ editingLotId ? "Editar lote" : "Nuevo lote" }}</h2>
            </div>
            <button type="button" aria-label="Cerrar" @click="closeLotModal">x</button>
          </header>
          <div class="modal-grid">
            <label class="full-field" :class="{ invalid: lotErrors.productoId }">
              <span>Producto</span>
              <VyAutocompleteSelect
                v-model="selectedLotProductIds"
                :options="productos"
                label-key="nombre"
                value-key="id"
                placeholder="Buscar producto"
                empty-text="No hay productos disponibles"
              />
              <small v-if="lotErrors.productoId">{{ lotErrors.productoId }}</small>
            </label>
            <label :class="{ invalid: lotErrors.numeroLote }"><span>Numero de lote</span><input v-model.trim="lotForm.numeroLote" /><small v-if="lotErrors.numeroLote">{{ lotErrors.numeroLote }}</small></label>
            <label><span>Vencimiento</span><input v-model="lotForm.fechaVencimiento" type="date" /></label>
            <label :class="{ invalid: lotErrors.cantidadInicial }"><span>Cantidad inicial</span><input v-model.number="lotForm.cantidadInicial" type="number" min="0" /><small v-if="lotErrors.cantidadInicial">{{ lotErrors.cantidadInicial }}</small></label>
            <label :class="{ invalid: lotErrors.cantidadDisponible }"><span>Cantidad disponible</span><input v-model.number="lotForm.cantidadDisponible" type="number" min="0" /><small v-if="lotErrors.cantidadDisponible">{{ lotErrors.cantidadDisponible }}</small></label>
            <label :class="{ invalid: lotErrors.costoUnitario }"><span>Costo unitario</span><input v-model.number="lotForm.costoUnitario" type="number" min="0" step="0.01" /><small v-if="lotErrors.costoUnitario">{{ lotErrors.costoUnitario }}</small></label>
            <label class="full-field"><span>Observacion</span><textarea v-model.trim="lotForm.observacion" rows="3" /></label>
          </div>
          <footer>
            <button class="vy-btn vy-btn-ghost" type="button" @click="closeLotModal">Cancelar</button>
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
.vy-btn-dark { background: var(--vy-ink); color: #fff; }
.vy-btn-ghost { background: var(--vy-surface); border: 1px solid var(--vy-line); color: var(--vy-ink-2); }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin-bottom: 18px; }
.summary-card { padding: 18px; }
.summary-icon { width: 38px; height: 38px; border-radius: 12px; display: flex; align-items: center; justify-content: center; margin-bottom: 12px; background: var(--vy-cream); color: var(--vy-orange-deep); }
.summary-card span { display: block; font-size: 12px; color: var(--vy-ink-3); font-weight: 700; text-transform: uppercase; }
.summary-card strong { display: block; font-family: var(--font-display); font-size: 28px; font-weight: 800; margin-top: 6px; }
.content-grid { display: grid; grid-template-columns: minmax(420px, 0.95fr) minmax(0, 1.2fr); gap: 18px; align-items: start; }
.products-card, .lots-card { padding: 18px; overflow: hidden; }
.card-header { margin-bottom: 16px; }
.card-header h2 { font-size: 16px; font-weight: 800; }
.search-field { min-width: 260px; height: 40px; padding: 0 12px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); display: flex; align-items: center; gap: 8px; color: var(--vy-ink-3); }
.search-field input { width: 100%; border: 0; outline: 0; background: transparent; font: inherit; font-size: 13px; font-weight: 700; color: var(--vy-ink); }
.product-list { display: grid; gap: 8px; max-height: 650px; overflow: auto; padding-right: 2px; }
.product-row { width: 100%; min-height: 68px; padding: 10px 12px; border: 1px solid var(--vy-line); border-radius: 13px; background: var(--vy-surface-2); display: grid; grid-template-columns: minmax(0, 1fr) auto 54px 88px auto; align-items: center; gap: 10px; text-align: left; }
.product-row:hover, .product-row.active { border-color: rgba(242, 135, 5, 0.42); background: #fff; box-shadow: var(--vy-shadow-sm); }
.product-main strong, .product-main small { display: block; }
.product-main strong { font-size: 13px; font-weight: 900; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.product-main small { margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 700; }
.stock-pill { padding: 4px 9px; border-radius: 999px; font-size: 11px; font-weight: 900; white-space: nowrap; }
.stock-pill.ok { background: rgba(63, 143, 92, 0.12); color: var(--vy-success); }
.stock-pill.warn { background: var(--vy-cream); color: #6b4a12; }
.stock-pill.danger { background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); }
.stock-number, .price { font-size: 13px; font-weight: 900; text-align: right; }
.price small { display: block; margin-top: 3px; color: var(--vy-ink-3); font-size: 10px; font-weight: 800; white-space: nowrap; }
.row-actions { display: inline-flex; gap: 6px; color: var(--vy-orange-deep); }
.row-actions svg { width: 30px; height: 30px; padding: 7px; border: 1px solid var(--vy-line); border-radius: 9px; background: #fff; }
.row-actions svg:last-child { color: var(--vy-danger); }
.mini-action { height: 38px; padding: 0 12px; border-radius: 11px; background: var(--vy-ink); color: #fff; display: inline-flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 900; }
table { width: 100%; border-collapse: collapse; font-size: 13px; }
th { padding: 10px 8px; color: var(--vy-ink-3); font-size: 11px; text-transform: uppercase; text-align: left; white-space: nowrap; }
td { padding: 13px 8px; border-top: 1px solid var(--vy-line-2); color: var(--vy-ink-2); vertical-align: middle; }
td strong { color: var(--vy-ink); }
.table-actions { text-align: right; white-space: nowrap; }
.table-actions button { width: 30px; height: 30px; border-radius: 9px; background: var(--vy-surface-2); color: var(--vy-orange-deep); display: inline-flex; align-items: center; justify-content: center; margin-left: 4px; }
.table-actions .danger { color: var(--vy-danger); }
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
input, select, textarea { width: 100%; padding: 12px 13px; border: 1px solid var(--vy-line); border-radius: 10px; background: var(--vy-surface); color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 600; }
input[type="file"] { padding: 10px; }
input:focus, select:focus, textarea:focus { outline: 2px solid rgba(242, 135, 5, 0.22); border-color: var(--vy-orange); }
label.invalid > span { color: var(--vy-danger); }
label.invalid input, label.invalid select, label.invalid textarea { border-color: var(--vy-danger); background: rgba(196, 69, 42, 0.04); }
label.invalid input:focus, label.invalid select:focus, label.invalid textarea:focus { outline-color: rgba(196, 69, 42, 0.18); border-color: var(--vy-danger); }
label.invalid :deep(.select-box) { border-color: var(--vy-danger); background: rgba(196, 69, 42, 0.04); }
label.invalid :deep(.select-box.active) { outline-color: rgba(196, 69, 42, 0.18); border-color: var(--vy-danger); }
.image-field { display: grid; grid-template-columns: minmax(220px, 0.85fr) minmax(260px, 1.15fr); gap: 14px; align-items: end; }
.entity-modal footer { justify-content: flex-end; padding-top: 18px; margin-top: 18px; border-top: 1px solid var(--vy-line-2); }
@media (max-width: 1180px) { .summary-grid { grid-template-columns: repeat(2, 1fr); } .content-grid { grid-template-columns: 1fr; } }
@media (max-width: 720px) { .workspace { padding: 24px 20px 32px; } .page-header, .card-header, .entity-modal header, .entity-modal footer { align-items: stretch; flex-direction: column; } .header-actions, .header-actions .vy-btn { width: 100%; } .summary-grid, .modal-grid, .image-field { grid-template-columns: 1fr; } .product-row { grid-template-columns: 1fr; } .search-field { min-width: 0; width: 100%; } .lots-card { overflow-x: auto; } table { min-width: 760px; } }
</style>
