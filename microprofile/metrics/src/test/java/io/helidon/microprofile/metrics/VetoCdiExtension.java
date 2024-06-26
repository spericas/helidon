/*
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.
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
package io.helidon.microprofile.metrics;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;
import jakarta.ws.rs.Path;

/**
 * Vetoes selected resources which should suppress the registration of their annotation-defined metrics and
 * implicitly-created synthetic simply timed metrics.
 */
public class VetoCdiExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(VetoCdiExtension.class.getName());

    private static final Set<Class<?>> VETOED_RESOURCE_CLASSES = Set.of(VetoedResource.class,
            VetoedJaxRsButOtherwiseUnmeasuredResource.class);

    private void vetoResourceClass(@Observes @WithAnnotations(Path.class) ProcessAnnotatedType<?> resourceType) {
        Class<?> resourceClass = resourceType.getAnnotatedType().getJavaClass();
        if (VETOED_RESOURCE_CLASSES.contains(resourceClass)) {
            LOGGER.log(Level.FINE, () -> "Unit test is vetoing " + resourceClass.getName());
            resourceType.veto();
        }
    }
}
