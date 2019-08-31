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
 *
 */
package io.helidon.metrics;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.helidon.common.metrics.InternalBridge;
import io.helidon.config.Config;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.metrics.MetricType;

/**
 * Implements the metrics bridge interface.
 */
public class InternalBridgeImpl implements InternalBridge {

    @Override
    public RegistryFactory getRegistryFactory() {
        return io.helidon.metrics.RegistryFactory.getInstance();
    }

    @Override
    public RegistryFactory createRegistryFactory() {
        return io.helidon.metrics.RegistryFactory.create();
    }

    @Override
    public RegistryFactory createRegistryFactory(Config config) {
        return io.helidon.metrics.RegistryFactory.create(config);
    }

    @Override
    public MetricID.Factory getMetricIDFactory() {
        return new InternalMetricIDImpl.FactoryImpl();
    }

    @Override
    public Metadata.MetadataBuilder.Factory getMetadataBuilderFactory() {
        return new InternalMetadataBuilderImpl.FactoryImpl();
    }

}
