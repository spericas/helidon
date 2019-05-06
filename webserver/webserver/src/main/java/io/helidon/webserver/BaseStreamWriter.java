/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.webserver;

import java.util.Objects;
import java.util.function.Function;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow;

/**
 * Class BaseStreamWriter.
 **/
public abstract class BaseStreamWriter<T> implements Function<Flow.Publisher<T>, Flow.Publisher<DataChunk>> {

    private final ServerResponse response;
    private final Class<T> type;
    private final DataChunk beginChunk;
    private final DataChunk separatorChunk;
    private final DataChunk endChunk;

    public BaseStreamWriter(ServerResponse response,
                            Class<T> type,
                            DataChunk beginChunk,
                            DataChunk separatorChunk,
                            DataChunk endChunk) {
        Objects.requireNonNull(response, "Response must be non-null");
        Objects.requireNonNull(type, "Type must be non-null");

        this.response = response;
        this.type = type;
        this.beginChunk = beginChunk;
        this.separatorChunk = separatorChunk;
        this.endChunk = endChunk;
    }

    public ServerResponse getResponse() {
        return response;
    }

    public Class<T> getType() {
        return type;
    }

    public DataChunk getBeginChunk() {
        return beginChunk;
    }

    public DataChunk getSeparatorChunk() {
        return separatorChunk;
    }

    public DataChunk getEndChunk() {
        return endChunk;
    }
}
