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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.helidon.common.http.BodyPartHeaders;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.common.http.Parameters;
import io.helidon.common.reactive.Flow;
import io.helidon.webserver.ContentWriters;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Response;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

/**
 * MultiPartSupport support {@link Service} and {@link Handler}. Adds support
 * for media types <tt>multipart/form-data</tt> and <tt>multipart/mixed</tt>.
 */
public class MultiPartSupport implements Service, Handler {

    private static final String CRLF = "\r\n";

    @Override
    public void update(final Routing.Rules rules) {
        rules.any(this);
    }

    @Override
    public void accept(final ServerRequest req, final ServerResponse res) {
        // Register readers
        req.headers().contentType().ifPresent(contentType -> {
            if (contentType.type().equals(MediaType.MULTIPART_FORM_DATA.type())
                    && (contentType.subtype().equals(MediaType.MULTIPART_FORM_DATA.subtype())
                    || contentType.subtype().equals(MediaType.MULTIPART_MIXED.subtype()))) {
                req.content().registerReader(MultiPart.class, (publisher, clazz) -> {
                    CompletableFuture<MultiPart> future = new CompletableFuture<>();
                    future.complete(new MultiPart(req, res, publisher));
                    return future;
                });
            }
        });

        // Register writers
        res.registerWriter(multipart -> (multipart instanceof MultiPart)
                        && testOrSetContentType(req, res),
                multipart -> {
                    Charset charset = determineCharset(res.headers());
                    return writer(res, charset).apply((MultiPart) multipart);
                });

        req.next();
    }

    public static Flow.Publisher<DataChunk> multipart(Flow.Publisher<BodyPart> publisher) {
        Flow.Processor<BodyPart, DataChunk> processor = new Flow.Processor<BodyPart, DataChunk>() {

            private Flow.Subscription subscription;
            private DataChunk dataChunk;
            private boolean completed = false;

            // -- Publisher ---------------------------------------------------

            @Override
            public void subscribe(Flow.Subscriber<? super DataChunk> subscriber) {
                subscriber.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void request(long n) {
                        if (completed) {
                            subscriber.onComplete();
                        }
                        while (n-- > 0) {
                            subscription.request(1);
                            subscriber.onNext(dataChunk);
                        }
                    }

                    @Override
                    public void cancel() {
                        subscription.cancel();
                    }
                });
            }

            // -- Subscriber ---------------------------------------------------

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
            }

            @Override
            public void onNext(BodyPart bodyPart) {
                // convert bodyPart to dataChunk
            }

            @Override
            public void onError(Throwable throwable) {
            }

            @Override
            public void onComplete() {
                completed = true;
            }
        };

        publisher.subscribe(processor);
        return processor;
    }

    /**
     * Returns a function (writer) converting {@link MultiPart} to the {@link Flow.Publisher Publisher}
     * of {@link DataChunk}s. Default charset to use is ISO-8859-1 as in HTTP 1.1.
     *
     * @param charset a charset to use or {@code null} for default charset
     * @return created function
     */
    @SuppressWarnings("unchecked")
    public Function<MultiPart, Flow.Publisher<DataChunk>> writer(ServerResponse res, Charset charset) {
        return multipart -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Charset selected = charset == null ? Charset.forName("ISO-8859-1") : charset;

            try (Writer writer = new OutputStreamWriter(baos, selected)) {
                MediaType mediaType = res.headers().contentType().get();
                String boundary = mediaType.parameters().get("boundary");

                // No preamble, so start with two new lines
                writer.write(CRLF);
                writer.write(CRLF);

                // Serialize body parts
                int length = multipart.bodyParts().size();
                for (int i = 0; i < length; i++) {
                    BodyPart bodypart = multipart.bodyParts().get(i);

                    // Write boundary for this part
                    writer.write("--");
                    writer.write(boundary);
                    writer.write(CRLF);

                    // Write headers for this part
                    BodyPartHeaders headers = bodypart.headers();
                    if (headers != null) {
                        // Content type for this part
                        if (headers.hasContentType()) {
                            writer.write("Content-Type: ");
                            writer.write(headers.contentType());
                            if (headers.charset() != null) {
                                writer.write(";charset=\"");
                                writer.write(headers.charset().name());
                                writer.write("\"");
                            }
                            writer.write(CRLF);
                        }
                        // Content disposition if any params set
                        if (headers.hasParameters()) {
                            writer.write("Content-Disposition: ");
                            if (mediaType.subtype().equals("form-data")) {
                                writer.write("form-data");
                            } else {
                                writer.write("attachment");
                            }
                            if (headers.name() != null) {
                                writer.write(";name=\"");
                                writer.write(headers.name());
                                writer.write("\"");
                            }
                            if (headers.filename() != null) {
                                writer.write(";filename=\"");
                                writer.write(headers.filename());
                                writer.write("\"");
                            }
                            if (headers.size() > 0) {
                                writer.write(";size=\"");
                                writer.write(Long.toString(headers.size()));
                                writer.write("\"");
                            }
                            writer.write(CRLF);
                        }
                        // Transfer encoding for this part
                        if (headers.contentTransferEncoding() != null) {
                            writer.write("Content-Transfer-Encoding: ");
                            writer.write(headers.contentTransferEncoding());
                            writer.write(CRLF);
                        }
                        writer.write(CRLF);
                    } else {
                        // If no headers, there needs to be an extra line
                        writer.write(CRLF);
                    }

                    // Serialize a part using a suitable writer
                    Object entity = bodypart.entity();
                    List<io.helidon.webserver.Response.Writer> writers = ((Response) res).writers();
                    for (int j = writers.size() - 1; j >= 0; j--) {
                        Response.Writer<Object> entityWriter = writers.get(j);
                        if (entityWriter.accept(entity)) {
                            Flow.Publisher<DataChunk> pub = entityWriter.function().apply(entity);
                            if (pub != null) {
                                ByteArraySubscriber sub = new ByteArraySubscriber();
                                pub.subscribe(sub);
                                writer.flush();
                                baos.write(sub.buffer());       // write to underlying stream
                                writer.write(CRLF);
                                writer.write(CRLF);
                            } else {
                                throw new RuntimeException("Unable to write entity " + entity);
                            }
                        }
                    }

                    // Write final boundary if at end
                    if (i == length - 1) {
                        writer.write("--");
                        writer.write(boundary);
                        writer.write("--");
                        writer.write(CRLF);
                    }
                }
                writer.flush();

                return ContentWriters.byteArrayWriter(false).apply(baos.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Returns a charset from {@code Content-Type} header parameter or {@code null} if not defined.
     *
     * @param headers parameters representing request or response headers
     * @return a charset or {@code null}
     * @throws RuntimeException if charset is not supported
     */
    private Charset determineCharset(Parameters headers) {
        return headers.first(Http.Header.CONTENT_TYPE)
                .map(MediaType::parse)
                .flatMap(MediaType::charset)
                .map(sch -> {
                    try {
                        return Charset.forName(sch);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .orElse(null);
    }

    /**
     * Deals with req {@code Accept} and response {@code Content-Type} headers to determine
     * if writer can be used.
     * <p>
     * If response has no {@code Content-Type} header then it is set to the response.
     *
     * @param req a server request
     * @param res a server response
     * @return {@code true} if Multipart writer can be used
     */
    private boolean testOrSetContentType(ServerRequest req, ServerResponse res) {
        MediaType mt = res.headers().contentType().orElse(null);
        String boundary = "HelidonBoundary" + Long.toHexString(System.currentTimeMillis());
        if (mt == null) {
            List<MediaType> acceptedTypes = req.headers().acceptedTypes();
            MediaType preferredType;
            if (acceptedTypes.isEmpty()) {
                preferredType = MediaType.MULTIPART_MIXED;
            } else {
                preferredType = acceptedTypes
                        .stream()
                        .map(type -> {
                            if (type.test(MediaType.MULTIPART_MIXED)) {
                                return MediaType.MULTIPART_MIXED;
                            } else if (type.test(MediaType.MULTIPART_FORM_DATA)) {
                                return MediaType.MULTIPART_FORM_DATA;
                            } else {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
            }
            if (preferredType == null) {
                return false;
            } else {
                res.headers().contentType(createWithBoundary(preferredType, boundary));
                return true;
            }
        } else {
            boolean isCompatible = MediaType.MULTIPART_MIXED.test(mt) || MediaType.MULTIPART_FORM_DATA.test(mt);
            if (isCompatible) {
                res.headers().contentType(createWithBoundary(mt, boundary));
            }
            return isCompatible;
        }
    }

    /**
     * Creates a new media type with a multipart boundary.
     *
     * @param mediaType The media type.
     * @param boundary The boundary string to use.
     * @return New media type with boundary parameter.
     */
    private MediaType createWithBoundary(MediaType mediaType, String boundary) {
        return MediaType.builder()
                .type(mediaType.type())
                .subtype(mediaType.subtype())
                .charset(mediaType.charset().orElse(null))
                .parameters(mediaType.parameters())
                .addParameter("boundary", boundary)
                .build();
    }

    private class ByteArraySubscriber implements Flow.Subscriber<DataChunk> {

        private ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buffer() {
            return baos.toByteArray();
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(DataChunk chunk) {
            try {
                baos.write(chunk.bytes());
                chunk.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            throw new RuntimeException(throwable);
        }

        @Override
        public void onComplete() {
            try {
                baos.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
