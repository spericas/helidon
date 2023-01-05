/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.tracing.jaeger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.helidon.microprofile.tests.junit5.AddBean;
import io.helidon.microprofile.tests.junit5.HelidonTest;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.opentracing.Traced;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@HelidonTest
@AddBean(Gh5633Test.TestResource.class)
@AddBean(Gh5633Test.ResourceHelper.class)
@AddBean(Gh5633Test.Service.class)
class Gh5633Test {

    @Inject
    private WebTarget target;

    @Test
    void testTracing() {
        // Pre-condition: SCOPES map size is zero
        assertThat(JaegerScopeManager.SCOPES.size(), is(0));

        List<String> rs = IntStream.range(0, 32)
                .mapToObj(i -> target.path("/test").request().async().get(String.class))
                .map(f -> {
                    try {
                        return f.get();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());

        assertThat(rs.size(), is(32));
        assertThat(rs.get(0), is("Test Message"));
        assertThat(new HashSet<>(rs).size(), is(1));

        // Post-condition: SCOPES map size is back to zero
        assertThat(JaegerScopeManager.SCOPES.size(), is(0));
    }

    @Path("/test")
    public static class TestResource {

        @Inject
        private ResourceHelper helper;

        @GET
        public void getTest(@Suspended AsyncResponse r) {
            helper.help().thenAccept(r::resume);
        }
    }

    public static class ResourceHelper {

        @Inject
        private Service service;

        @Asynchronous
        public CompletionStage<String> help() {
            service.process();
            return CompletableFuture.completedFuture("Test Message");
        }
    }

    @Traced
    @ApplicationScoped
    public static class Service {

        private final io.opentracing.Tracer tracer = GlobalTracer.get();

        public void process() {
            Span span = tracer.buildSpan("some-span").asChildOf(tracer.activeSpan()).start();
            try (Scope scope = tracer.scopeManager().activate(span)) {
                span.setTag("tag1", "value1");
                span.setTag("tag2", "value2");
            } finally {
                span.finish();
            }
        }
    }
}
