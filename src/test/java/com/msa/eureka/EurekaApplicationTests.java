package com.msa.eureka;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.assertj.core.api.Assertions.assertThat;

/** 실제 HTTP 서버를 통해 레지스트리 API와 상태 확인 경로를 검증합니다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=local")
class EurekaApplicationTests {
    @LocalServerPort
    private int port;

    @Test
    void servesRegistryAndHealth() throws Exception {
        // 설정만 검사하지 않고 클라이언트가 접근하는 실제 경로를 호출합니다.
        try (var client = HttpClient.newHttpClient()) {
            var registry = client.send(HttpRequest.newBuilder(
                    URI.create("http://localhost:" + port + "/eureka/apps"))
                    .header("Accept", "application/json").GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(registry.statusCode()).isEqualTo(200);
            assertThat(registry.body()).contains("applications");
            var health = client.send(HttpRequest.newBuilder(
                    URI.create("http://localhost:" + port + "/actuator/health"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(health.statusCode()).isEqualTo(200);
            assertThat(health.body()).contains("\"status\":\"UP\"");
        }
    }
}
