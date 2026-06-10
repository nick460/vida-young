package com.vidayoung.platform.Security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration-ms}")
    private long expirationMs;

    public String generarToken(UserDetails userDetails) {
        Instant now = Instant.now();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        String payload = "{"
                + "\"sub\":\"" + escape(userDetails.getUsername()) + "\","
                + "\"roles\":" + rolesJson(roles) + ","
                + "\"iat\":" + now.getEpochSecond() + ","
                + "\"exp\":" + now.plusMillis(expirationMs).getEpochSecond()
                + "}";
        return firmar(payload);
    }

    public String extraerUsername(String token) {
        return extraerStringClaim(leerPayload(token), "sub");
    }

    public boolean esTokenValido(String token, UserDetails userDetails) {
        String username = extraerUsername(token);
        return username.equals(userDetails.getUsername()) && !estaExpirado(token) && firmaValida(token);
    }

    private String firmar(String payload) {
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String encodedHeader = encode(header.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = encode(payload.getBytes(StandardCharsets.UTF_8));
        String content = encodedHeader + "." + encodedPayload;
        return content + "." + encode(hmac(content));
    }

    private String leerPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Token JWT invalido");
            }

            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return new String(payload, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Token JWT invalido", exception);
        }
    }

    private boolean estaExpirado(String token) {
        long expiration = extraerLongClaim(leerPayload(token), "exp");
        return Instant.now().getEpochSecond() >= expiration;
    }

    private boolean firmaValida(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        String content = parts[0] + "." + parts[1];
        String expectedSignature = encode(hmac(content));
        return MessageDigestSafe.equals(expectedSignature, parts[2]);
    }

    private byte[] hmac(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo firmar el token JWT", exception);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String rolesJson(List<String> roles) {
        return roles.stream()
                .map(role -> "\"" + escape(role) + "\"")
                .reduce((first, second) -> first + "," + second)
                .map(value -> "[" + value + "]")
                .orElse("[]");
    }

    private String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private String extraerStringClaim(String payload, String claimName) {
        String pattern = "\"" + claimName + "\":\"";
        int start = payload.indexOf(pattern);
        if (start < 0) {
            throw new IllegalArgumentException("Claim no encontrado: " + claimName);
        }

        int valueStart = start + pattern.length();
        StringBuilder value = new StringBuilder();
        boolean escaped = false;

        for (int index = valueStart; index < payload.length(); index++) {
            char current = payload.charAt(index);
            if (escaped) {
                value.append(current);
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                return value.toString();
            }
            value.append(current);
        }

        throw new IllegalArgumentException("Claim invalido: " + claimName);
    }

    private long extraerLongClaim(String payload, String claimName) {
        String pattern = "\"" + claimName + "\":";
        int start = payload.indexOf(pattern);
        if (start < 0) {
            throw new IllegalArgumentException("Claim no encontrado: " + claimName);
        }

        int valueStart = start + pattern.length();
        int valueEnd = valueStart;

        while (valueEnd < payload.length() && Character.isDigit(payload.charAt(valueEnd))) {
            valueEnd++;
        }

        return Long.parseLong(payload.substring(valueStart, valueEnd));
    }

    private static final class MessageDigestSafe {

        private MessageDigestSafe() {
        }

        private static boolean equals(String expected, String current) {
            byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
            byte[] currentBytes = current.getBytes(StandardCharsets.UTF_8);
            return java.security.MessageDigest.isEqual(expectedBytes, currentBytes);
        }
    }
}
