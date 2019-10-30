/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.microprofile.cors;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Optional;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import static io.helidon.microprofile.cors.CrossOrigin.ACCESS_CONTROL_ALLOW_ORIGIN;

/**
 * Class CrossOriginFilter.
 */
public class CrossOriginFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        lookupAnnotation(resourceInfo.getResourceClass(), resourceInfo.getResourceMethod())
                .ifPresent(crossOrigin -> {
                    MultivaluedMap<String, Object> headers = responseContext.getHeaders();
                    if (!headers.containsKey(ACCESS_CONTROL_ALLOW_ORIGIN)) {
                        headers.add(ACCESS_CONTROL_ALLOW_ORIGIN, formatArray(crossOrigin.value()));
                    }
                });
    }

    /**
     * Looks up a {@code CrossOrigin} annotation in method first and then class.
     *
     * @param beanClass The class.
     * @param method The method.
     * @return Outcome of lookup.
     */
    static Optional<CrossOrigin> lookupAnnotation(Class<?> beanClass, Method method) {
        CrossOrigin annotation = method.getAnnotation(CrossOrigin.class);
        if (annotation == null) {
            annotation = beanClass.getAnnotation(CrossOrigin.class);
            if (annotation == null) {
                annotation = method.getDeclaringClass().getAnnotation(CrossOrigin.class);
            }
        }
        return Optional.ofNullable(annotation);
    }

    /**
     * Formats an array as a comma-separate list without brackets.
     *
     * @param array The array.
     * @param <T> Type of elements in array.
     * @return Formatted array.
     */
    static <T> String formatArray(T[] array) {
        if (array.length == 0) {
            return "";
        }
        int i = 0;
        StringBuilder builder = new StringBuilder();
        do {
            builder.append(array[i++].toString());
            if (i == array.length) {
                break;
            }
            builder.append(", ");
        } while (true);
        return builder.toString();
    }
}
