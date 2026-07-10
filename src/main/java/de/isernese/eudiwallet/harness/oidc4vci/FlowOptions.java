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

import java.time.Duration;
import java.util.Map;

/**
 * Bundles all run parameters of an issuance flow in a single parameter object instead
 * of passing 8–9 individual parameters through the method chain (KISS/SOLID).
 * <p>
 * Immutable via Java record; per-request overrides (useDpop/verifySdJwtSignature,
 * see IssuanceController) create a new instance via the with* methods instead of
 * mutating state.
 */
public record FlowOptions(
        boolean verifyTls,
        String customCaBundle,
        Duration httpTimeout,
        boolean verifySdJwtSignature,
        boolean useDpop
) {
    public FlowOptions withUseDpop(boolean value) {
        return new FlowOptions(verifyTls, customCaBundle, httpTimeout, verifySdJwtSignature, value);
    }

    public FlowOptions withVerifySdJwtSignature(boolean value) {
        return new FlowOptions(verifyTls, customCaBundle, httpTimeout, value, useDpop);
    }

    /** true if TLS verification (optionally including a custom CA bundle) is enabled for this run. */
    public boolean tlsVerificationEnabled() {
        return verifyTls || (customCaBundle != null && !customCaBundle.isBlank());
    }
}
