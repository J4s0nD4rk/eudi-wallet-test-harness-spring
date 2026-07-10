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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/**
 * Resolves the effective host aliases: the {@code HOST_ALIASES} environment variable
 * (JSON object, e.g., from "docker run -e") takes precedence over
 * {@code harness.host-aliases} from application.yml / a mounted config file.
 * Separate, small class (SRP) rather than weaving this into HarnessProperties or
 * Oidc4vciClient.
 * <p>
 * The environment variable is injected via {@code @Value} to improve testability
 * without directly accessing {@code System.getenv()} (SOLID).
 */
@Component
public class HostAliasesResolver {

    private final Map<String, String> effectiveAliases;

    public HostAliasesResolver(
            HarnessProperties properties,
            JsonMapper jsonMapper,
            @Value("${HOST_ALIASES:}") String envJson) {
        this.effectiveAliases = resolve(properties, jsonMapper, envJson);
    }

    private static Map<String, String> resolve(HarnessProperties properties, JsonMapper jsonMapper, String envJson) {
        if (envJson == null || envJson.isBlank()) {
            return properties.hostAliases();
        }
        try {
            return jsonMapper.readValue(envJson, jsonMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "HOST_ALIASES environment variable does not contain a valid JSON object: " + e.getMessage(), e);
        }
    }

    public Map<String, String> aliases() {
        return effectiveAliases;
    }
}
