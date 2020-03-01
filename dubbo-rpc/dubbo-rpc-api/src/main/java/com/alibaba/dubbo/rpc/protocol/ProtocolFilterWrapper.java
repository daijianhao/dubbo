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
package com.alibaba.dubbo.rpc.protocol;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.rpc.Exporter;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Protocol;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

import java.util.List;

/**
 * ListenerProtocol
 * 实现 Protocol 接口，Protocol 的 Wrapper 拓展实现类，用于给 Invoker 增加过滤链
 */
public class ProtocolFilterWrapper implements Protocol {

    private final Protocol protocol;

    public ProtocolFilterWrapper(Protocol protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("protocol == null");
        }
        this.protocol = protocol;
    }

    /**
     * 创建带 Filter 链的 Invoker 对象
     */
    private static <T> Invoker<T> buildInvokerChain(final Invoker<T> invoker, String key, String group) {
        Invoker<T> last = invoker;
        // 获得过滤器数组
        //key 属性，获得 URL 参数名。
        //
        //该参数用于获得 ServiceConfig 或 ReferenceConfig 配置的自定义过滤器。
        //以 ServiceConfig 举例子，例如 url = injvm://127.0.0.1/com.alibaba.dubbo.demo.DemoService?anyhost=true
        // &application=demo-provider&bind.ip=192.168.3.17&bind.port=20880&default.delay=-1&default.retries=0&default.service.filter=demo&delay=-1&dubbo=2.0.0&generic=false&interface=com.alibaba.dubbo.demo.DemoService&methods=sayHello&pid=81844&qos.port=22222&service.filter=demo&side=provider&timestamp=1520682156043 中，service.filter=demo，这是笔者配置自定义的 DemoFilter 过滤器。
        //group 属性，分组。
        //      在暴露服务时，group = provider 。
        //      在引用服务时，group = consumer 。
        /*
        继续以上面的例子为基础，filters 为：
            EchoFilter
            ClassLoaderFilter
            GenericFilter
            ContextFilter
            TraceFilter
            TimeoutFilter
            MonitorFilter
            ExceptionFilter
            DemoFilter 【自定义】
         */
        List<Filter> filters = ExtensionLoader.getExtensionLoader(Filter.class).getActivateExtension(invoker.getUrl(), key, group);
        // 倒序循环 Filter ，创建带 Filter 链的 Invoker 对象
        if (!filters.isEmpty()) {
            //因为是通过嵌套声明匿名类循环调用的方式，所以要倒序。胖友可以手工模拟下这个过程。通过这样的方式，实际过滤的顺序，还是我们上面看到的正序。
            for (int i = filters.size() - 1; i >= 0; i--) {
                final Filter filter = filters.get(i);
                final Invoker<T> next = last;
                last = new Invoker<T>() {

                    @Override
                    public Class<T> getInterface() {
                        return invoker.getInterface();
                    }

                    @Override
                    public URL getUrl() {
                        return invoker.getUrl();
                    }

                    @Override
                    public boolean isAvailable() {
                        return invoker.isAvailable();
                    }

                    @Override
                    public Result invoke(Invocation invocation) throws RpcException {
                        return filter.invoke(next, invocation);
                    }

                    @Override
                    public void destroy() {
                        invoker.destroy();
                    }

                    @Override
                    public String toString() {
                        return invoker.toString();
                    }
                };
            }
        }
        return last;
    }

    @Override
    public int getDefaultPort() {
        return protocol.getDefaultPort();
    }

    @Override
    public <T> Exporter<T> export(Invoker<T> invoker) throws RpcException {
        // 注册中心
        if (Constants.REGISTRY_PROTOCOL.equals(invoker.getUrl().getProtocol())) {//跳过，本地暴露服务不会符合这个判断。在远程暴露服务会符合暴露该判断
            return protocol.export(invoker);
        }
        // 建立带有 Filter 过滤链的 Invoker ，再暴露服务。
        return protocol.export(buildInvokerChain(invoker, Constants.SERVICE_FILTER_KEY, Constants.PROVIDER));
    }

    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        // 注册中心
        if (Constants.REGISTRY_PROTOCOL.equals(url.getProtocol())) {
            return protocol.refer(type, url);
        }
        // 引用服务，返回 Invoker 对象
        // 给改 Invoker 对象，包装成带有 Filter 过滤链的 Invoker 对象
        return buildInvokerChain(protocol.refer(type, url), Constants.REFERENCE_FILTER_KEY, Constants.CONSUMER);
    }

    @Override
    public void destroy() {
        protocol.destroy();
    }

}
