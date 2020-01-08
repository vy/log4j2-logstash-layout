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

import java.io.IOException;

class EndOfBatchResolver implements EventResolver {

    private static final EndOfBatchResolver INSTANCE = new EndOfBatchResolver();

    private EndOfBatchResolver() {}

    static EndOfBatchResolver getInstance() {
        return INSTANCE;
    }

    static String getName() {
        return "endOfBatch";
    }

    @Override
    public void resolve(LogEvent logEvent, JsonGenerator jsonGenerator) throws IOException {
        boolean endOfBatch = logEvent.isEndOfBatch();
        jsonGenerator.writeBoolean(endOfBatch);
    }

}
