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

package io.helidon.common.http;

import java.nio.charset.Charset;

import io.helidon.common.Builder;

/**
 * Class BodyPartHeaders.
 */
public class BodyPartHeaders {

    private final String name;
    private final String filename;
    private final String contentType;
    private final String contentTransferEncoding;
    private final Charset charset;
    private final long size;

    BodyPartHeaders(String name, String filename, String contentType,
                    String contentTransferEncoding, Charset charset, long size) {
        this.name = name;
        this.filename = filename;
        this.contentType = contentType;
        this.contentTransferEncoding = contentTransferEncoding;
        this.charset = charset;
        this.size = size;
    }

    public boolean hasContentType() {
        return contentType != null;
    }

    public String name() {
        return name;
    }

    public String filename() {
        return filename;
    }

    public String contentType() {
        return contentType;
    }

    public String contentTransferEncoding() {
        return contentTransferEncoding;
    }

    public Charset charset() {
        return charset;
    }

    public long size() {
        return size;
    }

    public boolean hasParameters() {
        return name != null || filename != null || size > 0L;
    }

    public static BodyPartHeaderBuilder builder() {
        return new BodyPartHeaderBuilder();
    }

    public static class BodyPartHeaderBuilder implements Builder<BodyPartHeaders> {

        private String name;
        private String filename;
        private String contentType;
        private String contentTransferEncoding;
        private Charset charset;
        private long size;

        @Override
        public BodyPartHeaders build() {
            return new BodyPartHeaders(name, filename, contentType, contentTransferEncoding, charset, size);
        }

        public BodyPartHeaderBuilder name(String name) {
            this.name = name;
            return this;
        }

        public BodyPartHeaderBuilder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public BodyPartHeaderBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public BodyPartHeaderBuilder contentType(MediaType contentType) {
            this.contentType = contentType.toString();
            return this;
        }

        public BodyPartHeaderBuilder contentTransferEncoding(String contentTransferEncoding) {
            this.contentTransferEncoding = contentTransferEncoding;
            return this;
        }

        public BodyPartHeaderBuilder charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public BodyPartHeaderBuilder size(long size) {
            this.size = size;
            return this;
        }
    }
}
