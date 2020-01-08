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

class TimestampResolverFactory implements EventResolverFactory<TimestampResolver> {

    private static final TimestampResolverFactory INSTANCE = new TimestampResolverFactory();

    private TimestampResolverFactory() {}

    static TimestampResolverFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getName() {
        return TimestampResolver.getName();
    }

    @Override
    public TimestampResolver create(EventResolverContext context, String key) {
        return new TimestampResolver(context, key);
    }

}
