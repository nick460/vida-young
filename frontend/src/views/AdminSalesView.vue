<script setup>
import { computed, onMounted, ref } from "vue";
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

const personas = ref([]);
const productos = ref([]);
const compras = ref([]);
const loading = ref(false);
const saving = ref(false);
const error = ref("");
const personaQuery = ref("");
const productQuery = ref("");
const selectedPersonaId = ref("");
const ventaItems = ref([]);
const proofModalCompra = ref(null);
const receiptModalCompra = ref(null);

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
  const available = productos.value.filter((producto) => Number(producto.stockDisponible || 0) > 0);
  if (!text) return available;
  return available.filter((producto) => [
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

const saleTotal = computed(() =>
  ventaItems.value.reduce((sum, item) => sum + Number(item.precio || 0) * Number(item.cantidad || 0), 0)
);

const salePv = computed(() =>
  ventaItems.value.reduce((sum, item) => sum + Number(item.pv || 0) * Number(item.cantidad || 0), 0)
);

const saleQp = computed(() =>
  ventaItems.value.reduce((sum, item) => sum + Number(item.qp || 0) * Number(item.cantidad || 0), 0)
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
      <td class="num">PV ${money(detalle.pvUnitario)} / QP ${money(detalle.qpUnitario)}</td>
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
    doc.text(`Bs. ${money(detalle.subtotal)}`, 178, y + 6, { align: "right" });
    doc.setDrawColor(240, 234, 219);
    doc.line(margin, Math.max(nextY, y + 15), pageWidth - margin, Math.max(nextY, y + 15));
    y = Math.max(nextY + 4, y + 18);
  });

  y += 8;
  doc.setFillColor(255, 250, 240);
  doc.roundedRect(pageWidth - 86, y, 72, 31, 4, 4, "F");
  doc.setTextColor(31, 26, 20);
  doc.setFont("helvetica", "bold");
  doc.setFontSize(9);
  doc.text("Total PV", pageWidth - 80, y + 8);
  doc.text(money(compra.totalPv), pageWidth - 18, y + 8, { align: "right" });
  doc.text("Total QP", pageWidth - 80, y + 16);
  doc.text(money(compra.totalQp), pageWidth - 18, y + 16, { align: "right" });
  doc.setFillColor(242, 135, 5);
  doc.roundedRect(pageWidth - 86, y + 22, 72, 12, 4, 4, "F");
  doc.setTextColor(255, 255, 255);
  doc.text("Total", pageWidth - 80, y + 30);
  doc.text(`Bs. ${money(compra.subtotal)}`, pageWidth - 18, y + 30, { align: "right" });

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
    const stock = Number(producto.stockDisponible || 0);
    if (!stock || found.cantidad < stock) {
      found.cantidad += 1;
    }
    return;
  }

  ventaItems.value.push({
    id: producto.id,
    nombre: producto.nombre,
    sku: producto.sku,
    precio: Number(producto.precio || 0),
    pv: Number(producto.pv || 0),
    qp: Number(producto.qp || 0),
    stockDisponible: Number(producto.stockDisponible || 0),
    cantidad: 1
  });
}

function changeQuantity(item, value) {
  const next = Number(value || 1);
  const stock = Number(item.stockDisponible || 0);
  item.cantidad = Math.max(1, stock ? Math.min(stock, next) : next);
}

function removeItem(item) {
  ventaItems.value = ventaItems.value.filter((current) => Number(current.id) !== Number(item.id));
}

async function loadAll() {
  loading.value = true;
  error.value = "";

  try {
    const [personasData, productosData, comprasData] = await Promise.all([
      apiRequest("/api/personas"),
      apiRequest("/api/productos"),
      apiRequest("/api/compras")
    ]);
    personas.value = personasData;
    productos.value = productosData;
    compras.value = comprasData;
  } catch (exception) {
    error.value = exception.message || "No se pudo cargar la informacion de ventanilla.";
  } finally {
    loading.value = false;
  }
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
    cajaCode.value = generateCajaCode();
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

onMounted(loadAll);
</script>

<template>
  <div class="vy admin-sales-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Ventanilla</div>
          <h1>Ventas y validacion de pedidos</h1>
          <p>Registra compras presenciales, valida pagos pendientes y marca entregas en caja.</p>
        </div>
        <button class="vy-btn vy-btn-ghost" type="button" @click="loadAll">
          <RefreshCw :size="15" /> Actualizar
        </button>
      </header>

      <div v-if="error" class="error-box">{{ error }}</div>
      <div v-if="loading" class="loading-box">Cargando ventanilla...</div>

      <section class="shell-grid">
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
                <small>{{ producto.sku }} · Stock {{ producto.stockDisponible || 0 }}</small>
              </span>
              <b>Bs. {{ money(producto.precio) }}</b>
              <Plus :size="16" />
            </button>
          </div>

          <div class="sale-items">
            <div v-for="item in ventaItems" :key="item.id" class="sale-item">
              <div>
                <strong>{{ item.nombre }}</strong>
                <small>Bs. {{ money(item.precio) }} · PV {{ money(item.pv) }} · QP {{ money(item.qp) }}</small>
              </div>
              <input :value="item.cantidad" type="number" min="1" :max="item.stockDisponible || undefined" @input="changeQuantity(item, $event.target.value)" />
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
              <span>PV {{ money(salePv) }} · QP {{ money(saleQp) }}</span>
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
                      <td>PV {{ money(detalle.pvUnitario) }} / QP {{ money(detalle.qpUnitario) }}</td>
                      <td>Bs. {{ money(detalle.subtotal) }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div class="receipt-totals">
                <div><span>Total PV</span><strong>{{ money(receiptModalCompra.totalPv) }}</strong></div>
                <div><span>Total QP</span><strong>{{ money(receiptModalCompra.totalQp) }}</strong></div>
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
.proof-modal-backdrop { position: fixed; inset: 0; z-index: 120; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(31, 26, 20, 0.5); backdrop-filter: blur(6px); }
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
  .person-list, .sale-footer, .sale-item { grid-template-columns: 1fr; }
  .sale-item b { text-align: left; }
  .sale-item input, .sale-item button { width: 100%; }
  .proof-modal { padding: 16px; }
  .proof-modal header, .proof-modal footer { align-items: stretch; flex-direction: column; }
  .proof-modal footer .vy-btn { width: 100%; }
  .receipt-modal { padding: 16px; }
  .receipt-modal > header, .receipt-modal > footer, .receipt-sheet-header { align-items: stretch; flex-direction: column; }
  .receipt-modal > footer .vy-btn { width: 100%; }
  .receipt-preview { padding: 10px; }
  .receipt-sheet { padding: 18px; }
  .receipt-sheet-header > div:last-child { text-align: left; }
  .receipt-info-grid { grid-template-columns: 1fr; }
}
</style>
