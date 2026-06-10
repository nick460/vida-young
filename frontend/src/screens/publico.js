// Pantallas públicas: Landing + Login
import { defineComponent } from "vue";
import { VyIcon, VyLogo, VyMark, VyAvatar, VyProductImage, VySparkline, VyDonut } from "../components/ui.js";
import { PRODUCTS } from "../data.js";
import { useAuthStore } from "../stores/authStore.js";

// ============ LANDING ============
export const ScreenLanding = defineComponent({
  components: { VyLogo, VyMark, VyIcon, VyProductImage, VyAvatar, VySparkline },
  emits: ["navigate"],
  data: () => ({
    nav: ["Plataforma", "Productos", "Oportunidad", "Comunidad", "Soporte"],
    pillars: [
      { i: "leaf",    l: "Bienestar personal",   d: "Cuidado integral con productos certificados, clínicamente probados y formulados con ingredientes botánicos premium." },
      { i: "wallet",  l: "Crecimiento financiero", d: "Plan de compensación moderno, comisiones semanales y reportes financieros transparentes en tiempo real." },
      { i: "network", l: "Comunidad y liderazgo", d: "Mentores certificados, eventos presenciales y un sistema de rangos diseñado para reconocer tu esfuerzo." }
    ],
    sample: [320, 360, 380, 410, 460, 480, 510, 540, 580, 620, 670, 720]
  }),
  template: `<div class="vy" style="width:100%;background:var(--vy-bg)">
    <header style="display:flex;align-items:center;gap:32px;padding:22px 56px;border-bottom:1px solid var(--vy-line-2);position:sticky;top:0;background:rgba(251,248,240,.9);backdrop-filter:blur(8px);z-index:20">
      <vy-logo :size="22"/>
      <nav style="display:flex;gap:28px;font-size:13.5px;font-weight:600;color:var(--vy-ink-2)">
        <a v-for="n in nav" :key="n">{{ n }}</a>
      </nav>
      <div style="flex:1"></div>
      <button class="vy-btn vy-btn-ghost" style="padding:9px 18px" @click="$emit('navigate','login')">Iniciar sesión</button>
      <button class="vy-btn vy-btn-primary" style="padding:9px 18px" @click="$emit('navigate','dashboard')">Empezar gratis</button>
    </header>

    <section style="padding:80px 56px 60px;display:grid;grid-template-columns:1.1fr 1fr;gap:60px;align-items:center;max-width:1440px;margin:0 auto">
      <div>
        <div class="vy-chip vy-chip-cream" style="margin-bottom:20px"><span style="width:6px;height:6px;border-radius:50%;background:var(--vy-orange)"></span> Nueva temporada · Mayo 2026</div>
        <h1 style="font-size:64px;font-weight:800;line-height:1.02;letter-spacing:-0.04em">Generando<br/><span style="background:linear-gradient(135deg, var(--vy-orange) 0%, var(--vy-orange-deep) 100%);-webkit-background-clip:text;background-clip:text;color:transparent">bienestar</span> que se nota.</h1>
        <p style="font-size:17px;color:var(--vy-ink-2);margin-top:24px;max-width:520px;line-height:1.5">Vidayoung es la plataforma latinoamericana donde productos de cuidado personal de alta gama se encuentran con un modelo de negocio transparente. Construye una red, vende lo que amas y crece con tu comunidad.</p>
        <div style="display:flex;gap:12px;margin-top:32px">
          <button class="vy-btn vy-btn-primary" style="padding:16px 28px;font-size:15px" @click="$emit('navigate','login')">Ser embajador <vy-icon name="arrowR" :size="16"/></button>
          <button class="vy-btn vy-btn-ghost" style="padding:16px 28px;font-size:15px" @click="$emit('navigate','shop')">Ver catálogo</button>
        </div>
        <div style="display:flex;gap:32px;margin-top:48px;padding-top:32px;border-top:1px solid var(--vy-line-2)">
          <div v-for="s in [{v:'+12K',l:'embajadores activos'},{v:'18',l:'países'},{v:'4,9★',l:'calificación promedio'}]" :key="s.l">
            <div style="font-family:var(--font-display);font-size:28px;font-weight:800;letter-spacing:-0.03em">{{ s.v }}</div>
            <div style="font-size:12px;color:var(--vy-ink-3);margin-top:2px">{{ s.l }}</div>
          </div>
        </div>
      </div>

      <div style="position:relative">
        <div class="vy-card" style="padding:24px;background:linear-gradient(140deg, var(--vy-surface) 0%, var(--vy-cream) 100%);box-shadow:var(--vy-shadow-lg);border-radius:24px">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:18px">
            <div>
              <div style="font-size:11px;color:var(--vy-ink-3);font-weight:700;letter-spacing:0.06em;text-transform:uppercase">Tu dashboard</div>
              <div style="font-family:var(--font-display);font-size:28px;font-weight:800;letter-spacing:-0.03em;line-height:1.05;margin-top:2px">$ 4.281.500</div>
            </div>
            <span class="vy-chip vy-chip-success">+24,8%</span>
          </div>
          <vy-sparkline :data="sample" :width="380" :height="100"/>
          <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin-top:18px">
            <div v-for="m in [{l:'Red',v:'138'},{l:'Bonos',v:'$ 480K'},{l:'Nivel',v:'Diamante'}]" :key="m.l" style="padding:12px;background:var(--vy-surface);border-radius:10px">
              <div style="font-size:10px;color:var(--vy-ink-3);font-weight:700;text-transform:uppercase;letter-spacing:0.06em">{{ m.l }}</div>
              <div style="font-weight:800;font-size:14px;margin-top:2px">{{ m.v }}</div>
            </div>
          </div>
        </div>
        <div class="vy-card" style="position:absolute;left:-40px;bottom:-30px;padding:14px 18px;display:flex;align-items:center;gap:12px;border-radius:16px;box-shadow:var(--vy-shadow-lg)">
          <div style="width:38px;height:38px;border-radius:50%;background:var(--vy-success);color:#fff;display:flex;align-items:center;justify-content:center"><vy-icon name="check" :size="18"/></div>
          <div>
            <div style="font-weight:800;font-size:13px">Pago acreditado</div>
            <div style="font-size:11px;color:var(--vy-ink-3)">Bono liderazgo · $ 320.000</div>
          </div>
        </div>
      </div>
    </section>

    <section style="background:var(--vy-ink);color:#fff;padding:60px 56px;margin:40px 0">
      <div style="max-width:1440px;margin:0 auto;display:grid;grid-template-columns:repeat(3,1fr);gap:40px">
        <div v-for="p in pillars" :key="p.l">
          <div style="width:48px;height:48px;border-radius:12px;background:rgba(242,135,5,.15);color:var(--vy-orange);display:flex;align-items:center;justify-content:center;margin-bottom:18px"><vy-icon :name="p.i" :size="22"/></div>
          <h3 style="font-size:22px;font-weight:800;letter-spacing:-0.02em;margin-bottom:10px">{{ p.l }}</h3>
          <p style="font-size:14px;line-height:1.55;color:rgba(255,255,255,.7)">{{ p.d }}</p>
        </div>
      </div>
    </section>

    <section style="padding:60px 56px;max-width:1440px;margin:0 auto">
      <div style="display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:32px">
        <div>
          <div class="vy-eyebrow">Catálogo destacado</div>
          <h2 style="font-size:36px;font-weight:800;letter-spacing:-0.03em;margin-top:6px">Productos hechos para sentirse bien</h2>
        </div>
        <button class="vy-btn vy-btn-ghost" @click="$emit('navigate','shop')">Ver tienda <vy-icon name="arrowR" :size="14"/></button>
      </div>
      <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:14px">
        <div v-for="p in PRODUCTS.slice(0,4)" :key="p.id" class="vy-card" style="padding:14px">
          <vy-product-image :grad="p.img" :h="200"/>
          <div style="padding:14px 4px 0">
            <div style="font-size:11px;color:var(--vy-ink-3);font-weight:700;letter-spacing:0.05em;text-transform:uppercase">{{ p.cat }}</div>
            <div style="font-size:14px;font-weight:700;margin-top:4px">{{ p.name }}</div>
            <div style="font-family:var(--font-display);font-size:17px;font-weight:800;margin-top:6px">$ {{ p.price.toLocaleString('es-CO') }}</div>
          </div>
        </div>
      </div>
    </section>

    <section style="padding:60px 56px;background:var(--vy-cream)">
      <div style="max-width:1100px;margin:0 auto;text-align:center">
        <div class="vy-eyebrow">Nuestra promesa</div>
        <h2 style="font-size:48px;font-weight:800;letter-spacing:-0.04em;margin-top:8px;line-height:1.1">"Generando bienestar"<br/>no es un eslogan. Es nuestra forma de operar.</h2>
        <p style="font-size:16px;color:var(--vy-ink-2);margin-top:20px;max-width:680px;margin-left:auto;margin-right:auto;line-height:1.55">Cada producto está formulado con ingredientes responsables; cada comisión se paga puntual; cada embajador tiene un mentor; cada cliente tiene garantía. Esa es la diferencia Vidayoung.</p>
        <button class="vy-btn vy-btn-dark" style="margin-top:28px;padding:14px 26px;font-size:14px" @click="$emit('navigate','login')">Empezar mi camino</button>
      </div>
    </section>

    <footer style="padding:40px 56px;border-top:1px solid var(--vy-line-2);display:flex;justify-content:space-between;align-items:center;font-size:12.5px;color:var(--vy-ink-3)">
      <div style="display:flex;align-items:center;gap:18px">
        <vy-mark :size="20"/>
        <span>© 2026 Vidayoung S.A.S · NIT 901.482.220-1</span>
      </div>
      <div style="display:flex;gap:24px;font-weight:600">
        <a>Términos</a><a>Privacidad</a><a>Plan de compensación</a><a>Soporte</a>
      </div>
    </footer>
  </div>`
});

// ============ LOGIN ============
export const ScreenLogin = defineComponent({
  components: { VyLogo, VyMark, VyIcon },
  emits: ["navigate"],
  data: () => ({
    username: "",
    password: "",
    loading: false,
    error: ""
  }),
  methods: {
    async submitLogin() {
      this.error = "";
      this.loading = true;

      try {
        const auth = useAuthStore();
        await auth.login(this.username, this.password);
        this.$emit("navigate", "dashboard");
      } catch (error) {
        this.error = "Usuario o contraseña incorrectos";
      } finally {
        this.loading = false;
      }
    }
  },
  template: `<div class="vy" style="width:100%;min-height:900px;display:grid;grid-template-columns:1fr 1.05fr;background:var(--vy-bg)">
    <div style="padding:56px;display:flex;flex-direction:column;justify-content:space-between">
      <vy-logo :size="22"/>

      <div style="max-width:380px;width:100%;margin:0 auto">
        <div class="vy-eyebrow" style="margin-bottom:14px">Bienvenida de regreso</div>
        <h1 style="font-size:36px;font-weight:800;letter-spacing:-0.03em;line-height:1.1">Tu bienestar<br/>te está esperando.</h1>
        <p style="color:var(--vy-ink-2);margin-top:14px;font-size:14.5px;line-height:1.5">Ingresa con tu correo o tu código de embajador para acceder a tu panel.</p>

        <form style="margin-top:36px;display:flex;flex-direction:column;gap:14px" @submit.prevent="submitLogin">
          <label style="display:block">
            <span style="font-size:12px;font-weight:700;letter-spacing:0.05em;text-transform:uppercase;color:var(--vy-ink-3);display:block;margin-bottom:6px">Correo o código embajador</span>
            <input v-model="username" type="text" autocomplete="username" placeholder="carolina.m@vidayoung.com" style="width:100%;padding:14px 16px;background:var(--vy-surface);border:1px solid var(--vy-line);border-radius:12px;font-size:14px;font-weight:600;font-family:inherit"/>
          </label>
          <label style="display:block">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
              <span style="font-size:12px;font-weight:700;letter-spacing:0.05em;text-transform:uppercase;color:var(--vy-ink-3)">Contraseña</span>
              <a style="font-size:11.5px;color:var(--vy-orange-deep);font-weight:700">¿Olvidaste tu contraseña?</a>
            </div>
            <input v-model="password" type="password" autocomplete="current-password" placeholder="••••••••••" style="width:100%;padding:14px 16px;background:var(--vy-surface);border:1px solid var(--vy-line);border-radius:12px;font-size:14px;font-weight:600;font-family:inherit"/>
          </label>
          <div v-if="error" style="padding:10px 12px;border-radius:10px;background:rgba(196,69,42,.1);color:var(--vy-danger);font-size:12px;font-weight:700">{{ error }}</div>
          <label style="display:flex;align-items:center;gap:10px;font-size:13px;font-weight:600;color:var(--vy-ink-2);padding:6px 0">
            <input type="checkbox" checked style="width:16px;height:16px;accent-color:var(--vy-orange)"/>
            Mantener sesión iniciada en este dispositivo
          </label>
          <button type="submit" class="vy-btn vy-btn-primary" :disabled="loading" style="padding:14px;font-size:14px;margin-top:6px">{{ loading ? 'Ingresando...' : 'Entrar a mi panel' }} <vy-icon name="arrowR" :size="14"/></button>
          <button type="button" class="vy-btn vy-btn-ghost" style="padding:13px;font-size:13px"><vy-icon name="key" :size="14"/> Ingresar con código de un solo uso</button>
        </form>

        <div style="margin-top:32px;padding-top:24px;border-top:1px solid var(--vy-line-2);text-align:center;font-size:13px;color:var(--vy-ink-2)">
          ¿Aún no eres embajadora? <a style="color:var(--vy-orange-deep);font-weight:700">Crear cuenta gratis</a>
        </div>
      </div>

      <div style="font-size:11.5px;color:var(--vy-ink-3);display:flex;gap:16px;font-weight:600">
        <a>Términos</a><a>Privacidad</a><a>© 2026 Vidayoung</a>
      </div>
    </div>

    <div style="background:linear-gradient(135deg, var(--vy-orange) 0%, var(--vy-orange-deep) 100%);position:relative;overflow:hidden;display:flex;flex-direction:column;justify-content:space-between;padding:56px;color:#fff">
      <div style="position:absolute;inset:0;background:radial-gradient(circle at 80% 20%, rgba(255,255,255,.25), transparent 50%), radial-gradient(circle at 20% 80%, rgba(31,26,20,.2), transparent 50%)"></div>
      <div class="vy-dotgrid" style="position:absolute;inset:0;opacity:0.18"></div>

      <div style="position:relative;display:flex;justify-content:flex-end">
        <div style="background:rgba(255,255,255,.18);backdrop-filter:blur(10px);padding:6px 14px;border-radius:99px;font-size:12px;font-weight:700;letter-spacing:0.06em;text-transform:uppercase">Plataforma 2.6</div>
      </div>

      <div style="position:relative;max-width:520px">
        <vy-mark :size="56" color="#fff"/>
        <h2 style="font-size:48px;font-weight:800;letter-spacing:-0.04em;line-height:1.05;margin-top:32px">Generando bienestar para una nueva generación.</h2>
        <p style="font-size:16px;line-height:1.55;margin-top:18px;opacity:0.92">Más de 12.000 embajadores latinoamericanos construyen un proyecto de vida con productos premium de cuidado personal y un plan de compensación transparente.</p>

        <div style="margin-top:36px;padding:20px;background:rgba(255,255,255,.14);backdrop-filter:blur(12px);border-radius:18px;border:1px solid rgba(255,255,255,.2)">
          <div style="font-size:13.5px;line-height:1.5;font-style:italic">"En 14 meses pasé de iniciadora a Líder Diamante. Vidayoung me dio una comunidad real, comisiones puntuales y productos que mis clientas aman. Cambió mi vida financiera."</div>
          <div style="display:flex;align-items:center;gap:10px;margin-top:14px">
            <div style="width:36px;height:36px;border-radius:50%;background:rgba(255,255,255,.25);display:flex;align-items:center;justify-content:center;font-weight:800">VN</div>
            <div>
              <div style="font-weight:800;font-size:13px">Valeria Nuñez</div>
              <div style="font-size:11px;opacity:0.8">Líder Diamante · Medellín</div>
            </div>
          </div>
        </div>
      </div>

      <div style="position:relative;display:flex;gap:8px">
        <span style="width:24px;height:4px;border-radius:99px;background:#fff"></span>
        <span style="width:8px;height:4px;border-radius:99px;background:rgba(255,255,255,.4)"></span>
        <span style="width:8px;height:4px;border-radius:99px;background:rgba(255,255,255,.4)"></span>
      </div>
    </div>
  </div>`
});
