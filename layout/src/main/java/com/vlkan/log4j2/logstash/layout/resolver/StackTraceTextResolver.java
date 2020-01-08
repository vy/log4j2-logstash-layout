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
import org.apache.logging.log4j.util.Constants;

import java.io.IOException;
import java.util.function.Supplier;

class StackTraceTextResolver implements StackTraceResolver {

    private final Supplier<BufferedPrintWriter> writerSupplier;

    private final ThreadLocal<BufferedPrintWriter> writerRef;

    StackTraceTextResolver(int writerCapacity) {
        this.writerSupplier = () -> BufferedPrintWriter.ofCapacity(writerCapacity);
        this.writerRef = Constants.ENABLE_THREADLOCALS
                ? ThreadLocal.withInitial(writerSupplier)
                : null;
    }

    @Override
    public void resolve(Throwable throwable, JsonGenerator jsonGenerator) throws IOException {
        BufferedPrintWriter writer = getResetWriter();
        throwable.printStackTrace(writer);
        jsonGenerator.writeString(writer.getBuffer(), 0, writer.getPosition());
    }

    private BufferedPrintWriter getResetWriter() {
        BufferedPrintWriter writer;
        if (Constants.ENABLE_THREADLOCALS) {
            writer = writerRef.get();
            writer.close();
        } else {
            writer = writerSupplier.get();
        }
        return writer;
    }

}
