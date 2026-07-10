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
package de.isernese.eudiwallet.harness.crypto;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;

import java.util.UUID;

/**
 * Ephemeral EC P-256 key pair for a single issuance run — used both for the
 * OIDC4VCI proof JWT (holder binding) and, if enabled, for DPoP (RFC 9449).
 * Nimbus's {@link ECKey} carries the private AND public part in a single
 * instance; {@code toPublicJWK()} returns the JWK representation for the
 * "jwk" header claim directly — saving custom PEM/JWK handling.
 */
public record DPoPKey(ECKey ecKey) {

    public static DPoPKey generate() {
        try {
            ECKey key = new ECKeyGenerator(Curve.P_256)
                    .keyID(UUID.randomUUID().toString())
                    .generate();
            return new DPoPKey(key);
        } catch (Exception e) {
            throw new IllegalStateException("Could not generate EC key pair", e);
        }
    }
}
