<script setup>
import { computed, onMounted, ref } from "vue";
import {
  BadgeCheck,
  CalendarClock,
  Gift,
  Network,
  RefreshCw,
  Search,
  ShoppingBag,
  UserRound,
  WalletCards
} from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import { VyAvatar } from "../components/ui.js";

const loading = ref(false);
const detailLoading = ref(false);
const error = ref("");
const query = ref("");
const personas = ref([]);
const usuarios = ref([]);
const referidos = ref([]);
const selectedPersonaId = ref("");
const walletSummary = ref(null);
const compras = ref([]);
const recompensas = ref([]);

const selectedPersona = computed(() =>
  personas.value.find((persona) => Number(persona.id) === Number(selectedPersonaId.value)) || null
);

const filteredPersonas = computed(() => {
  const term = normalize(query.value);
  if (!term) return personas.value.slice(0, 20);

  return personas.value
    .filter((persona) => normalize([
      persona.nombres,
      persona.apellidos,
      persona.documento,
      persona.email,
      persona.telefono
    ].join(" ")).includes(term))
    .slice(0, 30);
});

const usuario = computed(() =>
  usuarios.value.find((item) => Number(item.persona?.id) === Number(selectedPersonaId.value)) || null
);

const referido = computed(() =>
  referidos.value.find((item) => Number(item.persona?.id) === Number(selectedPersonaId.value)) || null
);

const patrocinador = computed(() => referido.value?.patrocinador || null);

const directos = computed(() =>
  referidos.value.filter((item) => Number(item.patrocinador?.id) === Number(selectedPersonaId.value))
);

const billetera = computed(() => walletSummary.value?.billetera || {});
const movimientos = computed(() => walletSummary.value?.movimientos || []);
const membresias = computed(() => walletSummary.value?.membresias || []);
const cierres = computed(() => walletSummary.value?.cierresMensuales || []);
const periodoActivo = computed(() => walletSummary.value?.periodoActivo || null);

const walletCards = computed(() => [
  { label: "Dinero", value: `Bs. ${money(billetera.value.saldoDinero)}` },
  { label: "PV", value: money(billetera.value.saldoPv) },
  { label: "QP", value: money(billetera.value.saldoQp) },
  { label: "CR", value: money(billetera.value.saldoCr) },
  { label: "Nivel 1 efectivo", value: `Bs. ${money(walletSummary.value?.efectivoNivel1Disponible)}` },
  { label: "Nivel 1 productos", value: `Bs. ${money(walletSummary.value?.productosNivel1Disponible)}` }
]);

const totals = computed(() => ({
  compras: compras.value.reduce((sum, compra) => sum + Number(compra.subtotal || 0), 0),
  pv: compras.value.reduce((sum, compra) => sum + Number(compra.totalPv || 0), 0),
  qp: compras.value.reduce((sum, compra) => sum + Number(compra.totalQp || 0), 0),
  recompensasEfectivo: recompensas.value.reduce((sum, item) => sum + Number(item.montoEfectivo || 0), 0),
  recompensasProductos: recompensas.value.reduce((sum, item) => sum + Number(item.valorProductos || 0), 0)
}));

const timeline = computed(() => {
  const items = [
    ...compras.value.map((compra) => ({
      id: `compra-${compra.id}`,
      type: "Compra",
      title: `Compra #${compra.id}`,
      subtitle: `${compra.estadoCompra || "Sin estado"} - Bs. ${money(compra.subtotal)}`,
      date: compra.fechaCompra,
      icon: ShoppingBag
    })),
    ...recompensas.value.map((recompensa) => ({
      id: `recompensa-${recompensa.id}`,
      type: "Recompensa",
      title: `Recompensa nivel ${recompensa.nivelGenerado || "-"}`,
      subtitle: `${recompensa.planIngreso?.nombre || "Plan"} - Bs. ${money(recompensa.montoEfectivo)} efectivo`,
      date: recompensa.fechaRegistro,
      icon: Gift
    })),
    ...membresias.value.map((membresia) => ({
      id: `membresia-${membresia.id}`,
      type: "Membresia",
      title: membershipName(membresia),
      subtitle: `${membresia.estadoMembresia || "Sin estado"} hasta ${formatDate(membresia.fechaFin)}`,
      date: membresia.fechaInicio || membresia.fechaRegistro,
      icon: BadgeCheck
    })),
    ...movimientos.value.map((movimiento) => ({
      id: `movimiento-${movimiento.id}`,
      type: movimiento.tipo || "Movimiento",
      title: movimiento.concepto || "Movimiento de billetera",
      subtitle: `${movimiento.referenciaTipo || "Billetera"} - ${money(movimiento.monto)}`,
      date: movimiento.fechaRegistro,
      icon: WalletCards
    }))
  ];

  return items
    .filter((item) => item.date)
    .sort((left, right) => new Date(right.date) - new Date(left.date))
    .slice(0, 80);
});

function normalize(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
}

function fullName(persona) {
  if (!persona) return "Sin persona";
  return `${persona.nombres || ""} ${persona.apellidos || ""}`.trim() || "Sin nombre";
}

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function formatDate(value) {
  if (!value) return "Sin fecha";
  return new Date(value).toLocaleDateString("es-BO", {
    year: "numeric",
    month: "short",
    day: "2-digit"
  });
}

function formatDateTime(value) {
  if (!value) return "Sin fecha";
  return new Date(value).toLocaleString("es-BO", {
    year: "numeric",
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

function membershipName(membership) {
  return membership?.nombreActivacion || membership?.plan?.nombre || "Plan";
}

function compraProductos(compra) {
  return (compra.detalles || [])
    .map((detalle) => `${detalle.producto?.nombre || "Producto"} x${detalle.cantidad || 0}`)
    .join(", ");
}

async function loadBaseData() {
  loading.value = true;
  error.value = "";

  try {
    const [personasData, usuariosData, referidosData] = await Promise.all([
      apiRequest("/api/personas"),
      apiRequest("/api/usuarios"),
      apiRequest("/api/referidos")
    ]);
    personas.value = Array.isArray(personasData) ? personasData : [];
    usuarios.value = Array.isArray(usuariosData) ? usuariosData : [];
    referidos.value = Array.isArray(referidosData) ? referidosData : [];
  } catch (exception) {
    error.value = exception.message || "No se pudieron cargar las personas.";
  } finally {
    loading.value = false;
  }
}

async function selectPersona(persona) {
  selectedPersonaId.value = persona?.id || "";
  walletSummary.value = null;
  compras.value = [];
  recompensas.value = [];

  if (!selectedPersonaId.value) return;

  detailLoading.value = true;
  error.value = "";

  try {
    const [walletResult, comprasResult, recompensasResult] = await Promise.allSettled([
      apiRequest(`/api/billeteras/persona/${selectedPersonaId.value}`),
      apiRequest(`/api/compras/persona/${selectedPersonaId.value}`),
      apiRequest(`/api/recompensas/persona/${selectedPersonaId.value}`)
    ]);

    walletSummary.value = walletResult.status === "fulfilled" ? walletResult.value : null;
    compras.value = comprasResult.status === "fulfilled" && Array.isArray(comprasResult.value) ? comprasResult.value : [];
    recompensas.value = recompensasResult.status === "fulfilled" && Array.isArray(recompensasResult.value) ? recompensasResult.value : [];

    if ([walletResult, comprasResult, recompensasResult].some((result) => result.status === "rejected")) {
      error.value = "Algunos datos no se pudieron cargar, pero la consulta disponible se mantiene visible.";
    }
  } finally {
    detailLoading.value = false;
  }
}

onMounted(loadBaseData);
</script>

<template>
  <div class="vy consult-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Consultar</div>
          <h1>Vista completa de persona</h1>
          <p>Busca una persona y revisa sus datos, red, billetera activa e historial operativo.</p>
        </div>
        <button class="vy-btn vy-btn-ghost" type="button" :disabled="loading" @click="loadBaseData">
          <RefreshCw :class="{ spinning: loading }" :size="15" />
          Actualizar
        </button>
      </header>

      <section class="lookup-layout">
        <aside class="vy-card search-panel">
          <label class="search-field">
            <Search :size="16" />
            <input v-model.trim="query" placeholder="Buscar por nombre, CI, telefono o email" />
          </label>

          <div v-if="loading" class="empty-box">Cargando personas...</div>
          <div v-else class="person-results">
            <button
              v-for="persona in filteredPersonas"
              :key="persona.id"
              type="button"
              :class="{ active: Number(persona.id) === Number(selectedPersonaId) }"
              @click="selectPersona(persona)"
            >
              <VyAvatar :name="fullName(persona)" :size="34" bg="var(--vy-orange)" color="#fff" />
              <span>
                <strong>{{ fullName(persona) }}</strong>
                <small>{{ persona.documento || "Sin documento" }} - {{ persona.telefono || "Sin telefono" }}</small>
              </span>
            </button>
            <div v-if="!filteredPersonas.length" class="empty-box">No se encontraron personas.</div>
          </div>
        </aside>

        <section class="detail-panel">
          <div v-if="error" class="error-box">{{ error }}</div>

          <section v-if="!selectedPersona" class="vy-card empty-state">
            <Search :size="30" />
            <h2>Selecciona una persona</h2>
            <p>La consulta mostrara datos personales, usuario, plan, patrocinador, directos, billetera y actividad historica.</p>
          </section>

          <template v-else>
            <section class="profile-strip vy-card">
              <VyAvatar :name="fullName(selectedPersona)" :size="64" bg="var(--vy-ink)" color="#fff" />
              <div class="profile-main">
                <span class="vy-eyebrow">Persona #{{ selectedPersona.id }}</span>
                <h2>{{ fullName(selectedPersona) }}</h2>
                <p>{{ selectedPersona.documento || "Sin documento" }} - {{ selectedPersona.email || "Sin correo" }} - {{ selectedPersona.telefono || "Sin telefono" }}</p>
              </div>
              <div class="profile-status">
                <strong>{{ selectedPersona.estado || "ACTIVO" }}</strong>
                <small>{{ usuario?.username || "Sin usuario" }}</small>
              </div>
            </section>

            <div v-if="detailLoading" class="loading-box">Cargando detalle...</div>

            <section class="summary-grid">
              <article class="summary-card vy-card">
                <span class="summary-icon"><UserRound :size="18" /></span>
                <small>Usuario</small>
                <strong>{{ usuario?.username || "Sin acceso" }}</strong>
                <p>{{ usuario?.activo === false ? "Inactivo" : "Activo" }}</p>
              </article>
              <article class="summary-card vy-card">
                <span class="summary-icon"><BadgeCheck :size="18" /></span>
                <small>Plan actual</small>
                <strong>{{ referido?.plan?.nombre || "Sin plan" }}</strong>
                <p>{{ referido?.membresiaActiva ? "Membresia activa" : "Sin membresia activa" }}</p>
              </article>
              <article class="summary-card vy-card">
                <span class="summary-icon"><Network :size="18" /></span>
                <small>Red directa</small>
                <strong>{{ directos.length }}</strong>
                <p>{{ patrocinador ? `Patrocinador: ${fullName(patrocinador)}` : "Sin patrocinador" }}</p>
              </article>
              <article class="summary-card vy-card">
                <span class="summary-icon"><CalendarClock :size="18" /></span>
                <small>Periodo activo</small>
                <strong>{{ periodoActivo?.nombre || "Sin periodo" }}</strong>
                <p>{{ periodoActivo?.gestion?.anio || "" }}</p>
              </article>
            </section>

            <section class="wallet-grid">
              <article v-for="card in walletCards" :key="card.label" class="wallet-card">
                <span>{{ card.label }}</span>
                <strong>{{ card.value }}</strong>
              </article>
            </section>

            <section class="content-grid">
              <article class="vy-card info-card">
                <header>
                  <h3>Red</h3>
                  <span>{{ directos.length }} directos</span>
                </header>
                <div class="info-row">
                  <small>Patrocinador</small>
                  <strong>{{ patrocinador ? fullName(patrocinador) : "Sin patrocinador" }}</strong>
                </div>
                <div class="direct-list">
                  <div v-for="item in directos.slice(0, 8)" :key="item.id" class="direct-row">
                    <strong>{{ fullName(item.persona) }}</strong>
                    <small>{{ item.persona?.documento || "Sin documento" }} - {{ item.plan?.nombre || "Sin plan" }}</small>
                  </div>
                  <div v-if="!directos.length" class="empty-box compact">No tiene referidos directos.</div>
                </div>
              </article>

              <article class="vy-card info-card">
                <header>
                  <h3>Totales historicos</h3>
                  <span>{{ compras.length }} compras</span>
                </header>
                <div class="metrics-list">
                  <div><span>Compras</span><strong>Bs. {{ money(totals.compras) }}</strong></div>
                  <div><span>PV compras</span><strong>{{ money(totals.pv) }}</strong></div>
                  <div><span>QP compras</span><strong>{{ money(totals.qp) }}</strong></div>
                  <div><span>Recompensas efectivo</span><strong>Bs. {{ money(totals.recompensasEfectivo) }}</strong></div>
                  <div><span>Recompensas productos</span><strong>Bs. {{ money(totals.recompensasProductos) }}</strong></div>
                </div>
              </article>
            </section>

            <section class="vy-card history-card">
              <header>
                <h3>Compras internas</h3>
                <span>{{ compras.length }} registros</span>
              </header>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Estado</th>
                      <th>Productos</th>
                      <th>Total</th>
                      <th>PV/QP/CR</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="compra in compras" :key="compra.id">
                      <td>{{ formatDate(compra.fechaCompra) }}</td>
                      <td><span class="status-pill">{{ compra.estadoCompra }}</span></td>
                      <td>{{ compraProductos(compra) || "Sin detalle" }}</td>
                      <td>Bs. {{ money(compra.subtotal) }}</td>
                      <td>{{ money(compra.totalPv) }} / {{ money(compra.totalQp) }} / {{ money(compra.totalCr) }}</td>
                    </tr>
                    <tr v-if="!compras.length">
                      <td colspan="5">No hay compras registradas.</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <section class="vy-card history-card">
              <header>
                <h3>Recompensas</h3>
                <span>{{ recompensas.length }} registros</span>
              </header>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Nivel</th>
                      <th>Origen</th>
                      <th>Efectivo</th>
                      <th>Productos</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="recompensa in recompensas" :key="recompensa.id">
                      <td>{{ formatDate(recompensa.fechaRegistro) }}</td>
                      <td>{{ recompensa.nivelGenerado }}</td>
                      <td>{{ fullName(recompensa.referido?.persona) }} - {{ recompensa.planIngreso?.nombre || "Plan" }}</td>
                      <td>Bs. {{ money(recompensa.montoEfectivo) }}</td>
                      <td>Bs. {{ money(recompensa.valorProductos) }}</td>
                    </tr>
                    <tr v-if="!recompensas.length">
                      <td colspan="5">No hay recompensas registradas.</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <section class="content-grid">
              <article class="vy-card history-card">
                <header>
                  <h3>Membresias</h3>
                  <span>{{ membresias.length }} registros</span>
                </header>
                <div class="stack-list">
                  <div v-for="item in membresias" :key="item.id" class="stack-row">
                    <strong>{{ membershipName(item) }}</strong>
                    <small>{{ item.estadoMembresia || "Sin estado" }} - {{ formatDate(item.fechaInicio) }} a {{ formatDate(item.fechaFin) }}</small>
                  </div>
                  <div v-if="!membresias.length" class="empty-box compact">Sin historial de membresias.</div>
                </div>
              </article>

              <article class="vy-card history-card">
                <header>
                  <h3>Cierres mensuales</h3>
                  <span>{{ cierres.length }} registros</span>
                </header>
                <div class="stack-list">
                  <div v-for="item in cierres" :key="item.id" class="stack-row">
                    <strong>{{ item.periodo || item.periodoNombre || "Periodo" }}</strong>
                    <small>Bs. {{ money(item.saldoDinero) }} - PV {{ money(item.saldoPv) }} - QP {{ money(item.saldoQp) }}</small>
                  </div>
                  <div v-if="!cierres.length" class="empty-box compact">Sin cierres mensuales.</div>
                </div>
              </article>
            </section>

            <section class="vy-card timeline-card">
              <header>
                <h3>Linea de tiempo</h3>
                <span>{{ timeline.length }} eventos recientes</span>
              </header>
              <div class="timeline-list">
                <article v-for="item in timeline" :key="item.id" class="timeline-item">
                  <span class="timeline-icon"><component :is="item.icon" :size="16" /></span>
                  <div>
                    <small>{{ item.type }} - {{ formatDateTime(item.date) }}</small>
                    <strong>{{ item.title }}</strong>
                    <p>{{ item.subtitle }}</p>
                  </div>
                </article>
                <div v-if="!timeline.length" class="empty-box compact">No hay actividad historica.</div>
              </div>
            </section>
          </template>
        </section>
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace { min-width: 0; padding: 28px 32px 48px; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.page-header h1 { margin-top: 8px; font-size: 30px; font-weight: 800; }
.page-header p { margin-top: 4px; color: var(--vy-ink-2); font-size: 14px; }
.vy-btn { min-height: 40px; padding: 10px 16px; border-radius: 12px; font-weight: 800; display: inline-flex; align-items: center; gap: 8px; }
.vy-btn-ghost { background: var(--vy-surface); border: 1px solid var(--vy-line); color: var(--vy-ink-2); }
.lookup-layout { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 18px; align-items: start; }
.search-panel { position: sticky; top: 18px; padding: 14px; display: grid; gap: 12px; }
.search-field { min-height: 42px; padding: 0 12px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); display: flex; align-items: center; gap: 8px; color: var(--vy-ink-3); }
.search-field input { width: 100%; border: 0; outline: 0; background: transparent; color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 700; }
.person-results { display: grid; gap: 8px; max-height: calc(100vh - 185px); overflow: auto; padding-right: 2px; }
.person-results button { width: 100%; min-height: 58px; padding: 10px; border: 1px solid var(--vy-line); border-radius: 12px; background: #fff; display: flex; align-items: center; gap: 10px; text-align: left; color: var(--vy-ink); }
.person-results button.active, .person-results button:hover { border-color: rgba(242, 135, 5, 0.5); background: #fff8e8; }
.person-results span { min-width: 0; display: grid; gap: 2px; }
.person-results strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; font-weight: 900; }
.person-results small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; }
.detail-panel { min-width: 0; display: grid; gap: 14px; }
.profile-strip { padding: 18px; display: flex; align-items: center; gap: 16px; }
.profile-main { min-width: 0; flex: 1; }
.profile-main h2 { margin-top: 5px; font-size: 24px; font-weight: 900; }
.profile-main p { margin-top: 4px; color: var(--vy-ink-2); font-size: 13px; overflow-wrap: anywhere; }
.profile-status { text-align: right; }
.profile-status strong, .profile-status small { display: block; }
.profile-status strong { font-size: 12px; font-weight: 900; color: var(--vy-success); }
.profile-status small { margin-top: 4px; color: var(--vy-ink-3); font-size: 12px; font-weight: 800; }
.summary-grid, .wallet-grid, .content-grid { display: grid; gap: 14px; }
.summary-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
.wallet-grid { grid-template-columns: repeat(6, minmax(0, 1fr)); }
.content-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.summary-card, .wallet-card { padding: 16px; }
.summary-icon { width: 36px; height: 36px; border-radius: 12px; background: var(--vy-cream); color: var(--vy-orange-deep); display: flex; align-items: center; justify-content: center; margin-bottom: 10px; }
.summary-card small, .wallet-card span, .info-row small { display: block; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.summary-card strong, .wallet-card strong { display: block; margin-top: 5px; font-size: 18px; font-weight: 900; overflow-wrap: anywhere; }
.summary-card p { margin-top: 5px; color: var(--vy-ink-2); font-size: 12px; line-height: 1.35; }
.wallet-card { border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); }
.wallet-card strong { font-size: 20px; }
.info-card, .history-card, .timeline-card { padding: 18px; min-width: 0; }
.info-card header, .history-card header, .timeline-card header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.info-card h3, .history-card h3, .timeline-card h3 { font-size: 16px; font-weight: 900; }
.info-card header span, .history-card header span, .timeline-card header span { color: var(--vy-ink-3); font-size: 12px; font-weight: 900; }
.info-row, .direct-row, .stack-row { padding: 12px; border: 1px solid var(--vy-line); border-radius: 10px; background: var(--vy-surface-2); }
.info-row strong, .direct-row strong, .stack-row strong { display: block; margin-top: 4px; font-size: 13px; font-weight: 900; }
.direct-list, .stack-list, .metrics-list, .timeline-list { display: grid; gap: 8px; }
.direct-row small, .stack-row small { display: block; margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; }
.metrics-list div { padding: 12px; border: 1px solid var(--vy-line); border-radius: 10px; display: flex; align-items: center; justify-content: space-between; gap: 10px; background: var(--vy-surface-2); }
.metrics-list span { color: var(--vy-ink-3); font-size: 12px; font-weight: 900; }
.metrics-list strong { font-size: 13px; font-weight: 900; text-align: right; }
.table-wrap { overflow: auto; }
table { width: 100%; border-collapse: collapse; min-width: 760px; }
th, td { padding: 11px 10px; border-bottom: 1px solid var(--vy-line-2); text-align: left; font-size: 12px; vertical-align: top; }
th { color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; background: var(--vy-surface-2); }
td { color: var(--vy-ink-2); font-weight: 700; }
.status-pill { padding: 4px 9px; border-radius: 999px; background: rgba(63, 143, 92, 0.12); color: var(--vy-success); font-size: 11px; font-weight: 900; }
.timeline-item { display: grid; grid-template-columns: 34px minmax(0, 1fr); gap: 10px; padding: 12px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); }
.timeline-icon { width: 34px; height: 34px; border-radius: 11px; display: flex; align-items: center; justify-content: center; background: var(--vy-ink); color: #fff; }
.timeline-item small { color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.timeline-item strong { display: block; margin-top: 4px; font-size: 13px; font-weight: 900; }
.timeline-item p { margin-top: 3px; color: var(--vy-ink-2); font-size: 12px; line-height: 1.35; }
.empty-state { min-height: 360px; padding: 42px; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; color: var(--vy-ink-3); }
.empty-state h2 { margin-top: 12px; color: var(--vy-ink); font-size: 22px; font-weight: 900; }
.empty-state p { max-width: 420px; margin-top: 7px; font-size: 13px; line-height: 1.45; }
.empty-box, .loading-box, .error-box { padding: 14px 16px; border-radius: 12px; font-size: 13px; font-weight: 800; }
.empty-box { background: var(--vy-surface-2); color: var(--vy-ink-3); text-align: center; }
.empty-box.compact { padding: 12px; }
.loading-box { background: var(--vy-surface-2); color: var(--vy-ink-2); }
.error-box { background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); }
.spinning { animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 1180px) {
  .lookup-layout { grid-template-columns: 300px minmax(0, 1fr); }
  .summary-grid, .wallet-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 860px) {
  .workspace { padding: 24px 20px 112px; }
  .page-header, .profile-strip { align-items: stretch; flex-direction: column; }
  .lookup-layout, .summary-grid, .wallet-grid, .content-grid { grid-template-columns: 1fr; }
  .search-panel { position: static; }
  .person-results { max-height: 320px; }
  .profile-status { text-align: left; }
}
</style>
