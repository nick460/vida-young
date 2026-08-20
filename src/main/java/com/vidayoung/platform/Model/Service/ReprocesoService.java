package com.vidayoung.platform.Model.Service;

import java.math.BigDecimal;
import java.util.List;

public interface ReprocesoService {

    ReprocesoResumen simular();

    ReprocesoResumen reprocesar(boolean notificar);

    record ReprocesoResumen(
            boolean simulacion,
            int comprasProcesadas,
            int bonosQpCreditados,
            BigDecimal qpTotalCreditado,
            int beneficiariosRecalculados,
            BigDecimal dineroTotalCreditado,
            int inactivosOmitidos,
            int posibleDebitoOmitidos,
            int fallos,
            List<String> detalles
    ) {
    }
}