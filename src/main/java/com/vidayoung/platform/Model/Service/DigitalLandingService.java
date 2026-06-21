package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Dto.DigitalLandingRequest;
import com.vidayoung.platform.Dto.DigitalLandingResponse;
import java.util.List;
import java.util.Optional;

public interface DigitalLandingService {

    List<DigitalLandingResponse> listar();

    Optional<DigitalLandingResponse> buscarPorId(Long id);

    Optional<DigitalLandingResponse> buscarPorSlug(String slug);

    DigitalLandingResponse guardar(DigitalLandingRequest request);

    DigitalLandingResponse actualizar(Long id, DigitalLandingRequest request);

    void eliminar(Long id);
}
