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
package io.helidon.webserver;

import io.helidon.common.http.BodyPartHeaders;
import io.helidon.common.http.Content;

/**
 * An entity representing a nested body part.
 */
public interface BodyPart {

    /**
     * Get the body part headers.
     * @return the body part headers
     * @throws IllegalStateException if this body part is the top level
     * {@link StreamingMultiPart} entity
     */
    BodyPartHeaders headers();

    /**
     * Returns a reactive representation of the body part content.
     * @return body part content
     * @throws IllegalStateException if this body part is the top level
     * {@link StreamingMultiPart} entity
     */
    Content content();

    /**
     * Get the parent part.
     * @return the parent body part if this part is nested,
     * or {@code null} if this is the top level {@link StreamingMultiPart} entity
     */
    BodyPart parent();
}
