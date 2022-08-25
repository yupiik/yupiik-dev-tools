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
package io.yupiik.tools.dev.configuration;

import io.yupiik.batch.runtime.configuration.Binder;
import io.yupiik.batch.runtime.configuration.Param;
import io.yupiik.batch.runtime.configuration.Prefix;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Optional.ofNullable;

@Prefix("custom-operations")
@ApplicationScoped
public class CustomOperationsConfiguration {
    @Param(name = "custom-operations-location", description = "Location of the definition of the custom operations - `custom-operations.json` by default.")
    private String location = ofNullable(System.getProperty("user.dir"))
            .map(Path::of)
            .map(p -> p.resolve("custom-operations.json"))
            .filter(Files::exists)
            .map(Path::toString)
            .orElse(null);

    @PostConstruct
    private void init() {
        Binder.bindPrefixed(this);
    }

    public Path getLocation() {
        return location == null ? null : Path.of(location);
    }
}
