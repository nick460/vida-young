package com.vidayoung.platform.Security;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Component
public class PrincipalHandshakeHandler extends DefaultHandshakeHandler {

    private static final String ATRIBUTO_USERNAME = "username";

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String username = (String) attributes.get(ATRIBUTO_USERNAME);
        return username == null ? null : new StompPrincipal(username);
    }
}