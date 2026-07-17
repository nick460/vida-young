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
const categorias = ref([]);
const tiposClientePublico = ref([]);
const descuentosCliente = ref([]);
const query = ref("");
const productModalOpen = ref(false);
const editingProductId = ref(null);
const productImageFile = ref(null);
const productImagePreview = ref("");
const productPublicImageFile = ref(null);
const productPublicImagePreview = ref("");

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
  precioPublico: 0,
  pv: 0,
  qp: 0,
  cr: 0,
  imagenUrl: "",
  imagenPublicaUrl: "",
  listarEnShop: false
});

const productDiscountForm = reactive({});

const categoryForm = reactive({
  nombre: "",
  sigla: ""
});

const productErrors = reactive({});
const categoryErrors = reactive({});
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
  categorias.value.length || new Set(productos.value.map((producto) => producto.categoria).filter(Boolean)).size
);

const sortedCategories = computed(() =>
  [...categorias.value].sort((left, right) => left.nombre.localeCompare(right.nombre))
);

const selectedCategory = computed(() =>
  sortedCategories.value.find((categoria) => categoria.nombre === productForm.categoria)
);

const nextSkuPreview = computed(() => {
  if (editingProductId.value && productForm.sku) {
    return productForm.sku;
  }

  if (!selectedCategory.value?.sigla) {
    return "Se generara al guardar";
  }

  const prefix = `${selectedCategory.value.sigla.toLowerCase()}-`;
  const max = productos.value
    .map((producto) => String(producto.sku || ""))
    .filter((sku) => sku.toLowerCase().startsWith(prefix))
    .map((sku) => Number(sku.slice(prefix.length)) || 0)
    .reduce((highest, value) => Math.max(highest, value), 0);

  return `${prefix}${String(max + 1).padStart(3, "0")}`;
});

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

  if (!productForm.categoria) {
    errors.categoria = "Selecciona una categoria registrada.";
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

  if (Number(productForm.cr) < 0) {
    errors.cr = "El CR no puede ser negativo.";
  }

  setErrors(productErrors, errors);
  return Object.keys(errors).length === 0;
}

function validateCategoryForm() {
  const errors = {};
  const sigla = categoryForm.sigla.trim().toLowerCase().replace(/[^a-z0-9]/g, "");

  if (!categoryForm.nombre.trim()) {
    errors.nombre = "Ingresa el nombre de la categoria.";
  }

  if (sigla.length < 2 || sigla.length > 12) {
    errors.sigla = "La sigla debe tener entre 2 y 12 caracteres.";
  }

  setErrors(categoryErrors, errors);
  return Object.keys(errors).length === 0;
}

async function loadAll() {
  loading.value = true;
  error.value = "";

  try {
    const [productosData, categoriasData, tiposData] = await Promise.all([
      apiRequest("/api/productos"),
      apiRequest("/api/producto-categorias"),
      apiRequest("/api/tipos-cliente-publico")
    ]);
    productos.value = productosData;
    categorias.value = categoriasData;
    tiposClientePublico.value = tiposData;
    const discountLists = await Promise.all(tiposData.map((tipo) => apiRequest(`/api/tipos-cliente-publico/${tipo.id}/descuentos`)));
    descuentosCliente.value = discountLists.flat();
  } catch {
    error.value = "No se pudo cargar inventario. Verifica que el backend este activo y la sesion sea valida.";
  } finally {
    loading.value = false;
  }
}

function resetDiscountForm() {
  Object.keys(productDiscountForm).forEach((key) => delete productDiscountForm[key]);
  tiposClientePublico.value.forEach((tipo) => {
    productDiscountForm[tipo.id] = 0;
  });
}

async function saveCategory() {
  if (!validateCategoryForm()) {
    return;
  }

  try {
    await apiRequest("/api/producto-categorias", {
      method: "POST",
      body: JSON.stringify({
        nombre: categoryForm.nombre,
        sigla: categoryForm.sigla
      })
    });
    Object.assign(categoryForm, { nombre: "", sigla: "" });
    setErrors(categoryErrors, {});
    await loadAll();
    await showSuccess("Categoria guardada", "La categoria se registro correctamente.");
  } catch (exception) {
    await showError("No se pudo guardar", exception.message || "Revisa la categoria.");
  }
}

function resetProductForm() {
  editingProductId.value = null;
  productImageFile.value = null;
  productImagePreview.value = "";
  productPublicImageFile.value = null;
  productPublicImagePreview.value = "";
  productSubmitted.value = false;
  setErrors(productErrors, {});
  resetDiscountForm();
  Object.assign(productForm, {
    sku: "",
    nombre: "",
    descripcion: "",
    categoria: "",
      precio: 0,
      precioPublico: 0,
      pv: 0,
    qp: 0,
    cr: 0,
    imagenUrl: "",
    imagenPublicaUrl: "",
    listarEnShop: false
  });
}

function openProductModal(producto = null) {
  if (producto) {
    editingProductId.value = producto.id;
    productImageFile.value = null;
    productImagePreview.value = producto.imagenUrl || "";
    productPublicImageFile.value = null;
    productPublicImagePreview.value = producto.imagenPublicaUrl || "";
    Object.assign(productForm, {
      sku: producto.sku || "",
      nombre: producto.nombre || "",
      descripcion: producto.descripcion || "",
      categoria: producto.categoria || "",
      precio: producto.precio || 0,
      precioPublico: producto.precioPublico || producto.precio || 0,
      pv: producto.pv || 0,
      qp: producto.qp || 0,
      cr: producto.cr || 0,
      imagenUrl: producto.imagenUrl || "",
      imagenPublicaUrl: producto.imagenPublicaUrl || "",
      listarEnShop: Boolean(producto.listarEnShop)
    });
    resetDiscountForm();
    descuentosCliente.value
      .filter((descuento) => Number(descuento.producto?.id) === Number(producto.id))
      .forEach((descuento) => {
        productDiscountForm[descuento.tipoCliente?.id] = Number(descuento.descuentoMonto || 0);
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

function handleProductPublicImage(event) {
  const [file] = event.target.files || [];
  productPublicImageFile.value = file || null;
  productPublicImagePreview.value = file ? URL.createObjectURL(file) : productForm.imagenPublicaUrl;
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
        precioPublico: Number(productForm.precioPublico || productForm.precio || 0),
        pv: Number(productForm.pv || 0),
        qp: Number(productForm.qp || 0),
        cr: Number(productForm.cr || 0),
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

    if (productPublicImageFile.value) {
      const formData = new FormData();
      formData.append("imagen", productPublicImageFile.value);
      await apiRequest(`/api/productos/${savedProduct.id}/imagen-publica`, {
        method: "POST",
        body: formData
      });
    }

    await Promise.all(tiposClientePublico.value.map((tipo) => apiRequest(`/api/productos/${savedProduct.id}/descuentos-cliente`, {
      method: "POST",
      body: JSON.stringify({
        tipoClienteId: tipo.id,
        descuentoMonto: Number(productDiscountForm[tipo.id] || 0)
      })
    })));

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
        <section class="category-manager">
          <div>
            <h2>Categorias de producto</h2>
            <p>Registra categorias con sigla para generar SKU automaticos.</p>
          </div>
          <form class="category-form" @submit.prevent="saveCategory">
            <label :class="{ invalid: categoryErrors.nombre }">
              <span>Categoria</span>
              <input v-model.trim="categoryForm.nombre" placeholder="Malteada" />
              <small v-if="categoryErrors.nombre">{{ categoryErrors.nombre }}</small>
            </label>
            <label :class="{ invalid: categoryErrors.sigla }">
              <span>Sigla</span>
              <input v-model.trim="categoryForm.sigla" maxlength="12" placeholder="mal" />
              <small v-if="categoryErrors.sigla">{{ categoryErrors.sigla }}</small>
            </label>
            <button class="vy-btn vy-btn-primary" type="submit">
              <Plus :size="14" />
              Categoria
            </button>
          </form>
          <div class="category-pills">
            <span v-for="categoria in sortedCategories" :key="categoria.id">
              {{ categoria.nombre }} <strong>{{ categoria.sigla }}</strong>
            </span>
          </div>
        </section>

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
              <small>{{ producto.sku }} · {{ producto.categoria || "Sin categoria" }} · CR {{ money(producto.cr) }}</small>
            </div>
            <span class="shop-state" :class="{ disabled: !producto.listarEnShop }">
              <Eye v-if="producto.listarEnShop" :size="14" />
              <EyeOff v-else :size="14" />
              {{ producto.listarEnShop ? "Shop" : "Oculto" }}
            </span>
            <span class="price">
              Bs. {{ money(producto.precio) }}
              <small>PV {{ money(producto.pv) }} · QP {{ money(producto.qp) }} · CR {{ money(producto.cr) }}</small>
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
      <div v-if="productModalOpen" class="modal-backdrop">
        <form class="entity-modal" novalidate @submit.prevent="saveProduct">
          <header>
            <div>
              <span class="vy-eyebrow">Producto</span>
              <h2>{{ editingProductId ? "Editar producto" : "Nuevo producto" }}</h2>
            </div>
            <button type="button" aria-label="Cerrar" @click="closeProductModal">x</button>
          </header>
          <div class="modal-grid">
            <label>
              <span>SKU</span>
              <input :value="nextSkuPreview" disabled />
            </label>
            <label :class="{ invalid: productErrors.nombre }">
              <span>Nombre</span>
              <input v-model.trim="productForm.nombre" />
              <small v-if="productErrors.nombre">{{ productErrors.nombre }}</small>
            </label>
            <label :class="{ invalid: productErrors.categoria }">
              <span>Categoria</span>
              <select v-model="productForm.categoria">
                <option value="">Selecciona categoria</option>
                <option v-for="categoria in sortedCategories" :key="categoria.id" :value="categoria.nombre">
                  {{ categoria.nombre }} ({{ categoria.sigla }})
                </option>
              </select>
              <small v-if="productErrors.categoria">{{ productErrors.categoria }}</small>
            </label>
            <label :class="{ invalid: productErrors.precio }">
              <span>Precio</span>
              <input v-model.number="productForm.precio" type="number" min="0" step="0.01" />
              <small v-if="productErrors.precio">{{ productErrors.precio }}</small>
            </label>
            <label>
              <span>Precio publico</span>
              <input v-model.number="productForm.precioPublico" type="number" min="0" step="0.01" />
            </label>
            <label v-for="tipo in tiposClientePublico" :key="tipo.id">
              <span>Descuento {{ tipo.nombre }}</span>
              <input v-model.number="productDiscountForm[tipo.id]" type="number" min="0" step="0.01" />
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
            <label :class="{ invalid: productErrors.cr }">
              <span>CR</span>
              <input v-model.number="productForm.cr" type="number" min="0" step="0.01" />
              <small v-if="productErrors.cr">{{ productErrors.cr }}</small>
            </label>
            <label class="toggle-field full-field">
              <input v-model="productForm.listarEnShop" type="checkbox" />
              <span>Listar este producto en /shop</span>
            </label>
            <section class="image-field full-field">
              <label>
                <span>Imagen interna</span>
                <input type="file" accept="image/png,image/jpeg,image/webp" @change="handleProductImage" />
              </label>
              <VyProductImage :grad="productImagePreview || productForm.imagenUrl || 'linear-gradient(135deg, #F2E7C4 0%, #F28705 100%)'" :h="120" />
            </section>
            <section class="image-field full-field">
              <label>
                <span>Imagen publica</span>
                <input type="file" accept="image/png,image/jpeg,image/webp" @change="handleProductPublicImage" />
                <small>Se usa solo en /tienda/nombreusuario. Si esta vacia, usa la imagen interna.</small>
              </label>
              <VyProductImage :grad="productPublicImagePreview || productForm.imagenPublicaUrl || productForm.imagenUrl || 'linear-gradient(135deg, #F2E7C4 0%, #F28705 100%)'" :h="120" />
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
.category-manager { padding-bottom: 18px; margin-bottom: 18px; border-bottom: 1px solid var(--vy-line-2); }
.category-manager h2, .card-header h2 { font-size: 16px; font-weight: 800; }
.category-manager p { margin-top: 4px; color: var(--vy-ink-2); font-size: 14px; }
.category-form { display: grid; grid-template-columns: minmax(180px, 1fr) minmax(120px, 0.45fr) auto; gap: 12px; align-items: end; margin-top: 14px; }
.category-form .vy-btn { min-height: 42px; border-radius: 12px; font-weight: 900; }
.category-pills { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 12px; }
.category-pills span { min-height: 30px; padding: 0 10px; border: 1px solid var(--vy-line); border-radius: 999px; background: var(--vy-surface-2); color: var(--vy-ink-2); display: inline-flex; align-items: center; gap: 7px; font-size: 12px; font-weight: 800; }
.category-pills strong { color: var(--vy-orange-deep); font-family: var(--font-mono); font-size: 11px; text-transform: uppercase; }
.card-header { margin-bottom: 16px; }
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
input, textarea, select { width: 100%; padding: 12px 13px; border: 1px solid var(--vy-line); border-radius: 10px; background: var(--vy-surface); color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 600; }
input:disabled { background: var(--vy-surface-2); color: var(--vy-ink-3); cursor: not-allowed; }
input[type="file"] { padding: 10px; }
input:focus, textarea:focus, select:focus { outline: 2px solid rgba(242, 135, 5, 0.22); border-color: var(--vy-orange); }
label.invalid > span { color: var(--vy-danger); }
label.invalid input, label.invalid textarea, label.invalid select { border-color: var(--vy-danger); background: rgba(196, 69, 42, 0.04); }
.toggle-field { min-height: 46px; padding: 12px 13px; border: 1px solid var(--vy-line); border-radius: 10px; background: var(--vy-surface-2); display: flex; align-items: center; gap: 10px; }
.toggle-field input { width: 18px; height: 18px; padding: 0; accent-color: var(--vy-orange); }
.toggle-field span { margin: 0; color: var(--vy-ink); font-size: 13px; text-transform: none; }
.image-field { display: grid; grid-template-columns: minmax(220px, 0.85fr) minmax(260px, 1.15fr); gap: 14px; align-items: end; }
.entity-modal footer { justify-content: flex-end; padding-top: 18px; margin-top: 18px; border-top: 1px solid var(--vy-line-2); }
@media (max-width: 1180px) { .product-row { grid-template-columns: 58px minmax(0, 1fr) auto auto; } .price { display: none; } }
@media (max-width: 720px) { .workspace { padding: 24px 20px 32px; } .page-header, .card-header, .entity-modal header, .entity-modal footer { align-items: stretch; flex-direction: column; } .header-actions, .header-actions .vy-btn { width: 100%; } .summary-grid, .modal-grid, .image-field, .category-form { grid-template-columns: 1fr; } .product-row { grid-template-columns: 1fr; } .search-field { min-width: 0; width: 100%; } .price { text-align: left; } .row-actions button { width: 100%; } }
</style>
