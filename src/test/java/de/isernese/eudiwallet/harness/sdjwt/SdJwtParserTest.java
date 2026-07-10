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

import com.nimbusds.jose.jwk.ECKey;
import de.isernese.eudiwallet.harness.testsupport.SdJwtTestFixtures;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SdJwtParserTest {

    @Test
    void parsesDisclosuresAndMatchesSdDigests() throws Exception {
        ECKey key = SdJwtTestFixtures.generateKey();
        var signed = SdJwtTestFixtures.build(
                Map.of("iss", "https://issuer.example",
                        "vct", "https://issuer.example/credentials/example-credential/v1"),
                Map.of("exampleClaimA", "example-value-a", "exampleClaimB", "example-value-b"),
                key, null);

        ParsedSdJwt parsed = SdJwtParser.parse(signed.compact(), null, false);

        assertThat(parsed.decodedClaims()).containsEntry("exampleClaimA", "example-value-a");
        assertThat(parsed.decodedClaims()).containsEntry("exampleClaimB", "example-value-b");
        assertThat(parsed.disclosures()).hasSize(2);
        assertThat(parsed.disclosures()).allMatch(d -> Boolean.TRUE.equals(d.digestMatchesSdArray()));
    }

    @Test
    void signatureVerificationIsSkippedWhenFlagIsFalseEvenWithX5cPresent() throws Exception {
        // Whether verification happens at all depends ONLY on the explicit flag —
        // not on whether x5c happens to be present.
        ECKey key = SdJwtTestFixtures.generateKey();
        var signed = SdJwtTestFixtures.build(Map.of("iss", "https://issuer.example"), Map.of(), key, key);

        ParsedSdJwt parsed = SdJwtParser.parse(signed.compact(), null, false);

        assertThat(parsed.signatureVerified()).isNull();
    }

    @Test
    void verifiesSignatureViaX5cWhenCertMatchesSigningKey() throws Exception {
        ECKey key = SdJwtTestFixtures.generateKey();
        var signed = SdJwtTestFixtures.build(Map.of("iss", "https://issuer.example"), Map.of(), key, key);

        ParsedSdJwt parsed = SdJwtParser.parse(signed.compact(), null, true);

        assertThat(parsed.signatureVerified()).isTrue();
    }

    @Test
    void signatureVerificationFailsWhenCertDoesNotMatchSigningKey() throws Exception {
        ECKey signingKey = SdJwtTestFixtures.generateKey();
        ECKey unrelatedCertKey = SdJwtTestFixtures.generateKey();
        var signed = SdJwtTestFixtures.build(Map.of("iss", "https://issuer.example"), Map.of(), signingKey, unrelatedCertKey);

        ParsedSdJwt parsed = SdJwtParser.parse(signed.compact(), null, true);

        assertThat(parsed.signatureVerified()).isFalse();
    }
}
