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

import io.helidon.webserver.MultiPart;
import io.helidon.webserver.MultiPartSupport;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.StaticContentSupport;
import io.helidon.webserver.WebServer;
import java.io.IOException;

/**
 * 
 * @author rgrecour
 */
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
                .any("/upload", (req, res) -> {
                    req.content().as(MultiPart.class).thenAccept((multiPart) -> {
                        multiPart.onBodyPart((bodyPart) -> {
                            bodyPart.content().as(String.class).thenAccept((str) -> {
                                System.out.println("File uploaded !");
                            });
                        }).onComplete().thenRun(() -> {
                            System.out.println("sending response");
                            res.send("Files uploaded successfully");
                        }).exceptionally((Throwable t) -> {
                            res.status(500);
                            res.send();
                            return null;
                        });
                    });
                })
                .build();
    }

    /**
     * Application main entry point.
     *
     * @param args command line arguments.
     * @throws IOException if there are problems reading logging properties
     */
    public static void main(String[] args) throws IOException{
        startServer();
    }

    /**
     * Start the server.
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
            System.out.println(
                    "WEB server is up! http://localhost:" + ws.port());
        });

        // Server threads are not demon. NO need to block. Just react.
        server.whenShutdown().thenRun(()
                -> System.out.println("WEB server is DOWN. Good bye!"));

        return server;
    }
}
