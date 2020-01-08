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
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;

import java.util.Map;

public class StackTraceElementObjectResolverContext implements TemplateResolverContext<StackTraceElement, StackTraceElementObjectResolverContext> {

    private final ObjectMapper objectMapper;

    private final StrSubstitutor substitutor;

    private final boolean emptyPropertyExclusionEnabled;

    private StackTraceElementObjectResolverContext(Builder builder) {
        this.objectMapper = builder.objectMapper;
        this.substitutor = builder.substitutor;
        this.emptyPropertyExclusionEnabled = builder.emptyPropertyExclusionEnabled;
    }

    @Override
    public Class<StackTraceElementObjectResolverContext> getContextClass() {
        return StackTraceElementObjectResolverContext.class;
    }

    @Override
    public Map<String, TemplateResolverFactory<StackTraceElement, StackTraceElementObjectResolverContext, ? extends TemplateResolver<StackTraceElement>>> getResolverFactoryByName() {
        return StackTraceElementObjectResolverFactories.getResolverFactoryByName();
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public StrSubstitutor getSubstitutor() {
        return substitutor;
    }

    @Override
    public boolean isEmptyPropertyExclusionEnabled() {
        return emptyPropertyExclusionEnabled;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private ObjectMapper objectMapper;

        private StrSubstitutor substitutor;

        private boolean emptyPropertyExclusionEnabled;

        private Builder() {
            // Do nothing.
        }

        public Builder setObjectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder setSubstitutor(StrSubstitutor substitutor) {
            this.substitutor = substitutor;
            return this;
        }

        public Builder setEmptyPropertyExclusionEnabled(boolean emptyPropertyExclusionEnabled) {
            this.emptyPropertyExclusionEnabled = emptyPropertyExclusionEnabled;
            return this;
        }

        public StackTraceElementObjectResolverContext build() {
            validate();
            return new StackTraceElementObjectResolverContext(this);
        }

        private void validate() {
            Validate.notNull(objectMapper, "objectMapper");
            Validate.notNull(substitutor, "substitutor");
        }

    }

}
