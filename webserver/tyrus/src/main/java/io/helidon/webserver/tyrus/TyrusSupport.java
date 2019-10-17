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

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.websocket.DeploymentException;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import org.glassfish.tyrus.core.RequestContext;
import org.glassfish.tyrus.core.TyrusUpgradeResponse;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.spi.CompletionHandler;
import org.glassfish.tyrus.spi.Connection;
import org.glassfish.tyrus.spi.WebSocketEngine;

/**
 * Class TyrusSupport.
 */
public class TyrusSupport implements Service {
    private static final Logger LOGGER = Logger.getLogger(TyrusSupport.class.getName());

    private static final ByteBuffer FLUSH_BUFFER = ByteBuffer.allocateDirect(0);

    private final WebSocketEngine engine;
    private final TyrusHandler handler = new TyrusHandler();

    TyrusSupport(WebSocketEngine engine) {
        this.engine = engine;
    }

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

        private Set<Class<?>> endpointClasses = new HashSet<>();

        private Builder() {
        }

        Builder register(Class<?> endpointClass) {
            endpointClasses.add(endpointClass);
            return this;
        }

        @Override
        public TyrusSupport build() {
            // Create container and engine
            TyrusServerContainer serverContainer = new TyrusServerContainer(endpointClasses) {
                private final WebSocketEngine engine =
                        TyrusWebSocketEngine.builder(this).build();

                @Override
                public void register(Class<?> endpointClass) throws DeploymentException {
                    engine.register(endpointClass, "/");        // TODO
                }

                @Override
                public void register(ServerEndpointConfig serverEndpointConfig) throws DeploymentException {
                    engine.register(serverEndpointConfig, "/");     // TODO
                }

                @Override
                public WebSocketEngine getWebSocketEngine() {
                    return engine;
                }
            };

            // Register classes with engine
            WebSocketEngine engine = serverContainer.getWebSocketEngine();
            endpointClasses.forEach(c -> {
                try {
                    engine.register(c, "/");
                } catch (DeploymentException e) {
                    throw new RuntimeException(e);
                }
            });

            // Create TyrusSupport using WebSockets engine
            return new TyrusSupport(serverContainer.getWebSocketEngine());
        }
    }

    private class TyrusHandler implements Handler {

        @Override
        public void accept(ServerRequest req, ServerResponse res) {
            req.headers().value(HandshakeRequest.SEC_WEBSOCKET_KEY).ifPresent(key -> {
                LOGGER.fine("Initiating WebSocket handshake ...");



                // Create request context and copy headers
                RequestContext requestContext = RequestContext.Builder.create()
                        .requestURI(URI.create(req.path().toString()))
                        .build();
                req.headers().toMap().entrySet().forEach(e -> {
                    requestContext.getHeaders().put(e.getKey(), e.getValue());
                });

                final TyrusUpgradeResponse upgradeResponse = new TyrusUpgradeResponse();
                final WebSocketEngine.UpgradeInfo upgradeInfo = engine.upgrade(requestContext, upgradeResponse);
                switch (upgradeInfo.getStatus()) {
                    case HANDSHAKE_FAILED:
                        LOGGER.fine("WebSocket handshake failed");
                        break;
                    case NOT_APPLICABLE:
                        LOGGER.fine("WebSocket handshake not applicable");
                        break;
                    case SUCCESS:
                        LOGGER.fine("WebSocket handshake successful");

                        // Respond to upgrade request
                        res.status(upgradeResponse.getStatus());
                        upgradeResponse.getHeaders().entrySet().forEach(e ->
                                res.headers().add(e.getKey(), e.getValue()));
                        TyrusWriterPublisher writer = new TyrusWriterPublisher();
                        res.send(writer);
                        writer.write(FLUSH_BUFFER, null);

                        // Setup the WebSocket connection
                        Connection connection = upgradeInfo.createConnection(writer, (closeReason) -> {
                            // TODO - close connection
                        });

                        // Set up reader to pass data back to Tyrus
                        TyrusReaderSubscriber reader = new TyrusReaderSubscriber();
                        reader.readHandler(connection.getReadHandler());
                        req.content().subscribe(reader);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + upgradeInfo.getStatus());
                }
            });
        }
    }
}
