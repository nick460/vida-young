<script setup>
import { computed, onMounted, ref } from "vue";
import { Download, RefreshCw, Sparkles, TrendingUp, Users } from "lucide-vue-next";
import { jsPDF } from "jspdf";
import { apiRequest } from "../services/api.js";
import { useAuthStore } from "../stores/authStore.js";
import { VyBarChart, VyDonut } from "../components/ui.js";
import logoFull from "../assets/logoFull.png";

const auth = useAuthStore();
const loading = ref(false);
const error = ref("");
const periodos = ref([]);
const selectedPeriodoId = ref("");
const stats = ref(null);
const activeTab = ref("estadisticas");
const iaLoading = ref(false);
const iaStatus = ref("");
const iaInforme = ref("");
const iaError = ref("");
const exportandoPdf = ref(false);
let iaStatusTimer = null;

const personaId = computed(() => auth.usuario?.persona?.id || "");
const selectedPeriodo = computed(() =>
  periodos.value.find((periodo) => Number(periodo.id) === Number(selectedPeriodoId.value))
);
const resumen = computed(() => stats.value?.resumen || {});
const brazos = computed(() => stats.value?.brazos || []);
const porNivel = computed(() => stats.value?.porNivel || []);
const topAportantes = computed(() => stats.value?.topAportantes || []);
const menosAportan = computed(() => stats.value?.menosAportan || []);
const inactivos = computed(() => stats.value?.inactivos || []);
const evolucion = computed(() => stats.value?.evolucion || []);
const conciliacion = computed(() => stats.value?.conciliacionWallet || null);
const resumenVentas = computed(() => stats.value?.resumenVentas || {});
const ventasBrazos = computed(() => stats.value?.ventasBrazos || []);
const topVendedores = computed(() => stats.value?.topVendedores || []);
const ingresosSplit = computed(() => stats.value?.ingresosSplit || null);
const splitChart = computed(() => [
  { l: "Afil.", v: Number(ingresosSplit.value?.afiliacionMonto || 0) },
  { l: "Ventas", v: Number(ingresosSplit.value?.ventasBeneficiosMonto || 0) }
]);
const splitPieStyle = computed(() => {
  const afiliacion = Number(ingresosSplit.value?.afiliacionMonto || 0);
  const ventas = Number(ingresosSplit.value?.ventasBeneficiosMonto || 0);
  const total = afiliacion + ventas;
  const pct = total > 0 ? (afiliacion / total) * 100 : 0;
  return {
    background: `conic-gradient(var(--vy-orange) 0 ${pct}%, var(--vy-success) ${pct}% 100%)`
  };
});
const iaInformeParrafos = computed(() =>
  String(iaInforme.value || "")
    .split(/\n+/)
    .map((line) => line.trim())
    .filter(Boolean)
);

const brazosChart = computed(() =>
  brazos.value.slice(0, 8).map((brazo) => ({
    l: shortName(brazo.nombre),
    v: Number(brazo.monto || 0)
  }))
);
const evolucionChart = computed(() =>
  evolucion.value.map((punto) => ({
    l: shortPeriodo(punto.periodoNombre),
    v: Number(punto.monto || 0)
  }))
);
const nivelChart = computed(() =>
  porNivel.value.map((nivel) => ({
    l: `N${nivel.nivel}`,
    v: Number(nivel.aporte || 0)
  }))
);
const maxBrazo = computed(() =>
  brazos.value.reduce((max, brazo) => Math.max(max, Number(brazo.monto || 0)), 0)
);
const ventasBrazosChart = computed(() =>
  ventasBrazos.value.slice(0, 8).map((brazo) => ({
    l: shortName(brazo.nombre),
    v: Number(brazo.monto || 0)
  }))
);
const evolucionComprasChart = computed(() =>
  evolucion.value.map((punto) => ({
    l: shortPeriodo(punto.periodoNombre),
    v: Number(punto.montoCompras || 0)
  }))
);
const maxVentasBrazo = computed(() =>
  ventasBrazos.value.reduce((max, brazo) => Math.max(max, Number(brazo.monto || 0)), 0)
);
const totalRed = computed(() => Number(resumen.value.totalRed || 0));
const pctActivos = computed(() => Math.round(Number(resumen.value.pctActivos || 0)));
const pieStyle = computed(() => {
  const activos = pctActivos.value;
  return {
    background: `conic-gradient(var(--vy-orange) 0 ${activos}%, var(--vy-cream) ${activos}% 100%)`
  };
});

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function shortName(value) {
  if (!value) return "-";
  const parts = String(value).trim().split(/\s+/);
  if (parts.length === 1) return parts[0].slice(0, 8);
  return `${parts[0].slice(0, 6)} ${parts[1].charAt(0)}.`;
}

function shortPeriodo(nombre) {
  if (!nombre) return "-";
  return String(nombre).split(" ").slice(0, 2).join(" ");
}

async function loadPeriodos() {
  const [activePeriodo, gestiones] = await Promise.all([
    apiRequest("/api/gestiones/periodos/activo"),
    apiRequest("/api/gestiones")
  ]);
  const periodosPorGestion = await Promise.all(
    gestiones.map(async (gestion) => {
      const items = await apiRequest(`/api/gestiones/${gestion.id}/periodos`);
      return items.map((periodo) => ({ ...periodo, gestion: periodo.gestion || gestion }));
    })
  );
  periodos.value = periodosPorGestion
    .flat()
    .sort((a, b) => {
      const gestionA = Number(a.gestion?.anio || 0);
      const gestionB = Number(b.gestion?.anio || 0);
      if (gestionA !== gestionB) return gestionB - gestionA;
      return Number(b.mes || 0) - Number(a.mes || 0);
    });
  if (!selectedPeriodoId.value) {
    selectedPeriodoId.value = String(activePeriodo?.id || periodos.value[0]?.id || "");
  }
}

async function loadStats() {
  loading.value = true;
  error.value = "";
  try {
    if (!periodos.value.length) {
      await loadPeriodos();
    }
    if (!personaId.value) {
      await auth.cargarPerfil();
    }
    if (!personaId.value) {
      error.value = "Tu usuario no tiene una persona asociada.";
      return;
    }
    const query = selectedPeriodoId.value ? `?personaId=${personaId.value}&periodoId=${selectedPeriodoId.value}` : `?personaId=${personaId.value}`;
    stats.value = await apiRequest(`/api/estadisticas/mi-red${query}`);
  } catch (exception) {
    error.value = exception.message || "No se pudieron cargar las estadisticas.";
  } finally {
    loading.value = false;
  }
}

async function generarAnalisis() {
  if (iaLoading.value) return;
  iaLoading.value = true;
  iaError.value = "";
  iaInforme.value = "";
  const pasos = [
    "Analizando tu red...",
    "Revisando brazos, niveles y aportes...",
    "Generando tu informe con IA..."
  ];
  let paso = 0;
  iaStatus.value = pasos[0];
  iaStatusTimer = setInterval(() => {
    paso = Math.min(paso + 1, pasos.length - 1);
    iaStatus.value = pasos[paso];
  }, 2200);
  try {
    if (!personaId.value) {
      await auth.cargarPerfil();
    }
    const query = selectedPeriodoId.value
      ? `?personaId=${personaId.value}&periodoId=${selectedPeriodoId.value}`
      : `?personaId=${personaId.value}`;
    const respuesta = await apiRequest(`/api/estadisticas/mi-red/analisis${query}`, { method: "POST" });
    iaInforme.value = respuesta?.informe || "La IA no devolvio un informe.";
  } catch (exception) {
    iaError.value = exception.message || "No se pudo generar el analisis con IA.";
  } finally {
    clearInterval(iaStatusTimer);
    iaStatusTimer = null;
    iaStatus.value = "";
    iaLoading.value = false;
  }
}

function imageToDataUrl(src) {
  return new Promise((resolve) => {
    const image = new Image();
    image.onload = () => {
      const canvas = document.createElement("canvas");
      canvas.width = image.naturalWidth;
      canvas.height = image.naturalHeight;
      canvas.getContext("2d").drawImage(image, 0, 0);
      resolve(canvas.toDataURL("image/png"));
    };
    image.onerror = () => resolve(null);
    image.src = src;
  });
}

function nombreEmbajador() {
  const persona = auth.usuario?.persona;
  const nombre = `${persona?.nombres || ""} ${persona?.apellidos || ""}`.trim();
  return nombre || auth.usuario?.username || "Embajador";
}

async function exportarPdf() {
  if (!iaInforme.value || exportandoPdf.value) return;
  exportandoPdf.value = true;
  try {
    const doc = new jsPDF({ unit: "mm", format: "a4" });
    const pageWidth = doc.internal.pageSize.getWidth();
    const pageHeight = doc.internal.pageSize.getHeight();
    const margin = 14;
    const maxWidth = pageWidth - margin * 2;
    const periodoNombre = selectedPeriodo.value
      ? `${selectedPeriodo.value.nombre} - Gestion ${selectedPeriodo.value.gestion?.anio || ""}`
      : (stats.value?.periodoNombre || "Periodo");
    const fechaGen = new Date().toLocaleString("es-BO", { year: "numeric", month: "short", day: "2-digit", hour: "2-digit", minute: "2-digit" });
    let page = 1;
    let y = 0;

    const footer = () => {
      doc.setDrawColor(234, 223, 202);
      doc.setLineWidth(0.4);
      doc.line(margin, pageHeight - 14, pageWidth - margin, pageHeight - 14);
      doc.setTextColor(137, 127, 112);
      doc.setFont("helvetica", "normal");
      doc.setFontSize(8);
      doc.text(`Vida Young - Analisis de red - ${periodoNombre}`, margin, pageHeight - 10);
      doc.text(`Pag. ${page}`, pageWidth - margin, pageHeight - 10, { align: "right" });
    };
    const newPage = () => {
      footer();
      doc.addPage();
      page += 1;
      y = 16;
    };
    const need = (h) => {
      if (y + h > pageHeight - 18) newPage();
    };

    // Cabecera con marca
    y = 12;
    const logo = await imageToDataUrl(logoFull);
    if (logo) {
      doc.addImage(logo, "PNG", margin, y, 44, 14);
    }
    doc.setTextColor(137, 127, 112);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(8);
    doc.text(`Generado ${fechaGen}`, pageWidth - margin, y + 5, { align: "right" });
    y += 22;
    doc.setTextColor(31, 26, 20);
    doc.setFont("helvetica", "bold");
    doc.setFontSize(20);
    doc.text("Analisis IA de mi red", margin, y);
    y += 8;
    doc.setFillColor(242, 135, 5);
    doc.roundedRect(margin, y, 90, 8, 4, 4, "F");
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(9);
    doc.text(periodoNombre.slice(0, 52), margin + 4, y + 5.5);
    y += 13;
    doc.setTextColor(74, 65, 53);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(10);
    doc.text(`Embajador: ${nombreEmbajador()}`, margin, y);
    y += 4;
    doc.setDrawColor(242, 135, 5);
    doc.setLineWidth(1);
    doc.line(margin, y, pageWidth - margin, y);
    y += 8;

    // Resumen del mes en cajas
    need(30);
    doc.setTextColor(31, 26, 20);
    doc.setFont("helvetica", "bold");
    doc.setFontSize(12);
    doc.text("Resumen del mes", margin, y);
    y += 6;
    const kpis = [
      ["Miembros en red", String(resumen.value.totalRed || 0)],
      ["Activos", `${resumen.value.activos || 0} (${pctActivos.value}%)`],
      ["Afiliacion", `Bs. ${money(ingresosSplit.value?.afiliacionMonto)}`],
      ["Beneficios ventas", `Bs. ${money(ingresosSplit.value?.ventasBeneficiosMonto)}`],
      ["Ventas de la red", `Bs. ${money(resumenVentas.value.montoComprasRed)}`],
      ["Efectivo wallet", `Bs. ${money(conciliacion.value?.totalEfectivoWallet)}`]
    ];
    const boxW = (maxWidth - 4) / 2;
    kpis.forEach(([label, value], index) => {
      const col = index % 2;
      const row = Math.floor(index / 2);
      const x = margin + col * (boxW + 4);
      const boxY = y + row * 20;
      need(index === 0 ? 20 : 0);
      doc.setFillColor(255, 250, 240);
      doc.setDrawColor(234, 223, 202);
      doc.roundedRect(x, boxY, boxW, 16, 3, 3, "FD");
      doc.setTextColor(137, 127, 112);
      doc.setFont("helvetica", "bold");
      doc.setFontSize(7.5);
      doc.text(String(label).toUpperCase(), x + 4, boxY + 6);
      doc.setTextColor(31, 26, 20);
      doc.setFontSize(11);
      doc.text(String(value).slice(0, 30), x + 4, boxY + 12);
    });
    y += Math.ceil(kpis.length / 2) * 20 + 6;

    // Informe IA
    need(12);
    doc.setTextColor(31, 26, 20);
    doc.setFont("helvetica", "bold");
    doc.setFontSize(12);
    doc.text("Informe IA", margin, y);
    y += 6;
    doc.setFontSize(10);
    for (const parrafo of iaInformeParrafos.value) {
      const esTitulo = /^(\d+[).]|[#*-])/.test(parrafo);
      doc.setFont("helvetica", esTitulo ? "bold" : "normal");
      doc.setTextColor(...(esTitulo ? [31, 26, 20] : [74, 65, 53]));
      const lines = doc.splitTextToSize(parrafo, maxWidth);
      const h = lines.length * 5 + 3;
      need(h);
      doc.text(lines, margin, y);
      y += h;
    }

    footer();
    const fileName = `analisis-red-${String(periodoNombre).toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "") || "mes"}.pdf`;
    doc.save(fileName);
  } finally {
    exportandoPdf.value = false;
  }
}

onMounted(loadStats);
</script>

<template>
  <div class="vy stats-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Estadisticas</div>
          <h1>Mi red mes a mes</h1>
          <p>Quienes mas te aportan, quienes menos, inactivos e ingresos por brazo.</p>
          <strong v-if="selectedPeriodo">Mostrando {{ selectedPeriodo.nombre }}.</strong>
        </div>
        <div class="header-actions">
          <label class="period-filter">
            <span>Mes</span>
            <select v-model="selectedPeriodoId" @change="loadStats">
              <option value="" disabled>Selecciona un mes</option>
              <option v-for="periodo in periodos" :key="periodo.id" :value="periodo.id">
                {{ periodo.nombre }} - Gestion {{ periodo.gestion?.anio || "" }}
              </option>
            </select>
          </label>
          <button class="refresh-button" type="button" :disabled="loading" @click="loadStats">
            <RefreshCw :class="{ spinning: loading }" :size="16" stroke-width="2.3" />
            <span>{{ loading ? "Actualizando" : "Actualizar" }}</span>
          </button>
        </div>
      </header>

      <div v-if="error" class="error-box">{{ error }}</div>
      <div v-if="loading" class="loading-box">Cargando estadisticas...</div>

      <nav class="tabs">
        <button type="button" :class="{ active: activeTab === 'estadisticas' }" @click="activeTab = 'estadisticas'">
          Estadisticas
        </button>
        <button type="button" :class="{ active: activeTab === 'analisis' }" @click="activeTab = 'analisis'">
          <Sparkles :size="14" /> Analisis IA
        </button>
      </nav>

      <div v-show="activeTab === 'estadisticas'">
      <section class="kpi-grid">
        <article class="vy-card kpi-card">
          <span><Users :size="13" /> Miembros en red</span>
          <div><strong>{{ resumen.totalRed || 0 }}</strong><small>{{ resumen.directos || 0 }} directos</small></div>
        </article>
        <article class="vy-card kpi-card">
          <span>Activos del mes</span>
          <div><strong>{{ resumen.activos || 0 }}</strong><small>{{ pctActivos }}% · {{ resumen.inactivos || 0 }} inactivos</small></div>
        </article>
        <article class="vy-card kpi-card highlight">
          <span>Ingreso de la red (Bs.)</span>
          <div><strong>{{ money(resumen.total) }}</strong><small><TrendingUp :size="12" /> efect. {{ money(resumen.totalEfectivo) }}</small></div>
        </article>
        <article class="vy-card kpi-card">
          <span>Brazos / Profundidad</span>
          <div><strong>{{ brazos.length }}</strong><small>{{ resumen.profundidad || 0 }} niveles</small></div>
        </article>
        <article class="vy-card kpi-card">
          <span>Ventas de la red (Bs.)</span>
          <div><strong>{{ money(resumenVentas.montoComprasRed) }}</strong><small>{{ resumenVentas.totalComprasRed || 0 }} compras</small></div>
        </article>
        <article class="vy-card kpi-card">
          <span>PV de la red / Mis compras</span>
          <div><strong>{{ money(resumenVentas.pvComprasRed) }}</strong><small>Bs. {{ money(resumenVentas.miMontoCompras) }} propias</small></div>
        </article>
        <article class="vy-card kpi-card">
          <span>Por afiliacion (Bs.)</span>
          <div><strong>{{ money(ingresosSplit?.afiliacionMonto) }}</strong><small>{{ ingresosSplit?.afiliacionCount || 0 }} recompensas</small></div>
        </article>
        <article class="vy-card kpi-card">
          <span>Por ventas (Bs.)</span>
          <div><strong>{{ money(ingresosSplit?.ventasBeneficiosMonto) }}</strong><small>{{ ingresosSplit?.ventasBeneficiosCount || 0 }} beneficios</small></div>
        </article>
      </section>

      <section class="charts-grid">
        <article class="vy-card chart-card">
          <h2>Ingresos por brazo</h2>
          <p>Cada directo es un brazo con todo su subtree.</p>
          <div v-if="brazosChart.length && maxBrazo > 0" class="chart-scroll">
            <VyBarChart :data="brazosChart" :width="560" :height="200" />
          </div>
          <p v-else class="empty">Sin ingresos este mes.</p>
          <div class="brazo-list">
            <div v-for="brazo in brazos" :key="brazo.personaId" class="brazo-row">
              <header><span>{{ brazo.nombre }}</span><strong>Bs. {{ money(brazo.monto) }}</strong></header>
              <div class="track"><span :style="{ width: `${maxBrazo ? (Number(brazo.monto || 0) / maxBrazo) * 100 : 0}%` }"></span></div>
              <small>{{ brazo.miembros }} miembros · {{ brazo.activos }} activos</small>
            </div>
            <p v-if="!brazos.length" class="empty">Aun no tienes directos.</p>
          </div>
        </article>

        <article class="vy-card mix-card">
          <h2>Activos vs inactivos</h2>
          <p>Salud de tu red en el mes.</p>
          <div class="donut-row">
            <VyDonut :value="pctActivos" :size="130" />
            <div class="pie" :style="pieStyle"></div>
          </div>
          <ul class="legend">
            <li><i class="dot active"></i>Activos: {{ resumen.activos || 0 }}</li>
            <li><i class="dot inactive"></i>Inactivos: {{ resumen.inactivos || 0 }}</li>
          </ul>
          <h2 class="sub">Aporte por nivel</h2>
          <div v-if="nivelChart.length" class="chart-scroll">
            <VyBarChart :data="nivelChart" :width="320" :height="140" />
          </div>
          <ul class="nivel-list">
            <li v-for="nivel in porNivel" :key="nivel.nivel">
              <span>Nivel {{ nivel.nivel }} · {{ nivel.miembros }} pers.</span>
              <strong>Bs. {{ money(nivel.aporte) }}</strong>
            </li>
          </ul>
        </article>
      </section>

      <section class="vy-card chart-card">
        <h2>Ventas por brazo (compras de la red)</h2>
        <p>Brazos que mas venden en Bs. y PV del mes.</p>
        <div v-if="ventasBrazosChart.length && maxVentasBrazo > 0" class="chart-scroll">
          <VyBarChart :data="ventasBrazosChart" :width="640" :height="180" />
        </div>
        <p v-else class="empty">Sin ventas en la red este mes.</p>
        <div class="brazo-list">
          <div v-for="brazo in ventasBrazos" :key="brazo.personaId" class="brazo-row">
            <header><span>{{ brazo.nombre }}</span><strong>Bs. {{ money(brazo.monto) }}</strong></header>
            <div class="track track-green"><span :style="{ width: `${maxVentasBrazo ? (Number(brazo.monto || 0) / maxVentasBrazo) * 100 : 0}%` }"></span></div>
            <small>{{ brazo.compras }} compras · PV {{ money(brazo.pv) }}</small>
          </div>
          <p v-if="!ventasBrazos.length" class="empty">Aun no tienes directos.</p>
        </div>
      </section>

      <section class="charts-grid">
        <article class="vy-card chart-card">
          <h2>Afiliacion vs ventas (Bs.)</h2>
          <p>De donde viene tu dinero este mes.</p>
          <div class="donut-row">
            <div class="pie" :style="splitPieStyle"></div>
            <div class="chart-scroll">
              <VyBarChart :data="splitChart" :width="280" :height="140" />
            </div>
          </div>
          <ul class="legend">
            <li><i class="dot active"></i>Afiliacion: Bs. {{ money(ingresosSplit?.afiliacionMonto) }} ({{ ingresosSplit?.afiliacionCount || 0 }})</li>
            <li><i class="dot sales"></i>Ventas: Bs. {{ money(ingresosSplit?.ventasBeneficiosMonto) }} ({{ ingresosSplit?.ventasBeneficiosCount || 0 }})</li>
          </ul>
        </article>
        <article class="vy-card mix-card">
          <h2>Volumen de red acreditado</h2>
          <p>PV/QP que te dieron las compras de tu red (ver Wallet).</p>
          <ul class="nivel-list">
            <li><span>PV de red en wallet</span><strong>{{ money(conciliacion?.pvWallet) }}</strong></li>
            <li><span>QP de red en wallet</span><strong>{{ money(conciliacion?.qpWallet) }}</strong></li>
            <li><span>CR en wallet</span><strong>{{ money(conciliacion?.crWallet) }}</strong></li>
          </ul>
        </article>
      </section>

      <section class="vy-card chart-card">
        <h2>Evolucion de ingresos (ultimos 6 periodos)</h2>
        <p>Para analizar como te va yendo mes a mes.</p>
        <div v-if="evolucionChart.length" class="chart-scroll">
          <VyBarChart :data="evolucionChart" :width="640" :height="180" />
        </div>
        <p v-else class="empty">Sin historial suficiente.</p>
        <h2 class="sub">Evolucion de ventas de la red (Bs.)</h2>
        <div v-if="evolucionComprasChart.length" class="chart-scroll">
          <VyBarChart :data="evolucionComprasChart" :width="640" :height="180" />
        </div>
      </section>

      <section class="tables-grid">
        <article class="vy-card">
          <h2>Top: quienes mas te aportan</h2>
          <div class="table-wrap"><table>
            <thead><tr><th>Miembro</th><th>Niv.</th><th>Aporte</th></tr></thead>
            <tbody>
              <tr v-for="miembro in topAportantes" :key="miembro.personaId">
                <td><strong>{{ miembro.nombre }}</strong><small>{{ miembro.activo ? "Activo" : "Inactivo" }}</small></td>
                <td>{{ miembro.nivel }}</td>
                <td class="amount">Bs. {{ money(miembro.aporte) }}</td>
              </tr>
              <tr v-if="!topAportantes.length"><td colspan="3" class="empty">Sin datos.</td></tr>
            </tbody>
          </table></div>
        </article>
        <article class="vy-card">
          <h2>Quienes menos aportan</h2>
          <div class="table-wrap"><table>
            <thead><tr><th>Miembro</th><th>Niv.</th><th>Aporte</th></tr></thead>
            <tbody>
              <tr v-for="miembro in menosAportan" :key="miembro.personaId">
                <td><strong>{{ miembro.nombre }}</strong><small>{{ miembro.activo ? "Activo" : "Inactivo" }}</small></td>
                <td>{{ miembro.nivel }}</td>
                <td class="amount">Bs. {{ money(miembro.aporte) }}</td>
              </tr>
              <tr v-if="!menosAportan.length"><td colspan="3" class="empty">Sin datos.</td></tr>
            </tbody>
          </table></div>
        </article>
        <article class="vy-card">
          <h2>Brazos que mas venden</h2>
          <div class="table-wrap"><table>
            <thead><tr><th>Brazo</th><th>Compras</th><th>Bs.</th></tr></thead>
            <tbody>
              <tr v-for="brazo in ventasBrazos.slice(0, 5)" :key="brazo.personaId">
                <td><strong>{{ brazo.nombre }}</strong><small>PV {{ money(brazo.pv) }} · benf. Bs. {{ money(brazo.beneficios) }}</small></td>
                <td>{{ brazo.compras }}</td>
                <td class="amount">Bs. {{ money(brazo.monto) }}</td>
              </tr>
              <tr v-if="!ventasBrazos.length"><td colspan="3" class="empty">Sin ventas.</td></tr>
            </tbody>
          </table></div>
        </article>
        <article class="vy-card">
          <h2>Top vendedores de la red</h2>
          <div class="table-wrap"><table>
            <thead><tr><th>Miembro</th><th>Niv.</th><th>Bs.</th></tr></thead>
            <tbody>
              <tr v-for="miembro in topVendedores" :key="miembro.personaId">
                <td><strong>{{ miembro.nombre }}</strong><small>{{ miembro.compras }} compras · PV {{ money(miembro.pv) }}</small></td>
                <td>{{ miembro.nivel }}</td>
                <td class="amount">Bs. {{ money(miembro.monto) }}</td>
              </tr>
              <tr v-if="!topVendedores.length"><td colspan="3" class="empty">Sin ventas.</td></tr>
            </tbody>
          </table></div>
        </article>
        <article class="vy-card">
          <h2>Inactivos del mes</h2>
          <p class="card-sub">Para reactivar o dar seguimiento.</p>
          <div class="table-wrap"><table>
            <thead><tr><th>Miembro</th><th>Niv.</th><th>Doc.</th></tr></thead>
            <tbody>
              <tr v-for="miembro in inactivos" :key="miembro.personaId">
                <td><strong>{{ miembro.nombre }}</strong></td>
                <td>{{ miembro.nivel }}</td>
                <td>{{ miembro.documento || "-" }}</td>
              </tr>
              <tr v-if="!inactivos.length"><td colspan="3" class="empty">Todos activos. Buen trabajo.</td></tr>
            </tbody>
          </table></div>
        </article>
      </section>

      <section v-if="conciliacion" class="vy-card conciliacion-card">
        <h2>Conciliacion con Wallet</h2>
        <p>Mismos datos y criterios que /wallet para este mes: {{ conciliacion.movimientosCount }} movimientos.</p>
        <div class="conciliacion-grid">
          <div><span>Dinero billetera</span><strong>Bs. {{ money(conciliacion.dineroBilletera) }}</strong></div>
          <div><span>Recompensas N2+ ({{ conciliacion.recompensasNivel2Count }})</span><strong>Bs. {{ money(conciliacion.recompensasNivel2Monto) }}</strong></div>
          <div class="total"><span>Efectivo total (= wallet)</span><strong>Bs. {{ money(conciliacion.totalEfectivoWallet) }}</strong></div>
          <div><span>Nivel 1 ({{ conciliacion.nivel1Count }})</span><strong>Bs. {{ money(conciliacion.nivel1Efectivo) }} + prod. {{ money(conciliacion.nivel1Productos) }}</strong></div>
          <div><span>PV / QP / CR wallet</span><strong>{{ money(conciliacion.pvWallet) }} / {{ money(conciliacion.qpWallet) }} / {{ money(conciliacion.crWallet) }}</strong></div>
          <div><span>Ventas red / Mis compras</span><strong>Bs. {{ money(resumenVentas.montoComprasRed) }} / {{ money(resumenVentas.miMontoCompras) }}</strong></div>
        </div>
      </section>
      </div>

      <div v-show="activeTab === 'analisis'">
        <section class="vy-card ia-card">
          <h2><Sparkles :size="16" /> Analisis IA de tu red</h2>
          <p v-if="selectedPeriodo">Analiza los datos de {{ selectedPeriodo.nombre }} con todos tus numeros del mes.</p>
          <p v-else>Analiza tus datos con inteligencia artificial.</p>
          <div class="ia-actions">
            <button class="refresh-button ia-button" type="button" :disabled="iaLoading || loading" @click="generarAnalisis">
              <Sparkles :size="16" />
              <span>{{ iaLoading ? "Generando..." : iaInforme ? "Regenerar analisis" : "Generar analisis con IA" }}</span>
            </button>
            <button v-if="iaInforme && !iaLoading" class="ghost-button" type="button" :disabled="exportandoPdf" @click="exportarPdf">
              <Download :size="16" />
              <span>{{ exportandoPdf ? "Exportando..." : "Exportar PDF" }}</span>
            </button>
          </div>
          <div v-if="iaLoading" class="ia-status">
            <span class="ia-spinner"></span>
            <strong>{{ iaStatus }}</strong>
          </div>
          <div v-if="iaError" class="error-box">{{ iaError }}</div>
          <div v-if="iaInforme && !iaLoading" class="ia-informe">
            <p v-for="(parrafo, index) in iaInformeParrafos" :key="index">{{ parrafo }}</p>
          </div>
          <p v-if="!iaInforme && !iaLoading && !iaError" class="empty">Aun no generas el analisis de este mes. Presiona el boton y la IA te dara recomendaciones para seguir mejorando en el negocio.</p>
        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
.workspace { padding: 28px 32px 40px; min-width: 0; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; gap: 20px; margin-bottom: 24px; }
.page-header h1 { font-size: 30px; font-weight: 800; margin-top: 8px; }
.page-header p { font-size: 14px; color: var(--vy-ink-2); margin-top: 4px; }
.page-header strong { display: block; margin-top: 8px; color: var(--vy-orange-deep); font-size: 13px; font-weight: 900; }
.header-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.period-filter { min-width: 230px; display: grid; gap: 6px; }
.period-filter span { color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.period-filter select { min-height: 42px; padding: 0 12px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface); font-weight: 800; outline: none; }
.refresh-button { min-height: 42px; padding: 0 16px; border-radius: 12px; background: linear-gradient(135deg, var(--vy-orange) 0%, var(--vy-orange-deep) 100%); color: #fff; display: inline-flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 900; }
.refresh-button:disabled { opacity: 0.72; cursor: wait; }
.spinning { animation: refresh-spin 0.8s linear infinite; }
@keyframes refresh-spin { to { transform: rotate(360deg); } }
.error-box, .loading-box { padding: 14px 16px; border-radius: 12px; font-size: 13px; font-weight: 800; margin-bottom: 14px; }
.error-box { color: var(--vy-danger); background: rgba(196, 69, 42, 0.1); }
.loading-box { color: var(--vy-ink-2); background: var(--vy-surface-2); }
.kpi-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin-bottom: 18px; }
.kpi-card { padding: 18px; }
.kpi-card.highlight { border-color: var(--vy-orange); background: rgba(242, 135, 5, 0.05); }
.kpi-card > span { font-size: 11px; color: var(--vy-ink-3); font-weight: 800; text-transform: uppercase; display: flex; align-items: center; gap: 6px; }
.kpi-card div { display: flex; align-items: baseline; gap: 8px; margin-top: 8px; flex-wrap: wrap; }
.kpi-card strong { font-size: 24px; font-weight: 800; }
.kpi-card small { font-size: 11px; font-weight: 800; color: var(--vy-ink-2); background: var(--vy-gray); padding: 3px 8px; border-radius: 999px; }
.charts-grid { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(320px, 1fr); gap: 14px; margin-bottom: 18px; }
.chart-card, .mix-card { padding: 22px; margin-bottom: 18px; }
.chart-card h2, .mix-card h2 { font-size: 16px; font-weight: 800; }
.chart-card p, .mix-card p, .card-sub { font-size: 12px; color: var(--vy-ink-3); margin-top: 2px; }
.chart-scroll { margin-top: 14px; overflow-x: auto; }
.empty { color: var(--vy-ink-3); font-size: 13px; font-weight: 700; margin-top: 10px; }
.brazo-list { display: grid; gap: 10px; margin-top: 14px; }
.brazo-row header { display: flex; justify-content: space-between; font-size: 13px; font-weight: 800; margin-bottom: 5px; }
.track { height: 8px; border-radius: 999px; background: var(--vy-line-2); }
.track span { display: block; height: 100%; border-radius: 999px; background: var(--vy-orange); }
.track-green span { background: var(--vy-success); }
.brazo-row small { color: var(--vy-ink-3); font-size: 11px; font-weight: 700; }
.donut-row { display: flex; align-items: center; gap: 18px; margin-top: 14px; }
.pie { width: 90px; height: 90px; border-radius: 50%; }
.legend { list-style: none; margin: 12px 0 0; padding: 0; display: grid; gap: 6px; font-size: 13px; font-weight: 800; }
.dot { display: inline-block; width: 10px; height: 10px; border-radius: 50%; margin-right: 8px; }
.dot.active { background: var(--vy-orange); }
.dot.inactive { background: var(--vy-cream); border: 1px solid var(--vy-line); }
.dot.sales { background: var(--vy-success); }
.sub { margin-top: 18px; }
.nivel-list { list-style: none; margin: 10px 0 0; padding: 0; display: grid; gap: 6px; }
.nivel-list li { display: flex; justify-content: space-between; font-size: 13px; font-weight: 800; background: var(--vy-surface-2); border-radius: 10px; padding: 8px 10px; }
.tabs { display: flex; gap: 8px; margin-bottom: 18px; }
.tabs button { display: inline-flex; align-items: center; gap: 6px; padding: 9px 18px; border-radius: 999px; background: var(--vy-gray); color: var(--vy-ink-2); font-size: 13px; font-weight: 800; }
.tabs button.active { background: var(--vy-ink); color: #fff; }
.conciliacion-card { padding: 22px; margin-bottom: 18px; border-color: var(--vy-orange); }
.conciliacion-card h2 { font-size: 16px; font-weight: 800; }
.conciliacion-card p { font-size: 12px; color: var(--vy-ink-3); margin-top: 2px; }
.conciliacion-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-top: 14px; }
.conciliacion-grid div { background: var(--vy-surface-2); border-radius: 12px; padding: 12px; }
.conciliacion-grid div.total { background: rgba(242, 135, 5, 0.08); }
.conciliacion-grid span { display: block; font-size: 11px; color: var(--vy-ink-3); font-weight: 800; text-transform: uppercase; }
.conciliacion-grid strong { display: block; margin-top: 6px; font-size: 16px; font-weight: 900; }
.ia-card { padding: 24px; }
.ia-card h2 { display: flex; align-items: center; gap: 8px; font-size: 17px; font-weight: 800; }
.ia-actions { display: flex; gap: 10px; margin-top: 14px; flex-wrap: wrap; }
.ia-button { margin-top: 0; }
.ghost-button { min-height: 42px; padding: 0 16px; border-radius: 12px; border: 1px solid var(--vy-line); background: var(--vy-surface); color: var(--vy-ink); display: inline-flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 900; }
.ghost-button:disabled { opacity: 0.6; cursor: wait; }
.ia-status { display: flex; align-items: center; gap: 10px; margin-top: 16px; padding: 14px; border-radius: 12px; background: var(--vy-surface-2); font-size: 13px; }
.ia-spinner { width: 18px; height: 18px; border-radius: 50%; border: 3px solid var(--vy-line); border-top-color: var(--vy-orange); animation: refresh-spin 0.8s linear infinite; flex-shrink: 0; }
.ia-informe { display: grid; gap: 10px; margin-top: 16px; }
.ia-informe p { font-size: 14px; line-height: 1.6; color: var(--vy-ink-2); background: var(--vy-surface-2); border-radius: 12px; padding: 12px 14px; }
.tables-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.tables-grid .vy-card { padding: 18px; }
.tables-grid h2 { font-size: 15px; font-weight: 800; margin-bottom: 10px; }
.table-wrap { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; font-size: 13px; }
thead tr { text-align: left; color: var(--vy-ink-3); font-size: 11px; text-transform: uppercase; }
th, td { padding: 10px 6px; }
tbody tr { border-top: 1px solid var(--vy-line-2); }
td small { display: block; color: var(--vy-ink-3); font-size: 11px; }
.amount { text-align: right; font-weight: 900; white-space: nowrap; }
@media (max-width: 1100px) { .kpi-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .charts-grid, .tables-grid, .conciliacion-grid { grid-template-columns: 1fr; } .workspace { padding: 24px 20px 32px; } }
@media (max-width: 680px) {
  .page-header { flex-direction: column; align-items: stretch; }
  .page-header h1 { font-size: 24px; }
  .header-actions, .period-filter, .refresh-button { width: 100%; }
  .kpi-grid { grid-template-columns: 1fr; }
  .tabs { overflow-x: auto; padding-bottom: 4px; }
  .tabs button { flex-shrink: 0; }
  .chart-card, .mix-card, .ia-card, .conciliacion-card { padding: 16px; }
  .tables-grid .vy-card { padding: 14px; }
  .table-wrap table { min-width: 460px; }
  .donut-row { flex-wrap: wrap; }
  .brazo-row header { flex-direction: column; gap: 2px; }
  .conciliacion-grid strong { font-size: 15px; }
  .ia-actions { flex-direction: column; align-items: stretch; }
  .ia-actions .refresh-button { width: 100%; }
}
</style>
