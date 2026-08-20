package com.vidayoung.platform.Security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StompPrincipal implements java.security.Principal {

    private final String name;
}