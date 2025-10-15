/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

package io.helidon.webclient.grpc.tests;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

import io.helidon.common.tls.TlsConfig;
import io.helidon.webclient.api.WebClient;
import io.helidon.webclient.grpc.GrpcClient;
import io.helidon.webclient.http2.Http2ClientProtocolConfig;
import io.helidon.webserver.Router;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.grpc.GrpcRouting;
import io.helidon.webserver.grpc.GrpcService;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import com.google.protobuf.Descriptors;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class ArtifactsServiceTest {

    /**
     * Total number of keys.
     */
    private static final int NUMBER_OF_KEYS = 50;

    /**
     * Number of entries in a batch.
     */
    private static final int BATCH_SIZE = 100;

    /**
     * Size of a value in bytes.
     */
    private static final int VALUE_SIZE = 4 * 1024;

    /**
     * Upper bound on the size of a frame.
     */
    private static final int MAX_FRAME_SIZE = 2 * BATCH_SIZE * VALUE_SIZE;

    /**
     * Number of bytes to receive before pausing for a WINDOW_UPDATE.
     */
    private static final int WINDOW_SIZE = 20 * MAX_FRAME_SIZE;

    private final WebClient webClient;

    ArtifactsServiceTest(WebServer server) {
        this.webClient = WebClient.builder()
                .baseUri("http://localhost:" + server.port())
                .addProtocolConfig(
                        Http2ClientProtocolConfig.builder()
                                .initialWindowSize(WINDOW_SIZE)
                                .maxFrameSize(MAX_FRAME_SIZE)
                                .build())
                .tls(TlsConfig.builder().enabled(false).build())
                .build();
    }

    @SetUpRoute
    static void routing(Router.RouterBuilder<?> router) {
        router.addRouting(GrpcRouting.builder().service(new ArtifactsService()));
    }

    @Test
    void testArtifactStream() {
        GrpcClient grpcClient = webClient.client(GrpcClient.PROTOCOL);
        ArtifactsServiceGrpc.ArtifactsServiceBlockingStub stub =
                ArtifactsServiceGrpc.newBlockingStub(grpcClient.channel());

        Artifacts.ArtifactRequest.Builder builder = Artifacts.ArtifactRequest.newBuilder();
        List<String> keys = IntStream.range(0, NUMBER_OF_KEYS)
                .mapToObj(String::valueOf)
                .toList();
        builder.addAllKeys(keys);
        Artifacts.ArtifactRequest request = builder.build();

        Iterator<Artifacts.ArtifactBatch> iterator = stub.artifactStream(request);
        int total = 0;
        while (iterator.hasNext()) {
            Artifacts.ArtifactBatch batch = iterator.next();
            assertThat(batch.getItemsCount(), is(BATCH_SIZE));
            total += batch.getItemsCount();
        }
        assertThat(total, is(NUMBER_OF_KEYS * BATCH_SIZE));
    }

    static class ArtifactsService implements GrpcService {

        private static final String DATA;

        static {
            byte[] bytes = new byte[VALUE_SIZE];
            Arrays.fill(bytes, (byte) 'A');
            DATA = new String(bytes);
        }

        @Override
        public Descriptors.FileDescriptor proto() {
            return Artifacts.getDescriptor();
        }

        @Override
        public void update(Routing router) {
            router.serverStream("ArtifactStream", this::artifactStream);
        }

        private void artifactStream(Artifacts.ArtifactRequest request,
                                    StreamObserver<Artifacts.ArtifactBatch> batch) {
            request.getKeysList().forEach(k -> {
                Artifacts.ArtifactBatch.Builder builder = Artifacts.ArtifactBatch.newBuilder();
                for (int i = 0; i < BATCH_SIZE; i++) {
                    builder.addItems(Artifacts.KeyValue.newBuilder()
                                             .setKey(k)
                                             .setValue(DATA)
                                             .build());
                }
                batch.onNext(builder.build());
            });
            batch.onCompleted();
        }
    }
}
