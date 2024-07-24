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

package io.helidon.examples.microprofile.grpc;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.grpc.api.Grpc;
import io.helidon.grpc.core.CollectingObserver;
import io.helidon.grpc.core.ResponseHelper;

import io.grpc.stub.StreamObserver;
import jakarta.enterprise.context.ApplicationScoped;

@Grpc.GrpcService
@ApplicationScoped
public class StringService {

    @Grpc.Unary("Upper")
    public Strings.StringMessage upper(Strings.StringMessage request) {
        return newMessage(request.getText().toUpperCase());
    }

    @Grpc.Unary("Lower")
    public Strings.StringMessage lower(Strings.StringMessage request) {
        return newMessage(request.getText().toLowerCase());
    }

    @Grpc.ServerStreaming("Split")
    public void split(Strings.StringMessage request, StreamObserver<Strings.StringMessage> observer) {
        String[] parts = request.getText().split(" ");
        ResponseHelper.stream(observer, Stream.of(parts).map(this::newMessage));
    }

    @Grpc.ClientStreaming("Join")
    public StreamObserver<Strings.StringMessage> join(StreamObserver<Strings.StringMessage> observer) {
        return CollectingObserver.create(
                Collectors.joining(" "),
                observer,
                Strings.StringMessage::getText,
                this::newMessage);
    }

    private Strings.StringMessage newMessage(String text) {
        return Strings.StringMessage.newBuilder().setText(text).build();
    }
}

