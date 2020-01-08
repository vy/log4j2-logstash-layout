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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

enum StackTraceElementObjectResolverFactories {;

    private static final Map<String, TemplateResolverFactory<StackTraceElement, StackTraceElementObjectResolverContext, ? extends TemplateResolver<StackTraceElement>>> RESOLVER_FACTORY_BY_NAME = createResolverFactoryByName();

    private static Map<String, TemplateResolverFactory<StackTraceElement, StackTraceElementObjectResolverContext, ? extends TemplateResolver<StackTraceElement>>> createResolverFactoryByName() {
        Map<String, TemplateResolverFactory<StackTraceElement, StackTraceElementObjectResolverContext, ? extends TemplateResolver<StackTraceElement>>> resolverFactoryByName = new LinkedHashMap<>();
        StackTraceElementObjectResolverFactory stackTraceElementObjectResolverFactory = StackTraceElementObjectResolverFactory.getInstance();
        resolverFactoryByName.put(stackTraceElementObjectResolverFactory.getName(), stackTraceElementObjectResolverFactory);
        return Collections.unmodifiableMap(resolverFactoryByName);
    }

    static Map<String, TemplateResolverFactory<StackTraceElement, StackTraceElementObjectResolverContext, ? extends TemplateResolver<StackTraceElement>>> getResolverFactoryByName() {
        return RESOLVER_FACTORY_BY_NAME;
    }

}
