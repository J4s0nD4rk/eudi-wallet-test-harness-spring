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

import java.util.List;
import java.util.Map;

/**
 * Result of {@link SdJwtParser#parse}. {@code signatureVerified} is {@code null}
 * as long as no verification was requested (verifySignature=false) — intentionally
 * distinct from {@code false} (verification attempted and failed).
 */
public record ParsedSdJwt(
        String rawCompact,
        Map<String, Object> header,
        Map<String, Object> issuerSignedPayload,
        Map<String, Object> decodedClaims,
        List<Disclosure> disclosures,
        Boolean signatureVerified
) {
}
