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

import io.yupiik.tools.dev.api.UiMetadata;
import io.yupiik.tools.dev.configuration.CustomOperationsConfiguration;
import io.yupiik.uship.backbone.johnzon.jsonschema.SchemaProcessor;
import io.yupiik.uship.jsonrpc.core.impl.JsonRpcMethodRegistry;
import io.yupiik.uship.jsonrpc.core.impl.Registration;
import io.yupiik.uship.jsonrpc.core.openrpc.OpenRPC;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Specializes;
import jakarta.inject.Inject;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import org.apache.johnzon.jsonlogic.JohnzonJsonLogic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Logger;

import static java.util.Optional.ofNullable;

@Specializes
@ApplicationScoped
public class EnhancedJsonRpcMethodRegistry extends JsonRpcMethodRegistry {
    private final Logger logger = Logger.getLogger(getClass().getName());

    @Inject
    private CustomOperationsConfiguration configuration;

    @Inject
    private Jsonb jsonb;

    @Inject
    private JsonBuilderFactory jsonBuilderFactory;

    private JohnzonJsonLogic jsonLogic;
    private final Collection<Unregisterable> releasables = new ArrayList<>();

    @PreDestroy
    private void destroy() {
        releasables.forEach(Unregisterable::close);
    }

    @Override
    protected OpenRPC doCreateOpenRpc() {
        if (configuration.getLocation() != null && Files.exists(configuration.getLocation())) {
            logger.info(() -> "Loading '" + configuration.getLocation() + "' operations");
            registerCustomOperations();
        } else {
            logger.info(() -> "'" + (configuration.getLocation() == null ?
                    ofNullable(System.getProperty("user.dir"))
                            .map(Path::of)
                            .map(p -> p.resolve("custom-operations.json"))
                            .orElse(null) :
                    configuration.getLocation()) + "' does not exist, skipping");
        }
        return super.doCreateOpenRpc();
    }

    @Override
    protected OpenRPC.RpcMethod toRpcMethod(final SchemaProcessor schemaProcessor,
                                            final SchemaProcessor.InMemoryCache componentsSchemaProcessorCache,
                                            final Map.Entry<String, JsonRpcMethodRegistration> handler) {
        final var builtIn = super.toRpcMethod(schemaProcessor, componentsSchemaProcessorCache, handler);
        if (handler.getValue().registration() instanceof JsonLogicRegistration r) {
            builtIn.setTags(r.tags);
        } else {
            ofNullable(handler.getValue().registration().clazz())
                    .map(c -> c.getAnnotation(UiMetadata.class))
                    .ifPresent(metadata -> builtIn.setTags(asTags(metadata)));
        }
        return builtIn;
    }

    private void registerCustomOperations() {
        jsonLogic = new JohnzonJsonLogic()
                .registerDefaultOperators()
                .registerExtensionsOperators();
        // todo: add other operators

        try {
            final var customOperationsModel = jsonb.fromJson(
                    Files.readString(configuration.getLocation()),
                    CustomOperationsModel.class);

            if (customOperationsModel.jsonLogics() != null) {
                customOperationsModel.jsonLogics().forEach(this::registerJsonLogic);
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void registerJsonLogic(final JsonLogicOperation jsonLogicOperation) {
        final var index = new AtomicInteger();
        final var parameters = jsonLogicOperation.parameters().stream()
                .map(it -> new Registration.Parameter(
                        switch (ofNullable(it.type()).orElse(CustomOperationsParameterType.STRING)) {
                            case STRING -> String.class;
                            case NUMBER -> double.class;
                            case BOOLEAN -> boolean.class;
                        },
                        it.name(),
                        index.incrementAndGet(),
                        it.required(),
                        it.documentation()))
                .toList();
        releasables.add(registerMethod(new JsonLogicRegistration(
                jsonLogicOperation.name(),
                // todo: support async? not needed with current operators
                args -> jsonLogic.apply(jsonLogicOperation.jsonLogic(), toJsonObject(jsonLogicOperation.parameters(), args)),
                parameters,
                List.of(),
                jsonLogicOperation.description(),
                List.of(
                        new OpenRPC.Tag("Custom Operations", null, "root_label", null),
                        new OpenRPC.Tag("", null, "command_prefix", null)))));
    }

    private JsonValue toJsonObject(final List<CustomOperationsParameter> parameters, final Object[] args) {
        if (parameters == null || parameters.isEmpty()) {
            return JsonValue.EMPTY_JSON_OBJECT;
        }

        final var obj = jsonBuilderFactory.createObjectBuilder();
        int i = 0;
        for (final var param : parameters) {
            final var value = args[i];
            if (value != null) {
                switch (ofNullable(param.type()).orElse(CustomOperationsParameterType.STRING)) {
                    case STRING -> obj.add(param.name(), value.toString());
                    case NUMBER -> obj.add(param.name(), ((Number) value).doubleValue());
                    case BOOLEAN -> obj.add(param.name(), Boolean.TRUE.equals(value));
                }
            }
            i++;
        }
        return obj.build();
    }

    private List<OpenRPC.Tag> asTags(final UiMetadata metadata) {
        return List.of(
                new OpenRPC.Tag(metadata.rootLabel(), null, "root_label", null),
                new OpenRPC.Tag(metadata.commandPrefix(), null, "command_prefix", null));
    }

    public record CustomOperationsModel(List<JsonLogicOperation> jsonLogics) {
    }

    public enum CustomOperationsParameterType {
        STRING,
        NUMBER,
        BOOLEAN
    }

    public record CustomOperationsParameter(
            CustomOperationsParameterType type,
            String name,
            boolean required,
            String documentation) {
    }

    public record JsonLogicOperation(
            String name,
            String description,
            List<CustomOperationsParameter> parameters,
            JsonValue jsonLogic) {
    }

    private static class JsonLogicRegistration extends Registration {
        private final List<OpenRPC.Tag> tags;

        private JsonLogicRegistration(final String jsonRpcMethod,
                                      final Function<Object[], Object> invoker,
                                      final Collection<Parameter> parameters,
                                      final Collection<ExceptionMapping> exceptionMappings,
                                      final String documentation,
                                      final List<OpenRPC.Tag> tags) {
            super(null, null, jsonRpcMethod, JsonValue.class, invoker, parameters, exceptionMappings, documentation);
            this.tags = tags;
        }
    }
}