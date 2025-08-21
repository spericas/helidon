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

import java.io.OutputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

import io.helidon.common.uri.UriQuery;
import io.helidon.http.Header;
import io.helidon.http.ServerResponseHeaders;
import io.helidon.http.ServerResponseTrailers;
import io.helidon.http.Status;
import io.helidon.jsonrpc.core.JsonRpcError;
import io.helidon.jsonrpc.core.JsonRpcMessageResponse;
import io.helidon.jsonrpc.core.JsonRpcResult;
import io.helidon.webserver.http.ServerResponse;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * An implementation of JSON-RPC response.
 */
class JsonRpcResponseImpl implements JsonRpcResponse {

    private final ServerResponse responseDelegate;
    private final JsonRpcMessageResponse messageDelegate;

    private Status status = Status.OK_200;

    JsonRpcResponseImpl(JsonValue rpcId, ServerResponse delegate) {
        this.responseDelegate = Objects.requireNonNull(delegate);
        this.messageDelegate = JsonRpcMessageResponse.create(rpcId);
    }

    @Override
    public JsonRpcResponse rpcId(JsonValue rpcId) {
        messageDelegate.rpcId(rpcId);
        return this;
    }

    @Override
    public JsonRpcResponse rpcId(int rpcId) {
        messageDelegate.rpcId(rpcId);
        return this;
    }

    @Override
    public JsonRpcResponse rpcId(String rpcId) {
        messageDelegate.rpcId(rpcId);
        return this;
    }

    @Override
    public JsonRpcResponse result(JsonValue result) {
        messageDelegate.result(result);
        return this;
    }

    @Override
    public JsonRpcResponse result(Object object) {
        messageDelegate.result(object);
        return this;
    }

    @Override
    public JsonRpcMessageResponse result(JsonRpcResult result) {
        return messageDelegate.result(result);
    }

    @Override
    public JsonRpcResponse error(int code, String message) {
        messageDelegate.error(code, message);
        return this;
    }

    @Override
    public JsonRpcResponse error(int code, String message, JsonValue data) {
        messageDelegate.error(code, message, data);
        return this;
    }

    @Override
    public JsonRpcMessageResponse error(JsonRpcError error) {
        return messageDelegate.error(error);
    }

    @Override
    public Optional<JsonValue> rpcId() {
        return messageDelegate.rpcId();
    }

    @Override
    public Optional<JsonRpcResult> result() {
        return messageDelegate.result();
    }

    @Override
    public Optional<JsonRpcError> error() {
        return messageDelegate.error();
    }

    @Override
    public JsonObject asJsonObject() {
        return messageDelegate.asJsonObject();
    }

    @Override
    public JsonRpcResponse status(int status) {
        this.status = Status.create(status);
        return this;
    }

    @Override
    public JsonRpcResponse status(Status status) {
        this.status = status;
        return this;
    }

    @Override
    public Status status() {
        return status;
    }

    @Override
    public void send() {
        throw new UnsupportedOperationException("This method should be overridden");
    }

    @Override
    public ServerResponse header(Header header) {
        return responseDelegate.header(header);
    }

    @Override
    public void send(byte[] bytes) {
        responseDelegate.send(bytes);
    }

    @Override
    public void send(Object entity) {
        responseDelegate.send(entity);
    }

    @Override
    public boolean isSent() {
        return responseDelegate.isSent();
    }

    @Override
    public OutputStream outputStream() {
        return responseDelegate.outputStream();
    }

    @Override
    public long bytesWritten() {
        return responseDelegate.bytesWritten();
    }

    @Override
    public ServerResponse whenSent(Runnable listener) {
        return responseDelegate.whenSent(listener);
    }

    @Override
    public ServerResponse reroute(String newPath) {
        return responseDelegate.reroute(newPath);
    }

    @Override
    public ServerResponse reroute(String path, UriQuery query) {
        return responseDelegate.reroute(path, query);
    }

    @Override
    public ServerResponse next() {
        return responseDelegate.next();
    }

    @Override
    public ServerResponseHeaders headers() {
        return responseDelegate.headers();
    }

    @Override
    public ServerResponseTrailers trailers() {
        return responseDelegate.trailers();
    }

    @Override
    public void streamResult(String result) {
        responseDelegate.streamResult(result);
    }

    @Override
    public void streamFilter(UnaryOperator<OutputStream> filterFunction) {
        responseDelegate.streamFilter(filterFunction);
    }
}
