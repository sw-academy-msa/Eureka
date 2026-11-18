package com.msa.eureka.global.config.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** Eureka 등록 경로는 보존하고 인증 없는 메트릭 접근만 차단하는지 검증합니다. */
class MonitoringSecurityTest {
    private final String token = "synthetic-monitoring-token-for-test-only";

    @Test
    void protectsMetricsAndPreservesRegistration() throws Exception {
        var filter = new MonitoringSecurityConfig(token);
        var request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        var response = new MockHttpServletResponse();
        filter.doFilter(
                request,
                response,
                (a, b) -> {
                    throw new AssertionError("Anonymous scrape reached handler");
                });
        assertThat(response.getStatus()).isEqualTo(403);
        request = new MockHttpServletRequest("GET", "/actuator/prometheus");
        request.addHeader("Authorization", "Bearer " + token);
        response = new MockHttpServletResponse();
        filter.doFilter(
                request,
                response,
                (a, b) -> ((jakarta.servlet.http.HttpServletResponse) b).setStatus(204));
        assertThat(response.getStatus()).isEqualTo(204);
        request = new MockHttpServletRequest("POST", "/eureka/apps");
        response = new MockHttpServletResponse();
        filter.doFilter(
                request,
                response,
                (a, b) -> ((jakarta.servlet.http.HttpServletResponse) b).setStatus(204));
        assertThat(response.getStatus()).isEqualTo(204);
    }
}
