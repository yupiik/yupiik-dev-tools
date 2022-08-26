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
import io.yupiik.tools.dev.api.UiWidget;
import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcMethod;
import io.yupiik.uship.jsonrpc.core.api.JsonRpcParam;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriterFactory;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import static io.yupiik.tools.dev.api.UiWidget.TEXTAREA;

@JsonRpc
@ApplicationScoped
@UiMetadata(rootLabel = "Formatter", commandPrefix = "json-")
public class Json {
    @Inject
    private JsonReaderFactory readers;

    @Inject
    private JsonProvider provider;

    private JsonWriterFactory writers;

    @PostConstruct
    private void formattingWriter() {
        writers = provider.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true));
    }

    @JsonRpcMethod(name = "json-format", documentation = "Format a JSON data.")
    public String sign(@JsonRpcParam(documentation = "JSON to format.", required = true) @UiWidget(TEXTAREA) final String value) {
        final var out = new StringWriter();
        try (final var reader = readers.createReader(new StringReader(value));
             final var writer = writers.createWriter(out)) {
            writer.write(reader.readValue());
        }
        return out.toString();
    }

    @JsonRpcMethod(name = "json-unescape", documentation = "Unescape an escaped JSON string.")
    public JsonValue unescape(@JsonRpcParam(documentation = "JSON to format.", required = true) @UiWidget(TEXTAREA) final String value) {
        try (final var reader = readers.createReader(new StringReader(!value.startsWith("\"") && !value.endsWith("\"") ? '"' + value + '"' : value))) {
            return reader.readValue();
        }
    }
}
