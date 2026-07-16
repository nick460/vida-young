<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { ArrowLeft, CircleMinus, Minus, Plus, Send, ShoppingCart } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import { clearPublicCart, readPublicCart, writePublicCart } from "../services/publicCartService.js";

const route = useRoute();
const router = useRouter();
const username = computed(() => String(route.params.username || ""));
const items = ref([]);
const tiposCliente = ref([]);
const loading = ref(false);
const error = ref("");

const form = reactive({
  tipoClienteCodigo: "NORMAL",
  clienteNombres: "",
  clienteApellidos: "",
  clienteDocumento: "",
  clienteEmail: "",
  clienteTelefono: "",
  envioRequiere: false,
  envioDireccion: "",
  envioCiudad: "",
  envioReferencia: "",
  metodoPago: "TRANSFERENCIA",
  referenciaPago: ""
});

const total = computed(() => items.value.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0));
const totalEmpresa = computed(() => items.value.reduce((sum, item) => sum + Number(item.distributorPrice || 0) * Number(item.quantity || 0), 0));
const totalDescuento = computed(() => items.value.reduce((sum, item) => sum + Number(item.discount || 0) * Number(item.quantity || 0), 0));
const gananciaDistribuidor = computed(() => Math.max(0, total.value - totalEmpresa.value));

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function persist() {
  writePublicCart(username.value, items.value);
}

function increment(item) {
  item.quantity = Number(item.quantity || 0) + 1;
  persist();
}

function decrement(item) {
  item.quantity = Math.max(1, Number(item.quantity || 0) - 1);
  persist();
}

function removeItem(item) {
  items.value = items.value.filter((current) => Number(current.id) !== Number(item.id));
  persist();
}

function validate() {
  if (!items.value.length) return "Agrega al menos un producto.";
  if (!form.clienteNombres.trim() || !form.clienteApellidos.trim()) return "Ingresa nombre y apellido.";
  if (!form.clienteTelefono.trim() && !form.clienteEmail.trim()) return "Ingresa telefono o correo de contacto.";
  if (form.envioRequiere && (!form.envioDireccion.trim() || !form.envioCiudad.trim())) return "Ingresa direccion y ciudad de envio.";
  return "";
}

async function checkout() {
  error.value = validate();
  if (error.value) return;

  loading.value = true;
  try {
    const response = await apiRequest(`/api/public/tiendas/${encodeURIComponent(username.value)}/compras`, {
      method: "POST",
      body: JSON.stringify({
        ...form,
        items: items.value.map((item) => ({
          productoId: Number(item.id),
          cantidad: Number(item.quantity || 1)
        }))
      })
    });

    clearPublicCart(username.value);
    items.value = [];
    await Swal.fire({
      title: "Pedido registrado",
      html: `Pedido #<b>${response.id}</b><br>Total: <b>Bs. ${money(response.totalCliente)}</b><br>Queda pendiente de validacion.`,
      icon: "success",
      confirmButtonColor: "#F28705"
    });
    router.push({ name: "tienda-publica", params: { username: username.value } });
  } catch (exception) {
    error.value = exception.message || "No se pudo registrar el pedido.";
  } finally {
    loading.value = false;
  }
}

async function load() {
  items.value = readPublicCart(username.value);
  try {
    tiposCliente.value = await apiRequest("/api/public/tipos-cliente");
  } catch {
    tiposCliente.value = [];
  }
}

onMounted(load);
</script>

<template>
  <div class="vy public-cart-view">
    <main class="workspace">
      <header class="page-header">
        <button type="button" class="back-button" @click="router.push({ name: 'tienda-publica', params: { username } })">
          <ArrowLeft :size="16" />
        </button>
        <div>
          <div class="vy-eyebrow">Carrito publico</div>
          <h1>Confirmar pedido</h1>
          <p>Completa tus datos y los datos de envio si corresponde.</p>
        </div>
      </header>

      <div v-if="error" class="error-box">{{ error }}</div>

      <section v-if="items.length" class="cart-layout">
        <article class="vy-card cart-list">
          <div v-for="item in items" :key="item.id" class="cart-row">
            <div class="cart-info">
              <small>{{ item.sku }} - {{ item.cat || "Producto" }}</small>
              <strong>{{ item.name }}</strong>
              <span>Bs. {{ money(item.price) }} <template v-if="item.discount">- descuento Bs. {{ money(item.discount) }}</template></span>
            </div>
            <div class="quantity-box">
              <button type="button" @click="decrement(item)"><Minus :size="13" /></button>
              <b>{{ item.quantity }}</b>
              <button type="button" @click="increment(item)"><Plus :size="13" /></button>
            </div>
            <strong class="line-total">Bs. {{ money(Number(item.price || 0) * Number(item.quantity || 0)) }}</strong>
            <button type="button" class="remove-button" @click="removeItem(item)">
              <CircleMinus :size="16" />
            </button>
          </div>
        </article>

        <aside class="vy-card summary-card">
          <div class="summary-icon"><ShoppingCart :size="18" /></div>
          <h2>Resumen</h2>
          <div class="summary-line"><span>Total cliente</span><b>Bs. {{ money(total) }}</b></div>
          <div class="summary-line"><span>Base empresa</span><b>Bs. {{ money(totalEmpresa) }}</b></div>
          <div class="summary-line"><span>Descuentos</span><b>Bs. {{ money(totalDescuento) }}</b></div>
          <div class="summary-total"><span>Para distribuidor</span><strong>Bs. {{ money(gananciaDistribuidor) }}</strong></div>
          <p>El sistema conserva el descuento aplicado para justificar la ganancia final del distribuidor.</p>
        </aside>

        <form class="vy-card checkout-card" @submit.prevent="checkout">
          <div class="form-grid">
            <label>
              <span>Tipo de cliente</span>
              <select v-model="form.tipoClienteCodigo">
                <option v-for="tipo in tiposCliente" :key="tipo.id" :value="tipo.codigo">{{ tipo.nombre }}</option>
              </select>
            </label>
            <label>
              <span>Nombres</span>
              <input v-model.trim="form.clienteNombres" />
            </label>
            <label>
              <span>Apellidos</span>
              <input v-model.trim="form.clienteApellidos" />
            </label>
            <label>
              <span>Documento</span>
              <input v-model.trim="form.clienteDocumento" />
            </label>
            <label>
              <span>Telefono</span>
              <input v-model.trim="form.clienteTelefono" />
            </label>
            <label>
              <span>Correo</span>
              <input v-model.trim="form.clienteEmail" type="email" />
            </label>
            <label>
              <span>Metodo de pago</span>
              <select v-model="form.metodoPago">
                <option value="TRANSFERENCIA">Transferencia</option>
                <option value="QR">QR</option>
                <option value="CAJA">Caja</option>
              </select>
            </label>
            <label>
              <span>Referencia de pago</span>
              <input v-model.trim="form.referenciaPago" placeholder="Numero de operacion, nota o codigo" />
            </label>
            <label class="toggle-field full-field">
              <input v-model="form.envioRequiere" type="checkbox" />
              <span>Requiere envio</span>
            </label>
            <template v-if="form.envioRequiere">
              <label>
                <span>Ciudad</span>
                <input v-model.trim="form.envioCiudad" />
              </label>
              <label>
                <span>Direccion</span>
                <input v-model.trim="form.envioDireccion" />
              </label>
              <label class="full-field">
                <span>Referencia de envio</span>
                <textarea v-model.trim="form.envioReferencia" rows="3" />
              </label>
            </template>
          </div>
          <button class="vy-btn vy-btn-primary checkout-button" type="submit" :disabled="loading">
            <Send :size="16" /> Registrar pedido
          </button>
        </form>
      </section>

      <section v-else class="vy-card empty-cart">
        <ShoppingCart :size="28" />
        <h2>Tu carrito esta vacio</h2>
        <button class="vy-btn vy-btn-primary" type="button" @click="router.push({ name: 'tienda-publica', params: { username } })">Ir a tienda</button>
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace { padding: 28px 32px 40px; min-width: 0; }
.page-header { display: flex; align-items: center; gap: 14px; margin-bottom: 22px; }
.page-header h1 { margin-top: 6px; font-size: 30px; font-weight: 900; }
.page-header p { margin-top: 4px; color: var(--vy-ink-2); font-size: 14px; }
.back-button { width: 38px; height: 38px; border-radius: 50%; border: 1px solid var(--vy-line); background: var(--vy-surface); display: inline-flex; align-items: center; justify-content: center; }
.error-box { margin-bottom: 14px; padding: 13px 15px; border-radius: 12px; background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); font-weight: 800; }
.cart-layout { display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.7fr); gap: 16px; align-items: start; }
.cart-list, .summary-card, .checkout-card, .empty-cart { padding: 20px; }
.cart-row { display: grid; grid-template-columns: minmax(0, 1fr) auto auto auto; align-items: center; gap: 14px; padding: 14px 0; border-top: 1px solid var(--vy-line-2); }
.cart-row:first-child { border-top: 0; padding-top: 0; }
.cart-info small, .cart-info span { display: block; color: var(--vy-ink-3); font-size: 12px; font-weight: 800; }
.cart-info strong { display: block; margin: 5px 0; font-size: 15px; font-weight: 900; }
.quantity-box { display: inline-flex; align-items: center; gap: 8px; padding: 5px; border: 1px solid var(--vy-line); border-radius: 999px; background: var(--vy-surface-2); }
.quantity-box button { width: 26px; height: 26px; border-radius: 50%; background: #fff; display: inline-flex; align-items: center; justify-content: center; }
.quantity-box b { min-width: 24px; text-align: center; }
.line-total { white-space: nowrap; }
.remove-button { width: 34px; height: 34px; border-radius: 10px; background: rgba(196, 69, 42, 0.08); color: var(--vy-danger); display: inline-flex; align-items: center; justify-content: center; }
.summary-icon { width: 42px; height: 42px; border-radius: 14px; background: var(--vy-cream); color: var(--vy-orange-deep); display: flex; align-items: center; justify-content: center; margin-bottom: 14px; }
.summary-card h2 { font-size: 18px; font-weight: 900; margin-bottom: 14px; }
.summary-line, .summary-total { display: flex; justify-content: space-between; gap: 12px; padding: 10px 0; border-top: 1px solid var(--vy-line-2); }
.summary-total strong { font-family: var(--font-display); font-size: 22px; }
.summary-card p { margin-top: 12px; color: var(--vy-ink-3); font-size: 12px; font-weight: 700; line-height: 1.45; }
.checkout-card { grid-column: 1 / -1; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
label span { display: block; margin-bottom: 7px; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
input, select, textarea { width: 100%; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 800; outline: 0; }
input, select { min-height: 42px; padding: 0 12px; }
textarea { padding: 12px; resize: vertical; }
.full-field { grid-column: 1 / -1; }
.toggle-field { display: inline-flex; align-items: center; gap: 10px; }
.toggle-field input { width: 18px; min-height: 18px; }
.toggle-field span { margin: 0; }
.checkout-button { width: 100%; margin-top: 16px; min-height: 46px; border-radius: 12px; font-weight: 900; }
.vy-btn-primary { background: var(--vy-orange); color: #fff; }
.empty-cart { min-height: 280px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; text-align: center; color: var(--vy-ink-2); }
@media (max-width: 980px) { .workspace { padding: 24px 20px 32px; } .cart-layout, .cart-row, .form-grid { grid-template-columns: 1fr; } .full-field { grid-column: auto; } }
</style>
