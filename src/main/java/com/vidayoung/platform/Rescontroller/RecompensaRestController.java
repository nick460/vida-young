package com.vidayoung.platform.Rescontroller;

import com.vidayoung.platform.Model.Dao.RecompensaDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.Recompensa;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recompensas")
@RequiredArgsConstructor
public class RecompensaRestController {

    private final RecompensaDao recompensaDao;

    @GetMapping("/persona/{personaId}")
    public ResponseEntity<List<Recompensa>> listarPorPersona(@PathVariable Long personaId) {
        return ResponseEntity.ok(recompensaDao.findByBeneficiarioId(personaId).stream()
                .filter(recompensa -> Auditoria.ESTADO_ACTIVO.equals(recompensa.getEstado()))
                .toList());
    }
}
