package com.vidayoung.platform.Config;

import com.vidayoung.platform.Model.Service.ReferidoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MembresiaScheduler {

    private final ReferidoService referidoService;

    @Scheduled(cron = "0 59 23 * * *", zone = "America/La_Paz")
    public void vencerMembresiasExpiradas() {
        int totalVencidas = referidoService.vencerMembresiasExpiradas();

        if (totalVencidas > 0) {
            log.info("Membresias vencidas marcadas como inactivas: {}", totalVencidas);
        }
    }
}
