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

/**
 *
 * @author rgrecour
 */
public interface BodyPartHeaders {

    /**
     * Get the body part name specified in the {@code Content-Disposition}
     * body part header.
     * @return the body part name
     */
    String name();

    /**
     * Get the body part filename specified in the {@code Content-Disposition}
     * body part header.
     * @return the body part filename
     */
    String filename();

    /**
     * Get the body part content type specified in the {@code Content-Type} body
     * part header.
     * @return the body part content type
     */
    String contentType();

    /**
     * Get the value of the {@code Content-Transfer-Encoding} body part header.
     * @return the body part content transfert encoding
     */
    String contentTransferEncoding();

    /**
     * Get the value of the {@code charset} body part header.
     * @return the body part charset
     */
    Charset charset();

    /**
     * Get the value of the {@code Content-Length} body part header.
     * @return the body part content length
     */
    long size();

}
