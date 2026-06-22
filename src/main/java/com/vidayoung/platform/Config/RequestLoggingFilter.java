package com.vidayoung.platform.Config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        Exception failure = null;

        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException ex) {
            failure = ex;
            throw ex;
        } finally {
            long elapsedMs = System.currentTimeMillis() - start;
            int status = failure == null ? response.getStatus() : resolveFailureStatus(response);
            String query = request.getQueryString();
            String path = query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
            String username = request.getUserPrincipal() == null ? "-" : request.getUserPrincipal().getName();

            if (failure == null) {
                log.info(
                        "HTTP {} {} -> {} {}ms ip={} user={}",
                        request.getMethod(),
                        path,
                        status,
                        elapsedMs,
                        clientIp(request),
                        username
                );
            } else {
                log.warn(
                        "HTTP {} {} -> {} {}ms ip={} user={} error={}: {}",
                        request.getMethod(),
                        path,
                        status,
                        elapsedMs,
                        clientIp(request),
                        username,
                        failure.getClass().getSimpleName(),
                        failure.getMessage()
                );
            }
        }
    }

    private int resolveFailureStatus(HttpServletResponse response) {
        int status = response.getStatus();
        return status >= 400 ? status : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp;
    }
}
