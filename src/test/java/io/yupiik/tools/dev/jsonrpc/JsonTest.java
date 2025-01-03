/*
 * Copyright (c) 2022 - Yupiik SAS - https://www.yupiik.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.yupiik.tools.dev.jsonrpc;

import io.yupiik.tools.dev.test.DevToolsSupport;
import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import jakarta.inject.Inject;
import jakarta.json.JsonString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DevToolsSupport
class JsonTest {
    @Inject
    @JsonRpc
    private Json json;

    @Test
    void unescape() {
        assertEquals("{\"name\":\"value\"}", ((JsonString) json.unescape("{\\\"name\\\":\\\"value\\\"}")).getString());
        assertEquals("{\"name\":\"value\"}", ((JsonString) json.unescape("\"{\\\"name\\\":\\\"value\\\"}\"")).getString());
    }
}
