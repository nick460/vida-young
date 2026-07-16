<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import Swal from "sweetalert2";
import "sweetalert2/dist/sweetalert2.min.css";
import { ArrowLeft, CircleMinus, Copy, Landmark, Minus, Plus, QrCode, Send, ShoppingCart, Store } from "lucide-vue-next";
import { apiRequest } from "../services/api.js";
import { clearPublicCart, readPublicCart, writePublicCart } from "../services/publicCartService.js";
import paymentQr from "../assets/paymentQr.png";

const route = useRoute();
const router = useRouter();
const username = computed(() => String(route.params.username || ""));
const items = ref([]);
const tiposCliente = ref([]);
const loading = ref(false);
const error = ref("");
const cajaCode = ref(generateCajaCode());
const activeStep = ref(0);
const searchingDocument = ref(false);
const documentLookupMessage = ref("");
const fieldErrors = reactive({});

const bankPayment = {
  banco: "BANCO ECONOMICO",
  cuenta: "1234 5678 9101 1121 4"
};

const paymentOptions = [
  { id: "TRANSFERENCIA", title: "Transferencia", subtitle: "Bancaria", icon: Landmark },
  { id: "QR", title: "Codigo QR", subtitle: "Escanea y paga", icon: QrCode },
  { id: "CAJA", title: "Pago en Caja", subtitle: "Presencial", icon: Store }
];

const checkoutSteps = [
  { title: "Pedido", hint: "Confirma los productos y cantidades antes de continuar." },
  { title: "Tus datos", hint: "Indicanos quien realiza el pedido." },
  { title: "Envio", hint: "Confirma si necesitas entrega a domicilio." },
  { title: "Pago", hint: "Elige como pagaras y guarda la referencia." },
  { title: "Revision", hint: "Revisa todo antes de registrar el pedido." }
];

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
const currentStep = computed(() => checkoutSteps[activeStep.value]);

function money(value) {
  return Number(value || 0).toLocaleString("es-BO", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function generateCajaCode() {
  return String(Math.floor(100000 + Math.random() * 900000));
}

async function copyText(value) {
  if (!value) return;
  try {
    await navigator.clipboard.writeText(value);
  } catch {
    error.value = "No se pudo copiar automaticamente.";
  }
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

function setFieldError(field, message) {
  if (message) {
    fieldErrors[field] = message;
  } else {
    delete fieldErrors[field];
  }
}

function validateField(field) {
  if (field === "clienteDocumento") {
    setFieldError(field, form.clienteDocumento.trim() ? "" : "Ingresa tu documento.");
  }

  if (field === "tipoClienteCodigo") {
    setFieldError(field, form.tipoClienteCodigo ? "" : "Selecciona tu tipo de cliente.");
  }

  if (field === "clienteNombres") {
    setFieldError(field, form.clienteNombres.trim() ? "" : "Ingresa tu nombre.");
  }

  if (field === "clienteApellidos") {
    setFieldError(field, form.clienteApellidos.trim() ? "" : "Ingresa tu apellido.");
  }

  if (field === "contacto") {
    const message = form.clienteTelefono.trim() || form.clienteEmail.trim()
      ? ""
      : "Ingresa telefono o correo.";
    setFieldError("clienteTelefono", message);
    setFieldError("clienteEmail", message);
  }

  if (field === "envioCiudad") {
    setFieldError(field, !form.envioRequiere || form.envioCiudad.trim() ? "" : "Ingresa la ciudad.");
  }

  if (field === "envioDireccion") {
    setFieldError(field, !form.envioRequiere || form.envioDireccion.trim() ? "" : "Ingresa la direccion.");
  }

  if (field === "metodoPago") {
    setFieldError(field, form.metodoPago ? "" : "Selecciona un metodo.");
  }

  if (field === "referenciaPago") {
    const needsReference = form.metodoPago === "TRANSFERENCIA" || form.metodoPago === "QR";
    setFieldError(field, !needsReference || form.referenciaPago.trim() ? "" : "Ingresa la referencia del pago.");
  }

  return !fieldErrors[field];
}

function validateStep(step = activeStep.value) {
  if (step === 0) {
    return items.value.length > 0;
  }

  if (step === 1) {
    validateField("clienteDocumento");
    validateField("tipoClienteCodigo");
    validateField("clienteNombres");
    validateField("clienteApellidos");
    validateField("contacto");
    return !fieldErrors.clienteDocumento
      && !fieldErrors.tipoClienteCodigo
      && !fieldErrors.clienteNombres
      && !fieldErrors.clienteApellidos
      && !fieldErrors.clienteTelefono
      && !fieldErrors.clienteEmail;
  }

  if (step === 2) {
    validateField("envioCiudad");
    validateField("envioDireccion");
    return !fieldErrors.envioCiudad && !fieldErrors.envioDireccion;
  }

  if (step === 3) {
    validateField("metodoPago");
    validateField("referenciaPago");
    return !fieldErrors.metodoPago && !fieldErrors.referenciaPago;
  }

  if (form.metodoPago === "CAJA") {
    form.referenciaPago = `Codigo de caja ${cajaCode.value}`;
  }

  return true;
}

function validateAllSteps() {
  for (let step = 0; step < checkoutSteps.length - 1; step += 1) {
    if (!validateStep(step)) {
      activeStep.value = step;
      return false;
    }
  }
  return true;
}

function nextStep() {
  if (!validateStep(activeStep.value)) return;
  activeStep.value = Math.min(activeStep.value + 1, checkoutSteps.length - 1);
  error.value = "";
}

function previousStep() {
  activeStep.value = Math.max(activeStep.value - 1, 0);
  error.value = "";
}

function goToStep(index) {
  if (index <= activeStep.value) {
    activeStep.value = index;
    error.value = "";
    return;
  }

  nextStep();
}

async function searchByDocument() {
  documentLookupMessage.value = "";
  validateField("clienteDocumento");
  if (fieldErrors.clienteDocumento) return;

  searchingDocument.value = true;
  try {
    const cliente = await apiRequest(`/api/public/tiendas/${encodeURIComponent(username.value)}/clientes/documento/${encodeURIComponent(form.clienteDocumento.trim())}`);
    form.clienteNombres = cliente.nombres || "";
    form.clienteApellidos = cliente.apellidos || "";
    form.clienteDocumento = cliente.documento || form.clienteDocumento;
    form.clienteEmail = cliente.email || "";
    form.clienteTelefono = cliente.telefono || "";
    form.envioRequiere = Boolean(cliente.envioRequiere);
    form.envioDireccion = cliente.envioDireccion || "";
    form.envioCiudad = cliente.envioCiudad || "";
    form.envioReferencia = cliente.envioReferencia || "";
    ["clienteDocumento", "clienteNombres", "clienteApellidos", "clienteTelefono", "clienteEmail", "envioCiudad", "envioDireccion"].forEach((field) => setFieldError(field, ""));
    documentLookupMessage.value = "Encontramos tus datos y completamos el formulario.";
  } catch {
    documentLookupMessage.value = "No encontramos datos para ese documento. Completa tus datos una sola vez para esta compra.";
  } finally {
    searchingDocument.value = false;
  }
}

async function checkout() {
  if (!validateAllSteps()) return;

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
        <form class="vy-card checkout-card" @submit.prevent="checkout">
          <nav class="checkout-stepper" aria-label="Pasos del pedido">
            <button
              v-for="(step, index) in checkoutSteps"
              :key="step.title"
              type="button"
              :class="{ active: activeStep === index, done: activeStep > index }"
              @click="goToStep(index)"
            >
              <b>{{ index + 1 }}</b>
              <span>{{ step.title }}</span>
            </button>
          </nav>

          <section class="step-intro">
            <span>Paso {{ activeStep + 1 }} de {{ checkoutSteps.length }}</span>
            <h2>{{ currentStep.title }}</h2>
            <p>{{ currentStep.hint }}</p>
          </section>

          <div v-if="activeStep === 0" class="order-step-grid">
            <article class="cart-list">
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

            <aside class="summary-card">
              <div class="summary-icon"><ShoppingCart :size="18" /></div>
              <h2>Resumen</h2>
              <div class="summary-line"><span>Total cliente</span><b>Bs. {{ money(total) }}</b></div>
              <div class="summary-line"><span>Base empresa</span><b>Bs. {{ money(totalEmpresa) }}</b></div>
              <div class="summary-line"><span>Descuentos</span><b>Bs. {{ money(totalDescuento) }}</b></div>
              <div class="summary-total"><span>Para distribuidor</span><strong>Bs. {{ money(gananciaDistribuidor) }}</strong></div>
              <p>El sistema conserva el descuento aplicado para justificar la ganancia final del distribuidor.</p>
            </aside>
          </div>

          <div v-if="activeStep === 1" class="form-grid">
            <div class="step-help full-field">
              <strong>Busca tus datos por documento</strong>
              <p>Si ya hiciste una compra antes o ya estas registrado, completaremos tus datos para que no vuelvas a escribir todo.</p>
            </div>
            <label class="document-field full-field" :class="{ invalid: fieldErrors.clienteDocumento }">
              <span>Documento</span>
              <div class="document-search-row">
                <input
                  v-model.trim="form.clienteDocumento"
                  placeholder="Ingresa tu documento"
                  @input="validateField('clienteDocumento'); documentLookupMessage = ''"
                  @keyup.enter.prevent="searchByDocument"
                />
                <button type="button" :disabled="searchingDocument" @click="searchByDocument">
                  {{ searchingDocument ? "Buscando..." : "Buscar datos" }}
                </button>
              </div>
              <small v-if="fieldErrors.clienteDocumento">{{ fieldErrors.clienteDocumento }}</small>
              <small v-else-if="documentLookupMessage" class="lookup-message">{{ documentLookupMessage }}</small>
            </label>
            <label :class="{ invalid: fieldErrors.tipoClienteCodigo }">
              <span>Tipo de cliente</span>
              <select v-model="form.tipoClienteCodigo" @change="validateField('tipoClienteCodigo')">
                <option v-for="tipo in tiposCliente" :key="tipo.id" :value="tipo.codigo">{{ tipo.nombre }}</option>
              </select>
              <small v-if="fieldErrors.tipoClienteCodigo">{{ fieldErrors.tipoClienteCodigo }}</small>
            </label>
            <label :class="{ invalid: fieldErrors.clienteNombres }">
              <span>Nombres</span>
              <input v-model.trim="form.clienteNombres" placeholder="Tu nombre" @input="validateField('clienteNombres')" />
              <small v-if="fieldErrors.clienteNombres">{{ fieldErrors.clienteNombres }}</small>
            </label>
            <label :class="{ invalid: fieldErrors.clienteApellidos }">
              <span>Apellidos</span>
              <input v-model.trim="form.clienteApellidos" placeholder="Tu apellido" @input="validateField('clienteApellidos')" />
              <small v-if="fieldErrors.clienteApellidos">{{ fieldErrors.clienteApellidos }}</small>
            </label>
            <label :class="{ invalid: fieldErrors.clienteTelefono }">
              <span>Telefono</span>
              <input v-model.trim="form.clienteTelefono" placeholder="Para coordinar el pedido" @input="validateField('contacto')" />
              <small v-if="fieldErrors.clienteTelefono">{{ fieldErrors.clienteTelefono }}</small>
            </label>
            <label :class="{ invalid: fieldErrors.clienteEmail }">
              <span>Correo</span>
              <input v-model.trim="form.clienteEmail" type="email" placeholder="Opcional si dejas telefono" @input="validateField('contacto')" />
              <small v-if="fieldErrors.clienteEmail">{{ fieldErrors.clienteEmail }}</small>
            </label>
          </div>

          <div v-if="activeStep === 2" class="form-grid">
            <div class="step-help full-field">
              <strong>Entrega del producto</strong>
              <p>Si retirarás tu pedido directamente o coordinarás por otro medio, deja desmarcada la opcion de envio.</p>
            </div>
            <label class="toggle-field full-field">
              <input v-model="form.envioRequiere" type="checkbox" @change="validateField('envioCiudad'); validateField('envioDireccion')" />
              <span>Requiere envio</span>
            </label>
            <template v-if="form.envioRequiere">
              <label :class="{ invalid: fieldErrors.envioCiudad }">
                <span>Ciudad</span>
                <input v-model.trim="form.envioCiudad" placeholder="Ej. Santa Cruz" @input="validateField('envioCiudad')" />
                <small v-if="fieldErrors.envioCiudad">{{ fieldErrors.envioCiudad }}</small>
              </label>
              <label :class="{ invalid: fieldErrors.envioDireccion }">
                <span>Direccion</span>
                <input v-model.trim="form.envioDireccion" placeholder="Calle, zona, numero de casa" @input="validateField('envioDireccion')" />
                <small v-if="fieldErrors.envioDireccion">{{ fieldErrors.envioDireccion }}</small>
              </label>
              <label class="full-field">
                <span>Referencia de envio</span>
                <textarea v-model.trim="form.envioReferencia" rows="3" placeholder="Horarios, punto de referencia o instrucciones para ubicarte" />
              </label>
            </template>
          </div>

          <div v-if="activeStep === 3" class="form-grid">
            <section class="payment-card full-field">
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
                  :class="{ active: form.metodoPago === option.id }"
                  @click="form.metodoPago = option.id; validateField('metodoPago'); validateField('referenciaPago')"
                >
                  <span class="payment-icon"><component :is="option.icon" :size="24" /></span>
                  <strong>{{ option.title }}</strong>
                  <small>{{ option.subtitle }}</small>
                </button>
              </div>

              <div v-if="form.metodoPago === 'TRANSFERENCIA'" class="payment-detail">
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
                <label>Referencia de pago</label>
                <input
                  v-model.trim="form.referenciaPago"
                  :class="{ invalid: fieldErrors.referenciaPago }"
                  placeholder="Numero de operacion o nota de transferencia"
                  @input="validateField('referenciaPago')"
                />
                <small v-if="fieldErrors.referenciaPago" class="field-error">{{ fieldErrors.referenciaPago }}</small>
              </div>

              <div v-if="form.metodoPago === 'QR'" class="payment-detail qr-detail">
                <div>
                  <h3>Pago por QR</h3>
                  <p>Escanea la imagen QR desde tu app bancaria y paga el total del pedido.</p>
                  <label>Referencia de pago</label>
                  <input
                    v-model.trim="form.referenciaPago"
                    :class="{ invalid: fieldErrors.referenciaPago }"
                    placeholder="Numero de operacion o nota del pago QR"
                    @input="validateField('referenciaPago')"
                  />
                  <small v-if="fieldErrors.referenciaPago" class="field-error">{{ fieldErrors.referenciaPago }}</small>
                </div>
                <img :src="paymentQr" alt="QR de pago" />
              </div>

              <div v-if="form.metodoPago === 'CAJA'" class="payment-detail caja-detail">
                <h3>Pago en caja</h3>
                <p>Presenta este codigo en ventanilla junto a tus datos para pagar el pedido.</p>
                <div class="cash-code">{{ cajaCode }}</div>
                <button type="button" @click="copyText(cajaCode)"><Copy :size="14" /> Copiar codigo</button>
              </div>
            </section>
          </div>

          <div v-if="activeStep === 4" class="review-grid">
            <section>
              <h3>Datos del cliente</h3>
              <p>{{ form.clienteNombres }} {{ form.clienteApellidos }}</p>
              <span>{{ form.clienteTelefono || "Sin telefono" }} · {{ form.clienteEmail || "Sin correo" }}</span>
              <span>Tipo: {{ form.tipoClienteCodigo }}</span>
            </section>
            <section>
              <h3>Envio</h3>
              <p>{{ form.envioRequiere ? `${form.envioCiudad} - ${form.envioDireccion}` : "No requiere envio" }}</p>
              <span>{{ form.envioReferencia || "Sin referencia adicional" }}</span>
            </section>
            <section>
              <h3>Pago</h3>
              <p>{{ form.metodoPago }}</p>
              <span>{{ form.metodoPago === "CAJA" ? `Codigo ${cajaCode}` : form.referenciaPago || "Referencia pendiente" }}</span>
            </section>
            <section>
              <h3>Total</h3>
              <p>Bs. {{ money(total) }}</p>
              <span>Tu pedido quedara pendiente de validacion.</span>
            </section>
          </div>

          <footer class="step-actions">
            <button class="vy-btn vy-btn-ghost" type="button" :disabled="activeStep === 0 || loading" @click="previousStep">
              Atras
            </button>
            <button v-if="activeStep < checkoutSteps.length - 1" class="vy-btn vy-btn-primary" type="button" @click="nextStep">
              Siguiente
            </button>
            <button v-else class="vy-btn vy-btn-primary" type="submit" :disabled="loading">
              <Send :size="16" /> Registrar pedido
            </button>
          </footer>
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
.cart-layout { display: block; }
.checkout-card, .empty-cart { padding: 20px; }
.order-step-grid { display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(320px, 0.7fr); gap: 16px; align-items: start; }
.cart-list, .summary-card { padding: 16px; border: 1px solid var(--vy-line); border-radius: 16px; background: var(--vy-surface-2); }
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
.checkout-stepper { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 8px; margin-bottom: 18px; }
.checkout-stepper button { min-height: 54px; padding: 8px; border-radius: 12px; border: 1px solid var(--vy-line); background: var(--vy-surface-2); color: var(--vy-ink-2); display: flex; align-items: center; gap: 8px; text-align: left; }
.checkout-stepper b { width: 26px; height: 26px; border-radius: 50%; background: #fff; color: var(--vy-ink); display: inline-flex; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0; }
.checkout-stepper span { font-size: 12px; font-weight: 900; }
.checkout-stepper button.active { border-color: var(--vy-orange); background: #fff7e8; color: var(--vy-orange-deep); }
.checkout-stepper button.active b, .checkout-stepper button.done b { background: var(--vy-orange); color: #fff; }
.step-intro { margin-bottom: 16px; padding: 14px; border-radius: 14px; background: #fffaf0; border: 1px solid rgba(242, 135, 5, 0.22); }
.step-intro span { color: var(--vy-orange-deep); font-size: 11px; font-weight: 900; text-transform: uppercase; }
.step-intro h2 { margin-top: 4px; font-size: 20px; font-weight: 900; }
.step-intro p { margin-top: 4px; color: var(--vy-ink-2); font-size: 13px; font-weight: 800; line-height: 1.45; }
.step-help { padding: 14px; border-radius: 14px; background: var(--vy-surface-2); border: 1px solid var(--vy-line); }
.step-help strong { display: block; font-size: 14px; font-weight: 900; }
.step-help p { margin-top: 4px; color: var(--vy-ink-2); font-size: 13px; font-weight: 700; line-height: 1.45; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
label span { display: block; margin-bottom: 7px; color: var(--vy-ink-3); font-size: 11px; font-weight: 900; text-transform: uppercase; }
input, select, textarea { width: 100%; border: 1px solid var(--vy-line); border-radius: 12px; background: var(--vy-surface-2); color: var(--vy-ink); font: inherit; font-size: 13px; font-weight: 800; outline: 0; }
input, select { min-height: 42px; padding: 0 12px; }
textarea { padding: 12px; resize: vertical; }
.document-search-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 8px; align-items: start; }
.document-search-row button { min-height: 42px; padding: 0 14px; border-radius: 12px; background: var(--vy-ink); color: #fff; font-size: 12px; font-weight: 900; white-space: nowrap; }
.document-search-row button:disabled { cursor: wait; opacity: 0.72; }
label.invalid input, label.invalid select, label.invalid textarea, input.invalid { border-color: var(--vy-danger); background: rgba(196, 69, 42, 0.06); }
label > small, .field-error { display: block; margin-top: 6px; color: var(--vy-danger); font-size: 11px; font-weight: 900; line-height: 1.35; }
.lookup-message { color: var(--vy-success); }
.full-field { grid-column: 1 / -1; }
.toggle-field { display: inline-flex; align-items: center; gap: 10px; }
.toggle-field input { width: 18px; min-height: 18px; }
.toggle-field span { margin: 0; }
.payment-card { padding: 16px; border: 1px solid var(--vy-line); border-radius: 16px; background: var(--vy-surface); }
.payment-heading { display: flex; justify-content: space-between; gap: 14px; margin-bottom: 16px; }
.payment-heading h2 { margin-top: 6px; font-size: 20px; font-weight: 900; }
.payment-options { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.payment-option { min-height: 132px; padding: 18px 14px; border: 2px solid var(--vy-line); border-radius: 16px; background: var(--vy-surface-2); display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 7px; color: var(--vy-ink-2); }
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
.qr-detail { display: grid; grid-template-columns: minmax(0, 1fr) 220px; align-items: center; gap: 18px; }
.qr-detail img { width: 220px; border-radius: 18px; border: 1px solid var(--vy-line); background: #fff; padding: 10px; box-shadow: var(--vy-shadow-sm); }
.caja-detail { text-align: center; }
.cash-code { width: fit-content; margin: 14px auto; padding: 12px 24px; border-radius: 14px; background: #fff; border: 1px solid var(--vy-line); font-family: var(--font-mono); font-size: 30px; font-weight: 900; letter-spacing: 0.16em; color: var(--vy-ink); }
.review-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.review-grid section { padding: 14px; border: 1px solid var(--vy-line); border-radius: 14px; background: var(--vy-surface-2); }
.review-grid h3 { font-size: 13px; font-weight: 900; }
.review-grid p { margin-top: 6px; color: var(--vy-ink); font-size: 15px; font-weight: 900; line-height: 1.35; }
.review-grid span { display: block; margin-top: 4px; color: var(--vy-ink-3); font-size: 12px; font-weight: 800; line-height: 1.35; }
.step-actions { display: flex; justify-content: space-between; gap: 10px; margin-top: 18px; }
.step-actions .vy-btn { min-height: 46px; padding: 0 18px; border-radius: 12px; font-weight: 900; display: inline-flex; align-items: center; justify-content: center; gap: 8px; }
.vy-btn-ghost { background: var(--vy-surface); border: 1px solid var(--vy-line); color: var(--vy-ink-2); }
.vy-btn-primary { background: var(--vy-orange); color: #fff; }
.empty-cart { min-height: 280px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px; text-align: center; color: var(--vy-ink-2); }
@media (max-width: 980px) { .workspace { padding: 24px 20px 32px; } .order-step-grid, .cart-row, .form-grid, .payment-options, .qr-detail, .review-grid { grid-template-columns: 1fr; } .checkout-stepper { grid-template-columns: repeat(2, minmax(0, 1fr)); } .full-field { grid-column: auto; } .qr-detail img { width: min(100%, 260px); justify-self: center; } .copy-row { align-items: stretch; flex-direction: column; } }
@media (max-width: 520px) {
  .checkout-stepper { display: flex; gap: 8px; margin-inline: -20px; padding-inline: 20px; overflow-x: auto; scrollbar-width: none; }
  .checkout-stepper::-webkit-scrollbar { display: none; }
  .checkout-stepper button { flex: 0 0 62px; min-height: 44px; padding: 8px; justify-content: center; }
  .checkout-stepper button.active { flex-basis: 118px; justify-content: flex-start; }
  .checkout-stepper button:not(.active) span { display: none; }
  .checkout-stepper b { width: 28px; height: 28px; }
  .document-search-row { grid-template-columns: 1fr; }
  .document-search-row button { width: 100%; }
  .step-actions { flex-direction: column; }
  .step-actions .vy-btn { width: 100%; }
}
</style>
