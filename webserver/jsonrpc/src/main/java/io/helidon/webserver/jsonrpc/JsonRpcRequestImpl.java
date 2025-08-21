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

import java.util.Optional;

import io.helidon.common.socket.PeerInfo;
import io.helidon.common.uri.UriInfo;
import io.helidon.common.uri.UriPath;
import io.helidon.common.uri.UriQuery;
import io.helidon.http.Header;
import io.helidon.http.HttpPrologue;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.jsonrpc.core.JsonRpcMessageRequest;
import io.helidon.jsonrpc.core.JsonRpcParams;
import io.helidon.webserver.http.HttpRequest;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * An implementation of a JSON-RPC request.
 */
class JsonRpcRequestImpl implements JsonRpcRequest {

    private final HttpRequest requestDelegate;
    private final JsonRpcMessageRequest messageDelegate;

    JsonRpcRequestImpl(HttpRequest delegate, JsonObject request) {
        this.requestDelegate = delegate;
        this.messageDelegate = JsonRpcMessageRequest.create(request);
    }

    @Override
    public JsonRpcMessageRequest rpcId(JsonValue rpcId) {
        return messageDelegate.rpcId(rpcId);
    }

    @Override
    public JsonRpcMessageRequest rpcId(int rpcId) {
        return messageDelegate.rpcId(rpcId);
    }

    @Override
    public JsonRpcMessageRequest rpcId(String rpcId) {
        return messageDelegate.rpcId(rpcId);
    }

    @Override
    public JsonRpcMessageRequest rpcMethod(String rpcMethod) {
        return messageDelegate.rpcMethod(rpcMethod);
    }

    @Override
    public JsonRpcParams params() {
        return messageDelegate.params();
    }

    @Override
    public String version() {
        return messageDelegate.version();
    }

    @Override
    public String rpcMethod() {
        return messageDelegate.rpcMethod();
    }

    @Override
    public Optional<JsonValue> rpcId() {
        return messageDelegate.rpcId();
    }

    @Override
    public JsonObject asJsonObject() {
        return messageDelegate.asJsonObject();
    }

    @Override
    public HttpPrologue prologue() {
        return requestDelegate.prologue();
    }

    @Override
    public ServerRequestHeaders headers() {
        return requestDelegate.headers();
    }

    @Override
    public UriPath path() {
        return requestDelegate.path();
    }

    @Override
    public UriQuery query() {
        return requestDelegate.query();
    }

    @Override
    public PeerInfo remotePeer() {
        return requestDelegate.remotePeer();
    }

    @Override
    public PeerInfo localPeer() {
        return requestDelegate.localPeer();
    }

    @Override
    public String authority() {
        return requestDelegate.authority();
    }

    @Override
    public void header(Header header) {
        requestDelegate.header(header);
    }

    @Override
    public int id() {
        return requestDelegate.id();
    }

    @Override
    public UriInfo requestedUri() {
        return requestDelegate.requestedUri();
    }
}
