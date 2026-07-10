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

import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;

/**
 * Shared constants and field names per the OIDC4VCI specification.
 */
public interface Oidc4vciConstants {

    String PRE_AUTH_GRANT = "urn:ietf:params:oauth:grant-type:pre-authorized_code";
    String WELL_KNOWN_CREDENTIAL_ISSUER = "/.well-known/openid-credential-issuer";
    List<String> WELL_KNOWN_AUTH_SERVER_CANDIDATES = List.of(
            "/.well-known/oauth-authorization-server",
            "/.well-known/openid-configuration"
    );

    String FIELD_CREDENTIAL_OFFER = "credential_offer";
    String FIELD_CREDENTIAL_OFFER_URI = "credential_offer_uri";
    String FIELD_CREDENTIAL_ISSUER = "credential_issuer";
    String FIELD_GRANTS = "grants";
    String FIELD_PRE_AUTHORIZED_CODE = "pre-authorized_code";
    String FIELD_TX_CODE = "tx_code";
    String FIELD_TOKEN_ENDPOINT = "token_endpoint";
    String FIELD_ACCESS_TOKEN = "access_token";
    String FIELD_CREDENTIAL_ENDPOINT = "credential_endpoint";
    String FIELD_C_NONCE = "c_nonce";
    String FIELD_CREDENTIAL_CONFIGURATION_IDS = "credential_configuration_ids";
    String FIELD_CREDENTIAL_CONFIGURATIONS_SUPPORTED = "credential_configurations_supported";
    String FIELD_CREDENTIAL_CONFIGURATION_ID = "credential_configuration_id";
    String FIELD_JWKS_URI = "jwks_uri";
    String FIELD_CREDENTIAL = "credential";

    ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT = new ParameterizedTypeReference<>() {};
}
