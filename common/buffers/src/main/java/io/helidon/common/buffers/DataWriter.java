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

package io.helidon.common.buffers;

/**
 * Write data to the underlying transport (most likely a socket).
 */
public interface DataWriter extends AutoCloseable {
    /**
     * Write buffers, may delay writing and may write on a different thread.
     * This method also may combine multiple calls into a single write to the underlying transport.
     * @param buffers buffers to write
     */
    void write(BufferData... buffers);

    /**
     * Write buffer, may delay writing and may write on a different thread.
     * This method also may combine multiple calls into a single write to the underlying transport.
     * @param buffer buffer to write
     */
    void write(BufferData buffer);

    /**
     * Writes a buffer that remains owned by the caller and may be reused immediately after this method returns.
     * The writer must not retain the buffer or its backing storage after this method returns. Any writes submitted
     * before this invocation must be written first.
     * <p>
     * This method consumes the buffer. The caller may reset or otherwise reuse it after the method returns.
     *
     * @param buffer buffer to write
     */
    default void writeBorrowed(BufferData buffer) {
        writeNow(buffer);
    }

    /**
     * Write buffers to underlying transport blocking until the buffers are written. Any writes submitted before this
     * invocation must be written first.
     *
     * @param buffers buffers to write
     */
    void writeNow(BufferData... buffers);

    /**
     * Write buffer to underlying transport blocking until the buffer is written. Any writes submitted before this
     * invocation must be written first.
     *
     * @param buffer buffer to write
     */
    void writeNow(BufferData buffer);

    /**
     * Flushes to the underlying transport any pending data that has been written using
     * either {@link #write(BufferData)} or {@link #write(BufferData...)}.
     */
    default void flush() {
    }

    /**
     * Closes this writer and frees any associated resources. Defaults to just a call
     * to {@link #flush()}.
     */
    default void close() {
        flush();
    }
}
