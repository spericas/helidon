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
package io.helidon.common.metrics;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.Timer;

/**
 *
 */
public interface InternalMetricRegistryBridge {

    static Map<String, String> tags(String[] tags) {

        return Arrays.stream(tags)
                .filter(expr -> expr.indexOf("=") > 0)
                .map(tag -> {
                    final int eq = tag.indexOf("=");
                    return new AbstractMap.SimpleEntry<>(tag.substring(0, eq),
                                    tag.substring(eq + 1));
                })
                .collect(HashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        HashMap::putAll);
    }

    Counter counter(String name);
    Counter counter(CompatibleMetadata metadata, Map<String, String> tags);
    Counter counter(CompatibleMetadata metadata);

    Histogram histogram(CompatibleMetadata metadata);
    Histogram histogram(CompatibleMetadata metadata, Map<String, String> tags);
    Histogram histogram(String name);

    Meter meter(String name);
    Meter meter(CompatibleMetadata metadata);
    Meter meter(CompatibleMetadata metadata, Map<String, String> tags);

    Timer timer(String name);
    Timer timer(CompatibleMetadata metadata);
    Timer timer(CompatibleMetadata metadata, Map<String, String> tags);

    SortedMap<String, Counter> getCounters();
    SortedMap<String, Gauge> getGauges();
    SortedMap<String, Timer> getTimers();
    SortedMap<String, Meter> getMeters();

    Map<String, ? extends Metric> metricsViaNames();
    SortedMap<String, Gauge> gaugesViaNames();

    <T extends Metric> T register(CompatibleMetadata metadata, T metric) throws IllegalArgumentException;
    <T extends Metric> T register(String name, T metric);
    <T extends Metric> T register(CompatibleMetadata metadata, T metric, Map<String, String> tags);
    SortedSet<String> getNames();
    Map<String, Metric> getMetrics();

    boolean remove(String name);


    public interface MetadataFactory {

        CompatibleMetadata newMetadata(String name, String displayName, String description, MetricType type, String unit);
        CompatibleMetadata newMetadata(String name, MetricType type);
    }

    public interface CompatibleMetadata {

        <T> T as(Class<T> clazz); //

        /**
         * Returns the metric name.
         *
         * @return the metric name.
         */
        String getName();

        /**
         * Returns the display name if set, otherwise this method returns the metric name.
         *
         * @return the display name
         */
        String getDisplayName();

        /**
         * Returns the description of the metric.
         *
         * @return the description
         */
        Optional<String> getDescription();

        void setDescription(String description);
        /**
         * Returns the String representation of the {@link MetricType}.
         *
         * @return the MetricType as a String
         * @see MetricType
         */
        String getType();

        /**
         * Returns the {@link MetricType} of the metric
         *
         * @return the {@link MetricType}
         */
        MetricType getTypeRaw();

        Optional<String> getUnit();

        void setUnit(String unit);

        boolean isReusable();

//        /**
//         * Returns a new builder
//         *
//         * @return a new {@link MetadataBuilder} instance
//         */
//        static MetadataBuilder builder() {
//            return new MetadataBuilder();
//        }
//
//        /**
//         * Returns a new builder with the {@link CompatibleMetadata} information
//         *
//         * @param metadata the metadata
//         * @return a new {@link MetadataBuilder} instance with the {@link CompatibleMetadata} values
//         * @throws NullPointerException when metadata is null
//         */
//        static MetadataBuilder builder(CompatibleMetadata metadata) {
//            Objects.requireNonNull(metadata, "metadata is required");
//            return new MetadataBuilder(metadata);
//        }

    }
}
