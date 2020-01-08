/*
 * Copyright 2017-2020 Volkan Yazıcı
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permits and
 * limitations under the License.
 */

package com.vlkan.log4j2.logstash.layout.resolver;

import org.apache.logging.log4j.core.LogEvent;

import java.util.*;

enum EventResolverFactories {;

    private static final Map<String, TemplateResolverFactory<LogEvent, EventResolverContext, ? extends TemplateResolver<LogEvent>>> RESOLVER_FACTORY_BY_NAME =
            createResolverFactoryByName();

    private static Map<String, TemplateResolverFactory<LogEvent, EventResolverContext, ? extends TemplateResolver<LogEvent>>> createResolverFactoryByName() {

        // Collect resolver factories.
        List<EventResolverFactory<? extends EventResolver>> resolverFactories = Arrays.asList(
                ContextDataResolverFactory.getInstance(),
                ContextStackResolverFactory.getInstance(),
                EndOfBatchResolverFactory.getInstance(),
                ExceptionResolverFactory.getInstance(),
                ExceptionRootCauseResolverFactory.getInstance(),
                LevelResolverFactory.getInstance(),
                LoggerResolverFactory.getInstance(),
                MainMapResolverFactory.getInstance(),
                MapResolverFactory.getInstance(),
                MarkerResolverFactory.getInstance(),
                MessageResolverFactory.getInstance(),
                SourceResolverFactory.getInstance(),
                ThreadResolverFactory.getInstance(),
                TimestampResolverFactory.getInstance());

        // Convert collection to map.
        Map<String, TemplateResolverFactory<LogEvent, EventResolverContext, ? extends TemplateResolver<LogEvent>>> resolverFactoryByName = new LinkedHashMap<>();
        for (EventResolverFactory<? extends EventResolver> resolverFactory : resolverFactories) {
            resolverFactoryByName.put(resolverFactory.getName(), resolverFactory);
        }
        return Collections.unmodifiableMap(resolverFactoryByName);

    }

    static Map<String, TemplateResolverFactory<LogEvent, EventResolverContext, ? extends TemplateResolver<LogEvent>>> getResolverFactoryByName() {
        return RESOLVER_FACTORY_BY_NAME;
    }

}
