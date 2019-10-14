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

import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

/**
 * Class TyrusSupport.
 */
public class TyrusSupport implements Service {

    private final TyrusHandler handler = new TyrusHandler();

    @Override
    public void update(Routing.Rules routingRules) {
        routingRules.any(handler);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for convenient way to create {@link TyrusSupport}.
     */
    public static final class Builder implements io.helidon.common.Builder<TyrusSupport> {

        private Builder() {
        }

        Builder register(Class<?> endpointClass) {
            // TODO
            return this;
        }

        @Override
        public TyrusSupport build() {
            return new TyrusSupport();
        }
    }

    private class TyrusHandler implements Handler {

        @Override
        public void accept(ServerRequest req, ServerResponse res) {
        }
    }
}
