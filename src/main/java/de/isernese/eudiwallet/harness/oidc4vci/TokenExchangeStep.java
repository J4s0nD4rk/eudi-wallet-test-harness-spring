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
import de.isernese.eudiwallet.harness.crypto.DPoPSignerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static de.isernese.eudiwallet.harness.oidc4vci.Oidc4vciConstants.*;
import static de.isernese.eudiwallet.harness.oidc4vci.Oidc4vciHttpService.*;

/**
 * Step 3: Optionally generates a DPoP key and exchanges the pre-authorized code
 * for an access token.
 */
@Component
public class TokenExchangeStep implements FlowStep {

    private final Oidc4vciHttpService httpService;
    private final DPoPSignerFactory signerFactory;

    public TokenExchangeStep(Oidc4vciHttpService httpService, DPoPSignerFactory signerFactory) {
        this.httpService = httpService;
        this.signerFactory = signerFactory;
    }

    @Override
    public void execute(FlowContext ctx, FlowOptions options) {
        // 1. Generate DPoPSigner
        DPoPSigner dpopSigner = signerFactory.createSigner();
        ctx.setDpopSigner(dpopSigner);
        if (options.useDpop()) {
            ctx.log("DPoP enabled (RFC 9449) — ephemeral key generated for this run.");
        }

        // 2. Perform token request
        String tokenEndpoint = requireString(ctx.authServerMetadata(), FIELD_TOKEN_ENDPOINT,
                "Authorization server metadata does not contain " + FIELD_TOKEN_ENDPOINT, ctx);
        String preAuthorizedCode = requireString(ctx.grant(), FIELD_PRE_AUTHORIZED_CODE,
                "Grant does not contain '" + FIELD_PRE_AUTHORIZED_CODE + "'", ctx);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", PRE_AUTH_GRANT);
        form.add(FIELD_PRE_AUTHORIZED_CODE, preAuthorizedCode);
        if (ctx.txCode() != null) {
            form.add(FIELD_TX_CODE, ctx.txCode());
        }

        ctx.log("POST " + tokenEndpoint + " (Token-Request, Pre-Authorized-Code-Grant)");

        Map<String, Object> tokenResponse = httpService.executeWithDpopNonceRetry(
                tokenEndpoint, "Token-Request", options.useDpop() ? dpopSigner : null, ctx, nonce -> {
                    RestClient.RequestBodySpec request = httpService.restClient().post()
                            .uri(tokenEndpoint)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED);
                    if (options.useDpop()) {
                        request = request.header("DPoP", dpopSigner.generateDpopProof("POST", tokenEndpoint, nonce, null));
                    }
                    return request.body(form);
                });

        ctx.setTokenResponse(tokenResponse);
        String accessToken = requireString(tokenResponse, FIELD_ACCESS_TOKEN,
                "Token response does not contain " + FIELD_ACCESS_TOKEN, ctx);
        ctx.setAccessToken(accessToken);
    }
}
