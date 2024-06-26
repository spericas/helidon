/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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

/**
 * Support for YAML configuration sources.
 */
module io.helidon.config.yaml.mp {
    requires org.yaml.snakeyaml;
    requires microprofile.config.api;

    requires io.helidon.config.mp;
    requires transitive io.helidon.config;
    requires io.helidon.config.yaml;

    exports io.helidon.config.yaml.mp;

    provides io.helidon.config.mp.spi.MpConfigSourceProvider with io.helidon.config.yaml.mp.YamlConfigSourceProvider;
    provides io.helidon.config.mp.spi.MpMetaConfigProvider with io.helidon.config.yaml.mp.YamlMetaConfigProvider;
}
