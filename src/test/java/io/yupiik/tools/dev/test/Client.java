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
package io.yupiik.tools.dev.test;

import io.yupiik.uship.webserver.tomcat.TomcatWebServerConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import jakarta.json.bind.Jsonb;
import org.apache.johnzon.jsonb.extension.JsonValueReader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

@ApplicationScoped
public class Client {
    private final HttpClient client = HttpClient.newHttpClient();

    @Inject
    private Jsonb jsonb;

    @Inject
    private TomcatWebServerConfiguration configuration;

    public <T> T jsonRpc(final JsonObject request, final Class<T> response) {
        try {
            final var httpResponse = client.send(
                    HttpRequest.newBuilder()
                            .POST(HttpRequest.BodyPublishers.ofString(request.toString()))
                            .uri(URI.create("http://localhost:" + configuration.getPort() + "/jsonrpc"))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, httpResponse.statusCode());

            final var json = jsonb.fromJson(httpResponse.body(), JsonObject.class);
            assertNull(json.get("error"));

            final var result = json.get("result");
            if (response.isInstance(result)) {
                return response.cast(result);
            }
            if (result instanceof JsonStructure s) {
                return jsonb.fromJson(new JsonValueReader<>(s), response);
            }
            return fail(result.toString());
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
