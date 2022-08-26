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
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static io.yupiik.tools.dev.api.UiWidget.TEXTAREA;

@JsonRpc
@ApplicationScoped
@UiMetadata(rootLabel = "Text", commandPrefix = "regex-")
public class Regex {
    @JsonRpcMethod(name = "regex-matches", documentation = "Enables to test 'matches' of a regex.")
    public Matches matches(@JsonRpcParam(documentation = "Regex to test.", required = true) final String regex, @JsonRpcParam(documentation = "Text to test the regex against.", required = true) @UiWidget(TEXTAREA) final String text) {
        final var pattern = Pattern.compile(regex);
        final var matcher = pattern.matcher(text);
        final var matches = matcher.matches();
        final var groupCount = matcher.groupCount();
        return new Matches(matches, getGroups(matcher, groupCount));
    }

    @JsonRpcMethod(name = "regex-find", documentation = "Enables to test 'finds' of a regex.")
    public Finds find(@JsonRpcParam(documentation = "Regex to test.", required = true) final String regex, @JsonRpcParam(documentation = "Text to test the regex against.", required = true) @UiWidget(TEXTAREA) final String text) {
        final var pattern = Pattern.compile(regex);
        final var matcher = pattern.matcher(text);
        final var finds = new ArrayList<Find>();
        while (matcher.find()) {
            finds.add(new Find(matcher.start(), getGroups(matcher, matcher.groupCount())));
        }
        return new Finds(finds);
    }

    private List<String> getGroups(final Matcher matcher, final int groupCount) {
        return groupCount > 0 ? IntStream.range(0, groupCount).mapToObj(matcher::group).toList() : null;
    }

    public record Matches(boolean matches, List<String> groups) {
    }

    public record Finds(List<Find> finds) {
    }

    public record Find(int start, List<String> groups) {
    }
}
