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

package io.helidon.webserver.multipart;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.concurrent.CountDownLatch;

import io.helidon.common.http.BodyPartHeaders;
import io.helidon.common.http.MediaType;
import io.helidon.common.reactive.Flow;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerConfiguration;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.json.JsonSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Class MultiPartTest.
 */
public class MultiPartTest {

    private static MultiPartServer server = new MultiPartServer();

    private String entity;
    private String contentType;

    @BeforeAll
    public static void startServer() throws InterruptedException {
        server.start();
    }

    @AfterAll
    public static void stopServer() throws InterruptedException {
        server.stop();
    }

    public void testDownloadForm() {
        Response res = newWebTarget(server).path("downloadForm").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());
        contentType = res.getHeaderString("Content-Type");
        entity = res.readEntity(String.class);
        log();
    }

    @Test
    public void testUploadForm() {
        if (entity == null) {
            testDownloadForm();
        }
        Response res = newWebTarget(server).path("uploadForm")
                .request()
                .post(Entity.entity(entity, contentType));
        assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());
        reset();
    }

    public void testDownloadMixed() {
        Response res = newWebTarget(server).path("downloadMixed").request().get();
        assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());
        entity = res.readEntity(String.class);
        contentType = res.getHeaderString("Content-Type");
        log();
    }

    // @Test
    public void testUploadMixed() {
        if (entity == null) {
            testDownloadMixed();
        }
        Response res = newWebTarget(server).path("uploadMixed")
                .request()
                .post(Entity.entity(entity, contentType));
        assertEquals(Response.Status.OK.getStatusCode(), res.getStatus());
        reset();
    }

    // -- MultiPartServer implementation --------------------------------------

    private static class MultiPartServer {

        private WebServer server;

        private Routing createRouting() {
            return Routing.builder()
                    .register(new MultiPartSupport())
                    .register(JsonSupport.get())
                    .post("/uploadForm", this::handleUploadForm)
                    .get("/downloadForm", this::handleDownloadForm)
                    .post("/uploadMixed", this::handleUploadMixed)
                    .get("/downloadMixed", this::handleDownloadMixed)
                    .build();
        }

        private void handleDownloadForm(ServerRequest req, ServerResponse res) {
            res.headers().contentType(MediaType.MULTIPART_FORM_DATA);

            MultiPart multiPart = MultiPart.builder()
                    .bodyPart(BodyPart.builder()
                            .headers(BodyPartHeaders.builder()
                                    .name("part1")
                                    .build())
                            .entity("This is part 1")
                            .build())
                    .bodyPart(BodyPart.builder()
                            .headers(BodyPartHeaders.builder()
                                    .name("part2")
                                    .build())
                            .entity("This is part 2")
                            .build())
                    .build();

            res.send(multiPart);
        }

        private void handleUploadForm(ServerRequest req, ServerResponse res) {
            req.content().as(MultiPart.class).thenAccept(multiPart ->
                    multiPart.subscribe(new Flow.Subscriber<BodyPart>() {
                        @Override
                        public void onSubscribe(Flow.Subscription subscription) {
                            subscription.request(Long.MAX_VALUE);
                        }

                        @Override
                        public void onNext(BodyPart bodyPart) {
                            assertTrue(bodyPart.headers().name().contains("part"));
                            bodyPart.content().as(String.class).thenAccept(str -> {
                                        System.out.println("#### part " + str);
                                        assertTrue(str.contains("This is part"));
                                    });
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            res.status(500).send("Error processing form");
                        }

                        @Override
                        public void onComplete() {
                            res.send("Form uploaded");
                        }
                    })
            );
        }

        private void handleDownloadMixed(ServerRequest req, ServerResponse res) {
            res.headers().contentType(MediaType.MULTIPART_MIXED);

            JsonObject json = Json.createObjectBuilder()
                    .add("msg", "This is part 2")
                    .build();

            MultiPart multiPart = MultiPart.builder()
                    .bodyPart(BodyPart.builder()
                            .headers(BodyPartHeaders.builder()
                                    .name("part1")
                                    .contentType(MediaType.TEXT_PLAIN)
                                    .build())
                            .entity("This is part 1")
                            .build())
                    .bodyPart(BodyPart.builder()
                            .headers(BodyPartHeaders.builder()
                                    .name("part2")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .build())
                            .entity(json)
                            .build())
                    .build();

            res.send(multiPart);
        }

        private void handleUploadMixed(ServerRequest req, ServerResponse res) {
            req.content().as(MultiPart.class).thenAccept(multiPart ->
                    multiPart.subscribe(new Flow.Subscriber<BodyPart>() {
                        @Override
                        public void onSubscribe(Flow.Subscription subscription) {
                            subscription.request(Long.MAX_VALUE);
                        }

                        @Override
                        public void onNext(BodyPart bodyPart) {
                            if (bodyPart.headers().contentType().contains("text/plain")) {
                                bodyPart.content().as(String.class).thenAccept(str ->
                                        assertTrue(str.contains("This is part"))
                                );
                            } else {
                                bodyPart.content().as(JsonObject.class).thenAccept(json ->
                                        assertNotNull(json)
                                );
                            }
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            res.status(500).send("Error uploading multipart");
                        }

                        @Override
                        public void onComplete() {
                            res.send("Multipart uploaded");
                        }
                    })
            );
        }

        int port() {
            if (server == null) {
                throw new IllegalStateException("Server is not started");
            }
            return server.port();
        }

        void start() throws InterruptedException {
            server = WebServer.create(ServerConfiguration.builder().port(8080).build(),
                    createRouting());
            CountDownLatch latch = new CountDownLatch(1);
            server.start().thenAccept(ws -> {
                System.out.println("MultiPartServer up at http://localhost:" + ws.port());
                latch.countDown();
            });
            latch.await();
        }

        void stop() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            server.shutdown().thenRun(() -> latch.countDown());
            latch.await();
            server = null;
        }
    }

    // -- Utility methods -----------------------------------------------------

    private WebTarget newWebTarget(MultiPartServer server) {
        return ClientBuilder.newClient().target("http://localhost:" + server.port());
    }

    private void reset() {
        entity = null;
        contentType = null;
    }

    private void log() {
        System.out.println("\n====");
        System.out.println("Content-Type: " + contentType);
        System.out.println(entity);
        System.out.println("====\n");
    }
}
