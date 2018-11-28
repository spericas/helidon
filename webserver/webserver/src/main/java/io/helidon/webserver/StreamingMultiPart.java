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

import io.helidon.common.reactive.Flow;

/**
 * An entity that represents a top level multipart payload (i.e all the body parts in the request),
 * or a nested part payload in the case of a multipart-mixed request. Only 1 level of
 * nested parts is supported.
 *
 * The number of body parts in the request is not known ahead of time.
 */
public interface StreamingMultiPart extends Flow.Publisher<BodyPart> {
}
