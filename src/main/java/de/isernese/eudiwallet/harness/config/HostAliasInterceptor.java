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
package de.isernese.eudiwallet.harness.config;

import de.isernese.eudiwallet.harness.oidc4vci.HostAliasApplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

import java.io.IOException;
import java.net.URI;

/**
 * HTTP client interceptor (RestClient) that intercepts all outgoing HTTP requests
 * and rewrites hostnames according to the configured aliases.
 * Removes the manual coupling to HostAliasApplier in the client (separation of concerns).
 */
public class HostAliasInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(HostAliasInterceptor.class);
    private final HostAliasesResolver hostAliasesResolver;

    public HostAliasInterceptor(HostAliasesResolver hostAliasesResolver) {
        this.hostAliasesResolver = hostAliasesResolver;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        URI originalUri = request.getURI();
        String rewrittenUrl = HostAliasApplier.apply(originalUri.toString(), hostAliasesResolver.aliases());
        URI rewrittenUri = URI.create(rewrittenUrl);

        if (!originalUri.equals(rewrittenUri)) {
            log.info("Host alias applied: {} -> {}", originalUri, rewrittenUri);
            HttpRequest wrappedRequest = new HttpRequestWrapper(request) {
                @Override
                public URI getURI() {
                    return rewrittenUri;
                }
            };
            return execution.execute(wrappedRequest, body);
        }

        return execution.execute(request, body);
    }
}
