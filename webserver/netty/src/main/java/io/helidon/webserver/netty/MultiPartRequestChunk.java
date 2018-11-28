/*
 * Copyright (c) 2017, 2018 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.webserver.netty;

import io.helidon.common.http.BodyPartHeaders;
import io.helidon.common.http.MultiPartDataChunk;
import io.netty.buffer.ByteBuf;

/**
 * @author rgrecour
 */
class MultiPartRequestChunk extends ByteBufRequestChunk implements MultiPartDataChunk {

    private final BodyPartHeaders headers;
    private final boolean isLast;

    MultiPartRequestChunk(final ByteBuf buf,
                          final BodyPartHeaders headers, final boolean isLast,
                          final ReferenceHoldingQueue<ByteBufRequestChunk> referenceHoldingQueue) {
        super(buf, referenceHoldingQueue);
        this.headers = headers;
        this.isLast = isLast;
    }

    @Override
    public BodyPartHeaders headers() {
        return headers;
    }

    @Override
    public boolean isLast() {
        return isLast;
    }
}
