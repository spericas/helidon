/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates.
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

package io.helidon.microprofile.faulttolerance;

import java.util.logging.Logger;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * Intercepts calls to a method and applies fault tolerance semantics based
 * on annotations present.
 *
 * @deprecated this class should not have been public
 */
@Interceptor
@CommandBinding
@Priority(Interceptor.Priority.PLATFORM_AFTER + 10)
@Deprecated(since = "2.1.0")
public class CommandInterceptor {

    private static final Logger LOGGER = Logger.getLogger(CommandInterceptor.class.getName());

    /**
     * Intercepts a call to bean method annotated by any of the fault tolerance
     * annotations.
     *
     * @param context Invocation context.
     * @return Whatever the intercepted method returns.
     * @throws Throwable If a problem occurs.
     */
    @AroundInvoke
    public Object interceptCommand(InvocationContext context) throws Throwable {
        try {
            LOGGER.fine("Interceptor called for '" + context.getTarget().getClass()
                        + "::" + context.getMethod().getName() + "'");

            // Create method introspector and executer retrier
            final MethodIntrospector introspector = new MethodIntrospector(
                    context.getTarget().getClass(), context.getMethod());
            final CommandRetrier retrier = new CommandRetrier(context, introspector);
            return retrier.execute();
        } catch (Throwable t) {
            LOGGER.fine("Throwable caught by interceptor '" + t.getMessage() + "'");
            throw t;
        }
    }
}
