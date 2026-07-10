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
package de.isernese.eudiwallet.harness.sdjwt;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal SD-JWT VC parser (IETF draft-ietf-oauth-sd-jwt-vc), compact form
 * {@code <issuer-jwt>~<disclosure>~...~[<kb-jwt>]}, intentionally with limited MVP scope:
 * only object-valued disclosures {@code [salt, claimName, value]}, no
 * array-element selective disclosure, no key-binding JWT verification (see
 * README.md, assumptions).
 * <p>
 * IMPORTANT: whether the signature is verified at all depends solely on the
 * explicit {@code verifySignature} flag — NOT on whether an x5c header happens
 * to be present. Otherwise, "verifySdJwtSignature: false" would be silently
 * ignored as soon as the issuer (HAIP-compliant) sends x5c.
 */
public final class SdJwtParser {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static final String CLAIM_SD = "_sd";
    private static final String CLAIM_SD_ALG = "_sd_alg";
    private static final String DEFAULT_SD_ALG = "sha-256";

    private SdJwtParser() {
    }

    public static ParsedSdJwt parse(String compact, JWKSet jwks, boolean verifySignature) {
        try {
            boolean hasKeyBindingJwt = !compact.endsWith("~");
            String[] segments = compact.split("~", -1);
            String issuerJwtCompact = segments[0];
            int disclosureEnd = hasKeyBindingJwt ? segments.length - 1 : segments.length;

            SignedJWT signedJwt = SignedJWT.parse(issuerJwtCompact);
            Map<String, Object> header = signedJwt.getHeader().toJSONObject();
            Map<String, Object> payload = new LinkedHashMap<>(signedJwt.getJWTClaimsSet().toJSONObject());

            @SuppressWarnings("unchecked")
            List<String> sdDigests = (List<String>) payload.getOrDefault(CLAIM_SD, List.of());
            String sdAlg = (String) payload.getOrDefault(CLAIM_SD_ALG, DEFAULT_SD_ALG);

            List<Disclosure> disclosures = new ArrayList<>();
            Map<String, Object> decodedClaims = new LinkedHashMap<>(payload);
            decodedClaims.remove(CLAIM_SD);
            decodedClaims.remove(CLAIM_SD_ALG);

            for (int i = 1; i < disclosureEnd; i++) {
                String raw = segments[i];
                if (raw.isBlank()) {
                    continue;
                }
                Disclosure disclosure = decodeDisclosure(raw, sdAlg, sdDigests);
                disclosures.add(disclosure);
                if (Boolean.TRUE.equals(disclosure.digestMatchesSdArray())) {
                    decodedClaims.put(disclosure.claimName(), disclosure.value());
                }
            }

            Boolean signatureVerified = null;
            if (verifySignature) {
                @SuppressWarnings("unchecked")
                List<Object> x5c = (List<Object>) header.get("x5c");
                if (x5c != null && !x5c.isEmpty()) {
                    signatureVerified = verifyViaX5c(signedJwt, x5c);
                } else if (jwks != null) {
                    signatureVerified = verifyViaJwks(signedJwt, header, jwks);
                } else {
                    signatureVerified = false;
                }
            }

            return new ParsedSdJwt(compact, header, payload, decodedClaims, disclosures, signatureVerified);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Could not parse SD-JWT", e);
        }
    }

    private static Disclosure decodeDisclosure(String raw, String sdAlg, List<String> sdDigests) {
        byte[] jsonBytes = Base64.getUrlDecoder().decode(padBase64Url(raw));
        List<?> array = JSON.readValue(new String(jsonBytes, StandardCharsets.UTF_8), List.class);
        if (array.size() != 3) {
            throw new IllegalArgumentException(
                    "Only object-valued disclosures ([salt, claimName, value]) are supported, received: "
                            + array.size() + " elements");
        }
        String salt = String.valueOf(array.get(0));
        String claimName = String.valueOf(array.get(1));
        Object value = array.get(2);

        String digest = digest(raw, sdAlg);
        boolean matches = sdDigests.contains(digest);
        return new Disclosure(raw, salt, claimName, value, digest, matches);
    }

    private static String digest(String rawDisclosure, String sdAlg) {
        if (!DEFAULT_SD_ALG.equalsIgnoreCase(sdAlg)) {
            throw new IllegalArgumentException(
                    "Unsupported _sd_alg: " + sdAlg + " (only sha-256 supported, see README.md assumptions)");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawDisclosure.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Digest computation failed", e);
        }
    }

    private static boolean verifyViaX5c(SignedJWT signedJwt, List<Object> x5c) {
        try {
            byte[] certDer = Base64.getDecoder().decode(String.valueOf(x5c.get(0)));
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certDer));
            PublicKey publicKey = cert.getPublicKey();
            if (!(publicKey instanceof ECPublicKey ecPublicKey)) {
                throw new IllegalArgumentException("x5c certificate does not contain an EC key (only ES256 supported)");
            }
            return signedJwt.verify(new ECDSAVerifier(ecPublicKey));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean verifyViaJwks(SignedJWT signedJwt, Map<String, Object> header, JWKSet jwks) {
        try {
            String kid = (String) header.get("kid");
            JWK jwk = kid != null ? jwks.getKeyByKeyId(kid)
                    : (jwks.getKeys().isEmpty() ? null : jwks.getKeys().get(0));
            if (jwk == null) {
                return false;
            }
            return signedJwt.verify(new ECDSAVerifier(jwk.toECKey()));
        } catch (Exception e) {
            return false;
        }
    }

    private static String padBase64Url(String value) {
        int padding = (4 - value.length() % 4) % 4;
        return value + "=".repeat(padding);
    }
}
