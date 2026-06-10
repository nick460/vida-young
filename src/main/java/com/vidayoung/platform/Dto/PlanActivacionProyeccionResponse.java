package com.vidayoung.platform.Dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PlanActivacionProyeccionResponse {

    private Long personaId;

    private BigDecimal pvMensualActual;

    private List<PlanProyectado> planes;

    @Getter
    @AllArgsConstructor
    public static class PlanProyectado {

        private Long planActivacionId;

        private String nombre;

        private BigDecimal pvMinimoMensual;

        private Integer nivelesAlcance;

        private Boolean activable;

        private BigDecimal pvFaltante;

        private BigDecimal gananciaMaximaPorProducto;

        private List<NivelProyectado> niveles;
    }

    @Getter
    @AllArgsConstructor
    public static class NivelProyectado {

        private Integer numeroNivel;

        private Integer personasEnNivel;

        private BigDecimal montoPorProducto;

        private BigDecimal potencialPorProducto;
    }
}
