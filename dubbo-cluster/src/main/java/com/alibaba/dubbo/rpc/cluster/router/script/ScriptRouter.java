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
package com.alibaba.dubbo.rpc.cluster.router.script;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.cluster.router.AbstractRouter;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ScriptRouter
 *
 * 实现 Router 接口，基于脚本的 Router 实现类。
 *
 * 脚本路由规则 4 支持 JDK 脚本引擎的所有脚本，
 * 比如：javascript, jruby, groovy 等，通过 type=javascript 参数设置脚本类型，缺省为 javascript。
 */
public class ScriptRouter extends AbstractRouter {

    private static final Logger logger = LoggerFactory.getLogger(ScriptRouter.class);

    private static final int DEFAULT_PRIORITY = 1;

    /**
     * 脚本类型 与 ScriptEngine 的映射缓存
     */
    private static final Map<String, ScriptEngine> engines = new ConcurrentHashMap<String, ScriptEngine>();

    private final ScriptEngine engine;

    /**
     * 路由规则内容
     */
    private final String rule;

    public ScriptRouter(URL url) {
        this.url = url;
        String type = url.getParameter(Constants.TYPE_KEY);
        this.priority = url.getParameter(Constants.PRIORITY_KEY, DEFAULT_PRIORITY);
        String rule = url.getParameterAndDecoded(Constants.RULE_KEY);
        if (type == null || type.length() == 0) {
            type = Constants.DEFAULT_SCRIPT_TYPE_KEY;
        }
        if (rule == null || rule.length() == 0) {
            throw new IllegalStateException(new IllegalStateException("route rule can not be empty. rule:" + rule));
        }
        // 初始化 `engine`
        ScriptEngine engine = engines.get(type);
        if (engine == null) {
            // 在缓存中不存在，则进行创建 ScriptEngine 对象
            engine = new ScriptEngineManager().getEngineByName(type);
            if (engine == null) {
                throw new IllegalStateException(new IllegalStateException("Unsupported route rule type: " + type + ", rule: " + rule));
            }
            engines.put(type, engine);
        }
        this.engine = engine;
        this.rule = rule;
    }

    /**
     *
     * 基于脚本引擎的路由规则，如：
     * > function route(invokers) {
     * >     var result = new java.util.ArrayList(invokers.size());
     * >     for (i = 0; i < invokers.size(); i ++) {
     * >         if ("10.20.153.10".equals(invokers.get(i).getUrl().getHost())) {
     * >             result.add(invokers.get(i));
     * >         }
     * >     }
     * >     return result;
     * > } (invokers); // 表示立即执行方法
     * >
     * @param invokers
     * @param url        refer url
     * @param invocation
     * @param <T>
     * @return
     * @throws RpcException
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {
        try {
            // 执行脚本
            List<Invoker<T>> invokersCopy = new ArrayList<Invoker<T>>(invokers);
            Compilable compilable = (Compilable) engine;
            Bindings bindings = engine.createBindings();
            bindings.put("invokers", invokersCopy);
            bindings.put("invocation", invocation);
            bindings.put("context", RpcContext.getContext());
            // 编译
            CompiledScript function = compilable.compile(rule);
            // 执行
            Object obj = function.eval(bindings);
            // 根据结果类型，转换成 (List<Invoker<T>> 类型返回
            if (obj instanceof Invoker[]) {
                invokersCopy = Arrays.asList((Invoker<T>[]) obj);
            } else if (obj instanceof Object[]) {
                invokersCopy = new ArrayList<Invoker<T>>();
                for (Object inv : (Object[]) obj) {
                    invokersCopy.add((Invoker<T>) inv);
                }
            } else {
                // 发生异常，忽略路由规则，返回全 `invokers` 集合
                invokersCopy = (List<Invoker<T>>) obj;
            }
            return invokersCopy;
        } catch (ScriptException e) {
            //fail then ignore rule .invokers.
            logger.error("route error , rule has been ignored. rule: " + rule + ", method:" + invocation.getMethodName() + ", url: " + RpcContext.getContext().getUrl(), e);
            return invokers;
        }
    }

}
