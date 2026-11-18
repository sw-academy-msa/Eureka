package com.msa.eureka.global.config.monitoring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Eureka 등록과 peer 통신의 기존 보안 계약은 유지하고 메트릭 경로만 보호합니다. */
@Component
@Order(-100)
public class MonitoringSecurityConfig extends OncePerRequestFilter {
    private final String token;

    public MonitoringSecurityConfig(@Value("${monitoring.scrape-token:}") String token) {
        this.token = token;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ((path.equals("/actuator/prometheus") || path.startsWith("/actuator/prometheus/"))
                && !MonitoringAccess.allowed(
                        token, request.getHeader("Authorization"), request.getMethod())) {
            response.setStatus(403);
            return;
        }
        chain.doFilter(request, response);
    }
}
