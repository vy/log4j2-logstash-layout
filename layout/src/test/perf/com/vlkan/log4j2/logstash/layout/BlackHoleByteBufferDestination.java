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

package com.vlkan.log4j2.logstash.layout;

import org.apache.logging.log4j.core.layout.ByteBufferDestination;

import java.nio.ByteBuffer;

class BlackHoleByteBufferDestination implements ByteBufferDestination {

    private final ByteBuffer byteBuffer;

    BlackHoleByteBufferDestination(int maxByteCount) {
        this.byteBuffer = ByteBuffer.allocate(maxByteCount);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    @Override
    public ByteBuffer drain(ByteBuffer byteBuffer) {
        byteBuffer.clear();
        return byteBuffer;
    }

    @Override
    public void writeBytes(ByteBuffer byteBuffer) {
        byteBuffer.clear();
    }

    @Override
    public void writeBytes(byte[] data, int offset, int length) {}

}
