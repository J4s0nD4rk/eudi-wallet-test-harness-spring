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
package de.isernese.eudiwallet.harness.sdjwt;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single SD-JWT disclosure {@code [salt, claimName, value]}. {@code digestMatchesSdArray}
 * is set directly at creation time, rather than determining in a later, separate
 * step which digests were disclosed.
 */
public record Disclosure(
        String raw,
        String salt,
        String claimName,
        Object value,
        String digest,
        Boolean digestMatchesSdArray
) {
    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("claimName", claimName);
        map.put("value", value);
        map.put("digest", digest);
        map.put("digestMatchesSdArray", digestMatchesSdArray);
        return map;
    }
}
