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

import java.nio.charset.StandardCharsets;

import static java.util.Optional.ofNullable;

@JsonRpc
@ApplicationScoped
@UiMetadata(rootLabel = "Encoding", commandPrefix = "base64-")
public class Base64 {
    @JsonRpcMethod(name = "base64-encode", documentation = "Enables to encode a string in base64.")
    public String encode(@JsonRpcParam(documentation = "Base64 mode (URL, URL_NO_PADDING, DEFAULT)") final Base64EncodingMode mode,
                         @JsonRpcParam(documentation = "Value to encode", required = true) final String value) {
        return switch (ofNullable(mode).orElse(Base64EncodingMode.DEFAULT)) {
            case DEFAULT -> java.util.Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
            case URL -> java.util.Base64.getUrlEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
            case URL_NO_PADDING ->
                    java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
            case MIME -> java.util.Base64.getMimeEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
            case MIME_NO_PADDING ->
                    java.util.Base64.getMimeEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        };
    }

    @JsonRpcMethod(name = "base64-decode", documentation = "Enables to decode a base64 string.")
    public String encode(@JsonRpcParam(documentation = "Base64 mode (URL, URL_NO_PADDING, DEFAULT)") final Base64DecodingMode mode,
                         @JsonRpcParam(documentation = "Value to decode", required = true) final String value) {
        return switch (ofNullable(mode).orElse(Base64DecodingMode.DEFAULT)) {
            case DEFAULT -> new String(java.util.Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
            case URL -> new String(java.util.Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            case MIME -> new String(java.util.Base64.getMimeDecoder().decode(value), StandardCharsets.UTF_8);
        };
    }

    public enum Base64DecodingMode {
        MIME,
        URL,
        DEFAULT
    }

    public enum Base64EncodingMode {
        MIME,
        MIME_NO_PADDING,
        URL,
        URL_NO_PADDING,
        DEFAULT
    }
}
