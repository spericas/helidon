/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.examples.nima.udp;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public interface UdpMultiClient extends UdpMessageSender {

    List<UdpClient> udpClients();

    void addUdpClient(UdpClient udpClient);

    boolean removeUdpClient(UdpClient udpClient);

    @Override
    default void sendMessage(Object msg) throws IOException {
        for (var c : udpClients()) {
            c.sendMessage(msg);
        }
    }

    @Override
    default void sendMessage(byte[] msg) throws IOException {
        for (var c : udpClients()) {
            c.sendMessage(msg);
        }
    }

    @Override
    default void sendMessage(InputStream msg) throws IOException {
       var bytes = msg.readAllBytes();
        for (UdpClient c : udpClients()) {
            c.sendMessage(bytes);
        }
    }

    @Override
    default CompletableFuture<Void> sendMessageAsync(Object msg) {
        var futures = udpClients().stream()
                .map(c -> c.sendMessageAsync(msg))
                .collect(Collectors.toSet());
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{}));
    }

    @Override
    default CompletableFuture<Void> sendMessageAsync(byte[] msg) {
        var futures = udpClients().stream()
                .map(c -> c.sendMessageAsync(msg))
                .collect(Collectors.toSet());
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{}));
    }

    @Override
    default CompletableFuture<Void> sendMessageAsync(InputStream msg) {
        try {
            var bytes = msg.readAllBytes();
            var futures = udpClients().stream()
                    .map(c -> c.sendMessageAsync(bytes))
                    .collect(Collectors.toSet());
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[]{}));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
