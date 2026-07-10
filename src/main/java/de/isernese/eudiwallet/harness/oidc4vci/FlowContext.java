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

import de.isernese.eudiwallet.harness.crypto.DPoPSigner;
import de.isernese.eudiwallet.harness.sdjwt.ParsedSdJwt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * State container for a single issuance run.
 * Stores all inputs, intermediate results, outputs, and logs.
 * Intentionally mutable; a FlowContext lives only within a single run and is not
 * shared across threads.
 */
public final class FlowContext {

    // Inputs
    private final String offerUri;
    private final String credentialConfigurationIdOverride;
    private final String txCode;

    // State / intermediate results
    private Map<String, Object> offer;
    private String credentialIssuer;
    private Map<String, Object> issuerMetadata;
    private Map<String, Object> authServerMetadata;
    private String credentialConfigId;
    private Map<String, Object> grant;
    private DPoPSigner dpopSigner;
    private Map<String, Object> tokenResponse;
    private String accessToken;
    private Map<String, Object> credentialResponse;
    private String rawCredential;
    private ParsedSdJwt parsedSdJwt;

    // Logs
    private final List<String> logs = new ArrayList<>();

    public FlowContext(String offerUri, String credentialConfigurationIdOverride, String txCode) {
        this.offerUri = offerUri;
        this.credentialConfigurationIdOverride = credentialConfigurationIdOverride;
        this.txCode = txCode;
    }

    public void log(String message) {
        logs.add(message);
    }

    public List<String> logs() {
        return Collections.unmodifiableList(logs);
    }

    // Getters and Setters for inputs and outputs

    public String offerUri() {
        return offerUri;
    }

    public String credentialConfigurationIdOverride() {
        return credentialConfigurationIdOverride;
    }

    public String txCode() {
        return txCode;
    }

    public Map<String, Object> offer() {
        return offer;
    }

    public void setOffer(Map<String, Object> offer) {
        this.offer = offer;
    }

    public String credentialIssuer() {
        return credentialIssuer;
    }

    public void setCredentialIssuer(String credentialIssuer) {
        this.credentialIssuer = credentialIssuer;
    }

    public Map<String, Object> issuerMetadata() {
        return issuerMetadata;
    }

    public void setIssuerMetadata(Map<String, Object> issuerMetadata) {
        this.issuerMetadata = issuerMetadata;
    }

    public Map<String, Object> authServerMetadata() {
        return authServerMetadata;
    }

    public void setAuthServerMetadata(Map<String, Object> authServerMetadata) {
        this.authServerMetadata = authServerMetadata;
    }

    public String credentialConfigId() {
        return credentialConfigId;
    }

    public void setCredentialConfigId(String credentialConfigId) {
        this.credentialConfigId = credentialConfigId;
    }

    public Map<String, Object> grant() {
        return grant;
    }

    public void setGrant(Map<String, Object> grant) {
        this.grant = grant;
    }

    public DPoPSigner dpopSigner() {
        return dpopSigner;
    }

    public void setDpopSigner(DPoPSigner dpopSigner) {
        this.dpopSigner = dpopSigner;
    }

    public Map<String, Object> tokenResponse() {
        return tokenResponse;
    }

    public void setTokenResponse(Map<String, Object> tokenResponse) {
        this.tokenResponse = tokenResponse;
    }

    public String accessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Map<String, Object> credentialResponse() {
        return credentialResponse;
    }

    public void setCredentialResponse(Map<String, Object> credentialResponse) {
        this.credentialResponse = credentialResponse;
    }

    public String rawCredential() {
        return rawCredential;
    }

    public void setRawCredential(String rawCredential) {
        this.rawCredential = rawCredential;
    }

    public ParsedSdJwt parsedSdJwt() {
        return parsedSdJwt;
    }

    public void setParsedSdJwt(ParsedSdJwt parsedSdJwt) {
        this.parsedSdJwt = parsedSdJwt;
    }
}
