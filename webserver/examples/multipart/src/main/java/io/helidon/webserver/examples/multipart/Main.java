/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.webserver.examples.multipart;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.helidon.common.reactive.Flow;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.StaticContentSupport;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.multipart.BodyPart;
import io.helidon.webserver.multipart.MultiPart;
import io.helidon.webserver.multipart.MultiPartSupport;

import static io.helidon.webserver.multipart.MultiPartSupport.multipart;

public class Main {

    /**
     * Creates new {@link Routing}.
     *
     * @return the new instance
     */
    private static Routing createRouting() {
        return Routing.builder()
                .register(StaticContentSupport.builder("/static", Main.class.getClassLoader())
                        .welcomeFileName("index.html")
                        .build())
                .register(new MultiPartSupport())
                .any("/upload", Main::handleUpload)
                .any("/download", Main::handleDownload)
                .build();
    }

    private static void handleUpload(ServerRequest req, ServerResponse res) {
        log("handleUpload");

        req.content().as(MultiPart.class).thenAccept(multiPart ->
                multiPart.subscribe(new Flow.Subscriber<BodyPart>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(BodyPart bodyPart) {
                        bodyPart.content().as(String.class).thenAccept(str -> {
                            log(str);
                        });
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        res.status(500).send("Error uploading files");
                    }

                    @Override
                    public void onComplete() {
                        log("handleStreamingUpload sending response");
                        res.send("Files uploaded successfully");
                    }
                })
        );
    }

    private static void handleDownload(ServerRequest req, ServerResponse res) {
        log("handleDownload");

        MultiPart multiPart = MultiPart.builder()
                .bodyPart(BodyPart.builder().build())
                .bodyPart(BodyPart.builder().build())
                .build();

        res.send(multiPart);
    }

    // --

    private static void log(String message) {
        System.out.println("LOG: (" + Thread.currentThread().getName() + ") " + message);
        System.out.flush();
    }

    /**
     * Application main entry point.
     *
     * @param args command line arguments.
     * @throws IOException if there are problems reading logging properties
     */
    public static void main(String[] args) throws IOException {
        startServer();
    }

    /**
     * Start the server.
     *
     * @return the created {@link WebServer} instance
     * @throws IOException if there are problems reading logging properties
     */
    protected static WebServer startServer() throws IOException {

        WebServer server = WebServer.create(
                ServerConfiguration.builder()
                        .port(8080)
                        .build(),
                createRouting());

        // Start the server and print some info.
        server.start().thenAccept(ws -> {
            log("WEB server is up! http://localhost:" + ws.port());
        });

        // Server threads are not demon. NO need to block. Just react.
        server.whenShutdown().thenRun(()
                -> log("WEB server is DOWN. Good bye!"));

        return server;
    }
}
