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
package io.helidon.microprofile.server;

import io.helidon.microprofile.config.ConfigCdiExtension;
import io.helidon.microprofile.tests.junit5.AddExtension;
import io.helidon.microprofile.tests.junit5.DisableDiscovery;
import io.helidon.microprofile.tests.junit5.HelidonTest;

import org.glassfish.jersey.ext.cdi1x.internal.CdiComponentProvider;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.glassfish.jersey.client.ClientProperties.IGNORE_EXCEPTION_RESPONSE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test that it is possible to override {@code IGNORE_EXCEPTION_RESPONSE} in
 * Jersey using config. See {@link io.helidon.microprofile.server.JaxRsHandler}
 * for more information.
 */
@HelidonTest
@DisableDiscovery
@AddExtension(ServerCdiExtension.class)
@AddExtension(JaxRsCdiExtension.class)
@AddExtension(CdiComponentProvider.class)
@AddExtension(ConfigCdiExtension.class)
@Disabled
class JerseyPropertiesTest {
    @Test
    void testIgnoreExceptionResponseOverride() {
        JaxRsHandler jerseySupport = JaxRsHandler.create(new ResourceConfig().property(IGNORE_EXCEPTION_RESPONSE, "false"),
                                                         null);
        assertNotNull(jerseySupport);
        assertThat(System.getProperty(IGNORE_EXCEPTION_RESPONSE), is("false"));
    }
}
