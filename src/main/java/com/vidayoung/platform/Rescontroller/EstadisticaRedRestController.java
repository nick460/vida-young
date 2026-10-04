package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Dao.RecompensaDao;
import com.vidayoung.platform.Model.Dao.ReferidoDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Entity.Persona;
import com.vidayoung.platform.Model.Entity.Recompensa;
import com.vidayoung.platform.Model.Entity.Referido;
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
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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

        // Evolucion: ultimos 6 periodos con recompensa (por fechaInicio).
        Map<Long, BigDecimal> montoPorPeriodoId = new HashMap<>();
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
        List<PuntoEvolucion> evolucion = periodoPorId.values().stream()
                .sorted(Comparator.comparing(PeriodoGestion::getFechaInicio))
                .map(p -> new PuntoEvolucion(
                        p.getId(),
                        p.getNombre(),
                        p.getGestion() == null ? null : p.getGestion().getAnio(),
                        p.getMes(),
                        montoPorPeriodoId.getOrDefault(p.getId(), BigDecimal.ZERO)
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
                evolucion
        ));
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
    }
}
