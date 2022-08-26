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

import io.yupiik.tools.dev.api.UiMetadata;
import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcMethod;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcParam;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;

import java.util.Base64;

@JsonRpc
@ApplicationScoped
@UiMetadata(rootLabel = "Security", commandPrefix = "jwt-")
public class Jwt {
    @Inject
    private Jsonb jsonb;

    @JsonRpcMethod(name = "jwt-read", documentation = "Reads a JWT in clear text.")
    public JwtModel encode(@JsonRpcParam(documentation = "Value to read", required = true) final String value) {
        try {
            final var segments = value.split("\\.");
            return new JwtModel(null, read(segments[0]), read(segments[1]));
        } catch (final RuntimeException re) {
            return new JwtModel(re.getMessage(), null, null);
        }
    }

    private JsonObject read(final String value) {
        return jsonb.fromJson(new String(Base64.getUrlDecoder().decode(value)), JsonObject.class);
    }

    public record JwtModel(String error, JsonObject header, JsonObject payload) {
    }
}
