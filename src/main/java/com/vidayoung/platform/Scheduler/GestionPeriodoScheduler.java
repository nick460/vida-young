package com.vidayoung.platform.Scheduler;

import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Service.GestionPeriodoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GestionPeriodoScheduler {

    private final GestionPeriodoService gestionPeriodoService;

    /**
     * Se ejecuta el ultimo dia del mes a las 23:59 hora Bolivia (America/La_Paz).
     * - Pasa el periodo ACTIVO actual a PENDIENTE_CIERRE
     * - Crea el siguiente mes (crea Gestion si es enero) y lo pone en ACTIVO
     * Cron L = Last day of month
     */
    @Scheduled(cron = "0 59 23 L * ?", zone = "America/La_Paz")
    public void rotarPeriodoUltimoDiaMes() {
        try {
            log.info("[Scheduler] Iniciando rotacion automatica de periodo (ultimo dia 23:59 America/La_Paz)...");
            PeriodoGestion anterior = gestionPeriodoService.buscarPeriodoActivo().orElse(null);
            PeriodoGestion nuevo = gestionPeriodoService.rotarPeriodoMensualAutomatico();
            if (anterior != null && anterior.getId().equals(nuevo.getId())) {
                log.info("[Scheduler] No se realizo rotacion. Periodo activo sigue siendo: {} ({} - {})", nuevo.getNombre(), nuevo.getFechaInicio(), nuevo.getFechaFin());
            } else {
                log.info("[Scheduler] Rotacion completada: {} ({}) -> {} ({}) | Anterior ahora: PENDIENTE_CIERRE, Nuevo: ACTIVO",
                        anterior != null ? anterior.getNombre() : "N/A",
                        anterior != null ? anterior.getId() : "N/A",
                        nuevo.getNombre(), nuevo.getId());
            }
        } catch (Exception e) {
            log.error("[Scheduler] Error en rotacion automatica de periodo: {}", e.getMessage(), e);
        }
    }

    /**
     * Reconciliacion diaria a las 00:05 del dia 1 de cada mes.
     * Si el servidor estuvo apagado a las 23:59 del ultimo dia, este job corrige el estado.
     */
    @Scheduled(cron = "0 5 0 1 * ?", zone = "America/La_Paz")
    public void reconciliarPeriodoPrimerDiaMes() {
        try {
            PeriodoGestion activo = gestionPeriodoService.buscarPeriodoActivo().orElse(null);
            if (activo == null) {
                log.warn("[Scheduler] Reconciliacion: no hay periodo activo, creando periodo actual...");
                PeriodoGestion creado = gestionPeriodoService.obtenerPeriodoActivo();
                log.info("[Scheduler] Reconciliacion: periodo creado {}", creado.getNombre());
                return;
            }
            java.time.LocalDate hoy = java.time.LocalDate.now(java.time.ZoneId.of("America/La_Paz"));
            // Si el activo ya vencio (fechaFin < hoy), significa que no se rodo
            if (activo.getFechaFin().isBefore(hoy)) {
                log.warn("[Scheduler] Reconciliacion: periodo activo vencido {} (fin {}), hoy {} -> forzando rotacion",
                        activo.getNombre(), activo.getFechaFin(), hoy);
                PeriodoGestion nuevo = gestionPeriodoService.rotarPeriodoMensualAutomatico();
                log.info("[Scheduler] Reconciliacion: rotacion forzada a {}", nuevo.getNombre());
            }
        } catch (Exception e) {
            log.error("[Scheduler] Error en reconciliacion de periodo: {}", e.getMessage(), e);
        }
    }
}
