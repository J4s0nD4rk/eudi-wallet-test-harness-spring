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

import de.isernese.eudiwallet.harness.config.HarnessProperties;
import de.isernese.eudiwallet.harness.config.HostAliasInterceptor;
import de.isernese.eudiwallet.harness.config.HostAliasesResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.InputStream;
import java.net.Socket;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Builds the single RestClient used by the entire harness, including TLS
 * configuration from {@link HarnessProperties}.
 * <p>
 * Precedence: customCaBundle (if set) &gt; verifyTls=false (trust-all, only for
 * local/CI test environments with self-signed certificates) &gt; default JDK truststore.
 * <p>
 * Intentionally starts from the Spring Boot auto-configured {@link RestClient.Builder}
 * bean (not {@code RestClient.builder()}), so that Jackson 3 message converters and
 * other Boot auto-configuration are preserved — only the request factory is replaced.
 */
@Configuration
public class HarnessRestClientConfig {

    @Bean
    public RestClient harnessRestClient(
            RestClient.Builder builder,
            HarnessProperties properties,
            ResourceLoader resourceLoader,
            HostAliasesResolver hostAliasesResolver) {
        
        return builder
                .requestInterceptor(new HostAliasInterceptor(hostAliasesResolver))
                .requestFactory(buildRequestFactory(properties, resourceLoader))
                .build();
    }

    private ClientHttpRequestFactory buildRequestFactory(HarnessProperties properties, ResourceLoader resourceLoader) {
        try {
            SSLContext sslContext = buildSslContext(properties, resourceLoader);
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(properties.httpTimeout())
                    .sslContext(sslContext)
                    .build();
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
            factory.setReadTimeout(properties.httpTimeout());
            return factory;
        } catch (Exception e) {
            throw new IllegalStateException("Could not configure HTTP client for the credential issuer", e);
        }
    }

    private SSLContext buildSslContext(HarnessProperties properties, ResourceLoader resourceLoader) throws Exception {
        if (properties.customCaBundle() != null && !properties.customCaBundle().isBlank()) {
            return buildCustomCaSslContext(properties.customCaBundle(), resourceLoader);
        }
        if (!properties.verifyTls()) {
            return buildTrustAllSslContext();
        }
        return SSLContext.getDefault();
    }

    private SSLContext buildTrustAllSslContext() throws Exception {
        TrustManager[] trustAll = {
                new X509ExtendedTrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) { }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) { }

                    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) { }

                    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) { }

                    public void checkClientTrusted(X509Certificate[] chain, String authType,
                                                    javax.net.ssl.SSLEngine engine) { }

                    public void checkServerTrusted(X509Certificate[] chain, String authType,
                                                    javax.net.ssl.SSLEngine engine) { }

                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAll, new SecureRandom());
        return sslContext;
    }

    private SSLContext buildCustomCaSslContext(String bundlePath, ResourceLoader resourceLoader) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        try (InputStream in = resourceLoader.getResource(bundlePath).getInputStream()) {
            int index = 0;
            for (Certificate certificate : certificateFactory.generateCertificates(in)) {
                keyStore.setCertificateEntry("ca-" + (index++), certificate);
            }
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
        return sslContext;
    }
}
