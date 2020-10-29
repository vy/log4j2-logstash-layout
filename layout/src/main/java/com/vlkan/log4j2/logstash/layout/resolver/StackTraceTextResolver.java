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
import com.vlkan.log4j2.logstash.layout.util.BufferedPrintWriter;
import com.vlkan.log4j2.logstash.layout.util.BufferedPrintWriterPool;

import java.io.IOException;

class StackTraceTextResolver implements StackTraceResolver {

    private final BufferedPrintWriterPool writerPool;

    StackTraceTextResolver(BufferedPrintWriterPool writerPool) {
        this.writerPool = writerPool;
    }

    @Override
    public void resolve(Throwable throwable, JsonGenerator jsonGenerator) throws IOException {
        BufferedPrintWriter writer = writerPool.acquire();
        try {
            Throwable lastThrowable = throwable;
            while (true) {
                try {
                    lastThrowable.printStackTrace(writer);
                    break;
                }
                // It is indeed not a good practice to catch `Throwable`s, but
                // what one should do while trying to dump the stack trace of a
                // failure? Hence, if `Throwable#printStackTrace(PrintWriter)`
                // fails for some reason, at least try to dump the reason of the
                // failure.
                catch (Throwable newThrowable) {
                    writer.close();
                    lastThrowable = newThrowable;
                }
            }
            jsonGenerator.writeString(writer.getBuffer(), 0, writer.getPosition());
        } finally {
            writerPool.release(writer);
        }
    }

}
