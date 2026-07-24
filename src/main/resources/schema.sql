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

CREATE TABLE IF NOT EXISTS asistente_config (
    id BIGINT PRIMARY KEY,
    system_instruction TEXT,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

ALTER TABLE asistente_config
    ADD COLUMN IF NOT EXISTS system_instruction TEXT,
    ADD COLUMN IF NOT EXISTS estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN IF NOT EXISTS fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM';

INSERT INTO asistente_config (id, system_instruction)
VALUES (1, '')
ON CONFLICT (id) DO NOTHING;

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
    ('login-carousel-config', 'Novedades', 'Images', FALSE, 40),
    ('landing-productos-config', 'Configuracion landings', 'PanelsTopLeft', FALSE, 50),
    ('asistente', 'Asistente', 'Bot', FALSE, 60),
    ('asistente-config', 'Config asistente', 'Settings', FALSE, 70),
    ('personas', 'Personas', 'User', FALSE, 80),
    ('rangos', 'Rangos', 'Trophy', FALSE, 90),
    ('planes', 'Planes', 'BadgePercent', FALSE, 100),
    ('planes-activacion', 'Activaciones', 'Activity', FALSE, 110),
    ('referidos', 'Referidos', 'Network', FALSE, 120),
    ('inventario', 'Inventario', 'PackageSearch', FALSE, 130),
    ('ventanilla', 'Ventanilla', 'Store', FALSE, 140),
    ('registro-referido', 'Registro referido', 'UserPlus', FALSE, 150),
    ('herramientas-digitales', 'Herramientas digitales', 'Wrench', FALSE, 160),
    ('gestiones', 'Gestiones', 'CalendarClock', FALSE, 170),
    ('caja-empresa', 'Caja empresa', 'Building2', FALSE, 180),
    ('retiros-billeteras', 'Retiros billeteras', 'ArrowDownToLine', FALSE, 190),
    ('retiros-nivel-1', 'Retiros nivel 1', 'Gift', FALSE, 200),
    ('wallet', 'Finanzas', 'Wallet', FALSE, 210),
    ('shop', 'Tienda', 'ShoppingBag', FALSE, 220),
    ('network', 'Mi red', 'Users', FALSE, 230),
    ('rewards', 'Recompensas', 'Gift', FALSE, 240),
    ('stats', 'Estadisticas', 'BarChart3', FALSE, 250),
    ('profile', 'Perfil', 'User', FALSE, 260)
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
JOIN menus_sistema m ON m.menu_id IN ('dashboard', 'asistente', 'wallet', 'shop', 'network', 'profile', 'rewards', 'herramientas-digitales')
WHERE r.nombre = 'EMBAJADOR'
ON CONFLICT DO NOTHING;

INSERT INTO roles_menus (rol_id, menu_id)
SELECT r.id, m.id
FROM roles r
JOIN menus_sistema m ON m.menu_id IN ('dashboard', 'asistente', 'wallet', 'shop', 'profile', 'herramientas-digitales')
WHERE r.nombre = 'USUARIO'
ON CONFLICT DO NOTHING;

INSERT INTO roles_menus (rol_id, menu_id)
SELECT r.id, m.id
FROM roles r
JOIN menus_sistema m ON m.menu_id IN ('dashboard', 'asistente', 'shop', 'profile')
WHERE r.nombre = 'CLIENTE'
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS gestiones (
    id BIGSERIAL PRIMARY KEY,
    anio INTEGER NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

CREATE TABLE IF NOT EXISTS login_carousel_items (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(140) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    imagen_url VARCHAR(255) NOT NULL,
    imagen_mobile_url VARCHAR(255),
    orden INTEGER NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

ALTER TABLE login_carousel_items
    ADD COLUMN IF NOT EXISTS titulo VARCHAR(140) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS descripcion VARCHAR(500) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS imagen_url VARCHAR(255) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS imagen_mobile_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS orden INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    ADD COLUMN IF NOT EXISTS fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    ADD COLUMN IF NOT EXISTS usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM';

CREATE INDEX IF NOT EXISTS idx_login_carousel_items_activo_orden
    ON login_carousel_items (activo, orden);

CREATE TABLE IF NOT EXISTS periodos_gestion (
    id BIGSERIAL PRIMARY KEY,
    gestion_id BIGINT NOT NULL REFERENCES gestiones(id),
    mes INTEGER NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    estado_periodo VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM',
    CONSTRAINT uk_periodos_gestion_mes UNIQUE (gestion_id, mes)
);

INSERT INTO gestiones (anio, nombre, fecha_inicio, fecha_fin)
SELECT EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER,
       'Gestion ' || EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER,
       make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 1, 1),
       make_date(EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER, 12, 31)
WHERE NOT EXISTS (
    SELECT 1
    FROM gestiones
    WHERE anio = EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER
);

INSERT INTO periodos_gestion (gestion_id, mes, nombre, fecha_inicio, fecha_fin, estado_periodo)
SELECT g.id,
       EXTRACT(MONTH FROM CURRENT_DATE)::INTEGER,
       initcap(to_char(CURRENT_DATE, 'TMMonth')) || ' ' || g.anio,
       date_trunc('month', CURRENT_DATE)::DATE,
       (date_trunc('month', CURRENT_DATE) + INTERVAL '1 month - 1 day')::DATE,
       CASE
           WHEN EXISTS (SELECT 1 FROM periodos_gestion WHERE estado_periodo = 'ACTIVO')
           THEN 'PENDIENTE'
           ELSE 'ACTIVO'
       END
FROM gestiones g
WHERE g.anio = EXTRACT(YEAR FROM CURRENT_DATE)::INTEGER
  AND NOT EXISTS (
      SELECT 1
      FROM periodos_gestion pg
      WHERE pg.gestion_id = g.id
        AND pg.mes = EXTRACT(MONTH FROM CURRENT_DATE)::INTEGER
  );

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
    imagen_publica_url VARCHAR(255),
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
    ADD COLUMN IF NOT EXISTS precio_publico NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS imagen_publica_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS listar_en_shop BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE productos
SET precio_publico = precio
WHERE precio_publico IS NULL OR precio_publico <= 0;

CREATE TABLE IF NOT EXISTS tipos_cliente_publico (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(220),
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

INSERT INTO tipos_cliente_publico (codigo, nombre, descripcion)
VALUES
    ('NORMAL', 'Cliente normal', 'Cliente publico sin descuentos especiales.'),
    ('PREFERENCIAL', 'Cliente preferencial', 'Cliente publico con descuentos configurables por producto.')
ON CONFLICT (codigo) DO NOTHING;

CREATE TABLE IF NOT EXISTS clientes_publicos (
    id BIGSERIAL PRIMARY KEY,
    distribuidor_id BIGINT NOT NULL REFERENCES personas(id),
    tipo_cliente_id BIGINT NOT NULL REFERENCES tipos_cliente_publico(id),
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    documento VARCHAR(40) NOT NULL,
    email VARCHAR(120),
    telefono VARCHAR(40),
    envio_direccion VARCHAR(220),
    envio_ciudad VARCHAR(80),
    envio_referencia VARCHAR(220),
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM',
    CONSTRAINT uk_clientes_publicos_distribuidor_documento UNIQUE (distribuidor_id, documento)
);

CREATE TABLE IF NOT EXISTS productos_descuentos_cliente (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    tipo_cliente_id BIGINT NOT NULL REFERENCES tipos_cliente_publico(id),
    descuento_monto NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM',
    CONSTRAINT uk_producto_descuento_tipo UNIQUE (producto_id, tipo_cliente_id)
);

CREATE TABLE IF NOT EXISTS compras_publicas (
    id BIGSERIAL PRIMARY KEY,
    distribuidor_id BIGINT NOT NULL REFERENCES personas(id),
    tipo_cliente_id BIGINT NOT NULL REFERENCES tipos_cliente_publico(id),
    cliente_publico_id BIGINT NOT NULL REFERENCES clientes_publicos(id),
    fecha_compra TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado_compra VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    cliente_nombres VARCHAR(100) NOT NULL,
    cliente_apellidos VARCHAR(100) NOT NULL,
    cliente_documento VARCHAR(40) NOT NULL,
    cliente_email VARCHAR(120),
    cliente_telefono VARCHAR(40),
    envio_requiere BOOLEAN NOT NULL DEFAULT FALSE,
    envio_direccion VARCHAR(220),
    envio_ciudad VARCHAR(80),
    envio_referencia VARCHAR(220),
    metodo_pago VARCHAR(30),
    referencia_pago VARCHAR(180),
    comprobante_pago_url VARCHAR(255),
    comprobante_pago_nombre VARCHAR(180),
    comprobante_pago_tipo VARCHAR(80),
    total_cliente NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_empresa NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_descuento NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_ganancia_distribuidor NUMERIC(12, 2) NOT NULL DEFAULT 0,
    usuario_validacion VARCHAR(80),
    fecha_validacion TIMESTAMP,
    usuario_entrega VARCHAR(80),
    fecha_entrega TIMESTAMP,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

ALTER TABLE compras_publicas
    ADD COLUMN IF NOT EXISTS cliente_publico_id BIGINT REFERENCES clientes_publicos(id),
    ADD COLUMN IF NOT EXISTS comprobante_pago_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS comprobante_pago_nombre VARCHAR(180),
    ADD COLUMN IF NOT EXISTS comprobante_pago_tipo VARCHAR(80),
    ADD COLUMN IF NOT EXISTS periodo_id BIGINT REFERENCES periodos_gestion(id);

CREATE TABLE IF NOT EXISTS compras_publicas_detalles (
    id BIGSERIAL PRIMARY KEY,
    compra_id BIGINT NOT NULL REFERENCES compras_publicas(id),
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    cantidad INTEGER NOT NULL,
    precio_distribuidor_unitario NUMERIC(12, 2) NOT NULL,
    precio_publico_unitario NUMERIC(12, 2) NOT NULL,
    descuento_unitario NUMERIC(12, 2) NOT NULL DEFAULT 0,
    precio_final_unitario NUMERIC(12, 2) NOT NULL,
    subtotal_cliente NUMERIC(12, 2) NOT NULL,
    subtotal_empresa NUMERIC(12, 2) NOT NULL,
    subtotal_descuento NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ganancia_distribuidor NUMERIC(12, 2) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

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
    monto_efectivo_retirado NUMERIC(12, 2) NOT NULL DEFAULT 0,
    valor_productos_retirado NUMERIC(12, 2) NOT NULL DEFAULT 0,
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
    ADD COLUMN IF NOT EXISTS motivo_no_cobrable VARCHAR(180),
    ADD COLUMN IF NOT EXISTS monto_efectivo_retirado NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS valor_productos_retirado NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS periodo_id BIGINT REFERENCES periodos_gestion(id);

CREATE TABLE IF NOT EXISTS preinscripciones_referidos (
    id BIGSERIAL PRIMARY KEY,
    patrocinador_id BIGINT NOT NULL REFERENCES personas(id),
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    documento VARCHAR(30) NOT NULL,
    telefono VARCHAR(30) NOT NULL,
    email VARCHAR(120),
    username_solicitado VARCHAR(50) NOT NULL DEFAULT '',
    password_solicitado VARCHAR(255) NOT NULL DEFAULT '',
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

ALTER TABLE preinscripciones_referidos
    ADD COLUMN IF NOT EXISTS username_solicitado VARCHAR(50) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS password_solicitado VARCHAR(255) NOT NULL DEFAULT '';

CREATE INDEX IF NOT EXISTS idx_preinscripciones_referidos_estado
    ON preinscripciones_referidos (estado_preinscripcion);

CREATE INDEX IF NOT EXISTS idx_preinscripciones_referidos_documento
    ON preinscripciones_referidos (documento);

CREATE INDEX IF NOT EXISTS idx_preinscripciones_referidos_username
    ON preinscripciones_referidos (username_solicitado);

CREATE TABLE IF NOT EXISTS billeteras (
    id BIGSERIAL PRIMARY KEY,
    persona_id BIGINT NOT NULL REFERENCES personas(id),
    saldo_dinero NUMERIC(12, 2) NOT NULL DEFAULT 0,
    saldo_pv NUMERIC(12, 2) NOT NULL DEFAULT 0,
    saldo_qp NUMERIC(12, 2) NOT NULL DEFAULT 0,
    saldo_cr NUMERIC(12, 2) NOT NULL DEFAULT 0,
    saldo_productos NUMERIC(12, 2) NOT NULL DEFAULT 0,
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

CREATE TABLE IF NOT EXISTS retiros_billetera (
    id BIGSERIAL PRIMARY KEY,
    persona_id BIGINT NOT NULL REFERENCES personas(id),
    monto_dinero NUMERIC(12, 2) NOT NULL DEFAULT 0,
    monto_productos NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado_retiro VARCHAR(30) NOT NULL DEFAULT 'PROCESADO',
    fecha_retiro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacion VARCHAR(240),
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

ALTER TABLE movimientos_billetera
    ADD COLUMN IF NOT EXISTS periodo_id BIGINT REFERENCES periodos_gestion(id);

ALTER TABLE retiros_billetera
    ADD COLUMN IF NOT EXISTS periodo_id BIGINT REFERENCES periodos_gestion(id);

ALTER TABLE retiros_billetera
    ADD COLUMN IF NOT EXISTS referencia_tipo VARCHAR(60);

ALTER TABLE retiros_billetera
    ADD COLUMN IF NOT EXISTS referencia_id BIGINT;

CREATE TABLE IF NOT EXISTS retiros_billetera_detalles (
    id BIGSERIAL PRIMARY KEY,
    retiro_id BIGINT NOT NULL REFERENCES retiros_billetera(id),
    producto_id BIGINT NOT NULL REFERENCES productos(id),
    cantidad INTEGER NOT NULL,
    precio_proveedor NUMERIC(12, 2) NOT NULL DEFAULT 0,
    subtotal NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

CREATE TABLE IF NOT EXISTS carteras_empresa (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL,
    saldo_actual NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

CREATE TABLE IF NOT EXISTS movimientos_cartera_empresa (
    id BIGSERIAL PRIMARY KEY,
    cartera_id BIGINT NOT NULL REFERENCES carteras_empresa(id),
    tipo VARCHAR(20) NOT NULL,
    concepto VARCHAR(180) NOT NULL,
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

ALTER TABLE movimientos_cartera_empresa
    ADD COLUMN IF NOT EXISTS periodo_id BIGINT REFERENCES periodos_gestion(id);

INSERT INTO carteras_empresa (codigo, nombre, saldo_actual)
SELECT 'PRINCIPAL', 'Caja principal de la empresa', 0
WHERE NOT EXISTS (SELECT 1 FROM carteras_empresa WHERE codigo = 'PRINCIPAL');

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
    saldo_productos NUMERIC(12, 2) NOT NULL DEFAULT 0,
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
    ADD COLUMN IF NOT EXISTS saldo_productos NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rango_id BIGINT REFERENCES rangos(id),
    ADD COLUMN IF NOT EXISTS rango_nombre VARCHAR(100),
    ADD COLUMN IF NOT EXISTS rango_qp_minimo NUMERIC(12, 2),
    ADD COLUMN IF NOT EXISTS periodo_id BIGINT REFERENCES periodos_gestion(id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_historial_membresias_referencia
    ON historial_membresias (referencia_tipo, referencia_id, tipo)
    WHERE referencia_tipo IS NOT NULL AND referencia_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_movimientos_billetera_referencia
    ON movimientos_billetera (referencia_tipo, referencia_id, tipo)
    WHERE referencia_tipo IS NOT NULL AND referencia_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_movimientos_cartera_empresa_referencia
    ON movimientos_cartera_empresa (referencia_tipo, referencia_id, tipo)
    WHERE referencia_tipo IS NOT NULL AND referencia_id IS NOT NULL;

ALTER TABLE billeteras
    ADD COLUMN IF NOT EXISTS saldo_cr NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS saldo_productos NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ALTER COLUMN estado SET DEFAULT 'ACTIVO',
    ALTER COLUMN fecha_registro SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN fecha_modificacion SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN usuario_registro SET DEFAULT 'SYSTEM',
    ALTER COLUMN usuario_modificacion SET DEFAULT 'SYSTEM';

ALTER TABLE historial_membresias
    ADD COLUMN IF NOT EXISTS periodo_id BIGINT REFERENCES periodos_gestion(id),
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

INSERT INTO billeteras (persona_id, saldo_dinero, saldo_pv, saldo_qp, saldo_cr, saldo_productos)
SELECT p.id, 0, 0, 0, 0, 0
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
    'PRODUCTOS',
    'Productos canjeables por recompensa #' || r.id,
    'RECOMPENSA_PRODUCTOS',
    r.id,
    GREATEST(COALESCE(r.valor_productos, 0) - COALESCE(r.valor_productos_retirado, 0), 0),
    0
FROM recompensas r
JOIN billeteras b ON b.persona_id = r.beneficiario_id
WHERE r.estado = 'ACTIVO'
  AND r.cobrable = TRUE
  AND GREATEST(COALESCE(r.valor_productos, 0) - COALESCE(r.valor_productos_retirado, 0), 0) > 0
  AND NOT EXISTS (
    SELECT 1
    FROM movimientos_billetera mb
    WHERE mb.referencia_tipo = 'RECOMPENSA_PRODUCTOS'
      AND mb.referencia_id = r.id
      AND mb.tipo = 'PRODUCTOS'
);

UPDATE billeteras b
SET saldo_productos = COALESCE((
    SELECT SUM(mb.monto)
    FROM movimientos_billetera mb
    WHERE mb.billetera_id = b.id
      AND mb.tipo = 'PRODUCTOS'
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
    ADD COLUMN IF NOT EXISTS total_cr NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS periodo_id BIGINT REFERENCES periodos_gestion(id);

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

ALTER TABLE beneficios_activacion_compras
    ADD COLUMN IF NOT EXISTS periodo_id BIGINT REFERENCES periodos_gestion(id);

WITH periodo_activo AS (
    SELECT id
    FROM periodos_gestion
    WHERE estado_periodo = 'ACTIVO'
    ORDER BY fecha_inicio DESC
    LIMIT 1
)
UPDATE compras
SET periodo_id = (SELECT id FROM periodo_activo)
WHERE periodo_id IS NULL
  AND EXISTS (SELECT 1 FROM periodo_activo);

WITH periodo_activo AS (
    SELECT id
    FROM periodos_gestion
    WHERE estado_periodo = 'ACTIVO'
    ORDER BY fecha_inicio DESC
    LIMIT 1
)
UPDATE compras_publicas
SET periodo_id = (SELECT id FROM periodo_activo)
WHERE periodo_id IS NULL
  AND EXISTS (SELECT 1 FROM periodo_activo);

WITH periodo_activo AS (
    SELECT id
    FROM periodos_gestion
    WHERE estado_periodo = 'ACTIVO'
    ORDER BY fecha_inicio DESC
    LIMIT 1
)
UPDATE historial_membresias
SET periodo_id = (SELECT id FROM periodo_activo)
WHERE periodo_id IS NULL
  AND EXISTS (SELECT 1 FROM periodo_activo);

WITH periodo_activo AS (
    SELECT id
    FROM periodos_gestion
    WHERE estado_periodo = 'ACTIVO'
    ORDER BY fecha_inicio DESC
    LIMIT 1
)
UPDATE recompensas
SET periodo_id = (SELECT id FROM periodo_activo)
WHERE periodo_id IS NULL
  AND EXISTS (SELECT 1 FROM periodo_activo);

WITH periodo_activo AS (
    SELECT id
    FROM periodos_gestion
    WHERE estado_periodo = 'ACTIVO'
    ORDER BY fecha_inicio DESC
    LIMIT 1
)
UPDATE movimientos_billetera
SET periodo_id = (SELECT id FROM periodo_activo)
WHERE periodo_id IS NULL
  AND EXISTS (SELECT 1 FROM periodo_activo);

WITH periodo_activo AS (
    SELECT id
    FROM periodos_gestion
    WHERE estado_periodo = 'ACTIVO'
    ORDER BY fecha_inicio DESC
    LIMIT 1
)
UPDATE retiros_billetera
SET periodo_id = (SELECT id FROM periodo_activo)
WHERE periodo_id IS NULL
  AND EXISTS (SELECT 1 FROM periodo_activo);

UPDATE retiros_billetera retiro
SET referencia_tipo = 'RETIRO_RECOMPENSA_NIVEL_1'
FROM movimientos_cartera_empresa movimiento
WHERE retiro.referencia_tipo IS NULL
  AND movimiento.referencia_tipo = 'RETIRO_RECOMPENSA_NIVEL_1'
  AND movimiento.referencia_id = retiro.id;

WITH periodo_activo AS (
    SELECT id
    FROM periodos_gestion
    WHERE estado_periodo = 'ACTIVO'
    ORDER BY fecha_inicio DESC
    LIMIT 1
)
UPDATE movimientos_cartera_empresa
SET periodo_id = (SELECT id FROM periodo_activo)
WHERE periodo_id IS NULL
  AND EXISTS (SELECT 1 FROM periodo_activo);

WITH periodo_activo AS (
    SELECT id
    FROM periodos_gestion
    WHERE estado_periodo = 'ACTIVO'
    ORDER BY fecha_inicio DESC
    LIMIT 1
)
UPDATE cierres_mensuales_billetera
SET periodo_id = (SELECT id FROM periodo_activo)
WHERE periodo_id IS NULL
  AND EXISTS (SELECT 1 FROM periodo_activo);

WITH periodo_activo AS (
    SELECT id
    FROM periodos_gestion
    WHERE estado_periodo = 'ACTIVO'
    ORDER BY fecha_inicio DESC
    LIMIT 1
)
UPDATE beneficios_activacion_compras
SET periodo_id = (SELECT id FROM periodo_activo)
WHERE periodo_id IS NULL
  AND EXISTS (SELECT 1 FROM periodo_activo);

INSERT INTO movimientos_cartera_empresa (cartera_id, tipo, concepto, referencia_tipo, referencia_id, monto, saldo_resultado)
SELECT c.id, ingreso.tipo, ingreso.concepto, ingreso.referencia_tipo, ingreso.referencia_id, ingreso.monto, 0
FROM carteras_empresa c
JOIN (
    SELECT
        'INGRESO' AS tipo,
        'Ingreso historico por venta interna #' || co.id AS concepto,
        'VENTA_INTERNA' AS referencia_tipo,
        co.id AS referencia_id,
        COALESCE(co.subtotal, 0) AS monto
    FROM compras co
    WHERE co.estado_compra IN ('VALIDADA', 'ENTREGADA', 'CONFIRMADA')
      AND COALESCE(co.subtotal, 0) > 0
    UNION ALL
    SELECT
        'INGRESO' AS tipo,
        'Ingreso historico por venta publica #' || cp.id AS concepto,
        'VENTA_PUBLICA' AS referencia_tipo,
        cp.id AS referencia_id,
        COALESCE(cp.total_cliente, 0) AS monto
    FROM compras_publicas cp
    WHERE cp.estado_compra IN ('VALIDADA', 'ENTREGADA')
      AND COALESCE(cp.total_cliente, 0) > 0
    UNION ALL
    SELECT
        'INGRESO' AS tipo,
        'Ingreso historico por membresia #' || hm.id AS concepto,
        COALESCE(hm.referencia_tipo, 'MEMBRESIA_ACTIVACION') AS referencia_tipo,
        COALESCE(hm.referencia_id, hm.id) AS referencia_id,
        COALESCE(hm.precio_plan, 0) AS monto
    FROM historial_membresias hm
    WHERE COALESCE(hm.precio_plan, 0) > 0
) ingreso ON c.codigo = 'PRINCIPAL'
WHERE NOT EXISTS (
    SELECT 1
    FROM movimientos_cartera_empresa existente
    WHERE existente.referencia_tipo = ingreso.referencia_tipo
      AND existente.referencia_id = ingreso.referencia_id
      AND existente.tipo = ingreso.tipo
);

WITH acumulados AS (
    SELECT
        m.id,
        SUM(m.monto) OVER (ORDER BY m.fecha_registro, m.id) AS saldo
    FROM movimientos_cartera_empresa m
    JOIN carteras_empresa c ON c.id = m.cartera_id
    WHERE c.codigo = 'PRINCIPAL'
)
UPDATE movimientos_cartera_empresa m
SET saldo_resultado = acumulados.saldo
FROM acumulados
WHERE m.id = acumulados.id;

UPDATE carteras_empresa c
SET saldo_actual = COALESCE((
    SELECT SUM(m.monto)
    FROM movimientos_cartera_empresa m
    WHERE m.cartera_id = c.id
      AND m.estado = 'ACTIVO'
), 0)
WHERE c.codigo = 'PRINCIPAL';

WITH periodo_activo AS (
    SELECT id
    FROM periodos_gestion
    WHERE estado_periodo = 'ACTIVO'
    ORDER BY fecha_inicio DESC
    LIMIT 1
)
UPDATE movimientos_cartera_empresa
SET periodo_id = (SELECT id FROM periodo_activo)
WHERE periodo_id IS NULL
  AND EXISTS (SELECT 1 FROM periodo_activo);
