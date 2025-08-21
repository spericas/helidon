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

import io.helidon.http.Status;
import io.helidon.jsonrpc.core.JsonRpcMessageResponse;
import io.helidon.webserver.http.ServerResponse;

import jakarta.json.JsonValue;

/**
 * A representation of a JSON-RPC response.
 */
public interface JsonRpcResponse extends JsonRpcMessageResponse, ServerResponse {

    @Override
    JsonRpcResponse rpcId(JsonValue rpcId);

    @Override
    JsonRpcResponse rpcId(int rpcId);

    @Override
    JsonRpcResponse rpcId(String rpcId);

    @Override
    JsonRpcResponse result(JsonValue result);

    @Override
    JsonRpcResponse result(Object object);

    @Override
    JsonRpcResponse error(int code, String message);

    @Override
    JsonRpcResponse error(int code, String message, JsonValue data);

    /**
     * Set an HTTP status for the underlying response. Normally this will be
     * set by Helidon, but this method allows to override the default values.
     * The default value is {@link io.helidon.http.Status#OK_200_CODE}.
     *
     * @param status the status
     * @return this response
     */
    JsonRpcResponse status(int status);

    /**
     * Set an HTTP status for the underlying response. Normally this will be
     * set by Helidon, but this method allows to override the default values.
     * The default value is {@link io.helidon.http.Status#OK_200}.
     *
     * @param status the status
     * @return this response
     */
    default JsonRpcResponse status(Status status) {
        return status(status.code());
    }

    /**
     * Get the status set on this response.
     *
     * @return the status
     */
    Status status();

    /**
     * Send this response over the wire to the client. This method blocks
     * until the response is delivered.
     */
    void send();
}
