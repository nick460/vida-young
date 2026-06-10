// Pantallas Embajador (Dashboard, Wallet, Stats, Rewards, Profile, Network)
import { defineComponent } from "vue";
import { VyIcon, VyAvatar, VyMoney, VySparkline, VyDonut, VyBarChart, AppShell } from "../components/ui.js";
import { USER, KPIS, TRANSACTIONS, NETWORK_ROOT } from "../data.js";

// ============ DASHBOARD ============
export const ScreenDashboard = defineComponent({
  components: { AppShell, VyIcon, VyAvatar, VyMoney, VySparkline, VyDonut, VyBarChart },
  emits: ["navigate"],
  data: () => ({ USER, KPIS, TRANSACTIONS,
    sparks: {
      0: [320, 340, 360, 355, 380, 410, 420, 460, 472, 480, 510, 528],
      1: [80, 95, 90, 110, 120, 145, 160, 155, 168, 175, 180, 195],
      2: [12, 18, 16, 24, 22, 30, 36, 32, 40, 44, 46, 48],
      3: [80, 92, 100, 108, 115, 122, 128, 130, 134, 136, 137, 138],
    },
    chartData: [
      { l: "Ene", v: 4.2 }, { l: "Feb", v: 5.1 }, { l: "Mar", v: 4.8 },
      { l: "Abr", v: 6.4 }, { l: "May", v: 7.8 }, { l: "Jun", v: 8.6 },
      { l: "Jul", v: 9.2 }, { l: "Ago", v: 10.4 }, { l: "Sep", v: 11.1 },
      { l: "Oct", v: 12.0 }, { l: "Nov", v: 11.8 }, { l: "Dic", v: 12.4 },
    ]
  }),
  template: `<app-shell active="dashboard" @navigate="$emit('navigate', $event)">
    <div style="display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:24px">
      <div>
        <div class="vy-eyebrow" style="margin-bottom:8px">Hola {{ USER.short.split(' ')[0] }}, jueves 9 de mayo</div>
        <h1 style="font-size:30px;font-weight:800">Tu bienestar financiero, hoy</h1>
        <p style="font-size:14px;color:var(--vy-ink-2);margin-top:4px">Has crecido 24% este mes. Vas en camino al rango Diamante Élite.</p>
      </div>
      <div style="display:flex;gap:10px">
        <button class="vy-btn vy-btn-ghost" style="padding:10px 16px"><vy-icon name="arrowDn" :size="14"/> Retirar</button>
        <button class="vy-btn vy-btn-primary" style="padding:10px 18px"><vy-icon name="plus" :size="14"/> Invitar embajador</button>
      </div>
    </div>

    <div style="display:grid;grid-template-columns:repeat(4, 1fr);gap:14px;margin-bottom:18px">
      <div v-for="(kpi, i) in KPIS" :key="i" class="vy-card" style="padding:18px">
        <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:10px">
          <div style="font-size:12px;color:var(--vy-ink-3);font-weight:600">{{ kpi.label }}</div>
          <span :class="['vy-chip', kpi.up ? 'vy-chip-success' : '']" style="font-size:10px">
            <vy-icon :name="kpi.up ? 'arrowUp' : 'arrowDn'" :size="10"/> {{ kpi.delta }}
          </span>
        </div>
        <div style="font-family:var(--font-display);font-size:24px;font-weight:800;letter-spacing:-0.03em;line-height:1.05">{{ kpi.value }}</div>
        <div style="margin-top:10px;display:flex;justify-content:space-between;align-items:flex-end">
          <div style="font-size:11px;color:var(--vy-ink-3)">{{ kpi.hint }}</div>
          <vy-sparkline :data="sparks[i]" :width="80" :height="28"/>
        </div>
      </div>
    </div>

    <div style="display:grid;grid-template-columns:1.6fr 1fr;gap:14px;margin-bottom:18px">
      <div class="vy-card" style="padding:22px">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:18px">
          <div>
            <h3 style="font-size:16px;font-weight:800">Ganancias acumuladas</h3>
            <p style="font-size:12px;color:var(--vy-ink-3);margin-top:2px">Últimos 12 meses · valor en millones COP</p>
          </div>
          <div style="display:flex;gap:6px;background:var(--vy-cream);padding:4px;border-radius:99px">
            <button v-for="t in ['Año','6M','3M']" :key="t"
                    :style="{padding:'6px 14px',borderRadius:'99px',fontSize:'12px',fontWeight:700,background: t==='Año' ? 'var(--vy-surface)' : 'transparent', boxShadow: t==='Año' ? 'var(--vy-shadow-sm)' : 'none'}">{{ t }}</button>
          </div>
        </div>
        <vy-bar-chart :data="chartData" :width="640" :height="180"/>
      </div>

      <div class="vy-card" style="padding:22px;background:linear-gradient(140deg, var(--vy-cream) 0%, #FFF 60%);position:relative;overflow:hidden">
        <div style="position:absolute;right:-30px;bottom:-30px;width:160px;height:160px;border-radius:50%;background:rgba(242,135,5,.18)"></div>
        <div style="position:relative">
          <div class="vy-chip vy-chip-orange" style="margin-bottom:14px">Tu nivel actual</div>
          <h3 style="font-size:24px;font-weight:800;letter-spacing:-0.03em">Líder Diamante</h3>
          <p style="font-size:13px;color:var(--vy-ink-2);margin-top:4px">Top 8% de embajadores en Colombia</p>
          <div style="display:flex;align-items:center;gap:18px;margin-top:18px">
            <vy-donut :value="68" :size="124" :stroke="11"/>
            <div style="font-size:13px">
              <div style="font-weight:700;margin-bottom:6px">Próximo: Diamante Élite</div>
              <div style="color:var(--vy-ink-2);line-height:1.45;font-size:12px">Cierre de mes el 31 de mayo. Faltan <b>$ 1,2M</b> en volumen para alcanzarlo.</div>
              <button class="vy-btn vy-btn-dark" style="margin-top:14px;padding:8px 14px;font-size:12px">Ver requisitos</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="vy-card" style="padding:22px">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:14px">
        <div>
          <h3 style="font-size:16px;font-weight:800">Movimientos recientes</h3>
          <p style="font-size:12px;color:var(--vy-ink-3);margin-top:2px">Últimas transacciones de tu cuenta</p>
        </div>
        <button @click="$emit('navigate','wallet')" style="font-size:13px;font-weight:700;color:var(--vy-orange-deep);display:flex;align-items:center;gap:4px">Ver todo <vy-icon name="arrowR" :size="14"/></button>
      </div>
      <table style="width:100%;border-collapse:collapse;font-size:13px">
        <thead>
          <tr style="text-align:left;color:var(--vy-ink-3);font-weight:600;font-size:11px;letter-spacing:0.06em;text-transform:uppercase">
            <th style="padding:10px 0">ID</th><th>Concepto</th><th>Fecha</th><th>Estado</th><th style="text-align:right">Monto</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tx in TRANSACTIONS.slice(0,5)" :key="tx.id" style="border-top:1px solid var(--vy-line-2)">
            <td style="padding:14px 0;font-family:var(--font-mono);font-size:12px;color:var(--vy-ink-3)">{{ tx.id }}</td>
            <td style="font-weight:700">{{ tx.type }}</td>
            <td style="color:var(--vy-ink-2)">{{ tx.date }}</td>
            <td><span class="vy-chip vy-chip-success" style="font-size:10px">● {{ tx.status }}</span></td>
            <td style="text-align:right"><vy-money :v="tx.amt" sign/></td>
          </tr>
        </tbody>
      </table>
    </div>
  </app-shell>`
});

// ============ WALLET / FINANZAS ============
export const ScreenWallet = defineComponent({
  components: { AppShell, VyIcon, VyMoney, VySparkline },
  emits: ["navigate"],
  data: () => ({ TRANSACTIONS,
    methods: [
      { l: "Bancolombia · ahorros · ••4218", primary: true },
      { l: "Nequi · ••8841", primary: false },
    ]
  }),
  template: `<app-shell active="wallet" @navigate="$emit('navigate', $event)">
    <h1 style="font-size:28px;font-weight:800;margin-bottom:6px">Finanzas</h1>
    <p style="color:var(--vy-ink-2);font-size:14px;margin-bottom:24px">Saldo, retiros y todo el historial de tu actividad financiera.</p>

    <div style="display:grid;grid-template-columns:1.4fr 1fr;gap:14px;margin-bottom:18px">
      <div class="vy-card" style="padding:24px;background:linear-gradient(135deg, var(--vy-ink) 0%, #2D2418 100%);color:#fff;position:relative;overflow:hidden">
        <div style="position:absolute;right:-40px;top:-40px;width:200px;height:200px;border-radius:50%;background:radial-gradient(circle, rgba(242,135,5,.4), transparent 70%)"></div>
        <div style="position:relative;display:flex;justify-content:space-between;align-items:flex-start">
          <div>
            <div style="font-size:11px;letter-spacing:0.18em;text-transform:uppercase;color:rgba(255,255,255,.6);font-weight:700;margin-bottom:10px">Saldo disponible</div>
            <div style="font-family:var(--font-display);font-size:40px;font-weight:800;letter-spacing:-0.04em;line-height:1">$ 4.281.500</div>
            <div style="margin-top:10px;font-size:13px;color:rgba(255,255,255,.7)">Próximo corte: viernes 31 de mayo</div>
          </div>
          <div style="font-family:var(--font-display);font-weight:800;letter-spacing:-0.03em">VY</div>
        </div>
        <div style="display:flex;gap:10px;margin-top:28px;position:relative">
          <button class="vy-btn vy-btn-primary"><vy-icon name="arrowDn" :size="14"/> Retirar fondos</button>
          <button class="vy-btn vy-btn-ghost" style="background:rgba(255,255,255,.1);color:#fff;border-color:rgba(255,255,255,.2)"><vy-icon name="copy" :size="14"/> Detalles</button>
        </div>
      </div>
      <div class="vy-card" style="padding:22px">
        <div style="font-size:11px;letter-spacing:0.18em;text-transform:uppercase;color:var(--vy-ink-3);font-weight:700;margin-bottom:12px">Métodos de cobro</div>
        <div v-for="(m, i) in methods" :key="i" :style="{padding:'14px',borderRadius:'12px',border: m.primary ? '2px solid var(--vy-orange)' : '1px solid var(--vy-line)', background: m.primary ? 'rgba(242,135,5,.04)' : 'transparent', marginBottom: i < methods.length - 1 ? '10px' : '0', display:'flex',justifyContent:'space-between',alignItems:'center'}">
          <div>
            <div style="font-weight:700;font-size:13.5px">{{ m.l }}</div>
            <div v-if="m.primary" style="font-size:11px;color:var(--vy-orange-deep);font-weight:700;margin-top:2px">Cuenta principal</div>
          </div>
          <vy-icon name="dots" :size="16" stroke="var(--vy-ink-3)"/>
        </div>
        <button class="vy-btn vy-btn-ghost" style="width:100%;margin-top:12px;padding:10px;font-size:13px"><vy-icon name="plus" :size="14"/> Agregar cuenta</button>
      </div>
    </div>

    <div class="vy-card" style="padding:22px">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:14px">
        <h3 style="font-size:16px;font-weight:800">Historial completo</h3>
        <div style="display:flex;gap:8px">
          <span class="vy-chip vy-chip-cream">Todas (148)</span>
          <span class="vy-chip" style="background:var(--vy-gray);color:var(--vy-ink-2)">Comisiones</span>
          <span class="vy-chip" style="background:var(--vy-gray);color:var(--vy-ink-2)">Bonos</span>
          <span class="vy-chip" style="background:var(--vy-gray);color:var(--vy-ink-2)">Retiros</span>
        </div>
      </div>
      <table style="width:100%;border-collapse:collapse;font-size:13px">
        <thead>
          <tr style="text-align:left;color:var(--vy-ink-3);font-weight:600;font-size:11px;letter-spacing:0.06em;text-transform:uppercase">
            <th style="padding:10px 0">ID</th><th>Concepto</th><th>Fecha</th><th>Estado</th><th style="text-align:right">Monto</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tx in TRANSACTIONS" :key="tx.id" style="border-top:1px solid var(--vy-line-2)">
            <td style="padding:14px 0;font-family:var(--font-mono);font-size:12px;color:var(--vy-ink-3)">{{ tx.id }}</td>
            <td style="font-weight:700">{{ tx.type }}</td>
            <td style="color:var(--vy-ink-2)">{{ tx.date }}</td>
            <td><span class="vy-chip vy-chip-success" style="font-size:10px">● {{ tx.status }}</span></td>
            <td style="text-align:right"><vy-money :v="tx.amt" sign/></td>
          </tr>
        </tbody>
      </table>
    </div>
  </app-shell>`
});
