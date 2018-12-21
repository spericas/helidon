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

import java.nio.charset.Charset;

import io.helidon.common.http.BodyPartHeaders;
import io.helidon.common.http.MediaType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Class BodyPartTest.
 */
public class BodyPartTest {

    @Test
    public void testBuilder1() {
        BodyPartHeaders headers = BodyPartHeaders.builder()
                .name("name")
                .contentType(MediaType.TEXT_PLAIN)
                .build();
        BodyPart.BodyPartBuilder builder = BodyPart.builder();
        BodyPart bodypart = builder.headers(headers)
                .entity("entity")
                .build();

        assertEquals(headers, bodypart.headers());
        assertEquals("entity", bodypart.entity());
        assertEquals("name", bodypart.headers().name());
        assertEquals(MediaType.TEXT_PLAIN.toString(), bodypart.headers().contentType());
    }

    @Test
    public void testBuilder2() {
        BodyPartHeaders headers = BodyPartHeaders.builder()
                .name("name")
                .filename("filename")
                .size(1L)
                .charset(Charset.defaultCharset())
                .contentTransferEncoding("encoding")
                .contentType(MediaType.TEXT_PLAIN)
                .build();
        BodyPart.BodyPartBuilder builder = BodyPart.builder();
        BodyPart bodypart = builder.headers(headers)
                .entity("entity")
                .build();

        assertEquals(headers, bodypart.headers());
        assertEquals("entity", bodypart.entity());
        assertEquals("name", bodypart.headers().name());
        assertEquals("filename", bodypart.headers().filename());
        assertEquals(1L, bodypart.headers().size());
        assertEquals(Charset.defaultCharset(), bodypart.headers().charset());
        assertEquals("encoding", bodypart.headers().contentTransferEncoding());
        assertEquals(MediaType.TEXT_PLAIN.toString(), bodypart.headers().contentType());
    }
}
