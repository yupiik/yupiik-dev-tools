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
package io.yupiik.tools.dev.jsonrpc.internal;

import io.yupiik.tools.dev.test.DevToolsSupport;
import jakarta.inject.Inject;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonString;
import org.apache.johnzon.jsonlogic.JohnzonJsonLogic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DevToolsSupport
class EnhancedJsonRpcMethodRegistryTest {
    @Inject
    private JohnzonJsonLogic jsonLogic;

    @Inject
    private JsonBuilderFactory jsonBuilderFactory;

    @Test
    void sequence() {
        final var result = jsonLogic.apply(
                jsonBuilderFactory.createObjectBuilder()
                        .add("sequence", jsonBuilderFactory.createObjectBuilder()
                                .add("operations", jsonBuilderFactory.createArrayBuilder()
                                        .add(jsonBuilderFactory.createObjectBuilder()
                                                .add("resultName", "res1")
                                                .add("jsonLogic", jsonBuilderFactory.createObjectBuilder()
                                                        .add("cat", jsonBuilderFactory.createArrayBuilder()
                                                                .add("Hello")
                                                                .add(" ")
                                                                .add("world"))))
                                        .add(jsonBuilderFactory.createObjectBuilder()
                                                .add("resultName", "res1")
                                                .add("jsonLogic", jsonBuilderFactory.createObjectBuilder()
                                                        .add("cat", jsonBuilderFactory.createArrayBuilder()
                                                                .add(jsonBuilderFactory.createObjectBuilder()
                                                                        .add("var", "res1"))
                                                                .add(" from ")
                                                                .add(jsonBuilderFactory.createObjectBuilder()
                                                                        .add("var", "in"))
                                                                .add("!"))))))
                        .build(),
                jsonBuilderFactory.createObjectBuilder()
                        .add("in", "the-name")
                        .build());
        assertEquals("Hello world from the-name!", assertInstanceOf(JsonString.class, result).getString());
    }
}
