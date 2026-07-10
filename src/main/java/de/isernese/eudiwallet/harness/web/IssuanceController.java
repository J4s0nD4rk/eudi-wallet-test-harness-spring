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
package de.isernese.eudiwallet.harness.web;

import de.isernese.eudiwallet.harness.config.CredentialConfigLoader;
import de.isernese.eudiwallet.harness.config.CredentialConfiguration;
import de.isernese.eudiwallet.harness.config.HarnessProperties;
import de.isernese.eudiwallet.harness.oidc4vci.FlowOptions;
import de.isernese.eudiwallet.harness.oidc4vci.IssuanceFlowException;
import de.isernese.eudiwallet.harness.oidc4vci.IssuanceResult;
import de.isernese.eudiwallet.harness.oidc4vci.Oidc4vciClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST layer of the harness. Health check is served via Spring Boot Actuator
 * ({@code GET /actuator/health}), see application.yml.
 */
@RestController
public class IssuanceController {

    private final Oidc4vciClient client;
    private final HarnessProperties properties;
    private final CredentialConfigLoader credentialConfigLoader;

    public IssuanceController(Oidc4vciClient client, HarnessProperties properties,
                               CredentialConfigLoader credentialConfigLoader) {
        this.client = client;
        this.properties = properties;
        this.credentialConfigLoader = credentialConfigLoader;
    }

    @GetMapping("/config/credential-configurations")
    public Map<String, CredentialConfiguration> credentialConfigurations() {
        return credentialConfigLoader.load();
    }

    @PostMapping("/issuance/run")
    public IssuanceResult runIssuance(@RequestBody RunIssuanceRequest request) {
        if (request.offerUri() == null || request.offerUri().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "offerUri is required");
        }

        String runId = UUID.randomUUID().toString();
        FlowOptions options = flowOptionsFor(request);

        IssuanceResult result;
        try {
            result = client.run(request.offerUri(), request.credentialConfigurationId(), request.txCode(), options);
        } catch (IssuanceFlowException e) {
            result = IssuanceResult.failed(e.getMessage(), e.logs());
        } catch (Exception e) {
            result = IssuanceResult.failed("Unexpected error: " + e.getMessage(), List.of());
        }
        return result.withRunId(runId);
    }

    /**
     * Maps HarnessProperties (global defaults) + per-request overrides onto a
     * FlowOptions object — the single place where this happens.
     */
    private FlowOptions flowOptionsFor(RunIssuanceRequest request) {
        FlowOptions base = new FlowOptions(
                properties.verifyTls(),
                properties.customCaBundle(),
                properties.httpTimeout(),
                properties.verifySdJwtSignature(),
                properties.useDpop()
        );
        FlowOptions withDpopOverride = request.useDpop() != null ? base.withUseDpop(request.useDpop()) : base;
        return request.verifySdJwtSignature() != null
                ? withDpopOverride.withVerifySdJwtSignature(request.verifySdJwtSignature())
                : withDpopOverride;
    }
}
