// ============ DATOS MOCK Vidayoung ============
export const USER = {
  name: "Carolina Mendoza", short: "Carolina",
  email: "carolina.m@vidayoung.com", avatar: "CM",
  level: "Líder Diamante", levelTier: 4, totalLevels: 6,
  joined: "Mar 2024", code: "CM-VY-8421", city: "Bogotá, CO"
};

export const KPIS = [
  { label: "Saldo disponible",  value: "$ 4.281.500", delta: "+12,4%", up: true,  hint: "vs. mes anterior" },
  { label: "Ganancias del mes", value: "$ 1.652.300", delta: "+24,8%", up: true,  hint: "Comisiones + bonos" },
  { label: "Bonificaciones",    value: "$ 480.200",   delta: "+8,1%",  up: true,  hint: "3 bonos activos" },
  { label: "Red activa",        value: "138",         delta: "+12",    up: true,  hint: "miembros activos" },
];

export const PRODUCTS = [
  { id: 1, name: "Sérum Vitalidad C+", cat: "Cuidado facial",  price: 168000, old: 195000, rating: 4.9, badge: "Más vendido", img: "linear-gradient(135deg, #F2B885 0%, #F28705 100%)" },
  { id: 2, name: "Colágeno Hidrolizado", cat: "Suplementos",   price: 240000, old: null,   rating: 4.8, badge: "Nuevo",       img: "linear-gradient(135deg, #F2E7C4 0%, #E9C879 100%)" },
  { id: 3, name: "Aceite Esencial Bienestar", cat: "Aromaterapia", price: 92000, old: null, rating: 4.7, badge: null,           img: "linear-gradient(135deg, #FFE4C2 0%, #F2B885 100%)" },
  { id: 4, name: "Té Detox Botánico", cat: "Nutrición",        price: 78000,  old: 95000,  rating: 4.6, badge: "Oferta",      img: "linear-gradient(135deg, #DDE9C9 0%, #A8C682 100%)" },
  { id: 5, name: "Crema Renovadora Noche", cat: "Cuidado facial", price: 145000, old: null, rating: 4.9, badge: null,         img: "linear-gradient(135deg, #FBF0D9 0%, #E8C886 100%)" },
  { id: 6, name: "Multivitamínico Mujer", cat: "Suplementos",   price: 132000, old: null,  rating: 4.8, badge: null,          img: "linear-gradient(135deg, #F8D9B8 0%, #F28705 100%)" },
  { id: 7, name: "Mascarilla Arcilla Roja", cat: "Cuidado facial", price: 64000, old: 85000, rating: 4.5, badge: "Oferta",   img: "linear-gradient(135deg, #F2C7B5 0%, #C4452A 100%)" },
  { id: 8, name: "Probiótico Equilibrium", cat: "Suplementos",   price: 215000, old: null,  rating: 5.0, badge: "Premium",    img: "linear-gradient(135deg, #F2E7C4 0%, #F28705 100%)" },
];

export const TRANSACTIONS = [
  { id: "TX-2089", type: "Comisión nivel 2", amt: 184500,  date: "9 May 2026", status: "Acreditado" },
  { id: "TX-2088", type: "Bono liderazgo",   amt: 320000,  date: "8 May 2026", status: "Acreditado" },
  { id: "TX-2087", type: "Venta directa",    amt: 168000,  date: "7 May 2026", status: "Acreditado" },
  { id: "TX-2086", type: "Retiro a banco",   amt: -750000, date: "5 May 2026", status: "Procesado"  },
  { id: "TX-2085", type: "Comisión nivel 1", amt: 96400,   date: "4 May 2026", status: "Acreditado" },
  { id: "TX-2084", type: "Bono inicio rápido", amt: 150000, date: "3 May 2026", status: "Acreditado" },
];

export const NETWORK_ROOT = {
  id: "u0", name: "Carolina M.", role: "Líder Diamante", sales: "$ 12,4M", root: true,
  children: [
    { id: "u1", name: "Andrés P.",  role: "Líder Oro",     sales: "$ 4,2M", children: [
      { id: "u11", name: "Sofía L.",    role: "Constructor", sales: "$ 1,1M" },
      { id: "u12", name: "Mariana R.",  role: "Constructor", sales: "$ 980K" },
      { id: "u13", name: "Diego H.",    role: "Iniciador",   sales: "$ 420K" },
    ]},
    { id: "u2", name: "Valentina T.", role: "Líder Plata",  sales: "$ 3,1M", children: [
      { id: "u21", name: "Luis F.",     role: "Constructor", sales: "$ 870K" },
      { id: "u22", name: "Camila S.",   role: "Constructor", sales: "$ 760K" },
    ]},
    { id: "u3", name: "Ricardo V.",  role: "Líder Plata",  sales: "$ 2,8M", children: [
      { id: "u31", name: "Paula G.",    role: "Iniciador",   sales: "$ 510K" },
      { id: "u32", name: "Tomás A.",    role: "Iniciador",   sales: "$ 380K" },
      { id: "u33", name: "Elena M.",    role: "Constructor", sales: "$ 690K" },
    ]},
  ]
};

export const SCREENS = [
  { id: "landing",   label: "Landing corporativa", icon: "home",     group: "Público",      public: true },
  { id: "login",     label: "Login",                icon: "key",      group: "Público",      public: true, fullbleed: true },
  { id: "dashboard", label: "Dashboard",            icon: "home",     group: "Embajador" },
  { id: "personas",  label: "Personas",             icon: "user",     group: "Operación" },
  { id: "wallet",    label: "Finanzas",             icon: "wallet",   group: "Embajador" },
  { id: "shop",      label: "Tienda",               icon: "shop",     group: "Comercio" },
  { id: "cart",      label: "Carrito",              icon: "cart",     group: "Comercio" },
  { id: "network",   label: "Red multinivel",       icon: "network",  group: "Embajador", badge: "138" },
  { id: "rewards",   label: "Recompensas",          icon: "gift",     group: "Embajador", badge: "3" },
  { id: "stats",     label: "Estadísticas",         icon: "bars",     group: "Embajador" },
  { id: "profile",   label: "Perfil",               icon: "user",     group: "Embajador" },
  { id: "admin",     label: "Panel administrativo", icon: "shield",   group: "Operación", fullbleed: true },
];
