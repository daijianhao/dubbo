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
package com.alibaba.dubbo.rpc.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.PojoUtils;
import com.alibaba.dubbo.common.utils.ReflectUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.ProxyFactory;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.RpcInvocation;
import com.alibaba.dubbo.rpc.RpcResult;
import com.alibaba.fastjson.JSON;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实现 Invoker 接口，Mock Invoker 实现类
 * @param <T>
 */
final public class MockInvoker<T> implements Invoker<T> {

    /**
     * ProxyFactory$Adaptive 对象
     */
    private final static ProxyFactory proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();

    /**
     * mock 与 Invoker 对象的映射缓存
     *
     * @see #getInvoker(String)
     */
    private final static Map<String, Invoker<?>> mocks = new ConcurrentHashMap<String, Invoker<?>>();

    /**
     * mock 与 Throwable 对象的映射缓存
     *
     * @see #getThrowable(String)
     */
    private final static Map<String, Throwable> throwables = new ConcurrentHashMap<String, Throwable>();

    /**
     * URL 对象
     */
    private final URL url;

    public MockInvoker(URL url) {
        this.url = url;
    }

    public static Object parseMockValue(String mock) throws Exception {
        return parseMockValue(mock, null);
    }

    public static Object parseMockValue(String mock, Type[] returnTypes) throws Exception {
        Object value = null;
        if ("empty".equals(mock)) {
            value = ReflectUtils.getEmptyObject(returnTypes != null && returnTypes.length > 0 ? (Class<?>) returnTypes[0] : null);
        } else if ("null".equals(mock)) {
            value = null;
        } else if ("true".equals(mock)) {
            value = true;
        } else if ("false".equals(mock)) {
            value = false;
        } else if (mock.length() >= 2 && (mock.startsWith("\"") && mock.endsWith("\"")
                || mock.startsWith("\'") && mock.endsWith("\'"))) {
            value = mock.subSequence(1, mock.length() - 1);
        } else if (returnTypes != null && returnTypes.length > 0 && returnTypes[0] == String.class) {
            value = mock;
        } else if (StringUtils.isNumeric(mock)) {
            value = JSON.parse(mock);
        } else if (mock.startsWith("{")) {
            value = JSON.parseObject(mock, Map.class);
        } else if (mock.startsWith("[")) {
            value = JSON.parseObject(mock, List.class);
        } else {
            value = mock;
        }
        if (returnTypes != null && returnTypes.length > 0) {
            value = PojoUtils.realize(value, (Class<?>) returnTypes[0], returnTypes.length > 1 ? returnTypes[1] : null);
        }
        return value;
    }

    @Override
    public Result invoke(Invocation invocation) throws RpcException {
        // 获得 `"mock"` 配置项，方法级 > 类级
        String mock = getUrl().getParameter(invocation.getMethodName() + "." + Constants.MOCK_KEY);
        if (invocation instanceof RpcInvocation) {
            ((RpcInvocation) invocation).setInvoker(this);
        }
        if (StringUtils.isBlank(mock)) {
            mock = getUrl().getParameter(Constants.MOCK_KEY);
        }

        if (StringUtils.isBlank(mock)) {
            // 不允许为空
            throw new RpcException(new IllegalAccessException("mock can not be null. url :" + url));
        }
        // 标准化 `"mock"` 配置项
        mock = normalizeMock(URL.decode(mock));
        // 等于 "return " ，返回值为空的 RpcResult 对象
        if (mock.startsWith(Constants.RETURN_PREFIX)) {
            mock = mock.substring(Constants.RETURN_PREFIX.length()).trim();
            try {
                Type[] returnTypes = RpcUtils.getReturnTypes(invocation);
                //解析mock返回值
                Object value = parseMockValue(mock, returnTypes);
                return new RpcResult(value);
            } catch (Exception ew) {
                throw new RpcException("mock return invoke error. method :" + invocation.getMethodName()
                        + ", mock:" + mock + ", url: " + url, ew);
            }
        } else if (mock.startsWith(Constants.THROW_PREFIX)) { // 以 "throw" 开头，抛出 RpcException 异常
            mock = mock.substring(Constants.THROW_PREFIX.length()).trim();
            if (StringUtils.isBlank(mock)) {
                // 禁止为空
                throw new RpcException("mocked exception for service degradation.");
            } else { // user customized class
                // 创建自定义异常
                Throwable t = getThrowable(mock);
                // 抛出业务类型的 RpcException 异常
                throw new RpcException(RpcException.BIZ_EXCEPTION, t);
            }
        } else { //impl mock
            try {
                // 创建 Invoker 对象
                Invoker<T> invoker = getInvoker(mock);
                // 执行 Invoker 对象的调用逻辑
                return invoker.invoke(invocation);
            } catch (Throwable t) {
                throw new RpcException("Failed to create mock implementation class " + mock, t);
            }
        }
    }

    public static Throwable getThrowable(String throwstr) {
        Throwable throwable = throwables.get(throwstr);
        if (throwable != null) {
            return throwable;
        }

        try {
            Throwable t;
            Class<?> bizException = ReflectUtils.forName(throwstr);
            Constructor<?> constructor;
            constructor = ReflectUtils.findConstructor(bizException, String.class);
            t = (Throwable) constructor.newInstance(new Object[]{"mocked exception for service degradation."});
            if (throwables.size() < 1000) {
                throwables.put(throwstr, t);
            }
            return t;
        } catch (Exception e) {
            throw new RpcException("mock throw error :" + throwstr + " argument error.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Invoker<T> getInvoker(String mockService) {
        // 从缓存中，获得 Invoker 对象
        Invoker<T> invoker = (Invoker<T>) mocks.get(mockService);
        if (invoker != null) {
            return invoker;
        }
        // 不存在，创建 Invoker 对象
        // 1. 获得接口类
        Class<T> serviceType = (Class<T>) ReflectUtils.forName(url.getServiceInterface());
        T mockObject = (T) getMockObject(mockService, serviceType);
        // 6. 创建 Mock 对应，对应的 Invoker 对象
        invoker = proxyFactory.getInvoker(mockObject, serviceType, url);
        // 7. 添加到缓存
        if (mocks.size() < 10000) {
            mocks.put(mockService, invoker);
        }
        return invoker;
    }

    @SuppressWarnings("unchecked")
    public static Object getMockObject(String mockService, Class serviceType) {
        // 2. 若为 `true` `default` ，修改修改为对应接口 + "Mock" 类。这种情况出现在原始 `mock = fail:true` 或 `mock = force:true` 等情况
        if (ConfigUtils.isDefault(mockService)) {
            mockService = serviceType.getName() + "Mock";
        }
        // 3. 获得 Mock 类
        Class<?> mockClass = ReflectUtils.forName(mockService);
        // 4. 校验 Mock 类，实现了接口类
        if (!serviceType.isAssignableFrom(mockClass)) {
            throw new IllegalStateException("The mock class " + mockClass.getName() +
                    " not implement interface " + serviceType.getName());
        }

        try {
            // 5. 创建 Mock 对象
            return mockClass.newInstance();
        } catch (InstantiationException e) {
            throw new IllegalStateException("No default constructor from mock class " + mockClass.getName(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }


    /**
     * Normalize mock string:
     *
     * <ol>
     * <li>return => return null</li>
     * <li>fail => default</li>
     * <li>force => default</li>
     * <li>fail:throw/return foo => throw/return foo</li>
     * <li>force:throw/return foo => throw/return foo</li>
     * </ol>
     *
     * @param mock mock string
     * @return normalized mock string
     */
    public static String normalizeMock(String mock) {
        // 若为空，直接返回
        if (mock == null) {
            return mock;
        }

        mock = mock.trim();

        if (mock.length() == 0) {
            return mock;
        }

        if (Constants.RETURN_KEY.equalsIgnoreCase(mock)) {
            return Constants.RETURN_PREFIX + "null";
        }

        // 若果为 "true" "default" "fail" "force" 四种字符串，修改为对应接口 + "Mock" 类
        if (ConfigUtils.isDefault(mock) || "fail".equalsIgnoreCase(mock) || "force".equalsIgnoreCase(mock)) {
            return "default";
        }
        // 若以 "fail:" 开头，去掉该开头
        if (mock.startsWith(Constants.FAIL_PREFIX)) {
            mock = mock.substring(Constants.FAIL_PREFIX.length()).trim();
        }
        // 若以 "force:" 开头，去掉该开头
        if (mock.startsWith(Constants.FORCE_PREFIX)) {
            mock = mock.substring(Constants.FORCE_PREFIX.length()).trim();
        }

        if (mock.startsWith(Constants.RETURN_PREFIX) || mock.startsWith(Constants.THROW_PREFIX)) {
            mock = mock.replace('`', '"');
        }

        return mock;
    }

    @Override
    public URL getUrl() {
        return this.url;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void destroy() {
        //do nothing
    }

    @Override
    public Class<T> getInterface() {
        //FIXME
        return null;
    }
}
