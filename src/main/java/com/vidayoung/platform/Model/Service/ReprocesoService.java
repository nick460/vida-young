package com.vidayoung.platform.Model.Service;

import java.math.BigDecimal;
import java.util.List;

public interface ReprocesoService {

    ReprocesoResumen simular();

    ReprocesoResumen reprocesar(boolean notificar);

    RecreacionResumen simularRecreacion();

    RecreacionResumen recrearRecompensas(boolean notificar);

    record ReprocesoResumen(
            boolean simulacion,
            int beneficiariosRecalculados,
            BigDecimal dineroTotalCreditado,
            int inactivosOmitidos,
            int posibleDebitoOmitidos,
            int fallos,
            List<String> detalles
    ) {
    }

    record RecreacionResumen(
            boolean simulacion,
            int comprasProcesadas,
            int beneficiosGenerados,
            int volumenesRedCreditados,
            int saldosPvPropioActualizados,
            int fallos,
            List<String> detalles
    ) {
    }
}