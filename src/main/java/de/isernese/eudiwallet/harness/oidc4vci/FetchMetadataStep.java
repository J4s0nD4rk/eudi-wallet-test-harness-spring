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

import java.util.List;
import java.util.Map;

import static de.isernese.eudiwallet.harness.oidc4vci.Oidc4vciConstants.*;
import static de.isernese.eudiwallet.harness.oidc4vci.Oidc4vciHttpService.*;

/**
 * Step 2: Downloads issuer and authorization server metadata and selects the
 * credential configuration and grant to use.
 */
@Component
public class FetchMetadataStep implements FlowStep {

    private final Oidc4vciHttpService httpService;

    public FetchMetadataStep(Oidc4vciHttpService httpService) {
        this.httpService = httpService;
    }

    @Override
    public void execute(FlowContext ctx, FlowOptions options) {
        String baseIssuer = ctx.credentialIssuer();

        // 1. Fetch Issuer Metadata
        String issuerUrl = stripTrailingSlash(baseIssuer) + WELL_KNOWN_CREDENTIAL_ISSUER;
        ctx.log("Loading issuer metadata.");
        Map<String, Object> issuerMetadata = httpService.getJson(issuerUrl, ctx);
        ctx.setIssuerMetadata(issuerMetadata);

        // 2. Fetch Auth Server Metadata
        Map<String, Object> authServerMetadata = null;
        String base = stripTrailingSlash(baseIssuer);
        IssuanceFlowException lastError = null;
        for (String candidate : WELL_KNOWN_AUTH_SERVER_CANDIDATES) {
            try {
                ctx.log("Loading authorization server metadata: " + candidate);
                authServerMetadata = httpService.getJson(base + candidate, ctx);
                break;
            } catch (IssuanceFlowException e) {
                lastError = e;
            }
        }
        if (authServerMetadata == null) {
            throw new IssuanceFlowException(
                    "Authorization server metadata could not be loaded from any known well-known path ("
                            + String.join(", ", WELL_KNOWN_AUTH_SERVER_CANDIDATES) + ")",
                    ctx.logs(), lastError);
        }
        ctx.setAuthServerMetadata(authServerMetadata);

        // 3. Resolve credential config and grant
        String configId = selectCredentialConfigurationId(ctx.offer(), issuerMetadata, ctx.credentialConfigurationIdOverride(), ctx);
        ctx.setCredentialConfigId(configId);

        Map<String, Object> grant = extractPreAuthorizedGrant(ctx.offer(), ctx);
        ctx.setGrant(grant);
    }

    @SuppressWarnings("unchecked")
    private String selectCredentialConfigurationId(Map<String, Object> offer, Map<String, Object> issuerMetadata,
                                                     String overrideId, FlowContext ctx) {
        Map<String, Object> supported = (Map<String, Object>) issuerMetadata.getOrDefault(
                FIELD_CREDENTIAL_CONFIGURATIONS_SUPPORTED, Map.of());

        if (overrideId != null) {
            if (!supported.containsKey(overrideId)) {
                throw new IssuanceFlowException(
                        "Requested credentialConfigurationId '" + overrideId + "' is not supported by the issuer",
                        ctx.logs());
            }
            return overrideId;
        }

        List<String> offeredIds = (List<String>) offer.getOrDefault(FIELD_CREDENTIAL_CONFIGURATION_IDS, List.of());
        return offeredIds.stream()
                .filter(supported::containsKey)
                .findFirst()
                .orElseThrow(() -> new IssuanceFlowException(
                        "None of the " + FIELD_CREDENTIAL_CONFIGURATION_IDS
                                + " offered in the credential offer are supported by the issuer: " + offeredIds, ctx.logs()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPreAuthorizedGrant(Map<String, Object> offer, FlowContext ctx) {
        Object grantsObj = offer.get(FIELD_GRANTS);
        if (!(grantsObj instanceof Map<?, ?> grants) || !grants.containsKey(PRE_AUTH_GRANT)) {
            throw new IssuanceFlowException(
                    "Credential offer does not contain a pre-authorized_code grant (" + PRE_AUTH_GRANT
                            + ") — the harness intentionally covers only this flow in the MVP (see README.md, assumptions)",
                    ctx.logs());
        }
        return (Map<String, Object>) grants.get(PRE_AUTH_GRANT);
    }
}
