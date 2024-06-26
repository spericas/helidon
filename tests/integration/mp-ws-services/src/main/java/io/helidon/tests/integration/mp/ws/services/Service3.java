/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates.
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
package io.helidon.tests.integration.mp.ws.services;

import io.helidon.microprofile.server.RoutingPath;
import io.helidon.webserver.Routing;
import io.helidon.webserver.Service;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service example.
 */
@Priority(3)
@ApplicationScoped
@RoutingPath("/services")
public class Service3 implements Service {
    @Override
    public void update(Routing.Rules rules) {
        rules.get("/service3", (req, res) -> res.send("service3"))
                .get("/", (req, res) -> res.send("service3"))
                .get("/shared", (req, res) -> res.send("service3"));
    }
}
