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
package io.yupiik.tools.dev.api;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;

import java.util.List;
import java.util.Map;
import java.util.stream.Collector;

public record ReactUi(String type, Map<String, Object> props,
                      List<Object> children) {
    public record Eval(String variable) {
        public JsonValue asJson(final JsonBuilderFactory jsons) {
            return jsons.createObjectBuilder().add("$eval", variable).build();
        }
    }

    public JsonValue asJson(final JsonBuilderFactory jsons, final JsonProvider provider) {
        final var builder = jsons.createObjectBuilder()
                .add("type", type)
                .add("props", props != null ?
                        props.entrySet().stream()
                                .collect(Collector.of(
                                        jsons::createObjectBuilder,
                                        (a, i) -> {
                                            final var key = i.getKey();
                                            final var value = i.getValue();
                                            if (value instanceof String s) {
                                                a.add(key, s);
                                            } else if (value instanceof Integer integer) {
                                                a.add(key, integer);
                                            } else if (value instanceof Double v) {
                                                a.add(key, v);
                                            } else if (value instanceof Long l) {
                                                a.add(key, l);
                                            } else if (value instanceof JsonValue j) {
                                                a.add(key, j);
                                            } else if (value instanceof ReactUi ui) {
                                                a.add(key, ui.asJson(jsons, provider));
                                            } else if (value instanceof ReactUi.Eval e) {
                                                a.add(key, e.asJson(jsons));
                                            } else {
                                                throw new IllegalStateException("Unsupported value: " + i);
                                            }
                                        },
                                        JsonObjectBuilder::addAll,
                                        JsonObjectBuilder::build)) :
                        JsonValue.EMPTY_JSON_OBJECT);
        if (children != null && !children.isEmpty()) {
            builder.add("children", children.stream()
                    .map(it -> {
                        if (it instanceof ReactUi ui) {
                            return ui.asJson(jsons, provider);
                        } else if (it instanceof Eval e) {
                            return e.asJson(jsons);
                        } else if (it instanceof String s) {
                            return provider.createValue(s);
                        } else {
                            throw new IllegalStateException("Unsupported type: " + it);
                        }
                    })
                    .collect(Collector.of(
                            jsons::createArrayBuilder,
                            JsonArrayBuilder::add,
                            JsonArrayBuilder::addAll,
                            JsonArrayBuilder::build)));
        }
        return builder.build();
    }
}
