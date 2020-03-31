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
package com.alibaba.dubbo.common.serialize.fst;

import com.alibaba.dubbo.common.serialize.support.SerializableClassRegistry;

import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * FST fast-serialization 是重新实现的 Java 快速对象序列化的开发包。序列化速度更快（2-10倍）、体积更小，
 * 而且兼容 JDK 原生的序列化。要求 JDK 1.7 支持。
 */
public class FstFactory {
    /**
     * 单例
     */
    private static final FstFactory factory = new FstFactory();
    /**
     * 配置对象
     * FST 配置对象。在构造方法中，将 SerializableClassRegistry 注册表需要使用优化的类，注册到 FSTConfiguration 中
     */
    private final FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();


    public static FstFactory getDefaultFactory() {
        return factory;
    }

    public FstFactory() {
        // 注册
        for (Class clazz : SerializableClassRegistry.getRegisteredClasses().keySet()) {
            conf.registerClass(clazz);
        }
    }

    public FSTObjectOutput getObjectOutput(OutputStream outputStream) {
        return conf.getObjectOutput(outputStream);
    }

    public FSTObjectInput getObjectInput(InputStream inputStream) {
        return conf.getObjectInput(inputStream);
    }
}
