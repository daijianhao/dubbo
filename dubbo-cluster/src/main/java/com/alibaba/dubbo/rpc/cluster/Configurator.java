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
package com.alibaba.dubbo.rpc.cluster;

import com.alibaba.dubbo.common.URL;

/**
 * Configurator. (SPI, Prototype, ThreadSafe)
 * <p>
 * 实现 Comparable 接口，配置规则接口
 *
 * 一个 Configurator 对象，对应一条配置规则。
 * Configurator 有优先级的要求，所以实现 Comparable 接口。
 * #getUrl() 接口方法，获得配置 URL ，里面带有配置规则。
 * #configure(Url url) 接口方法，设置配置规则到指定 URL 中。
 */
public interface Configurator extends Comparable<Configurator> {

    /**
     * get the configurator url.
     * <p>
     * 配置规则
     *
     * @return configurator url.
     */
    URL getUrl();

    /**
     * Configure the provider url.
     * O
     * 配置到 URL 中
     *
     * @param url - old rovider url.
     * @return new provider url.
     */
    URL configure(URL url);

}
