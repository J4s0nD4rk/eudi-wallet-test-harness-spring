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

/**
 * Incoming issuance request. Java records are already camelCase (offerUri,
 * credentialConfigurationId, txCode, useDpop, verifySdJwtSignature); Jackson 3 binds
 * the record component names directly, without additional {@code @JsonProperty} aliases.
 * <p>
 * useDpop/verifySdJwtSignature are intentionally {@link Boolean} (not {@code boolean}):
 * {@code null} means "no override, use the harness-global default from application.yml"
 * — this allows testing the same harness against a credential issuer at different
 * maturity levels without restarting.
 */
public record RunIssuanceRequest(
        String offerUri,
        String credentialConfigurationId,
        String txCode,
        Boolean useDpop,
        Boolean verifySdJwtSignature
) {
}
