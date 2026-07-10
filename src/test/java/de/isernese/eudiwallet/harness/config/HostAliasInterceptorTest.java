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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class HostAliasInterceptorTest {

    @Test
    void interceptsAndRewritesUriUsingHostAliases() throws Exception {
        // Arrange
        HarnessProperties properties = new HarnessProperties(
                false,
                null,
                Map.of("issuer.example", "host.docker.internal:8080"),
                null,
                false,
                false,
                null,
                null
        );
        HostAliasesResolver resolver = new HostAliasesResolver(properties, null, null);
        HostAliasInterceptor interceptor = new HostAliasInterceptor(resolver);

        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("https://issuer.example/token"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(execution.execute(any(), any())).thenReturn(response);

        byte[] body = new byte[0];

        // Act
        interceptor.intercept(request, body, execution);

        // Assert
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(execution).execute(requestCaptor.capture(), eq(body));

        HttpRequest executedRequest = requestCaptor.getValue();
        assertThat(executedRequest.getURI()).isEqualTo(URI.create("https://host.docker.internal:8080/token"));
    }

    @Test
    void leavesUriUnchangedIfNoAliasMatches() throws Exception {
        // Arrange
        HarnessProperties properties = new HarnessProperties(
                false,
                null,
                Map.of("issuer.example", "host.docker.internal:8080"),
                null,
                false,
                false,
                null,
                null
        );
        HostAliasesResolver resolver = new HostAliasesResolver(properties, null, null);
        HostAliasInterceptor interceptor = new HostAliasInterceptor(resolver);

        HttpRequest request = mock(HttpRequest.class);
        when(request.getURI()).thenReturn(URI.create("https://other.example/token"));

        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(execution.execute(any(), any())).thenReturn(response);

        byte[] body = new byte[0];

        // Act
        interceptor.intercept(request, body, execution);

        // Assert
        verify(execution).execute(request, body);
    }
}
