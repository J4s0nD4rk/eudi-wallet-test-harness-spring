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

import java.util.List;

/**
 * Expected termination of an issuance flow (e.g., missing pre-authorized_code grant,
 * HTTP error from the credential issuer). Carries the logs collected up to that
 * point, so the controller can build a {@code status: FAILED} response with the
 * full trace.
 */
public class IssuanceFlowException extends RuntimeException {

    private final List<String> logs;

    public IssuanceFlowException(String message, List<String> logs) {
        super(message);
        this.logs = List.copyOf(logs);
    }

    public IssuanceFlowException(String message, List<String> logs, Throwable cause) {
        super(message, cause);
        this.logs = List.copyOf(logs);
    }

    public List<String> logs() {
        return logs;
    }
}
