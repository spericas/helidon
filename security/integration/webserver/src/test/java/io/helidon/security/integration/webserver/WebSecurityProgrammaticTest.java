/*
 * Copyright (c) 2018, 2021 Oracle and/or its affiliates.
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

package io.helidon.security.integration.webserver;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.config.Config;
import io.helidon.security.Security;
import io.helidon.security.SecurityContext;
import io.helidon.security.util.TokenHandler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;

import org.junit.jupiter.api.BeforeAll;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test for {@link WebSecurity}.
 */
public class WebSecurityProgrammaticTest extends WebSecurityTests {
    private static String baseUri;

    @BeforeAll
    public static void initClass() throws InterruptedException {
        WebSecurityTestUtil.auditLogFinest();
        myAuditProvider = new UnitTestAuditProvider();

        Config config = Config.create();

        Security security = Security.builder(config.get("security"))
                .addAuditProvider(myAuditProvider).build();

        Routing routing = Routing.builder()
                .register(WebSecurity.create(security)
                                  .securityDefaults(
                                          SecurityHandler.create()
                                                  .queryParam(
                                                          "jwt",
                                                          TokenHandler.builder()
                                                                  .tokenHeader("BEARER_TOKEN")
                                                                  .tokenPattern(Pattern.compile("bearer (.*)"))
                                                                  .build())
                                                  .queryParam(
                                                          "name",
                                                          TokenHandler.builder()
                                                                  .tokenHeader("NAME_FROM_REQUEST")
                                                                  .build())))
                .get("/noRoles", WebSecurity.secure())
                .get("/user[/{*}]", WebSecurity.rolesAllowed("user"))
                .get("/admin", WebSecurity.rolesAllowed("admin"))
                .get("/deny", WebSecurity.rolesAllowed("deny"), (req, res) -> {
                    res.status(Http.Status.INTERNAL_SERVER_ERROR_500);
                    res.send("Should not get here, this role doesn't exist");
                })
                .get("/auditOnly", WebSecurity
                        .audit()
                        .auditEventType("unit_test")
                        .auditMessageFormat(AUDIT_MESSAGE_FORMAT)
                )
                .get("/{*}", (req, res) -> {
                    Optional<SecurityContext> securityContext = req.context().get(SecurityContext.class);
                    res.headers().contentType(MediaType.TEXT_PLAIN.withCharset("UTF-8"));
                    res.send("Hello, you are: \n" + securityContext
                            .map(ctx -> ctx.user().orElse(SecurityContext.ANONYMOUS).toString())
                            .orElse("Security context is null"));
                })
                .build();

        server = WebServer.create(routing);
        long t = System.currentTimeMillis();
        CountDownLatch cdl = new CountDownLatch(1);
        server.start().thenAccept(webServer -> {
            long time = System.currentTimeMillis() - t;
            System.out.println("Started server on localhost:" + webServer.port() + " in " + time + " millis");
            cdl.countDown();
        });

        //we must wait for server to start, so other tests are not triggered until it is ready!
        assertThat("Timeout while waiting for server to start!", cdl.await(5, TimeUnit.SECONDS), is(true));

        baseUri = "http://localhost:" + server.port();
    }

    @Override
    String serverBaseUri() {
        return baseUri;
    }
}
