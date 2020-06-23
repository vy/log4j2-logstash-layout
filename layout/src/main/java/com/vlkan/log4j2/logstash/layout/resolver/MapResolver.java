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

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;

import java.io.IOException;

class MapResolver implements EventResolver {

    private final String key;

    static String getName() {
        return "map";
    }

    MapResolver(String key) {
        this.key = key;
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        if (!(logEvent.getMessage() instanceof MapMessage)) {
            jsonGenerator.writeNull();
        } else {
            @SuppressWarnings("unchecked")
            MapMessage<?, Object> message = (MapMessage<?, Object>) logEvent.getMessage();
            IndexedReadOnlyStringMap map = message.getIndexedReadOnlyStringMap();
            final Object value = map.getValue(key);
            jsonGenerator.writeObject(value);
        }
    }

}
