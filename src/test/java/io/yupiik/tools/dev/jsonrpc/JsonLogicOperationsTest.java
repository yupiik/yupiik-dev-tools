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

import io.yupiik.tools.dev.test.Client;
import io.yupiik.tools.dev.test.DevToolsSupport;
import jakarta.inject.Inject;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DevToolsSupport
class JsonLogicOperationsTest {
    @Inject
    private Client client;

    @Inject
    private JsonBuilderFactory jsonBuilderFactory;

    @Test
    void validateEnhancedJsonRpcMethodRegistry() {
        final var spec = client.jsonRpc(
                jsonBuilderFactory.createObjectBuilder()
                        .add("jsonrpc", "2.0")
                        .add("method", "hello-world")
                        .add("params", jsonBuilderFactory.createObjectBuilder()
                                .add("name", "world"))
                        .build(),
                JsonString.class);
        assertEquals("Hello world!", spec.getString());
    }

    @Test
    void reuseJsonRpc() {
        final var spec = client.jsonRpc(
                jsonBuilderFactory.createObjectBuilder()
                        .add("jsonrpc", "2.0")
                        .add("method", "custom-base64")
                        .add("params", jsonBuilderFactory.createObjectBuilder()
                                .add("name", "world"))
                        .build(),
                JsonString.class);
        assertEquals(Base64.getEncoder().encodeToString("Hello world!".getBytes(StandardCharsets.UTF_8)), spec.getString());
    }
}
