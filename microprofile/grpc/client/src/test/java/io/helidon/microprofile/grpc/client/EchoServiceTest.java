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

package io.helidon.microprofile.grpc.client;

import io.helidon.microprofile.grpc.client.test.Echo;
import io.helidon.microprofile.grpc.core.Grpc;
import io.helidon.microprofile.grpc.core.Unary;
import io.helidon.microprofile.grpc.server.GrpcMpCdiExtension;
import io.helidon.microprofile.testing.junit5.AddExtension;
import io.helidon.microprofile.testing.junit5.HelidonTest;

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;

import static io.helidon.grpc.core.ResponseHelper.complete;

@HelidonTest
@AddExtension(GrpcMpCdiExtension.class)
class EchoServiceTest {

    // @Test
    void testEcho() {
        ClientServiceDescriptor descriptor = ClientServiceDescriptor.builder(EchoService.class)
                .name("EchoService")
                .marshallerSupplier(new JavaMarshaller.Supplier())
                .unary("Echo")
                .build();

        Channel channel = null;     // TODO
        GrpcServiceClient client = GrpcServiceClient.create(channel, descriptor);
        StreamObserver<Echo.EchoResponse> observer = new StreamObserver<>() {
            @Override
            public void onNext(Echo.EchoResponse value) {
                // TODO check value
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
            }
        };
        client.unary("Echo", fromString("Howdy"), observer);
    }

    private Echo.EchoRequest fromString(String value) {
        return Echo.EchoRequest.newBuilder().setMessage(value).build();
    }

    @Grpc
    public static class EchoService {

        /**
         * Echo the message back to the caller.
         *
         * @param request the echo request containing the message to echo
         * @param observer the call response
         */
        @Unary(name = "Echo")
        public void echo(Echo.EchoRequest request, StreamObserver<Echo.EchoResponse> observer) {
            try {
                String message = request.getMessage();
                Echo.EchoResponse response = Echo.EchoResponse.newBuilder().setMessage(message).build();
                complete(observer, response);
            } catch (IllegalStateException e) {
                observer.onError(e);
            }
        }
    }
}
