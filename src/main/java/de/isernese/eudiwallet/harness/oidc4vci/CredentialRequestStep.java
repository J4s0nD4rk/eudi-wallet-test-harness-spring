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

import de.isernese.eudiwallet.harness.crypto.DPoPSigner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static de.isernese.eudiwallet.harness.oidc4vci.Oidc4vciConstants.*;
import static de.isernese.eudiwallet.harness.oidc4vci.Oidc4vciHttpService.*;

/**
 * Step 4: Creates the holder-binding proof JWT and retrieves the credential.
 */
@Component
public class CredentialRequestStep implements FlowStep {

    private final Oidc4vciHttpService httpService;

    public CredentialRequestStep(Oidc4vciHttpService httpService) {
        this.httpService = httpService;
    }

    @Override
    public void execute(FlowContext ctx, FlowOptions options) {
        String credentialEndpoint = requireString(ctx.issuerMetadata(), FIELD_CREDENTIAL_ENDPOINT,
                "Issuer metadata does not contain " + FIELD_CREDENTIAL_ENDPOINT, ctx);
        String cNonce = ctx.tokenResponse().get(FIELD_C_NONCE) instanceof String s ? s : null;

        // Build Holder Binding Proof JWT
        String proofJwt = ctx.dpopSigner().generateHolderBindingProof(ctx.credentialIssuer(), cNonce);

        Map<String, Object> body = Map.of(
                FIELD_CREDENTIAL_CONFIGURATION_ID, ctx.credentialConfigId(),
                "proof", Map.of("proof_type", "jwt", "jwt", proofJwt)
        );

        ctx.log("POST " + credentialEndpoint + " (Credential-Request)");

        DPoPSigner dpopSigner = ctx.dpopSigner();
        Map<String, Object> credentialResponse = httpService.executeWithDpopNonceRetry(
                credentialEndpoint, "Credential-Request", options.useDpop() ? dpopSigner : null, ctx, nonce -> {
                    String authScheme = options.useDpop() ? "DPoP" : "Bearer";
                    RestClient.RequestBodySpec request = httpService.restClient().post()
                            .uri(credentialEndpoint)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header(HttpHeaders.AUTHORIZATION, authScheme + " " + ctx.accessToken());
                    if (options.useDpop()) {
                        request = request.header("DPoP", dpopSigner.generateDpopProof("POST", credentialEndpoint, nonce, ctx.accessToken()));
                    }
                    return request.body(body);
                });

        ctx.setCredentialResponse(credentialResponse);
        String rawCredential = extractRawCredential(credentialResponse, ctx);
        ctx.setRawCredential(rawCredential);
    }

    private String extractRawCredential(Map<String, Object> credentialResponse, FlowContext ctx) {
        if (!(credentialResponse.get(FIELD_CREDENTIAL) instanceof String raw)) {
            throw new IssuanceFlowException(
                    "Credential response does not contain a '" + FIELD_CREDENTIAL + "' field of type String "
                            + "(batch endpoint / 'credentials' array is intentionally not supported)",
                    ctx.logs());
        }
        return raw;
    }
}
