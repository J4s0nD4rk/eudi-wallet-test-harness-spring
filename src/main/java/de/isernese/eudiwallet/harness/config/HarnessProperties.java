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

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * Central harness configuration, bound from application.yml and environment variables
 * (Spring relaxed binding, e.g., HARNESS_VERIFY_TLS -&gt; harness.verify-tls).
 * <p>
 * Exception: {@code hostAliases} is only bound here as a fallback/default; a
 * JSON value in the {@code HOST_ALIASES} environment variable takes precedence
 * (see {@link HostAliasesResolver}) — reason: nested maps cannot be cleanly bound
 * via Docker env vars when the keys (hostnames) contain dots.
 *
 * @param verifyTls               TLS certificate verification when calling the credential issuer
 * @param customCaBundle          Path to a PEM CA bundle (takes precedence over verifyTls when set)
 * @param hostAliases             Default host aliases (YAML), overridable via HOST_ALIASES env (JSON)
 * @param httpTimeout             Timeout for all HTTP calls to the credential issuer
 * @param useDpop                 Default: use DPoP (RFC 9449) for token/credential requests
 * @param verifySdJwtSignature    Default: verify SD-JWT signature (x5c preferred, JWKS fallback)
 * @param credentialConfigPath    Path to credential-configs.yaml ("classpath:" or filesystem path)
 * @param outputDir               Optional directory for per-run result JSON (empty = disabled)
 */
@ConfigurationProperties(prefix = "harness")
public record HarnessProperties(
        boolean verifyTls,
        String customCaBundle,
        Map<String, String> hostAliases,
        Duration httpTimeout,
        boolean useDpop,
        boolean verifySdJwtSignature,
        String credentialConfigPath,
        String outputDir
) {
    public HarnessProperties {
        if (hostAliases == null) {
            hostAliases = Map.of();
        }
        if (httpTimeout == null) {
            httpTimeout = Duration.ofSeconds(10);
        }
        if (credentialConfigPath == null || credentialConfigPath.isBlank()) {
            credentialConfigPath = "classpath:config/credential-configs.yaml";
        }
    }
}
