# Lógica de Negocio — Plataforma Vida Young (MLM)

> Documento generado el 2026-09-30 a partir del código actual en `D:\vida-young`.
> Backend: Spring Boot 4.0.6 / Java 17 / JPA + PostgreSQL (`pom.xml:1-99`).
> Frontend: Vue 3.4 + vue-router 4.4 + Pinia + Firebase 11 + STOMP/SockJS (`frontend/package.json:1-27`).
> Config: `src/main/resources/application.properties:1-56` → `server.port=9095`, BD `jdbc:postgresql://localhost:5432/vidayoung_db`, JWT 24h (`86400000`), uploads 5MB, Firebase FCM activado, Gemini `gemini-3.1-flash-lite`.

---

## 1. Visión general del sistema

Vida Young es un **MLM unilevel** (red por patrocinio directo) con:

1. **Afiliación por Planes** (ej. Plan con precio, QP, alcance N niveles).
2. **Membresía mensual** por Activación (por compra de Plan o por volumen PV propio).
3. **Compras internas** (distribuidores) que generan volumen PV/QP/CR + comisiones.
4. **Tienda pública** (`/tienda/:username`) para clientes externos.
5. **Dos esquemas de ganancia**:
   - **Recompensas / Bono patrocinio** (`Recompensa.java`): al afiliarse alguien, sus uplines ganan efectivo (todos los niveles) + productos (solo nivel 1).
   - **Beneficios por Activación** (`BeneficioActivacionCompra.java`): al comprar productos, los uplines ganan `cantidad × montoPorProducto` por nivel (hasta 10 niveles).
6. **Billetera por persona** (`Billetera.java`) con 5 saldos: `DINERO, PV, PV_PROPIO, QP, CR, PRODUCTOS`.
7. **Rangos** por QP acumulado.
8. **Periodos mensuales** (`Gestion` + `PeriodoGestion`) + **cierre mensual** con retiro obligatorio total.
9. **Cartera de empresa** (caja única) que registra todos los ingresos/egresos.
10. **Roles y menús dinámicos** + notificaciones (BD + WebSocket + FCM) + landings/CMS + asistente Gemini.

Todas las entidades heredan de `Model/Entity/Auditoria.java` (`id, estado=ACTIVO/ELIMINADO, fechaRegistro...`). El borrado es **lógico** y todos los servicios filtran por `ACTIVO`.

---

## 2. Actores y roles

| Rol (`Model/Entity/Rol.java`) | Acceso frontend (`navigation/menuConfig.js`, `main.js` guard) |
|---|---|
| `ADMIN` | Todo: `/admin, /roles-menus, /ventanilla, /consultar, /personas, /rangos, /planes, /planes-activacion, /referidos, /inventario, /gestiones, /caja-empresa, /retiros-billeteras, /retiros-nivel-1, /logs, /notificaciones, /stats`, landings-config, etc. Puede hacer `POST /api/auth/impersonate/{usuarioId}`. |
| `EMBAJADOR` | `/dashboard, /network, /rewards, /wallet, /shop, /cart, /herramientas-digitales, /profile, /asistente` |
| `USUARIO` | `/dashboard, /wallet, /shop, /cart, /herramientas-digitales, /profile, /asistente` |
| `CLIENTE` | Solo `/shop` (compra como cliente) |
| Público (sin login, `meta.public`) | `/, /landing, /producto/:productId, /herramienta/:slug, /referido/:username, /preinscripcion-referido/:patrocinadorId, /tienda/:username, /carrito, /login`, `GET /api/public/**`, `GET /api/planes/public` |

Seguridad (`Security/SecurityConfig.java`, `JwtService.java`, `JwtAuthenticationFilter.java`, `CustomUserDetailsService.java`):
- `csrf.disable, STATELESS`, `permitAll` en `/api/auth/login, /api/public/**, /api/planes/public, /uploads/**, /ws/**, /ws-notificaciones/**`; resto `authenticated`.
- JWT manual HS256 (`HmacSHA256(secret)`), claims `sub, roles[], iat, exp=24h`, validación con `MessageDigest.isEqual` (constant-time).
- Password con `BCrypt`. Usuario seed: `Config/DataInitializer.java` → `Persona 00000000/Admin + Rol ADMIN + Usuario admin/admin123`.
- WebSocket STOMP+SockJS en `/ws-notificaciones` (`WebSocketConfig.java`, `JwtWebSocketHandshakeInterceptor.java`, `PrincipalHandshakeHandler.java`, `StompPrincipal.java`) para notificaciones realtime.
- CORS y `/uploads/**` público (`Config/WebConfig.java`). FCM push (`Config/FirebaseConfig.java`, `Service/DispositivoService.java`, `frontend/src/services/fcm-service.js` → `POST /api/dispositivos/vincular`).

> No hay jobs automáticos: `Config/MembresiaScheduler.java` está vacío y `PlatformApplication.java` solo habilita `@EnableScheduling`. Vencimientos y cierres son **manuales** vía endpoints admin.

---

## 3. Modelo de datos (40 entidades en `Model/Entity/`)

### 3.1 Personas, usuarios y red
- **`Persona`** (`personas`): `nombres, apellidos, documento(unique), email, telefono, direccion..., rangoActual @ManyToOne Rango, usuario @OneToOne`.
- **`Usuario`** (`usuarios`): `username(unique), password(BCrypt), activo, fotoPerfil, persona @OneToOne, roles @ManyToMany`.
- **`Rol`** (`roles` + join `roles_menus`): `nombre, descripcion, menus @ManyToMany MenuSistema`.
- **`Referido`** (`referidos`, unique `persona_id`): el nodo de red unilevel. `persona, patrocinador @ManyToOne Persona, plan @ManyToOne Plan, fechaUnion, fechaInicioMembresia, fechaFinMembresia:LocalDateTime, membresiaActiva:Boolean`.
- **`PreinscripcionReferido`** (`preinscripciones_referidos`): `patrocinador, nombres, documento, usernameDisponible, estado=PENDIENTE/VALIDADO/RECHAZADO`. Flujo público → admin valida/rechaza.
- **`MenuSistema`** (`menus_sistema`): `menuId (dashboard, logs...), label, icon, custom, orden`.
- **`Dispositivo`** (`dispositivos`): `persona, token FCM, activo`.

### 3.2 Planes, rangos, productos
- **`Plan`** (`planes`): plan de afiliación. `nombre, precio, qp, bonificacionDirecta, valorProductosBeneficio, nivelesAlcanceInt (ej. 9), niveles @OneToMany, productos @OneToMany`.
- **`PlanNivel`** (`planes_niveles`): `plan, numeroNivel, porcentajeComision:BigDecimal` (= monto efectivo que gana el upline de ese nivel), `valorProductosBeneficio` (solo nivel 1 paga productos).
- **`PlanProducto`** (`planes_productos`): `plan, producto, cantidad` (kit que recibe el afiliado).
- **`PlanActivacion`** (`planes_activacion`): plan de activación mensual. `nombre, pvMinimoMensual, nivelesAlcance (ej. 10), niveles @OneToMany`.
- **`PlanActivacionNivel`** (`planes_activacion_niveles`): `planActivacion, numeroNivel, montoPorProducto:BigDecimal` (S/ por producto vendido en la red).
- **`Rango`** (`rangos`): `nombre, qpMinimo, nivelesExtra:Int` (amplía alcance de activación), `icono...`, ordenado por `qpMinimo`.
- **`Producto`** (`productos`): `sku, nombre, precio, pv, qp, cr, stock, categoria @ManyToOne, imagenUrl`.
- **`ProductoCategoria`** (`productos_categorias`): `nombre, descripcion`.
- **`ProductoLanding`** (`productos_landings`): `producto @OneToOne, slug, secciones JSON`.
- **`ProductoDescuentoCliente`**: `producto, tipoCliente, descuentoUnitario`.
- **`TipoClientePublico`** (`tipos_cliente_publico`): `nombre, descuento%`.
- **`ClientePublico`**: `distribuidor @ManyToOne, tipoCliente, nombres/apellidos/documento/email/telefono/envioDireccion/Ciudad/Referencia`.

### 3.3 Compras
- **`Compra`** (`compras`): compra interna de distribuidor. `persona, periodo:PeriodoGestion, fechaCompra, estadoCompra=PENDIENTE/VALIDADA/RECHAZADA/CONFIRMADA/ANULADA, metodoPago/banco/cuenta/codigo/referencia/comprobanteUrl, subtotal, descuentoMonto/Concepto, totalPv/Qp/Cr, detalles @OneToMany, fechaValidacion/usuarioValidacion, motivoAnulacion/usuarioAnulacion/fechaAnulacion`.
- **`CompraDetalle`** (`compras_detalles`): `compra, producto, cantidad, precioUnitario, pv/qp/crUnitario, subtotal`.
- **`CompraPublica`** (`compras_publicas`): compra de cliente externo en `/tienda/:username`. `distribuidor Persona, clientePublico, tipoCliente, periodo, subtotalCliente, estado`.
- **`CompraPublicaDetalle`**: `compra, producto, cantidad, precioDistribuidorUnitario, precioPublicoUnitario, descuentoUnitario, precioFinalUnitario, subtotalCliente`.
- **`BeneficioActivacionCompra`** (`beneficios_activacion_compras`): ganancia generada por una compra hacia cada upline. `compra, periodo, beneficiario Persona, planActivacion, nivelGenerado 1..10, cantidadProductos, montoPorProducto, montoTotal, paga:Boolean, motivo:String`. **Siempre se crea la fila** (pague o no) para permitir pago retroactivo.

### 3.4 Dinero: billetera, recompensas, cierres, caja
- **`Billetera`** (`billeteras`, unique `persona_id`): `persona @OneToOne, saldoDinero, saldoPv (total propio+red), saldoPvPropio (solo compras propias → activa membresía/plan activación), saldoQp (define rango), saldoCr, saldoProductos`.
- **`MovimientoBilletera`** (`movimientos_billetera`): libro contable. `billetera, tipo=PV/QP/CR/DINERO/PRODUCTOS, concepto, referenciaTipo+referenciaId (COMPRA, COMPRA_RED, BENEFICIO_ACTIVACION_COMPRA, RECOMPENSA_PRODUCTOS, RETIRO_BILLETERA, CIERRE_MENSUAL, MEMBRESIA_ACTIVACION, REFERIDO_AFILIACION, PV_ACTIVACION...), monto, saldoResultado, periodo`.
- **`Recompensa`** (`recompensas`, bono patrocinio/afiliación): `referido (origen/afiliado), beneficiario (upline), planIngreso, nivelGenerado, montoEfectivo, montoEfectivoRetirado, valorProductos/Retirado, cobrable:Boolean, motivoNoCobrable, periodo`.
- **`RetiroBilletera`** (`retiros_billetera`): `persona, montoDinero, montoProductos, estadoRetiro=PROCESADO, fechaRetiro, observacion, periodo, detalles`.
- **`RetiroBilleteraDetalle`**: `retiro, producto, cantidad, precioProveedor, subtotal`.
- **`CierreMensualBilletera`**: `persona, periodo=YYYY-MM, snapshot saldoDinero/Pv/Qp/Cr/Productos, rango/rangoNombre/rangoQpMinimo, estadoPlanilla=PAGADA/PENDIENTE, fechaCierre, periodoGestion`.
- **`HistorialMembresia`** (`historial_membresias`): `persona, plan, tipo=AFILIACION/ACTIVACION, fechaInicio/Fin, precioPlan/qpPlan, referenciaTipo/Id, estadoMembresia=ACTIVA/VENCIDA, periodo`.
- **`CarteraEmpresa`** (`carteras_empresa`): caja única `codigo=CARTERA_PRINCIPAL, nombre, saldoActual`.
- **`MovimientoCarteraEmpresa`**: `cartera, tipo=INGRESO/EGRESO, concepto, referenciaTipo/Id, monto, saldoResultado, periodo`.

### 3.5 Periodos y misceláneos
- **`Gestion`** (`gestiones`): `anio(unique), nombre, fechaInicio 01-01, fechaFin 31-12`.
- **`PeriodoGestion`** (`periodos_gestion`): `gestion, mes 1-12, nombre ("Enero 2026"), fechaInicio/Fin:LocalDate, estadoPeriodo=ACTIVO/PENDIENTE_CIERRE/CERRADO/PENDIENTE`.
- **`Notificacion`** (`notificaciones`): `personaId, tipo=COMPRA/MEMBRESIA/RANGO/RECOMPENSA..., titulo, mensaje, menuDestino (wallet/shop/network), leida`.
- **`DigitalLanding`** (`digital_landings`): `slug, titulo, contenido JSON`.
- **`LoginCarouselItem`**: `titulo, imagenUrl, orden, activo`.
- **`AsistenteConfig`** (`asistente_config`): `systemInstruction:TEXT` (Gemini).

---

## 4. Reglas de negocio por módulo

### 4.1 Afiliación / Red unilevel (`Model/ServiceImpl/ReferidoServiceImpl.java:358`, `BilleteraServiceImpl.java:1114`)
1. `POST /api/referidos?personaId&patrocinadorId&planId` → `guardar()`:
   - Valida: no duplicado (`persona` ya tiene `Referido`), no auto-patrocinio.
   - `fechaUnion=now`. Si es nuevo: `fechaInicio=inicioPeriodoActivo`, `fechaFin=calcularFechaFinInicial()`: **día ≤ 14 → fin del periodo activo, sino fin del mes siguiente**; `membresiaActiva=true`.
   - Luego: `registrarAfiliacionInicial()` + `regenerarRecompensas()` + notifica al patrocinador.
2. `registrarAfiliacionInicial(referido)`:
   - Crea `Historial AFILIACION` idempotente.
   - `cartera.registrarIngreso(REFERIDO_AFILIACION, plan.precio)`.
   - Si hay patrocinador y `qpPlan>0`: `patrocinador.saldoQp += qpPlan` + `actualizarRango` + movimiento `TIPO_QP`.
3. `regenerarRecompensas(referido)` (bono patrocinio):
   - Borra (lógico `ELIMINADO`) recompensas previas de ese referido.
   - Camina `patrocinador` hacia arriba hasta `plan.nivelesAlcance` (ej. 9).
   - Por nivel: `efectivo = PlanNivel.porcentajeComision`, `productos = valorProductos solo si nivel==1`.
   - `cobrable = (nivel==1) || beneficiarioMembresiaActiva`. Nivel ≥ 2 exige membresía activa del cobrador.
   - Crea `Recompensa` + `sincronizarSaldoProductos()` + movimiento `RECOMPENSA_PRODUCTOS`.
4. `eliminar(id)`: reasigna hijos al `patrocinadorInmediato` (no se pierde la red) + regenera recompensas de cada hijo + marca `ELIMINADO + desactivarRecompensas`.
5. Vencimientos (manuales, sin cron): `vencerMembresiasExpiradas()` (`fechaFin <= inicioPeriodoActivo → membresia=false + vencerHistorial`), `desactivarMembresiasActivas()` (`fechaFin <= finPeriodo 23:59 → false`).
6. `actualizarRecompensasCobrables(persona, cobrable)`: nivel 1 siempre cobrable; nivel ≥ 2 = `cobrable` (se reactiva al pagar activación).

### 4.2 Activación de membresía (`BilleteraServiceImpl.java`)
Dos vías, ambas dejan `Referido.membresiaActiva=true`:

**A. Por compra de Plan** — `POST /api/billeteras/persona/{id}/activaciones {planId}` → `registrarActivacion()`:
- Cambia `Referido.plan`, `fechaInicio=inicioPeriodoActivo, fechaFin=finPeriodo 23:59:59, membresia=true`.
- Crea `Historial ACTIVACION`, `cartera Ingreso MEMBRESIA_ACTIVACION (precio)`, `saldoQp += qpPlan + actualizarRango + actualizarRecompensasCobrables(true)`.
- Dispara `recalcularBeneficiosActivacion(persona)` (pago retroactivo de beneficios pendientes).

**B. Por volumen PV propio** — automática en cada compra validada → `activarMembresiaPorPv(persona, pvActual, periodo)`:
- `planActivacion = max(pvMinimo <= saldoPvPropio)`. Solo `saldoPvPropio` (compras propias) activa, no el PV de red.
- Si ya `membresiaActiva && fechaFin >= finPeriodo` → no hace nada.
- Sino `membresia=true` + notifica `Membresia activada por PV` + `Historial PV_ACTIVACION precio=0` idempotente.

### 4.3 Compras internas (`Model/ServiceImpl/CompraServiceImpl.java:968`, `Rescontroller/CompraRestController.java`)
Estados: `PENDIENTE → VALIDADA/CONFIRMADA (procesan) | RECHAZADA | ANULADA`. `ENTREGADA` deshabilitada.

1. `POST /api/compras/persona/{personaId} {items, pago}` → `registrarCompra()`:
   - Crea `PENDIENTE`, `recalcularCompra()` suma `precio×cantidad, pv×cantidad, qp×cantidad, cr×cantidad`.
   - Valida `descuento <= subtotal + concepto obligatorio`, asigna `periodo=obtenerPeriodoActivo()`, notifica `TIPO_COMPRA`.
2. `PUT /{compraId}` → `modificarCompra()`: solo si `PENDIENTE` (`clear detalles + recalcular`).
3. `PUT /{compraId}/estado?estado&usuario` → `cambiarEstado()` + `validarCambioEstado()`: `RECHAZADA` solo desde `PENDIENTE`; `VALIDADA` solo desde `PENDIENTE/VALIDADA`. Si `!antesProcesada && ahoraProcesada (VALIDADA/CONFIRMADA)` → `procesarCompraValidada()`.
4. `procesarCompraValidada()` (orden fijo):
   1. `carteraEmpresa.registrarIngreso(VENTA_INTERNA, compraId, subtotal)`.
   2. `acreditarVolumenComprador()`: idempotente (`existsByReferenciaTipo(COMPRA,compraId)`); `saldoPv += totalPv`, **`saldoPvPropio += totalPv`** (único que activa), `saldoQp +=`, `saldoCr +=` + `actualizarRangoActual()`.
   3. `billetera.activarMembresiaPorPv(...)`.
   4. `recalcularBeneficiosActivacion(persona)` (retroactivo).
   5. `acreditarVolumenRed()`: sube por `Referido.patrocinador` **9 niveles** (`NIVELES_ALCANCE_RED=9`), acredita mismo `totalPv/Qp` a cada `saldoPv/Qp` con ref `COMPRA_RED`, notifica `Volumen de red nivel N`. No acredita `PvPropio` ni `CR` a la red.
   6. `generarBeneficiosActivacion()` si está vacío (ver 4.4).
5. `POST /{compraId}/anular?motivo` → `anularCompra()`: solo `VALIDADA`. `revertirMovimientosCompra()` (resta saldos, si `saldo<0` error "ya fue utilizado", crea movimiento `ANULACION_COMPRA` negativo, marca `ELIMINADO`) + `revertirBeneficiosCompra()` + `cartera.registrarEgreso(ANULACION_COMPRA)` + marca `ANULADA`.
6. Admin: `PUT /{compraId}/admin-reconstruir` → `reconstruirCompraAdmin()`: anula lógicamente la vieja (conserva fecha/periodo/validación), crea nueva `PENDIENTE`, recalcula y si era procesada la re-valida/reprocesa. `GET /{compraId}/beneficios`, `GET /{compraId}/movimientos` (une `COMPRA + COMPRA_RED + BENEFICIO/AJUSTE/ANULACION + CARTERA`).

### 4.4 Beneficios por Activación — el pago por compras de la red (`CompraServiceImpl.java`, `BilleteraServiceImpl.java`)
Constante: `BilleteraService.NIVELES_TOTALES=10`.

`generarBeneficiosActivacion(compra, totalProductos)`: camina hasta 10 uplines. Para cada beneficiario:
- `plan = obtenerPlanActivacionVigente(saldoPvPropio)` = mayor `pvMinimo <= pvPropio`.
- `alcanceEfectivo = calcularAlcanceEfectivo(persona, plan) = min(10, plan.nivelesAlcance + rango.nivelesExtra)`.
- `montoPorProducto = resolverMontoPorProducto(persona, plan, nivel)`:
  - Si `nivel <= plan.nivelesAlcance` → `PlanActivacionNivel.montoPorProducto` de ese nivel.
  - Si `nivel > plan.nivelesAlcance` (nivel extra por rango) → `RangoNivel.montoPorProducto` donde `numeroNivelExtra = nivel - plan.nivelesAlcance` (Extra 1 = primer nivel más allá del plan). Si el rango no tiene configurado ese extra → `S/0` (no paga, no reutiliza el último nivel del plan).
- `membresiaActivaReferido()` = `fechaFin >= finPeriodoActivo`.
- `paga = (plan!=null && nivel<=alcance && membresia && monto>0)`. **Siempre crea la fila** con `paga` o `motivo` (`excede alcance / membresia no activa / sin activacion`) para pago retroactivo.
- Si paga: `saldoDinero += cantidad × montoPorProducto` + movimiento `BENEFICIO_ACTIVACION_COMPRA`.

`recalcularBeneficiosActivacion(persona, notificar)`: para cada `Beneficio` del periodo activo recalcula `plan/membresía/alcance/nuevoMonto`, `diferencia = nuevo - anterior` (o `-anterior` si deja de pagar); ajusta `saldoDinero += diferencia` (valida `>=0`), actualiza `paga/motivo` + movimiento `ACTUALIZACION_BENEFICIO_ACTIVACION`. Se ejecuta al validar compra, al activar plan y en reprocesos.

`GET /api/planes-activacion/persona/{id}/proyeccion` → `proyectarPorPersona()`: `pvActual` (último `PV` del periodo), `personasPorNivel` por BFS (`Queue NodoRed`), por plan calcula `activable=pv>=pvMinimo, faltante, totalPotencial=Σ personasNivel×montoNivel` (`Dto/PlanActivacionProyeccionResponse.java`).

### 4.5 Rangos (`Model/ServiceImpl/RangoServiceImpl.java`, `BilleteraServiceImpl.java#actualizarRangoActual`)
- CRUD ordenado por `qpMinimo`, valida `nombre obligatorio, qp>=0`. Cada rango tiene `color` (hex, default por nombre: Oro #C9A227, Plata #9AA3B2, Bronce #B0703A...).
- `rangoAlcanzadoPorQp() = max(qpMinimo <= saldoQp)`. Si cambia, guarda en `Persona.rangoActual` + notifica `TIPO_RANGO`.
- Efecto: `rango.nivelesExtra` amplía `alcanceEfectivo` de activación (4.4). El rango se recalcula en cada acreditación de QP y se resetea a 0 en el cierre mensual.
- **Rango máximo histórico:** `Persona.rangoMaximo` nunca baja (solo sube al superar `qpMinimo`). El cierre mensual resetea `rangoActual` pero conserva `rangoMaximo`. Perfil muestra insignia con el color del máximo: `Rango ORO — Lo conservas hasta superarlo`.
- **Montos propios del extra:** cada rango tiene su tabla `rangos_niveles (RangoNivel.java)`: `numeroNivelExtra 1..N + montoPorProducto`. `GET/POST /api/rangos/{id}/niveles`, `DELETE /api/rangos/niveles/{nivelId}`. Sin fila configurada el extra paga `0`.

### 4.6 Billetera, retiros y cierres (`BilleteraServiceImpl.java`, `Rescontroller/BilleteraRestController.java`, `RecompensaRestController.java`)
- `asegurarBilletera(persona)`, `GET /api/billeteras/persona/{id}` (billetera+movs+membresías+cierres), `GET .../movimientos|membresias|cierres|retiros`.
- `sincronizarSaldoProductosRecompensa()`: `objetivo = valorProductos-retirado si cobrable sino 0`; ajusta `saldoProductos` vs movimientos `RECOMPENSA_PRODUCTOS`.
- **Retiro mensual — cierre personal obligatorio** `POST /api/billeteras/persona/{id}/retiros {dinero, productos, detalle}` → `registrarRetiro()`:
  - **Solo efectivo actualmente**: si `productos>0` → error "solo efectivo nivel2+".
  - `efectivoTotal = efectivoBilleteraPeriodo(DINERO periodo) + efectivoRecompensas(nivel>=2 cobrables periodo)`.
  - Exige **`dinero == totalDisponible`** (debe retirar el total, no parcial).
  - Crea `CierreMensual PAGADA` + `Retiro PROCESADO`, descuenta primero `saldoDinero` y luego `retirarEfectivoRecompensas()` (`montoRetirado+=`), `cartera.registrarEgreso(RETIRO_BILLETERA)`, luego resta `saldoPv/Qp/Cr/Productos del periodo` + `actualizarRango`.
  - Bono nivel 1 (patrocinio directo) se retira por separado: `GET /api/recompensas/nivel-1`, `POST /api/recompensas/nivel-1/{id}/retiro`.
- **Deshacer retiro (evita cierres por error)** `POST /api/billeteras/retiros/{retiroId}/anular {motivo min. 10 caracteres}` → `anularRetiro()`: exige estado `PROCESADO` + periodo no `CERRADO`; restaura `saldoDinero` (billetera + `montoEfectivoRetirado` de recompensas), devuelve egreso a caja (`ANULACION_RETIRO_BILLETERA`), restaura `PV/QP/CR/PRODUCTOS` desde el snapshot del cierre, elimina lógicamente el `CierreMensual` y sus movimientos `CIERRE_MENSUAL`, marca retiro `ANULADO + motivo/fecha/usuario` (auditable, la persona vuelve a saldos pendientes).
- **Cierre global** `POST /api/billeteras/cierres-mensuales` → `cerrarMesBilleteras()`: por cada billetera sin cierre `YYYY-MM` crea `Cierre PENDIENTE` snapshot + movimientos `CIERRE_MENSUAL` negativos + resetea saldos a 0 + rango a 0.
- **Cerrar periodo pagado** `POST /api/billeteras/cierres-mensuales/cerrar-periodo` → `cerrarPeriodoActivoPagado()`: exige `listarBilleterasConSaldos().isEmpty()` (todos hicieron su retiro personal) + sin `recompensas nivel>=2 cobrables con saldo pendiente`; luego `cerrarMes + vencerHistorialActivas + desactivarMembresias + gestion.cerrarPeriodoActivo()`.

### 4.7 Periodos y gestiones (`Model/ServiceImpl/GestionPeriodoServiceImpl.java`, `Rescontroller/GestionPeriodoRestController.java`)
- `Gestion`: `crearGestion(anio)` (`fechaInicio 01-01, fechaFin 31-12`); `actualizarGestion()` recalcula fechas y labels ("Enero 2026").
- `PeriodoGestion`: `crearPeriodo(gestion,mes)`, `activarPeriodo()` (anterior `ACTIVO→PENDIENTE_CIERRE`, valida no `CERRADO`), `desactivar→PENDIENTE_CIERRE`, `cerrarPeriodoActivo→CERRADO`.
- `obtenerPeriodoActivo()`: **autocrea el mes actual como `ACTIVO`** si no existe. Toda compra/movimiento/historial se imputa al periodo activo.
- Endpoints: `GET /api/gestiones, POST ?anio&nombre, PUT /{gestionId}, GET /{gestionId}/periodos, POST /{gestionId}/periodos?mes&nombre, PUT /periodos/{id}, GET /periodos/activo, PUT /periodos/{id}/activar, PUT /periodos/activo/desactivar`.

### 4.8 Caja de empresa (`Model/ServiceImpl/CarteraEmpresaServiceImpl.java`, `Rescontroller/CarteraEmpresaRestController.java`)
- Caja única `asegurarCarteraPrincipal(CARTERA_PRINCIPAL)`.
- `registrarIngreso/Egreso() → registrarMovimiento()`: ignora `monto<=0` o duplicado (`existsByReferencia`), calcula `saldo +/- monto`, valida que un egreso no deje saldo negativo, guarda con `periodoActivo`.
- Ingresos: `VENTA_INTERNA, REFERIDO_AFILIACION, MEMBRESIA_ACTIVACION`. Egresos: `ANULACION_COMPRA, RETIRO_BILLETERA`.
- Endpoints: `GET /api/cartera-empresa (saldo), GET /movimientos?periodoId, GET /resumen-periodo?periodoId {saldoInicial, ingresos, egresos, saldoFinal, count}`.

### 4.9 Tienda pública (`Model/ServiceImpl/TiendaPublicaServiceImpl.java:542`, `Rescontroller/TiendaPublicaRestController.java`)
- Replica la lógica de activación por PV para `CompraPublica` (el distribuidor dueño de la tienda acumula volumen).
- Públicos: `GET /api/public/tiendas/{username}, GET .../productos, GET /api/public/tipos-cliente, GET /api/public/clientes/documento/{doc}, GET .../tiendas/{username}/clientes/documento/{doc}, POST .../compras, POST .../compras/comprobante (multipart)`.
- Privados: `GET /api/compras-publicas, GET /api/clientes-publicos, PUT /api/compras-publicas/{id}/estado, GET /api/tipos-cliente-publico + /{id}/descuentos, POST /api/productos/{id}/descuentos-cliente`.
- Precios: `precioDistribuidorUnitario vs precioPublicoUnitario - descuentoUnitario = precioFinalUnitario` (`CompraPublicaDetalle.java`).

### 4.10 Reprocesos admin (`Model/ServiceImpl/ReprocesoServiceImpl.java:339`, `Rescontroller/ReprocesoRestController.java`)
- `GET /api/reproceso/dry-run?notificar` (`REQUIRES_NEW + setRollbackOnly`, simula) vs `POST /api/reproceso?notificar` (ejecuta): analiza beneficiarios del periodo activo, clasifica `INACTIVO/DEBITO/CREDITO/OMITIDO`, solo acredita vía `recalcularBeneficiosActivacion`.
- `GET /api/reproceso/recrear-recompensas/dry-run` vs `POST /api/reproceso/recrear-recompensas?notificar`: itera `compras VALIDADA/CONFIRMADA`, `reiniciarRecompensasCompra()` (revierte beneficios, regenera con lógica vigente + `acreditarVolumenRed()` idempotente) + reconstruye `saldoPvPropio = SUM movimientos COMPRA PV por billetera`.

### 4.11 Catálogo, landings, notificaciones, asistente
- Planes: `Rescontroller/PlanRestController.java` (`GET /, GET /public, GET /{id}, GET /nombre/{nombre}, POST /, PUT /{id}, POST /{id}/niveles, DELETE /niveles/{id}, POST /{id}/productos, DELETE /productos/{id}, DELETE /{id}`). Activación: `PlanActivacionRestController.java` (+ `GET /persona/{id}/proyeccion`). Rangos, productos (`POST /{id}/imagen|imagen-publica|imagen-herramienta`), categorías: CRUD estándar.
- Landings/CMS: `ProductoLandingRestController` (`GET /api/public/productos, /api/shop/productos, /api/public/productos/{id}/landing, PUT /api/productos/{id}/landing`), `DigitalLandingRestController` (`GET /api/public/digital-landings/{slug}` + CRUD), `LoginCarouselRestController` (`GET /api/public/login-carousel` + CRUD), `UploadRestController` (`POST /api/uploads/landings multipart 5MB → /uploads/**`).
- Notificaciones: `NotificacionRestController` (`GET ?personaId, GET /no-leidas, POST /{id}/leida, POST /marcar-todas-leidas, POST /enviar` broadcast admin + FCM). Frontend: `NotificationBell.vue + notificacionesStore (poll+WS)`.
- Asistente IA: `AsistenteRestController` (`POST /api/asistente/chat, GET|PUT /api/asistente/config`) → `GeminiAsistenteServiceImpl` (`gemini.api-key/model`).
- Personas/Usuarios/Roles/Menús: CRUD en `PersonaRestController, UsuarioRestController, RolRestController, MenuSistemaRestController` (+ `GET /config` menú filtrado por rol, `PUT /permissions {rol→menus}`). Auth: `AuthRestController` (`POST /login {username,password}→{token,usuarioId,roles}, POST /impersonate/{usuarioId}, GET /me, POST /me/foto`). Logs: `LogRestController` (`GET /, GET /{fileName}`).
- Presencia: `PresenciaRestController` (`POST /api/presencia/latido {rutaActual}`, `DELETE /api/presencia/salir`, `GET /api/presencia/online?minutos=2` + `/count` solo ADMIN). Tabla `presencias_usuarios (persona unique, ultimoLatido, ip, ruta)`. Frontend envía latido cada 60s + al cambiar de ruta (`App.vue` + `presenciaService.js`); admin ve `/usuarios-en-linea` con auto-refresh 15s.

---

## 5. Flujos end-to-end (cómo funciona hoy)

### F1. Nuevo distribuidor
`Preinscripción pública (/preinscripcion-referido/:patrocinadorId)` → `POST /api/public/preinscripciones-referidos (PENDIENTE)` → admin `POST /api/preinscripciones-referidos/{id}/validar` → `POST /api/referidos?personaId&patrocinadorId&planId` (4.1: membresía hasta fin de periodo o siguiente + `Recompensas` a uplines + QP al patrocinador + ingreso a caja).

### F2. Compra y comisiones
`Shop/Cart → POST /api/compras/persona/{id} (PENDIENTE)` → admin `PUT .../estado?estado=VALIDADA (Ventanilla)` → `procesarCompraValidada` (4.3): caja + PV/QP/CR propio + posible activación por PV + PV/QP a 9 uplines + `BeneficiosActivacion` en dinero a hasta 10 uplines (solo si cada uno tiene plan activación + membresía + alcance).

### F3. Activación mensual
Vía A (compra de plan, `Wallet → POST .../activaciones`) o Vía B (automática por `saldoPvPropio`, 4.2). Al activarse se vuelven `cobrables` sus recompensas nivel ≥ 2 y se pagan retroactivamente sus beneficios pendientes.

### F4. Cierre mensual
Cada persona `POST .../retiros` (retiro total obligatorio, 4.6) → admin verifica `GET /api/billeteras/saldos` vacío → `POST /cierres-mensuales/cerrar-periodo` (cierra mes, vence historiales, desactiva membresías, cierra periodo) → nuevo periodo activo (autocreado).

### F5. Cliente público
` /tienda/:username → /carrito → POST /api/public/.../compras` → distribuidor acumula volumen (4.9).

---

## 6. Tabla de endpoints REST (27 controladores en `Rescontroller/`)

| Controlador | Base | Endpoints principales |
|---|---|---|
| `AuthRestController` | `/api/auth` | `POST /login, POST /impersonate/{usuarioId}, GET /me, POST /me/foto` |
| `PersonaRestController` | `/api/personas` | `GET /, GET /{id}, GET /documento/{doc}, POST /, PUT /{id}, DELETE /{id}` |
| `UsuarioRestController` | `/api/usuarios` | `GET /, GET /{id}, GET /username/{username}, POST /, PUT /{id}, DELETE /{id}` |
| `RolRestController` | `/api/roles` | `GET /, GET /{id}, GET /nombre/{nombre}, POST /, PUT /{id}, DELETE /{id}` |
| `MenuSistemaRestController` | `/api/menus` | `GET /, GET /config, POST /, PUT /{menuId}, DELETE /{menuId}, PUT /permissions` |
| `ReferidoRestController` | `/api/referidos` | `GET /, GET /{id}, GET /persona/{pid}, GET /patrocinador/{pid}, POST ?personaId&patrocinadorId&planId, PUT /{id}, DELETE /{id}` |
| `PlanRestController` | `/api/planes` | `GET /, GET /public, GET /{id}, GET /nombre/{n}, POST /, PUT /{id}, POST /{id}/niveles, DELETE /niveles/{nid}, POST /{id}/productos, DELETE /productos/{id}, DELETE /{id}` |
| `PlanActivacionRestController` | `/api/planes-activacion` | `GET /, GET /{id}, POST /, PUT /{id}, POST /{id}/niveles, DELETE /niveles/{nid}, DELETE /{id}, GET /persona/{pid}/proyeccion` |
| `RangoRestController` | `/api/rangos` | `GET /, GET /{id}, POST /, PUT /{id}, DELETE /{id}` |
| `ProductoRestController` | `/api/productos` | `GET /, GET /{id}, GET /sku/{sku}, POST /, PUT /{id}, POST /{id}/imagen*, DELETE /{id}` |
| `ProductoCategoriaRestController` | `/api/producto-categorias` | CRUD estándar |
| `CompraRestController` | `/api/compras` | `GET /persona/{pid}, GET /, GET /admin/todas, GET /estado/{e}, POST /persona/{pid}, POST /persona/{pid}/comprobante, PUT /{id}, GET /{id}/beneficios|movimientos, PUT /{id}/estado, POST /{id}/anular, PUT /{id}/admin-reconstruir` |
| `BilleteraRestController` | `/api/billeteras` | `GET /saldos, GET /persona/{pid}, GET /persona/{pid}/movimientos|membresias|cierres|retiros, GET /retiros, POST /cierres-mensuales, POST /cierres-mensuales/cerrar-periodo, POST /persona/{pid}/retiros, POST /persona/{pid}/activaciones` |
| `RecompensaRestController` | `/api/recompensas` | `GET /, GET /persona/{pid}, GET /nivel-1, GET /nivel-1/retiros, POST /nivel-1/{id}/retiro` |
| `GestionPeriodoRestController` | `/api/gestiones` | `GET /, POST ?anio&nombre, PUT /{gid}, GET /{gid}/periodos, POST /{gid}/periodos?mes&nombre, PUT /periodos/{pid}, GET /periodos/activo, PUT /periodos/{pid}/activar, PUT /periodos/activo/desactivar` |
| `CarteraEmpresaRestController` | `/api/cartera-empresa` | `GET /, GET /movimientos?periodoId, GET /resumen-periodo?periodoId` |
| `ReprocesoRestController` | `/api/reproceso` | `GET /dry-run, POST /, GET /recrear-recompensas/dry-run, POST /recrear-recompensas` |
| `TiendaPublicaRestController` | mixto | Ver 4.9 (rutas `/api/public/**`, `/api/compras-publicas`, `/api/clientes-publicos`, `/api/tipos-cliente-publico`) |
| `PreinscripcionReferidoRestController` | mixto | `GET /api/public/.../patrocinadores/{id}, GET .../usuario/{u}, POST /api/public/preinscripciones-referidos, GET .../usuarios/{u}/disponible, GET /api/preinscripciones-referidos, POST /{id}/validar|rechazar` |
| `ProductoLandingRestController` | mixto | `GET /api/public/productos, /api/shop/productos, /api/public/productos/{id}/landing, GET /api/productos-landings, GET|PUT /api/productos/{id}/landing` |
| `DigitalLandingRestController` | mixto | `GET /api/public/digital-landings/{slug}, GET|POST /api/digital-landings, GET|PUT|DELETE /api/digital-landings/{id}` |
| `NotificacionRestController` | `/api/notificaciones` | `GET ?personaId, GET /no-leidas, POST /{id}/leida, POST /marcar-todas-leidas, POST /enviar` |
| `DispositivoController` | `/api/dispositivos` | `POST /vincular, GET /estado, PUT /{id}/revincular` |
| `AsistenteRestController` | `/api/asistente` | `POST /chat, GET|PUT /config` |
| `LoginCarouselRestController` | mixto | `GET /api/public/login-carousel, GET|POST /api/login-carousel, PUT|DELETE /api/login-carousel/{id}` |
| `UploadRestController` | `/api/uploads` | `POST /landings (multipart 5MB)` |
| `LogRestController` | `/api/logs` | `GET /, GET /{fileName}` |

---

## 7. Frontend (`frontend/src`, 42 rutas en `main.js`)
- Públicas: `CompanyHomeView (/), ScreenLanding (/landing), Producto Publico (/producto/:productId), Herramienta (/herramienta/:slug), Referido público + PreinscripcionReferidoPublicView, PublicStoreView + PublicCartView (/tienda/:username, /carrito), Login`.
- Admin (`roles:[ADMIN]`): `RolesMenusView, LoginCarouselConfigView, AsistenteConfig, ConsultarPersona*, PersonasView, RangosView, PlansView, ActivationPlansView, ReferidosView, Inventario, AdminSalesView (/ventanilla) + RegistroReferidoView, ProductLandingConfig, DigitalLandingConfig, CompanyHomeConfig, GestionPeriodsView, CompanyWalletView, WalletWithdrawalsView (/retiros-billeteras), LevelOneWithdrawalsView (/retiros-nivel-1 bono patrocinio), LogsView, NotificacionesAdminView, StatsView`.
- Mixtas: `DashboardView, ProfileView, AssistantView (ADMIN/EMBAJADOR/USUARIO), DigitalToolsView, WalletView (+Withdrawals), ShopView/CartView (+CLIENTE), NetworkView + NetworkTreeNode.vue (árbol, ADMIN/EMBAJADOR), RewardsView (ADMIN/EMBAJADOR)`.
- Guard: si no autenticado → `/login?redirect`; sino `menuStore.loadFromBackend() + canAccessMenu` (`navigation/menuConfig.js`: 30 items base + `ROLE_ADMIN` default + `normalizeRole/hasAnyRole/getDefaultRouteName`) sino ruta por defecto.
- Stores/servicios: `authStore (vy_token/vy_usuario + FCM vincular), menuStore (vy_role_menu_permissions), notificacionesStore (poll+WS), api.js (Bearer), authService/profileService/cartService/publicCartService/productCatalogService/productLandingService/digitalLandingService/companyHomeService/asistenteService/notificacionService/logService/fcm-service, screens/admin.js + embajador-1/2/3.js + publico.js`. Vistas usan `sweetalert2, jspdf, lucide`.

---

## 8. Notas operativas
- Todo el cálculo es **idempotente** (`existsByReferenciaTipo`) y auditable vía `MovimientoBilletera` + `MovimientoCarteraEmpresa` + `GET /api/compras/{id}/movimientos`.
- Los beneficios no-pagados **se conservan** con motivo para cobrarse retroactivamente al activarse.
- El cierre exige **retiro total** (no parcial) y que **nadie tenga saldos** antes de cerrar el periodo.
- Puntos a vigilar: `MembresiaScheduler` inactivo (todo manual), seed `admin/admin123` debe cambiarse en producción (`security.jwt.secret` también), `ddl-auto=none` + `sql.init.mode=always (schema.sql)`, tests en `src/test` (`RecalcularBeneficiosActivacionUnitTest, AdminReconstruirCompraTest, ValidacionDatosRealesTest`).

---

## 9. Comportamiento del sistema en flujos reales (4 ejemplos)

> Ejemplos ilustrativos con datos ficticios para entender el comportamiento end-to-end. Montos en S/.

### Ejemplo 1. Afiliación nueva — bono de patrocinio en cascada

**Contexto inicial:**
- Red existente: `Ana (nivel 0)` → patrocinó a `Bruno (nivel 1)`.
- Ambos con `membresiaActiva=true` en periodo `Septiembre 2026`.
- Plan `Emprendedor`: `precio=500, qp=50, nivelesAlcance=9`. Niveles: `N1=100 efectivo + 80 productos, N2=50, N3=30`.

**Flujo:**
1. `Carla` se preinscribe desde `/preinscripcion-referido/:brunoId` → `POST /api/public/preinscripciones-referidos (PENDIENTE)`.
2. Admin valida → `POST /api/preinscripciones-referidos/{id}/validar` → se crea `Persona + Usuario` de Carla.
3. Admin afilia: `POST /api/referidos?personaId=Carla&patrocinadorId=Bruno&planId=Emprendedor` el día 10/09.
4. Sistema (`ReferidoServiceImpl.guardar`):
   - Crea `Referido(Carla)`: `fechaUnion=10-09, fechaInicio=01-09, fechaFin=30-09 23:59:59` (día ≤14 → fin periodo activo), `membresiaActiva=true`.
   - Crea `HistorialMembresia AFILIACION` + `CarteraEmpresa Ingreso 500 (REFERIDO_AFILIACION)`.
   - `Bruno.saldoQp += 50` + `actualizarRango` + `MovimientoBilletera TIPO_QP`.
   - `regenerarRecompensas(Carla)`: camina uplines:
     - `N1 Bruno: efectivo 100 cobrable=true + productos 80` (nivel 1 siempre cobra).
     - `N2 Ana: efectivo 50 cobrable=true` (porque Ana tiene membresía activa; si no, `cobrable=false, motivo=membresia no activa`).
   - Notifica por WS+FCM a Bruno: `Nuevo referido: Carla`.

**Resultado visible:**
- Bruno ve en `/rewards`: `+S/100 + S/80 en productos`. Su `Billetera.saldoProductos` sube a 80.
- Ana ve `+S/50` en recompensas nivel 2 (solo retirable en cierre mensual, no como nivel 1).
- Caja empresa: `+S/500`.
- Si se elimina a Bruno, sus hijos (Carla) se reasignan a Ana y se regeneran recompensas.

### Ejemplo 2. Compra interna validada — volumen + beneficio por activación

**Contexto inicial:**
- `Bruno` tiene `saldoPvPropio=0`. Plan activación `Activo Mensual`: `pvMinimo=50, nivelesAlcance=5, N1=S/5 x producto, N2=S/3, N3=S/2`.
- Producto `Shake Vida`: `precio=50, pv=10, qp=5, cr=2`.
- Red: `Ana → Bruno → Carla`.

**Flujo:**
1. Carla compra desde `/shop`: `POST /api/compras/persona/Carla {6 x Shake}` → `Compra PENDIENTE, subtotal=300, totalPv=60, totalQp=30`.
2. Admin en `/ventanilla`: `PUT /api/compras/{id}/estado?estado=VALIDADA` → `procesarCompraValidada()`:
   1. `CarteraEmpresa Ingreso 300 (VENTA_INTERNA)`.
   2. A Carla: `saldoPv+=60, saldoPvPropio+=60, saldoQp+=30, saldoCr+=12` + movimientos `COMPRA`. Chequea rango.
   3. `activarMembresiaPorPv`: como `60 >= 50`, Carla `membresiaActiva=true` + `Historial PV_ACTIVACION precio=0` + notificación.
   4. `acreditarVolumenRed` (9 niveles): a Bruno y Ana `saldoPv+=60, saldoQp+=30` con ref `COMPRA_RED` + notificación `Volumen de red nivel N`. No les suma `PvPropio` ni `CR`.
   5. `generarBeneficiosActivacion(compra, 6 productos)` (hasta 10 uplines):
     - `Bruno N1`: tiene plan activación + membresía → `paga=true, 6×5=S/30` → `saldoDinero+=30`.
     - `Ana N2`: si tiene todo → `paga=true, 6×3=S/18` → `saldoDinero+=18`.
     - Si Ana no tuviera membresía → igual crea fila `paga=false, motivo=membresia no activa` para pago retroactivo futuro.

**Resultado visible:**
- Carla: activa por PV sin pagar plan, lista para cobrar red el próximo mes.
- Bruno: `/wallet +S/30`, `/network` ve volumen de Carla.
- Auditoría: `GET /api/compras/{id}/movimientos` muestra `COMPRA + COMPRA_RED + BENEFICIO + CARTERA`.

### Ejemplo 3. Activación por compra de plan — desbloqueo retroactivo

**Contexto inicial:**
- `Diana` (downline de Bruno) hizo compras y generó 2 `BeneficioActivacionCompra` en Septiembre, pero `paga=false, motivo=sin activacion / membresia no activa`. Tiene `S/0` cobrable.
- Tiene además `Recompensa N2= S/50 cobrable=false`.

**Flujo:**
1. Diana entra a `/wallet` → `POST /api/billeteras/persona/Diana/activaciones {planId: Mensual 150}` → `registrarActivacion()`:
   - `Referido.plan=nuevo, fechaInicio=01-09, fechaFin=30-09 23:59:59, membresia=true`.
   - `Historial ACTIVACION precio=150` + `Cartera Ingreso 150 (MEMBRESIA_ACTIVACION)`.
   - `saldoQp += qpPlan (ej. 20)` + `actualizarRango`.
   - `actualizarRecompensasCobrables(true)`: su `N2 S/50` pasa a `cobrable=true`.
   - `recalcularBeneficiosActivacion(Diana)`: recalcula sus 2 beneficios pendientes: `diferencia=nuevo-anterior`, `saldoDinero+=diferencia` + movimiento `ACTUALIZACION_BENEFICIO_ACTIVACION`. Ej. recupera `S/24 + S/18 = S/42`.

**Resultado visible:**
- Diana pasa de `S/0` a `S/42 + S/50 desbloqueado` sin nueva compra.
- Lo mismo ocurre automático si Diana hubiera llegado a `pvMinimo` solo con `saldoPvPropio` (Vía B), pero en ese caso sin ingreso a caja (`precio=0`).
- En `/planes-activacion/persona/Diana/proyeccion` ahora ve `activable=true, totalPotencial` calculado por BFS de su red.

### Ejemplo 4. Cierre mensual — retiro total obligatorio

**Contexto inicial (30/09 fin de mes):**
- `Elena`: `saldoDinero=120 (80 beneficios + 40 recompensas N>=2), saldoPv=200, saldoQp=100, saldoProductos=0`.
- `Luis`: ya retiró, saldos en 0.
- Periodo activo: `Septiembre 2026`.

**Flujo:**
1. Elena intenta `POST /api/billeteras/persona/Elena/retiros {dinero:50}` → **error**: `debe retirar el total (120), no parcial`.
2. Reintenta con `{dinero:120}` → `registrarRetiro()`:
   - Calcula `efectivoTotal = 80 (DINERO periodo) + 40 (recompensas N>=2 cobrables) = 120`, valida igualdad.
   - Crea `CierreMensual PAGADA snapshot {120,200,100...}` + `Retiro PROCESADO`.
   - Descuenta `saldoDinero→0`, marca `montoRetirado` en recompensas, `Cartera Egreso 120 (RETIRO_BILLETERA)`, luego resetea `Pv/Qp/Cr/Productos del periodo →0` + rango a 0.
   - Bono N1 (ej. `S/100 directos`) **no** entra aquí; se retira aparte en `/retiros-nivel-1`.
3. Admin verifica `GET /api/billeteras/saldos` → vacío (todos en 0).
4. Admin ejecuta `POST /api/billeteras/cierres-mensuales/cerrar-periodo`:
   - Si alguien tuviera saldo o recompensa N>=2 pendiente → **falla** y no cierra.
   - Si todo OK: `vencerHistorial + desactivarMembresias + cerrarPeriodo SEPTIEMBRE=CERRADO`. Próxima compra autocrea `Octubre 2026 ACTIVO`.
5. Si admin hubiera usado `POST /cierres-mensuales` directo (sin retiros), crearía `Cierre PENDIENTE` y resetearía saldos a 0 sin pago — solo para forzado.

**Resultado visible:**
- Elena en `/wallet` ve saldos en 0 + historial `Retiro Septiembre S/120 PAGADA`.
- Empresa en `/caja-empresa/resumen-periodo?periodoId=Septiembre`: `ingresos - egresos = saldoFinal`.
- En Octubre todos empiezan en 0 y con `membresiaActiva=false` hasta reactivarse (Ejemplo 2 o 3).
