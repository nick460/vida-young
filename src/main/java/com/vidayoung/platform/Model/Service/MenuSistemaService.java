package com.vidayoung.platform.Model.Service;

import com.vidayoung.platform.Dto.MenuConfigResponse;
import com.vidayoung.platform.Model.Entity.MenuSistema;
import java.util.List;
import java.util.Map;

public interface MenuSistemaService {

    MenuConfigResponse obtenerConfiguracion();

    MenuSistema guardarMenu(MenuSistema menu);

    void eliminarMenu(String menuId);

    void guardarPermisos(Map<String, List<String>> permissions);
}
