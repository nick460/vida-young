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

CREATE TABLE IF NOT EXISTS roles_menus (
    rol_id BIGINT NOT NULL REFERENCES roles(id),
    menu_id BIGINT NOT NULL REFERENCES menus_sistema(id),
    PRIMARY KEY (rol_id, menu_id)
);

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

CREATE TABLE IF NOT EXISTS productos (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(140) NOT NULL,
    descripcion VARCHAR(500),
    categoria VARCHAR(80),
    precio NUMERIC(12, 2) NOT NULL,
    pv NUMERIC(12, 2) NOT NULL DEFAULT 0,
    qp NUMERIC(12, 2) NOT NULL DEFAULT 0,
    qp_bono_referido NUMERIC(12, 2) NOT NULL DEFAULT 0,
    cr NUMERIC(12, 2) NOT NULL DEFAULT 0,
    imagen_url VARCHAR(255),
    imagen_publica_url VARCHAR(255),
    imagen_herramienta_url VARCHAR(255),
    listar_en_shop BOOLEAN NOT NULL DEFAULT FALSE,
    listar_publicamente BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS pv NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS qp NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS qp_bono_referido NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cr NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS precio_publico NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS imagen_publica_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS imagen_herramienta_url VARCHAR(255),
    ADD COLUMN IF NOT EXISTS listar_en_shop BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS listar_publicamente BOOLEAN;

ALTER TABLE productos
    ALTER COLUMN listar_publicamente SET DEFAULT FALSE,
    ALTER COLUMN listar_publicamente SET NOT NULL;

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

CREATE TABLE IF NOT EXISTS compras (
    id BIGSERIAL PRIMARY KEY,
    persona_id BIGINT NOT NULL REFERENCES personas(id),
    fecha_compra TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal NUMERIC(12, 2) NOT NULL DEFAULT 0,
    descuento_monto NUMERIC(12, 2) NOT NULL DEFAULT 0,
    descuento_concepto VARCHAR(180),
    total_pv NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_qp NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_qp_bono_referido NUMERIC(12, 2) NOT NULL DEFAULT 0,
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
    ADD COLUMN IF NOT EXISTS motivo_anulacion VARCHAR(240),
    ADD COLUMN IF NOT EXISTS usuario_anulacion VARCHAR(80),
    ADD COLUMN IF NOT EXISTS fecha_anulacion TIMESTAMP,
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
    ADD COLUMN IF NOT EXISTS descuento_monto NUMERIC(12, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS descuento_concepto VARCHAR(180),
    ADD COLUMN IF NOT EXISTS total_qp_bono_referido NUMERIC(12, 2) NOT NULL DEFAULT 0,
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
    qp_bono_referido_unitario NUMERIC(12, 2) NOT NULL DEFAULT 0,
    cr_unitario NUMERIC(12, 2) NOT NULL DEFAULT 0,
    subtotal NUMERIC(12, 2) NOT NULL DEFAULT 0,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

ALTER TABLE compras_detalles
    ADD COLUMN IF NOT EXISTS qp_bono_referido_unitario NUMERIC(12, 2) NOT NULL DEFAULT 0,
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

CREATE TABLE IF NOT EXISTS notificaciones (
    id BIGSERIAL PRIMARY KEY,
    destinatario_id BIGINT REFERENCES personas(id),
    titulo VARCHAR(120) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    tipo VARCHAR(30) NOT NULL DEFAULT 'INFO',
    link VARCHAR(255),
    leida BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_leida TIMESTAMP,
    fecha_enviado TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    usuario_registro VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    usuario_modificacion VARCHAR(50) DEFAULT 'SYSTEM'
);

CREATE INDEX IF NOT EXISTS idx_notificaciones_destinatario
    ON notificaciones (destinatario_id, leida, fecha_enviado DESC);
