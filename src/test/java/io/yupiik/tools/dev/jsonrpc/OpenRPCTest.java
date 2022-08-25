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
import io.yupiik.uship.jsonrpc.core.openrpc.OpenRPC;
import jakarta.inject.Inject;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import org.apache.johnzon.jsonb.extension.JsonValueReader;
import org.junit.jupiter.api.Test;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DevToolsSupport
class OpenRPCTest {
    @Inject
    private Client client;

    @Inject
    private JsonBuilderFactory jsonBuilderFactory;

    @Inject
    private Jsonb jsonb;

    @Test
    void validateEnhancedJsonRpcMethodRegistry() {
        final var json = client.jsonRpc(
                jsonBuilderFactory.createObjectBuilder()
                        .add("jsonrpc", "2.0")
                        .add("method", "openrpc")
                        .build(),
                JsonObject.class);
        final var spec = jsonb.fromJson(new JsonValueReader<>(json), OpenRPC.class);
        final var methods = spec.getMethods().stream().collect(toMap(OpenRPC.RpcMethod::getName, identity()));

        final var base64Encode = methods.get("base64-encode");
        assertNotNull(base64Encode);
        assertNotNull(base64Encode.getTags());

        // tags are present
        final var tags = base64Encode.getTags().stream().collect(toMap(OpenRPC.Tag::getSummary, OpenRPC.Tag::getName));
        assertEquals("Encoding", tags.get("root_label"));
        assertEquals("base64-", tags.get("command_prefix"));

        // and root widgets are present
        assertEquals("textarea", json.getJsonArray("methods").stream()
                .map(JsonValue::asJsonObject)
                .filter(m -> "http-signature-sign".equals(m.getString("name")))
                .findFirst()
                .flatMap(m -> m.getJsonArray("params").stream()
                        .map(JsonValue::asJsonObject)
                        .filter(a -> "requestHeaders".equals(a.getString("name")))
                        .findFirst())
                .orElseThrow()
                .getJsonObject("ui")
                .getString("widget"));
    }
}
