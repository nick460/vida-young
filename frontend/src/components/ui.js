// Componentes UI compartidos — Vue 3 con templates en string
import { defineComponent, computed, h } from "vue";

// ============ ICONOS ============
const ICON_PATHS = {
  home:    "M3 11l9-7 9 7v9a2 2 0 0 1-2 2h-4v-6h-6v6H5a2 2 0 0 1-2-2v-9z",
  wallet:  "M3 7h15a3 3 0 0 1 3 3v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7zm0 0V6a2 2 0 0 1 2-2h11M17 14h2",
  shop:    "M4 8h16l-1.5 11a2 2 0 0 1-2 1.8H7.5a2 2 0 0 1-2-1.8L4 8zm4 0V6a4 4 0 0 1 8 0v2",
  network: "M12 4v4M5 20v-4m14 4v-4M12 12V8m0 4l-7 4m7-4l7 4M9 6a3 3 0 1 1 6 0 3 3 0 0 1-6 0zM2 18a3 3 0 1 1 6 0 3 3 0 0 1-6 0zm14 0a3 3 0 1 1 6 0 3 3 0 0 1-6 0z",
  user:    "M5 21a7 7 0 0 1 14 0M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z",
  gift:    "M3 12h18v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-8zm-1-4h20v4H2V8zm10 0V5m0 3c-2 0-4-1-4-3s2-2 4 0c2-2 4-2 4 0s-2 3-4 3z",
  bars:    "M4 20V10m6 10V4m6 16v-7m6 7V8",
  cog:     "M12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6zm9 3a9 9 0 0 1-.2 1.9l2 1.6-2 3.4-2.4-.8a9 9 0 0 1-3.3 1.9L14.5 23h-5l-.6-2.5a9 9 0 0 1-3.3-1.9l-2.4.8-2-3.4 2-1.6A9 9 0 0 1 3 12a9 9 0 0 1 .2-1.9l-2-1.6 2-3.4 2.4.8a9 9 0 0 1 3.3-1.9L9.5 1h5l.6 2.5a9 9 0 0 1 3.3 1.9l2.4-.8 2 3.4-2 1.6c.1.6.2 1.3.2 1.9z",
  bell:    "M6 8a6 6 0 1 1 12 0c0 7 3 8 3 8H3s3-1 3-8zm3 13a3 3 0 0 0 6 0",
  search:  "M11 19a8 8 0 1 0 0-16 8 8 0 0 0 0 16zm10 2l-4.3-4.3",
  arrowUp: "M12 19V5m0 0l-7 7m7-7l7 7",
  arrowDn: "M12 5v14m0 0l7-7m-7 7l-7-7",
  arrowR:  "M5 12h14m0 0l-7-7m7 7l-7 7",
  check:   "M4 12l5 5L20 6",
  plus:    "M12 5v14m-7-7h14",
  minus:   "M5 12h14",
  star:    "M12 3l2.7 5.6 6.3.9-4.5 4.4 1 6.3L12 17.3 6.5 20.2l1-6.3L3 9.5l6.3-.9L12 3z",
  heart:   "M12 21s-8-5.4-8-11a5 5 0 0 1 9-3 5 5 0 0 1 9 3c0 5.6-8 11-8 11z",
  cart:    "M3 4h2l3 12h11l2-8H6m3 14a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3zm10 0a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z",
  copy:    "M8 8V4a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2h-4M4 8h10a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V10a2 2 0 0 1 2-2z",
  shield:  "M12 3l8 3v6c0 5-3.5 8.5-8 9-4.5-.5-8-4-8-9V6l8-3z",
  sparkle: "M12 3v6m0 6v6m9-9h-6M9 12H3m13.5-6.5l-3 3m-5 5l-3 3m11 0l-3-3m-5-5l-3-3",
  truck:   "M3 16V6h11v10M14 9h4l3 4v3h-7m-7 0a2 2 0 1 0 4 0 2 2 0 0 0-4 0zm10 0a2 2 0 1 0 4 0 2 2 0 0 0-4 0z",
  dots:    "M5 12h.01M12 12h.01M19 12h.01",
  chevR:   "M9 6l6 6-6 6",
  chevD:   "M6 9l6 6 6-6",
  leaf:    "M5 19c8 0 14-6 14-14C11 5 5 11 5 19zm0 0l9-9",
  key:     "M14 6a4 4 0 1 1-3.7 5.5L4 18v3h3v-2h2v-2h2l1.5-1.5A4 4 0 1 1 14 6z",
  box:     "M3 7l9-4 9 4v10l-9 4-9-4V7zm9-4v18M3 7l9 4m9-4l-9 4",
};

export const VyIcon = defineComponent({
  props: { name: String, size: { type: [Number, String], default: 18 }, stroke: { default: "currentColor" }, fill: { default: "none" }, sw: { default: 1.6 } },
  template: `<svg :width="size" :height="size" viewBox="0 0 24 24" :fill="fill" :stroke="stroke" :stroke-width="sw" stroke-linecap="round" stroke-linejoin="round"><path :d="path"/></svg>`,
  computed: { path() { return ICON_PATHS[this.name] || ""; } }
});

// ============ LOGO ============
export const VyMark = defineComponent({
  props: { size: { default: 28 }, color: { default: "var(--vy-orange)" } },
  template: `<svg :width="size" :height="size" viewBox="0 0 64 64" fill="none" aria-hidden="true">
    <path d="M10 14 L32 56 L54 14 L44 14 L32 38 L20 14 Z" :fill="color"/>
    <path d="M26 8 L32 22 L38 8 Z" :fill="color"/>
  </svg>`
});

export const VyLogo = defineComponent({
  components: { VyMark },
  props: { size: { default: 28 }, dark: Boolean, tagline: Boolean },
  computed: { tone() { return this.dark ? "#fff" : "var(--vy-ink)"; } },
  template: `<div style="display:inline-flex;align-items:center;gap:10px">
    <vy-mark :size="size"/>
    <div style="display:flex;flex-direction:column;line-height:1">
      <span :style="{fontFamily:'var(--font-display)',fontWeight:800,fontSize:(size*0.78)+'px',color:tone,letterSpacing:'-0.03em'}">Vidayoung</span>
      <span v-if="tagline" :style="{fontSize:(size*0.28)+'px',fontWeight:700,marginTop:'4px',letterSpacing:'0.22em',textTransform:'uppercase',color:'var(--vy-orange-deep)'}">Generando bienestar</span>
    </div>
  </div>`
});

// ============ AVATAR ============
export const VyAvatar = defineComponent({
  props: { name: String, size: { default: 36 }, bg: { default: "var(--vy-cream)" }, color: { default: "var(--vy-ink)" } },
  template: `<div :style="{width:size+'px',height:size+'px',borderRadius:'50%',background:bg,color:color,display:'flex',alignItems:'center',justifyContent:'center',fontWeight:700,fontSize:(size*0.38)+'px',letterSpacing:'-0.02em',flexShrink:0}">{{ name }}</div>`
});

// ============ MONEY ============
export const VyMoney = defineComponent({
  props: { v: Number, sign: Boolean },
  computed: {
    neg() { return this.v < 0; },
    txt() { return "$ " + Math.abs(this.v).toLocaleString("es-CO"); },
    prefix() { return this.sign ? (this.neg ? "− " : "+ ") : (this.neg ? "− " : ""); }
  },
  template: `<span class="vy-mono" :style="{color: neg ? 'var(--vy-danger)' : 'inherit', fontWeight:700}">{{ prefix }}{{ txt }}</span>`
});

// ============ CHARTS ============
export const VySparkline = defineComponent({
  props: { data: Array, width: { default: 200 }, height: { default: 56 }, color: { default: "var(--vy-orange)" }, fill: { default: "rgba(242,135,5,.14)" } },
  computed: {
    paths() {
      const { data, width, height } = this;
      const max = Math.max(...data), min = Math.min(...data);
      const range = max - min || 1;
      const dx = width / (data.length - 1);
      const pts = data.map((v, i) => [i * dx, height - ((v - min) / range) * (height - 8) - 4]);
      const line = pts.map((p, i) => (i ? "L" : "M") + p[0].toFixed(1) + " " + p[1].toFixed(1)).join(" ");
      const area = line + ` L ${width} ${height} L 0 ${height} Z`;
      return { line, area, last: pts[pts.length - 1] };
    }
  },
  template: `<svg :width="width" :height="height" :viewBox="'0 0 ' + width + ' ' + height">
    <path :d="paths.area" :fill="fill"/>
    <path :d="paths.line" fill="none" :stroke="color" stroke-width="2" stroke-linejoin="round" stroke-linecap="round"/>
    <circle :cx="paths.last[0]" :cy="paths.last[1]" r="4" fill="#fff" :stroke="color" stroke-width="2"/>
  </svg>`
});

export const VyDonut = defineComponent({
  props: { value: { default: 72 }, size: { default: 140 }, stroke: { default: 14 }, color: { default: "var(--vy-orange)" }, track: { default: "var(--vy-cream)" } },
  computed: {
    r() { return (this.size - this.stroke) / 2; },
    c() { return 2 * Math.PI * this.r; },
    off() { return this.c - (this.value / 100) * this.c; }
  },
  template: `<svg :width="size" :height="size" :viewBox="'0 0 ' + size + ' ' + size">
    <circle :cx="size/2" :cy="size/2" :r="r" fill="none" :stroke="track" :stroke-width="stroke"/>
    <circle :cx="size/2" :cy="size/2" :r="r" fill="none" :stroke="color" :stroke-width="stroke" stroke-linecap="round" :stroke-dasharray="c" :stroke-dashoffset="off" :transform="'rotate(-90 ' + size/2 + ' ' + size/2 + ')'"/>
    <text x="50%" y="50%" text-anchor="middle" dominant-baseline="middle" font-family="var(--font-display)" font-weight="800" :font-size="size*0.22" fill="var(--vy-ink)" letter-spacing="-0.04em">{{ value }}%</text>
  </svg>`
});

export const VyBarChart = defineComponent({
  props: { data: Array, width: { default: 360 }, height: { default: 160 }, accent: { default: "var(--vy-orange)" } },
  computed: {
    bars() {
      const { data, width, height, accent } = this;
      const max = Math.max(...data.map(d => d.v));
      const bw = (width - 40) / data.length - 10;
      return data.map((d, i) => {
        const h = (d.v / max) * (height - 16);
        const x = 24 + i * (bw + 10);
        const y = height - h;
        return { ...d, x, y, h, bw, isMax: d.v === max };
      });
    }
  },
  template: `<svg :width="width" :height="height + 24" :viewBox="'0 0 ' + width + ' ' + (height + 24)">
    <line v-for="p in [0.25, 0.5, 0.75, 1]" :key="p" x1="20" :x2="width - 8" :y1="height - height * p" :y2="height - height * p" stroke="var(--vy-line)" stroke-dasharray="2 4"/>
    <g v-for="b in bars" :key="b.l">
      <rect :x="b.x" :y="b.y" :width="b.bw" :height="b.h" rx="6" :fill="b.isMax ? accent : 'var(--vy-beige)'" :opacity="b.isMax ? 1 : 0.85"/>
      <text :x="b.x + b.bw/2" :y="height + 16" font-size="10" text-anchor="middle" fill="var(--vy-ink-3)" font-family="var(--font-sans)" font-weight="600">{{ b.l }}</text>
    </g>
  </svg>`
});

// ============ PRODUCT IMAGE ============
export const VyProductImage = defineComponent({
  props: { grad: String, label: String, h: { default: 220 }, big: Boolean },
  computed: {
    isImage() {
      return this.grad && (this.grad.startsWith("/uploads/") || this.grad.startsWith("http") || this.grad.startsWith("blob:"));
    }
  },
  template: `<div :style="{height:h+'px',borderRadius:'14px',background:isImage ? 'var(--vy-surface-2)' : grad,position:'relative',overflow:'hidden',display:'flex',alignItems:'flex-end',padding:'14px'}">
    <img v-if="isImage" :src="grad" alt="" style="position:absolute;inset:0;width:100%;height:100%;object-fit:cover" />
    <div v-if="!isImage" style="position:absolute;inset:0;background:radial-gradient(circle at 30% 20%, rgba(255,255,255,.5), transparent 60%)"></div>
    <svg v-if="!isImage" viewBox="0 0 120 180" :style="{position:'absolute',left:'50%',top:'50%',transform:'translate(-50%, -52%)',width:(big ? 140 : 90)+'px',height:(big ? 210 : 135)+'px',filter:'drop-shadow(0 12px 18px rgba(31,26,20,.18))'}">
      <rect x="48" y="6" width="24" height="22" rx="3" fill="rgba(31,26,20,.85)"/>
      <rect x="42" y="26" width="36" height="8" rx="2" fill="rgba(31,26,20,.7)"/>
      <path d="M30 50 Q30 38 50 36 L70 36 Q90 38 90 50 L90 160 Q90 174 76 174 L44 174 Q30 174 30 160 Z" fill="rgba(255,255,255,.9)"/>
      <rect x="38" y="78" width="44" height="42" rx="4" fill="rgba(31,26,20,.06)"/>
      <text x="60" y="100" text-anchor="middle" font-family="var(--font-display)" font-size="13" font-weight="800" fill="var(--vy-orange-deep)">VY</text>
      <text x="60" y="114" text-anchor="middle" font-family="var(--font-sans)" font-size="6" font-weight="600" fill="var(--vy-ink-2)" letter-spacing="0.15em">VIDAYOUNG</text>
    </svg>
    <div v-if="label" style="position:relative;font-family:var(--font-mono);font-size:10px;color:rgba(31,26,20,.55);letter-spacing:0.1em;text-transform:uppercase">{{ label }}</div>
  </div>`
});

// ============ APP SHELL (sidebar + topbar para pantallas embajador) ============
import { USER, SCREENS } from "../data.js";

export const AppShell = defineComponent({
  components: { VyLogo, VyAvatar, VyIcon },
  props: {
    active: String,
    search: { default: "Buscar productos, miembros, transacciones…" }
  },
  emits: ["navigate"],
  data() {
    return {
      USER,
      items: [
        { id: "dashboard", l: "Dashboard",    icon: "home"    },
        { id: "wallet",    l: "Finanzas",     icon: "wallet"  },
        { id: "shop",      l: "Tienda",       icon: "shop"    },
        { id: "network",   l: "Mi red",       icon: "network", badge: "138" },
        { id: "rewards",   l: "Recompensas",  icon: "gift",    badge: "3" },
        { id: "stats",     l: "Estadísticas", icon: "bars" },
        { id: "profile",   l: "Perfil",       icon: "user" },
      ]
    };
  },
  template: `<div class="vy" style="width:100%;min-height:900px;display:grid;grid-template-columns:248px 1fr;background:var(--vy-bg)">
    <aside style="background:var(--vy-surface);border-right:1px solid var(--vy-line);padding:24px 18px;display:flex;flex-direction:column;gap:22px;position:sticky;top:0;height:100vh">
      <div style="padding:4px 8px"><vy-logo :size="22"/></div>

      <button style="display:flex;align-items:center;gap:12px;padding:10px 12px;background:var(--vy-surface-2);border-radius:12px;border:1px solid var(--vy-line)">
        <vy-avatar :name="USER.avatar" :size="32" bg="var(--vy-orange)" color="#fff"/>
        <div style="flex:1;text-align:left;min-width:0">
          <div style="font-size:12px;color:var(--vy-ink-3);font-weight:600">Mi cuenta</div>
          <div style="font-size:13px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">{{ USER.name }}</div>
        </div>
        <vy-icon name="chevD" :size="14" stroke="var(--vy-ink-3)"/>
      </button>

      <div>
        <div style="font-size:10px;color:var(--vy-ink-3);letter-spacing:0.12em;text-transform:uppercase;padding:4px 12px;margin-bottom:6px;font-weight:700">General</div>
        <nav style="display:flex;flex-direction:column;gap:2px">
          <button v-for="it in items" :key="it.id"
                  @click="$emit('navigate', it.id)"
                  :style="{display:'flex',alignItems:'center',gap:'12px',padding:'10px 12px',borderRadius:'10px',fontSize:'13.5px',fontWeight: it.id === active ? 700 : 600, background: it.id === active ? 'var(--vy-ink)' : 'transparent', color: it.id === active ? '#fff' : 'var(--vy-ink-2)'}">
            <span :style="{color: it.id === active ? 'var(--vy-orange)' : 'var(--vy-ink-3)'}">
              <vy-icon :name="it.icon" :size="18"/>
            </span>
            <span style="flex:1;text-align:left">{{ it.l }}</span>
            <span v-if="it.badge"
                  :style="{padding:'1px 8px',borderRadius:'99px',fontSize:'11px',fontWeight:700,background: it.id === active ? 'var(--vy-orange)' : 'var(--vy-cream)', color: it.id === active ? '#fff' : '#6B4A12'}">{{ it.badge }}</span>
          </button>
        </nav>
      </div>

      <div style="margin-top:auto;padding:16px;background:var(--vy-cream);border-radius:16px;position:relative;overflow:hidden">
        <div style="position:absolute;right:-20px;top:-20px;width:80px;height:80px;border-radius:50%;background:rgba(242,135,5,.25)"></div>
        <div style="position:relative">
          <div class="vy-chip vy-chip-orange" style="margin-bottom:10px">Próximo nivel</div>
          <div style="font-weight:800;font-size:14px;line-height:1.25">Te faltan <span style="color:var(--vy-orange-deep)">$ 1,2M</span> para Diamante Élite</div>
          <div style="height:6px;border-radius:99px;background:rgba(31,26,20,.1);margin-top:10px">
            <div style="height:100%;width:68%;border-radius:99px;background:var(--vy-orange)"></div>
          </div>
          <div style="font-size:11px;color:var(--vy-ink-2);margin-top:6px;font-weight:600">68% completado</div>
        </div>
      </div>
    </aside>

    <div style="display:flex;flex-direction:column">
      <header style="display:flex;align-items:center;gap:16px;padding:18px 32px;border-bottom:1px solid var(--vy-line-2);background:rgba(251,248,240,.85);backdrop-filter:blur(8px);position:sticky;top:0;z-index:10">
        <div style="flex:1;max-width:460px;display:flex;align-items:center;gap:10px;padding:10px 14px;background:var(--vy-surface);border-radius:12px;border:1px solid var(--vy-line)">
          <vy-icon name="search" :size="16" stroke="var(--vy-ink-3)"/>
          <span style="flex:1;font-size:13px;color:var(--vy-ink-3)">{{ search }}</span>
          <span style="font-size:11px;font-family:var(--font-mono);color:var(--vy-ink-3);padding:2px 6px;border:1px solid var(--vy-line);border-radius:5px">⌘ K</span>
        </div>
        <div style="flex:1"></div>
        <button class="vy-btn vy-btn-ghost" style="padding:10px 14px"><vy-icon name="cog" :size="16"/> Ajustes</button>
        <button style="position:relative;width:40px;height:40px;border-radius:12px;background:var(--vy-surface);border:1px solid var(--vy-line);display:flex;align-items:center;justify-content:center">
          <vy-icon name="bell" :size="16"/>
          <span style="position:absolute;top:8px;right:8px;width:8px;height:8px;background:var(--vy-orange);border-radius:50%;border:2px solid var(--vy-surface)"></span>
        </button>
        <vy-avatar :name="USER.avatar" :size="40" bg="var(--vy-orange)" color="#fff"/>
      </header>

      <main style="padding:28px 32px 40px;flex:1">
        <slot/>
      </main>
    </div>
  </div>`
});
