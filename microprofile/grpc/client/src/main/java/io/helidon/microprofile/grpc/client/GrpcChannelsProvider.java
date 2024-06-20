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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.helidon.common.tls.Tls;
import io.helidon.config.Config;
import io.helidon.grpc.core.GrpcTlsDescriptor;
import io.helidon.webclient.grpc.GrpcClient;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;

/**
 * GrpcChannelsProvider is a factory for pre-configured gRPC Channel instances.
 */
public class GrpcChannelsProvider {

    /**
     * A constant for holding the default channel configuration name (which is "default").
     */
    public static final String DEFAULT_CHANNEL_NAME = "default";

    /**
     * A constant for holding the default host name (which is "localhost").
     */
    public static final String DEFAULT_HOST = "localhost";

    /**
     * The configuration key for the channels' configuration.
     */
    public static final String CFG_KEY_CHANNELS = "channels";

    /**
     * A constant for holding the default port (which is "1408").
     */
    public static final int DEFAULT_PORT = 1408;

    private final Map<String, GrpcChannelDescriptor> channelConfigs;

    private GrpcChannelsProvider(Map<String, GrpcChannelDescriptor> channelDescriptors) {
        this.channelConfigs = new HashMap<>(channelDescriptors);
    }

    /**
     * Builds a new instance of {@link GrpcChannelsProvider} using default configuration. The
     * default configuration connects to "localhost:1408" without TLS.
     *
     * @return a new instance of {@link GrpcChannelsProvider}
     */
    public static GrpcChannelsProvider create() {
        return GrpcChannelsProvider.builder().build();
    }

    /**
     * Creates a {@link GrpcChannelsProvider} using the specified configuration.
     *
     * @param config The externalized configuration.
     * @return a new instance of {@link GrpcChannelsProvider}
     */
    public static GrpcChannelsProvider create(Config config) {
        return new Builder(config).build();
    }

    /**
     * Create a new {@link Builder}.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return builder(null);
    }

    /**
     * Create a new {@link Builder}.
     *
     * @param config the {@link Config} to bootstrap from
     * @return a new {@link Builder}
     */
    public static Builder builder(Config config) {
        return new Builder(config);
    }

    // --------------- private methods of GrpcChannelsProvider ---------------

    /**
     * Returns a {@link ManagedChannel} for the specified channel or host name.
     * <p>
     * If the specified channel name does not exist in the configuration, we will assume
     * that it represents the name of the gRPC host to connect to and will create a plain text
     * channel to the host with the specified {@code name}, on a default port (1408).
     *
     * @param name the name of the channel configuration as specified in the configuration file,
     *             or the name of the host to connect to
     * @return a new instance of {@link ManagedChannel}
     * @throws NullPointerException if name is null
     * @throws IllegalArgumentException if name is empty
     */
    public ManagedChannel channel(String name) {
        if (name == null) {
            throw new NullPointerException("name cannot be null.");
        }
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("name cannot be empty or blank.");
        }
        GrpcChannelDescriptor chCfg = channelConfigs.computeIfAbsent(name, hostName ->
                GrpcChannelDescriptor.builder().host(name).build());
        return createChannel(chCfg);
    }

    Map<String, GrpcChannelDescriptor> channels() {
        return channelConfigs;
    }

    ManagedChannel createChannel(GrpcChannelDescriptor descriptor) {
        GrpcTlsDescriptor tlsDescriptor = descriptor.tlsDescriptor().orElse(null);
        if (tlsDescriptor == null) {
            throw new IllegalArgumentException("Missing TLS information in gRPC channel descriptor");
        }
        // TODO mutual TLS
        Tls clientTls = Tls.builder()
                .trust(trust -> trust
                        .keystore(store -> store
                                .passphrase("password")
                                .trustStore(true)
                                .keystore(tlsDescriptor.tlsCaCert())))
                .build();
        GrpcClient grpcClient = GrpcClient.builder()
                .tls(clientTls)
                .baseUri("https://" + descriptor.host() + ":" + descriptor.port())
                .build();
        return new GrpcManagedChannel(grpcClient.channel());
    }

    /**
     * A managed channel wrapper over a {@link GrpcClient} channel.
     */
    private static class GrpcManagedChannel extends ManagedChannel {

        private final Channel delegate;

        GrpcManagedChannel(Channel delegate) {
            this.delegate = delegate;
        }
        @Override
        public ManagedChannel shutdown() {
            return this;
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public ManagedChannel shutdownNow() {
            return this;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return false;
        }

        @Override
        public <ReqT, ResT> ClientCall<ReqT, ResT> newCall(MethodDescriptor<ReqT, ResT> methodDescriptor,
                                                           CallOptions callOptions) {
            return delegate.newCall(methodDescriptor, callOptions);
        }

        @Override
        public String authority() {
            return delegate.authority();
        }
    }

    /**
     * Builder builds an instance of {@link GrpcChannelsProvider}.
     */
    public static class Builder implements io.helidon.common.Builder<Builder, GrpcChannelsProvider> {

        private final Map<String, GrpcChannelDescriptor> channelConfigs = new HashMap<>();

        private Builder(Config config) {
            // Add the default channel (which can be overridden in the config)
            channel(DEFAULT_CHANNEL_NAME, GrpcChannelDescriptor.builder().build());

            if (config == null) {
                return;
            }

            Config channelsConfig = config.get(CFG_KEY_CHANNELS);
            if (channelsConfig.exists()) {
                for (Config channelConfig : channelsConfig.asNodeList().get()) {
                    String key = channelConfig.key().name();
                    GrpcChannelDescriptor cfg = channelConfig.asNode().get().as(GrpcChannelDescriptor.class).get();
                    channelConfigs.put(key, cfg);
                }
            }
        }

        /**
         * Add or replace the specified {@link GrpcChannelDescriptor}.
         *
         * @param name       the name of the configuration
         * @param descriptor the {@link GrpcChannelDescriptor} to be added
         * @return this Builder instance
         */
        public Builder channel(String name, GrpcChannelDescriptor descriptor) {
            channelConfigs.put(name, descriptor);
            return this;
        }

        /**
         * Create a new instance of {@link GrpcChannelsProvider} from this Builder.
         *
         * @return a new instance of {@link GrpcChannelsProvider}
         */
        public GrpcChannelsProvider build() {
            return new GrpcChannelsProvider(channelConfigs);
        }
    }
}
