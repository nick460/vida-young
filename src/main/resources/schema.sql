CREATE TABLE IF NOT EXISTS personas (
    id BIGSERIAL PRIMARY KEY,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    documento VARCHAR(30) UNIQUE,
    email VARCHAR(120),
    telefono VARCHAR(30)
);

ALTER TABLE personas ALTER COLUMN documento DROP NOT NULL;

CREATE TABLE IF NOT EXISTS roles (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    persona_id BIGINT NOT NULL UNIQUE REFERENCES personas(id)
);

ALTER TABLE personas
    ADD COLUMN IF NOT EXISTS estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN IF NOT EXISTS fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM';

ALTER TABLE usuarios
    ADD COLUMN IF NOT EXISTS estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN IF NOT EXISTS fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS foto_perfil VARCHAR(255);

ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN IF NOT EXISTS fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM';

CREATE TABLE IF NOT EXISTS menus_sistema (
    id BIGSERIAL PRIMARY KEY,
    menu_id VARCHAR(80) NOT NULL UNIQUE,
    label VARCHAR(120) NOT NULL,
    icon VARCHAR(80) NOT NULL,
    custom BOOLEAN NOT NULL DEFAULT FALSE,
    orden INTEGER NOT NULL DEFAULT 0,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

ALTER TABLE menus_sistema
    ADD COLUMN IF NOT EXISTS estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN IF NOT EXISTS fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM';

ALTER TABLE menus_sistema
    ALTER COLUMN estado SET DEFAULT 'ACTIVO',
    ALTER COLUMN fecha_registro SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN fecha_modificacion SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN usuario_registro SET DEFAULT 'SYSTEM',
    ALTER COLUMN usuario_modificacion SET DEFAULT 'SYSTEM';

UPDATE menus_sistema
SET estado = COALESCE(estado, 'ACTIVO'),
    fecha_registro = COALESCE(fecha_registro, CURRENT_TIMESTAMP),
    fecha_modificacion = COALESCE(fecha_modificacion, CURRENT_TIMESTAMP),
    usuario_registro = COALESCE(usuario_registro, 'SYSTEM'),
    usuario_modificacion = COALESCE(usuario_modificacion, 'SYSTEM');

CREATE TABLE IF NOT EXISTS roles_menus (
    rol_id BIGINT NOT NULL REFERENCES roles(id),
    menu_id BIGINT NOT NULL REFERENCES menus_sistema(id),
    PRIMARY KEY (rol_id, menu_id)
);

INSERT INTO menus_sistema (menu_id, label, icon, custom, orden)
VALUES
    ('dashboard', 'Dashboard', 'Home', FALSE, 10),
    ('roles-menus', 'Roles y menus', 'Shield', FALSE, 20),
    ('pagina-principal-config', 'Pagina principal', 'PanelTop', FALSE, 30),
    ('personas', 'Personas', 'User', FALSE, 40),
    ('rangos', 'Rangos', 'Trophy', FALSE, 50),
    ('planes', 'Planes', 'BadgePercent', FALSE, 60),
    ('planes-activacion', 'Activaciones', 'Activity', FALSE, 70),
    ('referidos', 'Referidos', 'Network', FALSE, 80),
    ('inventario', 'Inventario', 'PackageSearch', FALSE, 90),
    ('ventanilla', 'Ventanilla', 'Store', FALSE, 100),
    ('registro-referido', 'Registro referido', 'UserPlus', FALSE, 110),
    ('herramientas-digitales', 'Herramientas digitales', 'Wrench', FALSE, 120),
    ('wallet', 'Finanzas', 'Wallet', FALSE, 130),
    ('shop', 'Tienda', 'ShoppingBag', FALSE, 140),
    ('network', 'Mi red', 'Users', FALSE, 150),
    ('rewards', 'Recompensas', 'Gift', FALSE, 160),
    ('stats', 'Estadisticas', 'BarChart3', FALSE, 170),
    ('profile', 'Perfil', 'User', FALSE, 180)
ON CONFLICT (menu_id) DO UPDATE
SET label = EXCLUDED.label,
    icon = EXCLUDED.icon,
    custom = FALSE,
    orden = EXCLUDED.orden;

INSERT INTO roles_menus (rol_id, menu_id)
SELECT r.id, m.id
FROM roles r
CROSS JOIN menus_sistema m
WHERE r.nombre = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO roles_menus (rol_id, menu_id)
SELECT r.id, m.id
FROM roles r
JOIN menus_sistema m ON m.menu_id IN ('dashboard', 'wallet', 'shop', 'network', 'profile', 'rewards', 'herramientas-digitales')
WHERE r.nombre = 'EMBAJADOR'
ON CONFLICT DO NOTHING;

INSERT INTO roles_menus (rol_id, menu_id)
SELECT r.id, m.id
FROM roles r
JOIN menus_sistema m ON m.menu_id IN ('dashboard', 'wallet', 'shop', 'profile', 'herramientas-digitales')
WHERE r.nombre = 'USUARIO'
ON CONFLICT DO NOTHING;

INSERT INTO roles_menus (rol_id, menu_id)
SELECT r.id, m.id
FROM roles r
JOIN menus_sistema m ON m.menu_id IN ('dashboard', 'shop', 'profile')
WHERE r.nombre = 'CLIENTE'
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS productos (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(140) NOT NULL,
    descripcion VARCHAR(500),
    categoria VARCHAR(80),
    precio NUMERIC(12, 2) NOT NULL,
    pv NUMERIC(12, 2) NOT NULL DEFAULT 0,
    qp NUMERIC(12, 2) NOT NULL DEFAULT 0,
    cr NUMERIC(12, 2) NOT NULL DEFAULT 0,
    imagen_url VARCHAR(255),
    listar_en_shop BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS pv NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS qp NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cr NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS listar_en_shop BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS productos_categorias (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    sigla VARCHAR(12) NOT NULL UNIQUE,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

ALTER TABLE productos_categorias
    ADD COLUMN IF NOT EXISTS estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN IF NOT EXISTS fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM';

ALTER TABLE productos_categorias
    ALTER COLUMN estado SET DEFAULT 'ACTIVO',
    ALTER COLUMN fecha_registro SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN fecha_modificacion SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN usuario_registro SET DEFAULT 'SYSTEM',
    ALTER COLUMN usuario_modificacion SET DEFAULT 'SYSTEM';

UPDATE productos_categorias
SET estado = COALESCE(estado, 'ACTIVO'),
    fecha_registro = COALESCE(fecha_registro, CURRENT_TIMESTAMP),
    fecha_modificacion = COALESCE(fecha_modificacion, CURRENT_TIMESTAMP),
    usuario_registro = COALESCE(usuario_registro, 'SYSTEM'),
    usuario_modificacion = COALESCE(usuario_modificacion, 'SYSTEM');

INSERT INTO productos_categorias (nombre, sigla)
SELECT DISTINCT
    p.categoria,
    LOWER(SUBSTRING(REGEXP_REPLACE(p.categoria, '[^A-Za-z0-9]', '', 'g') FROM 1 FOR 3))
FROM productos p
WHERE p.categoria IS NOT NULL
  AND TRIM(p.categoria) <> ''
  AND LENGTH(REGEXP_REPLACE(p.categoria, '[^A-Za-z0-9]', '', 'g')) >= 2
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS productos_landings (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL UNIQUE REFERENCES productos(id),
    headline VARCHAR(220),
    subtitle TEXT,
    story TEXT,
    usage TEXT,
    ingredients TEXT,
    benefits TEXT,
    gallery TEXT,
    sections TEXT,
    share_message TEXT,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

ALTER TABLE productos_landings
    ADD COLUMN IF NOT EXISTS sections TEXT;

ALTER TABLE productos_landings
    ADD COLUMN IF NOT EXISTS share_message TEXT;

CREATE TABLE IF NOT EXISTS digital_landings (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(160) NOT NULL,
    category VARCHAR(120),
    image_url VARCHAR(255),
    description TEXT,
    sections TEXT,
    share_message TEXT,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

ALTER TABLE digital_landings
    ADD COLUMN IF NOT EXISTS category VARCHAR(120),
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS description TEXT,
    ADD COLUMN IF NOT EXISTS sections TEXT,
    ADD COLUMN IF NOT EXISTS share_message TEXT;

ALTER TABLE digital_landings
    ALTER COLUMN estado SET DEFAULT 'ACTIVO',
    ALTER COLUMN fecha_registro SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN fecha_modificacion SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN usuario_registro SET DEFAULT 'SYSTEM',
    ALTER COLUMN usuario_modificacion SET DEFAULT 'SYSTEM';

INSERT INTO digital_landings (slug, title, category, description, share_message)
SELECT
    'pagina-principal',
    'Vidayoung',
    'Empresa',
    'Bienestar, comunidad y productos funcionales para acompanar tu crecimiento diario.',
    ''
WHERE NOT EXISTS (SELECT 1 FROM digital_landings WHERE slug = 'pagina-principal');

INSERT INTO digital_landings (slug, title, category, description, share_message)
SELECT
    'educacion-financiera',
    'Educacion financiera',
    'Formacion',
    'Una landing editable para compartir conceptos de orden financiero, habitos y vision de crecimiento.',
    'Te comparto esta guia de {tema}. Entra aqui:'
WHERE NOT EXISTS (SELECT 1 FROM digital_landings WHERE slug = 'educacion-financiera');

INSERT INTO digital_landings (slug, title, category, description, share_message)
SELECT
    'claves-del-negocio',
    'Las claves del negocio',
    'Negocio',
    'Una landing editable para explicar la oportunidad, el sistema y los primeros pasos.',
    'Te comparto esta informacion sobre {tema}. Entra aqui:'
WHERE NOT EXISTS (SELECT 1 FROM digital_landings WHERE slug = 'claves-del-negocio');

CREATE TABLE IF NOT EXISTS planes (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(500),
    precio NUMERIC(12, 2) NOT NULL,
    qp NUMERIC(12, 2) NOT NULL DEFAULT 0,
    imagen_url VARCHAR(255),
    niveles_alcance INTEGER NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

CREATE TABLE IF NOT EXISTS rangos (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    qp_minimo NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

ALTER TABLE personas
    ADD COLUMN IF NOT EXISTS rango_actual_id BIGINT REFERENCES rangos(id);

ALTER TABLE planes
    ADD COLUMN IF NOT EXISTS imagen_url VARCHAR(255);

ALTER TABLE planes
    ADD COLUMN IF NOT EXISTS qp NUMERIC(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE planes
    ADD COLUMN IF NOT EXISTS bonificacion_directa NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS valor_productos_beneficio NUMERIC(12, 2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS planes_niveles (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES planes(id),
    numero_nivel INTEGER NOT NULL,
    porcentaje_comision NUMERIC(12, 2) NOT NULL,
    valor_productos_beneficio NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM',
    CONSTRAINT uk_planes_niveles_plan_nivel UNIQUE (plan_id, numero_nivel)
);

ALTER TABLE planes_niveles
    ALTER COLUMN porcentaje_comision TYPE NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS valor_productos_beneficio NUMERIC(12, 2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS planes_productos (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES planes(id),
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    cantidad INTEGER NOT NULL DEFAULT 1,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM',
    CONSTRAINT uk_planes_productos_plan_producto UNIQUE (plan_id, producto_id)
);

CREATE TABLE IF NOT EXISTS referidos (
    id BIGSERIAL PRIMARY KEY,
    persona_id BIGINT NOT NULL REFERENCES personas(id),
    patrocinador_id BIGINT REFERENCES personas(id),
    plan_id BIGINT NOT NULL REFERENCES planes(id),
    fecha_union TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM',
    CONSTRAINT uk_referidos_persona UNIQUE (persona_id),
    CONSTRAINT chk_referidos_persona_patrocinador CHECK (persona_id <> patrocinador_id)
);

ALTER TABLE referidos
    ALTER COLUMN patrocinador_id DROP NOT NULL;

ALTER TABLE referidos
    ADD COLUMN IF NOT EXISTS fecha_inicio_membresia TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS fecha_fin_membresia TIMESTAMP NOT NULL DEFAULT (date_trunc('day', CURRENT_TIMESTAMP + INTERVAL '1 month') + INTERVAL '23 hours 59 minutes 59 seconds'),
    ADD COLUMN IF NOT EXISTS membresia_activa BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE IF NOT EXISTS recompensas (
    id BIGSERIAL PRIMARY KEY,
    referido_id BIGINT NOT NULL REFERENCES referidos(id),
    beneficiario_id BIGINT NOT NULL REFERENCES personas(id),
    plan_ingreso_id BIGINT NOT NULL REFERENCES planes(id),
    nivel_generado INTEGER NOT NULL,
    monto_efectivo NUMERIC(12, 2) NOT NULL DEFAULT 0,
    valor_productos NUMERIC(12, 2) NOT NULL DEFAULT 0,
    cobrable BOOLEAN NOT NULL DEFAULT TRUE,
    motivo_no_cobrable VARCHAR(180),
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

ALTER TABLE recompensas
    ADD COLUMN IF NOT EXISTS cobrable BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS motivo_no_cobrable VARCHAR(180);

CREATE TABLE IF NOT EXISTS preinscripciones_referidos (
    id BIGSERIAL PRIMARY KEY,
    patrocinador_id BIGINT NOT NULL REFERENCES personas(id),
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    documento VARCHAR(30) NOT NULL,
    telefono VARCHAR(30) NOT NULL,
    email VARCHAR(120),
    estado_preinscripcion VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    plan_id BIGINT REFERENCES planes(id),
    persona_id BIGINT REFERENCES personas(id),
    referido_id BIGINT REFERENCES referidos(id),
    fecha_validacion TIMESTAMP,
    usuario_validacion VARCHAR(80),
    observacion VARCHAR(300),
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

CREATE INDEX IF NOT EXISTS idx_preinscripciones_referidos_estado
    ON preinscripciones_referidos (estado_preinscripcion);

CREATE INDEX IF NOT EXISTS idx_preinscripciones_referidos_documento
    ON preinscripciones_referidos (documento);

CREATE TABLE IF NOT EXISTS billeteras (
    id BIGSERIAL PRIMARY KEY,
    persona_id BIGINT NOT NULL REFERENCES personas(id),
    saldo_dinero NUMERIC(12, 2) NOT NULL DEFAULT 0,
    saldo_pv NUMERIC(12, 2) NOT NULL DEFAULT 0,
    saldo_qp NUMERIC(12, 2) NOT NULL DEFAULT 0,
    saldo_cr NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM',
    CONSTRAINT uk_billeteras_persona UNIQUE (persona_id)
);

CREATE TABLE IF NOT EXISTS movimientos_billetera (
    id BIGSERIAL PRIMARY KEY,
    billetera_id BIGINT NOT NULL REFERENCES billeteras(id),
    tipo VARCHAR(20) NOT NULL,
    concepto VARCHAR(160) NOT NULL,
    referencia_tipo VARCHAR(60),
    referencia_id BIGINT,
    monto NUMERIC(12, 2) NOT NULL,
    saldo_resultado NUMERIC(12, 2) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

CREATE TABLE IF NOT EXISTS historial_membresias (
    id BIGSERIAL PRIMARY KEY,
    persona_id BIGINT NOT NULL REFERENCES personas(id),
    plan_id BIGINT NOT NULL REFERENCES planes(id),
    tipo VARCHAR(30) NOT NULL,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    precio_plan NUMERIC(12, 2) NOT NULL DEFAULT 0,
    qp_plan NUMERIC(12, 2) NOT NULL DEFAULT 0,
    referencia_tipo VARCHAR(60),
    referencia_id BIGINT,
    estado_membresia VARCHAR(30) NOT NULL DEFAULT 'ACTIVA',
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

CREATE TABLE IF NOT EXISTS cierres_mensuales_billetera (
    id BIGSERIAL PRIMARY KEY,
    persona_id BIGINT NOT NULL REFERENCES personas(id),
    periodo VARCHAR(7) NOT NULL,
    saldo_dinero NUMERIC(12, 2) NOT NULL DEFAULT 0,
    saldo_pv NUMERIC(12, 2) NOT NULL DEFAULT 0,
    saldo_qp NUMERIC(12, 2) NOT NULL DEFAULT 0,
    saldo_cr NUMERIC(12, 2) NOT NULL DEFAULT 0,
    rango_id BIGINT REFERENCES rangos(id),
    rango_nombre VARCHAR(100),
    rango_qp_minimo NUMERIC(12, 2),
    estado_planilla VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE_PLANILLA',
    fecha_cierre TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM',
    CONSTRAINT uk_cierres_mensuales_persona_periodo UNIQUE (persona_id, periodo)
);

ALTER TABLE cierres_mensuales_billetera
    ADD COLUMN IF NOT EXISTS saldo_cr NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rango_id BIGINT REFERENCES rangos(id),
    ADD COLUMN IF NOT EXISTS rango_nombre VARCHAR(100),
    ADD COLUMN IF NOT EXISTS rango_qp_minimo NUMERIC(12, 2);

CREATE UNIQUE INDEX IF NOT EXISTS uk_historial_membresias_referencia
    ON historial_membresias (referencia_tipo, referencia_id, tipo)
    WHERE referencia_tipo IS NOT NULL AND referencia_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_movimientos_billetera_referencia
    ON movimientos_billetera (referencia_tipo, referencia_id, tipo)
    WHERE referencia_tipo IS NOT NULL AND referencia_id IS NOT NULL;

ALTER TABLE billeteras
    ADD COLUMN IF NOT EXISTS saldo_cr NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ALTER COLUMN estado SET DEFAULT 'ACTIVO',
    ALTER COLUMN fecha_registro SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN fecha_modificacion SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN usuario_registro SET DEFAULT 'SYSTEM',
    ALTER COLUMN usuario_modificacion SET DEFAULT 'SYSTEM';

ALTER TABLE historial_membresias
    ALTER COLUMN estado SET DEFAULT 'ACTIVO',
    ALTER COLUMN fecha_registro SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN fecha_modificacion SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN usuario_registro SET DEFAULT 'SYSTEM',
    ALTER COLUMN usuario_modificacion SET DEFAULT 'SYSTEM';

ALTER TABLE movimientos_billetera
    ALTER COLUMN estado SET DEFAULT 'ACTIVO',
    ALTER COLUMN fecha_registro SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN fecha_modificacion SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN usuario_registro SET DEFAULT 'SYSTEM',
    ALTER COLUMN usuario_modificacion SET DEFAULT 'SYSTEM';

INSERT INTO billeteras (persona_id, saldo_dinero, saldo_pv, saldo_qp, saldo_cr)
SELECT p.id, 0, 0, 0, 0
FROM personas p
WHERE NOT EXISTS (
    SELECT 1 FROM billeteras b WHERE b.persona_id = p.id
);

INSERT INTO historial_membresias (
    persona_id,
    plan_id,
    tipo,
    fecha_inicio,
    fecha_fin,
    precio_plan,
    qp_plan,
    referencia_tipo,
    referencia_id,
    estado_membresia
)
SELECT
    r.persona_id,
    r.plan_id,
    'AFILIACION',
    r.fecha_inicio_membresia,
    r.fecha_fin_membresia,
    COALESCE(p.precio, 0),
    COALESCE(p.qp, 0),
    'REFERIDO_AFILIACION',
    r.id,
    CASE WHEN r.membresia_activa THEN 'ACTIVA' ELSE 'VENCIDA' END
FROM referidos r
JOIN planes p ON p.id = r.plan_id
WHERE NOT EXISTS (
    SELECT 1
    FROM historial_membresias hm
    WHERE hm.referencia_tipo = 'REFERIDO_AFILIACION'
      AND hm.referencia_id = r.id
      AND hm.tipo = 'AFILIACION'
);

INSERT INTO movimientos_billetera (
    billetera_id,
    tipo,
    concepto,
    referencia_tipo,
    referencia_id,
    monto,
    saldo_resultado
)
SELECT
    b.id,
    'QP',
    'QP por afiliar al plan ' || p.nombre,
    'REFERIDO_AFILIACION',
    r.id,
    COALESCE(p.qp, 0),
    COALESCE(p.qp, 0)
FROM referidos r
JOIN planes p ON p.id = r.plan_id
JOIN billeteras b ON b.persona_id = r.patrocinador_id
WHERE COALESCE(p.qp, 0) > 0
  AND r.patrocinador_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM movimientos_billetera mb
    WHERE mb.referencia_tipo = 'REFERIDO_AFILIACION'
      AND mb.referencia_id = r.id
      AND mb.tipo = 'QP'
);

UPDATE billeteras b
SET saldo_qp = COALESCE((
    SELECT SUM(mb.monto)
    FROM movimientos_billetera mb
    WHERE mb.billetera_id = b.id
      AND mb.tipo = 'QP'
      AND mb.estado = 'ACTIVO'
), 0);

UPDATE billeteras b
SET saldo_cr = COALESCE((
    SELECT SUM(mb.monto)
    FROM movimientos_billetera mb
    WHERE mb.billetera_id = b.id
      AND mb.tipo = 'CR'
      AND mb.estado = 'ACTIVO'
), 0);

CREATE TABLE IF NOT EXISTS planes_activacion (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    descripcion VARCHAR(500),
    pv_minimo_mensual NUMERIC(12, 2) NOT NULL DEFAULT 0,
    niveles_alcance INTEGER NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

CREATE TABLE IF NOT EXISTS planes_activacion_niveles (
    id BIGSERIAL PRIMARY KEY,
    plan_activacion_id BIGINT NOT NULL REFERENCES planes_activacion(id),
    numero_nivel INTEGER NOT NULL,
    monto_por_producto NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM',
    CONSTRAINT uk_planes_activacion_nivel UNIQUE (plan_activacion_id, numero_nivel)
);

ALTER TABLE planes_activacion
    ALTER COLUMN estado SET DEFAULT 'ACTIVO',
    ALTER COLUMN fecha_registro SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN fecha_modificacion SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN usuario_registro SET DEFAULT 'SYSTEM',
    ALTER COLUMN usuario_modificacion SET DEFAULT 'SYSTEM';

ALTER TABLE planes_activacion_niveles
    ALTER COLUMN estado SET DEFAULT 'ACTIVO',
    ALTER COLUMN fecha_registro SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN fecha_modificacion SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN usuario_registro SET DEFAULT 'SYSTEM',
    ALTER COLUMN usuario_modificacion SET DEFAULT 'SYSTEM';

INSERT INTO planes_activacion (nombre, descripcion, pv_minimo_mensual, niveles_alcance)
SELECT 'Activacion Estandar', 'Activa beneficios por compras de producto hasta 3 niveles.', 200, 3
WHERE NOT EXISTS (SELECT 1 FROM planes_activacion WHERE nombre = 'Activacion Estandar');

INSERT INTO planes_activacion (nombre, descripcion, pv_minimo_mensual, niveles_alcance)
SELECT 'Activacion Ultra', 'Activa beneficios superiores por compras de producto hasta 3 niveles.', 400, 3
WHERE NOT EXISTS (SELECT 1 FROM planes_activacion WHERE nombre = 'Activacion Ultra');

INSERT INTO planes_activacion_niveles (plan_activacion_id, numero_nivel, monto_por_producto)
SELECT pa.id, nivel.numero_nivel, 6.50
FROM planes_activacion pa
CROSS JOIN (VALUES (1), (2), (3)) AS nivel(numero_nivel)
WHERE pa.nombre = 'Activacion Estandar'
  AND NOT EXISTS (
    SELECT 1
    FROM planes_activacion_niveles pan
    WHERE pan.plan_activacion_id = pa.id
      AND pan.numero_nivel = nivel.numero_nivel
);

INSERT INTO planes_activacion_niveles (plan_activacion_id, numero_nivel, monto_por_producto)
SELECT pa.id, nivel.numero_nivel, 11.00
FROM planes_activacion pa
CROSS JOIN (VALUES (1), (2), (3)) AS nivel(numero_nivel)
WHERE pa.nombre = 'Activacion Ultra'
  AND NOT EXISTS (
    SELECT 1
    FROM planes_activacion_niveles pan
    WHERE pan.plan_activacion_id = pa.id
      AND pan.numero_nivel = nivel.numero_nivel
);

CREATE TABLE IF NOT EXISTS compras (
    id BIGSERIAL PRIMARY KEY,
    persona_id BIGINT NOT NULL REFERENCES personas(id),
    fecha_compra TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_pv NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_qp NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_cr NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado_compra VARCHAR(30) NOT NULL DEFAULT 'CONFIRMADA',
    usuario_validacion VARCHAR(80),
    fecha_validacion TIMESTAMP,
    usuario_entrega VARCHAR(80),
    fecha_entrega TIMESTAMP,
    metodo_pago VARCHAR(30),
    banco_pago VARCHAR(120),
    cuenta_pago VARCHAR(80),
    codigo_pago VARCHAR(30),
    referencia_pago VARCHAR(180),
    comprobante_pago_url VARCHAR(255),
    comprobante_pago_nombre VARCHAR(180),
    comprobante_pago_tipo VARCHAR(80),
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

ALTER TABLE compras
    ADD COLUMN IF NOT EXISTS usuario_validacion VARCHAR(80),
    ADD COLUMN IF NOT EXISTS fecha_validacion TIMESTAMP,
    ADD COLUMN IF NOT EXISTS usuario_entrega VARCHAR(80),
    ADD COLUMN IF NOT EXISTS fecha_entrega TIMESTAMP,
    ADD COLUMN IF NOT EXISTS metodo_pago VARCHAR(30),
    ADD COLUMN IF NOT EXISTS banco_pago VARCHAR(120),
    ADD COLUMN IF NOT EXISTS cuenta_pago VARCHAR(80),
    ADD COLUMN IF NOT EXISTS codigo_pago VARCHAR(30),
    ADD COLUMN IF NOT EXISTS referencia_pago VARCHAR(180),
    ADD COLUMN IF NOT EXISTS comprobante_pago_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS comprobante_pago_nombre VARCHAR(180),
    ADD COLUMN IF NOT EXISTS comprobante_pago_tipo VARCHAR(80),
    ADD COLUMN IF NOT EXISTS total_cr NUMERIC(12, 2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS compras_detalles (
    id BIGSERIAL PRIMARY KEY,
    compra_id BIGINT NOT NULL REFERENCES compras(id),
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    cantidad INTEGER NOT NULL,
    precio_unitario NUMERIC(12, 2) NOT NULL DEFAULT 0,
    pv_unitario NUMERIC(12, 2) NOT NULL DEFAULT 0,
    qp_unitario NUMERIC(12, 2) NOT NULL DEFAULT 0,
    cr_unitario NUMERIC(12, 2) NOT NULL DEFAULT 0,
    subtotal NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

ALTER TABLE compras_detalles
    ADD COLUMN IF NOT EXISTS cr_unitario NUMERIC(12, 2) NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS beneficios_activacion_compras (
    id BIGSERIAL PRIMARY KEY,
    compra_id BIGINT NOT NULL REFERENCES compras(id),
    beneficiario_id BIGINT NOT NULL REFERENCES personas(id),
    plan_activacion_id BIGINT REFERENCES planes_activacion(id),
    nivel_generado INTEGER NOT NULL,
    cantidad_productos INTEGER NOT NULL,
    monto_por_producto NUMERIC(12, 2) NOT NULL DEFAULT 0,
    monto_total NUMERIC(12, 2) NOT NULL DEFAULT 0,
    paga BOOLEAN NOT NULL DEFAULT FALSE,
    motivo VARCHAR(180),
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);
