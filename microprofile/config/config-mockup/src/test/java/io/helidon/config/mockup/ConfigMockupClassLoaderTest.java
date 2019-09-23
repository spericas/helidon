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

package io.helidon.config.mockup;

import org.junit.jupiter.api.Test;

/**
 * Class ConfigMockupClassLoaderTest.
 */
public class ConfigMockupClassLoaderTest {

    @Test
    public void testClassLoader() throws Exception {
        ConfigMockupClassLoader loader = new ConfigMockupClassLoader();
        Class<?> configProviderClass = loader.loadClass("org.eclipse.microprofile.config.ConfigProvider");
        System.out.println("### " + configProviderClass);
        Object config = configProviderClass.getMethod("getConfig").invoke(null);
        System.out.println("### " + config);
        Object optional = config.getClass().getMethod("getOptionalValue", String.class, Class.class)
                .invoke(config, "", String.class);
        System.out.println("### " + optional);
    }
}
