<script setup>
import { computed, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { ArrowLeft, CircleMinus, Copy, FileText, Landmark, Minus, Plus, QrCode, ShoppingCart, Sparkles, Store, UploadCloud } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import { clearCart, readCartItems, writeCartItems } from "../services/cartService.js";
import { useAuthStore } from "../stores/authStore.js";
import { VyProductImage } from "../components/ui.js";
import paymentQr from "../assets/paymentQr.png";

const router = useRouter();
const auth = useAuthStore();
const items = ref(readCartItems());
const loading = ref(false);
const error = ref("");
const selectedPayment = ref("TRANSFERENCIA");
const cajaCode = ref(generateCajaCode());
const comprobanteFile = ref(null);

const bankPayment = {
  banco: "BANCO ECONOMICO",
  cuenta: "1234 5678 9101 1121 4"
};

const paymentOptions = [
  { id: "TRANSFERENCIA", title: "Transferencia", subtitle: "Bancaria", icon: Landmark },
  { id: "QR", title: "Codigo QR", subtitle: "Escanea y paga", icon: QrCode },
  { id: "CAJA", title: "Pago en Caja", subtitle: "Presencial", icon: Store }
];

const total = computed(() => items.value.reduce((sum, item) => sum + Number(item.price || 0) * Number(item.quantity || 0), 0));
const totalPv = computed(() => items.value.reduce((sum, item) => sum + Number(item.pv || 0) * Number(item.quantity || 0), 0));
const totalQp = computed(() => items.value.reduce((sum, item) => sum + Number(item.qp || 0) * Number(item.quantity || 0), 0));
const totalProducts = computed(() => items.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0));

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
}

function generateCajaCode() {
  return String(Math.floor(100000 + Math.random() * 900000));
}

function handleComprobante(event) {
  const [file] = event.target.files || [];
  error.value = "";

  if (!file) {
    comprobanteFile.value = null;
    return;
  }

  const allowedTypes = ["image/png", "image/jpeg", "image/webp", "application/pdf"];
  if (!allowedTypes.includes(file.type)) {
    comprobanteFile.value = null;
    event.target.value = "";
    error.value = "El comprobante debe ser imagen PNG/JPG/WEBP o PDF.";
    return;
  }

  comprobanteFile.value = file;
}

async function copyText(value) {
  if (!value) {
    return;
  }

  try {
    await navigator.clipboard.writeText(value);
  } catch {
    error.value = "No se pudo copiar automaticamente.";
  }
}

function persist() {
  writeCartItems(items.value);
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

async function checkout() {
  loading.value = true;
  error.value = "";

  try {
    if ((selectedPayment.value === "TRANSFERENCIA" || selectedPayment.value === "QR") && !comprobanteFile.value) {
      throw new Error("Adjunta el comprobante de pago antes de finalizar la compra.");
    }

    if (!auth.usuario?.persona?.id) {
      await auth.cargarPerfil();
    }

    const personaId = auth.usuario?.persona?.id;
    if (!personaId) {
      throw new Error("Tu usuario no tiene una persona asociada.");
    }

    const compraPayload = {
      items: items.value.map((item) => ({
        productoId: Number(item.id),
        cantidad: Number(item.quantity || 1)
      })),
      metodoPago: selectedPayment.value,
      bancoPago: selectedPayment.value === "TRANSFERENCIA" ? bankPayment.banco : null,
      cuentaPago: selectedPayment.value === "TRANSFERENCIA" ? bankPayment.cuenta : null,
      codigoPago: selectedPayment.value === "CAJA" ? cajaCode.value : null,
      referenciaPago: selectedPayment.value === "CAJA"
        ? `Codigo de caja ${cajaCode.value}`
        : selectedPayment.value === "QR"
          ? "Pago por codigo QR"
          : `Transferencia a ${bankPayment.banco}`
    };

    const formData = new FormData();
    formData.append("compra", new Blob([JSON.stringify(compraPayload)], { type: "application/json" }));
    if (comprobanteFile.value) {
      formData.append("comprobante", comprobanteFile.value);
    }

    const response = await apiRequest(`/api/compras/persona/${personaId}/comprobante`, {
      method: "POST",
      body: formData
    });

    clearCart();
    items.value = [];
    comprobanteFile.value = null;

    await Swal.fire({
      title: "Compra pendiente",
      html: `Metodo de pago: <b>${selectedPayment.value}</b><br>${selectedPayment.value === "CAJA" ? `Codigo de caja: <b>${cajaCode.value}</b><br>` : ""}PV: <b>${money(response.compra?.totalPv)}</b><br>QP: <b>${money(response.compra?.totalQp)}</b><br>La compra queda pendiente de validacion en ventanilla.`,
      icon: "success",
      confirmButtonText: "Entendido",
      confirmButtonColor: "#F28705"
    });
    router.push({ name: "wallet" });
  } catch (exception) {
    error.value = exception.message || "No se pudo registrar la compra.";
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  items.value = readCartItems();
});
</script>

<template>
  <div class="vy cart-view">
    <main class="workspace">
      <header class="page-header">
        <button type="button" class="back-button" @click="router.push({ name: 'shop' })">
          <ArrowLeft :size="16" />
        </button>
        <div>
          <div class="vy-eyebrow">Carrito</div>
          <h1>Confirmar compra</h1>
          <p>La compra se registrara para la persona logeada y sumara PV/QP mensual.</p>
        </div>
      </header>

      <div v-if="error" class="error-box">{{ error }}</div>

      <section v-if="items.length" class="cart-layout">
        <article class="vy-card cart-list">
          <div v-for="item in items" :key="item.id" class="cart-row">
            <VyProductImage
              class="cart-product-image"
              :grad="item.img || 'linear-gradient(135deg, #F2E7C4 0%, #F28705 100%)'"
              :h="96"
            />
            <div class="cart-info">
              <small>{{ item.sku }} · {{ item.cat || "Producto" }}</small>
              <strong>{{ item.name }}</strong>
              <span>Bs. {{ money(item.price) }} · PV {{ money(item.pv) }} · QP {{ money(item.qp) }}</span>
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
          <div class="summary-line"><span>Productos</span><b>{{ totalProducts }}</b></div>
          <div class="summary-line"><span>PV mensual</span><b>{{ money(totalPv) }}</b></div>
          <div class="summary-line"><span>QP mensual</span><b>{{ money(totalQp) }}</b></div>
          <div class="summary-total"><span>Total</span><strong>Bs. {{ money(total) }}</strong></div>
          <p>Los beneficios de activacion se pagan hacia arriba segun PV activo y niveles alcanzados.</p>
        </aside>

        <section class="vy-card payment-card">
          <div class="payment-heading">
            <div>
              <div class="vy-eyebrow">Metodo de pago</div>
              <h2>Selecciona como pagaras el pedido</h2>
            </div>
          </div>

          <div class="payment-options">
            <button
              v-for="option in paymentOptions"
              :key="option.id"
              type="button"
              class="payment-option"
              :class="{ active: selectedPayment === option.id }"
              @click="selectedPayment = option.id"
            >
              <span class="payment-icon"><component :is="option.icon" :size="24" /></span>
              <strong>{{ option.title }}</strong>
              <small>{{ option.subtitle }}</small>
            </button>
          </div>

          <div v-if="selectedPayment === 'TRANSFERENCIA'" class="payment-detail">
            <h3>Datos bancarios</h3>
            <label>Banco</label>
            <div class="copy-row">
              <strong>{{ bankPayment.banco }}</strong>
              <button type="button" @click="copyText(bankPayment.banco)"><Copy :size="14" /> Copiar</button>
            </div>
            <label>Numero de cuenta</label>
            <div class="copy-row">
              <strong>{{ bankPayment.cuenta }}</strong>
              <button type="button" @click="copyText(bankPayment.cuenta)"><Copy :size="14" /> Copiar</button>
            </div>
            <p>En concepto coloca tu nombre de usuario y el numero de pedido cuando se registre.</p>
            <label class="proof-upload">
              <UploadCloud :size="22" />
              <span>
                <strong>{{ comprobanteFile ? comprobanteFile.name : "Adjuntar comprobante" }}</strong>
                <small>Imagen PNG/JPG/WEBP o PDF de la transferencia</small>
              </span>
              <input type="file" accept="image/png,image/jpeg,image/webp,application/pdf" @change="handleComprobante" />
            </label>
          </div>

          <div v-if="selectedPayment === 'QR'" class="payment-detail qr-detail">
            <div>
              <h3>Pago por QR</h3>
              <p>Escanea la imagen QR desde tu app bancaria y paga el total del pedido.</p>
              <label class="proof-upload">
                <FileText :size="22" />
                <span>
                  <strong>{{ comprobanteFile ? comprobanteFile.name : "Adjuntar comprobante" }}</strong>
                  <small>Imagen o PDF del pago realizado por QR</small>
                </span>
                <input type="file" accept="image/png,image/jpeg,image/webp,application/pdf" @change="handleComprobante" />
              </label>
            </div>
            <img :src="paymentQr" alt="QR de pago" />
          </div>

          <div v-if="selectedPayment === 'CAJA'" class="payment-detail caja-detail">
            <h3>Pago en caja</h3>
            <p>Presenta este codigo en ventanilla junto a tu documento para pagar el pedido.</p>
            <div class="cash-code">{{ cajaCode }}</div>
            <button type="button" @click="copyText(cajaCode)"><Copy :size="14" /> Copiar codigo</button>
          </div>

          <button type="button" class="vy-btn vy-btn-primary checkout-button" :disabled="loading" @click="checkout">
            <Sparkles :size="16" /> Finalizar compra
          </button>
        </section>
      </section>

      <section v-else class="vy-card empty-cart">
        <ShoppingCart :size="28" />
        <h2>Tu carrito esta vacio</h2>
        <p>Agrega productos desde la tienda para registrar una compra real.</p>
        <button type="button" class="vy-btn vy-btn-primary" @click="router.push({ name: 'shop' })">Ir a tienda</button>
      </section>
    </main>
  </div>
</template>

<style scoped>
.workspace { padding: 28px 32px 40px; min-width: 0; }
.page-header { display: flex; align-items: center; gap: 14px; margin-bottom: 22px; }
.page-header h1 { margin-top: 6px; font-size: 30px; font-weight: 800; }
.page-header p { margin-top: 4px; color: var(--vy-ink-2); font-size: 14px; }
.back-button { width: 38px; height: 38px; border-radius: 50%; border: 1px solid var(--vy-line); background: var(--vy-surface); display: inline-flex; align-items: center; justify-content: center; }
.cart-layout { display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.7fr); gap: 16px; align-items: start; }
.cart-list, .summary-card, .payment-card, .empty-cart { padding: 20px; }
.cart-row { display: grid; grid-template-columns: 96px minmax(0, 1fr) auto auto auto; align-items: center; gap: 14px; padding: 14px 0; border-top: 1px solid var(--vy-line-2); }
.cart-row:first-child { border-top: 0; padding-top: 0; }
.cart-info { min-width: 0; }
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
.summary-line span { color: var(--vy-ink-2); font-weight: 800; }
.summary-total strong { font-family: var(--font-display); font-size: 24px; }
.checkout-button { width: 100%; margin-top: 14px; min-height: 46px; border-radius: 12px; font-weight: 900; }
.summary-card p { margin-top: 12px; color: var(--vy-ink-3); font-size: 12px; font-weight: 700; line-height: 1.45; }
.payment-card { grid-column: 1 / -1; }
.payment-heading { display: flex; justify-content: space-between; gap: 14px; margin-bottom: 16px; }
.payment-heading h2 { margin-top: 6px; font-size: 20px; font-weight: 900; }
.payment-options { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.payment-option { min-height: 132px; padding: 18px 14px; border: 2px solid var(--vy-line); border-radius: 16px; background: var(--vy-surface-2); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 7px; color: var(--vy-ink-2); transition: border-color 0.16s ease, background 0.16s ease, transform 0.16s ease; }
.payment-option:hover, .payment-option.active { border-color: var(--vy-orange); background: #fff7e8; transform: translateY(-1px); }
.payment-option strong { font-size: 14px; font-weight: 900; color: var(--vy-ink); }
.payment-option small { font-size: 12px; font-weight: 800; color: var(--vy-ink-3); }
.payment-icon { width: 48px; height: 48px; border-radius: 50%; background: rgba(242, 135, 5, 0.14); color: var(--vy-orange-deep); display: inline-flex; align-items: center; justify-content: center; }
.payment-option.active .payment-icon { background: var(--vy-orange); color: #fff; }
.payment-detail { margin-top: 16px; padding: 16px; border-radius: 16px; border: 1px solid var(--vy-line); background: var(--vy-surface-2); }
.payment-detail h3 { font-size: 16px; font-weight: 900; margin-bottom: 12px; }
.payment-detail label { display: block; margin: 12px 0 6px; color: var(--vy-ink-3); font-size: 12px; font-weight: 900; }
.payment-detail p { color: var(--vy-ink-2); font-size: 13px; font-weight: 700; line-height: 1.45; }
.copy-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 11px 12px; border-radius: 12px; background: #fff; border: 1px solid var(--vy-line-2); }
.copy-row strong { font-size: 14px; font-weight: 900; word-break: break-word; }
.copy-row button, .caja-detail button { min-height: 36px; padding: 0 12px; border-radius: 10px; background: var(--vy-ink); color: #fff; display: inline-flex; align-items: center; justify-content: center; gap: 7px; font-size: 12px; font-weight: 900; flex-shrink: 0; }
.proof-upload { margin-top: 14px; padding: 14px; border: 1.5px dashed rgba(242, 135, 5, 0.42); border-radius: 14px; background: #fff; color: var(--vy-ink); display: flex; align-items: center; gap: 12px; cursor: pointer; transition: border-color 0.16s ease, background 0.16s ease; }
.proof-upload:hover { border-color: var(--vy-orange); background: #fffaf0; }
.proof-upload > svg { color: var(--vy-orange-deep); flex-shrink: 0; }
.proof-upload span { min-width: 0; display: flex; flex-direction: column; gap: 3px; }
.proof-upload strong { font-size: 13px; font-weight: 900; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.proof-upload small { color: var(--vy-ink-3); font-size: 12px; font-weight: 800; line-height: 1.35; }
.proof-upload input { display: none; }
.qr-detail { display: grid; grid-template-columns: minmax(0, 1fr) 220px; align-items: center; gap: 18px; }
.qr-detail img { width: 220px; border-radius: 18px; border: 1px solid var(--vy-line); background: #fff; padding: 10px; box-shadow: var(--vy-shadow-sm); }
.caja-detail { text-align: center; }
.cash-code { width: fit-content; margin: 14px auto; padding: 12px 24px; border-radius: 14px; background: #fff; border: 1px solid var(--vy-line); font-family: var(--font-mono); font-size: 30px; font-weight: 900; letter-spacing: 0.16em; color: var(--vy-ink); }
.error-box { margin-bottom: 14px; padding: 13px 15px; border-radius: 12px; background: rgba(196, 69, 42, 0.1); color: var(--vy-danger); font-weight: 800; }
.empty-cart { min-height: 280px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; text-align: center; color: var(--vy-ink-2); }
.empty-cart h2 { color: var(--vy-ink); font-size: 22px; }
@media (max-width: 980px) {
  .workspace { padding: 24px 20px 32px; }
  .cart-layout, .cart-row { grid-template-columns: 1fr; }
  .cart-row { align-items: stretch; }
  .cart-product-image { display: none !important; }
  .payment-options { grid-template-columns: 1fr; }
  .payment-option { min-height: 104px; }
  .qr-detail { grid-template-columns: 1fr; }
  .qr-detail img { width: min(100%, 260px); justify-self: center; }
  .copy-row { align-items: stretch; flex-direction: column; }
}
</style>
