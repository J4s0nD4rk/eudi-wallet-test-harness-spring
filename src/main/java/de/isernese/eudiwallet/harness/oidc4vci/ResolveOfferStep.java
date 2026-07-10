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

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static de.isernese.eudiwallet.harness.oidc4vci.Oidc4vciConstants.*;
import static de.isernese.eudiwallet.harness.oidc4vci.Oidc4vciHttpService.*;

/**
 * Step 1: Resolves the credential offer (inline or remote via HTTPS).
 */
@Component
public class ResolveOfferStep implements FlowStep {

    private final Oidc4vciHttpService httpService;

    public ResolveOfferStep(Oidc4vciHttpService httpService) {
        this.httpService = httpService;
    }

    @Override
    public void execute(FlowContext ctx, FlowOptions options) {
        URI uri = URI.create(ctx.offerUri());
        Map<String, String> queryParams = parseQuery(uri.getRawQuery());

        Map<String, Object> offer;
        String inlineOffer = queryParams.get(FIELD_CREDENTIAL_OFFER);
        if (inlineOffer != null) {
            ctx.log("Credential offer read inline from deep link.");
            offer = httpService.parseJsonObject(URLDecoder.decode(inlineOffer, StandardCharsets.UTF_8));
        } else {
            String offerUriParam = queryParams.get(FIELD_CREDENTIAL_OFFER_URI);
            if (offerUriParam != null) {
                String decoded = URLDecoder.decode(offerUriParam, StandardCharsets.UTF_8);
                ctx.log("Credential offer loaded from " + FIELD_CREDENTIAL_OFFER_URI + ".");
                offer = httpService.getJson(decoded, ctx);
            } else {
                throw new IssuanceFlowException(
                        "offerUri contains neither '" + FIELD_CREDENTIAL_OFFER + "' nor '" + FIELD_CREDENTIAL_OFFER_URI + "'",
                        ctx.logs());
            }
        }

        ctx.setOffer(offer);
        String credentialIssuer = requireString(offer, FIELD_CREDENTIAL_ISSUER,
                "Credential offer does not contain '" + FIELD_CREDENTIAL_ISSUER + "'", ctx);
        ctx.setCredentialIssuer(credentialIssuer);
    }
}
