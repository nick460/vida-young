<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import $ from "jquery";
import select2 from "select2";
import "select2/dist/css/select2.css";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { jsPDF } from "jspdf";
import {
  CheckCircle2,
  CircleX,
  ClipboardCheck,
  FileText,
  PackageCheck,
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

select2($);

const personas = ref([]);
const productos = ref([]);
const compras = ref([]);
const comprasPublicas = ref([]);
const periodosVenta = ref([]);
const selectedPeriodoId = ref("");
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const personaQuery = ref("");
const productQuery = ref("");
const selectedPersonaId = ref("");
const ventaItems = ref([]);
const saleModalOpen = ref(false);
const personaSelect = ref(null);
const periodoSelect = ref(null);
const proofModalCompra = ref(null);
const receiptModalCompra = ref(null);
const publicReviewModalCompra = ref(null);

const cajaCode = ref(generateCajaCode());
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

const selectedPersona = computed(() =>
  personas.value.find((persona) => Number(persona.id) === Number(selectedPersonaId.value))
);

const visibleCompras = computed(() =>
  compras.value.filter((compra) => ["PENDIENTE", "VALIDADA", "ENTREGADA"].includes(compra.estadoCompra))
);

const visibleComprasPublicas = computed(() =>
  comprasPublicas.value.filter((compra) => ["PENDIENTE", "VALIDADA", "ENTREGADA"].includes(compra.estadoCompra))
);

const selectedPeriodo = computed(() =>
  periodosVenta.value.find((periodo) => Number(periodo.id) === Number(selectedPeriodoId.value))
);

const saleTotal = computed(() =>
  ventaItems.value.reduce((sum, item) => sum + Number(item.precio || 0) * Number(item.cantidad || 0), 0)
);

const salePv = computed(() =>
  ventaItems.value.reduce((sum, item) => sum + Number(item.pv || 0) * Number(item.cantidad || 0), 0)
);

const saleQp = computed(() =>
  ventaItems.value.reduce((sum, item) => sum + Number(item.qp || 0) * Number(item.cantidad || 0), 0)
);

const saleCr = computed(() =>
  ventaItems.value.reduce((sum, item) => sum + Number(item.cr || 0) * Number(item.cantidad || 0), 0)
);

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function generateCajaCode() {
  return String(Math.floor(100000 + Math.random() * 900000));
}

function fullName(persona) {
  return `${persona?.nombres || ""} ${persona?.apellidos || ""}`.trim() || "Sin nombre";
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

function openSaleModal() {
  saleModalOpen.value = true;
}

function closeSaleModal() {
  saleModalOpen.value = false;
  destroyPersonaSelect2();
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
    <tr>
      <td>${escapeHtml(detalle.producto?.nombre || "Producto")}</td>
      <td>${escapeHtml(detalle.producto?.sku || "")}</td>
      <td class="num">${Number(detalle.cantidad || 0)}</td>
      <td class="num">Bs. ${money(detalle.precioUnitario)}</td>
      <td class="num">PV ${money(detalle.pvUnitario)} / QP ${money(detalle.qpUnitario)} / CR ${money(detalle.crUnitario)}</td>
      <td class="num">Bs. ${money(detalle.subtotal)}</td>
    </tr>
  `).join("");

  return `<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <title>Comprobante Compra #${escapeHtml(compra.id)}</title>
  <style>
    * { box-sizing: border-box; }
    body { margin: 0; padding: 34px; background: #f6f2e9; color: #1f1a14; font-family: Arial, sans-serif; }
    .receipt { max-width: 920px; margin: 0 auto; padding: 34px; background: #fff; border: 1px solid #eadfca; border-radius: 24px; box-shadow: 0 18px 48px rgba(31, 26, 20, .1); }
    header { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; padding-bottom: 22px; border-bottom: 3px solid #f28705; }
    .logo { width: 180px; object-fit: contain; }
    h1 { margin: 0; font-size: 28px; letter-spacing: -.03em; }
    .badge { display: inline-block; margin-top: 8px; padding: 6px 12px; border-radius: 999px; background: #fff3df; color: #f27405; font-weight: 800; font-size: 12px; }
    .meta { text-align: right; color: #4a4135; font-size: 13px; line-height: 1.5; }
    .grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; margin-top: 24px; }
    .box { padding: 16px; border: 1px solid #eadfca; border-radius: 16px; background: #fffaf0; }
    .box span { display: block; color: #897f70; font-size: 11px; font-weight: 800; text-transform: uppercase; letter-spacing: .08em; }
    .box strong { display: block; margin-top: 6px; font-size: 16px; }
    table { width: 100%; border-collapse: collapse; margin-top: 24px; font-size: 13px; }
    th { padding: 11px 9px; background: #1f1a14; color: #fff; text-align: left; font-size: 11px; text-transform: uppercase; }
    td { padding: 12px 9px; border-bottom: 1px solid #f0eadb; color: #4a4135; }
    .num { text-align: right; white-space: nowrap; }
    .totals { margin-top: 22px; margin-left: auto; width: min(360px, 100%); border: 1px solid #eadfca; border-radius: 16px; overflow: hidden; }
    .totals div { display: flex; justify-content: space-between; gap: 12px; padding: 12px 14px; border-bottom: 1px solid #f0eadb; }
    .totals div:last-child { border-bottom: 0; background: #f28705; color: #fff; font-size: 18px; font-weight: 900; }
    footer { margin-top: 28px; padding-top: 18px; border-top: 1px solid #f0eadb; color: #897f70; font-size: 12px; line-height: 1.55; }
    @media print {
      body { padding: 0; background: #fff; }
      .receipt { box-shadow: none; border-radius: 0; border: 0; }
    }
  </style>
</head>
<body>
  <main class="receipt">
    <header>
      <div>
        <img class="logo" src="${logoFull}" alt="Vida Young">
        <h1>Comprobante de compra</h1>
        <span class="badge">Compra #${escapeHtml(compra.id)} · ${escapeHtml(compra.estadoCompra)}</span>
      </div>
      <div class="meta">
        <strong>Fecha de compra</strong><br>${escapeHtml(formatDateTime(compra.fechaCompra))}<br>
        <strong>Metodo</strong><br>${escapeHtml(compra.metodoPago || "Sin metodo")}
      </div>
    </header>

    <section class="grid">
      <div class="box"><span>Cliente</span><strong>${escapeHtml(fullName(compra.persona))}</strong></div>
      <div class="box"><span>Documento</span><strong>${escapeHtml(compra.persona?.documento || "Sin documento")}</strong></div>
      <div class="box"><span>Validado por</span><strong>${escapeHtml(compra.usuarioValidacion || "Sin validar")}</strong></div>
      <div class="box"><span>Fecha y hora validacion</span><strong>${escapeHtml(formatDateTime(compra.fechaValidacion))}</strong></div>
      <div class="box"><span>Entregado por</span><strong>${escapeHtml(compra.usuarioEntrega || "Pendiente")}</strong></div>
      <div class="box"><span>Fecha y hora entrega</span><strong>${escapeHtml(formatDateTime(compra.fechaEntrega))}</strong></div>
    </section>

    <table>
      <thead>
        <tr><th>Producto</th><th>SKU</th><th class="num">Cant.</th><th class="num">Precio</th><th class="num">Volumen</th><th class="num">Subtotal</th></tr>
      </thead>
      <tbody>${detalles}</tbody>
    </table>

    <section class="totals">
      <div><span>Total PV</span><strong>${money(compra.totalPv)}</strong></div>
      <div><span>Total QP</span><strong>${money(compra.totalQp)}</strong></div>
      <div><span>Total CR</span><strong>${money(compra.totalCr)}</strong></div>
      <div><span>Total pagado</span><strong>Bs. ${money(compra.subtotal)}</strong></div>
    </section>

    <footer>
      Comprobante generado por el sistema Vida Young. Documento valido como respaldo interno de compra, validacion y entrega en ventanilla.
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
    ["Fecha y hora validacion", formatDateTime(compra.fechaValidacion)],
    ["Entregado por", compra.usuarioEntrega || "Pendiente"],
    ["Fecha y hora entrega", formatDateTime(compra.fechaEntrega)]
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
  doc.roundedRect(pageWidth - 86, y, 72, 39, 4, 4, "F");
  doc.setTextColor(31, 26, 20);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(9);
  doc.text("Total PV", pageWidth - 80, y + 8);
  doc.text(money(compra.totalPv), pageWidth - 18, y + 8, { align: "right" });
  doc.text("Total QP", pageWidth - 80, y + 16);
  doc.text(money(compra.totalQp), pageWidth - 18, y + 16, { align: "right" });
  doc.text("Total CR", pageWidth - 80, y + 24);
  doc.text(money(compra.totalCr), pageWidth - 18, y + 24, { align: "right" });
  doc.setFillColor(242, 135, 5);
  doc.roundedRect(pageWidth - 86, y + 30, 72, 12, 4, 4, "F");
  doc.setTextColor(255, 255, 255);
  doc.text("Total", pageWidth - 80, y + 38);
  doc.text(`Bs. ${money(compra.subtotal)}`, pageWidth - 18, y + 38, { align: "right" });

  doc.save(`comprobante-compra-${compra.id}.pdf`);
}

function printReceipt(compra) {
  const printWindow = window.open("", "_blank", "width=980,height=720");
  if (!printWindow) {
    showError("No se pudo imprimir", "El navegador bloqueo la ventana de impresion.");
    return;
  }

  printWindow.document.open();
  printWindow.document.write(buildReceiptHtml(compra));
  printWindow.document.close();
  printWindow.focus();
  printWindow.setTimeout(() => {
    printWindow.print();
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

function changeQuantity(item, value) {
  const next = Number(value || 1);
  item.cantidad = Math.max(1, next);
}

function removeItem(item) {
  ventaItems.value = ventaItems.value.filter((current) => Number(current.id) !== Number(item.id));
}

async function loadAll() {
  loading.value = true;
  error.value = "";

  try {
    const [personasData, productosData] = await Promise.all([
      apiRequest("/api/personas"),
      apiRequest("/api/productos")
    ]);
    personas.value = personasData;
    productos.value = productosData;
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

async function createCajaSale() {
  error.value = "";

  if (!selectedPersonaId.value) {
    await showError("Falta cliente", "Selecciona la persona que realiza la compra.");
    return;
  }

  if (!ventaItems.value.length) {
    await showError("Faltan productos", "Agrega al menos un producto a la venta.");
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
      referenciaPago: `Venta por ventanilla codigo ${cajaCode.value}`
    };
    const formData = new FormData();
    formData.append("compra", new Blob([JSON.stringify(payload)], { type: "application/json" }));

    const response = await apiRequest(`/api/compras/persona/${selectedPersonaId.value}/comprobante`, {
      method: "POST",
      body: formData
    });

    await showSuccess("Venta registrada", `Compra #${response.compra?.id} creada como PENDIENTE. Codigo de caja: ${cajaCode.value}.`);
    ventaItems.value = [];
    selectedPersonaId.value = "";
    personaQuery.value = "";
    productQuery.value = "";
    cajaCode.value = generateCajaCode();
    closeSaleModal();
    await loadAll();
  } catch (exception) {
    await showError("No se pudo registrar", exception.message || "No se pudo registrar la venta.");
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
    await loadAll();
  } catch (exception) {
    await showError("No se pudo actualizar", exception.message || "No se pudo actualizar la compra.");
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
    await loadAll();
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

watch(personas, () => {
  initPersonaSelect2();
});

watch(periodosVenta, () => {
  initPeriodoSelect2();
});

watch(selectedPersonaId, (value) => {
  if (!personaSelect.value) return;
  const element = $(personaSelect.value);
  if (element.hasClass("select2-hidden-accessible") && element.val() !== value) {
    element.val(value || null).trigger("change.select2");
  }
});

watch(selectedPeriodoId, (value) => {
  if (!periodoSelect.value) return;
  const element = $(periodoSelect.value);
  if (element.hasClass("select2-hidden-accessible") && element.val() !== value) {
    element.val(value || null).trigger("change.select2");
  }
});

onBeforeUnmount(() => {
  destroyPersonaSelect2();
  destroyPeriodoSelect2();
});

onMounted(loadAll);
</script>

<template>
  <div class="vy admin-sales-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Ventanilla</div>
          <h1>Ventas y validacion de pedidos</h1>
          <p>
            Registra compras presenciales, valida pagos pendientes y marca entregas en caja.
            <strong v-if="selectedPeriodo">Mostrando {{ selectedPeriodo.nombre }}.</strong>
          </p>
        </div>
        <div class="header-actions">
          <label class="period-filter">
            <span>Mes</span>
            <select ref="periodoSelect" v-model="selectedPeriodoId">
              <option value="" disabled>Selecciona un mes</option>
              <option v-for="periodo in periodosVenta" :key="periodo.id" :value="periodo.id">
                {{ periodo.nombre }} - Gestion {{ periodo.gestion?.anio || "" }}
              </option>
            </select>
          </label>
          <button class="vy-btn vy-btn-ghost" type="button" @click="loadAll">
            <RefreshCw :size="15" /> Actualizar
          </button>
          <button class="vy-btn vy-btn-primary" type="button" @click="openSaleModal">
            <Plus :size="16" /> Nueva venta
          </button>
        </div>
      </header>

      <div v-if="error" class="error-box">{{ error }}</div>
      <div v-if="loading" class="loading-box">Cargando ventanilla...</div>

      <section class="vy-card sales-table-card">
        <div class="card-title">
          <span class="icon-box"><ClipboardCheck :size="18" /></span>
          <div>
            <h2>Ventas realizadas</h2>
            <p>Compras registradas por tienda o ventanilla, con control de validacion y entrega.</p>
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
              <tr v-for="compra in visibleCompras" :key="compra.id">
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
                <td><strong>Bs. {{ money(compra.subtotal) }}</strong></td>
                <td>{{ formatDateTime(compra.fechaCompra) }}</td>
                <td>
                  <div class="table-actions">
                    <button v-if="compra.comprobantePagoUrl" type="button" title="Ver comprobante" @click="openProofModal(compra)">
                      <FileText :size="15" />
                    </button>
                    <button v-if="compra.estadoCompra === 'PENDIENTE'" type="button" title="Validar" @click="updateCompraEstado(compra, 'VALIDADA')">
                      <CheckCircle2 :size="15" />
                    </button>
                    <button type="button" title="Entregar" @click="updateCompraEstado(compra, 'ENTREGADA')">
                      <PackageCheck :size="15" />
                    </button>
                    <button type="button" title="Rechazar" @click="updateCompraEstado(compra, 'RECHAZADA')">
                      <CircleX :size="15" />
                    </button>
                    <button
                      v-if="['VALIDADA', 'ENTREGADA'].includes(compra.estadoCompra)"
                      type="button"
                      title="Comprobante"
                      @click="openReceiptModal(compra)"
                    >
                      <ClipboardCheck :size="15" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

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
              <tr v-for="compra in visibleComprasPublicas" :key="compra.id">
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
                  <div class="table-actions">
                    <button v-if="compra.comprobantePagoUrl" type="button" title="Ver comprobante" @click="openProofModal(compra)">
                      <FileText :size="15" />
                    </button>
                    <button v-if="compra.estadoCompra === 'PENDIENTE'" type="button" title="Revisar y validar" @click="openPublicReviewModal(compra)">
                      <CheckCircle2 :size="15" />
                    </button>
                    <button type="button" title="Entregar" @click="updateCompraPublicaEstado(compra, 'ENTREGADA')">
                      <PackageCheck :size="15" />
                    </button>
                    <button type="button" title="Rechazar" @click="updateCompraPublicaEstado(compra, 'RECHAZADA')">
                      <CircleX :size="15" />
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div v-if="!visibleComprasPublicas.length" class="empty-state">
            <PackageCheck :size="28" />
            <strong>No hay ventas publicas pendientes</strong>
            <span>Cuando llegue una venta desde una tienda publica aparecera aqui.</span>
          </div>
        </div>
      </section>

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
            <button class="vy-btn vy-btn-primary" type="button" :disabled="saving" @click="createCajaSale">
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
              </div>

              <div class="details-list">
                <div v-for="detalle in compra.detalles || []" :key="detalle.id">
                  <span>{{ detalle.producto?.nombre || "Producto" }} x{{ detalle.cantidad }}</span>
                  <b>Bs. {{ money(detalle.subtotal) }}</b>
                </div>
              </div>

              <button v-if="compra.comprobantePagoUrl" class="proof-link" type="button" @click="openProofModal(compra)">
                <FileText :size="15" /> Ver comprobante
              </button>

              <footer>
                <button v-if="compra.estadoCompra === 'PENDIENTE'" type="button" class="validate" @click="updateCompraEstado(compra, 'VALIDADA')">
                  <CheckCircle2 :size="15" /> Validar
                </button>
                <button type="button" class="deliver" @click="updateCompraEstado(compra, 'ENTREGADA')">
                  <PackageCheck :size="15" /> Entregar
                </button>
                <button type="button" class="reject" @click="updateCompraEstado(compra, 'RECHAZADA')">
                  <CircleX :size="15" /> Rechazar
                </button>
                <button
                  v-if="['VALIDADA', 'ENTREGADA'].includes(compra.estadoCompra)"
                  type="button"
                  class="receipt"
                  @click="openReceiptModal(compra)"
                >
                  <FileText :size="15" /> Comprobante
                </button>
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

    <Teleport to="body">
      <div v-if="saleModalOpen" class="sale-modal-backdrop" @click.self="closeSaleModal">
        <article class="sale-modal">
          <header>
            <div>
              <span class="vy-eyebrow">Ventanilla</span>
              <h2>Nueva venta</h2>
              <p>Genera una compra pendiente con pago en caja.</p>
            </div>
            <button type="button" aria-label="Cerrar" @click="closeSaleModal">
              <X :size="18" />
            </button>
          </header>

          <section class="sale-modal-body">
            <div class="sale-card">
              <label class="field">
                <span>Persona</span>
                <select ref="personaSelect" class="persona-select">
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
                  </span>
                  <b>Bs. {{ money(producto.precio) }}</b>
                  <Plus :size="16" />
                </button>
              </div>

              <div class="sale-items">
                <div v-for="item in ventaItems" :key="item.id" class="sale-item">
                  <div>
                    <strong>{{ item.nombre }}</strong>
                    <small>Bs. {{ money(item.precio) }} - PV {{ money(item.pv) }} - QP {{ money(item.qp) }} - CR {{ money(item.cr) }}</small>
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
                  <span>PV {{ money(salePv) }} - QP {{ money(saleQp) }} - CR {{ money(saleCr) }}</span>
                </div>
                <button class="vy-btn vy-btn-primary" type="button" :disabled="saving" @click="createCajaSale">
                  <ShoppingBag :size="16" /> Registrar venta
                </button>
              </footer>
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
                <div><small>Entregado por</small><strong>{{ receiptModalCompra.usuarioEntrega || "Pendiente" }}</strong></div>
                <div><small>Fecha y hora entrega</small><strong>{{ formatDateTime(receiptModalCompra.fechaEntrega) }}</strong></div>
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
    </Teleport>
  </div>
</template>

<style scoped>
.workspace { padding: 28px 32px 40px; min-width: 0; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 20px; }
.page-header h1 { margin-top: 8px; font-size: 30px; font-weight: 900; }
.page-header p { margin-top: 5px; color: var(--vy-ink-2); font-size: 14px; }
.page-header p strong { color: var(--vy-orange-deep); font-weight: 900; }
.header-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.period-filter { display: grid; gap: 6px; min-width: 260px; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.period-filter select { width: 100%; }
.sales-table-card { padding: 20px; }
.public-sales-card { margin-top: 18px; }
.sales-table-wrap { overflow-x: auto; }
.sales-table { width: 100%; border-collapse: collapse; min-width: 980px; font-size: 13px; }
.sales-table th { padding: 12px 10px; background: var(--vy-ink); color: #fff; text-align: left; font-size: 11px; font-weight: 900; text-transform: uppercase; white-space: nowrap; }
.sales-table td { padding: 13px 10px; border-bottom: 1px solid var(--vy-line-2); color: var(--vy-ink-2); vertical-align: middle; }
.sales-table td strong, .sales-table td small { display: block; }
.sales-table td strong { color: var(--vy-ink); font-weight: 900; }
.sales-table td small { margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; white-space: nowrap; }
.status-pill { display: inline-flex; align-items: center; min-height: 26px; padding: 0 9px; border-radius: 999px; background: #fff3df; color: var(--vy-orange-deep); font-size: 11px; font-weight: 900; }
.table-actions { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.table-actions button { width: 32px; height: 32px; border-radius: 9px; background: var(--vy-surface-2); color: var(--vy-ink-2); border: 1px solid var(--vy-line); display: inline-flex; align-items: center; justify-content: center; }
.table-actions button:hover { border-color: var(--vy-orange); color: var(--vy-orange-deep); background: #fffaf0; }
.shell-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(360px, 0.85fr); gap: 18px; align-items: start; }
.sale-card, .pending-card { padding: 20px; }
.card-title { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; }
.card-title h2 { font-size: 18px; font-weight: 900; }
.card-title p { margin-top: 3px; color: var(--vy-ink-3); font-size: 13px; font-weight: 700; }
.icon-box { width: 42px; height: 42px; border-radius: 14px; background: var(--vy-cream); color: var(--vy-orange-deep); display: inline-flex; align-items: center; justify-content: center; }
.field { display: block; margin-top: 14px; }
.field > span { display: block; margin-bottom: 7px; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
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
.selected-person { margin-top: 10px; padding: 10px 12px; border-radius: 12px; background: rgba(63, 143, 92, 0.1); color: var(--vy-success); font-size: 13px; font-weight: 800; }
.product-picker { margin-top: 10px; max-height: 300px; overflow: auto; }
.product-picker button span { flex: 1; min-width: 0; }
.product-picker button b { white-space: nowrap; font-size: 13px; }
.sale-items { display: grid; gap: 8px; margin-top: 16px; }
.sale-item { display: grid; grid-template-columns: minmax(0, 1fr) 76px 100px 34px; align-items: center; gap: 10px; padding: 10px 0; border-top: 1px solid var(--vy-line-2); }
.sale-item strong, .sale-item small { display: block; }
.sale-item strong { font-size: 13px; font-weight: 900; }
.sale-item small { margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; }
.sale-item input { width: 76px; padding: 8px; border: 1px solid var(--vy-line); border-radius: 10px; font-weight: 900; }
.sale-item b { text-align: right; font-size: 13px; }
.sale-item button { width: 34px; height: 34px; border-radius: 10px; background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); display: inline-flex; align-items: center; justify-content: center; }
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
.order-card footer { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }
.order-card footer button { min-height: 36px; padding: 0 11px; border-radius: 10px; display: inline-flex; align-items: center; gap: 7px; color: #fff; font-size: 12px; font-weight: 900; }
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
@media (max-width: 1120px) {
  .shell-grid { grid-template-columns: 1fr; }
}
@media (max-width: 720px) {
  .workspace { padding: 24px 20px 32px; }
  .page-header { align-items: stretch; flex-direction: column; }
  .header-actions { align-items: stretch; flex-direction: column; }
  .period-filter { min-width: 0; width: 100%; }
  .person-list, .sale-footer, .sale-item { grid-template-columns: 1fr; }
  .sale-item b { text-align: left; }
  .sale-item input, .sale-item button { width: 100%; }
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
