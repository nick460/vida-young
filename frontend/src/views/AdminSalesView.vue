<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import $ from "jquery";
import select2 from "select2";
import "select2/dist/css/select2.css";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { jsPDF } from "jspdf";
import {
  CheckCircle2,
  CircleX,
  Ban,
  ClipboardCheck,
  ClipboardList,
  FileText,
  MoreVertical,
  PackageCheck,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  ShoppingBag,
  Store,
  Trash2,
  X
} from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import logoFull from "../assets/logoFull.png";
import logoMark from "../assets/logoMark.png";

select2($);

const personas = ref([]);
const usuarios = ref([]);
const productos = ref([]);
const compras = ref([]);
const comprasPublicas = ref([]);
const tiposClientePublico = ref([]);
const productosPublicos = ref([]);
const periodosVenta = ref([]);
const selectedPeriodoId = ref("");
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const personaQuery = ref("");
const productQuery = ref("");
const selectedPersonaId = ref("");
const ventaItems = ref([]);
const discountAmount = ref("");
const discountConcept = ref("");
const editingCompra = ref(null);
const saleModalOpen = ref(false);
const publicSaleModalOpen = ref(false);
const personaSelect = ref(null);
const periodoSelect = ref(null);
const publicDistributorSelect = ref(null);
const publicTipoClienteSelect = ref(null);
const proofModalCompra = ref(null);
const receiptModalCompra = ref(null);
const publicReviewModalCompra = ref(null);
const detalleModalCompra = ref(null);
const detalleMovimientos = ref([]);
const detalleLoading = ref(false);
const detalleError = ref("");
const activeActionMenu = ref("");
const ventasPage = ref(1);
const publicVentasPage = ref(1);
const pageSize = 8;
const ventanillaQuery = ref("");

const cajaCode = ref(generateCajaCode());
const publicCajaCode = ref(generateCajaCode());
const selectedPublicDistributorId = ref("");
const publicProductQuery = ref("");
const publicVentaItems = ref([]);
const publicSaleSubmitted = ref(false);
const searchingPublicClient = ref(false);
const publicClientLookupMessage = ref("");
const publicTouched = reactive({
  distribuidor: false,
  tipoClienteCodigo: false,
  clienteNombres: false,
  clienteDocumento: false,
  clienteEmail: false,
  envioCiudad: false,
  envioDireccion: false,
  productos: false
});
const publicForm = reactive({
  tipoClienteCodigo: "NORMAL",
  clienteNombres: "",
  clienteApellidos: "",
  clienteDocumento: "",
  clienteEmail: "",
  clienteTelefono: "",
  envioRequiere: false,
  envioDireccion: "",
  envioCiudad: "",
  envioReferencia: ""
});
let bodyOverflowBeforeModal = "";
let pageScrollLocked = false;
const alertClasses = {
  popup: "vy-swal-popup",
  title: "vy-swal-title",
  htmlContainer: "vy-swal-text",
  confirmButton: "vy-swal-confirm",
  cancelButton: "vy-swal-cancel"
};

const filteredPersonas = computed(() => {
  const text = personaQuery.value.trim().toLowerCase();
  if (!text) return personas.value;
  return personas.value.filter((persona) => [
    persona.nombres,
    persona.apellidos,
    persona.documento,
    persona.email
  ].some((value) => String(value || "").toLowerCase().includes(text)));
});

const filteredProducts = computed(() => {
  const text = productQuery.value.trim().toLowerCase();
  if (!text) return productos.value;
  return productos.value.filter((producto) => [
    producto.nombre,
    producto.sku,
    producto.categoria
  ].some((value) => String(value || "").toLowerCase().includes(text)));
});

const filteredPublicProducts = computed(() => {
  const text = publicProductQuery.value.trim().toLowerCase();
  if (!text) return productosPublicos.value;
  return productosPublicos.value.filter((producto) => [
    producto.nombre,
    producto.sku,
    producto.categoria
  ].some((value) => String(value || "").toLowerCase().includes(text)));
});

const selectedPersona = computed(() =>
  personas.value.find((persona) => Number(persona.id) === Number(selectedPersonaId.value))
);

const selectedPublicDistributor = computed(() =>
  personas.value.find((persona) => Number(persona.id) === Number(selectedPublicDistributorId.value))
);

const selectedPublicDistributorUser = computed(() => usuarioDePersona(selectedPublicDistributor.value));

const selectedPublicDistributorUsername = computed(() =>
  selectedPublicDistributorUser.value?.username || ""
);

const visibleCompras = computed(() => {
  const text = ventanillaQuery.value.trim().toLowerCase();
  return compras.value
    .filter((compra) => ["PENDIENTE", "VALIDADA", "ANULADA"].includes(compra.estadoCompra))
    .filter((compra) => {
      if (!text) return true;
      const persona = compra.persona || {};
      return [
        compra.id,
        compra.codigoPago,
        compra.metodoPago,
        compra.estadoCompra,
        persona.nombres,
        persona.apellidos,
        persona.documento,
        persona.email
      ].some((value) => String(value ?? "").toLowerCase().includes(text));
    });
});

const visibleComprasPublicas = computed(() => {
  const text = ventanillaQuery.value.trim().toLowerCase();
  return comprasPublicas.value
    .filter((compra) => ["PENDIENTE", "VALIDADA"].includes(compra.estadoCompra))
    .filter((compra) => {
      if (!text) return true;
      const distribuidor = compra.distribuidor || {};
      return [
        compra.id,
        compra.estadoCompra,
        compra.clienteNombres,
        compra.clienteApellidos,
        compra.clienteTelefono,
        compra.clienteEmail,
        compra.tipoCliente?.nombre,
        distribuidor.nombres,
        distribuidor.apellidos,
        distribuidor.documento
      ].some((value) => String(value ?? "").toLowerCase().includes(text));
    });
});

const ventasTotalPages = computed(() => totalPages(visibleCompras.value.length));
const publicVentasTotalPages = computed(() => totalPages(visibleComprasPublicas.value.length));
const paginatedCompras = computed(() => paginateItems(visibleCompras.value, ventasPage.value));
const paginatedComprasPublicas = computed(() => paginateItems(visibleComprasPublicas.value, publicVentasPage.value));

const selectedPeriodo = computed(() =>
  periodosVenta.value.find((periodo) => Number(periodo.id) === Number(selectedPeriodoId.value))
);

const modalOpen = computed(() =>
  saleModalOpen.value
  || publicSaleModalOpen.value
  || Boolean(proofModalCompra.value)
  || Boolean(receiptModalCompra.value)
  || Boolean(publicReviewModalCompra.value)
);

const saleSubtotal = computed(() =>
  ventaItems.value.reduce((sum, item) => sum + Number(item.precio || 0) * Number(item.cantidad || 0), 0)
);

const rawDiscountAmount = computed(() => Number(discountAmount.value || 0));
const discountAmountNumber = computed(() => Math.max(0, rawDiscountAmount.value));
const discountError = computed(() => {
  if (rawDiscountAmount.value < 0) return "El descuento no puede ser negativo.";
  if (discountAmountNumber.value <= 0) return "";
  if (discountAmountNumber.value > saleSubtotal.value) return "El descuento no puede ser mayor al total de la venta.";
  if (!discountConcept.value.trim()) return "Ingresa el concepto del descuento.";
  return "";
});
const saleTotal = computed(() => Math.max(0, saleSubtotal.value - discountAmountNumber.value));

const salePv = computed(() =>
  ventaItems.value.reduce((sum, item) => sum + Number(item.pv || 0) * Number(item.cantidad || 0), 0)
);

const saleQp = computed(() =>
  ventaItems.value.reduce((sum, item) => sum + Number(item.qp || 0) * Number(item.cantidad || 0), 0)
);

const saleCr = computed(() =>
  ventaItems.value.reduce((sum, item) => sum + Number(item.cr || 0) * Number(item.cantidad || 0), 0)
);

const publicSaleSubtotal = computed(() =>
  publicVentaItems.value.reduce((sum, item) => sum + Number(item.precioFinal || 0) * Number(item.cantidad || 0), 0)
);

const publicSaleEmpresa = computed(() =>
  publicVentaItems.value.reduce((sum, item) => sum + Number(item.precioDistribuidor || 0) * Number(item.cantidad || 0), 0)
);

const publicSaleDescuento = computed(() =>
  publicVentaItems.value.reduce((sum, item) => sum + Number(item.descuento || 0) * Number(item.cantidad || 0), 0)
);

const publicSaleGanancia = computed(() =>
  Math.max(0, publicSaleSubtotal.value - publicSaleEmpresa.value)
);

const publicValidationErrors = computed(() => {
  const errors = {};
  const email = publicForm.clienteEmail.trim();

  errors.distribuidor = !selectedPublicDistributorId.value
    ? "Selecciona el distribuidor."
    : !selectedPublicDistributorUsername.value
      ? "El distribuidor no tiene usuario para su tienda publica."
      : "";
  errors.tipoClienteCodigo = !publicForm.tipoClienteCodigo ? "Selecciona el tipo de cliente." : "";
  errors.clienteNombres = !publicForm.clienteNombres.trim() ? "Ingresa el nombre del cliente." : "";
  errors.clienteDocumento = !publicForm.clienteDocumento.trim() ? "Ingresa el documento del cliente." : "";
  errors.clienteEmail = email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ? "Ingresa un correo valido." : "";
  errors.envioCiudad = publicForm.envioRequiere && !publicForm.envioCiudad.trim() ? "Ingresa la ciudad." : "";
  errors.envioDireccion = publicForm.envioRequiere && !publicForm.envioDireccion.trim() ? "Ingresa la direccion." : "";
  errors.productos = !publicVentaItems.value.length ? "Agrega al menos un producto." : "";

  return errors;
});

const publicSaleHasErrors = computed(() =>
  Object.values(publicValidationErrors.value).some(Boolean)
);

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function compraDescuento(compra) {
  return Number(compra?.descuentoMonto || 0);
}

function compraTieneDescuento(compra) {
  return compraDescuento(compra) > 0;
}

function compraSubtotalBruto(compra) {
  return Number(compra?.subtotal || 0) + compraDescuento(compra);
}

function generateCajaCode() {
  return String(Math.floor(100000 + Math.random() * 900000));
}

function fullName(persona) {
  return `${persona?.nombres || ""} ${persona?.apellidos || ""}`.trim() || "Sin nombre";
}

function usuarioDePersona(persona) {
  if (!persona?.id) return null;
  return usuarios.value.find((usuario) => Number(usuario.persona?.id) === Number(persona.id)) || null;
}

function isClubRoyaleProduct(producto) {
  return Number(producto?.cr || 0) > 0;
}

function touchPublicField(field) {
  if (field in publicTouched) {
    publicTouched[field] = true;
  }
}

function publicFieldError(field) {
  if (!publicSaleSubmitted.value && !publicTouched[field]) return "";
  return publicValidationErrors.value[field] || "";
}

function resetPublicTouched() {
  Object.keys(publicTouched).forEach((field) => {
    publicTouched[field] = false;
  });
  publicSaleSubmitted.value = false;
}

function applyPublicClientLookup(cliente) {
  publicForm.clienteNombres = cliente.nombres || "";
  publicForm.clienteApellidos = cliente.apellidos || "";
  publicForm.clienteDocumento = cliente.documento || publicForm.clienteDocumento;
  publicForm.clienteEmail = cliente.email || "";
  publicForm.clienteTelefono = cliente.telefono || "";
  publicForm.envioRequiere = Boolean(cliente.envioRequiere);
  publicForm.envioDireccion = cliente.envioDireccion || "";
  publicForm.envioCiudad = cliente.envioCiudad || "";
  publicForm.envioReferencia = cliente.envioReferencia || "";
  ["clienteNombres", "clienteDocumento", "clienteEmail", "envioCiudad", "envioDireccion"].forEach(touchPublicField);
}

function firstPublicValidationError() {
  return Object.values(publicValidationErrors.value).find(Boolean) || "Revisa los campos marcados.";
}

function currentUserName() {
  try {
    const usuario = JSON.parse(localStorage.getItem("vy_usuario") || "null");
    const personaName = [usuario?.persona?.nombres, usuario?.persona?.apellidos]
      .filter(Boolean)
      .join(" ")
      .trim();
    return personaName || usuario?.username || "Usuario del sistema";
  } catch {
    return "Usuario del sistema";
  }
}

function isImageProof(compra) {
  return String(compra?.comprobantePagoTipo || "").startsWith("image/");
}

function openProofModal(compra) {
  proofModalCompra.value = compra;
}

function closeProofModal() {
  proofModalCompra.value = null;
}

function openReceiptModal(compra) {
  receiptModalCompra.value = compra;
}

function closeReceiptModal() {
  receiptModalCompra.value = null;
}

function openDetallesModal(compra) {
  detalleModalCompra.value = compra;
  detalleMovimientos.value = [];
  detalleError.value = "";
  cargarMovimientosCompra(compra.id);
}

function closeDetallesModal() {
  detalleModalCompra.value = null;
  detalleMovimientos.value = [];
  detalleError.value = "";
}

async function cargarMovimientosCompra(compraId) {
  detalleLoading.value = true;
  detalleError.value = "";
  try {
    const data = await apiRequest(`/api/compras/${compraId}/movimientos`);
    detalleMovimientos.value = Array.isArray(data) ? data : [];
  } catch (exception) {
    detalleError.value = exception.message || "No se pudieron cargar los movimientos.";
  } finally {
    detalleLoading.value = false;
  }
}

const MOVIMIENTO_LABELS = {
  VOLUMEN_COMPRADOR: "Volumen del comprador",
  BONO_REFERIDO: "QP bono referido (hacia arriba)",
  BENEFICIO_ACTIVACION: "Beneficios de activacion",
  AJUSTE_BENEFICIO: "Ajustes retroactivos",
  CARTERA_EMPRESA: "Cartera de la empresa",
  ANULACION: "Anulaciones"
};

const MOVIMIENTO_ORDEN = [
  "VOLUMEN_COMPRADOR",
  "BONO_REFERIDO",
  "BENEFICIO_ACTIVACION",
  "AJUSTE_BENEFICIO",
  "CARTERA_EMPRESA",
  "ANULACION"
];

function movimientoLabel(origen) {
  return MOVIMIENTO_LABELS[origen] || origen || "Movimientos";
}

const movimientosAgrupados = computed(() => {
  const grupos = [];
  for (const origen of MOVIMIENTO_ORDEN) {
    const items = detalleMovimientos.value.filter((mov) => mov.origen === origen);
    if (!items.length) continue;
    grupos.push({
      origen,
      label: movimientoLabel(origen),
      items,
      total: items.reduce((acc, mov) => acc + Number(mov.monto || 0), 0)
    });
  }
  const otros = detalleMovimientos.value.filter((mov) => !MOVIMIENTO_ORDEN.includes(mov.origen));
  if (otros.length) {
    grupos.push({
      origen: "OTROS",
      label: "Otros movimientos",
      items: otros,
      total: otros.reduce((acc, mov) => acc + Number(mov.monto || 0), 0)
    });
  }
  return grupos;
});

function toggleActionMenu(key) {
  activeActionMenu.value = activeActionMenu.value === key ? "" : key;
}

function closeActionMenu() {
  activeActionMenu.value = "";
}

function lockPageScroll() {
  if (pageScrollLocked) return;
  bodyOverflowBeforeModal = document.body.style.overflow;
  document.body.style.overflow = "hidden";
  pageScrollLocked = true;
}

function unlockPageScroll() {
  if (!pageScrollLocked) return;
  document.body.style.overflow = bodyOverflowBeforeModal;
  bodyOverflowBeforeModal = "";
  pageScrollLocked = false;
}

function totalPages(totalItems) {
  return Math.max(1, Math.ceil(Number(totalItems || 0) / pageSize));
}

function paginateItems(items, page) {
  const start = (Math.max(1, page) - 1) * pageSize;
  return items.slice(start, start + pageSize);
}

function paginationStart(page, totalItems) {
  if (!totalItems) return 0;
  return (Math.max(1, page) - 1) * pageSize + 1;
}

function paginationEnd(page, totalItems) {
  return Math.min(Math.max(1, page) * pageSize, totalItems);
}

function setVentasPage(page) {
  ventasPage.value = Math.min(Math.max(1, page), ventasTotalPages.value);
}

function setPublicVentasPage(page) {
  publicVentasPage.value = Math.min(Math.max(1, page), publicVentasTotalPages.value);
}

function handleActionMenuDocumentClick() {
  closeActionMenu();
}

function openPublicReviewModal(compra) {
  publicReviewModalCompra.value = compra;
}

function closePublicReviewModal() {
  publicReviewModalCompra.value = null;
}

function destroyPersonaSelect2() {
  if (!personaSelect.value) return;
  const element = $(personaSelect.value);
  if (element.hasClass("select2-hidden-accessible")) {
    element.off("change.ventanilla");
    element.select2("destroy");
  }
}

function destroyPeriodoSelect2() {
  if (!periodoSelect.value) return;
  const element = $(periodoSelect.value);
  if (element.hasClass("select2-hidden-accessible")) {
    element.off("change.periodo-ventas");
    element.select2("destroy");
  }
}

function destroyPublicDistributorSelect2() {
  if (!publicDistributorSelect.value) return;
  const element = $(publicDistributorSelect.value);
  if (element.hasClass("select2-hidden-accessible")) {
    element.off("change.public-distributor");
    element.select2("destroy");
  }
}

function destroyPublicTipoClienteSelect2() {
  if (!publicTipoClienteSelect.value) return;
  const element = $(publicTipoClienteSelect.value);
  if (element.hasClass("select2-hidden-accessible")) {
    element.off("change.public-tipo-cliente");
    element.select2("destroy");
  }
}

async function initPersonaSelect2() {
  if (!saleModalOpen.value) return;

  await nextTick();
  if (!personaSelect.value) return;

  destroyPersonaSelect2();
  const element = $(personaSelect.value);
  element
    .select2({
      width: "100%",
      placeholder: "Selecciona una persona",
      allowClear: true,
      dropdownParent: $(".sale-modal"),
      language: {
        noResults: () => "Sin resultados",
        searching: () => "Buscando..."
      }
    })
    .val(selectedPersonaId.value || null)
    .trigger("change.select2");

  element.on("change.ventanilla", () => {
    selectedPersonaId.value = element.val() || "";
  });
}

async function initPeriodoSelect2() {
  await nextTick();
  if (!periodoSelect.value) return;

  destroyPeriodoSelect2();
  const element = $(periodoSelect.value);
  element
    .select2({
      width: "100%",
      placeholder: "Selecciona un mes",
      allowClear: false,
      dropdownParent: $(".admin-sales-view"),
      language: {
        noResults: () => "Sin resultados",
        searching: () => "Buscando..."
      }
    })
    .val(selectedPeriodoId.value || null)
    .trigger("change.select2");

  element.on("change.periodo-ventas", async () => {
    selectedPeriodoId.value = element.val() || "";
    await loadVentasPeriodo();
  });
}

async function initPublicDistributorSelect2() {
  if (!publicSaleModalOpen.value) return;

  await nextTick();
  if (!publicDistributorSelect.value) return;

  destroyPublicDistributorSelect2();
  const element = $(publicDistributorSelect.value);
  element
    .select2({
      width: "100%",
      placeholder: "Selecciona un distribuidor",
      allowClear: true,
      dropdownParent: $(".public-sale-modal"),
      language: {
        noResults: () => "Sin resultados",
        searching: () => "Buscando..."
      }
    })
    .val(selectedPublicDistributorId.value || null)
    .trigger("change.select2");

  element.on("change.public-distributor", () => {
    selectedPublicDistributorId.value = element.val() || "";
    touchPublicField("distribuidor");
  });
}

async function initPublicTipoClienteSelect2() {
  if (!publicSaleModalOpen.value) return;

  await nextTick();
  if (!publicTipoClienteSelect.value) return;

  destroyPublicTipoClienteSelect2();
  const element = $(publicTipoClienteSelect.value);
  element
    .select2({
      width: "100%",
      placeholder: "Selecciona un tipo de cliente",
      allowClear: false,
      dropdownParent: $(".public-sale-modal"),
      language: {
        noResults: () => "Sin resultados",
        searching: () => "Buscando..."
      }
    })
    .val(publicForm.tipoClienteCodigo || null)
    .trigger("change.select2");

  element.on("change.public-tipo-cliente", () => {
    publicForm.tipoClienteCodigo = element.val() || "";
    touchPublicField("tipoClienteCodigo");
  });
}

function resetSaleForm() {
  editingCompra.value = null;
  ventaItems.value = [];
  discountAmount.value = "";
  discountConcept.value = "";
  selectedPersonaId.value = "";
  personaQuery.value = "";
  productQuery.value = "";
  cajaCode.value = generateCajaCode();
}

function resetPublicSaleForm() {
  resetPublicTouched();
  selectedPublicDistributorId.value = "";
  publicProductQuery.value = "";
  publicClientLookupMessage.value = "";
  searchingPublicClient.value = false;
  productosPublicos.value = [];
  publicVentaItems.value = [];
  publicCajaCode.value = generateCajaCode();
  Object.assign(publicForm, {
    tipoClienteCodigo: tiposClientePublico.value[0]?.codigo || "NORMAL",
    clienteNombres: "",
    clienteApellidos: "",
    clienteDocumento: "",
    clienteEmail: "",
    clienteTelefono: "",
    envioRequiere: false,
    envioDireccion: "",
    envioCiudad: "",
    envioReferencia: ""
  });
}

function openSaleModal() {
  resetSaleForm();
  saleModalOpen.value = true;
}

function openPublicSaleModal() {
  resetPublicSaleForm();
  publicSaleModalOpen.value = true;
}

function openEditSaleModal(compra) {
  editingCompra.value = compra;
  selectedPersonaId.value = String(compra.persona?.id || "");
  cajaCode.value = compra.codigoPago || generateCajaCode();
  discountAmount.value = Number(compra.descuentoMonto || 0);
  discountConcept.value = compra.descuentoConcepto || "";
  personaQuery.value = "";
  productQuery.value = "";
  ventaItems.value = (compra.detalles || []).map((detalle) => ({
    id: detalle.producto?.id,
    nombre: detalle.producto?.nombre || "Producto",
    sku: detalle.producto?.sku || "",
    precio: Number(detalle.precioUnitario || detalle.producto?.precio || 0),
    pv: Number(detalle.pvUnitario || detalle.producto?.pv || 0),
    qp: Number(detalle.qpUnitario || detalle.producto?.qp || 0),
    cr: Number(detalle.crUnitario || detalle.producto?.cr || 0),
    cantidad: Number(detalle.cantidad || 1)
  })).filter((item) => item.id);
  saleModalOpen.value = true;
}

function closeSaleModal() {
  saleModalOpen.value = false;
  destroyPersonaSelect2();
}

function closePublicSaleModal() {
  publicSaleModalOpen.value = false;
  destroyPublicDistributorSelect2();
  destroyPublicTipoClienteSelect2();
}

function formatDateTime(value) {
  if (!value) return "Sin registro";
  return new Date(value).toLocaleString("es-BO", {
    dateStyle: "medium",
    timeStyle: "short"
  });
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function buildReceiptHtml(compra) {
  const detalles = (compra.detalles || []).map((detalle) => `
    <div class="product-block">
      <div class="detail-row">
        <span class="qty">${Number(detalle.cantidad || 0)}</span>
        <div class="product-info">
          <strong>${escapeHtml(detalle.producto?.nombre || "Producto")}</strong>
          <span>PV ${money(detalle.pvUnitario)} / QP ${money(detalle.qpUnitario)} / CR ${money(detalle.crUnitario)}</span>
        </div>
        <strong class="amount">Bs. ${money(detalle.subtotal)}</strong>
      </div>
      <div class="product-separator"></div>
    </div>
  `).join("");
  const printedAt = formatDateTime(new Date());
  const printedBy = currentUserName();
  const discount = compraDescuento(compra);
  const grossTotal = compraSubtotalBruto(compra);
  const saleUser = compra.usuarioRegistro || compra.usuarioValidacion || "Sistema";
  const discountRows = discount > 0 ? `
      <div class="row"><span>Descuento</span><span>- Bs. ${money(discount)}</span></div>
      <div class="discount-concept"><strong>Concepto:</strong> ${escapeHtml(compra.descuentoConcepto || "Sin concepto")}</div>
  ` : `
      <div class="row"><span>Descuento</span><span>Bs. 0.00</span></div>
  `;

  return `<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <title>Comprobante Compra #${escapeHtml(compra.id)}</title>
  <style>
    * { box-sizing: border-box; }
    @page { size: 8cm auto; margin: .5cm; }
    body { width: 7cm; margin: 0 auto; padding: 0; background: #fff; color: #111; font-family: Arial, sans-serif; font-size: 10pt; }
    .receipt { width: 7cm; margin: 0 auto; }
    .center { text-align: center; }
    .logo { width: 2.45cm; max-width: 100%; height: auto; object-fit: contain; margin: 0 auto .18cm; display: block; }
    .brand-name { margin: .08cm 0 0; font-size: 12pt; font-weight: 900; text-transform: uppercase; letter-spacing: .02cm; }
    .company-meta { margin-top: .05cm; font-size: 8.4pt; line-height: 1.35; text-transform: uppercase; }
    h1 { margin: .15cm 0 .05cm; font-size: 10.4pt; text-transform: uppercase; }
    .receipt-id { margin-top: .04cm; font-size: 8.7pt; font-weight: 700; }
    .divider { border-top: 1px solid #111; margin: .22cm 0; }
    .row { display: flex; justify-content: space-between; gap: .2cm; margin: .08cm 0; }
    .row span:first-child { font-weight: 700; }
    .row span:last-child { text-align: right; }
    .section-title { margin: .16cm 0 .12cm; font-size: 9pt; font-weight: 800; text-transform: uppercase; }
    .detail-header, .detail-row { display: grid; grid-template-columns: .9cm 1fr 1.7cm; gap: .12cm; align-items: start; }
    .detail-header { padding-bottom: .08cm; font-size: 8pt; font-weight: 800; text-transform: uppercase; border-bottom: 1px dashed #111; }
    .product-block { margin-top: .08cm; }
    .product-separator { width: 100%; border-top: 1px dotted #555; margin: .03cm 0; }
    .detail-row { padding: .12cm 0; }
    .qty, .amount { font-weight: 800; }
    .qty { text-align: center; }
    .amount { white-space: nowrap; text-align: right; font-size: 9pt; }
    .product-info { min-width: 0; }
    .product-info strong { display: block; line-height: 1.25; font-size: 9pt; }
    .product-info span { display: block; margin-top: .04cm; color: #333; font-size: 8.2pt; line-height: 1.25; }
    .totals { margin-top: .16cm; }
    .discount-concept { margin: .08cm 0 .02cm; color: #333; font-size: 8.3pt; line-height: 1.25; }
    .account-pay { margin-top: .12cm; padding-top: .12cm; border-top: 1px solid #111; font-size: 10.6pt; font-weight: 900; }
    .signature { margin-top: .7cm; text-align: center; }
    .signature-space { height: 1.45cm; }
    .signature-line { border-top: 1px solid #111; padding-top: .08cm; font-size: 8.8pt; font-weight: 700; }
    footer { margin-top: .28cm; text-align: center; font-size: 8.2pt; line-height: 1.35; }
    @media print {
      html, body { width: 7cm; }
    }
  </style>
</head>
<body>
  <main class="receipt">
    <section class="center">
      <img class="logo" src="${logoMark}" alt="Vida Young">
    </section>
    <section class="center">
      <div class="brand-name">VIDAYOUNG S.R.L.</div>
      <div class="company-meta">TELEFONO: +591 66880316</div>
      <div class="company-meta">DISTRIBUIDOR: CASA MATRIZ</div>
      <h1>Comprobante de compra</h1>
      <div class="receipt-id">Compra #${escapeHtml(compra.id)} - ${escapeHtml(compra.estadoCompra || "Sin estado")}</div>
    </section>

    <div class="divider"></div>
    <section>
      <div class="row"><span>Cliente</span><span>${escapeHtml(fullName(compra.persona))}</span></div>
      <div class="row"><span>Documento</span><span>${escapeHtml(compra.persona?.documento || "Sin documento")}</span></div>
      <div class="row"><span>Fecha y hora compra</span><span>${escapeHtml(formatDateTime(compra.fechaCompra))}</span></div>
      <div class="row"><span>Venta realizada por</span><span>${escapeHtml(saleUser)}</span></div>
    </section>

    <div class="divider"></div>
    <section>
      <div class="section-title">Descripcion detalle compra</div>
      <div class="detail-header">
        <span>Cant.</span>
        <span>Producto</span>
        <span style="text-align:right;">Sub total</span>
      </div>
      ${detalles || '<div class="product-block"><div class="detail-row"><span class="qty">0</span><div class="product-info"><strong>Sin productos</strong></div><strong class="amount">Bs. 0.00</strong></div><div class="product-separator"></div></div>'}
    </section>

    <section class="totals">
      <div class="row"><span>Total</span><span>Bs. ${money(grossTotal)}</span></div>
      ${discountRows}
      <div class="row account-pay"><span>A cuenta</span><span>Bs. ${money(compra.subtotal)}</span></div>
    </section>

    <section class="signature">
      <div class="signature-space"></div>
      <div class="signature-line">Sello de la empresa</div>
    </section>

    <footer>
      <div>Usuario que imprime: ${escapeHtml(printedBy)}</div>
      <div>Fecha y hora de impresion: ${escapeHtml(printedAt)}</div>
    </footer>
  </main>
</body>
</html>`;
}

function imageToDataUrl(src) {
  return new Promise((resolve) => {
    const image = new Image();
    image.crossOrigin = "anonymous";
    image.onload = () => {
      const canvas = document.createElement("canvas");
      canvas.width = image.naturalWidth;
      canvas.height = image.naturalHeight;
      const context = canvas.getContext("2d");
      context.drawImage(image, 0, 0);
      resolve(canvas.toDataURL("image/png"));
    };
    image.onerror = () => resolve(null);
    image.src = src;
  });
}

function addPdfText(doc, text, x, y, options = {}) {
  const lines = doc.splitTextToSize(String(text || ""), options.maxWidth || 170);
  doc.text(lines, x, y);
  return y + lines.length * (options.lineHeight || 5);
}

async function downloadReceiptPdf(compra) {
  const doc = new jsPDF({ unit: "mm", format: "a4" });
  const logo = await imageToDataUrl(logoFull);
  const pageWidth = doc.internal.pageSize.getWidth();
  const margin = 14;
  let y = 16;

  doc.setFillColor(255, 255, 255);
  doc.rect(0, 0, pageWidth, doc.internal.pageSize.getHeight(), "F");

  if (logo) {
    doc.addImage(logo, "PNG", margin, y, 48, 16);
  }

  doc.setTextColor(31, 26, 20);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(20);
  doc.text("Comprobante de compra", margin, y + 28);
  doc.setFillColor(242, 135, 5);
  doc.roundedRect(margin, y + 33, 62, 9, 4, 4, "F");
  doc.setTextColor(255, 255, 255);
  doc.setFontSize(9);
  doc.text(`Compra #${compra.id} - ${compra.estadoCompra}`, margin + 4, y + 39);

  doc.setTextColor(74, 65, 53);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(9);
  doc.text("Fecha de compra", pageWidth - 60, y + 6);
  doc.setFont("helvetica", "normal");
  doc.text(formatDateTime(compra.fechaCompra), pageWidth - 60, y + 12, { maxWidth: 46 });
  doc.setFont("helvetica", "bold");
  doc.text("Metodo", pageWidth - 60, y + 24);
  doc.setFont("helvetica", "normal");
  doc.text(compra.metodoPago || "Sin metodo", pageWidth - 60, y + 30);

  y += 52;
  doc.setDrawColor(242, 135, 5);
  doc.setLineWidth(1);
  doc.line(margin, y, pageWidth - margin, y);
  y += 10;

  const boxes = [
    ["Cliente", fullName(compra.persona)],
    ["Documento", compra.persona?.documento || "Sin documento"],
    ["Validado por", compra.usuarioValidacion || "Sin validar"],
    ["Fecha y hora validacion", formatDateTime(compra.fechaValidacion)]
  ];

  boxes.forEach(([label, value], index) => {
    const col = index % 2;
    const row = Math.floor(index / 2);
    const x = margin + col * 91;
    const boxY = y + row * 22;
    doc.setFillColor(255, 250, 240);
    doc.setDrawColor(234, 223, 202);
    doc.roundedRect(x, boxY, 84, 17, 3, 3, "FD");
    doc.setTextColor(137, 127, 112);
    doc.setFont("helvetica", "bold");
    doc.setFontSize(7);
    doc.text(label.toUpperCase(), x + 4, boxY + 6);
    doc.setTextColor(31, 26, 20);
    doc.setFontSize(9);
    doc.text(String(value || ""), x + 4, boxY + 12, { maxWidth: 76 });
  });

  y += 76;
  doc.setFillColor(31, 26, 20);
  doc.rect(margin, y, pageWidth - margin * 2, 9, "F");
  doc.setTextColor(255, 255, 255);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(8);
  doc.text("Producto", margin + 3, y + 6);
  doc.text("Cant.", 111, y + 6);
  doc.text("Precio", 130, y + 6);
  doc.text("Volumen", 151, y + 6);
  doc.text("Subtotal", 178, y + 6, { align: "right" });
  y += 9;

  doc.setFont("helvetica", "normal");
  doc.setTextColor(74, 65, 53);
  (compra.detalles || []).forEach((detalle) => {
    if (y > 260) {
      doc.addPage();
      y = 18;
    }

    const productName = detalle.producto?.nombre || "Producto";
    const nextY = addPdfText(doc, productName, margin + 3, y + 6, { maxWidth: 80, lineHeight: 4 });
    doc.text(String(detalle.cantidad || 0), 113, y + 6, { align: "right" });
    doc.text(`Bs. ${money(detalle.precioUnitario)}`, 144, y + 6, { align: "right" });
    doc.text(`PV ${money(detalle.pvUnitario)}`, 151, y + 5);
    doc.text(`QP ${money(detalle.qpUnitario)}`, 151, y + 10);
    doc.text(`CR ${money(detalle.crUnitario)}`, 151, y + 15);
    doc.text(`Bs. ${money(detalle.subtotal)}`, 178, y + 6, { align: "right" });
    doc.setDrawColor(240, 234, 219);
    doc.line(margin, Math.max(nextY, y + 20), pageWidth - margin, Math.max(nextY, y + 20));
    y = Math.max(nextY + 4, y + 23);
  });

  y += 8;
  doc.setFillColor(255, 250, 240);
  const hasDiscount = compraTieneDescuento(compra);
  const totalsHeight = hasDiscount ? 62 : 39;
  doc.roundedRect(pageWidth - 86, y, 72, totalsHeight, 4, 4, "F");
  doc.setTextColor(31, 26, 20);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(9);
  doc.text("Total PV", pageWidth - 80, y + 8);
  doc.text(money(compra.totalPv), pageWidth - 18, y + 8, { align: "right" });
  doc.text("Total QP", pageWidth - 80, y + 16);
  doc.text(money(compra.totalQp), pageWidth - 18, y + 16, { align: "right" });
  doc.text("Total CR", pageWidth - 80, y + 24);
  doc.text(money(compra.totalCr), pageWidth - 18, y + 24, { align: "right" });
  let totalY = y + 30;
  if (hasDiscount) {
    doc.text("Subtotal", pageWidth - 80, y + 32);
    doc.text(`Bs. ${money(compraSubtotalBruto(compra))}`, pageWidth - 18, y + 32, { align: "right" });
    doc.text("Descuento", pageWidth - 80, y + 40);
    doc.text(`- Bs. ${money(compraDescuento(compra))}`, pageWidth - 18, y + 40, { align: "right" });
    doc.setFont("helvetica", "normal");
    doc.setFontSize(7);
    doc.text(`Concepto: ${compra.descuentoConcepto || "Sin concepto"}`, pageWidth - 80, y + 48, { maxWidth: 62 });
    doc.setFont("helvetica", "bold");
    doc.setFontSize(9);
    totalY = y + 53;
  }
  doc.setFillColor(242, 135, 5);
  doc.roundedRect(pageWidth - 86, totalY, 72, 12, 4, 4, "F");
  doc.setTextColor(255, 255, 255);
  doc.text("Total", pageWidth - 80, totalY + 8);
  doc.text(`Bs. ${money(compra.subtotal)}`, pageWidth - 18, totalY + 8, { align: "right" });

  doc.save(`comprobante-compra-${compra.id}.pdf`);
}

function printReceipt(compra) {
  const iframe = document.createElement("iframe");
  iframe.setAttribute("title", "Comprobante de compra");
  iframe.style.position = "fixed";
  iframe.style.right = "0";
  iframe.style.bottom = "0";
  iframe.style.width = "0";
  iframe.style.height = "0";
  iframe.style.border = "0";
  iframe.style.opacity = "0";
  document.body.appendChild(iframe);

  const printWindow = iframe.contentWindow;
  const printDocument = printWindow?.document;
  if (!printWindow || !printDocument) {
    iframe.remove();
    showError("No se pudo imprimir", "El navegador bloqueo la preparacion del comprobante.");
    return;
  }

  printDocument.open();
  printDocument.write(buildReceiptHtml(compra));
  printDocument.close();
  printWindow.onafterprint = () => iframe.remove();
  printWindow.setTimeout(() => {
    printWindow.focus();
    printWindow.print();
    printWindow.setTimeout(() => iframe.remove(), 1200);
  }, 400);
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

function addProduct(producto) {
  const found = ventaItems.value.find((item) => Number(item.id) === Number(producto.id));
  if (found) {
    found.cantidad += 1;
    return;
  }

  ventaItems.value.push({
    id: producto.id,
    nombre: producto.nombre,
    sku: producto.sku,
    precio: Number(producto.precio || 0),
    pv: Number(producto.pv || 0),
    qp: Number(producto.qp || 0),
    cr: Number(producto.cr || 0),
    cantidad: 1
  });
}

function addPublicProduct(producto) {
  touchPublicField("productos");
  const found = publicVentaItems.value.find((item) => Number(item.id) === Number(producto.id));
  if (found) {
    found.cantidad += 1;
    return;
  }

  publicVentaItems.value.push({
    id: producto.id,
    nombre: producto.nombre,
    sku: producto.sku,
    categoria: producto.categoria,
    precioDistribuidor: Number(producto.precioDistribuidor || 0),
    precioPublico: Number(producto.precioPublico || 0),
    descuento: Number(producto.descuento || 0),
    precioFinal: Number(producto.precioFinal || 0),
    cantidad: 1
  });
}

function changeQuantity(item, value) {
  const next = Number(value || 1);
  item.cantidad = Math.max(1, next);
}

function incrementQuantity(item) {
  item.cantidad = Number(item.cantidad || 1) + 1;
}

function decrementQuantity(item) {
  item.cantidad = Math.max(1, Number(item.cantidad || 1) - 1);
}

function removeItem(item) {
  ventaItems.value = ventaItems.value.filter((current) => Number(current.id) !== Number(item.id));
}

function removePublicItem(item) {
  touchPublicField("productos");
  publicVentaItems.value = publicVentaItems.value.filter((current) => Number(current.id) !== Number(item.id));
}

async function loadAll() {
  loading.value = true;
  error.value = "";

  try {
    const [personasData, usuariosData, productosData, tiposClienteData] = await Promise.all([
      apiRequest("/api/personas"),
      apiRequest("/api/usuarios"),
      apiRequest("/api/productos"),
      apiRequest("/api/public/tipos-cliente")
    ]);
    personas.value = personasData;
    usuarios.value = Array.isArray(usuariosData) ? usuariosData : [];
    productos.value = productosData;
    tiposClientePublico.value = Array.isArray(tiposClienteData) ? tiposClienteData : [];
    if (!tiposClientePublico.value.some((tipo) => tipo.codigo === publicForm.tipoClienteCodigo)) {
      publicForm.tipoClienteCodigo = tiposClientePublico.value[0]?.codigo || "NORMAL";
    }
    await loadPeriodoOptions();
    await loadVentasPeriodo();
    await initPeriodoSelect2();
  } catch (exception) {
    error.value = exception.message || "No se pudo cargar la informacion de ventanilla.";
  } finally {
    loading.value = false;
  }
}

async function loadPeriodoOptions() {
  const [activePeriodo, gestionesData] = await Promise.all([
    apiRequest("/api/gestiones/periodos/activo"),
    apiRequest("/api/gestiones")
  ]);

  const periodosPorGestion = await Promise.all(
    (Array.isArray(gestionesData) ? gestionesData : []).map(async (gestion) => {
      const periodos = await apiRequest(`/api/gestiones/${gestion.id}/periodos`);
      return periodos.map((periodo) => ({ ...periodo, gestion: periodo.gestion || gestion }));
    })
  );

  periodosVenta.value = periodosPorGestion
    .flat()
    .sort((left, right) => {
      const leftYear = Number(left.gestion?.anio || 0);
      const rightYear = Number(right.gestion?.anio || 0);
      if (leftYear !== rightYear) return rightYear - leftYear;
      return Number(right.mes || 0) - Number(left.mes || 0);
    });

  if (!selectedPeriodoId.value) {
    selectedPeriodoId.value = String(activePeriodo?.id || periodosVenta.value[0]?.id || "");
  }
}

async function loadVentasPeriodo() {
  const query = selectedPeriodoId.value ? `?periodoId=${selectedPeriodoId.value}` : "";
  const [comprasData, comprasPublicasData] = await Promise.all([
    apiRequest(`/api/compras${query}`),
    apiRequest(`/api/compras-publicas${query}`)
  ]);
  compras.value = Array.isArray(comprasData) ? comprasData : [];
  comprasPublicas.value = Array.isArray(comprasPublicasData) ? comprasPublicasData : [];
}

async function loadPublicProductsForSale() {
  productosPublicos.value = [];
  publicVentaItems.value = [];
  publicProductQuery.value = "";

  if (!publicSaleModalOpen.value || !selectedPublicDistributorId.value) return;

  if (!selectedPublicDistributorUsername.value) {
    await showError("Distribuidor sin tienda", "La persona seleccionada no tiene usuario asociado para su tienda publica.");
    return;
  }

  const query = publicForm.tipoClienteCodigo
    ? `?tipoCliente=${encodeURIComponent(publicForm.tipoClienteCodigo)}`
    : "";
  try {
    const data = await apiRequest(`/api/public/tiendas/${encodeURIComponent(selectedPublicDistributorUsername.value)}/productos${query}`);
    productosPublicos.value = Array.isArray(data) ? data : [];
  } catch (exception) {
    await showError("No se cargaron productos", exception.message || "No se pudieron cargar los productos publicos del distribuidor.");
  }
}

async function searchPublicClientByDocument() {
  publicClientLookupMessage.value = "";
  touchPublicField("distribuidor");
  touchPublicField("clienteDocumento");

  if (!selectedPublicDistributorUsername.value) {
    publicClientLookupMessage.value = "Selecciona primero un distribuidor con tienda publica.";
    return;
  }

  const documento = publicForm.clienteDocumento.trim();
  if (!documento) {
    publicClientLookupMessage.value = "Ingresa el documento para buscar datos guardados.";
    return;
  }

  searchingPublicClient.value = true;
  try {
    const cliente = await apiRequest(
      `/api/public/tiendas/${encodeURIComponent(selectedPublicDistributorUsername.value)}/clientes/documento/${encodeURIComponent(documento)}`
    );
    applyPublicClientLookup(cliente);
    publicClientLookupMessage.value = "Datos encontrados y cargados en el formulario.";
  } catch {
    publicClientLookupMessage.value = "No encontramos datos para ese documento. Puedes completar el formulario manualmente.";
  } finally {
    searchingPublicClient.value = false;
  }
}

async function saveCajaSale() {
  error.value = "";

  if (!selectedPersonaId.value) {
    await showError("Falta cliente", "Selecciona la persona que realiza la compra.");
    return;
  }

  if (!ventaItems.value.length) {
    await showError("Faltan productos", "Agrega al menos un producto a la venta.");
    return;
  }

  if (discountError.value) {
    await showError("Descuento invalido", discountError.value);
    return;
  }

  saving.value = true;
  try {
    const payload = {
      items: ventaItems.value.map((item) => ({
        productoId: Number(item.id),
        cantidad: Number(item.cantidad || 1)
      })),
      metodoPago: "CAJA",
      codigoPago: cajaCode.value,
      referenciaPago: `Venta por ventanilla codigo ${cajaCode.value}`,
      descuentoMonto: discountAmountNumber.value,
      descuentoConcepto: discountConcept.value.trim() || null
    };
    const isEditing = Boolean(editingCompra.value?.id);
    let response;

    if (isEditing) {
      response = await apiRequest(`/api/compras/${editingCompra.value.id}`, {
        method: "PUT",
        body: JSON.stringify(payload)
      });
    } else {
      const formData = new FormData();
      formData.append("compra", new Blob([JSON.stringify(payload)], { type: "application/json" }));

      response = await apiRequest(`/api/compras/persona/${selectedPersonaId.value}/comprobante`, {
        method: "POST",
        body: formData
      });
    }

    await showSuccess(
      isEditing ? "Venta modificada" : "Venta registrada",
      isEditing
        ? `Compra #${response.compra?.id} actualizada correctamente.`
        : `Compra #${response.compra?.id} creada como PENDIENTE.`
    );
    resetSaleForm();
    closeSaleModal();
    await loadVentasPeriodo();
  } catch (exception) {
    await showError("No se pudo guardar", exception.message || "No se pudo guardar la venta.");
  } finally {
    saving.value = false;
  }
}

async function savePublicCajaSale() {
  error.value = "";
  publicSaleSubmitted.value = true;

  if (publicSaleHasErrors.value) {
    await showError("Venta publica incompleta", firstPublicValidationError());
    return;
  }

  saving.value = true;
  try {
    const payload = {
      items: publicVentaItems.value.map((item) => ({
        productoId: Number(item.id),
        cantidad: Number(item.cantidad || 1)
      })),
      tipoClienteCodigo: publicForm.tipoClienteCodigo,
      clienteNombres: publicForm.clienteNombres.trim(),
      clienteApellidos: publicForm.clienteApellidos.trim(),
      clienteDocumento: publicForm.clienteDocumento.trim(),
      clienteEmail: publicForm.clienteEmail.trim(),
      clienteTelefono: publicForm.clienteTelefono.trim(),
      envioRequiere: Boolean(publicForm.envioRequiere),
      envioDireccion: publicForm.envioRequiere ? publicForm.envioDireccion.trim() : "",
      envioCiudad: publicForm.envioRequiere ? publicForm.envioCiudad.trim() : "",
      envioReferencia: publicForm.envioRequiere ? publicForm.envioReferencia.trim() : "",
      metodoPago: "CAJA",
      referenciaPago: `Venta publica por ventanilla codigo ${publicCajaCode.value}`
    };

    const response = await apiRequest(`/api/public/tiendas/${encodeURIComponent(selectedPublicDistributorUsername.value)}/compras`, {
      method: "POST",
      body: JSON.stringify(payload)
    });

    await showSuccess("Venta publica registrada", `Pedido publico #${response.id} creado como PENDIENTE.`);
    resetPublicSaleForm();
    closePublicSaleModal();
    await loadVentasPeriodo();
  } catch (exception) {
    await showError("No se pudo guardar", exception.message || "No se pudo guardar la venta publica.");
  } finally {
    saving.value = false;
  }
}

async function updateCompraEstado(compra, estadoCompra) {
  error.value = "";

  try {
    await apiRequest(`/api/compras/${compra.id}/estado`, {
      method: "PUT",
      body: JSON.stringify({ estadoCompra })
    });
    await showSuccess("Compra actualizada", `Compra #${compra.id} actualizada a ${estadoCompra}.`);
    await loadVentasPeriodo();
  } catch (exception) {
    await showError("No se pudo actualizar", exception.message || "No se pudo actualizar la compra.");
  }
}

async function anularCompra(compra) {
  const result = await Swal.fire({
    title: `Anular compra #${compra.id}`,
    input: "textarea",
    inputLabel: "Motivo de anulacion",
    inputPlaceholder: "Escribe el motivo...",
    inputAttributes: { maxlength: 240 },
    showCancelButton: true,
    confirmButtonText: "Anular compra",
    cancelButtonText: "Cancelar",
    customClass: alertClasses,
    inputValidator: (value) => !String(value || "").trim() ? "El motivo es obligatorio." : undefined
  });

  if (!result.isConfirmed) return;

  try {
    await apiRequest(`/api/compras/${compra.id}/anular`, {
      method: "POST",
      body: JSON.stringify({ motivo: result.value.trim() })
    });
    await showSuccess("Compra anulada", `La compra #${compra.id} fue anulada y sus movimientos fueron revertidos.`);
    await loadVentasPeriodo();
  } catch (exception) {
    await showError("No se pudo anular", exception.message || "No se pudo anular la compra.");
  }
}

async function updateCompraPublicaEstado(compra, estadoCompra) {
  error.value = "";
  if (saving.value) return;

  saving.value = true;
  try {
    await apiRequest(`/api/compras-publicas/${compra.id}/estado`, {
      method: "PUT",
      body: JSON.stringify({ estadoCompra })
    });
    if (publicReviewModalCompra.value?.id === compra.id) {
      closePublicReviewModal();
    }
    await showSuccess("Venta publica actualizada", `Pedido publico #${compra.id} actualizado a ${estadoCompra}.`);
    await loadVentasPeriodo();
  } catch (exception) {
    await showError("No se pudo actualizar", exception.message || "No se pudo actualizar la venta publica.");
  } finally {
    saving.value = false;
  }
}

watch(saleModalOpen, (isOpen) => {
  if (isOpen) {
    initPersonaSelect2();
  }
});

watch(publicSaleModalOpen, (isOpen) => {
  if (isOpen) {
    initPublicDistributorSelect2();
    initPublicTipoClienteSelect2();
    if (selectedPublicDistributorId.value) {
      loadPublicProductsForSale();
    }
  }
});

watch(personas, () => {
  initPersonaSelect2();
  initPublicDistributorSelect2();
});

watch(tiposClientePublico, () => {
  initPublicTipoClienteSelect2();
});

watch(periodosVenta, () => {
  initPeriodoSelect2();
});

watch(visibleCompras, () => {
  if (ventasPage.value > ventasTotalPages.value) {
    ventasPage.value = ventasTotalPages.value;
  }
});

watch(visibleComprasPublicas, () => {
  if (publicVentasPage.value > publicVentasTotalPages.value) {
    publicVentasPage.value = publicVentasTotalPages.value;
  }
});

watch(modalOpen, (isOpen) => {
  if (isOpen) {
    lockPageScroll();
  } else {
    unlockPageScroll();
  }
});

watch(selectedPersonaId, (value) => {
  if (!personaSelect.value) return;
  const element = $(personaSelect.value);
  if (element.hasClass("select2-hidden-accessible") && element.val() !== value) {
    element.val(value || null).trigger("change.select2");
  }
});

watch(selectedPublicDistributorId, (value) => {
  if (!publicDistributorSelect.value) return;
  const element = $(publicDistributorSelect.value);
  if (element.hasClass("select2-hidden-accessible") && element.val() !== value) {
    element.val(value || null).trigger("change.select2");
  }
});

watch(() => publicForm.tipoClienteCodigo, (value) => {
  if (!publicTipoClienteSelect.value) return;
  const element = $(publicTipoClienteSelect.value);
  if (element.hasClass("select2-hidden-accessible") && element.val() !== value) {
    element.val(value || null).trigger("change.select2");
  }
});

watch([selectedPublicDistributorId, () => publicForm.tipoClienteCodigo], () => {
  if (publicSaleModalOpen.value) {
    loadPublicProductsForSale();
  }
});

watch(selectedPeriodoId, (value) => {
  ventasPage.value = 1;
  publicVentasPage.value = 1;
  if (!periodoSelect.value) return;
  const element = $(periodoSelect.value);
  if (element.hasClass("select2-hidden-accessible") && element.val() !== value) {
    element.val(value || null).trigger("change.select2");
  }
});

watch(ventanillaQuery, () => {
  ventasPage.value = 1;
  publicVentasPage.value = 1;
});

onBeforeUnmount(() => {
  document.removeEventListener("click", handleActionMenuDocumentClick);
  unlockPageScroll();
  destroyPersonaSelect2();
  destroyPeriodoSelect2();
  destroyPublicDistributorSelect2();
  destroyPublicTipoClienteSelect2();
});

onMounted(() => {
  document.addEventListener("click", handleActionMenuDocumentClick);
  loadAll();
});
</script>

<template>
  <div class="vy admin-sales-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Ventanilla</div>
          <h1>Ventas y validacion de pedidos</h1>
          <p>
            Registra compras presenciales y valida pagos pendientes en caja.
            <strong v-if="selectedPeriodo">Mostrando {{ selectedPeriodo.nombre }}.</strong>
          </p>
        </div>
        <div class="header-actions">
          <label class="period-filter search-filter">
            <span>Buscar</span>
            <div class="input-icon">
              <Search :size="15" />
              <input v-model.trim="ventanillaQuery" placeholder="Compra, cliente, documento o estado..." />
            </div>
          </label>
          <label class="period-filter">
            <span>Mes</span>
            <select ref="periodoSelect" v-model="selectedPeriodoId">
              <option value="" disabled>Selecciona un mes</option>
              <option v-for="periodo in periodosVenta" :key="periodo.id" :value="periodo.id">
                {{ periodo.nombre }} - Gestion {{ periodo.gestion?.anio || "" }}
              </option>
            </select>
          </label>
          <button class="refresh-action" type="button" :disabled="loading" @click="loadAll">
            <RefreshCw :class="{ spinning: loading }" :size="15" /> {{ loading ? "Actualizando" : "Actualizar" }}
          </button>
        </div>
      </header>

      <div v-if="error" class="error-box">{{ error }}</div>
      <section v-if="loading" class="ventanilla-skeleton" aria-label="Cargando ventanilla">
        <article v-for="section in 2" :key="section" class="vy-card skeleton-card">
          <div class="skeleton-title">
            <span class="skeleton-block skeleton-icon"></span>
            <div>
              <span class="skeleton-block skeleton-heading"></span>
              <span class="skeleton-block skeleton-subtitle"></span>
            </div>
          </div>

          <div class="skeleton-table">
            <div class="skeleton-row skeleton-row-head">
              <span v-for="column in 8" :key="column" class="skeleton-block"></span>
            </div>
            <div v-for="row in 5" :key="row" class="skeleton-row">
              <span v-for="column in 8" :key="column" class="skeleton-block"></span>
            </div>
          </div>
        </article>
      </section>

      <template v-else>
        <section class="vy-card sales-table-card">
          <div class="card-title">
            <span class="icon-box"><ClipboardCheck :size="18" /></span>
            <div>
              <h2>Ventas realizadas</h2>
              <p>Compras registradas por tienda o ventanilla, con control de validacion.</p>
            </div>
          </div>

          <div class="sales-table-wrap">
            <table class="sales-table">
              <thead>
                <tr>
                  <th>Compra</th>
                  <th>Cliente</th>
                  <th>Metodo</th>
                  <th>Estado</th>
                  <th>Volumen</th>
                  <th>Total</th>
                  <th>Fecha</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="compra in paginatedCompras" :key="compra.id">
                  <td>
                    <strong>#{{ compra.id }}</strong>
                    <small v-if="compra.codigoPago">Caja {{ compra.codigoPago }}</small>
                  </td>
                  <td>
                    <strong>{{ fullName(compra.persona) }}</strong>
                    <small>{{ compra.persona?.documento || "Sin documento" }}</small>
                  </td>
                  <td>{{ compra.metodoPago || "Sin metodo" }}</td>
                  <td><span class="status-pill">{{ compra.estadoCompra }}</span></td>
                  <td>
                    <small>PV {{ money(compra.totalPv) }}</small>
                    <small>QP {{ money(compra.totalQp) }}</small>
                    <small>CR {{ money(compra.totalCr) }}</small>
                  </td>
                  <td>
                    <strong>Bs. {{ money(compra.subtotal) }}</strong>
                    <small v-if="compraTieneDescuento(compra)">Desc. Bs. {{ money(compraDescuento(compra)) }}</small>
                  </td>
                  <td>{{ formatDateTime(compra.fechaCompra) }}</td>
                  <td>
                    <div class="action-menu" @click.stop>
                      <button class="action-menu-toggle" type="button" title="Acciones" @click="toggleActionMenu(`venta-${compra.id}`)">
                        <MoreVertical :size="17" />
                      </button>
                      <div v-if="activeActionMenu === `venta-${compra.id}`" class="action-menu-panel">
                        <button type="button" @click="closeActionMenu(); openDetallesModal(compra)">
                          <ClipboardList :size="15" /> Detalles
                        </button>
                        <button v-if="compra.comprobantePagoUrl" type="button" @click="closeActionMenu(); openProofModal(compra)">
                          <FileText :size="15" /> Ver pago
                        </button>
                        <button v-if="compra.estadoCompra === 'PENDIENTE'" type="button" @click="closeActionMenu(); openEditSaleModal(compra)">
                          <Pencil :size="15" /> Modificar
                        </button>
                        <button v-if="compra.estadoCompra === 'PENDIENTE'" type="button" @click="closeActionMenu(); updateCompraEstado(compra, 'VALIDADA')">
                          <CheckCircle2 :size="15" /> Validar
                        </button>
                        <button v-if="compra.estadoCompra === 'PENDIENTE'" type="button" @click="closeActionMenu(); updateCompraEstado(compra, 'RECHAZADA')">
                          <CircleX :size="15" /> Rechazar
                        </button>
                        <button
                          v-if="compra.estadoCompra === 'VALIDADA'"
                          type="button"
                          @click="closeActionMenu(); openReceiptModal(compra)"
                        >
                          <ClipboardCheck :size="15" /> Comprobante
                        </button>
                        <button v-if="compra.estadoCompra === 'VALIDADA'" type="button" @click="closeActionMenu(); anularCompra(compra)">
                          <Ban :size="15" /> Anular compra
                        </button>
                      </div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>

            <div v-if="visibleCompras.length > pageSize" class="pagination-bar">
              <span>
                {{ paginationStart(ventasPage, visibleCompras.length) }}-{{ paginationEnd(ventasPage, visibleCompras.length) }}
                de {{ visibleCompras.length }}
              </span>
              <div>
                <button type="button" :disabled="ventasPage <= 1" @click="setVentasPage(ventasPage - 1)">Anterior</button>
                <strong>{{ ventasPage }} / {{ ventasTotalPages }}</strong>
                <button type="button" :disabled="ventasPage >= ventasTotalPages" @click="setVentasPage(ventasPage + 1)">Siguiente</button>
              </div>
            </div>

            <div v-if="!visibleCompras.length" class="empty-state">
              <PackageCheck :size="28" />
              <strong>No hay ventas registradas</strong>
              <span>Cuando llegue una compra por tienda o ventanilla aparecera aqui.</span>
            </div>
          </div>
        </section>

        <section class="vy-card sales-table-card public-sales-card">
          <div class="card-title">
            <span class="icon-box"><Store :size="18" /></span>
            <div>
              <h2>Ventas publicas por distribuidor</h2>
              <p>Pedidos hechos desde links publicos; al validar se acredita la ganancia al distribuidor.</p>
            </div>
          </div>

          <div class="sales-table-wrap">
            <table class="sales-table">
              <thead>
                <tr>
                  <th>Pedido</th>
                  <th>Cliente</th>
                  <th>Distribuidor</th>
                  <th>Tipo</th>
                  <th>Total</th>
                  <th>Empresa</th>
                  <th>Distribuidor</th>
                  <th>Estado</th>
                  <th>Acciones</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="compra in paginatedComprasPublicas" :key="compra.id">
                  <td><strong>#{{ compra.id }}</strong></td>
                  <td>
                    <strong>{{ compra.clienteNombres }} {{ compra.clienteApellidos }}</strong>
                    <small>{{ compra.clienteTelefono || compra.clienteEmail || "Sin contacto" }}</small>
                  </td>
                  <td>
                    <strong>{{ fullName(compra.distribuidor) }}</strong>
                    <small>{{ compra.distribuidor?.documento || "Sin documento" }}</small>
                  </td>
                  <td>{{ compra.tipoCliente?.nombre || "Cliente" }}</td>
                  <td><strong>Bs. {{ money(compra.totalCliente) }}</strong></td>
                  <td>Bs. {{ money(compra.totalEmpresa) }}</td>
                  <td>
                    <strong>Bs. {{ money(compra.totalGananciaDistribuidor) }}</strong>
                    <small v-if="Number(compra.totalDescuento || 0) > 0">Desc. Bs. {{ money(compra.totalDescuento) }}</small>
                  </td>
                  <td><span class="status-pill">{{ compra.estadoCompra }}</span></td>
                  <td>
                    <div class="action-menu" @click.stop>
                      <button class="action-menu-toggle" type="button" title="Acciones" @click="toggleActionMenu(`publica-${compra.id}`)">
                        <MoreVertical :size="17" />
                      </button>
                      <div v-if="activeActionMenu === `publica-${compra.id}`" class="action-menu-panel">
                        <button type="button" @click="closeActionMenu(); openDetallesModal(compra)">
                          <ClipboardList :size="15" /> Detalles
                        </button>
                        <button v-if="compra.comprobantePagoUrl" type="button" @click="closeActionMenu(); openProofModal(compra)">
                          <FileText :size="15" /> Ver pago
                        </button>
                        <button v-if="compra.estadoCompra === 'PENDIENTE'" type="button" @click="closeActionMenu(); openPublicReviewModal(compra)">
                          <CheckCircle2 :size="15" /> Revisar
                        </button>
                        <button v-if="compra.estadoCompra === 'PENDIENTE'" type="button" @click="closeActionMenu(); updateCompraPublicaEstado(compra, 'RECHAZADA')">
                          <CircleX :size="15" /> Rechazar
                        </button>
                      </div>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>

            <div v-if="visibleComprasPublicas.length > pageSize" class="pagination-bar">
              <span>
                {{ paginationStart(publicVentasPage, visibleComprasPublicas.length) }}-{{ paginationEnd(publicVentasPage, visibleComprasPublicas.length) }}
                de {{ visibleComprasPublicas.length }}
              </span>
              <div>
                <button type="button" :disabled="publicVentasPage <= 1" @click="setPublicVentasPage(publicVentasPage - 1)">Anterior</button>
                <strong>{{ publicVentasPage }} / {{ publicVentasTotalPages }}</strong>
                <button type="button" :disabled="publicVentasPage >= publicVentasTotalPages" @click="setPublicVentasPage(publicVentasPage + 1)">Siguiente</button>
              </div>
            </div>

            <div v-if="!visibleComprasPublicas.length" class="empty-state">
              <PackageCheck :size="28" />
              <strong>No hay ventas publicas pendientes</strong>
              <span>Cuando llegue una venta desde una tienda publica aparecera aqui.</span>
            </div>
          </div>
        </section>
      </template>

      <section v-if="false" class="shell-grid">
        <article class="vy-card sale-card">
          <div class="card-title">
            <span class="icon-box"><Store :size="18" /></span>
            <div>
              <h2>Venta presencial</h2>
              <p>Genera una compra pendiente con pago en caja.</p>
            </div>
          </div>

          <label class="field">
            <span>Buscar persona</span>
            <div class="input-icon">
              <Search :size="15" />
              <input v-model.trim="personaQuery" placeholder="Nombre, documento o correo" />
            </div>
          </label>

          <div class="person-list">
            <button
              v-for="persona in filteredPersonas.slice(0, 6)"
              :key="persona.id"
              type="button"
              :class="{ active: Number(selectedPersonaId) === Number(persona.id) }"
              @click="selectedPersonaId = String(persona.id)"
            >
              <strong>{{ fullName(persona) }}</strong>
              <small>{{ persona.documento }} · {{ persona.email || "Sin correo" }}</small>
            </button>
          </div>

          <div v-if="selectedPersona" class="selected-person">
            Cliente seleccionado: <strong>{{ fullName(selectedPersona) }}</strong>
          </div>

          <label class="field">
            <span>Buscar producto</span>
            <div class="input-icon">
              <Search :size="15" />
              <input v-model.trim="productQuery" placeholder="Producto, SKU o categoria" />
            </div>
          </label>

          <div class="product-picker">
            <button
              v-for="producto in filteredProducts.slice(0, 8)"
              :key="producto.id"
              type="button"
              @click="addProduct(producto)"
            >
              <span>
                <strong>{{ producto.nombre }}</strong>
                <small>{{ producto.sku }} · {{ producto.categoria || "Producto" }}</small>
              </span>
              <b>Bs. {{ money(producto.precio) }}</b>
              <Plus :size="16" />
            </button>
          </div>

          <div class="sale-items">
            <div v-for="item in ventaItems" :key="item.id" class="sale-item">
              <div>
                <strong>{{ item.nombre }}</strong>
                <small>Bs. {{ money(item.precio) }} · PV {{ money(item.pv) }} · QP {{ money(item.qp) }} · CR {{ money(item.cr) }}</small>
              </div>
              <input :value="item.cantidad" type="number" min="1" @input="changeQuantity(item, $event.target.value)" />
              <b>Bs. {{ money(item.precio * item.cantidad) }}</b>
              <button type="button" @click="removeItem(item)"><Trash2 :size="15" /></button>
            </div>
          </div>

          <footer class="sale-footer">
            <div>
              <small>Codigo de caja</small>
              <strong class="cash-code">{{ cajaCode }}</strong>
            </div>
            <div>
              <small>Total</small>
              <strong>Bs. {{ money(saleTotal) }}</strong>
              <span>PV {{ money(salePv) }} · QP {{ money(saleQp) }} · CR {{ money(saleCr) }}</span>
            </div>
            <button class="vy-btn vy-btn-primary" type="button" :disabled="saving" @click="saveCajaSale">
              <ShoppingBag :size="16" /> Registrar venta
            </button>
          </footer>
        </article>

        <article class="vy-card pending-card">
          <div class="card-title">
            <span class="icon-box"><ClipboardCheck :size="18" /></span>
            <div>
              <h2>Validacion y entrega</h2>
              <p>Compras pendientes por comprobante o por pago en caja.</p>
            </div>
          </div>

          <div class="orders-list">
            <section v-for="compra in visibleCompras" :key="compra.id" class="order-card">
              <header>
                <div>
                  <strong>Compra #{{ compra.id }}</strong>
                  <small>{{ compra.metodoPago || "Sin metodo" }} · {{ compra.estadoCompra }}</small>
                </div>
                <b>Bs. {{ money(compra.subtotal) }}</b>
              </header>

              <p>
                {{ fullName(compra.persona) }}
                <span v-if="compra.persona?.documento">· {{ compra.persona.documento }}</span>
              </p>

              <div class="order-meta">
                <span>PV {{ money(compra.totalPv) }}</span>
                <span>QP {{ money(compra.totalQp) }}</span>
                <span>CR {{ money(compra.totalCr) }}</span>
                <span v-if="compra.codigoPago">Caja {{ compra.codigoPago }}</span>
                <span v-if="compraTieneDescuento(compra)">Desc. Bs. {{ money(compraDescuento(compra)) }}</span>
              </div>

              <div class="details-list">
                <div v-for="detalle in compra.detalles || []" :key="detalle.id">
                  <span>{{ detalle.producto?.nombre || "Producto" }} x{{ detalle.cantidad }}</span>
                  <b>Bs. {{ money(detalle.subtotal) }}</b>
                </div>
              </div>

              <footer>
                <div class="action-menu action-menu-card" @click.stop>
                  <button class="action-menu-trigger" type="button" @click="toggleActionMenu(`card-${compra.id}`)">
                    <MoreVertical :size="16" /> Acciones
                  </button>
                  <div v-if="activeActionMenu === `card-${compra.id}`" class="action-menu-panel">
                    <button v-if="compra.comprobantePagoUrl" type="button" @click="closeActionMenu(); openProofModal(compra)">
                      <FileText :size="15" /> Ver pago
                    </button>
                    <button v-if="compra.estadoCompra === 'PENDIENTE'" type="button" @click="closeActionMenu(); openEditSaleModal(compra)">
                      <Pencil :size="15" /> Modificar
                    </button>
                    <button v-if="compra.estadoCompra === 'PENDIENTE'" type="button" @click="closeActionMenu(); updateCompraEstado(compra, 'VALIDADA')">
                      <CheckCircle2 :size="15" /> Validar
                    </button>
                    <button v-if="compra.estadoCompra === 'PENDIENTE'" type="button" @click="closeActionMenu(); updateCompraEstado(compra, 'RECHAZADA')">
                      <CircleX :size="15" /> Rechazar
                    </button>
                    <button
                      v-if="compra.estadoCompra === 'VALIDADA'"
                      type="button"
                      @click="closeActionMenu(); openReceiptModal(compra)"
                    >
                      <FileText :size="15" /> Comprobante
                    </button>
                    <button v-if="compra.estadoCompra === 'VALIDADA'" type="button" @click="closeActionMenu(); anularCompra(compra)">
                      <Ban :size="15" /> Anular compra
                    </button>
                  </div>
                </div>
              </footer>
            </section>

            <div v-if="!visibleCompras.length" class="empty-state">
              <PackageCheck :size="28" />
              <strong>No hay compras pendientes</strong>
              <span>Cuando llegue una compra por tienda o ventanilla aparecera aqui.</span>
            </div>
          </div>
        </article>
      </section>
    </main>

    <div class="floating-sale-actions">
      <button class="floating-sale-button public" type="button" @click="openPublicSaleModal">
        <Store :size="20" />
        <span>Venta publica</span>
      </button>
      <button class="floating-sale-button" type="button" @click="openSaleModal">
        <Plus :size="20" />
        <span>Venta interna</span>
      </button>
    </div>

    <Teleport to="body">
      <div v-if="saleModalOpen" class="sale-modal-backdrop" @click.self="closeSaleModal">
        <article class="sale-modal">
          <header>
            <div>
              <span class="vy-eyebrow">Ventanilla</span>
              <h2>{{ editingCompra ? `Modificar venta #${editingCompra.id}` : "Nueva venta" }}</h2>
              <p>{{ editingCompra ? "Ajusta productos, cantidades o descuento antes de validar." : "Genera una compra pendiente con pago en caja." }}</p>
            </div>
            <button type="button" aria-label="Cerrar" @click="closeSaleModal">
              <X :size="18" />
            </button>
          </header>

          <section class="sale-modal-body">
            <div class="sale-card">
              <label class="field">
                <span>Persona</span>
                <select ref="personaSelect" class="persona-select" :disabled="Boolean(editingCompra)">
                  <option value=""></option>
                  <option v-for="persona in personas" :key="persona.id" :value="persona.id">
                    {{ fullName(persona) }} - {{ persona.documento || "Sin documento" }} - {{ persona.email || "Sin correo" }}
                  </option>
                </select>
              </label>

              <div v-if="selectedPersona" class="selected-person">
                Cliente seleccionado: <strong>{{ fullName(selectedPersona) }}</strong>
              </div>

              <label class="field">
                <span>Buscar producto</span>
                <div class="input-icon">
                  <Search :size="15" />
                  <input v-model.trim="productQuery" placeholder="Producto, SKU o categoria" />
                </div>
              </label>

              <div class="product-picker">
                <button
                  v-for="producto in filteredProducts.slice(0, 8)"
                  :key="producto.id"
                  type="button"
                  @click="addProduct(producto)"
                >
                  <span>
                    <strong>{{ producto.nombre }}</strong>
                    <small>{{ producto.sku }} - {{ producto.categoria || "Producto" }}</small>
                    <em v-if="isClubRoyaleProduct(producto)" class="club-royale-badge">Producto Club Royale</em>
                  </span>
                  <b>Bs. {{ money(producto.precio) }}</b>
                </button>
              </div>

              <div class="sale-items">
                <div v-for="item in ventaItems" :key="item.id" class="sale-item">
                  <div>
                    <strong>{{ item.nombre }}</strong>
                    <small>Bs. {{ money(item.precio) }} - PV {{ money(item.pv) }} - QP {{ money(item.qp) }} - CR {{ money(item.cr) }}</small>
                    <em v-if="isClubRoyaleProduct(item)" class="club-royale-badge">Producto Club Royale</em>
                  </div>
                  <div class="quantity-stepper">
                    <button type="button" aria-label="Disminuir cantidad" @click="decrementQuantity(item)">-</button>
                    <input :value="item.cantidad" type="number" min="1" @input="changeQuantity(item, $event.target.value)" />
                    <button type="button" aria-label="Aumentar cantidad" @click="incrementQuantity(item)">+</button>
                  </div>
                  <b>Bs. {{ money(item.precio * item.cantidad) }}</b>
                  <button type="button" @click="removeItem(item)"><Trash2 :size="15" /></button>
                </div>
              </div>

              <section class="discount-box">
                <div class="discount-grid">
                  <label class="field">
                    <span>Monto descuento</span>
                    <input v-model.number="discountAmount" type="number" min="0" :max="saleSubtotal" step="0.01" placeholder="0.00" />
                  </label>
                  <label class="field">
                    <span>Concepto descuento</span>
                    <input v-model.trim="discountConcept" :disabled="discountAmountNumber <= 0" placeholder="Motivo del descuento" />
                  </label>
                </div>
                <p v-if="discountError" class="discount-error">{{ discountError }}</p>
              </section>

              <footer class="sale-footer">
                <div>
                  <small>Subtotal</small>
                  <strong>Bs. {{ money(saleSubtotal) }}</strong>
                  <span v-if="discountAmountNumber > 0">Descuento Bs. {{ money(discountAmountNumber) }}</span>
                </div>
                <div>
                  <small>Total a pagar</small>
                  <strong>Bs. {{ money(saleTotal) }}</strong>
                  <span>PV {{ money(salePv) }} - QP {{ money(saleQp) }} - CR {{ money(saleCr) }}</span>
                </div>
                <button class="vy-btn vy-btn-primary" type="button" :disabled="saving || !!discountError" @click="saveCajaSale">
                  <ShoppingBag :size="16" /> {{ editingCompra ? "Guardar cambios" : "Registrar venta" }}
                </button>
              </footer>
            </div>
          </section>
        </article>
      </div>

      <div v-if="publicSaleModalOpen" class="sale-modal-backdrop" @click.self="closePublicSaleModal">
        <article class="sale-modal public-sale-modal">
          <header>
            <div>
              <span class="vy-eyebrow">Venta publica</span>
              <h2>Nueva venta publica</h2>
              <p>Registra un pedido publico para la tienda del distribuidor seleccionado.</p>
            </div>
            <button type="button" aria-label="Cerrar" @click="closePublicSaleModal">
              <X :size="18" />
            </button>
          </header>

          <section class="sale-modal-body">
            <div class="sale-card public-sale-grid">
              <section class="public-sale-panel">
                <label class="field" :class="{ invalid: publicFieldError('distribuidor') }">
                  <span>Distribuidor</span>
                  <select ref="publicDistributorSelect" v-model="selectedPublicDistributorId" @change="touchPublicField('distribuidor')">
                    <option value="">Selecciona un distribuidor</option>
                    <option v-for="persona in personas" :key="persona.id" :value="persona.id">
                      {{ fullName(persona) }} - {{ usuarioDePersona(persona)?.username || "Sin usuario" }}
                    </option>
                  </select>
                  <small v-if="publicFieldError('distribuidor')" class="field-error">{{ publicFieldError("distribuidor") }}</small>
                </label>

                <div v-if="selectedPublicDistributor" class="selected-person">
                  Tienda: <strong>{{ selectedPublicDistributorUsername || "Sin usuario" }}</strong>
                  <span>{{ fullName(selectedPublicDistributor) }}</span>
                </div>

                <label class="field" :class="{ invalid: publicFieldError('tipoClienteCodigo') }">
                  <span>Tipo de cliente</span>
                  <select ref="publicTipoClienteSelect" v-model="publicForm.tipoClienteCodigo" @change="touchPublicField('tipoClienteCodigo')">
                    <option v-for="tipo in tiposClientePublico" :key="tipo.id" :value="tipo.codigo">
                      {{ tipo.nombre }}
                    </option>
                  </select>
                  <small v-if="publicFieldError('tipoClienteCodigo')" class="field-error">{{ publicFieldError("tipoClienteCodigo") }}</small>
                </label>

                <div class="public-client-grid">
                  <label class="field" :class="{ invalid: publicFieldError('clienteNombres') }">
                    <span>Nombres</span>
                    <input v-model.trim="publicForm.clienteNombres" placeholder="Nombre del cliente" @blur="touchPublicField('clienteNombres')" @input="touchPublicField('clienteNombres')" />
                    <small v-if="publicFieldError('clienteNombres')" class="field-error">{{ publicFieldError("clienteNombres") }}</small>
                  </label>
                  <label class="field">
                    <span>Apellidos</span>
                    <input v-model.trim="publicForm.clienteApellidos" placeholder="Apellidos" />
                  </label>
                  <label class="field" :class="{ invalid: publicFieldError('clienteDocumento') }">
                    <span>Documento</span>
                    <div class="public-document-search">
                      <input
                        v-model.trim="publicForm.clienteDocumento"
                        placeholder="CI / NIT"
                        @blur="touchPublicField('clienteDocumento')"
                        @input="touchPublicField('clienteDocumento'); publicClientLookupMessage = ''"
                        @keyup.enter.prevent="searchPublicClientByDocument"
                      />
                      <button type="button" :disabled="searchingPublicClient" @click="searchPublicClientByDocument">
                        {{ searchingPublicClient ? "Buscando..." : "Buscar" }}
                      </button>
                    </div>
                    <small v-if="publicFieldError('clienteDocumento')" class="field-error">{{ publicFieldError("clienteDocumento") }}</small>
                    <small v-else-if="publicClientLookupMessage" class="lookup-message">{{ publicClientLookupMessage }}</small>
                  </label>
                  <label class="field">
                    <span>Telefono</span>
                    <input v-model.trim="publicForm.clienteTelefono" placeholder="Telefono" />
                  </label>
                  <label class="field public-client-wide" :class="{ invalid: publicFieldError('clienteEmail') }">
                    <span>Email</span>
                    <input v-model.trim="publicForm.clienteEmail" type="email" placeholder="correo@dominio.com" @blur="touchPublicField('clienteEmail')" @input="touchPublicField('clienteEmail')" />
                    <small v-if="publicFieldError('clienteEmail')" class="field-error">{{ publicFieldError("clienteEmail") }}</small>
                  </label>
                </div>

                <label class="toggle-field">
                  <input v-model="publicForm.envioRequiere" type="checkbox" @change="touchPublicField('envioCiudad'); touchPublicField('envioDireccion')" />
                  <span>Requiere envio</span>
                </label>

                <div v-if="publicForm.envioRequiere" class="public-client-grid">
                  <label class="field" :class="{ invalid: publicFieldError('envioCiudad') }">
                    <span>Ciudad</span>
                    <input v-model.trim="publicForm.envioCiudad" placeholder="Ciudad" @blur="touchPublicField('envioCiudad')" @input="touchPublicField('envioCiudad')" />
                    <small v-if="publicFieldError('envioCiudad')" class="field-error">{{ publicFieldError("envioCiudad") }}</small>
                  </label>
                  <label class="field public-client-wide" :class="{ invalid: publicFieldError('envioDireccion') }">
                    <span>Direccion</span>
                    <input v-model.trim="publicForm.envioDireccion" placeholder="Direccion de entrega" @blur="touchPublicField('envioDireccion')" @input="touchPublicField('envioDireccion')" />
                    <small v-if="publicFieldError('envioDireccion')" class="field-error">{{ publicFieldError("envioDireccion") }}</small>
                  </label>
                  <label class="field public-client-wide">
                    <span>Referencia</span>
                    <input v-model.trim="publicForm.envioReferencia" placeholder="Referencia opcional" />
                  </label>
                </div>
              </section>

              <section class="public-sale-panel">
                <label class="field">
                  <span>Buscar producto publico</span>
                  <div class="input-icon">
                    <Search :size="15" />
                    <input v-model.trim="publicProductQuery" placeholder="Producto, SKU o categoria" />
                  </div>
                </label>

                <div class="product-picker public-product-picker">
                  <button
                    v-for="producto in filteredPublicProducts.slice(0, 8)"
                    :key="producto.id"
                    type="button"
                    @click="addPublicProduct(producto)"
                  >
                    <span>
                      <strong>{{ producto.nombre }}</strong>
                      <small>{{ producto.sku }} - {{ producto.categoria || "Producto" }}</small>
                      <em v-if="Number(producto.descuento || 0) > 0" class="discount-badge">Desc. Bs. {{ money(producto.descuento) }}</em>
                    </span>
                    <b>Bs. {{ money(producto.precioFinal) }}</b>
                  </button>
                  <div v-if="selectedPublicDistributorId && !productosPublicos.length" class="inline-empty">
                    No hay productos publicos para este distribuidor y tipo de cliente.
                  </div>
                </div>
                <p v-if="publicFieldError('productos')" class="field-error public-products-error">{{ publicFieldError("productos") }}</p>

                <div class="sale-items">
                  <div v-for="item in publicVentaItems" :key="item.id" class="sale-item public-sale-item">
                    <div>
                      <strong>{{ item.nombre }}</strong>
                      <small>Publico Bs. {{ money(item.precioPublico) }} - Final Bs. {{ money(item.precioFinal) }}</small>
                    </div>
                    <div class="quantity-stepper">
                      <button type="button" aria-label="Disminuir cantidad" @click="decrementQuantity(item)">-</button>
                      <input :value="item.cantidad" type="number" min="1" @input="changeQuantity(item, $event.target.value)" />
                      <button type="button" aria-label="Aumentar cantidad" @click="incrementQuantity(item)">+</button>
                    </div>
                    <b>Bs. {{ money(item.precioFinal * item.cantidad) }}</b>
                    <button type="button" @click="removePublicItem(item)"><Trash2 :size="15" /></button>
                  </div>
                </div>

                <footer class="sale-footer public-sale-footer">
                  <div>
                    <small>Total cliente</small>
                    <strong>Bs. {{ money(publicSaleSubtotal) }}</strong>
                    <span>Empresa Bs. {{ money(publicSaleEmpresa) }} - Desc. Bs. {{ money(publicSaleDescuento) }} - Gan. Bs. {{ money(publicSaleGanancia) }}</span>
                  </div>
                  <button class="vy-btn vy-btn-primary" type="button" :disabled="saving" @click="savePublicCajaSale">
                    <Store :size="16" /> Registrar venta publica
                  </button>
                </footer>
              </section>
            </div>
          </section>
        </article>
      </div>

      <div v-if="publicReviewModalCompra" class="public-review-backdrop" @click.self="closePublicReviewModal">
        <article class="public-review-modal">
          <header>
            <div>
              <span class="vy-eyebrow">Validacion de venta publica</span>
              <h2>Pedido #{{ publicReviewModalCompra.id }}</h2>
              <p>{{ publicReviewModalCompra.estadoCompra }} - {{ publicReviewModalCompra.metodoPago || "Sin metodo" }}</p>
            </div>
            <button type="button" aria-label="Cerrar" @click="closePublicReviewModal">
              <X :size="18" />
            </button>
          </header>

          <section class="public-review-body">
            <div class="review-grid">
              <div>
                <small>Cliente</small>
                <strong>{{ publicReviewModalCompra.clienteNombres }} {{ publicReviewModalCompra.clienteApellidos }}</strong>
                <span>{{ publicReviewModalCompra.clienteDocumento || "Sin documento" }}</span>
              </div>
              <div>
                <small>Contacto</small>
                <strong>{{ publicReviewModalCompra.clienteTelefono || "Sin telefono" }}</strong>
                <span>{{ publicReviewModalCompra.clienteEmail || "Sin correo" }}</span>
              </div>
              <div>
                <small>Distribuidor</small>
                <strong>{{ fullName(publicReviewModalCompra.distribuidor) }}</strong>
                <span>{{ publicReviewModalCompra.distribuidor?.documento || "Sin documento" }}</span>
              </div>
              <div>
                <small>Tipo de cliente</small>
                <strong>{{ publicReviewModalCompra.tipoCliente?.nombre || "Cliente" }}</strong>
                <span v-if="Number(publicReviewModalCompra.totalDescuento || 0) > 0">Descuento aplicado Bs. {{ money(publicReviewModalCompra.totalDescuento) }}</span>
                <span v-else>Sin descuento aplicado</span>
              </div>
              <div>
                <small>Envio</small>
                <strong>{{ publicReviewModalCompra.envioRequiere ? publicReviewModalCompra.envioCiudad || "Requiere envio" : "No requiere envio" }}</strong>
                <span>{{ publicReviewModalCompra.envioRequiere ? publicReviewModalCompra.envioDireccion || "Sin direccion" : "Retiro o coordinacion directa" }}</span>
              </div>
              <div>
                <small>Pago</small>
                <strong>{{ publicReviewModalCompra.metodoPago || "Sin metodo" }}</strong>
                <span>{{ publicReviewModalCompra.referenciaPago || publicReviewModalCompra.comprobantePagoNombre || "Sin referencia" }}</span>
              </div>
            </div>

            <button v-if="publicReviewModalCompra.comprobantePagoUrl" class="proof-link" type="button" @click="openProofModal(publicReviewModalCompra)">
              <FileText :size="15" /> Ver comprobante de pago
            </button>

            <div class="public-detail-table">
              <table>
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>Cant.</th>
                    <th>Precio publico</th>
                    <th>Descuento</th>
                    <th>Cliente paga</th>
                    <th>Empresa</th>
                    <th>Distribuidor</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="detalle in publicReviewModalCompra.detalles || []" :key="detalle.id">
                    <td>
                      <strong>{{ detalle.producto?.nombre || "Producto" }}</strong>
                      <small>{{ detalle.producto?.sku || "" }}</small>
                    </td>
                    <td>{{ detalle.cantidad }}</td>
                    <td>Bs. {{ money(detalle.precioPublicoUnitario) }}</td>
                    <td>Bs. {{ money(detalle.subtotalDescuento) }}</td>
                    <td><strong>Bs. {{ money(detalle.subtotalCliente) }}</strong></td>
                    <td>Bs. {{ money(detalle.subtotalEmpresa) }}</td>
                    <td>Bs. {{ money(detalle.gananciaDistribuidor) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <section class="review-totals">
              <div><span>Total cliente</span><strong>Bs. {{ money(publicReviewModalCompra.totalCliente) }}</strong></div>
              <div><span>Empresa</span><strong>Bs. {{ money(publicReviewModalCompra.totalEmpresa) }}</strong></div>
              <div><span>Descuentos</span><strong>Bs. {{ money(publicReviewModalCompra.totalDescuento) }}</strong></div>
              <div><span>Distribuidor</span><strong>Bs. {{ money(publicReviewModalCompra.totalGananciaDistribuidor) }}</strong></div>
            </section>
          </section>

          <footer>
            <button class="vy-btn vy-btn-ghost" type="button" :disabled="saving" @click="closePublicReviewModal">Cancelar</button>
            <button class="vy-btn vy-btn-primary" type="button" :disabled="saving" @click="updateCompraPublicaEstado(publicReviewModalCompra, 'VALIDADA')">
              <CheckCircle2 :size="16" /> {{ saving ? "Validando..." : "Validar pedido" }}
            </button>
          </footer>
        </article>
      </div>

      <div v-if="proofModalCompra" class="proof-modal-backdrop" @click.self="closeProofModal">
        <article class="proof-modal">
          <header>
            <div>
              <span class="vy-eyebrow">Comprobante</span>
              <h2>Compra #{{ proofModalCompra.id }}</h2>
              <p>{{ proofModalCompra.comprobantePagoNombre || proofModalCompra.metodoPago }}</p>
            </div>
            <button type="button" aria-label="Cerrar" @click="closeProofModal">
              <X :size="18" />
            </button>
          </header>

          <div class="proof-viewer">
            <img v-if="isImageProof(proofModalCompra)" :src="proofModalCompra.comprobantePagoUrl" alt="Comprobante de pago" />
            <iframe v-else :src="proofModalCompra.comprobantePagoUrl" title="Comprobante de pago"></iframe>
          </div>

          <footer>
            <a class="vy-btn vy-btn-ghost" :href="proofModalCompra.comprobantePagoUrl" target="_blank" rel="noreferrer">Abrir aparte</a>
            <button class="vy-btn vy-btn-primary" type="button" @click="closeProofModal">Cerrar</button>
          </footer>
        </article>
      </div>

      <div v-if="receiptModalCompra" class="receipt-modal-backdrop" @click.self="closeReceiptModal">
        <article class="receipt-modal">
          <header>
            <div>
              <span class="vy-eyebrow">Comprobante del sistema</span>
              <h2>Compra #{{ receiptModalCompra.id }}</h2>
              <p>{{ receiptModalCompra.estadoCompra }} · {{ receiptModalCompra.metodoPago || "Sin metodo" }}</p>
            </div>
            <button type="button" aria-label="Cerrar" @click="closeReceiptModal">
              <X :size="18" />
            </button>
          </header>

          <section class="receipt-preview">
            <div class="receipt-sheet">
              <div class="receipt-sheet-header">
                <div>
                  <img :src="logoFull" alt="Vida Young" />
                  <h3>Comprobante de compra</h3>
                  <span>Compra #{{ receiptModalCompra.id }} · {{ receiptModalCompra.estadoCompra }}</span>
                </div>
                <div>
                  <strong>Fecha de compra</strong>
                  <p>{{ formatDateTime(receiptModalCompra.fechaCompra) }}</p>
                  <strong>Metodo</strong>
                  <p>{{ receiptModalCompra.metodoPago || "Sin metodo" }}</p>
                </div>
              </div>

              <div class="receipt-info-grid">
                <div><small>Cliente</small><strong>{{ fullName(receiptModalCompra.persona) }}</strong></div>
                <div><small>Documento</small><strong>{{ receiptModalCompra.persona?.documento || "Sin documento" }}</strong></div>
                <div><small>Validado por</small><strong>{{ receiptModalCompra.usuarioValidacion || "Sin validar" }}</strong></div>
                <div><small>Fecha y hora validacion</small><strong>{{ formatDateTime(receiptModalCompra.fechaValidacion) }}</strong></div>
                <div v-if="compraTieneDescuento(receiptModalCompra)"><small>Descuento</small><strong>Bs. {{ money(compraDescuento(receiptModalCompra)) }}</strong></div>
                <div v-if="compraTieneDescuento(receiptModalCompra)"><small>Concepto descuento</small><strong>{{ receiptModalCompra.descuentoConcepto || "Sin concepto" }}</strong></div>
              </div>

              <div class="receipt-table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Producto</th>
                      <th>SKU</th>
                      <th>Cant.</th>
                      <th>Precio</th>
                      <th>Volumen</th>
                      <th>Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="detalle in receiptModalCompra.detalles || []" :key="detalle.id">
                      <td>{{ detalle.producto?.nombre || "Producto" }}</td>
                      <td>{{ detalle.producto?.sku || "" }}</td>
                      <td>{{ detalle.cantidad }}</td>
                      <td>Bs. {{ money(detalle.precioUnitario) }}</td>
                      <td>PV {{ money(detalle.pvUnitario) }} / QP {{ money(detalle.qpUnitario) }} / CR {{ money(detalle.crUnitario) }}</td>
                      <td>Bs. {{ money(detalle.subtotal) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="receipt-totals">
                <div><span>Total PV</span><strong>{{ money(receiptModalCompra.totalPv) }}</strong></div>
                <div><span>Total QP</span><strong>{{ money(receiptModalCompra.totalQp) }}</strong></div>
                <div><span>Total CR</span><strong>{{ money(receiptModalCompra.totalCr) }}</strong></div>
                <div v-if="compraTieneDescuento(receiptModalCompra)"><span>Subtotal</span><strong>Bs. {{ money(compraSubtotalBruto(receiptModalCompra)) }}</strong></div>
                <div v-if="compraTieneDescuento(receiptModalCompra)"><span>Descuento</span><strong>- Bs. {{ money(compraDescuento(receiptModalCompra)) }}</strong></div>
                <div><span>Total pagado</span><strong>Bs. {{ money(receiptModalCompra.subtotal) }}</strong></div>
              </div>
            </div>
          </section>

          <footer>
            <button class="vy-btn vy-btn-ghost" type="button" @click="printReceipt(receiptModalCompra)">Imprimir</button>
            <button class="vy-btn vy-btn-primary" type="button" @click="downloadReceiptPdf(receiptModalCompra)">Descargar PDF</button>
          </footer>
        </article>
      </div>

      <div v-if="detalleModalCompra" class="receipt-modal-backdrop" @click.self="closeDetallesModal">
        <article class="receipt-modal detalle-modal">
          <header>
            <div>
              <span class="vy-eyebrow">Detalle completo</span>
              <h2>Compra #{{ detalleModalCompra.id }}</h2>
              <p>{{ detalleModalCompra.estadoCompra }} · {{ fullName(detalleModalCompra.persona) }} · {{ formatDateTime(detalleModalCompra.fechaCompra) }}</p>
            </div>
            <button type="button" aria-label="Cerrar" @click="closeDetallesModal">
              <X :size="18" />
            </button>
          </header>

          <section class="detalle-modal-body">
            <div class="detalle-block">
              <h3>Productos de la compra</h3>
              <div class="receipt-table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Producto</th>
                      <th>SKU</th>
                      <th>Cant.</th>
                      <th>Precio</th>
                      <th>Volumen</th>
                      <th>Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="detalle in detalleModalCompra.detalles || []" :key="detalle.id">
                      <td>{{ detalle.producto?.nombre || "Producto" }}</td>
                      <td>{{ detalle.producto?.sku || "" }}</td>
                      <td>{{ detalle.cantidad }}</td>
                      <td>Bs. {{ money(detalle.precioUnitario) }}</td>
                      <td>PV {{ money(detalle.pvUnitario) }} / QP {{ money(detalle.qpUnitario) }} / CR {{ money(detalle.crUnitario) }}</td>
                      <td>Bs. {{ money(detalle.subtotal) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <div class="detalle-block">
              <h3>Movimientos que genero la compra</h3>
              <p v-if="detalleLoading" class="detalle-loading">Cargando movimientos...</p>
              <div v-else-if="detalleError" class="detalle-error">{{ detalleError }}</div>
              <div v-else-if="!detalleMovimientos.length" class="detalle-empty">Esta compra no genero movimientos.</div>
              <template v-else>
                <section v-for="grupo in movimientosAgrupados" :key="grupo.origen" class="detalle-grupo">
                  <header>
                    <strong>{{ grupo.label }}</strong>
                    <span>Total {{ money(grupo.total) }}</span>
                  </header>
                  <div class="receipt-table-wrap">
                    <table>
                      <thead>
                        <tr>
                          <th>Persona</th>
                          <th>Tipo</th>
                          <th>Concepto</th>
                          <th>Monto</th>
                          <th>Saldo</th>
                          <th>Fecha</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="(mov, index) in grupo.items" :key="`${grupo.origen}-${index}`">
                          <td>
                            <strong>{{ mov.personaNombres }} {{ mov.personaApellidos }}</strong>
                            <small v-if="mov.nivel">Nivel {{ mov.nivel }}</small>
                          </td>
                          <td>{{ mov.tipo }}</td>
                          <td>{{ mov.concepto }}</td>
                          <td>{{ money(mov.monto) }}</td>
                          <td>{{ money(mov.saldoResultado) }}</td>
                          <td>{{ formatDateTime(mov.fechaRegistro) }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </section>
              </template>
            </div>
          </section>

          <footer>
            <button class="vy-btn vy-btn-primary" type="button" @click="closeDetallesModal">Cerrar</button>
          </footer>
        </article>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.workspace { padding: 28px 32px 110px; min-width: 0; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 20px; }
.page-header h1 { margin-top: 8px; font-size: 30px; font-weight: 900; }
.page-header p { margin-top: 5px; color: var(--vy-ink-2); font-size: 14px; }
.page-header p strong { color: var(--vy-orange-deep); font-weight: 900; }
.header-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.period-filter { display: grid; gap: 6px; min-width: 260px; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.search-filter { min-width: 320px; flex: 1 1 320px; }
.search-filter .input-icon input::placeholder { font-weight: 800; }
.period-filter select { width: 100%; }
.refresh-action {
  min-height: 42px;
  padding: 0 15px;
  border: 1px solid var(--vy-line);
  border-radius: 12px;
  background: var(--vy-surface);
  color: var(--vy-ink-2);
  box-shadow: var(--vy-shadow-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 900;
  transition: transform .16s ease, border-color .16s ease, color .16s ease, background .16s ease;
}
.refresh-action:hover:not(:disabled) { transform: translateY(-1px); border-color: var(--vy-orange); color: var(--vy-orange-deep); background: #fffaf0; }
.refresh-action:disabled { cursor: wait; opacity: .7; }
.refresh-action .spinning { animation: refresh-spin .8s linear infinite; }
@keyframes refresh-spin { to { transform: rotate(360deg); } }
.floating-sale-actions {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 90;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}
.floating-sale-button {
  min-height: 54px;
  padding: 0 20px;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--vy-orange) 0%, var(--vy-orange-deep) 100%);
  color: #fff;
  box-shadow: 0 18px 36px rgba(242, 135, 5, .32);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 900;
  transition: transform .16s ease, box-shadow .16s ease;
}
.floating-sale-button.public { background: linear-gradient(135deg, #3f8f5c 0%, #166534 100%); box-shadow: 0 18px 36px rgba(22, 101, 52, .24); }
.floating-sale-button:hover { transform: translateY(-2px); box-shadow: 0 22px 44px rgba(242, 135, 5, .38); }
.floating-sale-button.public:hover { box-shadow: 0 22px 44px rgba(22, 101, 52, .3); }
.floating-sale-button:active { transform: translateY(0); }
.ventanilla-skeleton { display: grid; gap: 18px; }
.skeleton-card { padding: 20px; overflow: hidden; }
.skeleton-title { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; }
.skeleton-title > div { display: grid; gap: 8px; width: min(360px, 70%); }
.skeleton-block { display: block; min-width: 0; border-radius: 999px; background: linear-gradient(90deg, #f0eadb 0%, #fff7ea 45%, #f0eadb 90%); background-size: 220% 100%; animation: skeleton-shimmer 1.15s ease-in-out infinite; }
.skeleton-icon { width: 42px; height: 42px; border-radius: 14px; flex: 0 0 auto; }
.skeleton-heading { width: 220px; max-width: 100%; height: 18px; }
.skeleton-subtitle { width: 320px; max-width: 100%; height: 12px; }
.skeleton-table { display: grid; gap: 1px; min-width: 820px; overflow: hidden; border: 1px solid var(--vy-line-2); border-radius: 14px; background: var(--vy-line-2); }
.skeleton-row { display: grid; grid-template-columns: .7fr 1.25fr .8fr .75fr 1fr .75fr 1fr .5fr; gap: 12px; align-items: center; min-height: 54px; padding: 0 12px; background: #fff; }
.skeleton-row-head { min-height: 42px; background: var(--vy-ink); }
.skeleton-row-head .skeleton-block { height: 10px; background: rgba(255,255,255,.22); animation: none; }
.skeleton-row:not(.skeleton-row-head) .skeleton-block { height: 13px; }
.skeleton-row:not(.skeleton-row-head) .skeleton-block:nth-child(2),
.skeleton-row:not(.skeleton-row-head) .skeleton-block:nth-child(7) { height: 24px; border-radius: 10px; }
@keyframes skeleton-shimmer {
  0% { background-position: 120% 0; }
  100% { background-position: -120% 0; }
}
.sales-table-card { padding: 20px; }
.public-sales-card { margin-top: 18px; }
.sales-table-wrap { overflow-x: auto; padding-bottom: 76px; }
.sales-table { width: 100%; border-collapse: collapse; min-width: 980px; font-size: 13px; }
.sales-table th { padding: 12px 10px; background: var(--vy-ink); color: #fff; text-align: left; font-size: 11px; font-weight: 900; text-transform: uppercase; white-space: nowrap; }
.sales-table td { padding: 13px 10px; border-bottom: 1px solid var(--vy-line-2); color: var(--vy-ink-2); vertical-align: middle; }
.sales-table td strong, .sales-table td small { display: block; }
.sales-table td strong { color: var(--vy-ink); font-weight: 900; }
.sales-table td small { margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; white-space: nowrap; }
.status-pill { display: inline-flex; align-items: center; min-height: 26px; padding: 0 9px; border-radius: 999px; background: #fff3df; color: var(--vy-orange-deep); font-size: 11px; font-weight: 900; }
.action-menu { position: relative; display: inline-flex; justify-content: flex-end; }
.action-menu-toggle { width: 34px; height: 34px; border-radius: 9px; background: var(--vy-surface-2); color: var(--vy-ink-2); border: 1px solid var(--vy-line); display: inline-flex; align-items: center; justify-content: center; }
.action-menu-toggle:hover, .action-menu-trigger:hover { border-color: var(--vy-orange); color: var(--vy-orange-deep); background: #fffaf0; }
.action-menu-trigger { min-height: 36px; padding: 0 12px; border-radius: 10px; background: var(--vy-surface-2); color: var(--vy-ink-2); border: 1px solid var(--vy-line); display: inline-flex; align-items: center; gap: 7px; font-size: 12px; font-weight: 900; }
.action-menu-panel { position: absolute; top: calc(100% + 6px); right: 0; z-index: 40; min-width: 170px; padding: 6px; border: 1px solid var(--vy-line); border-radius: 12px; background: #fff; box-shadow: var(--vy-shadow-lg); }
.action-menu-panel button { width: 100%; min-height: 34px; padding: 0 9px; border-radius: 8px; background: transparent; color: var(--vy-ink-2); display: flex; align-items: center; gap: 8px; font-size: 12px; font-weight: 900; text-align: left; }
.action-menu-panel button:hover { background: #fffaf0; color: var(--vy-orange-deep); }
.sales-table td:last-child { text-align: right; }
.pagination-bar {
  min-width: 980px;
  margin-top: 12px;
  padding: 11px 12px;
  border: 1px solid var(--vy-line);
  border-radius: 12px;
  background: var(--vy-surface-2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.pagination-bar span {
  color: var(--vy-ink-3);
  font-size: 12px;
  font-weight: 900;
}
.pagination-bar div {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.pagination-bar button {
  min-height: 32px;
  padding: 0 11px;
  border: 1px solid var(--vy-line);
  border-radius: 9px;
  background: #fff;
  color: var(--vy-ink-2);
  font-size: 12px;
  font-weight: 900;
}
.pagination-bar button:hover:not(:disabled) { border-color: var(--vy-orange); color: var(--vy-orange-deep); background: #fffaf0; }
.pagination-bar button:disabled { opacity: .45; cursor: not-allowed; }
.pagination-bar strong { color: var(--vy-ink); font-size: 12px; font-weight: 900; white-space: nowrap; }
.shell-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(360px, 0.85fr); gap: 18px; align-items: start; }
.sale-card, .pending-card { padding: 20px; }
.card-title { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; }
.card-title h2 { font-size: 18px; font-weight: 900; }
.card-title p { margin-top: 3px; color: var(--vy-ink-3); font-size: 13px; font-weight: 700; }
.icon-box { width: 42px; height: 42px; border-radius: 14px; background: var(--vy-cream); color: var(--vy-orange-deep); display: inline-flex; align-items: center; justify-content: center; }
.field { display: block; margin-top: 14px; }
.field > span { display: block; margin-bottom: 7px; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.field > input, .field > select {
  width: 100%;
  min-height: 42px;
  padding: 0 12px;
  border: 1px solid var(--vy-line);
  border-radius: 12px;
  background: var(--vy-surface-2);
  color: var(--vy-ink);
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  outline: 0;
}
.field > input:focus, .field > select:focus { border-color: var(--vy-orange); box-shadow: 0 0 0 3px rgba(242, 135, 5, .12); background: #fff; }
.field.invalid > input, .field.invalid > select { border-color: var(--vy-danger); background: rgba(196, 69, 42, 0.06); }
.field.invalid :deep(.select2-container--default .select2-selection--single) { border-color: var(--vy-danger); background: rgba(196, 69, 42, 0.06); }
.field-error { display: block; margin-top: 7px; color: var(--vy-danger); font-size: 11px; font-weight: 900; line-height: 1.3; }
.input-icon { min-height: 42px; padding: 0 12px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); display: flex; align-items: center; gap: 8px; color: var(--vy-ink-3); }
.input-icon input { width: 100%; border: 0; outline: 0; background: transparent; color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 800; }
.persona-select { width: 100%; }
:deep(.select2-container--default .select2-selection--single) { min-height: 42px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); display: flex; align-items: center; }
:deep(.select2-container--default .select2-selection--single .select2-selection__rendered) { padding-left: 12px; padding-right: 34px; color: var(--vy-ink); font-size: 13px; font-weight: 800; line-height: 42px; }
:deep(.select2-container--default .select2-selection--single .select2-selection__placeholder) { color: var(--vy-ink-3); }
:deep(.select2-container--default .select2-selection--single .select2-selection__arrow) { height: 42px; right: 8px; }
:deep(.select2-container--default.select2-container--open .select2-selection--single) { border-color: var(--vy-orange); }
:deep(.select2-dropdown) { border: 1px solid var(--vy-line); border-radius: 12px; overflow: hidden; color: var(--vy-ink); }
:deep(.select2-search--dropdown) { padding: 8px; }
:deep(.select2-container--default .select2-search--dropdown .select2-search__field) { min-height: 36px; border: 1px solid var(--vy-line); border-radius: 9px; outline: 0; padding: 0 10px; }
:deep(.select2-results__option) { padding: 9px 12px; font-size: 13px; font-weight: 800; }
:deep(.select2-container--default .select2-results__option--highlighted.select2-results__option--selectable) { background: var(--vy-orange); color: #fff; }
.person-list, .product-picker, .orders-list { display: grid; gap: 8px; }
.person-list { margin-top: 10px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.person-list button, .product-picker button { padding: 11px 12px; border: 1px solid var(--vy-line); border-radius: 13px; background: var(--vy-surface-2); text-align: left; display: flex; align-items: center; gap: 10px; }
.person-list button.active, .person-list button:hover, .product-picker button:hover { border-color: var(--vy-orange); background: #fffaf0; }
.person-list strong, .product-picker strong { display: block; color: var(--vy-ink); font-size: 13px; font-weight: 900; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.person-list small, .product-picker small { display: block; margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; }
.club-royale-badge { width: fit-content; min-height: 24px; margin-top: 7px; padding: 0 9px; border: 1px solid rgba(22, 101, 52, 0.24); border-radius: 999px; background: rgba(22, 101, 52, 0.1); color: #166534; display: inline-flex; align-items: center; justify-content: center; font-size: 10px; font-style: normal; font-weight: 950; text-transform: uppercase; white-space: nowrap; }
.discount-badge { width: fit-content; min-height: 23px; margin-top: 7px; padding: 0 8px; border-radius: 999px; background: #fff3df; color: var(--vy-orange-deep); display: inline-flex; align-items: center; justify-content: center; font-size: 10px; font-style: normal; font-weight: 950; text-transform: uppercase; white-space: nowrap; }
.selected-person { margin-top: 10px; padding: 10px 12px; border-radius: 12px; background: rgba(63, 143, 92, 0.1); color: var(--vy-success); font-size: 13px; font-weight: 800; }
.selected-person span { display: block; margin-top: 3px; color: var(--vy-ink-2); font-size: 12px; font-weight: 800; }
.product-picker { margin-top: 10px; max-height: 300px; overflow: auto; }
.product-picker button span { flex: 1; min-width: 0; }
.product-picker button b { white-space: nowrap; font-size: 13px; }
.sale-items { display: grid; gap: 8px; margin-top: 16px; }
.sale-item { display: grid; grid-template-columns: minmax(0, 1fr) 136px 100px 34px; align-items: center; gap: 10px; padding: 10px 0; border-top: 1px solid var(--vy-line-2); }
.sale-item strong, .sale-item small { display: block; }
.sale-item strong { font-size: 13px; font-weight: 900; }
.sale-item small { margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; }
.quantity-stepper { min-height: 38px; padding: 3px; border: 1px solid var(--vy-line); border-radius: 999px; background: #fff; box-shadow: inset 0 1px 0 rgba(255,255,255,.8); display: grid; grid-template-columns: 32px minmax(34px, 1fr) 32px; align-items: center; gap: 3px; }
.quantity-stepper button { width: 32px; height: 32px; border-radius: 50%; background: var(--vy-surface-2); color: var(--vy-ink-2); border: 1px solid transparent; font-size: 18px; line-height: 1; font-weight: 900; display: inline-flex; align-items: center; justify-content: center; transition: background .15s ease, color .15s ease, border-color .15s ease, transform .15s ease; }
.quantity-stepper button:hover { background: #fff3df; border-color: rgba(242, 135, 5, .35); color: var(--vy-orange-deep); transform: translateY(-1px); }
.quantity-stepper button:active { transform: translateY(0); }
.quantity-stepper input { width: 100%; min-width: 0; height: 32px; padding: 0 2px; border: 0; border-radius: 999px; background: transparent; color: var(--vy-ink); text-align: center; font-size: 13px; font-weight: 900; outline: 0; }
.quantity-stepper input:focus { background: #fffaf0; box-shadow: 0 0 0 2px rgba(242, 135, 5, .12); }
.quantity-stepper input::-webkit-outer-spin-button,
.quantity-stepper input::-webkit-inner-spin-button { margin: 0; appearance: none; }
.sale-item b { text-align: right; font-size: 13px; }
.sale-item > button { width: 34px; height: 34px; border-radius: 10px; background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); display: inline-flex; align-items: center; justify-content: center; }
.discount-box { margin-top: 16px; padding: 14px; border: 1px solid var(--vy-line); border-radius: 14px; background: #fffaf0; }
.discount-grid { display: grid; grid-template-columns: 180px minmax(0, 1fr); gap: 12px; }
.discount-box .field { margin-top: 0; }
.discount-box input { width: 100%; min-height: 40px; padding: 0 11px; border: 1px solid var(--vy-line); border-radius: 11px; background: #fff; color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 800; outline: 0; }
.discount-box input:focus { border-color: var(--vy-orange); box-shadow: 0 0 0 3px rgba(242, 135, 5, .12); }
.discount-box input:disabled { background: var(--vy-surface-2); color: var(--vy-ink-3); }
.discount-error { margin-top: 9px; color: var(--vy-danger); font-size: 12px; font-weight: 900; }
.sale-footer { margin-top: 18px; padding-top: 16px; border-top: 1px solid var(--vy-line); display: grid; grid-template-columns: auto 1fr auto; align-items: end; gap: 16px; }
.sale-footer small, .sale-footer span { display: block; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.sale-footer strong { display: block; margin-top: 4px; font-size: 22px; font-weight: 900; }
.cash-code { font-family: var(--font-mono); letter-spacing: 0.12em; }
.order-card { padding: 14px; border: 1px solid var(--vy-line); border-radius: 16px; background: var(--vy-surface-2); }
.order-card header { display: flex; justify-content: space-between; gap: 12px; }
.order-card header strong, .order-card header small { display: block; }
.order-card header strong { font-size: 15px; font-weight: 900; }
.order-card header small { margin-top: 3px; color: var(--vy-orange-deep); font-size: 11px; font-weight: 900; }
.order-card header b { white-space: nowrap; }
.order-card p { margin-top: 10px; color: var(--vy-ink-2); font-size: 13px; font-weight: 800; }
.order-meta { display: flex; flex-wrap: wrap; gap: 7px; margin-top: 10px; }
.order-meta span { padding: 4px 8px; border-radius: 999px; background: #fff; border: 1px solid var(--vy-line-2); color: var(--vy-ink-2); font-size: 11px; font-weight: 900; }
.details-list { display: grid; gap: 4px; margin-top: 10px; }
.details-list div { display: flex; justify-content: space-between; gap: 10px; color: var(--vy-ink-2); font-size: 12px; font-weight: 800; }
.proof-link { width: fit-content; margin-top: 12px; padding: 8px 10px; border-radius: 10px; background: #fff; border: 1px solid var(--vy-line); color: var(--vy-orange-deep); display: inline-flex; align-items: center; gap: 7px; font-size: 12px; font-weight: 900; }
.order-card footer { display: flex; justify-content: flex-end; margin-top: 12px; }
.validate { background: var(--vy-orange); }
.deliver { background: var(--vy-success); }
.reject { background: var(--vy-danger); }
.receipt { background: var(--vy-ink); }
.empty-state { min-height: 260px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px; text-align: center; color: var(--vy-ink-3); }
.empty-state strong { color: var(--vy-ink); }
.error-box, .success-box, .loading-box { margin-bottom: 14px; padding: 13px 15px; border-radius: 12px; font-size: 13px; font-weight: 800; }
.error-box { color: var(--vy-danger); background: rgba(196, 69, 42, 0.1); }
.success-box { color: var(--vy-success); background: rgba(63, 143, 92, 0.1); }
.loading-box { color: var(--vy-ink-2); background: var(--vy-surface-2); }
.sale-modal-backdrop { position: fixed; inset: 0; z-index: 118; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(31, 26, 20, 0.5); backdrop-filter: blur(6px); }
.sale-modal { width: min(980px, 100%); max-height: calc(100vh - 40px); padding: 20px; border-radius: 22px; border: 1px solid var(--vy-line); background: var(--vy-surface); box-shadow: var(--vy-shadow-lg); color: var(--vy-ink); overflow: hidden; display: flex; flex-direction: column; }
.sale-modal > header { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; padding-bottom: 14px; border-bottom: 1px solid var(--vy-line-2); }
.sale-modal h2 { margin-top: 4px; font-size: 22px; font-weight: 900; }
.sale-modal p { margin-top: 3px; color: var(--vy-ink-3); font-size: 13px; font-weight: 800; }
.sale-modal > header button { width: 38px; height: 38px; border-radius: 12px; background: var(--vy-surface-2); color: var(--vy-ink-2); display: inline-flex; align-items: center; justify-content: center; }
.sale-modal-body { overflow: auto; padding-top: 16px; }
.sale-modal .sale-card { padding: 0; }
.public-sale-modal { width: min(1120px, 100%); }
.public-sale-grid { display: grid; grid-template-columns: minmax(320px, .9fr) minmax(0, 1.15fr); gap: 18px; }
.public-sale-panel { min-width: 0; padding: 16px; border: 1px solid var(--vy-line); border-radius: 16px; background: #fff; }
.public-client-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 12px; }
.public-client-wide { grid-column: 1 / -1; }
.public-document-search { min-height: 42px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: stretch; overflow: hidden; transition: border-color .15s ease, box-shadow .15s ease, background .15s ease; }
.public-document-search:focus-within { border-color: var(--vy-orange); box-shadow: 0 0 0 3px rgba(242, 135, 5, .12); background: #fff; }
.public-document-search input { width: 100%; min-width: 0; min-height: 42px; padding: 0 12px; border: 0; background: transparent; color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 800; outline: 0; }
.public-document-search button { min-height: 42px; padding: 0 15px; border-left: 1px solid var(--vy-line); border-radius: 0; background: var(--vy-ink); color: #fff; font-size: 12px; font-weight: 900; white-space: nowrap; transition: background .15s ease, color .15s ease; }
.public-document-search button:hover:not(:disabled) { background: var(--vy-orange); }
.public-document-search button:disabled { cursor: wait; opacity: .72; }
.field.invalid .public-document-search { border-color: var(--vy-danger); background: rgba(196, 69, 42, 0.06); }
.lookup-message { display: block; margin-top: 7px; color: var(--vy-success); font-size: 11px; font-weight: 900; line-height: 1.3; }
.toggle-field { margin-top: 14px; min-height: 42px; padding: 0 12px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); display: flex; align-items: center; gap: 9px; color: var(--vy-ink-2); font-size: 13px; font-weight: 900; }
.toggle-field input { width: 17px; height: 17px; accent-color: var(--vy-success); }
.public-product-picker { max-height: 250px; }
.inline-empty { padding: 14px; border: 1px dashed var(--vy-line); border-radius: 12px; color: var(--vy-ink-3); background: var(--vy-surface-2); font-size: 13px; font-weight: 800; text-align: center; }
.public-products-error { margin-top: 10px; }
.public-sale-item { grid-template-columns: minmax(0, 1fr) 136px 112px 34px; }
.public-sale-footer { grid-template-columns: minmax(0, 1fr) auto; }
.public-review-backdrop { position: fixed; inset: 0; z-index: 121; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(31, 26, 20, 0.55); backdrop-filter: blur(7px); }
.public-review-modal { width: min(1080px, 100%); max-height: calc(100vh - 40px); padding: 20px; border-radius: 22px; border: 1px solid var(--vy-line); background: var(--vy-surface); box-shadow: var(--vy-shadow-lg); color: var(--vy-ink); overflow: hidden; display: flex; flex-direction: column; }
.public-review-modal > header, .public-review-modal > footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.public-review-modal > header { padding-bottom: 14px; border-bottom: 1px solid var(--vy-line-2); }
.public-review-modal h2 { margin-top: 4px; font-size: 22px; font-weight: 900; }
.public-review-modal p { margin-top: 3px; color: var(--vy-ink-3); font-size: 13px; font-weight: 800; }
.public-review-modal > header button { width: 38px; height: 38px; border-radius: 12px; background: var(--vy-surface-2); color: var(--vy-ink-2); display: inline-flex; align-items: center; justify-content: center; }
.public-review-body { margin: 16px 0; overflow: auto; }
.review-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.review-grid div { padding: 14px; border: 1px solid var(--vy-line); border-radius: 14px; background: var(--vy-surface-2); }
.review-grid small { display: block; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.review-grid strong { display: block; margin-top: 5px; color: var(--vy-ink); font-size: 14px; font-weight: 900; }
.review-grid span { display: block; margin-top: 4px; color: var(--vy-ink-2); font-size: 12px; font-weight: 800; line-height: 1.35; }
.public-detail-table { margin-top: 16px; overflow-x: auto; border: 1px solid var(--vy-line); border-radius: 14px; }
.public-detail-table table { width: 100%; min-width: 900px; border-collapse: collapse; font-size: 13px; }
.public-detail-table th { padding: 11px 10px; background: var(--vy-ink); color: #fff; text-align: left; font-size: 11px; font-weight: 900; text-transform: uppercase; white-space: nowrap; }
.public-detail-table td { padding: 12px 10px; border-bottom: 1px solid var(--vy-line-2); color: var(--vy-ink-2); vertical-align: top; }
.public-detail-table tr:last-child td { border-bottom: 0; }
.public-detail-table td:nth-child(n + 2), .public-detail-table th:nth-child(n + 2) { text-align: right; white-space: nowrap; }
.public-detail-table td strong, .public-detail-table td small { display: block; }
.public-detail-table td strong { color: var(--vy-ink); font-weight: 900; }
.public-detail-table td small { margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; }
.review-totals { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-top: 16px; }
.review-totals div { padding: 13px; border: 1px solid var(--vy-line); border-radius: 14px; background: #fffaf0; }
.review-totals span, .review-totals strong { display: block; }
.review-totals span { color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.review-totals strong { margin-top: 5px; color: var(--vy-ink); font-size: 16px; font-weight: 900; }
.public-review-modal > footer { justify-content: flex-end; padding-top: 14px; border-top: 1px solid var(--vy-line-2); }
.proof-modal-backdrop { position: fixed; inset: 0; z-index: 130; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(31, 26, 20, 0.5); backdrop-filter: blur(6px); }
.proof-modal { width: min(900px, 100%); max-height: calc(100vh - 40px); padding: 20px; border-radius: 22px; border: 1px solid var(--vy-line); background: var(--vy-surface); box-shadow: var(--vy-shadow-lg); color: var(--vy-ink); overflow: hidden; display: flex; flex-direction: column; }
.proof-modal header, .proof-modal footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.proof-modal header { padding-bottom: 14px; border-bottom: 1px solid var(--vy-line-2); }
.proof-modal h2 { margin-top: 4px; font-size: 22px; font-weight: 900; }
.proof-modal p { margin-top: 3px; color: var(--vy-ink-3); font-size: 13px; font-weight: 800; }
.proof-modal header button { width: 38px; height: 38px; border-radius: 12px; background: var(--vy-surface-2); color: var(--vy-ink-2); display: inline-flex; align-items: center; justify-content: center; }
.proof-viewer { min-height: 280px; max-height: min(68vh, 720px); margin: 16px 0; border-radius: 16px; border: 1px solid var(--vy-line); background: var(--vy-surface-2); overflow: hidden; display: flex; align-items: center; justify-content: center; }
.proof-viewer img { width: 100%; height: 100%; max-height: min(68vh, 720px); object-fit: contain; background: #fff; }
.proof-viewer iframe { width: 100%; height: min(68vh, 720px); border: 0; background: #fff; }
.proof-modal footer { justify-content: flex-end; padding-top: 14px; border-top: 1px solid var(--vy-line-2); }
.receipt-modal-backdrop { position: fixed; inset: 0; z-index: 125; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(31, 26, 20, 0.55); backdrop-filter: blur(7px); }
.receipt-modal { width: min(1040px, 100%); max-height: calc(100vh - 40px); padding: 20px; border-radius: 22px; border: 1px solid var(--vy-line); background: var(--vy-surface); box-shadow: var(--vy-shadow-lg); color: var(--vy-ink); overflow: hidden; display: flex; flex-direction: column; }
.receipt-modal > header, .receipt-modal > footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.receipt-modal > header { padding-bottom: 14px; border-bottom: 1px solid var(--vy-line-2); }
.receipt-modal h2 { margin-top: 4px; font-size: 22px; font-weight: 900; }
.receipt-modal p { margin-top: 3px; color: var(--vy-ink-3); font-size: 13px; font-weight: 800; }
.receipt-modal > header button { width: 38px; height: 38px; border-radius: 12px; background: var(--vy-surface-2); color: var(--vy-ink-2); display: inline-flex; align-items: center; justify-content: center; }
.receipt-preview { margin: 16px 0; padding: 18px; border-radius: 18px; background: #f6f2e9; overflow: auto; }
.receipt-sheet { width: min(900px, 100%); margin: 0 auto; padding: 28px; border: 1px solid #eadfca; border-radius: 22px; background: #fff; box-shadow: 0 18px 48px rgba(31, 26, 20, 0.1); }
.receipt-sheet-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 22px; padding-bottom: 20px; border-bottom: 3px solid var(--vy-orange); }
.receipt-sheet-header img { width: 180px; max-width: 100%; object-fit: contain; }
.receipt-sheet-header h3 { margin-top: 10px; font-size: 26px; font-weight: 900; letter-spacing: -0.03em; }
.receipt-sheet-header span { display: inline-flex; width: fit-content; margin-top: 8px; padding: 6px 12px; border-radius: 999px; background: #fff3df; color: var(--vy-orange-deep); font-size: 12px; font-weight: 900; }
.receipt-sheet-header > div:last-child { min-width: 180px; text-align: right; color: var(--vy-ink-2); }
.receipt-sheet-header strong { display: block; margin-top: 6px; font-size: 12px; font-weight: 900; color: var(--vy-ink); }
.receipt-sheet-header p { margin-top: 2px; color: var(--vy-ink-2); font-size: 13px; }
.receipt-info-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-top: 22px; }
.receipt-info-grid div { padding: 14px; border: 1px solid #eadfca; border-radius: 15px; background: #fffaf0; }
.receipt-info-grid small { display: block; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; letter-spacing: 0.07em; text-transform: uppercase; }
.receipt-info-grid strong { display: block; margin-top: 5px; font-size: 14px; font-weight: 900; }
.receipt-table-wrap { margin-top: 22px; overflow-x: auto; }
.receipt-table-wrap table { width: 100%; border-collapse: collapse; font-size: 13px; }
.receipt-table-wrap th { padding: 11px 9px; background: var(--vy-ink); color: #fff; text-align: left; font-size: 11px; text-transform: uppercase; white-space: nowrap; }
.receipt-table-wrap td { padding: 12px 9px; border-bottom: 1px solid var(--vy-line-2); color: var(--vy-ink-2); vertical-align: top; }
.receipt-table-wrap th:nth-child(n + 3), .receipt-table-wrap td:nth-child(n + 3) { text-align: right; white-space: nowrap; }
.receipt-totals { width: min(360px, 100%); margin: 22px 0 0 auto; border: 1px solid #eadfca; border-radius: 16px; overflow: hidden; }
.receipt-totals div { display: flex; justify-content: space-between; gap: 12px; padding: 12px 14px; border-bottom: 1px solid var(--vy-line-2); }
.receipt-totals div:last-child { border-bottom: 0; background: var(--vy-orange); color: #fff; font-size: 18px; font-weight: 900; }
.receipt-modal > footer { justify-content: flex-end; padding-top: 14px; border-top: 1px solid var(--vy-line-2); }
.detalle-modal { width: min(1040px, 100%); }
.detalle-modal-body { margin: 16px 0; padding-right: 4px; overflow: auto; display: grid; gap: 20px; }
.detalle-block h3 { margin-bottom: 10px; font-size: 15px; font-weight: 900; color: var(--vy-ink); }
.detalle-grupo { border: 1px solid var(--vy-line); border-radius: 16px; overflow: hidden; background: var(--vy-surface-2); }
.detalle-grupo > header { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 14px; background: #fffaf0; border-bottom: 1px solid var(--vy-line-2); }
.detalle-grupo > header strong { font-size: 14px; font-weight: 900; color: var(--vy-ink); }
.detalle-grupo > header span { font-size: 12px; font-weight: 900; color: var(--vy-orange-deep); }
.detalle-grupo .receipt-table-wrap { margin-top: 0; background: #fff; }
.detalle-grupo td small { display: block; margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; }
.detalle-loading, .detalle-empty { padding: 18px; color: var(--vy-ink-3); font-weight: 800; }
.detalle-error { padding: 18px; color: var(--vy-danger); font-weight: 800; }
@media (max-width: 1120px) {
  .shell-grid { grid-template-columns: 1fr; }
}
@media (max-width: 720px) {
  .workspace { padding: 24px 20px 32px; }
  .workspace { padding-bottom: 110px; }
  .page-header { align-items: stretch; flex-direction: column; }
  .header-actions { align-items: stretch; flex-direction: column; }
  .period-filter { min-width: 0; width: 100%; }
  .refresh-action { width: 100%; }
  .floating-sale-actions { right: 20px; bottom: 20px; }
  .floating-sale-button { min-height: 52px; padding: 0 18px; }
  .skeleton-card { padding: 16px; }
  .skeleton-title { align-items: flex-start; }
  .skeleton-table { min-width: 0; border: 0; background: transparent; gap: 10px; }
  .skeleton-row-head { display: none; }
  .skeleton-row { grid-template-columns: 1fr; gap: 9px; min-height: 132px; padding: 14px; border: 1px solid var(--vy-line-2); border-radius: 14px; }
  .skeleton-row .skeleton-block:nth-child(n + 5) { display: none; }
  .person-list, .sale-footer, .sale-item, .public-sale-grid, .public-client-grid, .public-sale-footer { grid-template-columns: 1fr; }
  .public-document-search { grid-template-columns: 1fr; }
  .public-document-search button { width: 100%; }
  .discount-grid { grid-template-columns: 1fr; }
  .sale-item b { text-align: left; }
  .quantity-stepper, .sale-item > button { width: 100%; }
  .proof-modal { padding: 16px; }
  .proof-modal header, .proof-modal footer { align-items: stretch; flex-direction: column; }
  .proof-modal footer .vy-btn { width: 100%; }
  .public-review-modal { padding: 16px; }
  .public-review-modal > header, .public-review-modal > footer { align-items: stretch; flex-direction: column; }
  .public-review-modal > footer .vy-btn { width: 100%; }
  .review-grid, .review-totals { grid-template-columns: 1fr; }
  .receipt-modal { padding: 16px; }
  .receipt-modal > header, .receipt-modal > footer, .receipt-sheet-header { align-items: stretch; flex-direction: column; }
  .receipt-modal > footer .vy-btn { width: 100%; }
  .receipt-preview { padding: 10px; }
  .receipt-sheet { padding: 18px; }
  .receipt-sheet-header > div:last-child { text-align: left; }
  .receipt-info-grid { grid-template-columns: 1fr; }
}
</style>
