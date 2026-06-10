// Pantallas: Tienda, Carrito, Red, Perfil, Recompensas, Stats
import { defineComponent } from "vue";
import { VyIcon, VyAvatar, VyMoney, VyDonut, VyBarChart, VyProductImage, AppShell } from "../components/ui.js";
import { USER, PRODUCTS, NETWORK_ROOT } from "../data.js";

// ============ TIENDA ============
export const ScreenShop = defineComponent({
  components: { AppShell, VyIcon, VyProductImage },
  emits: ["navigate"],
  data: () => ({ PRODUCTS, cats: ["Todos","Cuidado facial","Suplementos","Aromaterapia","Nutrición"], active: "Todos" }),
  computed: {
    hero() { return PRODUCTS[0]; },
    list() { return this.active === "Todos" ? PRODUCTS.slice(1) : PRODUCTS.filter(p => p.cat === this.active); }
  },
  template: `<app-shell active="shop" @navigate="$emit('navigate', $event)" search="Buscar productos de bienestar…">
    <div style="display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:20px">
      <div>
        <div class="vy-eyebrow" style="margin-bottom:8px">Tienda Vidayoung</div>
        <h1 style="font-size:30px;font-weight:800">Productos para tu bienestar</h1>
        <p style="font-size:14px;color:var(--vy-ink-2);margin-top:4px">Cada compra suma a tu volumen mensual y al de tu red.</p>
      </div>
      <button class="vy-btn vy-btn-ghost" @click="$emit('navigate','cart')"><vy-icon name="cart" :size="14"/> Carrito · 3</button>
    </div>

    <div style="display:flex;gap:8px;margin-bottom:20px;flex-wrap:wrap">
      <button v-for="c in cats" :key="c" @click="active = c"
              :style="{padding:'8px 16px',borderRadius:'99px',fontSize:'13px',fontWeight:700,background: active === c ? 'var(--vy-ink)' : 'var(--vy-surface)',color: active === c ? '#fff' : 'var(--vy-ink-2)',border:'1px solid', borderColor: active === c ? 'var(--vy-ink)' : 'var(--vy-line)'}">{{ c }}</button>
    </div>

    <div class="vy-card" style="padding:0;margin-bottom:18px;display:grid;grid-template-columns:1.1fr 1fr;gap:0;overflow:hidden">
      <vy-product-image :grad="hero.img" :h="380" big/>
      <div style="padding:36px">
        <div class="vy-chip vy-chip-orange" style="margin-bottom:14px">{{ hero.badge }}</div>
        <div style="font-size:12px;color:var(--vy-ink-3);font-weight:700;letter-spacing:0.08em;text-transform:uppercase">{{ hero.cat }}</div>
        <h2 style="font-size:32px;font-weight:800;letter-spacing:-0.03em;margin-top:8px;line-height:1.1">{{ hero.name }}</h2>
        <p style="font-size:14px;color:var(--vy-ink-2);margin-top:14px;line-height:1.5">Fórmula concentrada con vitamina C estabilizada al 15%, ácido hialurónico y extracto de papaya. Ilumina, hidrata y unifica el tono en 4 semanas.</p>
        <div style="display:flex;align-items:center;gap:14px;margin-top:18px">
          <div style="display:flex;align-items:center;gap:6px;color:var(--vy-orange-deep)"><vy-icon name="star" :size="14" fill="currentColor" stroke="currentColor"/> <span style="font-weight:700;font-size:13px">{{ hero.rating }}</span></div>
          <span style="font-size:12px;color:var(--vy-ink-3)">· 1.248 reseñas</span>
          <span class="vy-chip vy-chip-success" style="font-size:10px">En stock</span>
        </div>
        <div style="display:flex;align-items:baseline;gap:12px;margin-top:24px">
          <div style="font-family:var(--font-display);font-size:32px;font-weight:800;letter-spacing:-0.03em">$ {{ hero.price.toLocaleString('es-CO') }}</div>
          <div v-if="hero.old" style="text-decoration:line-through;color:var(--vy-ink-3);font-size:14px">$ {{ hero.old.toLocaleString('es-CO') }}</div>
          <span v-if="hero.old" class="vy-chip vy-chip-orange" style="font-size:10px">−14%</span>
        </div>
        <div style="display:flex;gap:10px;margin-top:18px">
          <button class="vy-btn vy-btn-primary" @click="$emit('navigate','cart')" style="flex:1"><vy-icon name="cart" :size="14"/> Agregar al carrito</button>
          <button class="vy-btn vy-btn-ghost" style="width:48px;padding:0"><vy-icon name="heart" :size="16"/></button>
        </div>
        <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin-top:20px;padding-top:20px;border-top:1px solid var(--vy-line-2)">
          <div v-for="f in [{i:'truck',l:'Envío gratis'},{i:'shield',l:'Garantía 30 días'},{i:'leaf',l:'Sin parabenos'}]" :key="f.l" style="display:flex;align-items:center;gap:8px;font-size:11.5px;color:var(--vy-ink-2);font-weight:600">
            <vy-icon :name="f.i" :size="14" stroke="var(--vy-orange-deep)"/> {{ f.l }}
          </div>
        </div>
      </div>
    </div>

    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:14px">
      <div v-for="p in list" :key="p.id" class="vy-card" style="padding:12px;display:flex;flex-direction:column;cursor:pointer;transition:transform .15s" @mouseenter="$event.currentTarget.style.transform='translateY(-2px)'" @mouseleave="$event.currentTarget.style.transform=''">
        <div style="position:relative">
          <vy-product-image :grad="p.img" :h="180"/>
          <span v-if="p.badge" class="vy-chip vy-chip-ink" style="position:absolute;top:10px;left:10px;font-size:10px">{{ p.badge }}</span>
          <button style="position:absolute;top:10px;right:10px;width:32px;height:32px;border-radius:50%;background:rgba(255,255,255,.92);display:flex;align-items:center;justify-content:center"><vy-icon name="heart" :size="14"/></button>
        </div>
        <div style="padding:14px 6px 6px">
          <div style="font-size:11px;color:var(--vy-ink-3);font-weight:700;letter-spacing:0.05em;text-transform:uppercase">{{ p.cat }}</div>
          <div style="font-size:14px;font-weight:700;margin-top:4px;line-height:1.3">{{ p.name }}</div>
          <div style="display:flex;align-items:center;gap:6px;margin-top:6px;font-size:12px;color:var(--vy-ink-2)"><vy-icon name="star" :size="12" fill="var(--vy-orange)" stroke="var(--vy-orange)"/> {{ p.rating }}</div>
          <div style="display:flex;justify-content:space-between;align-items:center;margin-top:12px">
            <div>
              <div style="font-family:var(--font-display);font-size:17px;font-weight:800">$ {{ p.price.toLocaleString('es-CO') }}</div>
              <div v-if="p.old" style="text-decoration:line-through;color:var(--vy-ink-3);font-size:11px">$ {{ p.old.toLocaleString('es-CO') }}</div>
            </div>
            <button style="width:36px;height:36px;border-radius:50%;background:var(--vy-ink);color:#fff;display:flex;align-items:center;justify-content:center"><vy-icon name="plus" :size="14"/></button>
          </div>
        </div>
      </div>
    </div>
  </app-shell>`
});

// ============ CARRITO ============
export const ScreenCart = defineComponent({
  components: { AppShell, VyIcon, VyProductImage, VyMoney },
  emits: ["navigate"],
  data: () => ({
    items: [
      { ...PRODUCTS[0], qty: 2 },
      { ...PRODUCTS[1], qty: 1 },
      { ...PRODUCTS[7], qty: 1 },
    ]
  }),
  computed: {
    sub() { return this.items.reduce((s, i) => s + i.price * i.qty, 0); },
    ship() { return 0; },
    total() { return this.sub + this.ship; }
  },
  methods: {
    inc(item) { item.qty++; },
    dec(item) { if (item.qty > 1) item.qty--; },
    remove(item) { this.items = this.items.filter(x => x !== item); }
  },
  template: `<app-shell active="shop" @navigate="$emit('navigate', $event)">
    <div style="display:flex;align-items:center;gap:14px;margin-bottom:24px">
      <button @click="$emit('navigate','shop')" style="width:36px;height:36px;border-radius:50%;background:var(--vy-surface);border:1px solid var(--vy-line);display:flex;align-items:center;justify-content:center"><vy-icon name="arrowR" :size="14" :sw="2" :style="{transform:'rotate(180deg)'}"/></button>
      <div>
        <h1 style="font-size:28px;font-weight:800">Tu carrito</h1>
        <p style="font-size:13px;color:var(--vy-ink-2);margin-top:2px">{{ items.length }} productos · te faltan <b>$ 132.000</b> para envío gratis exprés</p>
      </div>
    </div>

    <div style="display:grid;grid-template-columns:1.6fr 1fr;gap:14px">
      <div class="vy-card" style="padding:22px">
        <div v-for="(it, i) in items" :key="it.id" :style="{display:'grid',gridTemplateColumns:'100px 1fr auto',gap:'18px',alignItems:'center',padding:'18px 0',borderTop: i===0 ? '0' : '1px solid var(--vy-line-2)'}">
          <vy-product-image :grad="it.img" :h="100"/>
          <div>
            <div style="font-size:11px;color:var(--vy-ink-3);font-weight:700;letter-spacing:0.05em;text-transform:uppercase">{{ it.cat }}</div>
            <div style="font-size:15px;font-weight:700;margin-top:3px">{{ it.name }}</div>
            <div style="font-family:var(--font-display);font-size:16px;font-weight:800;margin-top:6px">$ {{ it.price.toLocaleString('es-CO') }}</div>
          </div>
          <div style="display:flex;flex-direction:column;align-items:flex-end;gap:14px">
            <button @click="remove(it)" style="font-size:12px;color:var(--vy-ink-3);font-weight:600">Quitar</button>
            <div style="display:flex;align-items:center;gap:6px;background:var(--vy-surface-2);border:1px solid var(--vy-line);border-radius:99px;padding:4px">
              <button @click="dec(it)" style="width:26px;height:26px;border-radius:50%;background:#fff;display:flex;align-items:center;justify-content:center"><vy-icon name="minus" :size="12"/></button>
              <span class="vy-mono" style="min-width:24px;text-align:center;font-weight:700;font-size:13px">{{ it.qty }}</span>
              <button @click="inc(it)" style="width:26px;height:26px;border-radius:50%;background:var(--vy-orange);color:#fff;display:flex;align-items:center;justify-content:center"><vy-icon name="plus" :size="12"/></button>
            </div>
          </div>
        </div>
      </div>

      <div style="display:flex;flex-direction:column;gap:14px">
        <div class="vy-card" style="padding:22px">
          <h3 style="font-size:16px;font-weight:800;margin-bottom:14px">Resumen del pedido</h3>
          <div style="display:flex;justify-content:space-between;font-size:13.5px;padding:8px 0;color:var(--vy-ink-2)"><span>Subtotal</span><vy-money :v="sub"/></div>
          <div style="display:flex;justify-content:space-between;font-size:13.5px;padding:8px 0;color:var(--vy-ink-2)"><span>Envío</span><span style="color:var(--vy-success);font-weight:700">Gratis</span></div>
          <div style="display:flex;justify-content:space-between;font-size:13.5px;padding:8px 0;color:var(--vy-ink-2)"><span>Volumen para tu rango</span><span class="vy-mono" style="color:var(--vy-orange-deep);font-weight:700">+ 740 PV</span></div>
          <div style="height:1px;background:var(--vy-line);margin:10px 0"></div>
          <div style="display:flex;justify-content:space-between;align-items:baseline;padding:8px 0">
            <span style="font-weight:700">Total</span>
            <span style="font-family:var(--font-display);font-size:24px;font-weight:800">$ {{ total.toLocaleString('es-CO') }}</span>
          </div>
          <button class="vy-btn vy-btn-primary" style="width:100%;margin-top:14px;padding:14px"><vy-icon name="check" :size="16"/> Finalizar compra</button>
          <button class="vy-btn vy-btn-ghost" style="width:100%;margin-top:8px;padding:12px;font-size:13px">Pagar con saldo Vidayoung</button>
        </div>

        <div class="vy-card" style="padding:18px;background:var(--vy-cream);border-color:transparent">
          <div style="display:flex;align-items:flex-start;gap:12px">
            <div style="width:36px;height:36px;border-radius:50%;background:var(--vy-orange);color:#fff;display:flex;align-items:center;justify-content:center;flex-shrink:0"><vy-icon name="sparkle" :size="16"/></div>
            <div>
              <div style="font-weight:800;font-size:14px">Comisión estimada</div>
              <div style="font-size:12.5px;color:var(--vy-ink-2);margin-top:4px;line-height:1.45">Esta compra genera <b>$ 28.460</b> en bono inmediato a tu patrocinador. Tu equipo gana cuando tú creces.</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </app-shell>`
});

// ============ RED MULTINIVEL ============
export const ScreenNetwork = defineComponent({
  components: { AppShell, VyIcon, VyAvatar },
  emits: ["navigate"],
  data: () => ({ NETWORK_ROOT, USER }),
  template: `<app-shell active="network" @navigate="$emit('navigate', $event)">
    <div style="display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:20px">
      <div>
        <div class="vy-eyebrow" style="margin-bottom:8px">Tu red</div>
        <h1 style="font-size:30px;font-weight:800">Comunidad de bienestar</h1>
        <p style="font-size:14px;color:var(--vy-ink-2);margin-top:4px">138 embajadores activos en 4 niveles. Crecimiento +12% en 30 días.</p>
      </div>
      <button class="vy-btn vy-btn-primary"><vy-icon name="plus" :size="14"/> Invitar embajador</button>
    </div>

    <div class="vy-card" style="padding:22px;margin-bottom:18px;display:grid;grid-template-columns:1.4fr 1fr;gap:24px;align-items:center">
      <div>
        <div class="vy-chip vy-chip-cream" style="margin-bottom:10px">Tu enlace de invitación</div>
        <div style="display:flex;align-items:center;gap:10px;padding:14px 18px;background:var(--vy-surface-2);border:1px dashed var(--vy-line);border-radius:14px">
          <vy-icon name="copy" :size="16" stroke="var(--vy-orange-deep)"/>
          <span class="vy-mono" style="font-size:14px;font-weight:700;flex:1">vidayoung.co/invitar/{{ USER.code }}</span>
          <button class="vy-btn vy-btn-dark" style="padding:8px 14px;font-size:12px">Copiar</button>
        </div>
        <div style="display:flex;gap:18px;margin-top:14px;font-size:12px;color:var(--vy-ink-2)">
          <span>· Compartir por WhatsApp</span><span>· Por correo</span><span>· Generar QR</span>
        </div>
      </div>
      <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:10px">
        <div v-for="(s, i) in [{l:'Nivel 1',v:'12'},{l:'Nivel 2',v:'34'},{l:'Nivel 3',v:'58'},{l:'Nivel 4',v:'34'},{l:'Activos',v:'138'},{l:'Volumen',v:'$12,4M'}]" :key="i" :style="{padding:'14px',background: i === 4 ? 'var(--vy-orange)' : 'var(--vy-surface-2)', color: i === 4 ? '#fff' : 'inherit', borderRadius:'12px'}">
          <div :style="{fontSize:'10px',color: i === 4 ? 'rgba(255,255,255,.85)' : 'var(--vy-ink-3)',fontWeight:700,letterSpacing:'0.06em',textTransform:'uppercase'}">{{ s.l }}</div>
          <div style="font-family:var(--font-display);font-weight:800;font-size:20px;letter-spacing:-0.02em;margin-top:4px">{{ s.v }}</div>
        </div>
      </div>
    </div>

    <div class="vy-card" style="padding:22px;background:var(--vy-bg);position:relative;overflow:hidden">
      <div class="vy-dotgrid" style="position:absolute;inset:0;opacity:0.5"></div>
      <div style="position:relative">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:24px">
          <h3 style="font-size:16px;font-weight:800">Estructura de red</h3>
          <div style="display:flex;gap:6px;background:var(--vy-surface);padding:4px;border-radius:99px;border:1px solid var(--vy-line)">
            <button v-for="(t, i) in ['Árbol','Lista','Mapa']" :key="t" :style="{padding:'6px 14px',borderRadius:'99px',fontSize:'12px',fontWeight:700,background: i === 0 ? 'var(--vy-ink)' : 'transparent',color: i === 0 ? '#fff' : 'var(--vy-ink-2)'}">{{ t }}</button>
          </div>
        </div>

        <!-- Root -->
        <div style="display:flex;justify-content:center;margin-bottom:36px">
          <div style="background:var(--vy-ink);color:#fff;padding:18px 28px;border-radius:18px;display:flex;align-items:center;gap:14px;box-shadow:var(--vy-shadow-lg);position:relative">
            <div style="position:absolute;top:-10px;right:-10px;background:var(--vy-orange);color:#fff;padding:4px 10px;border-radius:99px;font-size:10px;font-weight:800;letter-spacing:0.1em">TÚ</div>
            <vy-avatar name="CM" :size="44" bg="var(--vy-orange)" color="#fff"/>
            <div>
              <div style="font-weight:800;font-size:14px">{{ NETWORK_ROOT.name }}</div>
              <div style="font-size:11px;color:rgba(255,255,255,.7)">{{ NETWORK_ROOT.role }} · {{ NETWORK_ROOT.sales }}</div>
            </div>
          </div>
        </div>

        <!-- Connection lines -->
        <svg style="position:absolute;top:120px;left:0;width:100%;height:60px;pointer-events:none" viewBox="0 0 800 60" preserveAspectRatio="none">
          <path d="M 400 0 L 400 30 M 130 60 L 130 30 L 670 30 L 670 60 M 400 60 L 400 30" stroke="var(--vy-orange)" stroke-width="2" fill="none" stroke-dasharray="4 4" opacity="0.6"/>
        </svg>

        <!-- Level 1 -->
        <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:18px;margin-top:24px">
          <div v-for="n in NETWORK_ROOT.children" :key="n.id" style="background:var(--vy-surface);padding:18px;border-radius:16px;border:1px solid var(--vy-line);box-shadow:var(--vy-shadow-sm)">
            <div style="display:flex;align-items:center;gap:12px;margin-bottom:14px">
              <vy-avatar :name="n.name.split(' ').map(s => s[0]).join('')" :size="40" bg="var(--vy-cream)"/>
              <div style="flex:1;min-width:0">
                <div style="font-weight:800;font-size:13.5px">{{ n.name }}</div>
                <div style="font-size:11px;color:var(--vy-orange-deep);font-weight:700">{{ n.role }}</div>
              </div>
              <vy-icon name="dots" :size="14" stroke="var(--vy-ink-3)"/>
            </div>
            <div style="display:flex;justify-content:space-between;font-size:11px;color:var(--vy-ink-3);font-weight:600;margin-bottom:10px">
              <span>Volumen mensual</span><span class="vy-mono" style="color:var(--vy-ink);font-weight:800">{{ n.sales }}</span>
            </div>
            <div style="height:4px;border-radius:99px;background:var(--vy-line-2);margin-bottom:14px"><div :style="{height:'100%',width: n.id === 'u1' ? '78%' : (n.id === 'u2' ? '54%' : '48%'),borderRadius:'99px',background:'var(--vy-orange)'}"></div></div>
            <div v-if="n.children" style="border-top:1px dashed var(--vy-line);padding-top:12px">
              <div style="font-size:10px;color:var(--vy-ink-3);font-weight:700;letter-spacing:0.06em;text-transform:uppercase;margin-bottom:8px">{{ n.children.length }} en su línea</div>
              <div v-for="c in n.children" :key="c.id" style="display:flex;align-items:center;gap:10px;padding:6px 0">
                <vy-avatar :name="c.name.split(' ').map(s => s[0]).join('')" :size="26" bg="var(--vy-gray)"/>
                <div style="flex:1;font-size:12px;font-weight:600">{{ c.name }}</div>
                <span class="vy-mono" style="font-size:11px;color:var(--vy-ink-3);font-weight:700">{{ c.sales }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </app-shell>`
});
