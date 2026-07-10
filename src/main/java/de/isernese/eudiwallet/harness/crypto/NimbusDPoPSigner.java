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

import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Concrete implementation of {@link DPoPSigner} using Nimbus and EC P-256.
 */
public class NimbusDPoPSigner implements DPoPSigner {

    public static final String DPOP_JWT_TYP = "dpop+jwt";
    public static final String PROOF_JWT_TYP = "openid4vci-proof+jwt";

    private final DPoPKey key;

    public NimbusDPoPSigner(DPoPKey key) {
        if (key == null) {
            throw new IllegalArgumentException("DPoPKey must not be null");
        }
        this.key = key;
    }

    @Override
    public String generateDpopProof(String method, String url, String nonce, String accessToken) {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .jwtID(UUID.randomUUID().toString())
                .claim("htm", method)
                .claim("htu", url)
                .issueTime(Date.from(Instant.now()));
        if (nonce != null) {
            claims.claim("nonce", nonce);
        }
        if (accessToken != null) {
            claims.claim("ath", accessTokenHash(accessToken));
        }
        return JwsProofSigner.sign(key, DPOP_JWT_TYP, claims.build());
    }

    @Override
    public String generateHolderBindingProof(String audience, String nonce) {
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .audience(audience)
                .issueTime(Date.from(Instant.now()));
        if (nonce != null) {
            claims.claim("nonce", nonce);
        }
        return JwsProofSigner.sign(key, PROOF_JWT_TYP, claims.build());
    }

    private static String accessTokenHash(String accessToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(accessToken.getBytes(StandardCharsets.UTF_8));
            return Base64URL.encode(hash).toString();
        } catch (Exception e) {
            throw new IllegalStateException("Could not compute 'ath' claim", e);
        }
    }
}
