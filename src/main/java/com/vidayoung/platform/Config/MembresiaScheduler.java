package com.vidayoung.platform.Config;

import com.vidayoung.platform.Model.Service.ReferidoService;
import com.vidayoung.platform.Model.Service.BilleteraService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MembresiaScheduler {

    private final ReferidoService referidoService;
    private final BilleteraService billeteraService;

    @Scheduled(cron = "0 59 23 * * *", zone = "America/La_Paz")
    public void vencerMembresiasExpiradas() {
        int totalVencidas = referidoService.vencerMembresiasExpiradas();

        if (totalVencidas > 0) {
            log.info("Membresias vencidas marcadas como inactivas: {}", totalVencidas);
        }
    }

    @Scheduled(cron = "0 58 23 * * *", zone = "America/La_Paz")
    public void cerrarBilleterasFinDeMes() {
        if (LocalDate.now().plusDays(1).getDayOfMonth() != 1) {
            return;
        }

        int totalCierres = billeteraService.cerrarMesBilleteras();

        if (totalCierres > 0) {
            log.info("Billeteras cerradas para planilla mensual: {}", totalCierres);
        }
    }
}
