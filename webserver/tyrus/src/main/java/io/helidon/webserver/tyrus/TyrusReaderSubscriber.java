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

package io.helidon.webserver.tyrus;

import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Flow;

import org.glassfish.tyrus.spi.ReadHandler;

/**
 * Class TyrusReaderSubscriber.
 */
public class TyrusReaderSubscriber implements Flow.Subscriber<DataChunk> {

    private ReadHandler handler;

    void readHandler(ReadHandler handler) {
        this.handler = handler;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(DataChunk item) {
        if (handler != null) {
            handler.handle(item.data());
        }
    }

    @Override
    public void onError(Throwable throwable) {
    }

    @Override
    public void onComplete() {
    }
}
