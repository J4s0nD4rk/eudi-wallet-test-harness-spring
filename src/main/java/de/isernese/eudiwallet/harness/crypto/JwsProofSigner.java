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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Shared signing logic for the two proof-of-possession JWTs of the harness
 * (OIDC4VCI proof JWT and DPoP proof, RFC 9449): both are ES256-signed JWTs with
 * an embedded public JWK in the header; only the claims differ.
 * Extracted from {@code ProofJwtBuilder} and {@code DPoPProofBuilder}, which
 * previously duplicated identical header construction, signing, and
 * serialization code (DRY review finding).
 */
final class JwsProofSigner {

    private JwsProofSigner() {
    }

    static String sign(DPoPKey key, String typ, JWTClaimsSet claims) {
        try {
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                    .type(new JOSEObjectType(typ))
                    .jwk(key.ecKey().toPublicJWK())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(new ECDSASigner(key.ecKey()));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Could not sign JWT (typ=" + typ + ")", e);
        }
    }
}
