/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.serialize.kryo;

import com.alibaba.dubbo.common.serialize.Cleanable;
import com.alibaba.dubbo.common.serialize.ObjectOutput;
import com.alibaba.dubbo.common.serialize.kryo.utils.KryoUtils;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 实现 ObjectOutput, Cleanable 接口，Kryo 对象输出实现类。
 */
public class KryoObjectOutput implements ObjectOutput, Cleanable {

    /**
     * Kryo 对象
     */
    private Output output;

    /**
     * Kryo 输出
     */
    private Kryo kryo;

    public KryoObjectOutput(OutputStream outputStream) {
        output = new Output(outputStream);
        this.kryo = KryoUtils.get();
    }

    /**
     * 来自 DataOutput 的实现方法，调用 input 对应的方法
     * @param v value.
     * @throws IOException
     */
    @Override
    public void writeBool(boolean v) throws IOException {
        output.writeBoolean(v);
    }

    @Override
    public void writeByte(byte v) throws IOException {
        output.writeByte(v);
    }

    @Override
    public void writeShort(short v) throws IOException {
        output.writeShort(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        output.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        output.writeLong(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        output.writeFloat(v);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        output.writeDouble(v);
    }

    @Override
    public void writeBytes(byte[] v) throws IOException {
        if (v == null) {
            output.writeInt(-1);
        } else {
            writeBytes(v, 0, v.length);
        }
    }

    @Override
    public void writeBytes(byte[] v, int off, int len) throws IOException {
        if (v == null) {
            output.writeInt(-1);
        } else {
            output.writeInt(len);
            output.write(v, off, len);
        }
    }


    @Override
    public void writeUTF(String v) throws IOException {
        output.writeString(v);
    }

    @Override
    public void writeObject(Object v) throws IOException {
        // TODO carries class info every time.
        kryo.writeClassAndObject(output, v);
    }

    @Override
    public void flushBuffer() throws IOException {
        output.flush();
    }

    @Override
    public void cleanup() {
        // 释放 Kryo 对象
        KryoUtils.release(kryo);
        kryo = null;
    }
}
