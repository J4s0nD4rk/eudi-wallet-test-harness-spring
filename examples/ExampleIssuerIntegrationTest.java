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
package de.isernese.eudiwallet.harness.examples;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Example integration test for your actual issuer application.
 *
 * This test starts your application on a random port and sends the generated
 * credential offer to the running EUDI Wallet Test Harness to test the entire
 * issuance and verification flow end-to-end.
 *
 * Prerequisites:
 * - The EUDI Wallet Test Harness is running locally on port 8090 (e.g., via
 *   docker-compose or Maven).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExampleIssuerIntegrationTest {

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplateBuilder().build();

    // Path to the EUDI Wallet Test Harness test instance (default port)
    private static final String HARNESS_URL = "http://localhost:8090/issuance/run";

    @Test
    void testFullIssuanceFlowAgainstTestHarness() {
        // Step 1: Generate a credential offer in your application.
        // IMPORTANT: If the test harness runs in Docker, it cannot resolve "localhost".
        // In this case, use "host.docker.internal" instead of "localhost" for the issuer.
        boolean harnessInDocker = true; // Set to false if the harness runs locally without Docker
        String issuerHost = harnessInDocker ? "host.docker.internal" : "localhost";
        String localIssuerUrl = "http://" + issuerHost + ":" + port;
        
        String offerUri = generateTestCredentialOffer(localIssuerUrl);

        // Step 2: Create the request body for the test harness.
        Map<String, Object> requestBody = Map.of(
                "offerUri", offerUri,
                "credentialConfigurationId", "example_credential", // Must exist in the harness credential-configs.yaml
                "useDpop", true,              // Enable DPoP
                "verifySdJwtSignature", true  // Enable signature verification
        );

        // Step 3: Send the offer via POST to the test harness.
        ResponseEntity<HarnessResponse> response = restTemplate.postForEntity(
                HARNESS_URL,
                requestBody,
                HarnessResponse.class
        );

        // Step 4: Verify the result.
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        HarnessResponse result = response.getBody();
        assertThat(result).isNotNull();
        
        // The simulator must report SUCCESS
        assertThat(result.status()).isEqualTo("SUCCESS");
        
        // The issued credential (SD-JWT) must be present
        assertThat(result.rawSdJwt()).isNotEmpty();
        
        // Verify that the expected claims are correctly present in the issued document
        assertThat(result.decodedClaims())
                .containsEntry("exampleClaimA", "example-value-a");
    }

    private String generateTestCredentialOffer(String issuerUrl) {
        // Simulate how your application generates an OIDC4VCI credential offer.
        // In practice, call the corresponding service of your application here.
        // Example format for pre-authorized code flow:
        String preAuthorizedCode = "test-pre-auth-code-12345";
        return "openid-credential-offer://?credential_offer=" + 
                "{\"credential_issuer\":\"" + issuerUrl + "\"," +
                "\"credential_configuration_ids\":[\"example_credential\"]," +
                "\"grants\":{\"urn:ietf:params:oauth:grant-type:pre-authorized_code\":" +
                "{\"pre-authorized_code\":\"" + preAuthorizedCode + "\"}}}";
    }

    // Helper record for mapping the harness response format
    record HarnessResponse(
            String status,
            String error,
            List<String> logs,
            String rawSdJwt,
            Map<String, Object> decodedClaims
    ) {}
}
