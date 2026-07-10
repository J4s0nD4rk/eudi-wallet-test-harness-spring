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
package de.isernese.eudiwallet.harness;

import de.isernese.eudiwallet.harness.config.HarnessProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * EUDI Wallet Test Harness — Spring Boot 4 edition.
 *
 * Simulates the wallet side of an OIDC4VCI pre-authorized code flow (SD-JWT VC)
 * to test the credential issuer locally / in CI without a real wallet.
 * See README.md for assumptions and architectural decisions.
 */
@SpringBootApplication
@EnableConfigurationProperties(HarnessProperties.class)
public class WalletHarnessApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletHarnessApplication.class, args);
    }
}
