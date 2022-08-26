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
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@JsonRpc
@ApplicationScoped
@UiMetadata(rootLabel = "Generator", commandPrefix = "uuid-")
public class Uuid {
    @JsonRpcMethod(name = "uuid-generator", documentation = "Create a new UUID.")
    public String encode() {
        return UUID.randomUUID().toString();
    }
}
