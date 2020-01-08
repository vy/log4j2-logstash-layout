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

package com.vlkan.log4j2.logstash.layout.util;

import java.io.Writer;

public final class BufferedWriter extends Writer {

    private final char[] buffer;

    private int position;

    BufferedWriter(int capacity) {
        this.buffer = new char[capacity];
        this.position = 0;
    }

    char[] getBuffer() {
        return buffer;
    }

    int getPosition() {
        return position;
    }

    int getCapacity() {
        return buffer.length;
    }

    @Override
    public void write(char[] source, int offset, int length) {
        System.arraycopy(source, offset, buffer, position, length);
        position += length;
    }

    @Override
    public void flush() {}

    @Override
    public void close() {
        position = 0;
    }

}
