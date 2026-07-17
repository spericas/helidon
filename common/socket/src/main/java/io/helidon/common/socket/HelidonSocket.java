/*
 * Copyright (c) 2022, 2026 Oracle and/or its affiliates.
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

package io.helidon.common.socket;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import io.helidon.common.buffers.BufferData;

/**
 * Socket abstraction to allow usage of TLS or even non-socket transport.
 */
public interface HelidonSocket extends SocketContext, Supplier<byte[]> {
    /**
     * Close the underlying socket.
     */
    void close();

    /**
     * Sets the socket to idle mode. Idle mode expects no bytes coming over the
     * socket but keeps reading exactly one byte in case connection is severed.
     * Idle mode should be used in case of client side connection caching.
     */
    void idle();

    /**
     * Check if socket is connected.
     * @return true if connected
     */
    boolean isConnected();

    /**
     * Write the buffer to the underlying socket. This method blocks until all bytes are written.
     *
     * @param buffer buffer to write
     */
    void write(BufferData buffer);

    /**
     * Write a caller-owned buffer to the underlying socket. This method blocks until all bytes are written and does not
     * retain the buffer or its backing storage after returning.
     *
     * @param buffer borrowed buffer to write
     */
    default void writeBorrowed(BufferData buffer) {
        write(buffer);
    }

    /**
     * Whether a protocol was negotiated by the socket (such as ALPN when using TLS).
     * @return whether a protocol was negotiated
     */
    default boolean protocolNegotiated() {
        return false;
    }

    /**
     * Protocol that was negotiated.
     *
     * @return protocol name
     * @throws java.util.NoSuchElementException in case there is no negotiated protocol
     */
    default String protocol() {
        throw new NoSuchElementException("No protocol negotiated");
    }
}
