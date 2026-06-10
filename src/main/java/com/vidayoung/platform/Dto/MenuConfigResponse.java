package com.vidayoung.platform.Dto;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MenuConfigResponse {

    private List<MenuItem> menus;

    private Map<String, List<String>> permissions;

    @Getter
    @AllArgsConstructor
    public static class MenuItem {

        private String id;

        private String label;

        private String icon;

        private List<String> roles;

        private Boolean custom;
    }
}
