package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Entity.CarteraEmpresa;
import com.vidayoung.platform.Model.Entity.MovimientoCarteraEmpresa;
import com.vidayoung.platform.Model.Service.CarteraEmpresaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cartera-empresa")
@RequiredArgsConstructor
public class CarteraEmpresaRestController {

    private final CarteraEmpresaService carteraEmpresaService;

    @GetMapping
    public ResponseEntity<CarteraEmpresa> obtenerCarteraPrincipal() {
        return ResponseEntity.ok(carteraEmpresaService.asegurarCarteraPrincipal());
    }

    @GetMapping("/movimientos")
    public ResponseEntity<List<MovimientoCarteraEmpresa>> listarMovimientos() {
        return ResponseEntity.ok(carteraEmpresaService.listarMovimientos());
    }
}
