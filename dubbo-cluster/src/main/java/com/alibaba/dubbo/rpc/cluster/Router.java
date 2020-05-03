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
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.List;

/**
 * Router. (SPI, Prototype, ThreadSafe)
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Routing">Routing</a>
 *
 * 实现 Comparable 接口，路由规则接口
 *
 * 路由规则  即 可以控制某个消费端 或 某个消费端的某个方法 只调用指定的 服务端等
 *
 *
 * 一个 Router 对象，对应一条路由规则。
 * Configurator 有优先级的要求，所以实现 Comparable 接口。
 * #getUrl() 接口方法，获得路由 URL ，里面带有路由规则。
 * #route(List<Invoker<T>> invokers, URL url, Invocation invocation) 接口方法，路由，筛选匹配的 Invoker 集合。
 *
 * http://dubbo.apache.org/zh-cn/docs/user/demos/routing-rule.html
 * @see com.alibaba.dubbo.rpc.cluster.Cluster#join(Directory)
 * @see com.alibaba.dubbo.rpc.cluster.Directory#list(Invocation)
 */
public interface Router extends Comparable<Router>{

    /**
     * get the router url.
     * 路由规则 URL
     * @return url
     */
    URL getUrl();

    /**
     * route.
     *
     * 路由，筛选匹配的 Invoker 集合
     * @param invokers
     * @param url        refer url
     * @param invocation
     * @return routed invokers
     * @throws RpcException
     */
    <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException;

    /**
     * Router's priority, used to sort routers.
     *
     * @return router's priority
     */
    int getPriority();

}