/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

package io.helidon.webclient.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.helidon.webclient.api.ClientUri;

public class GrpcChannel extends Channel {

    private final GrpcClientImpl grpcClient;

    public GrpcChannel(GrpcClient grpcClient) {
        this.grpcClient = (GrpcClientImpl) grpcClient;
    }

    @Override
    public <ReqT, ResT> ClientCall<ReqT, ResT> newCall(
            MethodDescriptor<ReqT, ResT> methodDescriptor, CallOptions callOptions) {
        return new GrpcClientCall<>(grpcClient, methodDescriptor);
    }

    @Override
    public String authority() {
        ClientUri clientUri = grpcClient.prototype().baseUri().orElseThrow();
        return clientUri.authority();
    }
}
