<script setup>
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  BadgeCheck,
  CalendarClock,
  Network,
  RefreshCw,
  UserRound,
  Wallet
} from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import { VyAvatar } from "../components/ui.js";
import NetworkTreeNode from "../components/NetworkTreeNode.vue";

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const detailLoading = ref(false);
const error = ref("");
const personas = ref([]);
const usuarios = ref([]);
const referidos = ref([]);
const periodos = ref([]);
const selectedPeriodoId = ref("");
const selectedPersonaId = ref(route.params.personaId || "");
const walletSummary = ref(null);
const compras = ref([]);
const recompensas = ref([]);
const retiros = ref([]);

const selectedPersona = computed(() =>
  personas.value.find((persona) => Number(persona.id) === Number(selectedPersonaId.value)) || null
);

const selectedPersonaPhoto = computed(() => photoUrl(selectedPersona.value));

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
const networkTree = computed(() => buildTree(selectedPersonaId.value));
const networkRows = computed(() => flattenTree(networkTree.value));

const selectedPeriodo = computed(() =>
  periodos.value.find((periodo) => Number(periodo.id) === Number(selectedPeriodoId.value)) || null
);

const billetera = computed(() => walletSummary.value?.billetera || {});
const movimientos = computed(() => walletSummary.value?.movimientos || []);
const membresias = computed(() => (walletSummary.value?.membresias || []).filter(membershipInSelectedPeriod));
const cierres = computed(() => (walletSummary.value?.cierresMensuales || []).filter(cierreInSelectedPeriod));
const periodoActivo = computed(() => selectedPeriodo.value || walletSummary.value?.periodoActivo || null);
const movimientoGroups = computed(() => {
  const groups = new Map();

  movimientos.value.forEach((movimiento) => {
    // Agrupa por compra cualquier movimiento con compra asociada
    // (volumen propio/red, beneficios de activacion y sus ajustes)
    const compraAsociada = movimiento.compra || null;
    const referenciaTipo = compraAsociada ? "COMPRA" : (movimiento.referenciaTipo || "MOVIMIENTO");
    const referenciaId = compraAsociada ? compraAsociada.id : (movimiento.referenciaId || movimiento.id);
    const key = `${referenciaTipo}-${referenciaId}`;

    if (!groups.has(key)) {
      groups.set(key, {
        key,
        referenciaTipo,
        referenciaId,
        title: movimientoTitle(movimiento),
        concepts: new Set(),
        date: movimiento.fechaRegistro,
        compra: compraAsociada,
        dinero: 0,
        pv: 0,
        qp: 0,
        cr: 0,
        productos: 0
      });
    }

    const group = groups.get(key);
    group.date = newestDate(group.date, movimiento.fechaRegistro);
    group.compra = group.compra || movimiento.compra || null;
    if (movimiento.concepto) {
      group.concepts.add(movimiento.concepto);
    }

    const amount = Number(movimiento.monto || 0);
    if (movimiento.tipo === "DINERO") group.dinero += amount;
    if (movimiento.tipo === "PV") group.pv += amount;
    if (movimiento.tipo === "QP") group.qp += amount;
    if (movimiento.tipo === "CR") group.cr += amount;
    if (movimiento.tipo === "PRODUCTOS") group.productos += amount;
  });

  return Array.from(groups.values())
    .map((group) => ({
      ...group,
      subtitle: group.compra
        ? compraProductosMovimiento(group.compra) || Array.from(group.concepts).join(" / ")
        : Array.from(group.concepts).join(" / ")
    }))
    .sort((left, right) => new Date(right.date || 0) - new Date(left.date || 0));
});

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
  recompensasProductos: recompensas.value.reduce((sum, item) => sum + Number(item.valorProductos || 0), 0),
  retirosDinero: retiros.value.reduce((sum, item) => sum + Number(item.montoDinero || 0), 0),
  retirosProductos: retiros.value.reduce((sum, item) => sum + Number(item.montoProductos || 0), 0)
}));

const retiroMensual = computed(() => ({
  realizado: retiros.value.length > 0,
  cantidad: retiros.value.length,
  dinero: totals.value.retirosDinero,
  productos: totals.value.retirosProductos,
  ultimo: retiros.value[0] || null
}));

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

function usuarioDePersona(persona) {
  return usuarios.value.find((item) => Number(item.persona?.id) === Number(persona?.id)) || null;
}

function childrenOf(personaId) {
  if (!personaId) return [];
  return referidos.value.filter((item) => Number(item.patrocinador?.id) === Number(personaId));
}

function buildTree(rootPersonaId, visited = new Set()) {
  const personaId = Number(rootPersonaId || 0);
  if (!personaId || visited.has(personaId)) return [];
  visited.add(personaId);

  return childrenOf(personaId).map((item) => ({
    ...item,
    activo: esActivo(item),
    aporte: aportesPorPersona.value.get(Number(item.persona?.id)) || null,
    children: buildTree(item.persona?.id, new Set(visited))
  }));
}

/** Referido activo: membresia vigente al dia de hoy (misma regla que /red). */
function esActivo(referido) {
  if (!referido || referido.estado !== "ACTIVO") return false;
  if (!referido.persona || referido.persona.estado !== "ACTIVO") return false;
  if (!Boolean(referido.membresiaActiva) || !referido.fechaFinMembresia) return false;
  return new Date(referido.fechaFinMembresia).getTime() >= Date.now();
}

/** Cuanto aportó cada comprador a la persona consultada en el mes consultado (PV/QP/dinero). */
const aportesPorPersona = computed(() => {
  const mapa = new Map();
  movimientos.value.forEach((movimiento) => {
    // La compra expone al comprador plano: compra.personaId
    const compradorId = Number(movimiento.compra?.personaNombres !== undefined
      ? movimiento.compra?.personaId
      : movimiento.compra?.persona?.id || 0);
    if (!compradorId) return;
    const entrada = mapa.get(compradorId) || { pv: 0, qp: 0, dinero: 0 };
    const amount = Number(movimiento.monto || 0);
    if (movimiento.tipo === "PV") entrada.pv += amount;
    if (movimiento.tipo === "QP") entrada.qp += amount;
    if (movimiento.tipo === "DINERO") entrada.dinero += amount;
    mapa.set(compradorId, entrada);
  });
  return mapa;
});

function flattenTree(nodes, level = 1) {
  return nodes.flatMap((node) => [
    {
      ...node,
      level,
      directCount: node.children?.length || 0
    },
    ...flattenTree(node.children || [], level + 1)
  ]);
}

function selectTreePerson(node) {
  const personaId = node?.persona?.id;
  if (!personaId || Number(personaId) === Number(selectedPersonaId.value)) return;
  router.push({ name: "consultar-detalle", params: { personaId } });
}

function photoUrl(persona) {
  const photo = persona?.fotoPerfil || usuarioDePersona(persona)?.fotoPerfil || "";
  if (!photo) return "";
  if (photo.startsWith("http") || photo.startsWith("blob:")) return photo;
  return photo.startsWith("/") ? photo : `/${photo}`;
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

function dateOnly(value) {
  if (!value) return null;
  const [datePart] = String(value).split("T");
  const [year, month, day] = datePart.split("-").map(Number);
  if (!year || !month || !day) return null;
  return new Date(year, month - 1, day);
}

function periodMonthKey(periodo) {
  if (!periodo?.gestion?.anio || !periodo?.mes) return "";
  return `${periodo.gestion.anio}-${String(periodo.mes).padStart(2, "0")}`;
}

function membershipInSelectedPeriod(membership) {
  if (!selectedPeriodo.value) return true;
  if (membership?.periodo?.id && Number(membership.periodo.id) === Number(selectedPeriodoId.value)) return true;

  const start = dateOnly(membership?.fechaInicio);
  const end = dateOnly(membership?.fechaFin);
  const periodStart = dateOnly(selectedPeriodo.value.fechaInicio);
  const periodEnd = dateOnly(selectedPeriodo.value.fechaFin);
  if (!start || !end || !periodStart || !periodEnd) return false;
  return start <= periodEnd && end >= periodStart;
}

function cierreInSelectedPeriod(cierre) {
  if (!selectedPeriodo.value) return true;
  if (cierre?.periodoGestion?.id && Number(cierre.periodoGestion.id) === Number(selectedPeriodoId.value)) return true;
  return cierre?.periodo === periodMonthKey(selectedPeriodo.value);
}

function membershipName(membership) {
  return membership?.nombreActivacion || membership?.plan?.nombre || "Plan";
}

function compraProductos(compra) {
  return (compra.detalles || [])
    .map((detalle) => `${detalle.producto?.nombre || "Producto"} x${detalle.cantidad || 0}`)
    .join(", ");
}

function compraProductosMovimiento(compra) {
  return (compra.detalles || [])
    .map((detalle) => `${detalle.productoNombre || detalle.producto?.nombre || "Producto"} x${detalle.cantidad || 0}`)
    .join(", ");
}

function newestDate(left, right) {
  if (!left) return right;
  if (!right) return left;
  return new Date(right) > new Date(left) ? right : left;
}

function movimientoTitle(movimiento) {
  const reference = movimiento.referenciaId ? ` #${movimiento.referenciaId}` : "";
  const labels = {
    COMPRA: "Compra interna",
    VENTA_PUBLICA: "Venta publica",
    REFERIDO_AFILIACION: "Afiliacion",
    RECOMPENSA: "Recompensa",
    RETIRO_BILLETERA: "Retiro de billetera",
    CIERRE_MENSUAL: "Cierre mensual",
    MOVIMIENTO: "Movimiento de billetera"
  };
  return `${labels[movimiento.referenciaTipo] || movimiento.referenciaTipo || "Movimiento de billetera"}${reference}`;
}

function movementGroupSummary(group) {
  return [
    group.dinero ? `Dinero Bs. ${money(group.dinero)}` : "",
    group.productos ? `Productos Bs. ${money(group.productos)}` : "",
    group.pv ? `PV ${money(group.pv)}` : "",
    group.qp ? `QP ${money(group.qp)}` : "",
    group.cr ? `CR ${money(group.cr)}` : ""
  ].filter(Boolean).join(" - ") || group.subtitle || "Sin valores";
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

async function loadBaseData() {
  loading.value = true;
  error.value = "";

  try {
    const [personasData, usuariosData, referidosData] = await Promise.all([
      apiRequest("/api/personas"),
      apiRequest("/api/usuarios"),
      apiRequest("/api/referidos"),
      loadPeriodos()
    ]);
    personas.value = Array.isArray(personasData) ? personasData : [];
    usuarios.value = Array.isArray(usuariosData) ? usuariosData : [];
    referidos.value = Array.isArray(referidosData) ? referidosData : [];
    await loadPersonaDetail();
  } catch (exception) {
    error.value = exception.message || "No se pudieron cargar las personas.";
  } finally {
    loading.value = false;
  }
}

async function refreshAll() {
  await loadBaseData();
}

async function loadPersonaDetail() {
  if (!selectedPersonaId.value) return;

  detailLoading.value = true;
  error.value = "";

  try {
    if (!selectedPeriodoId.value) {
      await loadPeriodos();
    }

    const queryString = selectedPeriodoId.value ? `?periodoId=${selectedPeriodoId.value}` : "";
    const [walletResult, comprasResult, recompensasResult, retirosResult] = await Promise.allSettled([
      apiRequest(`/api/billeteras/persona/${selectedPersonaId.value}${queryString}`),
      apiRequest(`/api/compras/persona/${selectedPersonaId.value}${queryString}`),
      apiRequest(`/api/recompensas/persona/${selectedPersonaId.value}${queryString}`),
      apiRequest(`/api/billeteras/persona/${selectedPersonaId.value}/retiros${queryString}`)
    ]);

    if (walletResult.status === "fulfilled") {
      walletSummary.value = walletResult.value || null;
    }
    if (comprasResult.status === "fulfilled") {
      compras.value = Array.isArray(comprasResult.value) ? comprasResult.value : [];
    }
    if (recompensasResult.status === "fulfilled") {
      recompensas.value = Array.isArray(recompensasResult.value) ? recompensasResult.value : [];
    }
    if (retirosResult.status === "fulfilled") {
      retiros.value = Array.isArray(retirosResult.value) ? retirosResult.value : [];
    }

    if ([walletResult, comprasResult, recompensasResult, retirosResult].some((result) => result.status === "rejected")) {
      error.value = "Algunos datos no se pudieron cargar, pero la consulta disponible se mantiene visible.";
    }
  } finally {
    detailLoading.value = false;
  }
}

watch(selectedPeriodoId, async (newValue, oldValue) => {
  if (newValue && oldValue && selectedPersonaId.value) {
    await loadPersonaDetail();
  }
});

watch(() => route.params.personaId, async (value) => {
  selectedPersonaId.value = value || "";
  await loadPersonaDetail();
});

onMounted(loadBaseData);
</script>

<template>
  <div class="vy consult-view">
    <main class="workspace">
      <header class="page-header">
        <div>
          <div class="vy-eyebrow">Consultar</div>
          <h1>Vista completa de persona</h1>
          <p>Busca una persona y revisa sus datos, red, billetera e historial del mes seleccionado.</p>
        </div>
        <div class="header-actions">
          <label class="period-filter">
            <span>Mes</span>
            <select v-model="selectedPeriodoId">
              <option value="" disabled>Selecciona un mes</option>
              <option v-for="periodo in periodos" :key="periodo.id" :value="periodo.id">
                {{ periodo.nombre }} - Gestion {{ periodo.gestion?.anio || "" }}
              </option>
            </select>
          </label>
          <button class="vy-btn vy-btn-ghost" type="button" :disabled="loading || detailLoading" @click="refreshAll">
            <RefreshCw :class="{ spinning: loading || detailLoading }" :size="15" />
            Actualizar
          </button>
          <button class="vy-btn vy-btn-dark" type="button" @click="router.push({ name: 'consultar' })">
            <UserRound :size="15" />
            Otra consulta
          </button>
        </div>
      </header>

      <section class="detail-panel">
          <div v-if="error" class="error-box">{{ error }}</div>

          <section v-if="!selectedPersona" class="vy-card empty-state">
            <UserRound :size="30" />
            <h2>Persona no encontrada</h2>
            <p>Vuelve a la busqueda y selecciona una persona disponible.</p>
          </section>

          <template v-else>
            <section class="profile-strip vy-card">
              <img v-if="selectedPersonaPhoto" class="profile-photo" :src="selectedPersonaPhoto" :alt="fullName(selectedPersona)" />
              <VyAvatar v-else :name="fullName(selectedPersona).slice(0, 2).toUpperCase()" :size="64" bg="var(--vy-ink)" color="#fff" />
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
                <small>Mes consultado</small>
                <strong>{{ periodoActivo?.nombre || "Sin periodo" }}</strong>
                <p>{{ periodoActivo?.gestion?.anio || "" }}</p>
              </article>
              <article class="summary-card vy-card" :class="{ warning: !retiroMensual.realizado }">
                <span class="summary-icon"><Wallet :size="18" /></span>
                <small>Retiro mensual</small>
                <strong>{{ retiroMensual.realizado ? "Realizado" : "Pendiente" }}</strong>
                <p>{{ retiroMensual.realizado ? `${retiroMensual.cantidad} retiro(s) por Bs. ${money(retiroMensual.dinero)}` : "No registra retiro en este mes." }}</p>
              </article>
            </section>

            <section class="wallet-grid">
              <article v-for="card in walletCards" :key="card.label" class="wallet-card">
                <span>{{ card.label }}</span>
                <strong>{{ card.value }}</strong>
              </article>
            </section>

            <section class="vy-card history-card">
              <header>
                <h3>Movimientos de billetera</h3>
                <span>{{ movimientoGroups.length }} eventos agrupados</span>
              </header>
              <div class="table-wrap">
                <table class="movement-table">
                  <thead>
                    <tr>
                      <th>Movimiento</th>
                      <th>Detalle</th>
                      <th>Fecha</th>
                      <th>Efectivo</th>
                      <th>Productos</th>
                      <th>PV</th>
                      <th>QP</th>
                      <th>CR</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="group in movimientoGroups" :key="group.key">
                      <td>
                        <strong>{{ group.title }}</strong>
                        <small>{{ group.referenciaTipo }} #{{ group.referenciaId }}</small>
                      </td>
                      <td>{{ group.subtitle || "Sin detalle" }}</td>
                      <td>{{ formatDateTime(group.date) }}</td>
                      <td>Bs. {{ money(group.dinero) }}</td>
                      <td>Bs. {{ money(group.productos) }}</td>
                      <td>{{ money(group.pv) }}</td>
                      <td>{{ money(group.qp) }}</td>
                      <td>{{ money(group.cr) }}</td>
                    </tr>
                    <tr v-if="!movimientoGroups.length">
                      <td colspan="8">No hay movimientos de billetera para el mes seleccionado.</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <section class="content-grid">
              <article class="vy-card info-card network-tree-card">
                <header>
                  <h3>Red</h3>
                  <span>{{ directos.length }} directos - {{ networkRows.length }} en estructura</span>
                </header>
                <div class="info-row">
                  <small>Patrocinador</small>
                  <strong>{{ patrocinador ? fullName(patrocinador) : "Sin patrocinador" }}</strong>
                </div>
                <div v-if="directos.length" class="consult-tree-stage">
                  <div class="consult-tree-canvas">
                    <div class="consult-root-row">
                      <article class="consult-root-node">
                        <span class="root-label">Raiz</span>
                        <img v-if="selectedPersonaPhoto" :src="selectedPersonaPhoto" :alt="fullName(selectedPersona)" />
                        <VyAvatar v-else :name="fullName(selectedPersona).slice(0, 2).toUpperCase()" :size="42" bg="var(--vy-orange)" color="#fff" />
                        <div>
                          <strong>{{ fullName(selectedPersona) }}</strong>
                          <small>{{ referido?.plan?.nombre || "Sin plan" }} - {{ directos.length }} directos</small>
                        </div>
                      </article>
                    </div>
                    <ul class="consult-tree-root">
                      <NetworkTreeNode
                        v-for="node in networkTree"
                        :key="node.id"
                        :node="node"
                        :level="1"
                        @open-details="selectTreePerson"
                      />
                    </ul>
                  </div>
                </div>
                <div v-else class="empty-box compact">No tiene referidos directos.</div>
              </article>

              <article class="vy-card info-card">
                <header>
                  <h3>Totales del mes</h3>
                  <span>{{ compras.length }} compras del mes</span>
                </header>
                <div class="metrics-list">
                  <div><span>Compras</span><strong>Bs. {{ money(totals.compras) }}</strong></div>
                  <div><span>PV compras</span><strong>{{ money(totals.pv) }}</strong></div>
                  <div><span>QP compras</span><strong>{{ money(totals.qp) }}</strong></div>
                  <div><span>Recompensas efectivo</span><strong>Bs. {{ money(totals.recompensasEfectivo) }}</strong></div>
                  <div><span>Recompensas productos</span><strong>Bs. {{ money(totals.recompensasProductos) }}</strong></div>
                  <div><span>Retiros efectivo</span><strong>Bs. {{ money(totals.retirosDinero) }}</strong></div>
                  <div><span>Retiros productos</span><strong>Bs. {{ money(totals.retirosProductos) }}</strong></div>
                </div>
              </article>
            </section>

            <section class="vy-card history-card">
              <header>
                <h3>Retiros mensuales</h3>
                <span>{{ retiroMensual.realizado ? `${retiroMensual.cantidad} realizado(s)` : "Pendiente" }}</span>
              </header>
              <div class="table-wrap">
                <table>
                  <thead>
                    <tr>
                      <th>Fecha</th>
                      <th>Estado</th>
                      <th>Efectivo</th>
                      <th>Productos</th>
                      <th>Observacion</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="retiro in retiros" :key="retiro.id">
                      <td>{{ formatDateTime(retiro.fechaRetiro) }}</td>
                      <td><span class="status-pill">{{ retiro.estadoRetiro }}</span></td>
                      <td>Bs. {{ money(retiro.montoDinero) }}</td>
                      <td>Bs. {{ money(retiro.montoProductos) }}</td>
                      <td>{{ retiro.observacion || "Sin observacion" }}</td>
                    </tr>
                    <tr v-if="!retiros.length">
                      <td colspan="5">No realizo retiros en el mes seleccionado.</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </section>

            <section class="vy-card history-card">
              <header>
                <h3>Compras internas</h3>
                <span>{{ compras.length }} registros del mes</span>
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
                <span>{{ recompensas.length }} registros del mes</span>
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
          </template>
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace { min-width: 0; padding: 28px 32px 48px; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 18px; }
.page-header h1 { margin-top: 8px; font-size: 30px; font-weight: 800; }
.page-header p { margin-top: 4px; color: var(--vy-ink-2); font-size: 14px; }
.header-actions { display: flex; align-items: flex-end; gap: 10px; flex-wrap: wrap; }
.vy-btn { min-height: 40px; padding: 10px 16px; border-radius: 12px; font-weight: 800; display: inline-flex; align-items: center; gap: 8px; }
.vy-btn-ghost { background: var(--vy-surface); border: 1px solid var(--vy-line); color: var(--vy-ink-2); }
.vy-btn-dark { background: var(--vy-ink); color: #fff; }
.period-filter { min-width: 230px; display: grid; gap: 6px; }
.period-filter span { color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.period-filter select { width: 100%; min-height: 40px; padding: 9px 12px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface); color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 800; }
.detail-panel { min-width: 0; display: grid; gap: 14px; }
.profile-strip { padding: 18px; display: flex; align-items: center; gap: 16px; }
.profile-photo { width: 64px; height: 64px; border-radius: 50%; object-fit: cover; flex-shrink: 0; border: 2px solid #fff; box-shadow: var(--vy-shadow-sm); background: var(--vy-surface-2); }
.profile-main { min-width: 0; flex: 1; }
.profile-main h2 { margin-top: 5px; font-size: 24px; font-weight: 900; }
.profile-main p { margin-top: 4px; color: var(--vy-ink-2); font-size: 13px; overflow-wrap: anywhere; }
.profile-status { text-align: right; }
.profile-status strong, .profile-status small { display: block; }
.profile-status strong { font-size: 12px; font-weight: 900; color: var(--vy-success); }
.profile-status small { margin-top: 4px; color: var(--vy-ink-3); font-size: 12px; font-weight: 800; }
.summary-grid, .wallet-grid, .content-grid { display: grid; gap: 14px; }
.summary-grid { grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); }
.wallet-grid { grid-template-columns: repeat(6, minmax(0, 1fr)); }
.content-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.summary-card, .wallet-card { padding: 16px; }
.summary-icon { width: 36px; height: 36px; border-radius: 12px; background: var(--vy-cream); color: var(--vy-orange-deep); display: flex; align-items: center; justify-content: center; margin-bottom: 10px; }
.summary-card small, .wallet-card span, .info-row small { display: block; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.summary-card strong, .wallet-card strong { display: block; margin-top: 5px; font-size: 18px; font-weight: 900; overflow-wrap: anywhere; }
.summary-card p { margin-top: 5px; color: var(--vy-ink-2); font-size: 12px; line-height: 1.35; }
.summary-card.warning .summary-icon { background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); }
.summary-card.warning strong { color: var(--vy-danger); }
.wallet-card { border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); }
.wallet-card strong { font-size: 20px; }
.info-card, .history-card { padding: 18px; min-width: 0; }
.network-tree-card { grid-column: 1 / -1; }
.info-card header, .history-card header { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 14px; }
.info-card h3, .history-card h3 { font-size: 16px; font-weight: 900; }
.info-card header span, .history-card header span { color: var(--vy-ink-3); font-size: 12px; font-weight: 900; }
.info-row, .stack-row { padding: 12px; border: 1px solid var(--vy-line); border-radius: 10px; background: var(--vy-surface-2); }
.info-row strong, .stack-row strong { display: block; margin-top: 4px; font-size: 13px; font-weight: 900; }
.stack-list, .metrics-list { display: grid; gap: 8px; }
.stack-row small { display: block; margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; }
.consult-tree-stage { width: 100%; max-width: 100%; margin-top: 10px; overflow: auto; padding: 8px 4px 12px; max-height: 420px; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); scrollbar-width: thin; scrollbar-color: rgba(242, 135, 5, 0.52) rgba(214, 204, 188, 0.35); }
.consult-tree-canvas { width: max-content; min-width: 100%; display: flex; flex-direction: column; align-items: center; padding: 4px 0 8px; }
.consult-root-row { width: 100%; min-width: max-content; display: flex; justify-content: center; margin-bottom: 28px; position: relative; }
.consult-root-row::after { content: ""; position: absolute; left: 50%; bottom: -28px; height: 22px; border-left: 1px solid rgba(242, 135, 5, 0.62); }
.consult-root-node { width: 300px; min-height: 76px; padding: 12px; border-radius: 8px; background: var(--vy-ink); color: #fff; display: flex; align-items: center; gap: 10px; box-shadow: 0 12px 24px rgba(31, 26, 20, 0.1); }
.consult-root-node img { width: 42px; height: 42px; border-radius: 50%; object-fit: cover; border: 2px solid rgba(255, 255, 255, 0.78); background: var(--vy-surface-2); }
.consult-root-node > div { min-width: 0; flex: 1; }
.consult-root-node strong, .consult-root-node small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.consult-root-node strong { font-size: 13px; font-weight: 900; }
.consult-root-node small { margin-top: 2px; color: rgba(255, 255, 255, 0.7); font-size: 11px; font-weight: 800; }
.root-label { align-self: flex-start; padding: 3px 7px; border-radius: 999px; background: rgba(255, 255, 255, 0.12); color: rgba(255, 255, 255, 0.82); font-size: 10px; font-weight: 900; text-transform: uppercase; }
.consult-tree-root { display: flex; justify-content: center; gap: 16px; min-width: max-content; margin: 0; padding: 24px 0 0; list-style: none; position: relative; }
.consult-tree-root > :deep(.tree-item)::before { content: ""; position: absolute; top: -24px; left: 50%; height: 24px; border-left: 1px solid rgba(242, 135, 5, 0.5); }
.consult-tree-root > :deep(.tree-item)::after { content: ""; position: absolute; top: -24px; left: 0; right: 0; border-top: 1px solid rgba(242, 135, 5, 0.5); }
.consult-tree-root > :deep(.tree-item:first-child)::after { left: 50%; }
.consult-tree-root > :deep(.tree-item:last-child)::after { right: 50%; }
.consult-tree-root > :deep(.tree-item:only-child)::after { display: none; }
.metrics-list div { padding: 12px; border: 1px solid var(--vy-line); border-radius: 10px; display: flex; align-items: center; justify-content: space-between; gap: 10px; background: var(--vy-surface-2); }
.metrics-list span { color: var(--vy-ink-3); font-size: 12px; font-weight: 900; }
.metrics-list strong { font-size: 13px; font-weight: 900; text-align: right; }
.table-wrap { overflow: auto; }
table { width: 100%; border-collapse: collapse; min-width: 760px; }
th, td { padding: 11px 10px; border-bottom: 1px solid var(--vy-line-2); text-align: left; font-size: 12px; vertical-align: top; }
th { color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; background: var(--vy-surface-2); }
td { color: var(--vy-ink-2); font-weight: 700; }
.movement-table { min-width: 980px; }
.movement-table td:first-child strong, .movement-table td:first-child small { display: block; }
.movement-table td:first-child strong { color: var(--vy-ink); font-weight: 900; }
.movement-table td:first-child small { margin-top: 3px; color: var(--vy-ink-3); font-size: 11px; font-weight: 800; }
.movement-table td:nth-child(n + 4) { white-space: nowrap; font-weight: 900; }
.status-pill { padding: 4px 9px; border-radius: 999px; background: rgba(63, 143, 92, 0.12); color: var(--vy-success); font-size: 11px; font-weight: 900; }
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
  .summary-grid, .wallet-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (max-width: 860px) {
  .workspace { padding: 24px 20px 112px; }
  .page-header, .header-actions, .profile-strip { align-items: stretch; flex-direction: column; }
  .period-filter, .header-actions .vy-btn { width: 100%; }
  .summary-grid, .wallet-grid, .content-grid { grid-template-columns: 1fr; }
  .profile-status { text-align: left; }
}
</style>
