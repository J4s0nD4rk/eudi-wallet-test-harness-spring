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

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Loads credential-configs.yaml (path from {@link HarnessProperties#credentialConfigPath()},
 * "classpath:" or filesystem path, e.g., for a read-only mounted config/ directory
 * in a Docker container). The file is loaded at application startup to detect
 * errors early (fail-fast).
 */
@Component
public class CredentialConfigLoader {

    private final YAMLMapper yamlMapper = YAMLMapper.builder().build();
    private final ResourceLoader resourceLoader;
    private final HarnessProperties properties;
    private Map<String, CredentialConfiguration> cached;

    public CredentialConfigLoader(ResourceLoader resourceLoader, HarnessProperties properties) {
        this.resourceLoader = resourceLoader;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        this.cached = readFromResource(properties.credentialConfigPath());
    }

    public Map<String, CredentialConfiguration> load() {
        return cached;
    }

    private Map<String, CredentialConfiguration> readFromResource(String path) {
        Resource resource = resourceLoader.getResource(path);
        try (InputStream in = resource.getInputStream()) {
            return yamlMapper.readValue(in, yamlMapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, CredentialConfiguration.class));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read credential config file: " + path, e);
        }
    }
}
