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

import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import io.helidon.common.http.AlreadyCompletedException;
import io.helidon.common.http.BodyPartHeaders;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.Parameters;
import io.helidon.common.http.SetCookie;
import io.helidon.common.reactive.Flow;
import io.helidon.webserver.ContentWriters;
import io.helidon.webserver.Response;
import io.helidon.webserver.ResponseHeaders;
import io.opentracing.SpanContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Class MultiPartSupportTest.
 */
public class MultiPartSupportTest {

    private static final String BOUNDARY = "HelidonBoundaryXYZ";

    /**
     * Checks basic serialization of a multipart. Uses mocks for {@link Response}
     * and {@link ResponseHeaders}.
     */
    @Test
    public void testWriter1() {
        Response res = new ResponseMock();
        MultiPartSupport multiPartSupport = new MultiPartSupport();
        Function<MultiPart, Flow.Publisher<DataChunk>> f = multiPartSupport.writer(res, null);
        Flow.Publisher<DataChunk> publisher = f.apply(newMultiPart());
        publisher.subscribe(new Flow.Subscriber<DataChunk>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(DataChunk item) {
                String serialized = new String(item.bytes());
                System.out.println(serialized);
                assertTrue(serialized.contains("Content-Type: text/plain"));
                assertTrue(serialized.contains("Content-Disposition: attachment;name=\"part1\""));
                assertTrue(serialized.contains("This is part 1"));
                assertTrue(serialized.contains("Content-Disposition: attachment;name=\"part2\""));
                assertTrue(serialized.contains("This is part 2"));
                assertTrue(serialized.contains("--HelidonBoundaryXYZ"));
                assertTrue(serialized.contains("--HelidonBoundaryXYZ--"));
            }

            @Override
            public void onError(Throwable throwable) {
                fail(throwable);
            }

            @Override
            public void onComplete() {
            }
        });
    }

    private MultiPart newMultiPart() {
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
                                .contentType(MediaType.TEXT_PLAIN)
                                .build())
                        .entity("This is part 2")
                        .build())
                .build();
        return multiPart;
    }

    // -- Mocks ---------------------------------------------------------------

    static class ResponseMock extends Response {

        @Override
        public List<Writer> writers() {
            return Collections.singletonList(new Writer<>(CharSequence.class,
                    null, (CharSequence s) ->
                    ContentWriters.charSequenceWriter(Charset.defaultCharset()).apply(s)));
        }

        @Override
        public ResponseHeaders headers() {
            return new ResponseHeadersMock();
        }

        @Override
        protected SpanContext spanContext() {
            return null;
        }
    }

    static class ResponseHeadersMock implements ResponseHeaders {
        @Override
        public List<MediaType> acceptPatches() {
            return null;
        }

        @Override
        public void addAcceptPatches(MediaType... acceptableMediaTypes) throws AlreadyCompletedException {

        }

        @Override
        public Optional<MediaType> contentType() {
            MediaType mediaType = MediaType.builder().type("multipart")
                    .subtype("mixed")
                    .addParameter("boundary", BOUNDARY)
                    .build();
            return Optional.of(mediaType);
        }

        @Override
        public void contentType(MediaType contentType) throws AlreadyCompletedException {

        }

        @Override
        public OptionalLong contentLength() {
            return null;
        }

        @Override
        public void contentLength(long contentLength) throws AlreadyCompletedException {

        }

        @Override
        public Optional<ZonedDateTime> expires() {
            return Optional.empty();
        }

        @Override
        public void expires(ZonedDateTime dateTime) throws AlreadyCompletedException {

        }

        @Override
        public void expires(Instant dateTime) throws AlreadyCompletedException {

        }

        @Override
        public Optional<ZonedDateTime> lastModified() {
            return Optional.empty();
        }

        @Override
        public void lastModified(ZonedDateTime dateTime) throws AlreadyCompletedException {

        }

        @Override
        public void lastModified(Instant dateTime) throws AlreadyCompletedException {

        }

        @Override
        public Optional<URI> location() {
            return Optional.empty();
        }

        @Override
        public void location(URI location) throws AlreadyCompletedException {

        }

        @Override
        public void addCookie(String name, String value) throws AlreadyCompletedException, NullPointerException {

        }

        @Override
        public void addCookie(String name, String value, Duration maxAge) throws AlreadyCompletedException, NullPointerException {

        }

        @Override
        public void addCookie(SetCookie cookie) throws NullPointerException {

        }

        @Override
        public void beforeSend(Consumer<ResponseHeaders> headersConsumer) {

        }

        @Override
        public CompletionStage<ResponseHeaders> whenSend() {
            return null;
        }

        @Override
        public CompletionStage<ResponseHeaders> send() {
            return null;
        }

        @Override
        public List<String> all(String headerName) {
            return null;
        }

        @Override
        public Optional<String> first(String name) {
            return Optional.empty();
        }

        @Override
        public List<String> put(String key, String... values) {
            return null;
        }

        @Override
        public List<String> put(String key, Iterable<String> values) {
            return null;
        }

        @Override
        public List<String> putIfAbsent(String key, String... values) {
            return null;
        }

        @Override
        public List<String> putIfAbsent(String key, Iterable<String> values) {
            return null;
        }

        @Override
        public List<String> computeIfAbsent(String key, Function<String, Iterable<String>> values) {
            return null;
        }

        @Override
        public List<String> computeSingleIfAbsent(String key, Function<String, String> value) {
            return null;
        }

        @Override
        public void putAll(Parameters parameters) {

        }

        @Override
        public void add(String key, String... values) {

        }

        @Override
        public void add(String key, Iterable<String> values) {

        }

        @Override
        public void addAll(Parameters parameters) {

        }

        @Override
        public List<String> remove(String key) {
            return null;
        }

        @Override
        public Map<String, List<String>> toMap() {
            return null;
        }
    }
}
