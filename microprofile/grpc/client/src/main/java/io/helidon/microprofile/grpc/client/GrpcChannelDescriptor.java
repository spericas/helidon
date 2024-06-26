/*
 * Copyright (c) 2019, 2024 Oracle and/or its affiliates.
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

package io.helidon.microprofile.grpc.client;

import java.util.Optional;

import io.helidon.common.tls.Tls;
import io.helidon.config.Config;
import io.helidon.config.metadata.Configured;
import io.helidon.config.metadata.ConfiguredOption;

import io.grpc.NameResolver;

/**
 * GrpcChannelDescriptor contains the configuration for a {@link io.grpc.Channel}.
 */
public class GrpcChannelDescriptor {

    private final String host;
    private final int port;
    private final String target;
    private final Tls tls;
    private final String loadBalancerPolicy;
    private final NameResolver.Factory nameResolver;

    private GrpcChannelDescriptor(Builder builder) {
        this.target = builder.target();
        this.host = builder.host();
        this.port = builder.port();
        this.tls = builder.tls();
        this.loadBalancerPolicy = builder.loadBalancerPolicy();
        this.nameResolver = builder.nameResolverFactory();
    }

    /**
     * Create and return a new {@link Builder}.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the host name to connect.
     *
     * @return the host name to connect
     */
    public String host() {
        return host;
    }

    /**
     * Get the port that will be used to connect to the server.
     *
     * @return the port that will be used to connect to the server
     */
    public int port() {
        return port;
    }

    /**
     * Get the optional target string to use to resolve channel addresses.
     *
     * @return the optional target string to use to resolve channel addresses
     */
    public Optional<String> target() {
        return Optional.ofNullable(target);
    }

    /**
     * Get the default load balancer policy to use.
     *
     * @return the optional default load balancer policy to use
     */
    public Optional<String> loadBalancerPolicy() {
        return Optional.ofNullable(loadBalancerPolicy);
    }

    /**
     * Get the {@link NameResolver.Factory} to use.
     * <p>
     * This method is deprecated due to the deprecation of the
     * {@link io.grpc.ManagedChannelBuilder#nameResolverFactory(NameResolver.Factory)}
     * method in the gRPC Java API.
     *
     * @return the optional {@link NameResolver.Factory} to use
     */
    @Deprecated
    public Optional<NameResolver.Factory> nameResolverFactory() {
        return Optional.ofNullable(nameResolver);
    }

    /**
     * Get Tls instance.
     *
     * @return the optional {@link io.helidon.common.tls.Tls}
     */
    public Optional<Tls> tls() {
        return Optional.ofNullable(tls);
    }

    /**
     * Builder builds a GrpcChannelDescriptor.
     */
    @Configured
    public static class Builder implements io.helidon.common.Builder<Builder, GrpcChannelDescriptor> {
        private String host = GrpcChannelsProvider.DEFAULT_HOST;
        private int port = GrpcChannelsProvider.DEFAULT_PORT;
        private Tls tls;
        private String target;
        private String loadBalancerPolicy;
        private NameResolver.Factory nameResolver;

        /**
         * Create a descriptor from a config node.
         *
         * @param config the config
         * @return this instance for fluent API
         */
        public Builder config(Config config) {
            config.get("host").as(String.class).ifPresent(this::host);
            config.get("port").as(Integer.class).ifPresent(this::port);
            config.get("tls").map(Tls::create).ifPresent(this::tls);
            config.get("target").as(String.class).ifPresent(this::target);
            return this;
        }

        /**
         * Set the target string, which can be either a valid {@link NameResolver}
         * compliant URI, or an authority string.
         *
         * @param target the target string
         * @return this instance for fluent API
         *
         * @see io.grpc.ManagedChannelBuilder#forTarget(String)
         */
        @ConfiguredOption()
        public Builder target(String target) {
            this.target = target;
            return this;
        }

        /**
         * Set the host name to connect.
         *
         * @param host set the host name
         * @return this instance for fluent API
         */
        @ConfiguredOption(value = GrpcChannelsProvider.DEFAULT_HOST)
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        /**
         * Set the port that will be used to connect to the server.
         * @param port the port that will be used to connect to the server
         *
         * @return this instance for fluent API
         */
        @ConfiguredOption(value = "" + GrpcChannelsProvider.DEFAULT_PORT)
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets Tls.
         *
         * @param tls the TLS instance
         * @return this instance for fluent API
         */
        @ConfiguredOption(key = "tls")
        public Builder tls(Tls tls) {
            this.tls = tls;
            return this;
        }

        /**
         * Set the default load balancer policy name.
         *
         * @param policy the load balancer policy name
         * @return this instance for fluent API
         *
         * @see io.grpc.ManagedChannelBuilder#defaultLoadBalancingPolicy(String)
         */
        public Builder loadBalancerPolicy(String policy) {
            loadBalancerPolicy = policy;
            return this;
        }

        /**
         * Set the {@link NameResolver.Factory} to use.
         * @param factory the {@link NameResolver.Factory} to use
         * <p>
         * This method is deprecated due to the deprecation of the
         * {@link io.grpc.ManagedChannelBuilder#nameResolverFactory(NameResolver.Factory)}
         * method in the gRPC Java API.
         *
         * @return this instance for fluent API
         * @see io.grpc.ManagedChannelBuilder#nameResolverFactory(NameResolver.Factory)
         */
        @Deprecated
        public Builder nameResolverFactory(NameResolver.Factory factory) {
            this.nameResolver = factory;
            return this;
        }

        String host() {
            return host;
        }

        int port() {
            return port;
        }

        Tls tls() {
            return tls;
        }

        String target() {
            return target;
        }

        String loadBalancerPolicy() {
            return loadBalancerPolicy;
        }

        NameResolver.Factory nameResolverFactory() {
            return nameResolver;
        }

        /**
         * Build and return a new GrpcChannelDescriptor.
         * @return a new GrpcChannelDescriptor
         */
        public GrpcChannelDescriptor build() {
            return new GrpcChannelDescriptor(this);
        }
    }
}
