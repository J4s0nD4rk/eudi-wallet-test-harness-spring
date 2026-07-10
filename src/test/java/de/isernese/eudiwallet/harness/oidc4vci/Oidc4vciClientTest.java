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
package de.isernese.eudiwallet.harness.oidc4vci;

import com.nimbusds.jose.jwk.ECKey;
import de.isernese.eudiwallet.harness.crypto.NimbusDPoPSignerFactory;
import de.isernese.eudiwallet.harness.testsupport.SdJwtTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link Oidc4vciClient}. Mocks the HTTP layer via
 * {@code MockRestServiceServer} to avoid network access and verify the protocol
 * precisely.
 */
class Oidc4vciClientTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    private static final String OFFER_JSON = """
            {"credential_issuer":"https://issuer.example",\
            "credential_configuration_ids":["example-credential-sd-jwt"],\
            "grants":{"urn:ietf:params:oauth:grant-type:pre-authorized_code":{"pre-authorized_code":"abc123"}}}""";

    private static final String ISSUER_METADATA_JSON = """
            {"credential_endpoint":"https://issuer.example/credential",
             "credential_configurations_supported":{"example-credential-sd-jwt":{}}}""";

    private static final String AUTH_SERVER_METADATA_JSON =
            "{\"token_endpoint\":\"https://issuer.example/token\"}";

    private Oidc4vciClient createClient(RestClient restClient) {
        Oidc4vciHttpService httpService = new Oidc4vciHttpService(restClient, JSON_MAPPER);
        return new Oidc4vciClient(
                new ResolveOfferStep(httpService),
                new FetchMetadataStep(httpService),
                new TokenExchangeStep(httpService, new NimbusDPoPSignerFactory()),
                new CredentialRequestStep(httpService),
                new VerifyCredentialStep(httpService)
        );
    }

    @Test
    void happyPathIssuesCredentialWithoutDpop() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        Oidc4vciClient client = createClient(builder.build());

        ECKey issuerKey = SdJwtTestFixtures.generateKey();
        var signedSdJwt = SdJwtTestFixtures.build(
                Map.of("iss", "https://issuer.example"), Map.of("exampleClaimA", "example-value-a"), issuerKey, null);

        server.expect(requestTo("https://issuer.example/.well-known/openid-credential-issuer"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(ISSUER_METADATA_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://issuer.example/.well-known/oauth-authorization-server"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(AUTH_SERVER_METADATA_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://issuer.example/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"at-123\",\"token_type\":\"Bearer\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://issuer.example/credential"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"credential\":\"" + signedSdJwt.compact() + "\"}", MediaType.APPLICATION_JSON));

        FlowOptions options = new FlowOptions(false, null, Duration.ofSeconds(5), false, false);
        IssuanceResult result = client.run(offerUri(), null, null, options);

        assertThat(result.status()).isEqualTo(IssuanceStatus.SUCCESS);
        assertThat(result.decodedClaims()).containsEntry("exampleClaimA", "example-value-a");
        server.verify();
    }

    @Test
    void tokenRequestRetriesWithDpopNonceOnServerChallenge() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        Oidc4vciClient client = createClient(builder.build());

        ECKey issuerKey = SdJwtTestFixtures.generateKey();
        var signedSdJwt = SdJwtTestFixtures.build(Map.of("iss", "https://issuer.example"), Map.of(), issuerKey, null);

        server.expect(requestTo("https://issuer.example/.well-known/openid-credential-issuer"))
                .andRespond(withSuccess(ISSUER_METADATA_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://issuer.example/.well-known/oauth-authorization-server"))
                .andRespond(withSuccess(AUTH_SERVER_METADATA_JSON, MediaType.APPLICATION_JSON));

        HttpHeaders nonceHeaders = new HttpHeaders();
        nonceHeaders.add("DPoP-Nonce", "server-nonce-123");
        server.expect(requestTo("https://issuer.example/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .headers(nonceHeaders)
                        .body("{\"error\":\"use_dpop_nonce\"}")
                        .contentType(MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://issuer.example/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"at-123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://issuer.example/credential"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"credential\":\"" + signedSdJwt.compact() + "\"}", MediaType.APPLICATION_JSON));

        FlowOptions options = new FlowOptions(false, null, Duration.ofSeconds(5), false, true);
        IssuanceResult result = client.run(offerUri(), null, null, options);

        assertThat(result.status()).isEqualTo(IssuanceStatus.SUCCESS);
        assertThat(result.logs()).anyMatch(l -> l.contains("DPoP-Nonce"));
        server.verify();
    }

    @Test
    void missingPreAuthorizedGrantFailsWithExplicitLogs() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        Oidc4vciClient client = createClient(builder.build());

        String offerWithoutGrant = "openid-credential-offer://?credential_offer="
                + URLEncoder.encode("""
                {"credential_issuer":"https://issuer.example","credential_configuration_ids":["example-credential-sd-jwt"],"grants":{}}""",
                        StandardCharsets.UTF_8);

        server.expect(requestTo("https://issuer.example/.well-known/openid-credential-issuer"))
                .andRespond(withSuccess(ISSUER_METADATA_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://issuer.example/.well-known/oauth-authorization-server"))
                .andRespond(withSuccess(AUTH_SERVER_METADATA_JSON, MediaType.APPLICATION_JSON));

        FlowOptions options = new FlowOptions(false, null, Duration.ofSeconds(5), false, false);

        org.junit.jupiter.api.Assertions.assertThrows(IssuanceFlowException.class,
                () -> client.run(offerWithoutGrant, null, null, options));
    }

    @Test
    void failsIfSignatureVerificationFailsAndVerifySignatureIsTrue() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        Oidc4vciClient client = createClient(builder.build());

        ECKey signingKey = SdJwtTestFixtures.generateKey();
        ECKey unrelatedCertKey = SdJwtTestFixtures.generateKey();
        var signedSdJwt = SdJwtTestFixtures.build(
                Map.of("iss", "https://issuer.example"), Map.of(), signingKey, unrelatedCertKey);

        server.expect(requestTo("https://issuer.example/.well-known/openid-credential-issuer"))
                .andRespond(withSuccess(ISSUER_METADATA_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://issuer.example/.well-known/oauth-authorization-server"))
                .andRespond(withSuccess(AUTH_SERVER_METADATA_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://issuer.example/token"))
                .andRespond(withSuccess("{\"access_token\":\"at-123\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://issuer.example/credential"))
                .andRespond(withSuccess("{\"credential\":\"" + signedSdJwt.compact() + "\"}", MediaType.APPLICATION_JSON));

        FlowOptions options = new FlowOptions(false, null, Duration.ofSeconds(5), true, false);

        org.junit.jupiter.api.Assertions.assertThrows(IssuanceFlowException.class,
                () -> client.run(offerUri(), null, null, options));
    }

    private static String offerUri() {
        return "openid-credential-offer://?credential_offer="
                + URLEncoder.encode(OFFER_JSON, StandardCharsets.UTF_8);
    }
}
