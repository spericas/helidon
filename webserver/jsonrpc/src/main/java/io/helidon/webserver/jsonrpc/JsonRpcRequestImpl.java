/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.webserver.jsonrpc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

import io.helidon.common.socket.PeerInfo;
import io.helidon.common.uri.UriInfo;
import io.helidon.common.uri.UriPath;
import io.helidon.common.uri.UriQuery;
import io.helidon.http.Header;
import io.helidon.http.HttpPrologue;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.jsonrpc.core.JsonRpcMessage;
import io.helidon.jsonrpc.core.JsonRpcMessageType;
import io.helidon.jsonrpc.core.JsonRpcParams;
import io.helidon.jsonrpc.core.JsonUtil;
import io.helidon.webserver.http.HttpRequest;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

/**
 * An implementation of a JSON-RPC request.
 */
class JsonRpcRequestImpl implements JsonRpcRequest {

    private final HttpRequest delegate;
    private final JsonObject request;

    JsonRpcRequestImpl(HttpRequest delegate, JsonObject request) {
        this.delegate = delegate;
        this.request = request;
    }

    JsonRpcRequestImpl(JsonRpcMessage message) {
        this.delegate = null;
        JsonObjectBuilder builder = JsonUtil.JSON_BUILDER_FACTORY.createObjectBuilder();
        builder.add("jsonrpc", "2.0");
        builder.add("method", message.rpcMethod());

        // set the id using for a request using proper conversions
        if (message.type() == JsonRpcMessageType.REQUEST) {
            Object id = message.rpcId().orElseThrow(
                    () -> new IllegalArgumentException("Missing JSON-RPC ID"));
            if (id == null) {
                builder.add("id", JsonValue.EMPTY_JSON_OBJECT);
            } else {
                switch (id) {
                case String s -> builder.add("id", s);
                case Integer i -> builder.add("id", i);
                case JsonValue jsonValue -> builder.add("id", jsonValue);
                case Double v -> builder.add("id", v);
                case BigDecimal bigDecimal -> builder.add("id", bigDecimal);
                case BigInteger bigInteger -> builder.add("id", bigInteger);
                default -> throw new IllegalArgumentException("Invalid JSON-RPC ID " + id);
                }
            }
        }

        // set the params using proper conversions
        if (message.param().isPresent()) {
            builder.add("params", message.param().get());
        } else if (!message.params().isEmpty()) {
            JsonObjectBuilder paramBuilder = JsonUtil.JSON_BUILDER_FACTORY.createObjectBuilder();
            message.params().forEach((key, value) -> {
                switch (value) {
                case String s -> paramBuilder.add(key, s);
                case Integer i -> paramBuilder.add(key, i);
                case Boolean b -> paramBuilder.add(key, b);
                case JsonValue jsonValue -> paramBuilder.add(key, jsonValue);
                case Double v -> paramBuilder.add(key, v);
                case BigDecimal bigDecimal -> paramBuilder.add(key, bigDecimal);
                case BigInteger bigInteger -> paramBuilder.add(key, bigInteger);
                default -> paramBuilder.add(key, JsonUtil.jsonbToJsonp(value));
                }
            });
            builder.add("params", paramBuilder.build());
        }

        // build and set request
        this.request = builder.build();
    }

    @Override
    public String version() {
        return request.getString("jsonrpc");
    }

    @Override
    public String rpcMethod() {
        return request.getString("method");
    }

    @Override
    public Optional<JsonValue> rpcId() {
        return Optional.ofNullable(request.get("id"));
    }

    @Override
    public JsonRpcParams params() {
        JsonValue value = request.get("params");
        if (value == null) {
            value = JsonValue.EMPTY_JSON_OBJECT;
        }
        return JsonRpcParams.create((JsonStructure) value);
    }

    @Override
    public JsonObject asJsonObject() {
        return request;
    }

    @Override
    public HttpPrologue prologue() {
        return delegate.prologue();
    }

    @Override
    public ServerRequestHeaders headers() {
        return delegate.headers();
    }

    @Override
    public UriPath path() {
        return delegate.path();
    }

    @Override
    public UriQuery query() {
        return delegate.query();
    }

    @Override
    public PeerInfo remotePeer() {
        return delegate.remotePeer();
    }

    @Override
    public PeerInfo localPeer() {
        return delegate.localPeer();
    }

    @Override
    public String authority() {
        return delegate.authority();
    }

    @Override
    public void header(Header header) {
        delegate.header(header);
    }

    @Override
    public int id() {
        return delegate.id();
    }

    @Override
    public UriInfo requestedUri() {
        return delegate.requestedUri();
    }
}
