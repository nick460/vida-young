package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Dto.Asistente.AsistenteChatRequest;
import com.vidayoung.platform.Model.Dao.BeneficioActivacionCompraDao;
import com.vidayoung.platform.Model.Dao.CierreMensualBilleteraDao;
import com.vidayoung.platform.Model.Dao.CompraDao;
import com.vidayoung.platform.Model.Dao.MovimientoBilleteraDao;
import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.RecompensaDao;
import com.vidayoung.platform.Model.Dao.ReferidoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.BeneficioActivacionCompra;
import com.vidayoung.platform.Model.Entity.CierreMensualBilletera;
import com.vidayoung.platform.Model.Entity.Compra;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Recompensa;
import com.vidayoung.platform.Model.Entity.Referido;
import com.vidayoung.platform.Model.Service.AsistenteService;
import com.vidayoung.platform.Model.Service.GestionPeriodoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Estadisticas personales de red para /stats: la persona analiza mes a mes
 * como le va yendo (aportantes, inactivos, ingresos por brazo/rama, evolucion).
 */
@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
public class EstadisticaRedRestController {

    private final PersonaDao personaDao;
    private final ReferidoDao referidoDao;
    private final RecompensaDao recompensaDao;
    private final GestionPeriodoService gestionPeriodoService;
    private final MovimientoBilleteraDao movimientoBilleteraDao;
    private final CierreMensualBilleteraDao cierreMensualBilleteraDao;
    private final CompraDao compraDao;
    private final BeneficioActivacionCompraDao beneficioActivacionCompraDao;
    private final AsistenteService asistenteService;

    @GetMapping("/mi-red")
    public ResponseEntity<MiRedResponse> miRed(
            @RequestParam Long personaId,
            @RequestParam(required = false) Long periodoId
    ) {
        Persona persona = personaDao.findById(personaId).orElse(null);
        if (persona == null) {
            return ResponseEntity.notFound().build();
        }
        PeriodoGestion periodoActivo = gestionPeriodoService.buscarPeriodoActivo().orElse(null);
        PeriodoGestion periodo = periodoId == null
                ? periodoActivo
                : gestionPeriodoService.buscarPorId(periodoId);
        if (periodo == null) {
            return ResponseEntity.notFound().build();
        }

        // Arbol de red: mapa patrocinadorId -> referidos hijos (solo ACTIVOS en BD).
        List<Referido> todos = referidoDao.findAll().stream()
                .filter(r -> Auditoria.ESTADO_ACTIVO.equals(r.getEstado()))
                .toList();
        Map<Long, List<Referido>> hijosPorPatrocinador = new HashMap<>();
        Map<Long, Referido> referidoPorPersona = new HashMap<>();
        for (Referido referido : todos) {
            if (referido.getPersona() != null && referido.getPersona().getId() != null) {
                referidoPorPersona.put(referido.getPersona().getId(), referido);
            }
            Long patrocinadorId = referido.getPatrocinador() == null ? null : referido.getPatrocinador().getId();
            if (patrocinadorId != null) {
                hijosPorPatrocinador.computeIfAbsent(patrocinadorId, key -> new ArrayList<>()).add(referido);
            }
        }

        // Descendencia completa (BFS) con nivel de profundidad.
        List<NodoRed> nodos = new ArrayList<>();
        Map<Long, Integer> nivelPorPersona = new HashMap<>();
        ArrayList<Long> cola = new ArrayList<>();
        cola.add(personaId);
        nivelPorPersona.put(personaId, 0);
        Set<Long> visitados = new HashSet<>();
        visitados.add(personaId);
        int indice = 0;
        while (indice < cola.size()) {
            Long actual = cola.get(indice++);
            int nivelActual = nivelPorPersona.getOrDefault(actual, 0);
            for (Referido hijo : hijosPorPatrocinador.getOrDefault(actual, List.of())) {
                Long hijoPersonaId = hijo.getPersona() == null ? null : hijo.getPersona().getId();
                if (hijoPersonaId == null || !visitados.add(hijoPersonaId)) {
                    continue;
                }
                nivelPorPersona.put(hijoPersonaId, nivelActual + 1);
                nodos.add(new NodoRed(hijo, nivelActual + 1));
                cola.add(hijoPersonaId);
            }
        }

        // Recompensas del periodo que me genera mi red (bruto, sin descontar retiros).
        List<Recompensa> recompensasPeriodo = recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(r -> Auditoria.ESTADO_ACTIVO.equals(r.getEstado()))
                .filter(r -> r.getPeriodo() != null && periodo.getId().equals(r.getPeriodo().getId()))
                .toList();
        Map<Long, BigDecimal> aportePorPersona = new HashMap<>();
        Map<Integer, BigDecimal> montoPorNivel = new HashMap<>();
        BigDecimal totalEfectivo = BigDecimal.ZERO;
        BigDecimal totalProductos = BigDecimal.ZERO;
        for (Recompensa recompensa : recompensasPeriodo) {
            BigDecimal monto = zeroIfNull(recompensa.getMontoEfectivo());
            BigDecimal productos = zeroIfNull(recompensa.getValorProductos());
            totalEfectivo = totalEfectivo.add(monto);
            totalProductos = totalProductos.add(productos);
            montoPorNivel.merge(recompensa.getNivelGenerado() == null ? 0 : recompensa.getNivelGenerado(),
                    monto, BigDecimal::add);
            Long origenId = recompensa.getReferido() == null || recompensa.getReferido().getPersona() == null
                    ? null
                    : recompensa.getReferido().getPersona().getId();
            if (origenId != null) {
                aportePorPersona.merge(origenId, monto.add(productos), BigDecimal::add);
            }
        }

        // Miembros con su aporte y estado en el periodo.
        List<MiembroAporte> miembros = new ArrayList<>();
        int activos = 0;
        for (NodoRed nodo : nodos) {
            Referido referido = nodo.referido();
            Persona miembro = referido.getPersona();
            boolean activo = activoEnPeriodo(referido, periodo);
            if (activo) {
                activos++;
            }
            BigDecimal aporte = aportePorPersona.getOrDefault(miembro.getId(), BigDecimal.ZERO);
            miembros.add(new MiembroAporte(
                    miembro.getId(),
                    nombreCompleto(miembro),
                    miembro.getDocumento(),
                    nodo.nivel(),
                    activo,
                    aporte,
                    referido.getFechaUnion()
            ));
        }
        int totalRed = miembros.size();
        long directos = miembros.stream().filter(m -> m.getNivel() == 1).count();
        long inactivos = totalRed - activos;
        double pctActivos = totalRed == 0 ? 0 : (activos * 100.0 / totalRed);

        miembros.sort(Comparator.comparing(MiembroAporte::getAporte).reversed());
        List<MiembroAporte> topAportantes = miembros.stream().limit(5).toList();
        List<MiembroAporte> menosAportan = miembros.stream()
                .sorted(Comparator.comparing(MiembroAporte::getAporte))
                .limit(5)
                .toList();
        List<MiembroAporte> inactivosLista = miembros.stream()
                .filter(m -> !m.isActivo())
                .sorted(Comparator.comparing(MiembroAporte::getNombre))
                .limit(20)
                .toList();

        // Ventas de la red: compras del periodo (no anulan) agrupadas por comprador.
        // Son las que generan el volumen COMPRA_RED + recompensas de /wallet.
        List<Compra> comprasPeriodo = compraDao.findByPeriodoIdOrderByFechaCompraDesc(periodo.getId()).stream()
                .filter(c -> Auditoria.ESTADO_ACTIVO.equals(c.getEstado()))
                .filter(c -> !Compra.ESTADO_COMPRA_ANULADA.equals(c.getEstadoCompra()))
                .toList();
        Map<Long, List<Compra>> comprasPorPersona = new HashMap<>();
        for (Compra compra : comprasPeriodo) {
            Long compradorId = compra.getPersona() == null ? null : compra.getPersona().getId();
            if (compradorId != null) {
                comprasPorPersona.computeIfAbsent(compradorId, key -> new ArrayList<>()).add(compra);
            }
        }
        Map<Long, VentasMiembro> ventasPorPersona = new HashMap<>();
        for (NodoRed nodo : nodos) {
            Long miembroId = nodo.referido().getPersona() == null ? null : nodo.referido().getPersona().getId();
            if (miembroId == null) {
                continue;
            }
            List<Compra> compras = comprasPorPersona.getOrDefault(miembroId, List.of());
            ventasPorPersona.put(miembroId, ventasDe(nodo.referido(), nodo.nivel(), compras, periodo));
        }
        List<VentasMiembro> ventasRed = new ArrayList<>(ventasPorPersona.values());
        List<Compra> misComprasList = comprasPorPersona.getOrDefault(personaId, List.of());
        BigDecimal miMontoCompras = sumarSubtotal(misComprasList);
        BigDecimal miPvCompras = sumarPv(misComprasList);
        BigDecimal montoComprasRed = ventasRed.stream().map(VentasMiembro::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pvComprasRed = ventasRed.stream().map(VentasMiembro::getPv).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal qpComprasRed = ventasRed.stream().map(VentasMiembro::getQp).reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalComprasRed = ventasRed.stream().mapToLong(VentasMiembro::getCompras).sum();
        ResumenVentas resumenVentas = new ResumenVentas(
                totalComprasRed,
                montoComprasRed,
                pvComprasRed,
                qpComprasRed,
                misComprasList.size(),
                miMontoCompras,
                miPvCompras
        );
        List<VentasMiembro> topVendedores = ventasRed.stream()
                .filter(v -> v.getCompras() > 0)
                .sorted(Comparator.comparing(VentasMiembro::getMonto).reversed())
                .limit(5)
                .toList();

        // Split afiliacion vs ventas:
        // - Afiliacion = recompensas del periodo (nacen cuando ingresan nuevas personas).
        // - Ventas = beneficios de activacion pagados (dinero por compras de la red).
        List<BeneficioActivacionCompra> beneficiosPeriodo = beneficioActivacionCompraDao
                .findByBeneficiarioIdAndPeriodoId(personaId, periodo.getId()).stream()
                .filter(b -> Auditoria.ESTADO_ACTIVO.equals(b.getEstado()))
                .filter(b -> Boolean.TRUE.equals(b.getPaga()))
                .toList();
        BigDecimal beneficiosVentasMonto = beneficiosPeriodo.stream()
                .map(BeneficioActivacionCompra::getMontoTotal)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal afiliacionMonto = totalEfectivo.add(totalProductos);
        IngresosSplit split = new IngresosSplit(
                recompensasPeriodo.size(),
                afiliacionMonto,
                beneficiosPeriodo.size(),
                beneficiosVentasMonto
        );
        // Beneficios por brazo segun el comprador que origino cada beneficio.
        Map<Long, BigDecimal> beneficiosPorRaiz = new HashMap<>();
        for (BeneficioActivacionCompra beneficio : beneficiosPeriodo) {
            Long compradorId = beneficio.getCompra() == null || beneficio.getCompra().getPersona() == null
                    ? null
                    : beneficio.getCompra().getPersona().getId();
            Long raiz = raizDeBrazo(compradorId, personaId, referidoPorPersona);
            if (raiz != null) {
                beneficiosPorRaiz.merge(raiz, zeroIfNull(beneficio.getMontoTotal()), BigDecimal::add);
            }
        }

        // Ingresos por brazo: cada directo es un brazo con todo su subtree.
        List<BrazoStats> brazos = new ArrayList<>();
        for (Referido directo : hijosPorPatrocinador.getOrDefault(personaId, List.of())) {
            Long raizId = directo.getPersona() == null ? null : directo.getPersona().getId();
            if (raizId == null) {
                continue;
            }
            Set<Long> subtree = new HashSet<>();
            ArrayList<Long> pendientes = new ArrayList<>();
            pendientes.add(raizId);
            subtree.add(raizId);
            int pos = 0;
            while (pos < pendientes.size()) {
                Long actual = pendientes.get(pos++);
                for (Referido hijo : hijosPorPatrocinador.getOrDefault(actual, List.of())) {
                    Long hijoId = hijo.getPersona() == null ? null : hijo.getPersona().getId();
                    if (hijoId != null && subtree.add(hijoId)) {
                        pendientes.add(hijoId);
                    }
                }
            }
            BigDecimal montoBrazo = BigDecimal.ZERO;
            int miembrosBrazo = 0;
            int activosBrazo = 0;
            for (MiembroAporte miembro : miembros) {
                if (subtree.contains(miembro.getPersonaId())) {
                    miembrosBrazo++;
                    if (miembro.isActivo()) {
                        activosBrazo++;
                    }
                    montoBrazo = montoBrazo.add(miembro.getAporte());
                }
            }
            Persona raiz = directo.getPersona();
            brazos.add(new BrazoStats(
                    raizId,
                    nombreCompleto(raiz),
                    miembrosBrazo,
                    activosBrazo,
                    montoBrazo
            ));
        }
        brazos.sort(Comparator.comparing(BrazoStats::getMonto).reversed());

        // Ventas por brazo: mismo subtree, agregando compras de sus miembros.
        List<VentasBrazo> ventasBrazos = new ArrayList<>();
        for (Referido directo : hijosPorPatrocinador.getOrDefault(personaId, List.of())) {
            Long raizId = directo.getPersona() == null ? null : directo.getPersona().getId();
            if (raizId == null) {
                continue;
            }
            Set<Long> subtree = new HashSet<>();
            ArrayList<Long> pendientes = new ArrayList<>();
            pendientes.add(raizId);
            subtree.add(raizId);
            int pos = 0;
            while (pos < pendientes.size()) {
                Long actual = pendientes.get(pos++);
                for (Referido hijo : hijosPorPatrocinador.getOrDefault(actual, List.of())) {
                    Long hijoId = hijo.getPersona() == null ? null : hijo.getPersona().getId();
                    if (hijoId != null && subtree.add(hijoId)) {
                        pendientes.add(hijoId);
                    }
                }
            }
            long comprasBrazo = 0;
            BigDecimal montoBrazo = BigDecimal.ZERO;
            BigDecimal pvBrazo = BigDecimal.ZERO;
            for (Long miembroId : subtree) {
                VentasMiembro ventas = ventasPorPersona.get(miembroId);
                if (ventas != null) {
                    comprasBrazo += ventas.getCompras();
                    montoBrazo = montoBrazo.add(ventas.getMonto());
                    pvBrazo = pvBrazo.add(ventas.getPv());
                }
            }
            ventasBrazos.add(new VentasBrazo(
                    raizId,
                    nombreCompleto(directo.getPersona()),
                    (int) comprasBrazo,
                    montoBrazo,
                    pvBrazo,
                    beneficiosPorRaiz.getOrDefault(raizId, BigDecimal.ZERO)
            ));
        }
        ventasBrazos.sort(Comparator.comparing(VentasBrazo::getMonto).reversed());

        // Distribucion por nivel (para torta/barras).
        Map<Integer, Long> conteoPorNivel = new LinkedHashMap<>();
        Map<Integer, BigDecimal> aportePorNivel = new LinkedHashMap<>();
        for (NodoRed nodo : nodos) {
            conteoPorNivel.merge(nodo.nivel(), 1L, Long::sum);
        }
        for (MiembroAporte miembro : miembros) {
            aportePorNivel.merge(miembro.getNivel(), miembro.getAporte(), BigDecimal::add);
        }
        List<NivelStats> porNivel = conteoPorNivel.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new NivelStats(
                        entry.getKey(),
                        entry.getValue(),
                        aportePorNivel.getOrDefault(entry.getKey(), BigDecimal.ZERO),
                        montoPorNivel.getOrDefault(entry.getKey(), BigDecimal.ZERO)
                ))
                .toList();

        // Evolucion: ultimos 6 periodos con recompensa o compras en la red (por fechaInicio).
        Map<Long, BigDecimal> montoPorPeriodoId = new HashMap<>();
        Map<Long, BigDecimal> comprasPorPeriodoId = new HashMap<>();
        Map<Long, PeriodoGestion> periodoPorId = new HashMap<>();
        recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(r -> Auditoria.ESTADO_ACTIVO.equals(r.getEstado()))
                .filter(r -> r.getPeriodo() != null && r.getPeriodo().getId() != null)
                .forEach(r -> {
                    Long pid = r.getPeriodo().getId();
                    periodoPorId.putIfAbsent(pid, r.getPeriodo());
                    montoPorPeriodoId.merge(pid,
                            zeroIfNull(r.getMontoEfectivo()).add(zeroIfNull(r.getValorProductos())),
                            BigDecimal::add);
                });
        // Compras de toda la red por periodo (para la evolucion de ventas).
        for (Compra compra : compraDao.findAll().stream()
                .filter(c -> Auditoria.ESTADO_ACTIVO.equals(c.getEstado()))
                .filter(c -> !Compra.ESTADO_COMPRA_ANULADA.equals(c.getEstadoCompra()))
                .filter(c -> c.getPeriodo() != null && c.getPeriodo().getId() != null)
                .filter(c -> c.getPersona() != null && c.getPersona().getId() != null)
                .filter(c -> !personaId.equals(c.getPersona().getId()))
                .filter(c -> nivelPorPersona.containsKey(c.getPersona().getId()))
                .toList()) {
            Long pid = compra.getPeriodo().getId();
            periodoPorId.putIfAbsent(pid, compra.getPeriodo());
            comprasPorPeriodoId.merge(pid, zeroIfNull(compra.getSubtotal()), BigDecimal::add);
        }
        List<PuntoEvolucion> evolucion = periodoPorId.values().stream()
                .sorted(Comparator.comparing(PeriodoGestion::getFechaInicio))
                .map(p -> new PuntoEvolucion(
                        p.getId(),
                        p.getNombre(),
                        p.getGestion() == null ? null : p.getGestion().getAnio(),
                        p.getMes(),
                        montoPorPeriodoId.getOrDefault(p.getId(), BigDecimal.ZERO),
                        comprasPorPeriodoId.getOrDefault(p.getId(), BigDecimal.ZERO)
                ))
                .toList();
        if (evolucion.size() > 6) {
            evolucion = evolucion.subList(evolucion.size() - 6, evolucion.size());
        }

        ResumenRed resumen = new ResumenRed(
                totalRed,
                activos,
                (int) inactivos,
                (int) directos,
                nodos.stream().mapToInt(NodoRed::nivel).max().orElse(0),
                pctActivos,
                totalEfectivo,
                totalProductos,
                totalEfectivo.add(totalProductos)
        );

        // Conciliacion con /wallet: mismos criterios que BilleteraRestController para
        // que movimientos y recompensas coincidan entre ambas vistas.
        ConciliacionWallet conciliacion = conciliacionWallet(personaId, periodo, periodoActivo);

        return ResponseEntity.ok(new MiRedResponse(
                personaId,
                periodo.getId(),
                periodo.getNombre(),
                periodo.getGestion() == null ? null : periodo.getGestion().getAnio(),
                resumen,
                brazos,
                porNivel,
                topAportantes,
                menosAportan,
                inactivosLista,
                evolucion,
                conciliacion,
                resumenVentas,
                ventasBrazos,
                topVendedores,
                split
        ));
    }

    @PostMapping("/mi-red/analisis")
    public ResponseEntity<AnalisisResponse> analizarMiRed(
            @RequestParam Long personaId,
            @RequestParam(required = false) Long periodoId
    ) {
        ResponseEntity<MiRedResponse> respuesta = miRed(personaId, periodoId);
        MiRedResponse datos = respuesta.getBody();
        if (datos == null) {
            return ResponseEntity.notFound().build();
        }
        String prompt = construirPromptAnalisis(datos);
        AsistenteChatRequest chatRequest = new AsistenteChatRequest();
        chatRequest.setMessage(prompt);
        String informe = asistenteService.enviarMensaje(chatRequest).getText();
        return ResponseEntity.ok(new AnalisisResponse(
                datos.getPeriodoId(),
                datos.getPeriodoNombre(),
                informe
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> manejarErrorServicio(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(exception.getMessage());
    }

    /**
     * Replica los criterios de /wallet (BilleteraRestController): dinero de
     * movimientos del periodo (o snapshot de cierre si el mes ya cerro),
     * recompensas nivel 2+ y nivel 1 con sus conteos.
     */
    private ConciliacionWallet conciliacionWallet(Long personaId, PeriodoGestion periodo, PeriodoGestion periodoActivo) {
        List<MovimientoBilletera> movimientos = periodo == null || periodo.getId() == null
                ? List.of()
                : movimientoBilleteraDao.findByBilleteraPersonaIdAndPeriodoIdOrderByFechaRegistroDesc(personaId, periodo.getId()).stream()
                        .filter(m -> Auditoria.ESTADO_ACTIVO.equals(m.getEstado()))
                        .filter(m -> !esDeCompraAnulada(m))
                        .toList();
        boolean esHistorico = periodo != null && periodoActivo != null
                && periodo.getId() != null
                && !periodo.getId().equals(periodoActivo.getId());
        CierreMensualBilletera cierre = esHistorico ? buscarCierrePersonaPeriodo(personaId, periodo) : null;
        BigDecimal dineroBilletera;
        BigDecimal pvWallet;
        BigDecimal qpWallet;
        BigDecimal crWallet;
        BigDecimal productosWallet;
        if (cierre != null) {
            dineroBilletera = zeroIfNull(cierre.getSaldoDinero()).max(BigDecimal.ZERO);
            pvWallet = zeroIfNull(cierre.getSaldoPv()).max(BigDecimal.ZERO);
            qpWallet = zeroIfNull(cierre.getSaldoQp()).max(BigDecimal.ZERO);
            crWallet = zeroIfNull(cierre.getSaldoCr()).max(BigDecimal.ZERO);
            productosWallet = zeroIfNull(cierre.getSaldoProductos()).max(BigDecimal.ZERO);
        } else {
            dineroBilletera = totalMovimientoPorTipo(movimientos, MovimientoBilletera.TIPO_DINERO);
            pvWallet = totalMovimientoPorTipo(movimientos, MovimientoBilletera.TIPO_PV);
            qpWallet = totalMovimientoPorTipo(movimientos, MovimientoBilletera.TIPO_QP);
            crWallet = totalMovimientoPorTipo(movimientos, MovimientoBilletera.TIPO_CR);
            productosWallet = totalMovimientoPorTipo(movimientos, MovimientoBilletera.TIPO_PRODUCTOS);
        }

        List<Recompensa> recompensas = recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(r -> Auditoria.ESTADO_ACTIVO.equals(r.getEstado()))
                .filter(r -> periodo == null || r.getPeriodo() != null && periodo.getId().equals(r.getPeriodo().getId()))
                .toList();

        List<Recompensa> nivel2;
        List<Recompensa> nivel1;
        BigDecimal montoNivel2;
        BigDecimal efectivoNivel1;
        BigDecimal productosNivel1;
        if (esHistorico) {
            nivel2 = recompensas.stream()
                    .filter(r -> Optional.ofNullable(r.getNivelGenerado()).orElse(0) >= 2)
                    .filter(r -> zeroIfNull(r.getMontoEfectivo()).compareTo(BigDecimal.ZERO) > 0)
                    .toList();
            montoNivel2 = nivel2.stream()
                    .map(r -> zeroIfNull(r.getMontoEfectivo()).max(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            nivel1 = recompensas.stream()
                    .filter(r -> Optional.ofNullable(r.getNivelGenerado()).orElse(0) == 1)
                    .filter(r -> zeroIfNull(r.getMontoEfectivo()).compareTo(BigDecimal.ZERO) > 0
                            || zeroIfNull(r.getValorProductos()).compareTo(BigDecimal.ZERO) > 0)
                    .toList();
            efectivoNivel1 = nivel1.stream()
                    .map(r -> zeroIfNull(r.getMontoEfectivo()).max(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            productosNivel1 = nivel1.stream()
                    .map(r -> zeroIfNull(r.getValorProductos()).max(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            nivel2 = recompensas.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getCobrable()))
                    .filter(r -> Optional.ofNullable(r.getNivelGenerado()).orElse(0) >= 2)
                    .filter(r -> efectivoDisponible(r).compareTo(BigDecimal.ZERO) > 0)
                    .toList();
            montoNivel2 = nivel2.stream()
                    .map(this::efectivoDisponible)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            nivel1 = recompensas.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getCobrable()))
                    .filter(r -> Optional.ofNullable(r.getNivelGenerado()).orElse(0) == 1)
                    .filter(r -> efectivoDisponible(r).compareTo(BigDecimal.ZERO) > 0
                            || zeroIfNull(r.getValorProductos()).subtract(zeroIfNull(r.getValorProductosRetirado())).compareTo(BigDecimal.ZERO) > 0)
                    .toList();
            efectivoNivel1 = nivel1.stream()
                    .map(this::efectivoDisponible)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            productosNivel1 = nivel1.stream()
                    .map(r -> zeroIfNull(r.getValorProductos()).subtract(zeroIfNull(r.getValorProductosRetirado())).max(BigDecimal.ZERO))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return new ConciliacionWallet(
                movimientos.size(),
                dineroBilletera,
                nivel2.size(),
                montoNivel2,
                dineroBilletera.add(montoNivel2),
                nivel1.size(),
                efectivoNivel1,
                productosNivel1,
                pvWallet,
                qpWallet,
                crWallet,
                productosWallet
        );
    }

    /** Sube por la cadena de patrocinadores hasta el directo (brazo) que cuelga de personaId. */
    private Long raizDeBrazo(Long compradorId, Long personaId, Map<Long, Referido> referidoPorPersona) {
        if (compradorId == null || personaId == null || compradorId.equals(personaId)) {
            return null;
        }
        Long actual = compradorId;
        Long raiz = compradorId;
        Set<Long> visitados = new HashSet<>();
        while (actual != null && !actual.equals(personaId) && visitados.add(actual)) {
            raiz = actual;
            Referido referido = referidoPorPersona.get(actual);
            actual = referido == null || referido.getPatrocinador() == null
                    ? null
                    : referido.getPatrocinador().getId();
        }
        return personaId.equals(actual) ? raiz : null;
    }

    private BigDecimal totalMovimientoPorTipo(List<MovimientoBilletera> movimientos, String tipo) {
        return movimientos.stream()
                .filter(m -> tipo.equals(m.getTipo()))
                .map(MovimientoBilletera::getMonto)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .max(BigDecimal.ZERO);
    }

    private VentasMiembro ventasDe(Referido referido, int nivel, List<Compra> compras, PeriodoGestion periodo) {
        Persona miembro = referido.getPersona();
        return new VentasMiembro(
                miembro == null ? null : miembro.getId(),
                nombreCompleto(miembro),
                nivel,
                activoEnPeriodo(referido, periodo),
                compras.size(),
                sumarSubtotal(compras),
                sumarPv(compras),
                compras.stream().map(Compra::getTotalQp).map(this::zeroIfNull).reduce(BigDecimal.ZERO, BigDecimal::add)
        );
    }

    private BigDecimal sumarSubtotal(List<Compra> compras) {
        return compras.stream().map(Compra::getSubtotal).map(this::zeroIfNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumarPv(List<Compra> compras) {
        return compras.stream().map(Compra::getTotalPv).map(this::zeroIfNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal efectivoDisponible(Recompensa recompensa) {
        return zeroIfNull(recompensa.getMontoEfectivo())
                .subtract(zeroIfNull(recompensa.getMontoEfectivoRetirado()))
                .max(BigDecimal.ZERO);
    }

    private CierreMensualBilletera buscarCierrePersonaPeriodo(Long personaId, PeriodoGestion periodo) {
        if (personaId == null || periodo == null || periodo.getId() == null) {
            return null;
        }
        String key = periodoKey(periodo);
        return cierreMensualBilleteraDao.findByPersonaIdOrderByPeriodoDesc(personaId).stream()
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .filter(item -> (item.getPeriodoGestion() != null && periodo.getId().equals(item.getPeriodoGestion().getId()))
                        || (key != null && key.equals(item.getPeriodo())))
                .findFirst()
                .orElse(null);
    }

    private String periodoKey(PeriodoGestion periodo) {
        if (periodo == null || periodo.getGestion() == null || periodo.getGestion().getAnio() == null || periodo.getMes() == null) {
            return null;
        }
        return periodo.getGestion().getAnio() + "-" + String.format("%02d", periodo.getMes());
    }

    private boolean esDeCompraAnulada(MovimientoBilletera movimiento) {
        String tipo = movimiento.getReferenciaTipo();
        Long refId = movimiento.getReferenciaId();
        if (refId == null || tipo == null) {
            return false;
        }
        if ("COMPRA".equals(tipo) || "ANULACION_COMPRA".equals(tipo) || "COMPRA_BONO_REFERIDO".equals(tipo) || "COMPRA_RED".equals(tipo)) {
            return compraDao.findById(refId)
                    .map(c -> Compra.ESTADO_COMPRA_ANULADA.equals(c.getEstadoCompra()))
                    .orElse(false);
        }
        if ("BENEFICIO_ACTIVACION_COMPRA".equals(tipo) || "ACTUALIZACION_BENEFICIO_ACTIVACION".equals(tipo) || "ANULACION_BENEFICIO_COMPRA".equals(tipo)) {
            return beneficioActivacionCompraDao.findById(refId)
                    .map(b -> b.getCompra() != null && Compra.ESTADO_COMPRA_ANULADA.equals(b.getCompra().getEstadoCompra()))
                    .orElse(false);
        }
        return false;
    }

    private String construirPromptAnalisis(MiRedResponse datos) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Eres un coach de negocios multinivel de Vida Young. Analiza las estadisticas mensuales de la red de un embajador ");
        prompt.append("y devuelve un informe en espanol, claro y motivador, con este formato exacto:\n");
        prompt.append("1) Resumen del mes (3-4 lineas con cifras).\n");
        prompt.append("2) Ingresos por afiliacion: diagnostico (cuantas recompensas, cuanto dinero, tendencia) + QUE HACER en afiliacion (2-3 acciones concretas: a quienes contactar, como reactivar inactivos para afiliar, que brazos empujar).\n");
        prompt.append("3) Ingresos por ventas/compras: diagnostico (compras de la red en Bs. y PV, beneficios pagados, brazos que mas/menos venden, top vendedores) + QUE HACER en ventas (2-3 acciones concretas: que brazos y vendedores impulsar, como subir recompras y ticket).\n");
        prompt.append("4) Alertas (2-3 bullets: brazos debiles, inactivos, concentracion de ingresos en pocas personas).\n");
        prompt.append("5) Plan del proximo mes: las 3 prioridades ordenadas (1 de afiliacion, 1 de ventas, 1 de equipo).\n");
        prompt.append("Obligatorio: las secciones 2 y 3 deben existir separadas con sus cifras y sus acciones. Si un origen esta en cero, dilo y recomienda como activarlo.\n");
        prompt.append("Datos del periodo ").append(datos.getPeriodoNombre()).append(":\n");
        ResumenRed resumen = datos.getResumen();
        prompt.append("- Red total: ").append(resumen.getTotalRed())
                .append(" (activos ").append(resumen.getActivos())
                .append(", inactivos ").append(resumen.getInactivos())
                .append(", directos ").append(resumen.getDirectos())
                .append(", profundidad ").append(resumen.getProfundidad()).append(" niveles).\n");
        prompt.append("- Ingreso total red Bs. ").append(resumen.getTotal())
                .append(" (efectivo ").append(resumen.getTotalEfectivo())
                .append(", productos ").append(resumen.getTotalProductos()).append(").\n");
        ConciliacionWallet wallet = datos.getConciliacionWallet();
        if (wallet != null) {
            prompt.append("- Wallet del mes (todos sus datos): ").append(wallet.getMovimientosCount())
                    .append(" movimientos, dinero billetera Bs. ").append(wallet.getDineroBilletera())
                    .append(", ").append(wallet.getRecompensasNivel2Count()).append(" recompensas N2+ por Bs. ").append(wallet.getRecompensasNivel2Monto())
                    .append(", total efectivo Bs. ").append(wallet.getTotalEfectivoWallet())
                    .append(", PV ").append(wallet.getPvWallet())
                    .append(", QP ").append(wallet.getQpWallet())
                    .append(", CR ").append(wallet.getCrWallet())
                    .append(", productos Bs. ").append(wallet.getProductosWallet())
                    .append(", nivel 1: ").append(wallet.getNivel1Count())
                    .append(" por Bs. ").append(wallet.getNivel1Efectivo()).append(" + prod. ").append(wallet.getNivel1Productos()).append(".\n");
        }
        ResumenVentas ventas = datos.getResumenVentas();
        if (ventas != null) {
            prompt.append("- Ventas de la red en el mes: ").append(ventas.getTotalComprasRed())
                    .append(" compras por Bs. ").append(ventas.getMontoComprasRed())
                    .append(" (PV ").append(ventas.getPvComprasRed()).append(", QP ").append(ventas.getQpComprasRed()).append("). ")
                    .append("Mis compras propias: ").append(ventas.getMisCompras())
                    .append(" por Bs. ").append(ventas.getMiMontoCompras())
                    .append(" (PV ").append(ventas.getMiPvCompras()).append(").\n");
            prompt.append("- Brazos que mas venden: ");
            datos.getVentasBrazos().stream().limit(5).forEach(b -> prompt.append(b.getNombre())
                    .append(" [").append(b.getCompras()).append(" compras, Bs. ").append(b.getMonto())
                    .append(", PV ").append(b.getPv())
                    .append(", beneficios Bs. ").append(b.getBeneficios()).append("]; "));
            prompt.append("\n- Top vendedores: ");
            datos.getTopVendedores().stream().limit(5).forEach(v -> prompt.append(v.getNombre())
                    .append(" (N").append(v.getNivel()).append(", ").append(v.getCompras())
                    .append(" compras, Bs. ").append(v.getMonto()).append(", PV ").append(v.getPv()).append("); "));
            prompt.append("\n");
        }
        IngresosSplit split = datos.getIngresosSplit();
        if (split != null) {
            prompt.append("- Origen de ingresos: afiliacion (nuevas personas) ")
                    .append(split.getAfiliacionCount()).append(" recompensas por Bs. ").append(split.getAfiliacionMonto())
                    .append("; ventas (compras de la red) ").append(split.getVentasBeneficiosCount())
                    .append(" beneficios pagados por Bs. ").append(split.getVentasBeneficiosMonto()).append(".\n");
        }
        prompt.append("- Brazos (").append(datos.getBrazos().size()).append("): ");
        datos.getBrazos().stream().limit(6).forEach(brazo -> prompt.append(brazo.getNombre())
                .append(" [").append(brazo.getMiembros()).append(" pers, ").append(brazo.getActivos())
                .append(" act, Bs. ").append(brazo.getMonto()).append("]; "));
        prompt.append("\n- Top aportantes: ");
        datos.getTopAportantes().stream().limit(5).forEach(m -> prompt.append(m.getNombre())
                .append(" (N").append(m.getNivel()).append(", Bs. ").append(m.getAporte()).append("); "));
        prompt.append("\n- Menor aporte: ");
        datos.getMenosAportan().stream().limit(5).forEach(m -> prompt.append(m.getNombre())
                .append(" (N").append(m.getNivel()).append(m.isActivo() ? ", activo" : ", inactivo")
                .append(", Bs. ").append(m.getAporte()).append("); "));
        prompt.append("\n- Inactivos (").append(datos.getInactivos().size()).append(" mostrados): ");
        datos.getInactivos().stream().limit(10).forEach(m -> prompt.append(m.getNombre())
                .append(" (N").append(m.getNivel()).append("); "));
        prompt.append("- Evolucion ultimos periodos (recompensas / ventas): ");
        datos.getEvolucion().forEach(p -> prompt.append(p.getPeriodoNombre())
                .append(": Bs. ").append(p.getMonto())
                .append(" / Bs. ").append(p.getMontoCompras()).append("; "));
        prompt.append("\nResponde solo con el informe, sin preambulos tecnicos.");
        return prompt.toString();
    }

    private boolean activoEnPeriodo(Referido referido, PeriodoGestion periodo) {
        if (referido == null || periodo == null) {
            return false;
        }
        try {
            LocalDate inicio = periodo.getFechaInicio();
            LocalDate fin = periodo.getFechaFin();
            LocalDateTime inicioMembresia = referido.getFechaInicioMembresia();
            LocalDateTime finMembresia = referido.getFechaFinMembresia();
            if (inicio != null && fin != null && inicioMembresia != null && finMembresia != null) {
                LocalDateTime inicioPeriodo = inicio.atStartOfDay();
                LocalDateTime finPeriodo = LocalDateTime.of(fin, LocalTime.of(23, 59, 59));
                return !inicioMembresia.isAfter(finPeriodo) && !finMembresia.isBefore(inicioPeriodo);
            }
        } catch (Exception ignored) {
            // fallback abajo
        }
        return Boolean.TRUE.equals(referido.getMembresiaActiva());
    }

    private String nombreCompleto(Persona persona) {
        if (persona == null) {
            return "Persona";
        }
        String nombres = persona.getNombres() == null ? "" : persona.getNombres();
        String apellidos = persona.getApellidos() == null ? "" : persona.getApellidos();
        String completo = (nombres + " " + apellidos).trim();
        return completo.isBlank() ? "Persona" : completo;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record NodoRed(Referido referido, int nivel) {
    }

    @Getter
    @RequiredArgsConstructor
    public static class MiRedResponse {
        private final Long personaId;
        private final Long periodoId;
        private final String periodoNombre;
        private final Integer gestionAnio;
        private final ResumenRed resumen;
        private final List<BrazoStats> brazos;
        private final List<NivelStats> porNivel;
        private final List<MiembroAporte> topAportantes;
        private final List<MiembroAporte> menosAportan;
        private final List<MiembroAporte> inactivos;
        private final List<PuntoEvolucion> evolucion;
        private final ConciliacionWallet conciliacionWallet;
        private final ResumenVentas resumenVentas;
        private final List<VentasBrazo> ventasBrazos;
        private final List<VentasMiembro> topVendedores;
        private final IngresosSplit ingresosSplit;
    }

    /** Afiliacion (recompensas por nuevos ingresos) vs ventas (beneficios por compras). */
    @Getter
    @RequiredArgsConstructor
    public static class IngresosSplit {
        private final int afiliacionCount;
        private final BigDecimal afiliacionMonto;
        private final int ventasBeneficiosCount;
        private final BigDecimal ventasBeneficiosMonto;
    }

    /**
     * Mismos numeros que /wallet para el mismo periodo: movimientos de dinero,
     * recompensas N2+ y total efectivo (dinero + N2+).
     */
    @Getter
    @RequiredArgsConstructor
    public static class ConciliacionWallet {
        private final int movimientosCount;
        private final BigDecimal dineroBilletera;
        private final int recompensasNivel2Count;
        private final BigDecimal recompensasNivel2Monto;
        private final BigDecimal totalEfectivoWallet;
        private final int nivel1Count;
        private final BigDecimal nivel1Efectivo;
        private final BigDecimal nivel1Productos;
        private final BigDecimal pvWallet;
        private final BigDecimal qpWallet;
        private final BigDecimal crWallet;
        private final BigDecimal productosWallet;
    }

    /** Ventas de la red en el periodo: las compras que generan volumen y recompensas. */
    @Getter
    @RequiredArgsConstructor
    public static class ResumenVentas {
        private final long totalComprasRed;
        private final BigDecimal montoComprasRed;
        private final BigDecimal pvComprasRed;
        private final BigDecimal qpComprasRed;
        private final int misCompras;
        private final BigDecimal miMontoCompras;
        private final BigDecimal miPvCompras;
    }

    @Getter
    @RequiredArgsConstructor
    public static class VentasMiembro {
        private final Long personaId;
        private final String nombre;
        private final int nivel;
        private final boolean activo;
        private final int compras;
        private final BigDecimal monto;
        private final BigDecimal pv;
        private final BigDecimal qp;
    }

    @Getter
    @RequiredArgsConstructor
    public static class VentasBrazo {
        private final Long personaId;
        private final String nombre;
        private final int compras;
        private final BigDecimal monto;
        private final BigDecimal pv;
        private final BigDecimal beneficios;
    }

    @Getter
    @RequiredArgsConstructor
    public static class AnalisisResponse {
        private final Long periodoId;
        private final String periodoNombre;
        private final String informe;
    }

    @Getter
    @RequiredArgsConstructor
    public static class ResumenRed {
        private final int totalRed;
        private final int activos;
        private final int inactivos;
        private final int directos;
        private final int profundidad;
        private final double pctActivos;
        private final BigDecimal totalEfectivo;
        private final BigDecimal totalProductos;
        private final BigDecimal total;
    }

    @Getter
    @RequiredArgsConstructor
    public static class BrazoStats {
        private final Long personaId;
        private final String nombre;
        private final int miembros;
        private final int activos;
        private final BigDecimal monto;
    }

    @Getter
    @RequiredArgsConstructor
    public static class NivelStats {
        private final int nivel;
        private final long miembros;
        private final BigDecimal aporte;
        private final BigDecimal recompensaDirecta;
    }

    @Getter
    @RequiredArgsConstructor
    public static class MiembroAporte {
        private final Long personaId;
        private final String nombre;
        private final String documento;
        private final int nivel;
        private final boolean activo;
        private final BigDecimal aporte;
        private final LocalDateTime fechaUnion;
    }

    @Getter
    @RequiredArgsConstructor
    public static class PuntoEvolucion {
        private final Long periodoId;
        private final String periodoNombre;
        private final Integer gestionAnio;
        private final Integer mes;
        private final BigDecimal monto;
        private final BigDecimal montoCompras;
    }
}
