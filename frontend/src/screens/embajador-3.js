// Pantallas: Profile, Rewards, Stats
import { defineComponent } from "vue";
import { VyIcon, VyAvatar, VyMoney, VyDonut, VyBarChart, VySparkline, AppShell } from "../components/ui.js";
import { USER } from "../data.js";

// ============ PERFIL ============
export const ScreenProfile = defineComponent({
  components: { AppShell, VyIcon, VyAvatar, VyDonut },
  emits: ["navigate"],
  data: () => ({ USER,
    achievements: [
      { l: "Primera venta", d: "Mar 2024", done: true },
      { l: "10 referidos directos", d: "Jul 2024", done: true },
      { l: "Líder Plata", d: "Sep 2024", done: true },
      { l: "Líder Oro", d: "Nov 2024", done: true },
      { l: "Líder Diamante", d: "Mar 2025", done: true },
      { l: "Líder Diamante Élite", d: "—", done: false },
    ]
  }),
  template: `<app-shell active="profile" @navigate="$emit('navigate', $event)">
    <h1 style="font-size:28px;font-weight:800;margin-bottom:6px">Tu perfil</h1>
    <p style="color:var(--vy-ink-2);font-size:14px;margin-bottom:24px">Datos personales, configuración de cuenta y trayectoria en Vidayoung.</p>

    <div style="display:grid;grid-template-columns:1fr 1.6fr;gap:14px">
      <div class="vy-card" style="padding:28px;text-align:center;background:linear-gradient(180deg, var(--vy-cream) 0%, var(--vy-surface) 50%);border-color:transparent">
        <div style="position:relative;display:inline-block">
          <vy-avatar :name="USER.avatar" :size="92" bg="var(--vy-orange)" color="#fff"/>
          <button style="position:absolute;bottom:0;right:0;width:30px;height:30px;border-radius:50%;background:var(--vy-ink);color:#fff;display:flex;align-items:center;justify-content:center;border:3px solid var(--vy-surface)"><vy-icon name="cog" :size="12"/></button>
        </div>
        <h2 style="font-size:22px;font-weight:800;margin-top:16px;letter-spacing:-0.02em">{{ USER.name }}</h2>
        <p style="font-size:12px;color:var(--vy-ink-3);margin-top:4px">{{ USER.email }}</p>
        <div class="vy-chip vy-chip-orange" style="margin-top:12px"><vy-icon name="star" :size="11" fill="currentColor" stroke="currentColor"/> {{ USER.level }}</div>

        <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin-top:24px;text-align:left">
          <div v-for="s in [{l:'Red',v:'138'},{l:'Ventas',v:'$12M'},{l:'Bonos',v:'24'}]" :key="s.l" style="padding:12px;background:var(--vy-surface);border-radius:10px;border:1px solid var(--vy-line)">
            <div style="font-size:10px;color:var(--vy-ink-3);font-weight:700;letter-spacing:0.06em;text-transform:uppercase">{{ s.l }}</div>
            <div style="font-family:var(--font-display);font-weight:800;font-size:18px;margin-top:2px">{{ s.v }}</div>
          </div>
        </div>

        <button class="vy-btn vy-btn-dark" style="margin-top:20px;width:100%">Editar perfil</button>
      </div>

      <div style="display:flex;flex-direction:column;gap:14px">
        <div class="vy-card" style="padding:22px">
          <h3 style="font-size:14px;font-weight:800;margin-bottom:16px">Información personal</h3>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;font-size:13px">
            <div v-for="f in [{l:'Nombre',v:USER.name},{l:'Correo',v:USER.email},{l:'Código embajador',v:USER.code,mono:true},{l:'Ciudad',v:USER.city},{l:'Miembro desde',v:USER.joined},{l:'Patrocinador',v:'Vidayoung Founders'}]" :key="f.l">
              <div style="font-size:11px;color:var(--vy-ink-3);font-weight:700;letter-spacing:0.06em;text-transform:uppercase;margin-bottom:4px">{{ f.l }}</div>
              <div :class="{'vy-mono': f.mono}" style="font-weight:600">{{ f.v }}</div>
            </div>
          </div>
        </div>

        <div class="vy-card" style="padding:22px">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:18px">
            <h3 style="font-size:14px;font-weight:800">Trayectoria</h3>
            <span class="vy-chip vy-chip-success">5 de 6 desbloqueados</span>
          </div>
          <div style="display:flex;flex-direction:column;gap:10px">
            <div v-for="(a, i) in achievements" :key="a.l" style="display:flex;align-items:center;gap:14px">
              <div :style="{width:'36px',height:'36px',borderRadius:'50%',background: a.done ? 'var(--vy-orange)' : 'var(--vy-surface-2)',color: a.done ? '#fff' : 'var(--vy-ink-3)',display:'flex',alignItems:'center',justifyContent:'center',flexShrink:0,border: a.done ? 'none' : '2px dashed var(--vy-line)'}">
                <vy-icon :name="a.done ? 'check' : 'sparkle'" :size="16"/>
              </div>
              <div style="flex:1">
                <div :style="{fontWeight:700,fontSize:'13.5px',color: a.done ? 'var(--vy-ink)' : 'var(--vy-ink-3)'}">{{ a.l }}</div>
                <div style="font-size:11px;color:var(--vy-ink-3);margin-top:2px">{{ a.d }}</div>
              </div>
              <span v-if="!a.done" style="font-size:11px;color:var(--vy-orange-deep);font-weight:700">En progreso</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </app-shell>`
});

// ============ RECOMPENSAS ============
export const ScreenRewards = defineComponent({
  components: { AppShell, VyIcon, VyDonut },
  emits: ["navigate"],
  data: () => ({
    bonuses: [
      { l: "Bono inicio rápido", d: "Recompensa por activar 3 referidos en tu primer mes", v: 150000, status: "Activo", prog: 100 },
      { l: "Bono liderazgo Q2", d: "Cierra trimestre con $30M en volumen de equipo", v: 800000, status: "65%", prog: 65 },
      { l: "Bono viaje Cartagena", d: "Mantén rango Diamante por 3 meses consecutivos", v: 0, status: "2/3 meses", prog: 66 },
    ],
    rewards: [
      { l: "Premio en efectivo", icon: "wallet", c: "Comisiones directas, semanales, sin tope" },
      { l: "Productos gratis", icon: "gift", c: "Incentivos por volumen mensual" },
      { l: "Viajes con la marca", icon: "sparkle", c: "Convenciones internacionales todo pago" },
      { l: "Auto Vidayoung", icon: "truck", c: "Bono de auto al alcanzar Diamante Élite" },
    ]
  }),
  template: `<app-shell active="rewards" @navigate="$emit('navigate', $event)">
    <div style="display:flex;justify-content:space-between;align-items:flex-end;margin-bottom:24px">
      <div>
        <div class="vy-eyebrow" style="margin-bottom:8px">Recompensas y bonos</div>
        <h1 style="font-size:30px;font-weight:800">Tu crecimiento, recompensado</h1>
        <p style="font-size:14px;color:var(--vy-ink-2);margin-top:4px">3 bonos activos · próximo cierre el 31 de mayo</p>
      </div>
    </div>

    <div class="vy-card" style="padding:0;margin-bottom:18px;display:grid;grid-template-columns:1fr 1fr;overflow:hidden">
      <div style="padding:32px;background:linear-gradient(135deg, var(--vy-orange) 0%, var(--vy-orange-deep) 100%);color:#fff;position:relative;overflow:hidden">
        <div style="position:absolute;right:-40px;bottom:-40px;width:200px;height:200px;border-radius:50%;background:rgba(255,255,255,.15)"></div>
        <div style="position:relative">
          <div style="font-size:11px;letter-spacing:0.18em;text-transform:uppercase;font-weight:700;opacity:0.85;margin-bottom:10px">Bonos acumulados 2026</div>
          <div style="font-family:var(--font-display);font-size:44px;font-weight:800;letter-spacing:-0.04em;line-height:1">$ 4.820.000</div>
          <p style="margin-top:14px;font-size:14px;line-height:1.5;opacity:0.9">Has acumulado 24 bonos este año entre comisiones de red, liderazgo y volumen personal.</p>
          <div style="display:flex;gap:24px;margin-top:24px">
            <div><div style="font-size:11px;opacity:0.8;font-weight:700;letter-spacing:0.06em;text-transform:uppercase">Bonos pagados</div><div style="font-family:var(--font-display);font-size:22px;font-weight:800;margin-top:2px">21</div></div>
            <div><div style="font-size:11px;opacity:0.8;font-weight:700;letter-spacing:0.06em;text-transform:uppercase">En curso</div><div style="font-family:var(--font-display);font-size:22px;font-weight:800;margin-top:2px">3</div></div>
            <div><div style="font-size:11px;opacity:0.8;font-weight:700;letter-spacing:0.06em;text-transform:uppercase">Pendientes</div><div style="font-family:var(--font-display);font-size:22px;font-weight:800;margin-top:2px">8</div></div>
          </div>
        </div>
      </div>
      <div style="padding:32px;display:flex;flex-direction:column;justify-content:center">
        <div style="display:flex;align-items:center;gap:24px">
          <vy-donut :value="68" :size="140" :stroke="14"/>
          <div>
            <div style="font-size:11px;color:var(--vy-ink-3);font-weight:700;letter-spacing:0.06em;text-transform:uppercase;margin-bottom:6px">Próximo gran bono</div>
            <h3 style="font-size:20px;font-weight:800;letter-spacing:-0.02em">Viaje Cartagena 2026</h3>
            <p style="font-size:12.5px;color:var(--vy-ink-2);margin-top:6px;line-height:1.4">Faltan <b>3 referidos directos activos</b> para asegurar el cupo.</p>
            <button class="vy-btn vy-btn-dark" style="margin-top:14px;padding:8px 14px;font-size:12px">Ver requisitos completos</button>
          </div>
        </div>
      </div>
    </div>

    <div style="display:grid;grid-template-columns:1.4fr 1fr;gap:14px">
      <div class="vy-card" style="padding:22px">
        <h3 style="font-size:14px;font-weight:800;margin-bottom:18px">Bonos en curso</h3>
        <div style="display:flex;flex-direction:column;gap:14px">
          <div v-for="b in bonuses" :key="b.l" style="padding:18px;border-radius:14px;border:1px solid var(--vy-line);background:var(--vy-surface-2)">
            <div style="display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:10px">
              <div>
                <div style="font-weight:800;font-size:14px">{{ b.l }}</div>
                <div style="font-size:12px;color:var(--vy-ink-2);margin-top:2px">{{ b.d }}</div>
              </div>
              <div style="text-align:right">
                <div v-if="b.v" class="vy-mono" style="font-weight:800;font-size:16px;color:var(--vy-orange-deep)">$ {{ b.v.toLocaleString('es-CO') }}</div>
                <span class="vy-chip" :style="{background: b.status === 'Activo' ? 'rgba(63,143,92,.12)' : 'var(--vy-cream)', color: b.status === 'Activo' ? 'var(--vy-success)' : '#6B4A12',marginTop:'4px'}">{{ b.status }}</span>
              </div>
            </div>
            <div style="height:6px;border-radius:99px;background:var(--vy-line)">
              <div :style="{height:'100%',width: b.prog + '%',borderRadius:'99px',background: b.prog === 100 ? 'var(--vy-success)' : 'var(--vy-orange)'}"></div>
            </div>
          </div>
        </div>
      </div>

      <div class="vy-card" style="padding:22px">
        <h3 style="font-size:14px;font-weight:800;margin-bottom:18px">Tipos de recompensa</h3>
        <div style="display:flex;flex-direction:column;gap:12px">
          <div v-for="r in rewards" :key="r.l" style="display:flex;gap:14px;padding:14px;border-radius:12px;background:var(--vy-surface-2)">
            <div style="width:38px;height:38px;border-radius:10px;background:var(--vy-cream);color:var(--vy-orange-deep);display:flex;align-items:center;justify-content:center;flex-shrink:0"><vy-icon :name="r.icon" :size="16"/></div>
            <div>
              <div style="font-weight:800;font-size:13.5px">{{ r.l }}</div>
              <div style="font-size:12px;color:var(--vy-ink-2);margin-top:2px;line-height:1.4">{{ r.c }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </app-shell>`
});

// ============ ESTADÍSTICAS ============
export const ScreenStats = defineComponent({
  components: { AppShell, VyIcon, VyBarChart, VyDonut, VySparkline },
  emits: ["navigate"],
  data: () => ({
    salesByMonth: [
      { l: "Ene", v: 4.2 }, { l: "Feb", v: 5.1 }, { l: "Mar", v: 4.8 },
      { l: "Abr", v: 6.4 }, { l: "May", v: 7.8 }, { l: "Jun", v: 8.6 },
    ],
    cohort: [
      { l: "Sem 1", v: 92 }, { l: "Sem 2", v: 78 }, { l: "Sem 3", v: 64 },
      { l: "Sem 4", v: 56 }, { l: "Sem 5", v: 49 }, { l: "Sem 6", v: 44 }, { l: "Sem 7", v: 41 },
    ],
    cats: [
      { l: "Cuidado facial", v: 38, c: "var(--vy-orange)" },
      { l: "Suplementos",    v: 28, c: "var(--vy-beige)" },
      { l: "Aromaterapia",   v: 18, c: "var(--vy-cream)" },
      { l: "Nutrición",      v: 16, c: "var(--vy-ink-2)" },
    ]
  }),
  template: `<app-shell active="stats" @navigate="$emit('navigate', $event)">
    <h1 style="font-size:28px;font-weight:800;margin-bottom:6px">Estadísticas</h1>
    <p style="color:var(--vy-ink-2);font-size:14px;margin-bottom:24px">Indicadores de rendimiento, retención de equipo y mix de productos.</p>

    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-bottom:18px">
      <div v-for="kpi in [{l:'Ventas totales',v:'$12,4M',d:'+18%'},{l:'Volumen equipo',v:'$48,9M',d:'+24%'},{l:'Retención red',v:'87%',d:'+3pp'},{l:'Tasa conversión',v:'4,2%',d:'+0,8pp'}]" :key="kpi.l" class="vy-card" style="padding:18px">
        <div style="font-size:11px;color:var(--vy-ink-3);font-weight:700;letter-spacing:0.06em;text-transform:uppercase">{{ kpi.l }}</div>
        <div style="display:flex;align-items:baseline;gap:8px;margin-top:8px">
          <div style="font-family:var(--font-display);font-size:24px;font-weight:800;letter-spacing:-0.03em">{{ kpi.v }}</div>
          <span class="vy-chip vy-chip-success" style="font-size:10px">{{ kpi.d }}</span>
        </div>
      </div>
    </div>

    <div style="display:grid;grid-template-columns:1.4fr 1fr;gap:14px;margin-bottom:18px">
      <div class="vy-card" style="padding:22px">
        <h3 style="font-size:14px;font-weight:800;margin-bottom:18px">Ventas por mes (millones COP)</h3>
        <vy-bar-chart :data="salesByMonth" :width="560" :height="200"/>
      </div>
      <div class="vy-card" style="padding:22px">
        <h3 style="font-size:14px;font-weight:800;margin-bottom:18px">Mix de categorías</h3>
        <div style="display:flex;flex-direction:column;gap:10px">
          <div v-for="c in cats" :key="c.l">
            <div style="display:flex;justify-content:space-between;font-size:13px;font-weight:700;margin-bottom:6px">
              <span>{{ c.l }}</span><span class="vy-mono">{{ c.v }}%</span>
            </div>
            <div style="height:8px;border-radius:99px;background:var(--vy-line-2)">
              <div :style="{height:'100%',width: c.v + '%',borderRadius:'99px',background: c.c}"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="vy-card" style="padding:22px">
      <h3 style="font-size:14px;font-weight:800;margin-bottom:6px">Retención de cohorte (% activos por semana)</h3>
      <p style="font-size:12px;color:var(--vy-ink-3);margin-bottom:18px">Cohorte de embajadores activados en marzo 2026</p>
      <div style="display:grid;grid-template-columns:repeat(7,1fr);gap:8px">
        <div v-for="c in cohort" :key="c.l" style="text-align:center">
          <div :style="{height:'80px',borderRadius:'10px',background:'var(--vy-orange)',opacity: c.v / 100,display:'flex',alignItems:'center',justifyContent:'center',color:'#fff',fontWeight:800,fontFamily:'var(--font-display)'}">{{ c.v }}%</div>
          <div style="font-size:11px;color:var(--vy-ink-3);font-weight:600;margin-top:6px">{{ c.l }}</div>
        </div>
      </div>
    </div>
  </app-shell>`
});
