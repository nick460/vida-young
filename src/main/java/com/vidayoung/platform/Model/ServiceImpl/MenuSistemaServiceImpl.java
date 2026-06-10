package com.vidayoung.platform.Model.ServiceImpl;

import com.vidayoung.platform.Dto.MenuConfigResponse;
import com.vidayoung.platform.Model.Dao.MenuSistemaDao;
import com.vidayoung.platform.Model.Dao.RolDao;
import com.vidayoung.platform.Model.Entity.Auditoria;
import com.vidayoung.platform.Model.Entity.MenuSistema;
import com.vidayoung.platform.Model.Entity.Rol;
import com.vidayoung.platform.Model.Service.MenuSistemaService;
import jakarta.transaction.Transactional;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MenuSistemaServiceImpl implements MenuSistemaService {

    private static final String ROLE_ADMIN = "ADMIN";

    private final MenuSistemaDao menuSistemaDao;
    private final RolDao rolDao;

    @Override
    public MenuConfigResponse obtenerConfiguracion() {
        List<MenuSistema> menus = listarMenusActivos();
        Map<String, List<String>> permissions = construirPermisos(menus);
        List<MenuConfigResponse.MenuItem> menuItems = menus.stream()
                .map(menu -> new MenuConfigResponse.MenuItem(
                        menu.getMenuId(),
                        menu.getLabel(),
                        menu.getIcon(),
                        rolesParaMenu(menu.getMenuId(), permissions),
                        Boolean.TRUE.equals(menu.getCustom())
                ))
                .toList();

        return new MenuConfigResponse(menuItems, permissions);
    }

    @Override
    @Transactional
    public MenuSistema guardarMenu(MenuSistema menu) {
        String label = menu.getLabel() == null ? "" : menu.getLabel().trim();
        String menuId = normalizarMenuId(menu.getMenuId() == null || menu.getMenuId().isBlank() ? label : menu.getMenuId());

        if (label.isBlank() || menuId.isBlank()) {
            throw new IllegalArgumentException("Nombre de menu obligatorio.");
        }

        MenuSistema persistente = menuSistemaDao.findByMenuId(menuId).orElseGet(MenuSistema::new);
        persistente.setMenuId(menuId);
        persistente.setLabel(label);
        persistente.setIcon(menu.getIcon() == null || menu.getIcon().isBlank() ? "PanelTop" : menu.getIcon());
        persistente.setCustom(true);
        persistente.setOrden(menu.getOrden() == null ? 1000 : menu.getOrden());
        persistente.setEstado(Auditoria.ESTADO_ACTIVO);
        MenuSistema saved = menuSistemaDao.save(persistente);

        Rol admin = rolDao.findByNombre(ROLE_ADMIN).orElse(null);
        if (admin != null) {
            admin.getMenus().add(saved);
            rolDao.save(admin);
        }

        return saved;
    }

    @Override
    @Transactional
    public void eliminarMenu(String menuId) {
        menuSistemaDao.findByMenuId(menuId).ifPresent(menu -> {
            if (!Boolean.TRUE.equals(menu.getCustom())) {
                throw new IllegalArgumentException("No se puede eliminar un menu base.");
            }

            rolDao.findAll().forEach(rol -> {
                rol.getMenus().removeIf(item -> item.getMenuId().equals(menu.getMenuId()));
                rolDao.save(rol);
            });
            menu.setEstado(Auditoria.ESTADO_ELIMINADO);
            menuSistemaDao.save(menu);
        });
    }

    @Override
    @Transactional
    public void guardarPermisos(Map<String, List<String>> permissions) {
        Map<String, MenuSistema> menusById = listarMenusActivos().stream()
                .collect(Collectors.toMap(MenuSistema::getMenuId, menu -> menu));

        rolDao.findAll().stream()
                .filter(rol -> Auditoria.ESTADO_ACTIVO.equals(rol.getEstado()))
                .forEach(rol -> {
                    String roleName = normalizarRol(rol.getNombre());
                    List<String> menuIds = ROLE_ADMIN.equals(roleName)
                            ? new ArrayList<>(menusById.keySet())
                            : permissions.getOrDefault(roleName, List.of());
                    Set<MenuSistema> nextMenus = menuIds.stream()
                            .map(menusById::get)
                            .filter(menu -> menu != null)
                            .collect(Collectors.toSet());
                    rol.setMenus(nextMenus);
                    rolDao.save(rol);
                });
    }

    private List<MenuSistema> listarMenusActivos() {
        return menuSistemaDao.findAllByOrderByOrdenAscIdAsc().stream()
                .filter(menu -> Auditoria.ESTADO_ACTIVO.equals(menu.getEstado()))
                .toList();
    }

    private Map<String, List<String>> construirPermisos(List<MenuSistema> menus) {
        Map<String, List<String>> permissions = new LinkedHashMap<>();
        List<String> allMenuIds = menus.stream().map(MenuSistema::getMenuId).toList();

        rolDao.findAll().stream()
                .filter(rol -> Auditoria.ESTADO_ACTIVO.equals(rol.getEstado()))
                .forEach(rol -> {
                    String roleName = normalizarRol(rol.getNombre());
                    List<String> menuIds = ROLE_ADMIN.equals(roleName)
                            ? allMenuIds
                            : rol.getMenus().stream()
                                    .filter(menu -> Auditoria.ESTADO_ACTIVO.equals(menu.getEstado()))
                                    .map(MenuSistema::getMenuId)
                                    .filter(allMenuIds::contains)
                                    .toList();
                    permissions.put(roleName, menuIds);
                });

        return permissions;
    }

    private List<String> rolesParaMenu(String menuId, Map<String, List<String>> permissions) {
        List<String> roles = permissions.entrySet().stream()
                .filter(entry -> entry.getValue().contains(menuId))
                .map(Map.Entry::getKey)
                .toList();
        return roles.isEmpty() ? List.of(ROLE_ADMIN) : roles;
    }

    private String normalizarRol(String role) {
        return role == null ? "" : role.replaceFirst("(?i)^ROLE_", "").trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarMenuId(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return normalized;
    }
}
