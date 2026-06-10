// Panel Administrativo (modo oscuro)
import { defineComponent } from "vue";
import { VyIcon, VyLogo, VyMark, VyAvatar, VyMoney } from "../components/ui.js";

export const ScreenAdmin = defineComponent({
  components: { VyIcon, VyLogo, VyMark, VyAvatar },
  emits: ["navigate"],
  data: () => ({
    sidebar: [
      { l: "Resumen general", icon: "home", active: true },
      { l: "Usuarios", icon: "user", badge: "12K" },
      { l: "Productos", icon: "box" },
      { l: "Pedidos", icon: "cart", badge: "32" },
      { l: "Pagos y retiros", icon: "wallet" },
      { l: "Niveles y bonos", icon: "gift" },
      { l: "Estructura red", icon: "network" },
      { l: "Configuración", icon: "cog" },
    ],
    kpis: [
      { l: "Ventas (mes)",      v: "$ 482,1M", d: "+18,4%", up: true  },
      { l: "Nuevos embajadores", v: "1.284",    d: "+12,1%", up: true  },
      { l: "Pedidos abiertos",   v: "324",       d: "+8,7%",  up: true  },
      { l: "Retiros pendientes", v: "$ 28,4M",  d: "−4,2%",  up: false },
    ],
    orders: [
      { id: "#VY-48201", c: "Carolina M.", t: "$ 168.000", s: "Despachado",   sc: "ok" },
      { id: "#VY-48200", c: "Andrés P.",   t: "$ 412.000", s: "En preparación", sc: "warn" },
      { id: "#VY-48199", c: "Valentina T.", t: "$ 92.000",  s: "Entregado",    sc: "ok" },
      { id: "#VY-48198", c: "Ricardo V.",   t: "$ 240.000", s: "Despachado",   sc: "ok" },
      { id: "#VY-48197", c: "Sofía L.",     t: "$ 145.000", s: "Pendiente pago", sc: "danger" },
      { id: "#VY-48196", c: "Luis F.",      t: "$ 320.000", s: "En preparación", sc: "warn" },
    ],
    bars: [
      { l: "S 1", v: 38 }, { l: "S 2", v: 52 }, { l: "S 3", v: 44 },
      { l: "S 4", v: 68 }, { l: "S 5", v: 82 }, { l: "S 6", v: 96 }
    ],
    activity: [
      { c: "Pedido despachado", d: "VY-48201 · Carolina M.", t: "hace 2m", color: "var(--vy-success)" },
      { c: "Nuevo embajador", d: "Andrea R. se unió a la red de Andrés P.", t: "hace 14m", color: "var(--vy-orange)" },
      { c: "Retiro aprobado",  d: "$ 750.000 · Bancolombia ••4218", t: "hace 22m", color: "#7B92FF" },
      { c: "Producto agotado", d: "Probiótico Equilibrium (SKU PRB-008)", t: "hace 1h", color: "var(--vy-danger)" },
      { c: "Nuevo nivel",     d: "Mariana R. alcanzó Líder Plata", t: "hace 2h", color: "var(--vy-orange)" },
    ]
  }),
  computed: {
    barMax() { return Math.max(...this.bars.map(b => b.v)); }
  },
  methods: {
    sColor(sc) {
      return { ok: "var(--vy-success)", warn: "var(--vy-orange)", danger: "var(--vy-danger)" }[sc] || "var(--vy-ink-3)";
    }
  },
  template: `<div class="vy" style="width:100%;min-height:900px;background:#0F0C08;color:#fff;display:grid;grid-template-columns:240px 1fr">
    <aside style="background:#0A0805;border-right:1px solid rgba(255,255,255,.06);padding:24px 14px;display:flex;flex-direction:column;gap:22px">
      <div style="padding:4px 8px;display:flex;align-items:center;gap:10px">
        <vy-mark :size="22"/>
        <span style="font-family:var(--font-display);font-weight:800;font-size:16px;letter-spacing:-0.02em">Vidayoung</span>
        <span style="font-size:9px;font-weight:800;letter-spacing:0.1em;background:var(--vy-orange);color:#fff;padding:2px 6px;border-radius:99px;margin-left:auto">ADMIN</span>
      </div>
      <nav style="display:flex;flex-direction:column;gap:2px">
        <button v-for="it in sidebar" :key="it.l" :style="{display:'flex',alignItems:'center',gap:'12px',padding:'10px 12px',borderRadius:'10px',fontSize:'13px',fontWeight: it.active ? 700 : 600,background: it.active ? 'rgba(242,135,5,.14)' : 'transparent',color: it.active ? '#fff' : 'rgba(255,255,255,.6)'}">
          <span :style="{color: it.active ? 'var(--vy-orange)' : 'rgba(255,255,255,.4)'}"><vy-icon :name="it.icon" :size="16"/></span>
          <span style="flex:1;text-align:left">{{ it.l }}</span>
          <span v-if="it.badge" style="font-size:10px;font-weight:700;padding:1px 6px;border-radius:99px;background:rgba(255,255,255,.08);color:rgba(255,255,255,.7)">{{ it.badge }}</span>
        </button>
      </nav>
      <div style="margin-top:auto;padding:14px;background:rgba(242,135,5,.1);border:1px solid rgba(242,135,5,.25);border-radius:12px">
        <div style="font-size:11px;font-weight:800;letter-spacing:0.06em;text-transform:uppercase;color:var(--vy-orange);margin-bottom:6px">Salud del sistema</div>
        <div style="display:flex;align-items:center;gap:8px;font-size:12px"><span style="width:8px;height:8px;border-radius:50%;background:var(--vy-success);box-shadow:0 0 0 3px rgba(63,143,92,.25)"></span> Todos los servicios OK</div>
        <div style="font-size:11px;color:rgba(255,255,255,.5);margin-top:4px;font-family:var(--font-mono)">Latencia: 142ms · Uptime: 99,98%</div>
      </div>
    </aside>

    <div style="display:flex;flex-direction:column">
      <header style="display:flex;align-items:center;gap:16px;padding:18px 32px;border-bottom:1px solid rgba(255,255,255,.06)">
        <div>
          <div style="font-size:11px;color:rgba(255,255,255,.5);font-weight:700;letter-spacing:0.06em;text-transform:uppercase">Panel administrativo</div>
          <div style="font-family:var(--font-display);font-size:20px;font-weight:800;margin-top:2px">Resumen operativo</div>
        </div>
        <div style="flex:1"></div>
        <button @click="$emit('navigate','dashboard')" style="font-size:12px;font-weight:700;color:rgba(255,255,255,.7);padding:8px 14px;border:1px solid rgba(255,255,255,.12);border-radius:99px"><vy-icon name="arrowR" :size="12" :sw="2"/> Volver al embajador</button>
        <vy-avatar name="OP" :size="36" bg="var(--vy-orange)" color="#fff"/>
      </header>

      <main style="padding:24px 32px;display:flex;flex-direction:column;gap:14px">
        <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:14px">
          <div v-for="kpi in kpis" :key="kpi.l" style="padding:18px;background:#1A140C;border:1px solid rgba(255,255,255,.06);border-radius:16px">
            <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:10px">
              <div style="font-size:11px;color:rgba(255,255,255,.55);font-weight:700;letter-spacing:0.05em;text-transform:uppercase">{{ kpi.l }}</div>
              <span :style="{padding:'2px 8px',borderRadius:'99px',fontSize:'10px',fontWeight:700,background: kpi.up ? 'rgba(63,143,92,.18)' : 'rgba(196,69,42,.18)',color: kpi.up ? '#7AC793' : '#E68470'}">
                <vy-icon :name="kpi.up ? 'arrowUp' : 'arrowDn'" :size="9"/> {{ kpi.d }}
              </span>
            </div>
            <div style="font-family:var(--font-display);font-size:24px;font-weight:800;letter-spacing:-0.03em">{{ kpi.v }}</div>
          </div>
        </div>

        <div style="display:grid;grid-template-columns:1.5fr 1fr;gap:14px">
          <div style="padding:22px;background:#1A140C;border:1px solid rgba(255,255,255,.06);border-radius:16px">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:18px">
              <div>
                <h3 style="font-size:14px;font-weight:800">Ventas semanales</h3>
                <p style="font-size:11px;color:rgba(255,255,255,.5);margin-top:2px">millones COP · últimas 6 semanas</p>
              </div>
              <span style="font-size:11px;font-weight:700;color:var(--vy-orange);background:rgba(242,135,5,.12);padding:4px 10px;border-radius:99px">+ 38% vs trimestre</span>
            </div>
            <div style="display:flex;align-items:flex-end;gap:14px;height:180px;padding-top:10px">
              <div v-for="b in bars" :key="b.l" style="flex:1;display:flex;flex-direction:column;align-items:center;gap:8px">
                <div style="font-size:10px;color:rgba(255,255,255,.6);font-family:var(--font-mono)">{{ b.v }}M</div>
                <div :style="{width:'100%',background: b.v === barMax ? 'linear-gradient(180deg, var(--vy-orange), var(--vy-orange-deep))' : 'rgba(255,255,255,.12)',borderRadius:'8px 8px 0 0',height: ((b.v / barMax) * 140) + 'px'}"></div>
                <div style="font-size:11px;color:rgba(255,255,255,.5);font-weight:600">{{ b.l }}</div>
              </div>
            </div>
          </div>

          <div style="padding:22px;background:#1A140C;border:1px solid rgba(255,255,255,.06);border-radius:16px">
            <h3 style="font-size:14px;font-weight:800;margin-bottom:18px">Actividad en vivo</h3>
            <div style="display:flex;flex-direction:column;gap:14px">
              <div v-for="(a, i) in activity" :key="i" style="display:flex;gap:12px">
                <div :style="{width:'8px',height:'8px',borderRadius:'50%',background: a.color,marginTop:'8px',flexShrink:0,boxShadow: '0 0 0 3px ' + a.color + '33'}"></div>
                <div style="flex:1">
                  <div style="font-size:13px;font-weight:700">{{ a.c }}</div>
                  <div style="font-size:11.5px;color:rgba(255,255,255,.55);margin-top:2px">{{ a.d }}</div>
                </div>
                <span style="font-size:10.5px;color:rgba(255,255,255,.4);font-family:var(--font-mono);flex-shrink:0">{{ a.t }}</span>
              </div>
            </div>
          </div>
        </div>

        <div style="padding:22px;background:#1A140C;border:1px solid rgba(255,255,255,.06);border-radius:16px">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:14px">
            <div>
              <h3 style="font-size:14px;font-weight:800">Pedidos recientes</h3>
              <p style="font-size:11px;color:rgba(255,255,255,.5);margin-top:2px">324 pedidos abiertos · 12 con incidencia</p>
            </div>
            <div style="display:flex;gap:8px">
              <button style="font-size:11px;font-weight:700;color:#fff;padding:8px 14px;background:rgba(255,255,255,.06);border-radius:99px">Exportar CSV</button>
              <button class="vy-btn vy-btn-primary" style="padding:8px 14px;font-size:11px">Nuevo pedido manual</button>
            </div>
          </div>
          <table style="width:100%;border-collapse:collapse;font-size:13px">
            <thead>
              <tr style="text-align:left;color:rgba(255,255,255,.4);font-weight:600;font-size:10.5px;letter-spacing:0.06em;text-transform:uppercase">
                <th style="padding:10px 0">Pedido</th><th>Cliente</th><th>Total</th><th>Estado</th><th style="text-align:right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="o in orders" :key="o.id" style="border-top:1px solid rgba(255,255,255,.06)">
                <td style="padding:14px 0;font-family:var(--font-mono);font-size:12px;color:rgba(255,255,255,.6)">{{ o.id }}</td>
                <td style="font-weight:700">{{ o.c }}</td>
                <td class="vy-mono" style="font-weight:700">{{ o.t }}</td>
                <td><span :style="{padding:'2px 8px',borderRadius:'99px',fontSize:'10.5px',fontWeight:700,background: sColor(o.sc) + '22', color: sColor(o.sc)}">● {{ o.s }}</span></td>
                <td style="text-align:right"><button style="color:rgba(255,255,255,.5)"><vy-icon name="dots" :size="16"/></button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </main>
    </div>
  </div>`
});
