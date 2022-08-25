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

import io.yupiik.tools.dev.test.DevToolsSupport;
import io.yupiik.uship.jsonrpc.core.api.JsonRpc;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DevToolsSupport
class HttpSignatureTest {
    @Inject
    @JsonRpc
    private HttpSignature httpSignature;

    @Test
    void sign() {
        assertEquals(
                "Signature keyId=\"hmac-key-1\",algorithm=\"hmac-sha256\",headers=\"content-length host date (request-target)\",signature=\"W3MggCKvdHEey9HskCkY96IUiUKhcwBHtfpb3IhXMdk=\"",
                httpSignature.sign(
                        HttpSignature.HttpMethod.GET, "/foo/Bar", """
                                content-length: 18
                                host: example.org
                                date: Tue, 07 Jun 2014 20:51:35 GMT
                                """,
                        "hmac-key-1",
                        "dont-tell",
                        HttpSignature.Algorithm.hmac_sha256,
                        List.of("content-length", "host", "date", "(request-target)")));
    }

    @Test
    void verify() {
        assertTrue(httpSignature.verify(
                HttpSignature.HttpMethod.GET, "/foo/Bar", """
                        content-length: 18
                        host: example.org
                        date: Tue, 07 Jun 2014 20:51:35 GMT
                        """,
                "dont-tell",
                HttpSignature.Algorithm.hmac_sha256,
                List.of("content-length", "host", "date", "(request-target)"),
                "Signature keyId=\"hmac-key-1\",algorithm=\"hmac-sha256\",headers=\"content-length host date (request-target)\",signature=\"W3MggCKvdHEey9HskCkY96IUiUKhcwBHtfpb3IhXMdk=\""));
    }
}
