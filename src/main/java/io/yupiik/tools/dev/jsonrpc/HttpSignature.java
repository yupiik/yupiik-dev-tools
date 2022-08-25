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
import org.tomitribe.churchkey.pem.PemParser;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

import static io.yupiik.tools.dev.api.UiWidget.TEXTAREA;
import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

@JsonRpc
@ApplicationScoped
@UiMetadata(rootLabel = "Security", commandPrefix = "http-signature-")
public class HttpSignature {
    private final PemParser pemParser = new PemParser();

    @JsonRpcMethod(name = "http-signature-sign", documentation = "Compute the HTTP Signature of the provided request.")
    public String sign(@JsonRpcParam(value = "requestMethod", documentation = "Request method.", required = true) final HttpMethod method,
                       @JsonRpcParam(value = "requestUri", documentation = "Request uri.", required = true) final String uri,
                       @JsonRpcParam(value = "requestHeaders", documentation = "Request headers in properties format.", required = true) @UiWidget(TEXTAREA) final String requestHeader,
                       @JsonRpcParam(documentation = "KeyID to put in the signature header.") final String keyId,
                       @JsonRpcParam(documentation = "Key value to sign the request (in PEM for RSA/EC signatures).") final String key,
                       @JsonRpcParam(documentation = "Signature algorithm.", required = true) final Algorithm algo,
                       @JsonRpcParam(documentation = "Signature headers.", required = true) final List<String> headers) {
        return "Signature keyId=\"" + (keyId == null ? "<key>" : keyId) + '\"' +
                ",algorithm=\"" + algo.httpSignatureName + "\",headers=\"" +
                String.join(" ", headers) + '\"' + ",signature=\"" + doSign(method, uri, requestHeader, algo, key, headers) + '"';
    }

    @JsonRpcMethod(name = "http-signature-verify", documentation = "Verify the provided HTTP Signature.")
    public boolean verify(@JsonRpcParam(value = "requestMethod", documentation = "Request method.", required = true) final HttpMethod method,
                          @JsonRpcParam(value = "requestUri", documentation = "Request uri.", required = true) final String uri,
                          @JsonRpcParam(value = "requestHeaders", documentation = "Request headers in properties format.", required = true) @UiWidget(TEXTAREA) final String requestHeader,
                          @JsonRpcParam(documentation = "Key value to verify the request (in PEM for RSA/EC signatures).", required = true) final String key,
                          @JsonRpcParam(documentation = "Signature algorithm.", required = true) final Algorithm algo,
                          @JsonRpcParam(documentation = "Signature headers.", required = true) final List<String> headers,
                          @JsonRpcParam(documentation = "Signature to verify.", required = true) final String providedSignature) {
        return doVerify(method, uri, requestHeader, algo, key, headers, providedSignature);
    }

    private boolean doVerify(final HttpMethod method, final String uri, final String headerValues,
                             final Algorithm algo, final String rawKey, final List<String> headers,
                             final String expected) {
        final var signingString = createSigningString(headers, method.name(), uri, headerValues).getBytes(StandardCharsets.UTF_8);
        if (algo.httpSignatureName.startsWith("hmac")) {
            try {
                final var mac = Mac.getInstance(algo.jvmName);
                mac.init(new SecretKeySpec(rawKey.getBytes(StandardCharsets.UTF_8), algo.jvmName));
                return MessageDigest.isEqual(mac.doFinal(signingString), java.util.Base64.getDecoder().decode(findSignature(expected)));
            } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
                throw new IllegalStateException(e);
            }
        }
        try {
            final var signature = getSignature(algo.httpSignatureName, algo.jvmName);
            signature.initVerify((PublicKey) pemParser.decode(rawKey.getBytes(StandardCharsets.UTF_8)));
            signature.update(signingString);
            return signature.verify(java.util.Base64.getDecoder().decode(expected));
        } catch (final NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new IllegalStateException(e);
        }
    }

    private String findSignature(final String in) {
        final int from = in.indexOf("signature=\"");
        if (from > 0) {
            final int end = in.indexOf("\"", from + "signature=\"".length() + 1);
            if (end > 0) {
                return in.substring(from + "signature=\"".length(), end);
            }
        }
        return in;
    }

    private static Signature getSignature(final String algo, final String jvmAlgo) throws NoSuchAlgorithmException {
        final var signature = Signature.getInstance(jvmAlgo);
        if (jvmAlgo.startsWith("RSASSA")) {
            try {
                switch (algo) {
                    case "rsassa", "rsassa-pss256" -> signature.setParameter(
                            new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
                    case "rsassa-pss384" -> signature.setParameter(
                            new PSSParameterSpec("SHA-384", "MGF1", MGF1ParameterSpec.SHA384, 384 / 8, 1));
                    case "rsassa-pss512" -> signature.setParameter(
                            new PSSParameterSpec("SHA-512", "MGF1", MGF1ParameterSpec.SHA512, 512 / 8, 1));
                    default -> {
                    }
                }
            } catch (final InvalidAlgorithmParameterException e) {
                throw new IllegalStateException(e);
            }
        }
        return signature;
    }

    private String doSign(final HttpMethod method, final String uri, final String headerValues,
                          final Algorithm algo, final String rawKey, final List<String> headers) {
        final var signingString = createSigningString(headers, method.name(), uri, headerValues).getBytes(StandardCharsets.UTF_8);
        if (algo.httpSignatureName.startsWith("hmac")) {
            try {
                final var mac = Mac.getInstance(algo.jvmName);
                mac.init(new SecretKeySpec(rawKey.getBytes(StandardCharsets.UTF_8), algo.jvmName));
                return java.util.Base64.getEncoder().encodeToString(mac.doFinal(signingString));
            } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
                throw new IllegalStateException(e);
            }
        }
        try {
            final var signature = Signature.getInstance(algo.jvmName);
            signature.initSign((PrivateKey) pemParser.decode(rawKey.getBytes(StandardCharsets.UTF_8)));
            signature.update(signingString);
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (final NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new IllegalStateException(e);
        }
    }

    private String createSigningString(final List<String> headers, final String method, final String uri, final String headerValues) {
        final var providedHeaders = headers(headerValues);
        return headers.stream()
                .map(it -> it.toLowerCase(ROOT))
                .map(header -> {
                    if ("(request-target)".equalsIgnoreCase(header)) {
                        String path;
                        try {
                            final var url = new URL(uri);
                            path = url.getPath() + (url.getQuery() == null || url.getQuery().isEmpty() ? "" : ('?' + url.getQuery()));
                        } catch (final MalformedURLException e) {
                            path = uri;
                        }
                        return "(request-target): " + method.toLowerCase(ROOT) + " " + path;
                    }
                    if ("(expires)".equals(header)) {
                        final var value = providedHeaders.getProperty(header);
                        if (value != null) {
                            return value;
                        }
                        throw new IllegalArgumentException("Missing " + header + " in signature");
                    }
                    if ("(created)".equals(header)) {
                        final var value = providedHeaders.getProperty(header);
                        if (value != null) {
                            return value;
                        }
                        throw new IllegalArgumentException("Missing '" + header + "' in signature");
                    }

                    final var value = ofNullable(providedHeaders.getProperty(header))
                            .map(String::strip)
                            .orElseThrow(() -> new IllegalStateException("Missing header '" + header + "' in request."));
                    return header + ": " + value;
                })
                .collect(joining("\n"));
    }

    private Properties headers(final String values) {
        final var properties = new Properties();
        if (values != null && !values.isEmpty()) {
            try (final var r = new StringReader(values)) {
                properties.load(r);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return properties;
    }

    public enum Algorithm {
        hmac_sha1("hmac-sha1", "HmacSHA1"),
        hmac_sha224("hmac-sha224", "HmacSHA224"),
        hmac_sha256("hmac-sha256", "HmacSHA256"),
        hmac_sha384("hmac-sha384", "HmacSHA384"),
        hmac_sha512("hmac-sha512", "HmacSHA512"),
        rsa_sha1("rsa-sha1", "SHA1withRSA"),
        rsa_sha224("rsa-sha224", "SHA224withRSA"),
        rsa_sha256("rsa-sha256", "SHA256withRSA"),
        rsa_sha384("rsa-sha384", "SHA384withRSA"),
        rsa_sha512("rsa-sha512", "SHA512withRSA"),
        rsa_sha3_256("rsa-sha3-256", "SHA3-256withRSA"),
        rsa_sha3_384("rsa-sha3-384", "SHA3-384withRSA"),
        rsa_sha3_512("rsa-sha3-512", "SHA3-512withRSA"),
        dsa_sha1("dsa-sha1", "SHA1withDSA"),
        dsa_sha224("dsa-sha224", "SHA224withDSA"),
        dsa_sha256("dsa-sha256", "SHA256withDSA"),
        dsa_sha3_256("dsa-sha256", "SHA3-256withDSA"),
        dsa_sha3_384("dsa-sha256", "SHA3-256withDSA"),
        dsa_sha3_512("dsa-sha256", "SHA3-256withDSA"),
        ecdsa_sha1("ecdsa-sha1", "SHA1withECDSA"),
        ecdsa_sha256("ecdsa-sha256", "SHA256withECDSA"),
        ecdsa_sha384("ecdsa-sha384", "SHA384withECDSA"),
        ecdsa_sha512("ecdsa-sha512", "SHA512withECDSA"),
        ecdsa_sha3_256("ecdsa-sha3-256", "SHA3-256withECDSA"),
        ecdsa_sha3_384("ecdsa-sha3-384", "SHA3-384withECDSA"),
        ecdsa_sha3_512("ecdsa-sha3-512", "SHA3-512withECDSA"),
        ecdsa_sha256_256("ecdsa-sha256-256", "SHA256withECDSAinP1363Format"),
        ecdsa_sha256_384("ecdsa-sha256-384", "SHA384withECDSAinP1363Format"),
        ecdsa_sha256_512("ecdsa-sha256-512", "SHA512withECDSAinP1363Format"),
        rsassa_pss("rsassa-pss", "RSASSA-PSS"),
        rsassa_pss256("rsassa-pss256", "RSASSA-PSS"),
        rsassa_pss384("rsassa-pss384", "RSASSA-PSS"),
        rsassa_pss512("rsassa-pss512", "RSASSA-PSS");

        private final String httpSignatureName;
        private final String jvmName;

        Algorithm(final String httpSignatureName, final String jvmName) {
            this.httpSignatureName = httpSignatureName;
            this.jvmName = jvmName;
        }
    }

    public enum HttpMethod {
        CONNECT,
        DELETE,
        GET,
        HEAD,
        OPTIONS,
        PATCH,
        POST,
        PUT,
        TRACE
    }
}
