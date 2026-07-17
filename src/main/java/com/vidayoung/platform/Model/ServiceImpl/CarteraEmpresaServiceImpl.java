package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Model.Dao.CarteraEmpresaDao;
import com.vidayoung.platform.Model.Dao.MovimientoCarteraEmpresaDao;
import com.vidayoung.platform.Model.Entity.CarteraEmpresa;
import com.vidayoung.platform.Model.Entity.MovimientoCarteraEmpresa;
import com.vidayoung.platform.Model.Entity.PeriodoGestion;
import com.vidayoung.platform.Model.Service.CarteraEmpresaService;
import com.vidayoung.platform.Model.Service.GestionPeriodoService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarteraEmpresaServiceImpl implements CarteraEmpresaService {

    private final CarteraEmpresaDao carteraEmpresaDao;
    private final MovimientoCarteraEmpresaDao movimientoCarteraEmpresaDao;
    private final GestionPeriodoService gestionPeriodoService;

    @Override
    @Transactional
    public CarteraEmpresa asegurarCarteraPrincipal() {
        return carteraEmpresaDao.findByCodigo(CarteraEmpresa.CARTERA_PRINCIPAL)
                .orElseGet(() -> carteraEmpresaDao.save(CarteraEmpresa.builder()
                        .codigo(CarteraEmpresa.CARTERA_PRINCIPAL)
                        .nombre("Caja principal de la empresa")
                        .saldoActual(BigDecimal.ZERO)
                        .build()));
    }

    @Override
    @Transactional
    public MovimientoCarteraEmpresa registrarIngreso(String referenciaTipo, Long referenciaId, BigDecimal monto, String concepto) {
        return registrarMovimiento(MovimientoCarteraEmpresa.TIPO_INGRESO, referenciaTipo, referenciaId, monto, concepto);
    }

    @Override
    @Transactional
    public MovimientoCarteraEmpresa registrarEgreso(String referenciaTipo, Long referenciaId, BigDecimal monto, String concepto) {
        return registrarMovimiento(MovimientoCarteraEmpresa.TIPO_EGRESO, referenciaTipo, referenciaId, monto, concepto);
    }

    @Override
    public List<MovimientoCarteraEmpresa> listarMovimientos() {
        CarteraEmpresa cartera = asegurarCarteraPrincipal();
        return movimientoCarteraEmpresaDao.findByCarteraIdOrderByFechaRegistroDesc(cartera.getId());
    }

    @Override
    public List<MovimientoCarteraEmpresa> listarMovimientos(Long periodoId) {
        if (periodoId == null) {
            return listarMovimientos();
        }
        CarteraEmpresa cartera = asegurarCarteraPrincipal();
        return movimientoCarteraEmpresaDao.findByCarteraIdAndPeriodoIdOrderByFechaRegistroDesc(cartera.getId(), periodoId);
    }

    @Override
    public ResumenPeriodoCartera obtenerResumenPeriodo(Long periodoId) {
        CarteraEmpresa cartera = asegurarCarteraPrincipal();
        PeriodoGestion periodo = periodoId == null
                ? gestionPeriodoService.obtenerPeriodoActivo()
                : gestionPeriodoService.buscarPorId(periodoId);
        List<MovimientoCarteraEmpresa> movimientosPeriodo = listarMovimientos(periodo.getId());
        BigDecimal saldoInicial = zeroIfNull(movimientoCarteraEmpresaDao.sumMontoBeforePeriodo(cartera.getId(), periodo.getId()));
        BigDecimal movimientoNeto = zeroIfNull(movimientoCarteraEmpresaDao.sumMontoByPeriodo(cartera.getId(), periodo.getId()));
        BigDecimal ingresos = movimientosPeriodo.stream()
                .filter(movimiento -> MovimientoCarteraEmpresa.TIPO_INGRESO.equals(movimiento.getTipo()))
                .map(MovimientoCarteraEmpresa::getMonto)
                .map(this::zeroIfNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal egresos = movimientosPeriodo.stream()
                .filter(movimiento -> MovimientoCarteraEmpresa.TIPO_EGRESO.equals(movimiento.getTipo()))
                .map(MovimientoCarteraEmpresa::getMonto)
                .map(this::zeroIfNull)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ResumenPeriodoCartera(
                periodo.getId(),
                saldoInicial,
                ingresos,
                egresos,
                saldoInicial.add(movimientoNeto),
                movimientosPeriodo.size()
        );
    }

    private MovimientoCarteraEmpresa registrarMovimiento(
            String tipo,
            String referenciaTipo,
            Long referenciaId,
            BigDecimal monto,
            String concepto
    ) {
        BigDecimal importe = zeroIfNull(monto);
        if (importe.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        if (referenciaTipo != null
                && referenciaId != null
                && movimientoCarteraEmpresaDao.existsByReferenciaTipoAndReferenciaIdAndTipo(referenciaTipo, referenciaId, tipo)) {
            return null;
        }

        CarteraEmpresa cartera = asegurarCarteraPrincipal();
        BigDecimal saldoActual = zeroIfNull(cartera.getSaldoActual());
        BigDecimal movimientoMonto = MovimientoCarteraEmpresa.TIPO_EGRESO.equals(tipo) ? importe.negate() : importe;
        BigDecimal saldoResultado = saldoActual.add(movimientoMonto);
        if (MovimientoCarteraEmpresa.TIPO_EGRESO.equals(tipo) && saldoResultado.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La caja de la empresa no tiene saldo suficiente.");
        }

        cartera.setSaldoActual(saldoResultado);
        cartera = carteraEmpresaDao.save(cartera);
        return movimientoCarteraEmpresaDao.save(MovimientoCarteraEmpresa.builder()
                .cartera(cartera)
                .tipo(tipo)
                .concepto(concepto)
                .referenciaTipo(referenciaTipo)
                .referenciaId(referenciaId)
                .monto(movimientoMonto)
                .saldoResultado(saldoResultado)
                .periodo(gestionPeriodoService.obtenerPeriodoActivo())
                .build());
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
