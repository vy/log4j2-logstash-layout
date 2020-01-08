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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

import java.util.Map;

interface TemplateResolverContext<V, C extends TemplateResolverContext<V, C>> {

    Class<C> getContextClass();

    Map<String, TemplateResolverFactory<V, C, ? extends TemplateResolver<V>>> getResolverFactoryByName();

    ObjectMapper getObjectMapper();

    StrSubstitutor getSubstitutor();

    boolean isEmptyPropertyExclusionEnabled();

}
