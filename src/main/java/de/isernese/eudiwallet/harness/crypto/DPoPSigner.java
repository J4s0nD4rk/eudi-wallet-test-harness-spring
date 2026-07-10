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

/**
 * Abstraction for cryptographic DPoP and holder-binding signing (SOLID / dependency inversion).
 * Decouples the OIDC4VCI flows entirely from concrete libraries (such as Nimbus)
 * and the key algorithms used (e.g., ES256).
 */
public interface DPoPSigner {

    /**
     * Generates a DPoP proof for outgoing HTTP requests per RFC 9449.
     *
     * @param method      HTTP method (e.g., "POST")
     * @param url         Target URL of the request
     * @param nonce       Server-provided nonce (optional)
     * @param accessToken Access token (optional, for DPoP key binding)
     * @return The Base64URL-encoded JWS string
     */
    String generateDpopProof(String method, String url, String nonce, String accessToken);

    /**
     * Generates a holder binding proof (JWT) for the credential request per OIDC4VCI.
     *
     * @param audience The issuer URL (aud)
     * @param nonce    c_nonce provided by the token endpoint
     * @return The Base64URL-encoded JWS string (typ: openid4vci-proof+jwt)
     */
    String generateHolderBindingProof(String audience, String nonce);
}
