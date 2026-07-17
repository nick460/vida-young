package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.GestionDao;
import com.vidayoung.platform.Model.Dao.PeriodoGestionDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Gestion;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Service.GestionPeriodoService;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GestionPeriodoServiceImpl implements GestionPeriodoService {

    private final GestionDao gestionDao;
    private final PeriodoGestionDao periodoGestionDao;

    @Override
    public List<Gestion> listarGestiones() {
        return gestionDao.findAll().stream()
                .filter(gestion -> Auditoria.ESTADO_ACTIVO.equals(gestion.getEstado()))
                .sorted(Comparator.comparing(Gestion::getAnio).reversed())
                .toList();
    }

    @Override
    public List<PeriodoGestion> listarPeriodos(Long gestionId) {
        return periodoGestionDao.findByGestionIdOrderByMes(gestionId).stream()
                .filter(periodo -> Auditoria.ESTADO_ACTIVO.equals(periodo.getEstado()))
                .toList();
    }

    @Override
    @Transactional
    public Gestion crearGestion(Integer anio, String nombre) {
        int year = anio == null ? LocalDate.now().getYear() : anio;
        return gestionDao.findByAnio(year)
                .orElseGet(() -> gestionDao.save(Gestion.builder()
                        .anio(year)
                        .nombre(normalizarTexto(nombre) == null ? "Gestion " + year : normalizarTexto(nombre))
                        .fechaInicio(LocalDate.of(year, 1, 1))
                        .fechaFin(LocalDate.of(year, 12, 31))
                        .build()));
    }

    @Override
    @Transactional
    public Gestion actualizarGestion(Long gestionId, Integer anio, String nombre) {
        Gestion gestion = gestionDao.findById(gestionId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Gestion no encontrada."));

        int oldYear = gestion.getAnio();
        int year = anio == null ? gestion.getAnio() : anio;
        gestionDao.findByAnio(year)
                .filter(existing -> !Objects.equals(existing.getId(), gestion.getId()))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya existe una gestion para el anio indicado.");
                });

        gestion.setAnio(year);
        gestion.setNombre(normalizarTexto(nombre) == null ? "Gestion " + year : normalizarTexto(nombre));
        gestion.setFechaInicio(LocalDate.of(year, 1, 1));
        gestion.setFechaFin(LocalDate.of(year, 12, 31));
        Gestion actualizada = gestionDao.save(gestion);

        periodoGestionDao.findByGestionIdOrderByMes(actualizada.getId()).stream()
                .filter(periodo -> Auditoria.ESTADO_ACTIVO.equals(periodo.getEstado()))
                .forEach(periodo -> {
                    LocalDate inicio = LocalDate.of(year, periodo.getMes(), 1);
                    if (Objects.equals(periodo.getNombre(), buildPeriodoLabel(periodo.getMes(), oldYear))) {
                        periodo.setNombre(buildPeriodoLabel(periodo.getMes(), year));
                    }
                    periodo.setFechaInicio(inicio);
                    periodo.setFechaFin(inicio.withDayOfMonth(inicio.lengthOfMonth()));
                    periodoGestionDao.save(periodo);
                });

        return actualizada;
    }

    @Override
    @Transactional
    public PeriodoGestion crearPeriodo(Long gestionId, Integer mes, String nombre) {
        if (mes == null || mes < 1 || mes > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12.");
        }
        Gestion gestion = gestionDao.findById(gestionId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Gestion no encontrada."));
        return periodoGestionDao.findByGestionIdAndMes(gestion.getId(), mes)
                .orElseGet(() -> periodoGestionDao.save(buildPeriodo(gestion, mes, nombre, PeriodoGestion.ESTADO_PERIODO_PENDIENTE)));
    }

    @Override
    @Transactional
    public PeriodoGestion actualizarPeriodo(Long periodoId, String nombre) {
        PeriodoGestion periodo = periodoGestionDao.findById(periodoId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Periodo no encontrado."));
        periodo.setNombre(normalizarTexto(nombre) == null
                ? buildPeriodoLabel(periodo.getMes(), periodo.getGestion().getAnio())
                : normalizarTexto(nombre));
        return periodoGestionDao.save(periodo);
    }

    @Override
    public Optional<PeriodoGestion> buscarPeriodoActivo() {
        return periodoGestionDao.findFirstByEstadoPeriodoOrderByFechaInicioDesc(PeriodoGestion.ESTADO_PERIODO_ACTIVO)
                .filter(periodo -> Auditoria.ESTADO_ACTIVO.equals(periodo.getEstado()));
    }

    @Override
    public PeriodoGestion buscarPorId(Long periodoId) {
        return periodoGestionDao.findById(periodoId)
                .filter(periodo -> Auditoria.ESTADO_ACTIVO.equals(periodo.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Periodo no encontrado."));
    }

    @Override
    @Transactional
    public PeriodoGestion obtenerPeriodoActivo() {
        return buscarPeriodoActivo().orElseGet(this::crearPeriodoActualActivo);
    }

    @Override
    @Transactional
    public PeriodoGestion activarPeriodo(Long periodoId) {
        PeriodoGestion periodo = periodoGestionDao.findById(periodoId)
                .filter(item -> Auditoria.ESTADO_ACTIVO.equals(item.getEstado()))
                .orElseThrow(() -> new IllegalArgumentException("Periodo no encontrado."));

        buscarPeriodoActivo().ifPresent(actual -> {
            if (!actual.getId().equals(periodo.getId())) {
                actual.setEstadoPeriodo(PeriodoGestion.ESTADO_PERIODO_CERRADO);
                periodoGestionDao.save(actual);
            }
        });

        periodo.setEstadoPeriodo(PeriodoGestion.ESTADO_PERIODO_ACTIVO);
        return periodoGestionDao.save(periodo);
    }

    @Override
    @Transactional
    public PeriodoGestion cerrarPeriodoActivo() {
        PeriodoGestion activo = buscarPeriodoActivo()
                .orElseThrow(() -> new IllegalArgumentException("No existe un periodo activo."));
        activo.setEstadoPeriodo(PeriodoGestion.ESTADO_PERIODO_CERRADO);
        periodoGestionDao.save(activo);

        LocalDate siguienteFecha = activo.getFechaInicio().plusMonths(1);
        Gestion siguienteGestion = crearGestion(siguienteFecha.getYear(), "Gestion " + siguienteFecha.getYear());
        PeriodoGestion siguientePeriodo = periodoGestionDao.findByGestionIdAndMes(siguienteGestion.getId(), siguienteFecha.getMonthValue())
                .orElseGet(() -> periodoGestionDao.save(buildPeriodo(
                        siguienteGestion,
                        siguienteFecha.getMonthValue(),
                        null,
                        PeriodoGestion.ESTADO_PERIODO_PENDIENTE
                )));
        siguientePeriodo.setEstadoPeriodo(PeriodoGestion.ESTADO_PERIODO_ACTIVO);
        return periodoGestionDao.save(siguientePeriodo);
    }

    private PeriodoGestion crearPeriodoActualActivo() {
        LocalDate today = LocalDate.now();
        Gestion gestion = crearGestion(today.getYear(), "Gestion " + today.getYear());
        PeriodoGestion periodo = periodoGestionDao.findByGestionIdAndMes(gestion.getId(), today.getMonthValue())
                .orElseGet(() -> periodoGestionDao.save(buildPeriodo(gestion, today.getMonthValue(), null, PeriodoGestion.ESTADO_PERIODO_ACTIVO)));
        periodo.setEstadoPeriodo(PeriodoGestion.ESTADO_PERIODO_ACTIVO);
        return periodoGestionDao.save(periodo);
    }

    private PeriodoGestion buildPeriodo(Gestion gestion, Integer mes, String nombre, String estadoPeriodo) {
        LocalDate inicio = LocalDate.of(gestion.getAnio(), mes, 1);
        String label = normalizarTexto(nombre) == null ? buildPeriodoLabel(mes, gestion.getAnio()) : normalizarTexto(nombre);
        return PeriodoGestion.builder()
                .gestion(gestion)
                .mes(mes)
                .nombre(label)
                .fechaInicio(inicio)
                .fechaFin(inicio.withDayOfMonth(inicio.lengthOfMonth()))
                .estadoPeriodo(estadoPeriodo)
                .build();
    }

    private String buildPeriodoLabel(Integer mes, Integer anio) {
        String monthName = Month.of(mes).getDisplayName(TextStyle.FULL, new Locale("es", "BO"));
        return capitalize(monthName) + " " + anio;
    }

    private String normalizarTexto(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
