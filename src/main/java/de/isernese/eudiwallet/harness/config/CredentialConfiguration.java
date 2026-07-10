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
package de.isernese.eudiwallet.harness.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * A test scenario from credential-configs.yaml (e.g., "example_credential").
 * Intentionally use-case-neutral: which credential type, which claims, and which
 * vct are relevant is determined by the project testing the EUDI credential issuer
 * — this harness does not prescribe a specific use case.
 * expectedClaims is a placeholder catalog; the harness checks only structurally
 * (which claims were disclosed), not for full domain coverage.
 */
public record CredentialConfiguration(
        @JsonProperty("issuerCredentialConfigurationId") String issuerCredentialConfigurationId,
        @JsonProperty("format") String format,
        @JsonProperty("expectedVct") String expectedVct,
        @JsonProperty("txCodeRequired") boolean txCodeRequired,
        @JsonProperty("expectedClaims") List<String> expectedClaims,
        @JsonProperty("sampleInput") Map<String, Object> sampleInput
) {
}
