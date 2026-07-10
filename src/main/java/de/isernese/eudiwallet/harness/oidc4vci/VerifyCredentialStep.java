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

import com.nimbusds.jose.jwk.JWKSet;
import de.isernese.eudiwallet.harness.sdjwt.ParsedSdJwt;
import de.isernese.eudiwallet.harness.sdjwt.SdJwtParser;
import org.springframework.stereotype.Component;

import java.util.Map;

import static de.isernese.eudiwallet.harness.oidc4vci.Oidc4vciConstants.*;

/**
 * Step 5: Downloads JWKS and parses/verifies the received SD-JWT credential.
 */
@Component
public class VerifyCredentialStep implements FlowStep {

    private final Oidc4vciHttpService httpService;

    public VerifyCredentialStep(Oidc4vciHttpService httpService) {
        this.httpService = httpService;
    }

    @Override
    public void execute(FlowContext ctx, FlowOptions options) {
        JWKSet jwks = maybeFetchJwks(ctx.issuerMetadata(), ctx);
        ParsedSdJwt parsed = SdJwtParser.parse(ctx.rawCredential(), jwks, options.verifySdJwtSignature());
        ctx.setParsedSdJwt(parsed);

        if (options.verifySdJwtSignature()) {
            if (Boolean.TRUE.equals(parsed.signatureVerified())) {
                ctx.log("SD-JWT signature successfully verified (x5c/JWKS).");
            } else {
                throw new IssuanceFlowException("SD-JWT signature verification failed!", ctx.logs());
            }
        }
    }

    private JWKSet maybeFetchJwks(Map<String, Object> issuerMetadata, FlowContext ctx) {
        if (!(issuerMetadata.get(FIELD_JWKS_URI) instanceof String url)) {
            return null;
        }
        try {
            ctx.log("Loading JWKS (fallback, x5c preferred): " + url);
            String body = httpService.restClient().get().uri(url).retrieve().body(String.class);
            return JWKSet.parse(body);
        } catch (Exception e) {
            ctx.log("JWKS could not be loaded, continuing without JWKS fallback: " + e.getMessage());
            return null;
        }
    }
}
