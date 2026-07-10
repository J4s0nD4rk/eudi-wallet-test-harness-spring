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

import java.util.List;
import java.util.Map;

/**
 * Result of an issuance run — serves both as the internal domain result (from
 * {@link Oidc4vciClient#run}) and directly as the HTTP response body (Jackson
 * serializes record fields as camelCase directly, without additional alias
 * configuration). {@code runId} is intentionally set by the controller only
 * (generating a run ID is a web-layer responsibility, not part of the flow logic
 * itself).
 */
public record IssuanceResult(
        String runId,
        IssuanceStatus status,
        String error,
        List<String> logs,
        Map<String, Object> issuerMetadata,
        Map<String, Object> authServerMetadata,
        Map<String, Object> tokenResponse,
        String rawSdJwt,
        Map<String, Object> decodedClaims,
        List<Map<String, Object>> disclosures
) {
    public IssuanceResult withRunId(String newRunId) {
        return new IssuanceResult(newRunId, status, error, logs, issuerMetadata, authServerMetadata,
                tokenResponse, rawSdJwt, decodedClaims, disclosures);
    }

    public static IssuanceResult success(List<String> logs, Map<String, Object> issuerMetadata,
                                          Map<String, Object> authServerMetadata, Map<String, Object> tokenResponse,
                                          String rawSdJwt, Map<String, Object> decodedClaims,
                                          List<Map<String, Object>> disclosures) {
        return new IssuanceResult(null, IssuanceStatus.SUCCESS, null, logs, issuerMetadata, authServerMetadata,
                tokenResponse, rawSdJwt, decodedClaims, disclosures);
    }

    public static IssuanceResult failed(String error, List<String> logs) {
        return new IssuanceResult(null, IssuanceStatus.FAILED, error, logs, null, null, null, null, null, null);
    }
}
