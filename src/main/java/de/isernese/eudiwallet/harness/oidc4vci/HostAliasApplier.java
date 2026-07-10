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

import java.net.URI;
import java.util.Map;

/**
 * Replaces the host (and optionally port/scheme) of a URL based on an alias map,
 * before the harness sends a request to the credential issuer. Reason: the deep
 * link / issuer metadata usually contains an "externally" valid hostname (e.g.,
 * issuer.example) that is not resolvable from within the Docker container and must
 * be redirected to host.docker.internal or similar (see README.md, section
 * 'Assumptions and known limitations').
 */
public final class HostAliasApplier {

    private HostAliasApplier() {
    }

    /**
     * @param url         Original URL as it appears in issuer metadata / credential offer
     * @param hostAliases Map of "host" or "host:port" to either "newHost:newPort"
     *                    or a full origin ("http://newHost:newPort")
     */
    public static String apply(String url, Map<String, String> hostAliases) {
        if (hostAliases == null || hostAliases.isEmpty()) {
            return url;
        }
        URI original = URI.create(url);
        String hostPortKey = original.getPort() >= 0
                ? original.getHost() + ":" + original.getPort()
                : original.getHost();

        String aliasValue = hostAliases.getOrDefault(hostPortKey, hostAliases.get(original.getHost()));
        if (aliasValue == null) {
            return url;
        }

        if (aliasValue.contains("://")) {
            URI aliasUri = URI.create(aliasValue);
            return rebuild(original, aliasUri.getScheme(), aliasUri.getHost(), aliasUri.getPort());
        }

        String[] parts = aliasValue.split(":", 2);
        int newPort = parts.length > 1 ? Integer.parseInt(parts[1]) : -1;
        return rebuild(original, original.getScheme(), parts[0], newPort);
    }

    private static String rebuild(URI original, String scheme, String host, int port) {
        try {
            return new URI(
                    scheme != null ? scheme : original.getScheme(),
                    null,
                    host,
                    port,
                    original.getRawPath(),
                    original.getRawQuery(),
                    original.getRawFragment()
            ).toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not apply host alias to URL: " + original, e);
        }
    }
}
