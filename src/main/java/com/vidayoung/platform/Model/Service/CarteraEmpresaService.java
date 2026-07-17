package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Model.Entity.CarteraEmpresa;
import com.vidayoung.platform.Model.Entity.MovimientoCarteraEmpresa;
import java.math.BigDecimal;
import java.util.List;

public interface CarteraEmpresaService {

    CarteraEmpresa asegurarCarteraPrincipal();

    MovimientoCarteraEmpresa registrarIngreso(String referenciaTipo, Long referenciaId, BigDecimal monto, String concepto);

    MovimientoCarteraEmpresa registrarEgreso(String referenciaTipo, Long referenciaId, BigDecimal monto, String concepto);

    List<MovimientoCarteraEmpresa> listarMovimientos();

    List<MovimientoCarteraEmpresa> listarMovimientos(Long periodoId);

    ResumenPeriodoCartera obtenerResumenPeriodo(Long periodoId);

    record ResumenPeriodoCartera(
            Long periodoId,
            BigDecimal saldoInicial,
            BigDecimal ingresos,
            BigDecimal egresos,
            BigDecimal saldoFinal,
            Integer cantidadMovimientos
    ) {
    }
}
