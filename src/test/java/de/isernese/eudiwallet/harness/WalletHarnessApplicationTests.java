/*
 * Copyright 2026 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.isernese.eudiwallet.harness;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Startup smoke test: starts the full Spring application context against a
 * real (random) port and verifies that the application comes up and responds at
 * all — independent of the Oidc4vciClient unit/integration tests, which cover the
 * issuance flow itself with {@code MockRestServiceServer}. Catches errors that
 * pure unit tests would not see (e.g., broken bean wiring, invalid
 * application.yml, missing configuration class registration).
 * <p>
 * {@code @SpringBootTest} alone no longer provides a {@link TestRestTemplate}
 * automatically since Spring Boot 4 (change from Boot 3) —
 * {@code @AutoConfigureTestRestTemplate} configures the bean explicitly; this
 * also requires a test dependency on {@code spring-boot-resttestclient} (see
 * pom.xml).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class WalletHarnessApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void contextLoads() {
        // Intentionally empty: already fails if the ApplicationContext does not
        // start cleanly (broken bean wiring, invalid configuration, etc.).
    }

    @Test
    void applicationStartsAndHealthEndpointReportsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void credentialConfigurationsEndpointServesTheExampleScenario() {
        ResponseEntity<String> response = restTemplate.getForEntity("/config/credential-configurations", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("example_credential");
    }
}
