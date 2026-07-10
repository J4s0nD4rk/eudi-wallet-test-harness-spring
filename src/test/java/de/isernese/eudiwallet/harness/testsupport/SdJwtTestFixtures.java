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
package de.isernese.eudiwallet.harness.testsupport;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Test fixtures: builds signed SD-JWT VC compacts (with disclosures and optional
 * x5c header) for the signature verification and happy-path tests. Test-scoped
 * only, not part of the harness runtime code.
 */
public final class SdJwtTestFixtures {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private SdJwtTestFixtures() {
    }

    public record SignedSdJwt(String compact, ECKey signingKey, X509Certificate certificate) {
    }

    /**
     * @param plainClaims      Claims placed directly (not selectively disclosable) in the issuer JWT
     * @param disclosedClaims  Claims offloaded as disclosures ({@code _sd} array)
     * @param signingKey       Key used to actually sign
     * @param certKeyOrNull    Key whose public part is in the x5c certificate
     *                         (null = no x5c header; different from signingKey = simulates signature failure)
     */
    public static SignedSdJwt build(Map<String, Object> plainClaims, Map<String, String> disclosedClaims,
                                     ECKey signingKey, ECKey certKeyOrNull) throws Exception {
        List<String> discloseParts = new ArrayList<>();
        List<String> sdDigests = new ArrayList<>();
        for (Map.Entry<String, String> entry : disclosedClaims.entrySet()) {
            String salt = Base64URL.encode(randomBytes()).toString();
            String disclosureJson = "[\"" + salt + "\",\"" + entry.getKey() + "\",\"" + entry.getValue() + "\"]";
            String encoded = Base64URL.encode(disclosureJson.getBytes(StandardCharsets.UTF_8)).toString();
            discloseParts.add(encoded);
            sdDigests.add(digest(encoded));
        }

        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();
        plainClaims.forEach(claimsBuilder::claim);
        claimsBuilder.claim("_sd", sdDigests);
        claimsBuilder.claim("_sd_alg", "sha-256");

        JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .type(new JOSEObjectType("dc+sd-jwt"));

        X509Certificate certificate = null;
        if (certKeyOrNull != null) {
            certificate = selfSignedCertificate(certKeyOrNull);
            headerBuilder.x509CertChain(List.of(Base64.encode(certificate.getEncoded())));
        }

        SignedJWT signedJWT = new SignedJWT(headerBuilder.build(), claimsBuilder.build());
        signedJWT.sign(new ECDSASigner(signingKey));

        String compact = signedJWT.serialize() + "~" + String.join("~", discloseParts) + "~";
        return new SignedSdJwt(compact, signingKey, certificate);
    }

    public static ECKey generateKey() throws Exception {
        return new ECKeyGenerator(Curve.P_256).generate();
    }

    private static X509Certificate selfSignedCertificate(ECKey key) throws Exception {
        KeyPair keyPair = key.toKeyPair();
        Instant now = Instant.now();
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                new X500Name("CN=Test Issuer"),
                BigInteger.valueOf(now.toEpochMilli()),
                Date.from(now.minusSeconds(60)),
                Date.from(now.plusSeconds(3600)),
                new X500Name("CN=Test Issuer"),
                keyPair.getPublic()
        );
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(keyPair.getPrivate());
        X509CertificateHolder holder = certBuilder.build(signer);
        return new JcaX509CertificateConverter().getCertificate(holder);
    }

    private static byte[] randomBytes() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private static String digest(String rawDisclosure) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(rawDisclosure.getBytes(StandardCharsets.US_ASCII));
        return Base64URL.encode(hash).toString();
    }
}
