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

import io.yupiik.tools.dev.api.ReactUi;
import io.yupiik.tools.dev.api.UiMetadata;
import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcMethod;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcParam;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.spi.JsonProvider;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@JsonRpc
@ApplicationScoped
@UiMetadata(rootLabel = "Security", commandPrefix = "jwt-")
public class Jwt {
    @Inject
    private Jsonb jsonb;

    @Inject
    private JsonBuilderFactory jsons;

    @Inject
    private JsonProvider jsonProvider;

    @JsonRpcMethod(name = "jwt-read", documentation = "Reads a JWT in clear text.")
    public JwtResult encode(@JsonRpcParam(documentation = "Value to read", required = true) final String value) {
        try {
            final var segments = value.split("\\.");
            return new JwtResult(
                    new ReactUi("div", Map.of("className", "jwt-read-result"), List.of(
                            new ReactUi("div", Map.of("className", "jwt-read-result"), List.of(
                                    new ReactUi("div", Map.of("className", "jwt-read-result-header"), List.of(
                                            new ReactUi("h2", null, List.of("Header")),
                                            new ReactUi.Eval("header")
                                    )),
                                    new ReactUi("div", Map.of("className", "jwt-read-result-payload"), List.of(
                                            new ReactUi("h2", null, List.of("Payload")),
                                            new ReactUi.Eval("payload")
                                    ))))))
                            .asJson(jsons, jsonProvider),
                    new JwtModel(null, read(segments[0]), read(segments[1])));
        } catch (final RuntimeException re) {
            return new JwtResult(
                    new ReactUi("div", Map.of("className", "jwt-read-result"), List.of(new ReactUi(
                            "antd.Alert",
                            Map.of("type", "error", "message", new ReactUi.Eval("error")),
                            null)
                    )).asJson(jsons, jsonProvider),
                    new JwtModel(re.getMessage(), null, null));
        }
    }

    private JsonObject read(final String value) {
        return jsonb.fromJson(new String(Base64.getUrlDecoder().decode(value)), JsonObject.class);
    }

    public record JwtResult(JsonValue ui, JwtModel data) {
    }

    public record JwtModel(String error, JsonObject header, JsonObject payload) {
    }
}
