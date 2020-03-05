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
package com.alibaba.dubbo.rpc.service;

/**
 * Generic service interface
 *
 *泛化接口调用方式主要用于客户端没有 API 接口及模型类元的情况，参数及返回值中的所有 POJO 均用 Map 表示，
 * 通常用于框架集成，比如：实现一个通用的服务测试框架，可通过 GenericService 调用所有服务实现。
 *
 * 请注意，消费消费者没有 API 接口 及 模型类元。那就是说，Dubbo 在泛化引用中，需要做两件事情：
 * 没有 API 接口，所以提供一个泛化服务接口，目前是 com.alibaba.dubbo.rpc.service.GenericService
 *
 *
 * 一个泛化引用，只对应一个服务实现
 */
public interface GenericService {

    /**
     * Generic invocation
     *
     * 泛化调用
     *
     * @param method         Method name, e.g. findPerson. If there are overridden methods, parameter info is
     *                       required, e.g. findPerson(java.lang.String)
     *                       方法名
     * @param parameterTypes Parameter types
     *                       参数类型数组
     * @param args           Arguments
     *                       参数数组
     * @return invocation return value 调用结果
     * @throws Throwable potential exception thrown from the invocation
     */
    Object $invoke(String method, String[] parameterTypes, Object[] args) throws GenericException;

}