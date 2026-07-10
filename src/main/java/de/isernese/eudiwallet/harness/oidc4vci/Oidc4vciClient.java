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

import de.isernese.eudiwallet.harness.sdjwt.Disclosure;
import de.isernese.eudiwallet.harness.sdjwt.ParsedSdJwt;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Core service: simulates the wallet side of an OIDC4VCI pre-authorized code flow
 * (SD-JWT VC).
 * Refactored to a modular pipeline pattern (SOLID/open-closed principle). The
 * individual steps are implemented as standalone FlowSteps and executed in sequence.
 */
@Service
public class Oidc4vciClient {

    private final List<FlowStep> steps;

    public Oidc4vciClient(
            ResolveOfferStep resolveOfferStep,
            FetchMetadataStep fetchMetadataStep,
            TokenExchangeStep tokenExchangeStep,
            CredentialRequestStep credentialRequestStep,
            VerifyCredentialStep verifyCredentialStep) {
        
        this.steps = List.of(
                resolveOfferStep,
                fetchMetadataStep,
                tokenExchangeStep,
                credentialRequestStep,
                verifyCredentialStep
        );
    }

    public IssuanceResult run(String offerUri, String credentialConfigurationIdOverride, String txCode, FlowOptions options) {
        FlowContext ctx = new FlowContext(offerUri, credentialConfigurationIdOverride, txCode);

        for (FlowStep step : steps) {
            step.execute(ctx, options);
        }

        ParsedSdJwt parsed = ctx.parsedSdJwt();
        return IssuanceResult.success(
                ctx.logs(),
                ctx.issuerMetadata(),
                ctx.authServerMetadata(),
                ctx.tokenResponse(),
                parsed.rawCompact(),
                parsed.decodedClaims(),
                parsed.disclosures().stream().map(Disclosure::asMap).toList()
        );
    }
}
