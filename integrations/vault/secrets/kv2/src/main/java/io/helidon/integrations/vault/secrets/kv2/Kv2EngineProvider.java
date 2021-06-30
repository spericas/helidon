/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.integrations.vault.secrets.kv2;

import java.util.LinkedList;
import java.util.List;

import io.helidon.config.Config;
import io.helidon.integrations.common.rest.RestApi;
import io.helidon.integrations.vault.Engine;
import io.helidon.integrations.vault.spi.InjectionProvider;
import io.helidon.integrations.vault.spi.SecretsEngineProvider;

/**
 * Java Service Loader implementation of Vault KV version 2 secrets engine.
 */
public class Kv2EngineProvider implements SecretsEngineProvider<Kv2SecretsRx>,
                                          InjectionProvider {
    private static final List<InjectionType<?>> INJECTABLES;

    static {
        List<InjectionType<?>> injectables = new LinkedList<>();

        injectables.add(InjectionType.create(Kv2SecretsRx.class,
                                             (vault, config, instanceConfig) -> instanceConfig.vaultPath()
                                                     .map(it -> vault.secrets(Kv2SecretsRx.ENGINE, it))
                                                     .orElseGet(() -> vault.secrets(Kv2SecretsRx.ENGINE))));

        injectables.add(InjectionType.create(Kv2Secrets.class,
                                             (vault, config, instanceConfig) -> {
                                                 Kv2SecretsRx rx = instanceConfig.vaultPath()
                                                         .map(it -> vault.secrets(Kv2SecretsRx.ENGINE, it))
                                                         .orElseGet(() -> vault.secrets(Kv2SecretsRx.ENGINE));

                                                 return new Kv2SecretsImpl(rx);
                                             }));

        INJECTABLES = List.copyOf(injectables);
    }

    @Override
    public Engine<Kv2SecretsRx> supportedEngine() {
        return Kv2SecretsRx.ENGINE;
    }

    @Override
    public Kv2SecretsRx createSecrets(Config config, RestApi restAccess, String mount) {
        return new Kv2SecretsRxImpl(restAccess, mount);
    }

    @Override
    public List<InjectionType<?>> injectables() {
        return INJECTABLES;
    }
}
