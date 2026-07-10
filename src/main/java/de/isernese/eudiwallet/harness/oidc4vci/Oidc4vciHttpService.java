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
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Helper service for HTTP calls, JSON parsing, and query-string processing.
 * Shared by the individual FlowSteps (DRY).
 */
@Service
public class Oidc4vciHttpService {

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public Oidc4vciHttpService(RestClient harnessRestClient, JsonMapper jsonMapper) {
        this.restClient = harnessRestClient;
        this.jsonMapper = jsonMapper;
    }

    public RestClient restClient() {
        return restClient;
    }

    public JsonMapper jsonMapper() {
        return jsonMapper;
    }

    public Map<String, Object> getJson(String url, FlowContext ctx) {
        ctx.log("GET " + url);
        try {
            return restClient.get().uri(url).retrieve().body(Oidc4vciConstants.JSON_OBJECT);
        } catch (HttpClientErrorException e) {
            throw new IssuanceFlowException(
                    "GET " + url + " failed: HTTP " + e.getStatusCode().value(), ctx.logs(), e);
        } catch (RestClientException e) {
            throw new IssuanceFlowException("GET " + url + " failed: " + e.getMessage(), ctx.logs(), e);
        }
    }

    public Map<String, Object> parseJsonObject(String json) {
        try {
            return jsonMapper.readValue(json, jsonMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse JSON object: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> executeWithDpopNonceRetry(
            String url,
            String requestDescription,
            DPoPSigner dpopSigner,
            FlowContext ctx,
            Function<String, RestClient.RequestHeadersSpec<?>> requestFactory) {
        String nonce = null;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return requestFactory.apply(nonce).retrieve().body(Oidc4vciConstants.JSON_OBJECT);
            } catch (HttpClientErrorException e) {
                String retryNonce = tryExtractDpopRetryNonce(e, attempt, dpopSigner);
                if (retryNonce == null) {
                    throw tokenOrCredentialError(url, e, ctx);
                }
                nonce = retryNonce;
                ctx.log("Server requires DPoP-Nonce (RFC 9449) for " + requestDescription + " — retrying with nonce.");
            } catch (RestClientException e) {
                throw new IssuanceFlowException("POST " + url + " failed: " + e.getMessage(), ctx.logs(), e);
            }
        }
        throw new IssuanceFlowException("POST " + url + " failed after DPoP nonce retry", ctx.logs());
    }

    private String tryExtractDpopRetryNonce(HttpClientErrorException e, int attempt, DPoPSigner dpopSigner) {
        if (attempt != 1 || dpopSigner == null) {
            return null;
        }
        boolean isUseDpopNonceError = e.getStatusCode().value() == 400
                && e.getResponseBodyAsString().contains("use_dpop_nonce");
        if (!isUseDpopNonceError) {
            return null;
        }
        return e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("DPoP-Nonce") : null;
    }

    private IssuanceFlowException tokenOrCredentialError(String url, HttpClientErrorException e, FlowContext ctx) {
        return new IssuanceFlowException(
                "POST " + url + " failed: HTTP " + e.getStatusCode().value() + " " + e.getResponseBodyAsString(),
                ctx.logs(), e);
    }

    public static Map<String, String> parseQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) {
                continue;
            }
            result.put(pair.substring(0, idx), pair.substring(idx + 1));
        }
        return result;
    }

    public static String requireString(Map<String, Object> map, String key, String errorMessage, FlowContext ctx) {
        if (!(map.get(key) instanceof String s) || s.isBlank()) {
            throw new IssuanceFlowException(errorMessage, ctx.logs());
        }
        return s;
    }

    public static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
