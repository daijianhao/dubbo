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
package com.alibaba.dubbo.container;

import com.alibaba.dubbo.common.extension.SPI;

/**
 * Container. (SPI, Singleton, ThreadSafe)
 *
 * 服务容器是一个 standalone 的启动程序，因为后台服务不需要 Tomcat 或 JBoss 等 Web 容器的功能，如果硬要用 Web 容器去加载服务提供方，增加复杂性，也浪费资源。
 *
 * 服务容器只是一个简单的 Main 方法，并加载一个简单的 Spring 容器，用于暴露服务。
 *
 * 服务容器的加载内容可以扩展，内置了 spring, jetty, log4j 等加载，可通过容器扩展点进行扩展。配置配在 java 命令的 -D 参数或者 dubbo.properties 中。
 *
 * 从概念上我们可以看到，和 SpringBoot 类似，是 Dubbo 服务的启动器。🙂 考虑到目前 Spring 更加通用，所以实际实践时，更多采用的是 SpringBoot ，而不是 Dubbo 的服务容器。
 * jetty 服务容器实现已经移除，新增 logback 服务容器实现。
 */
@SPI("spring")//Dubbo SPI 拓展点，默认为 "spring"
public interface Container {

    /**
     * 启动
     * start.
     */
    void start();

    /**
     * 停止
     * stop.
     */
    void stop();

}