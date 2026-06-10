package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Dao.PersonaDao;
import com.vidayoung.platform.Model.Entity.Billetera;
import com.vidayoung.platform.Model.Entity.CierreMensualBilletera;
import com.vidayoung.platform.Model.Entity.HistorialMembresia;
import com.vidayoung.platform.Model.Entity.MovimientoBilletera;
import com.vidayoung.platform.Model.Service.BilleteraService;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/billeteras")
@RequiredArgsConstructor
public class BilleteraRestController {

    private final BilleteraService billeteraService;
    private final PersonaDao personaDao;

    @GetMapping("/persona/{personaId}")
    public ResponseEntity<BilleteraResumenResponse> resumenPorPersona(@PathVariable Long personaId) {
        return personaDao.findById(personaId)
                .map(persona -> {
                    Billetera billetera = billeteraService.asegurarBilletera(persona);
                    return ResponseEntity.ok(new BilleteraResumenResponse(
                            billetera,
                            billeteraService.listarMovimientos(personaId),
                            billeteraService.listarHistorialMembresias(personaId),
                            billeteraService.listarCierresMensuales(personaId)
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/persona/{personaId}/movimientos")
    public ResponseEntity<List<MovimientoBilletera>> movimientosPorPersona(@PathVariable Long personaId) {
        return ResponseEntity.ok(billeteraService.listarMovimientos(personaId));
    }

    @GetMapping("/persona/{personaId}/membresias")
    public ResponseEntity<List<HistorialMembresia>> membresiasPorPersona(@PathVariable Long personaId) {
        return ResponseEntity.ok(billeteraService.listarHistorialMembresias(personaId));
    }

    @GetMapping("/persona/{personaId}/cierres")
    public ResponseEntity<List<CierreMensualBilletera>> cierresPorPersona(@PathVariable Long personaId) {
        return ResponseEntity.ok(billeteraService.listarCierresMensuales(personaId));
    }

    @PostMapping("/cierres-mensuales")
    public ResponseEntity<Integer> cerrarMesBilleteras() {
        return ResponseEntity.ok(billeteraService.cerrarMesBilleteras());
    }

    @PostMapping("/persona/{personaId}/activaciones")
    public ResponseEntity<HistorialMembresia> registrarActivacion(
            @PathVariable Long personaId,
            @RequestBody ActivacionRequest request
    ) {
        return ResponseEntity.ok(billeteraService.registrarActivacion(personaId, request.getPlanId()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }

    @Getter
    @RequiredArgsConstructor
    public static class BilleteraResumenResponse {

        private final Billetera billetera;

        private final List<MovimientoBilletera> movimientos;

        private final List<HistorialMembresia> membresias;

        private final List<CierreMensualBilletera> cierresMensuales;
    }

    @Getter
    @Setter
    public static class ActivacionRequest {

        private Long planId;
    }
}
