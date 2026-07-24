<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import $ from "jquery";
import select2 from "select2";
import "select2/dist/css/select2.css";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { ArrowDownToLine, CalendarClock, CheckCircle2, Eye, PackageCheck, Printer, RefreshCw, Search, WalletCards, X } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import logoFull from "../assets/logoFull.png";

select2($);

const loading = ref(false);
const processing = ref(false);
const error = ref("");
const saldos = ref([]);
const retirosProcesados = ref([]);
const productos = ref([]);
const periodos = ref([]);
const selectedPeriodoId = ref("");
const periodoSelect = ref(null);
const productoSelect = ref(null);
const selectedSaldo = ref(null);
const selectedWallet = ref(null);
const selectedRetiroDetalle = ref(null);
const selectedProductoId = ref("");
const retiroModalOpen = ref(false);
const searchTerm = ref("");
const page = ref(1);
const pageSize = ref(10);
let previousBodyOverflow = "";
let previousHtmlOverflow = "";
const retiroForm = ref({
  montoDinero: 0,
  productos: [],
  observacion: ""
});

const selectedPeriodo = computed(() =>
  periodos.value.find((periodo) => Number(periodo.id) === Number(selectedPeriodoId.value))
);
const isPeriodoActivo = computed(() => selectedPeriodo.value?.estadoPeriodo === "ACTIVO");
const canCloseMonth = computed(() => isPeriodoActivo.value && !loading.value && saldos.value.length === 0);
const filteredSaldos = computed(() => {
  const term = searchTerm.value.trim().toLowerCase();
  if (!term) return saldos.value;
  return saldos.value.filter((saldo) =>
    `${saldo.nombres || ""} ${saldo.apellidos || ""} ${saldo.documento || ""}`.toLowerCase().includes(term)
  );
});
const retiroRows = computed(() =>
  retirosProcesados.value.map((retiro) => ({
    ...retiro,
    rowType: "RETIRADO",
    rowKey: `retiro-${retiro.id}`,
    nombres: retiro.nombres,
    apellidos: retiro.apellidos,
    documento: retiro.documento,
    saldoDinero: retiro.montoDinero,
    efectivoRecompensasDisponible: 0,
    saldoProductos: retiro.montoProductos,
    saldoPv: 0,
    saldoQp: 0,
    saldoCr: 0,
    origen: "RETIRO_PROCESADO"
  }))
);
const allRows = computed(() => [
  ...filteredSaldos.value.map((saldo) => ({
    ...saldo,
    rowType: "PENDIENTE",
    rowKey: `saldo-${saldo.personaId}-${saldo.origen}`
  })),
  ...retiroRows.value.filter((retiro) => {
    const term = searchTerm.value.trim().toLowerCase();
    if (!term) return true;
    return `${retiro.nombres || ""} ${retiro.apellidos || ""} ${retiro.documento || ""}`.toLowerCase().includes(term);
  })
]);
const totalPages = computed(() => Math.max(1, Math.ceil(allRows.value.length / Number(pageSize.value || 10))));
const paginatedRows = computed(() => {
  const size = Number(pageSize.value || 10);
  const start = (page.value - 1) * size;
  return allRows.value.slice(start, start + size);
});
const totals = computed(() =>
  filteredSaldos.value.reduce(
    (acc, saldo) => ({
      dinero: acc.dinero + Number(saldo.saldoDinero || 0) + Number(saldo.efectivoRecompensasDisponible || 0),
      productos: acc.productos + Number(saldo.saldoProductos || 0),
      pv: acc.pv + Number(saldo.saldoPv || 0),
      qp: acc.qp + Number(saldo.saldoQp || 0)
    }),
    { dinero: 0, productos: 0, pv: 0, qp: 0 }
  )
);
const billeteraSeleccionada = computed(() => selectedWallet.value?.billetera || {});
const efectivoDisponibleRetiro = computed(() =>
  Number(billeteraSeleccionada.value.saldoDinero || 0) + Number(selectedWallet.value?.efectivoRecompensasDisponible || 0)
);
const detalleEfectivoMensual = computed(() => selectedWallet.value?.detalleEfectivoMensual || []);
const productosDisponiblesRetiro = computed(() => Number(selectedWallet.value?.productosRecompensasDisponible || 0));
const selectedProducto = computed(() =>
  productos.value.find((producto) => Number(producto.id) === Number(selectedProductoId.value))
);
const retiroProductosTotal = computed(() =>
  retiroForm.value.productos.reduce((sum, item) => sum + Number(item.precio || 0) * Number(item.cantidad || 0), 0)
);

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function formatDateTime(value) {
  const date = value ? new Date(value) : new Date();
  return date.toLocaleString("es-BO", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function currentUserName() {
  try {
    const usuario = JSON.parse(localStorage.getItem("vy_usuario") || "null");
    const personaName = [usuario?.persona?.nombres, usuario?.persona?.apellidos].filter(Boolean).join(" ").trim();
    return personaName || usuario?.username || "Usuario del sistema";
  } catch {
    return "Usuario del sistema";
  }
}

function retiroReceipt(retiro) {
  return {
    persona: `${retiro.nombres || ""} ${retiro.apellidos || ""}`.trim(),
    documento: retiro.documento || "Sin documento",
    periodo: `${retiro.periodoNombre || selectedPeriodo.value?.nombre || "Periodo"}${retiro.gestionAnio ? ` - Gestion ${retiro.gestionAnio}` : ""}`,
    total: Number(retiro.montoDinero || 0),
    retiroId: retiro.id ? `#${retiro.id}` : "procesado",
    fechaRetiro: formatDateTime(retiro.fechaRetiro),
    fechaImpresion: formatDateTime(),
    impresoPor: currentUserName(),
    detalles: [
      {
        titulo: "Saldo directo de billetera",
        descripcion: "Dinero retirado desde el saldo directo",
        monto: Number(retiro.montoDesdeBilletera || 0)
      },
      {
        titulo: "Efectivo de recompensas",
        descripcion: "Monto retirado desde recompensas mensuales",
        monto: Number(retiro.montoDesdeRecompensas || 0)
      },
      ...(retiro.detalles || []).map((item) => ({
        titulo: item.productoNombre || "Producto",
        descripcion: `${item.cantidad || 0} x Bs. ${money(item.precioProveedor)}${item.productoSku ? ` - ${item.productoSku}` : ""}`,
        monto: Number(item.subtotal || 0)
      }))
    ].filter((item) => Number(item.monto || 0) > 0)
  };
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function buildWithdrawalReceiptHtml(receipt) {
  const detalles = receipt.detalles.length
    ? receipt.detalles.map((item) => `
      <div class="detail-row">
        <span>
          <strong>${escapeHtml(item.titulo)}</strong>
          <small>${escapeHtml(item.descripcion)}</small>
        </span>
        <b>Bs. ${escapeHtml(money(item.monto))}</b>
      </div>
    `).join("")
    : `<div class="detail-row"><span><strong>Sin detalle adicional</strong></span><b>Bs. 0,00</b></div>`;

  return `<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8">
  <title>Comprobante de retiro</title>
  <style>
    @page { size: 8cm auto; margin: 0.5cm; }
    * { box-sizing: border-box; }
    body {
      width: 7cm;
      margin: 0;
      color: #000;
      background: #fff;
      font-family: Arial, Helvetica, sans-serif;
      font-size: 10pt;
      line-height: 1.25;
    }
    main { width: 7cm; }
    .logo {
      display: block;
      width: 4.2cm;
      max-height: 1.4cm;
      object-fit: contain;
      margin: 0 auto 0.18cm;
    }
    h1 {
      margin: 0 0 0.16cm;
      text-align: center;
      font-size: 11pt;
      font-weight: 700;
      text-transform: uppercase;
    }
    .receipt-id {
      margin: 0 0 0.22cm;
      text-align: center;
      font-size: 8.5pt;
      font-weight: 700;
    }
    .row {
      display: flex;
      justify-content: space-between;
      gap: 0.25cm;
      padding: 0.08cm 0;
      border-bottom: 1px dashed #000;
    }
    .label { font-weight: 700; }
    .value { text-align: right; overflow-wrap: anywhere; }
    .section-title {
      margin: 0.22cm 0 0.08cm;
      padding-top: 0.12cm;
      border-top: 1px solid #000;
      text-align: center;
      font-size: 9pt;
      font-weight: 700;
      text-transform: uppercase;
    }
    .detail-row {
      display: flex;
      justify-content: space-between;
      gap: 0.18cm;
      padding: 0.08cm 0;
      border-bottom: 1px dotted #000;
    }
    .detail-row span { min-width: 0; }
    .detail-row strong,
    .detail-row small {
      display: block;
      overflow-wrap: anywhere;
    }
    .detail-row small { margin-top: 0.02cm; font-size: 8pt; }
    .detail-row b { white-space: nowrap; }
    .total {
      margin-top: 0.25cm;
      padding-top: 0.18cm;
      border-top: 2px solid #000;
      font-size: 12pt;
      font-weight: 700;
    }
    .signature {
      margin-top: 0.55cm;
      padding-top: 0.16cm;
      border-top: 1px solid #000;
      text-align: center;
      font-size: 8pt;
    }
  </style>
</head>
<body>
  <main>
    <img class="logo" src="${logoFull}" alt="Vida Young">
    <h1>Comprobante de retiro</h1>
    <p class="receipt-id">Retiro ${escapeHtml(receipt.retiroId)}</p>
    <div class="row"><span class="label">Persona</span><span class="value">${escapeHtml(receipt.persona)}</span></div>
    <div class="row"><span class="label">Documento</span><span class="value">${escapeHtml(receipt.documento)}</span></div>
    <div class="row"><span class="label">Mes</span><span class="value">${escapeHtml(receipt.periodo)}</span></div>
    <div class="row"><span class="label">Fecha retiro</span><span class="value">${escapeHtml(receipt.fechaRetiro)}</span></div>
    <div class="row"><span class="label">Impresion</span><span class="value">${escapeHtml(receipt.fechaImpresion)}</span></div>
    <div class="row"><span class="label">Impreso por</span><span class="value">${escapeHtml(receipt.impresoPor)}</span></div>
    <div class="section-title">Detalle del retiro</div>
    ${detalles}
    <div class="row total"><span>Total retiro</span><span>Bs. ${escapeHtml(money(receipt.total))}</span></div>
    <div class="signature">Firma / recibido conforme</div>
  </main>
</body>
</html>`;
}

function printWithdrawalReceipt(receipt) {
  const iframe = document.createElement("iframe");
  iframe.setAttribute("title", "Comprobante de retiro");
  iframe.style.position = "fixed";
  iframe.style.right = "0";
  iframe.style.bottom = "0";
  iframe.style.width = "0";
  iframe.style.height = "0";
  iframe.style.border = "0";
  iframe.style.opacity = "0";
  document.body.appendChild(iframe);

  const printDocument = iframe.contentWindow?.document;
  if (!printDocument || !iframe.contentWindow) {
    iframe.remove();
    Swal.fire({
      title: "No se pudo imprimir",
      text: "El navegador bloqueo la preparacion del comprobante.",
      icon: "error",
      confirmButtonColor: "#F28705"
    });
    return;
  }

  printDocument.open();
  printDocument.write(buildWithdrawalReceiptHtml(receipt));
  printDocument.close();

  iframe.contentWindow.onafterprint = () => {
    iframe.remove();
  };
  iframe.contentWindow.setTimeout(() => {
    iframe.contentWindow.focus();
    iframe.contentWindow.print();
    iframe.contentWindow.setTimeout(() => iframe.remove(), 1200);
  }, 400);
}

function destroyPeriodoSelect2() {
  if (!periodoSelect.value) return;
  const element = $(periodoSelect.value);
  if (element.hasClass("select2-hidden-accessible")) {
    element.off("change.retiros-periodo");
    element.select2("destroy");
  }
}

function destroyProductoSelect2() {
  if (!productoSelect.value) return;
  const element = $(productoSelect.value);
  if (element.hasClass("select2-hidden-accessible")) {
    element.off("change.retiros-producto");
    element.select2("destroy");
  }
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
      dropdownParent: $(".wallet-withdrawals-view"),
      language: {
        noResults: () => "Sin resultados",
        searching: () => "Buscando..."
      }
    })
    .val(selectedPeriodoId.value || null)
    .trigger("change.select2");

  element.on("change.retiros-periodo", async () => {
    selectedPeriodoId.value = element.val() || "";
    await loadSaldos();
  });
}

async function initProductoSelect2() {
  if (!retiroModalOpen.value || !selectedWallet.value) return;

  await nextTick();
  if (!productoSelect.value) return;

  destroyProductoSelect2();
  const element = $(productoSelect.value);
  element
    .select2({
      width: "100%",
      placeholder: "Selecciona producto",
      allowClear: true,
      dropdownParent: $(".retiro-modal"),
      language: {
        noResults: () => "Sin resultados",
        searching: () => "Buscando..."
      }
    })
    .val(selectedProductoId.value || null)
    .trigger("change.select2");

  element.on("change.retiros-producto", () => {
    selectedProductoId.value = element.val() || "";
  });
}

async function loadPeriodoOptions() {
  const [activePeriodo, gestionesData] = await Promise.all([
    apiRequest("/api/gestiones/periodos/activo"),
    apiRequest("/api/gestiones")
  ]);
  const periodosPorGestion = await Promise.all(
    (Array.isArray(gestionesData) ? gestionesData : []).map(async (gestion) => {
      const items = await apiRequest(`/api/gestiones/${gestion.id}/periodos`);
      return items.map((periodo) => ({ ...periodo, gestion: periodo.gestion || gestion }));
    })
  );

  periodos.value = periodosPorGestion
    .flat()
    .sort((left, right) => {
      const leftYear = Number(left.gestion?.anio || 0);
      const rightYear = Number(right.gestion?.anio || 0);
      if (leftYear !== rightYear) return rightYear - leftYear;
      return Number(right.mes || 0) - Number(left.mes || 0);
    });

  if (!selectedPeriodoId.value) {
    selectedPeriodoId.value = String(activePeriodo?.id || periodos.value[0]?.id || "");
  }
}

async function loadSaldos() {
  loading.value = true;
  error.value = "";

  try {
    if (!periodos.value.length) {
      await loadPeriodoOptions();
      await initPeriodoSelect2();
    }
    const query = selectedPeriodoId.value ? `?periodoId=${selectedPeriodoId.value}` : "";
    const [data, retirosData] = await Promise.all([
      apiRequest(`/api/billeteras/saldos${query}`),
      apiRequest(`/api/billeteras/retiros${query}`)
    ]);
    saldos.value = Array.isArray(data) ? data : [];
    retirosProcesados.value = Array.isArray(retirosData) ? retirosData : [];
    page.value = Math.min(page.value, totalPages.value);
  } catch (exception) {
    error.value = exception.message || "No se pudieron cargar los saldos de billeteras.";
  } finally {
    loading.value = false;
  }
}

function openRetiroDetalle(retiro) {
  selectedRetiroDetalle.value = retiro;
}

function closeRetiroDetalle() {
  selectedRetiroDetalle.value = null;
}

function imprimirRetiroProcesado(retiro) {
  printWithdrawalReceipt(retiroReceipt(retiro));
}

async function loadProductos() {
  try {
    const data = await apiRequest("/api/productos");
    productos.value = Array.isArray(data) ? data : [];
  } catch {
    productos.value = [];
  }
}

async function loadSelectedWallet() {
  if (!selectedSaldo.value?.personaId) return;
  selectedWallet.value = await apiRequest(`/api/billeteras/persona/${selectedSaldo.value.personaId}`);
}

async function openRetiroModal(saldo) {
  if (!isPeriodoActivo.value) return;
  selectedSaldo.value = saldo;
  selectedWallet.value = null;
  selectedProductoId.value = "";
  retiroForm.value = { montoDinero: 0, productos: [], observacion: "" };
  retiroModalOpen.value = true;
  lockPageScroll();
  await loadSelectedWallet();
  setMaxRetiro();
  await initProductoSelect2();
}

function closeRetiroModal() {
  if (processing.value) return;
  destroyProductoSelect2();
  retiroModalOpen.value = false;
  selectedSaldo.value = null;
  selectedWallet.value = null;
  selectedProductoId.value = "";
  retiroForm.value = { montoDinero: 0, productos: [], observacion: "" };
  unlockPageScroll();
}

function lockPageScroll() {
  previousBodyOverflow = document.body.style.overflow;
  previousHtmlOverflow = document.documentElement.style.overflow;
  document.body.style.overflow = "hidden";
  document.documentElement.style.overflow = "hidden";
}

function unlockPageScroll() {
  document.body.style.overflow = previousBodyOverflow;
  document.documentElement.style.overflow = previousHtmlOverflow;
}

function setMaxRetiro() {
  retiroForm.value.montoDinero = efectivoDisponibleRetiro.value;
}

function addRetiroProducto() {
  if (!selectedProducto.value) return;
  const existente = retiroForm.value.productos.find((item) => Number(item.productoId) === Number(selectedProducto.value.id));
  if (existente) {
    existente.cantidad += 1;
  } else {
    retiroForm.value.productos.push({
      productoId: selectedProducto.value.id,
      nombre: selectedProducto.value.nombre,
      sku: selectedProducto.value.sku,
      precio: Number(selectedProducto.value.precio || 0),
      cantidad: 1
    });
  }
  selectedProductoId.value = "";
  initProductoSelect2();
}

function removeRetiroProducto(item) {
  retiroForm.value.productos = retiroForm.value.productos.filter((current) => Number(current.productoId) !== Number(item.productoId));
}

function updateRetiroProductoCantidad(item, value) {
  item.cantidad = Math.max(1, Number(value || 1));
}

async function registrarRetiro() {
  if (!selectedSaldo.value?.personaId || processing.value) return;

  setMaxRetiro();
  const totalRetiro = Number(efectivoDisponibleRetiro.value || 0);
  retiroForm.value.montoDinero = totalRetiro;
  const receipt = {
    persona: `${selectedSaldo.value.nombres || ""} ${selectedSaldo.value.apellidos || ""}`.trim(),
    documento: selectedSaldo.value.documento || "Sin documento",
    periodo: selectedPeriodo.value?.nombre || "Periodo activo",
    total: totalRetiro,
    retiroId: "procesado",
    fechaRetiro: formatDateTime(),
    fechaImpresion: formatDateTime(),
    impresoPor: currentUserName(),
    detalles: [
      {
        titulo: "Saldo directo de billetera",
        descripcion: "Dinero acreditado previamente a la billetera",
        monto: Number(billeteraSeleccionada.value.saldoDinero || 0)
      },
      ...detalleEfectivoMensual.value.map((item) => ({
        titulo: `Nivel ${item.nivelGenerado} - ${item.referidoNombre || "Referido"}`,
        descripcion: `${item.planIngreso || "Plan"}${item.referidoDocumento ? ` - CI ${item.referidoDocumento}` : ""}`,
        monto: Number(item.montoDisponible || 0)
      }))
    ].filter((item) => Number(item.monto || 0) > 0)
  };

  const result = await Swal.fire({
    title: "Registrar retiro",
    text: `Se registrara el retiro total de Bs. ${money(receipt.total)} para ${receipt.persona}.`,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Si, registrar",
    cancelButtonText: "Cancelar",
    confirmButtonColor: "#F28705"
  });

  if (!result.isConfirmed) return;

  processing.value = true;
  error.value = "";
  try {
    const retiroProcesado = await apiRequest(`/api/billeteras/persona/${selectedSaldo.value.personaId}/retiros`, {
      method: "POST",
      body: JSON.stringify({
        montoDinero: totalRetiro,
        montoProductos: 0,
        productos: [],
        observacion: retiroForm.value.observacion || null
      })
    });
    receipt.retiroId = retiroProcesado?.id ? `#${retiroProcesado.id}` : "procesado";
    receipt.fechaRetiro = formatDateTime(retiroProcesado?.fechaRetiro);
    receipt.fechaImpresion = formatDateTime();
    receipt.impresoPor = currentUserName();
    await Swal.fire({
      title: "Retiro procesado",
      text: "Los saldos de la persona fueron actualizados.",
      icon: "success",
      confirmButtonText: "Cerrar",
      confirmButtonColor: "#F28705"
    });
    closeRetiroModal();
    await loadSaldos();
  } catch (exception) {
    error.value = exception.message || "No se pudo registrar el retiro.";
  } finally {
    processing.value = false;
  }
}

async function cerrarPeriodoMensual() {
  if (!canCloseMonth.value || processing.value) return;

  const result = await Swal.fire({
    title: "Cerrar mes",
    text: `Se marcara ${selectedPeriodo.value?.nombre || "el periodo activo"} como CERRADO. Luego no podra activarse nuevamente.`,
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Cerrar mes",
    cancelButtonText: "Cancelar",
    confirmButtonColor: "#C4452A"
  });

  if (!result.isConfirmed) return;

  processing.value = true;
  error.value = "";
  try {
    await apiRequest("/api/billeteras/cierres-mensuales/cerrar-periodo", { method: "POST" });
    await Swal.fire("Mes cerrado", "El periodo fue cerrado correctamente.", "success");
    selectedPeriodoId.value = "";
    periodos.value = [];
    await loadSaldos();
  } catch (exception) {
    error.value = exception.message || "No se pudo cerrar el periodo.";
  } finally {
    processing.value = false;
  }
}

function previousPage() {
  page.value = Math.max(1, page.value - 1);
}

function nextPage() {
  page.value = Math.min(totalPages.value, page.value + 1);
}

watch(periodos, () => {
  initPeriodoSelect2();
});

watch(selectedPeriodoId, (value) => {
  if (!periodoSelect.value) return;
  const element = $(periodoSelect.value);
  if (element.hasClass("select2-hidden-accessible") && element.val() !== value) {
    element.val(value || null).trigger("change.select2");
  }
});

watch(productos, () => {
  initProductoSelect2();
});

watch(selectedWallet, () => {
  initProductoSelect2();
});

watch(selectedProductoId, (value) => {
  if (!productoSelect.value) return;
  const element = $(productoSelect.value);
  if (element.hasClass("select2-hidden-accessible") && element.val() !== value) {
    element.val(value || null).trigger("change.select2");
  }
});

watch(searchTerm, () => {
  page.value = 1;
});

watch(pageSize, () => {
  page.value = 1;
});

onMounted(() => {
  loadSaldos();
  loadProductos();
});

onBeforeUnmount(() => {
  destroyPeriodoSelect2();
  destroyProductoSelect2();
  unlockPageScroll();
});
</script>

<template>
  <div class="vy wallet-withdrawals-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Administracion</div>
          <h1>Retiros de billeteras</h1>
          <p>
            Personas con saldos disponibles para consulta y retiros.
            <strong v-if="selectedPeriodo">Mostrando {{ selectedPeriodo.nombre }}.</strong>
          </p>
        </div>
        <div class="header-actions">
          <label class="period-filter">
            <span>Mes</span>
            <select ref="periodoSelect" v-model="selectedPeriodoId">
              <option value="" disabled>Selecciona un mes</option>
              <option v-for="periodo in periodos" :key="periodo.id" :value="periodo.id">
                {{ periodo.nombre }} - Gestion {{ periodo.gestion?.anio || "" }}
              </option>
            </select>
          </label>
          <button class="refresh-action" type="button" :disabled="loading" @click="loadSaldos">
            <RefreshCw :size="16" /> Actualizar
          </button>
          <button class="month-close-action" type="button" :disabled="!canCloseMonth || processing" @click="cerrarPeriodoMensual">
            <CheckCircle2 :size="16" /> Cierre de mes
          </button>
        </div>
      </header>

      <div v-if="error" class="error-box">{{ error }}</div>
      <div v-if="!isPeriodoActivo" class="info-box">
        El mes seleccionado no esta activo. Puedes consultar saldos cerrados, pero los retiros solo se registran en el mes activo.
      </div>
      <div v-else-if="canCloseMonth" class="success-box">
        Todas las personas con saldo mensual fueron cerradas. Ya puedes cerrar el mes de forma definitiva.
      </div>

      <section class="summary-grid">
        <article class="balance-card">
          <div class="balance-icon"><WalletCards :size="26" /></div>
          <span>Efectivo disponible</span>
          <strong>Bs. {{ money(totals.dinero) }}</strong>
          <p>{{ filteredSaldos.length }} personas con saldo.</p>
        </article>
        <article class="metric-card">
          <span class="metric-icon"><PackageCheck :size="20" /></span>
          <div>
            <small>Productos canjeables</small>
            <strong>Bs. {{ money(totals.productos) }}</strong>
          </div>
        </article>
        <article class="metric-card">
          <span class="metric-icon"><CalendarClock :size="20" /></span>
          <div>
            <small>PV del mes</small>
            <strong>{{ money(totals.pv) }}</strong>
          </div>
        </article>
        <article class="metric-card">
          <span class="metric-icon"><CalendarClock :size="20" /></span>
          <div>
            <small>QP del mes</small>
            <strong>{{ money(totals.qp) }}</strong>
          </div>
        </article>
      </section>

      <section class="vy-card table-card">
        <header class="section-header">
          <div>
            <h2>Retiros por persona</h2>
            <p>Consulta pendientes y retiros procesados del mes seleccionado.</p>
          </div>
          <label class="search-box">
            <Search :size="16" />
            <input v-model.trim="searchTerm" type="search" placeholder="Buscar persona o documento" />
          </label>
        </header>

        <div v-if="loading" class="loading-box">Cargando saldos...</div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Persona</th>
                <th>Documento</th>
                <th>Efectivo</th>
                <th>Productos</th>
                <th>PV</th>
                <th>QP</th>
                <th>CR</th>
                <th>Estado</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="saldo in paginatedRows" :key="saldo.rowKey">
                <td>
                  <strong>{{ saldo.nombres }} {{ saldo.apellidos }}</strong>
                  <small>
                    <template v-if="saldo.rowType === 'RETIRADO'">Retiro #{{ saldo.id }} - {{ formatDateTime(saldo.fechaRetiro) }}</template>
                    <template v-else>{{ saldo.origen === "CIERRE_MENSUAL" ? "Cierre mensual" : "Billetera actual" }}</template>
                  </small>
                </td>
                <td>{{ saldo.documento || "Sin documento" }}</td>
                <td>Bs. {{ money(Number(saldo.saldoDinero || 0) + Number(saldo.efectivoRecompensasDisponible || 0)) }}</td>
                <td>Bs. {{ money(saldo.saldoProductos) }}</td>
                <td>{{ money(saldo.saldoPv) }}</td>
                <td>{{ money(saldo.saldoQp) }}</td>
                <td>{{ money(saldo.saldoCr) }}</td>
                <td>
                  <span class="withdrawal-status" :class="saldo.rowType === 'RETIRADO' ? 'processed' : 'pending'">
                    {{ saldo.rowType === "RETIRADO" ? "Retirado" : "Pendiente" }}
                  </span>
                </td>
                <td class="actions-cell">
                  <button v-if="saldo.rowType === 'PENDIENTE'" class="row-withdraw-button" type="button" :disabled="!isPeriodoActivo" @click="openRetiroModal(saldo)">
                    <ArrowDownToLine :size="15" /> Retiro
                  </button>
                  <div v-else class="processed-actions">
                    <button class="detail-button" type="button" @click="openRetiroDetalle(saldo)">
                      <Eye :size="15" /> Detalle
                    </button>
                    <button class="print-button" type="button" @click="imprimirRetiroProcesado(saldo)">
                      <Printer :size="15" /> Imprimir
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="!allRows.length && !loading">
                <td colspan="9">No hay pendientes ni retiros procesados para el mes seleccionado.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <footer v-if="allRows.length" class="pagination-bar">
          <div>
            <span>Pagina {{ page }} de {{ totalPages }}</span>
            <select v-model.number="pageSize">
              <option :value="10">10 por pagina</option>
              <option :value="25">25 por pagina</option>
              <option :value="50">50 por pagina</option>
            </select>
          </div>
          <div class="pagination-actions">
            <button type="button" :disabled="page === 1" @click="previousPage">Anterior</button>
            <button type="button" :disabled="page === totalPages" @click="nextPage">Siguiente</button>
          </div>
        </footer>
      </section>
    </main>

    <Teleport to="body">
      <div v-if="selectedRetiroDetalle" class="retiro-backdrop">
        <article class="retiro-modal">
          <header>
            <div>
              <span class="vy-eyebrow">Retiro procesado</span>
              <h2>Detalle de retiro #{{ selectedRetiroDetalle.id }}</h2>
              <p>{{ selectedRetiroDetalle.nombres }} {{ selectedRetiroDetalle.apellidos }} - {{ selectedRetiroDetalle.periodoNombre }}</p>
            </div>
            <button type="button" aria-label="Cerrar" @click="closeRetiroDetalle">
              <X :size="18" />
            </button>
          </header>

          <section class="retiro-body person-wallet-detail">
            <div class="selected-person">
              <small>Persona</small>
              <strong>{{ selectedRetiroDetalle.nombres }} {{ selectedRetiroDetalle.apellidos }}</strong>
              <span>{{ selectedRetiroDetalle.documento || "Sin documento" }}</span>
            </div>
            <div class="wallet-values">
              <div>
                <small>Total retirado</small>
                <strong>Bs. {{ money(selectedRetiroDetalle.montoDinero) }}</strong>
                <span>{{ formatDateTime(selectedRetiroDetalle.fechaRetiro) }}</span>
              </div>
              <div>
                <small>Periodo</small>
                <strong>{{ selectedRetiroDetalle.periodoNombre }}</strong>
                <span>Gestion {{ selectedRetiroDetalle.gestionAnio || "" }}</span>
              </div>
            </div>
            <section class="cash-breakdown">
              <header>
                <div>
                  <small>Detalle del retiro</small>
                  <strong>Origen del efectivo pagado</strong>
                </div>
                <b>Bs. {{ money(selectedRetiroDetalle.montoDinero) }}</b>
              </header>
              <div class="breakdown-row">
                <span>
                  <strong>Saldo directo de billetera</strong>
                  <small>Dinero retirado desde el saldo directo.</small>
                </span>
                <b>Bs. {{ money(selectedRetiroDetalle.montoDesdeBilletera) }}</b>
              </div>
              <div class="breakdown-row">
                <span>
                  <strong>Efectivo de recompensas</strong>
                  <small>Monto retirado desde recompensas mensuales.</small>
                </span>
                <b>Bs. {{ money(selectedRetiroDetalle.montoDesdeRecompensas) }}</b>
              </div>
            </section>
            <div v-if="selectedRetiroDetalle.observacion" class="selected-person">
              <small>Observacion</small>
              <strong>{{ selectedRetiroDetalle.observacion }}</strong>
            </div>
          </section>

          <footer>
            <button class="vy-btn vy-btn-ghost" type="button" @click="closeRetiroDetalle">Cerrar</button>
            <button class="vy-btn vy-btn-primary" type="button" @click="imprimirRetiroProcesado(selectedRetiroDetalle)">
              <Printer :size="16" /> Imprimir comprobante
            </button>
          </footer>
        </article>
      </div>

      <div v-if="retiroModalOpen" class="retiro-backdrop">
        <article class="retiro-modal">
          <header>
            <div>
              <span class="vy-eyebrow">Retiros</span>
              <h2>Detalle y retiro de saldos</h2>
              <p>Retiro para {{ selectedSaldo?.nombres }} {{ selectedSaldo?.apellidos }}.</p>
            </div>
            <button type="button" aria-label="Cerrar" @click="closeRetiroModal">
              <X :size="18" />
            </button>
          </header>

          <section class="retiro-body">
            <div v-if="!selectedWallet" class="loading-box">Cargando saldos de la persona...</div>
            <section v-else class="person-wallet-detail">
              <div class="selected-person">
                <small>Persona seleccionada</small>
                <strong>{{ selectedSaldo?.nombres }} {{ selectedSaldo?.apellidos }}</strong>
                <span>{{ selectedSaldo?.documento || "Sin documento" }}</span>
              </div>
              <div class="wallet-values">
                <div>
                  <small>Efectivo mensual disponible</small>
                  <strong>Bs. {{ money(efectivoDisponibleRetiro) }}</strong>
                  <span>Billetera Bs. {{ money(billeteraSeleccionada.saldoDinero) }} + recompensas Bs. {{ money(selectedWallet?.efectivoRecompensasDisponible) }}</span>
                </div>
                <div>
                  <small>Productos nivel 1</small>
                  <strong>Bs. {{ money(productosDisponiblesRetiro) }}</strong>
                  <span>Se retiran desde el modulo de nivel 1.</span>
                </div>
              </div>

              <section class="cash-breakdown">
                <header>
                  <div>
                    <small>Origen del efectivo</small>
                    <strong>Detalle del monto a pagar</strong>
                  </div>
                  <b>Bs. {{ money(efectivoDisponibleRetiro) }}</b>
                </header>

                <div class="breakdown-row">
                  <span>
                    <strong>Saldo directo de billetera</strong>
                    <small>Dinero acreditado previamente a la billetera.</small>
                  </span>
                  <b>Bs. {{ money(billeteraSeleccionada.saldoDinero) }}</b>
                </div>

                <div v-for="item in detalleEfectivoMensual" :key="item.recompensaId" class="breakdown-row">
                  <span>
                    <strong>Nivel {{ item.nivelGenerado }} - {{ item.referidoNombre || "Referido" }}</strong>
                    <small>{{ item.planIngreso || "Plan" }}<template v-if="item.referidoDocumento"> - CI {{ item.referidoDocumento }}</template></small>
                  </span>
                  <b>Bs. {{ money(item.montoDisponible) }}</b>
                </div>

                <div v-if="!detalleEfectivoMensual.length" class="breakdown-empty">
                  No hay recompensas mensuales pendientes; el retiro saldra solo del saldo directo.
                </div>
              </section>

              <div class="withdraw-grid">
                <div class="field">
                  <span>Retiro total obligatorio</span>
                  <div class="readonly-total">Bs. {{ money(efectivoDisponibleRetiro) }}</div>
                </div>
                <div class="field">
                  <span>Tipo de retiro</span>
                  <div class="readonly-total">Cierre personal</div>
                </div>
              </div>

              <label class="field">
                <span>Observacion</span>
                <textarea v-model.trim="retiroForm.observacion" rows="3" placeholder="Detalle interno del retiro"></textarea>
              </label>
            </section>
          </section>

          <footer>
            <button class="vy-btn vy-btn-ghost" type="button" :disabled="processing" @click="closeRetiroModal">Cancelar</button>
            <button class="vy-btn vy-btn-primary" type="button" :disabled="processing || !selectedWallet" @click="registrarRetiro">
              {{ processing ? "Procesando..." : "Registrar retiro" }}
            </button>
          </footer>
        </article>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.wallet-withdrawals-view { min-height: 100vh; background: var(--vy-bg); color: var(--vy-ink); }
.workspace { width: min(1440px, 100%); margin: 0 auto; padding: 26px clamp(14px, 3vw, 34px) 42px; display: grid; gap: 18px; }
.page-header, .section-header, .pagination-bar { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.page-header h1 { margin: 4px 0; font-size: clamp(26px, 3vw, 38px); font-weight: 900; }
.page-header p, .section-header p { color: var(--vy-ink-3); font-weight: 800; line-height: 1.45; }
.header-actions { display: flex; align-items: flex-end; gap: 10px; flex-wrap: wrap; }
.period-filter, .field { display: grid; gap: 7px; }
.period-filter span, .field span { color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.period-filter { min-width: 260px; }
.refresh-action { min-height: 42px; padding: 0 16px; border: 1px solid rgba(242, 135, 5, 0.32); border-radius: 12px; background: #fff7e8; color: #1f1a14; display: inline-flex; align-items: center; justify-content: center; gap: 8px; font-size: 14px; font-weight: 950; box-shadow: 0 10px 24px rgba(242, 135, 5, 0.14); }
.refresh-action:hover:not(:disabled) { background: #f28705; color: #1f1a14; transform: translateY(-1px); }
.refresh-action:disabled { cursor: not-allowed; opacity: 0.55; box-shadow: none; }
.month-close-action { min-height: 42px; padding: 0 16px; border: 1px solid rgba(22, 101, 52, 0.22); border-radius: 12px; background: #166534; color: #fff; display: inline-flex; align-items: center; justify-content: center; gap: 8px; font-size: 14px; font-weight: 950; box-shadow: 0 10px 24px rgba(22, 101, 52, 0.16); }
.month-close-action:hover:not(:disabled) { background: #15803d; transform: translateY(-1px); }
.month-close-action:disabled { cursor: not-allowed; opacity: 0.45; box-shadow: none; }
.summary-grid { display: grid; grid-template-columns: minmax(260px, 1.35fr) repeat(3, minmax(160px, 1fr)); gap: 14px; }
.balance-card, .metric-card, .table-card { border: 1px solid var(--vy-line); background: var(--vy-surface); box-shadow: var(--vy-shadow-sm); }
.balance-card, .metric-card { border-radius: 18px; padding: 18px; }
.balance-icon, .metric-icon { width: 44px; height: 44px; border-radius: 14px; display: inline-flex; align-items: center; justify-content: center; color: #fff; background: var(--vy-orange); }
.balance-card span, .metric-card small { display: block; margin-top: 12px; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.balance-card strong, .metric-card strong { display: block; margin-top: 5px; font-size: 26px; font-weight: 900; }
.balance-card p { margin-top: 4px; color: var(--vy-ink-3); font-size: 12px; font-weight: 800; }
.metric-card { display: flex; gap: 12px; align-items: center; }
.metric-card small, .metric-card strong { margin-top: 0; }
.metric-card strong { font-size: 20px; }
.table-card { border-radius: 18px; padding: 18px; }
.search-box { min-width: min(340px, 100%); min-height: 42px; display: flex; align-items: center; gap: 8px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); padding: 0 12px; color: var(--vy-ink-3); }
.search-box input { width: 100%; border: 0; outline: 0; background: transparent; color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 800; }
.table-wrap { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; min-width: 980px; }
th, td { padding: 13px 12px; border-bottom: 1px solid var(--vy-line-2); text-align: left; vertical-align: middle; font-size: 13px; }
th { color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
td strong, td small { display: block; }
td small { margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; }
.actions-cell { text-align: right; }
.row-withdraw-button { min-height: 36px; padding: 0 12px; border: 1px solid rgba(31, 26, 20, 0.12); border-radius: 10px; background: #1f1a14; color: #fff; display: inline-flex; align-items: center; justify-content: center; gap: 7px; font-size: 13px; font-weight: 950; white-space: nowrap; box-shadow: 0 8px 18px rgba(31, 26, 20, 0.16); }
.row-withdraw-button:hover:not(:disabled) { background: #f28705; color: #1f1a14; transform: translateY(-1px); }
.row-withdraw-button:disabled { cursor: not-allowed; opacity: 0.5; box-shadow: none; }
.withdrawal-status { width: fit-content; min-height: 28px; padding: 0 10px; border-radius: 999px; display: inline-flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 950; text-transform: uppercase; white-space: nowrap; }
.withdrawal-status.pending { background: #fff7e8; color: #9a4d00; border: 1px solid rgba(242, 135, 5, 0.24); }
.withdrawal-status.processed { background: rgba(22, 101, 52, 0.1); color: #166534; border: 1px solid rgba(22, 101, 52, 0.2); }
.processed-actions { display: inline-flex; justify-content: flex-end; gap: 7px; flex-wrap: wrap; }
.detail-button, .print-button { min-height: 34px; padding: 0 10px; border-radius: 10px; display: inline-flex; align-items: center; justify-content: center; gap: 6px; font-size: 12px; font-weight: 950; white-space: nowrap; }
.detail-button { border: 1px solid var(--vy-line); background: var(--vy-surface-2); color: var(--vy-ink); }
.print-button { border: 1px solid rgba(242, 135, 5, 0.28); background: #fff7e8; color: #1f1a14; }
.detail-button:hover, .print-button:hover { transform: translateY(-1px); }
.pagination-bar { padding-top: 14px; }
.pagination-bar > div { display: flex; align-items: center; gap: 10px; color: var(--vy-ink-3); font-size: 12px; font-weight: 900; }
.pagination-bar select, .field select, .field input, .field textarea, .period-filter select { width: 100%; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 800; outline: 0; }
.pagination-bar select, .field select, .field input, .period-filter select { min-height: 42px; padding: 0 12px; }
.field textarea { padding: 11px 12px; resize: vertical; }
.pagination-actions button { min-height: 36px; padding: 0 13px; border-radius: 10px; border: 1px solid var(--vy-line); background: var(--vy-surface-2); color: var(--vy-ink); font-weight: 900; }
.error-box, .info-box, .success-box, .loading-box { padding: 13px 14px; border-radius: 14px; font-size: 13px; font-weight: 800; }
.error-box { border: 1px solid rgba(220, 38, 38, 0.2); background: rgba(220, 38, 38, 0.08); color: #991b1b; }
.info-box, .loading-box { border: 1px solid var(--vy-line); background: var(--vy-surface-2); color: var(--vy-ink-2); }
.success-box { border: 1px solid rgba(22, 101, 52, 0.2); background: rgba(22, 101, 52, 0.08); color: #166534; }
.retiro-backdrop { position: fixed; inset: 0; z-index: 80; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(31, 26, 20, 0.42); backdrop-filter: blur(8px); }
.retiro-modal { width: min(780px, 100%); max-height: calc(100vh - 40px); padding: 20px; border-radius: 22px; border: 1px solid var(--vy-line); background: var(--vy-surface); box-shadow: var(--vy-shadow-lg); color: var(--vy-ink); overflow: hidden; display: flex; flex-direction: column; }
.retiro-modal > header, .retiro-modal > footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.retiro-modal > header { padding-bottom: 14px; border-bottom: 1px solid var(--vy-line-2); }
.retiro-modal h2 { margin-top: 4px; font-size: 22px; font-weight: 900; }
.retiro-modal p { margin-top: 3px; color: var(--vy-ink-3); font-size: 13px; font-weight: 800; }
.retiro-modal > header button { width: 38px; height: 38px; border-radius: 12px; background: var(--vy-surface-2); color: var(--vy-ink-2); display: inline-flex; align-items: center; justify-content: center; }
.retiro-body { padding: 16px 2px; overflow: auto; }
.person-wallet-detail { display: grid; gap: 14px; }
.selected-person, .wallet-values div { padding: 14px; border: 1px solid var(--vy-line); border-radius: 14px; background: var(--vy-surface-2); }
.selected-person small, .wallet-values small { display: block; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.selected-person strong, .wallet-values strong { display: block; margin-top: 5px; color: var(--vy-ink); font-size: 16px; font-weight: 900; }
.selected-person span, .wallet-values span { display: block; margin-top: 4px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; line-height: 1.35; }
.wallet-values, .withdraw-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.cash-breakdown { border: 1px solid var(--vy-line); border-radius: 14px; background: #fff; overflow: hidden; }
.cash-breakdown header { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 13px 14px; border-bottom: 1px solid var(--vy-line-2); background: var(--vy-surface-2); }
.cash-breakdown header small { display: block; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.cash-breakdown header strong { display: block; margin-top: 3px; font-size: 15px; font-weight: 900; }
.cash-breakdown header b { white-space: nowrap; font-size: 18px; font-weight: 950; }
.breakdown-row { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 12px 14px; border-bottom: 1px solid var(--vy-line-2); }
.breakdown-row:last-child { border-bottom: 0; }
.breakdown-row span { min-width: 0; }
.breakdown-row strong, .breakdown-row small { display: block; }
.breakdown-row strong { font-size: 13px; font-weight: 900; }
.breakdown-row small { margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; }
.breakdown-row b { white-space: nowrap; font-size: 14px; font-weight: 950; }
.breakdown-empty { padding: 12px 14px; color: var(--vy-ink-3); font-size: 12px; font-weight: 800; }
.readonly-total { min-height: 42px; display: flex; align-items: center; padding: 0 12px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); font-weight: 900; }
.max-button { width: fit-content; min-height: 36px; padding: 0 13px; border-radius: 10px; background: rgba(242, 135, 5, 0.1); color: var(--vy-orange-deep); font-size: 12px; font-weight: 900; }
.product-withdrawal { display: grid; grid-template-columns: 1fr auto; gap: 10px; align-items: end; }
.withdraw-products-list { grid-column: 1 / -1; display: grid; gap: 8px; }
.withdraw-products-list article { display: grid; grid-template-columns: 1fr 90px 120px auto; gap: 8px; align-items: center; padding: 10px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); }
.withdraw-products-list input { min-height: 36px; border: 1px solid var(--vy-line); border-radius: 10px; padding: 0 10px; background: var(--vy-surface); font-weight: 900; }
.withdraw-products-list button { color: #991b1b; font-size: 12px; font-weight: 900; }
.retiro-modal > footer { justify-content: flex-end; padding-top: 14px; border-top: 1px solid var(--vy-line-2); }
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
button:disabled { cursor: not-allowed; opacity: 0.55; }
@media (max-width: 900px) {
  .page-header, .section-header, .pagination-bar, .header-actions { align-items: stretch; flex-direction: column; }
  .period-filter, .search-box, .header-actions .vy-btn, .refresh-action { width: 100%; }
  .summary-grid { grid-template-columns: 1fr; }
  .wallet-values, .withdraw-grid, .product-withdrawal { grid-template-columns: 1fr; }
  .withdraw-products-list article { grid-template-columns: 1fr; }
  .pagination-actions, .pagination-actions button, .retiro-modal > footer .vy-btn { width: 100%; }
  .retiro-modal > header, .retiro-modal > footer { align-items: stretch; flex-direction: column; }
}
</style>
