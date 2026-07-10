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

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HostAliasApplierTest {

    @Test
    void rewritesHostAndPortFromHostPortAlias() {
        String result = HostAliasApplier.apply(
                "https://issuer.eudi-platform.example:8080/.well-known/openid-credential-issuer",
                Map.of("issuer.eudi-platform.example:8080", "host.docker.internal:9090"));

        assertThat(result).isEqualTo("https://host.docker.internal:9090/.well-known/openid-credential-issuer");
    }

    @Test
    void rewritesUsingFullOriginAliasValue() {
        String result = HostAliasApplier.apply(
                "https://issuer.eudi-platform.example/token",
                Map.of("issuer.eudi-platform.example", "http://host.docker.internal:8080"));

        assertThat(result).isEqualTo("http://host.docker.internal:8080/token");
    }

    @Test
    void leavesUrlUnchangedWhenNoAliasMatches() {
        String url = "https://other-host.example/token";

        String result = HostAliasApplier.apply(url,
                Map.of("issuer.eudi-platform.example", "host.docker.internal:8080"));

        assertThat(result).isEqualTo(url);
    }

    @Test
    void leavesUrlUnchangedWhenNoAliasesConfigured() {
        String url = "https://issuer.eudi-platform.example/token";

        assertThat(HostAliasApplier.apply(url, Map.of())).isEqualTo(url);
    }
}
