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
package com.alibaba.dubbo.rpc.protocol.http;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.alibaba.dubbo.rpc.support.ProtocolUtils;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.httpinvoker.HttpComponentsHttpInvokerRequestExecutor;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import org.springframework.remoting.httpinvoker.HttpInvokerServiceExporter;
import org.springframework.remoting.httpinvoker.SimpleHttpInvokerRequestExecutor;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HttpProtocol
 * <p>
 * 实现 AbstractProxyProtocol 抽象类:http协议实现类
 */
public class HttpProtocol extends AbstractProxyProtocol {

    /**
     * 默认服务器端口
     */
    public static final int DEFAULT_PORT = 80;

    /**
     * Http 服务器集合
     * <p>
     * key：ip:port
     */
    private final Map<String, HttpServer> serverMap = new ConcurrentHashMap<String, HttpServer>();

    /**
     * Spring HttpInvokerServiceExporter 集合
     * <p>
     * key：path 服务名
     * <p>
     * Spring HttpInvokerServiceExporter 集合。
     * 请求处理过程为 HttpServer => DispatcherServlet => InternalHandler => HttpInvokerServiceExporter
     */
    private final Map<String, HttpInvokerServiceExporter> skeletonMap = new ConcurrentHashMap<String, HttpInvokerServiceExporter>();

    /**
     * HttpBinder$Adaptive 对象
     * <p>
     * HttpBinder$Adaptive 对象，通过 #setHttpBinder(httpBinder) 方法，Dubbo SPI 调用设置。
     */
    private HttpBinder httpBinder;

    public HttpProtocol() {
        super(RemoteAccessException.class);
    }

    public void setHttpBinder(HttpBinder httpBinder) {
        this.httpBinder = httpBinder;
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    protected <T> Runnable doExport(final T impl, Class<T> type, URL url) throws RpcException {
        // 获得服务器地址
        String addr = getAddr(url);
        // 获得 HttpServer 对象。若不存在，进行创建。
        HttpServer server = serverMap.get(addr);
        if (server == null) {
            //绑定并创建server
            server = httpBinder.bind(url, new InternalHandler());
            serverMap.put(addr, server);
        }
        // 创建 HttpInvokerServiceExporter 对象
        final String path = url.getAbsolutePath();
        skeletonMap.put(path, createExporter(impl, type));

        final String genericPath = path + "/" + Constants.GENERIC_KEY;
        // 添加到 skeletonMap 中
        skeletonMap.put(genericPath, createExporter(impl, GenericService.class));
        // 返回取消暴露的回调 Runnable
        return new Runnable() {
            @Override
            public void run() {
                skeletonMap.remove(path);
                skeletonMap.remove(genericPath);
            }
        };
    }

    private <T> HttpInvokerServiceExporter createExporter(T impl, Class<?> type) {
        final HttpInvokerServiceExporter httpServiceExporter = new HttpInvokerServiceExporter();
        httpServiceExporter.setServiceInterface(type);
        httpServiceExporter.setService(impl);
        try {
            httpServiceExporter.afterPropertiesSet();
        } catch (Exception e) {
            throw new RpcException(e.getMessage(), e);
        }
        return httpServiceExporter;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T> T doRefer(final Class<T> serviceType, final URL url) throws RpcException {
        // 创建 HttpInvokerProxyFactoryBean 对象
        final String generic = url.getParameter(Constants.GENERIC_KEY);
        final boolean isGeneric = ProtocolUtils.isGeneric(generic) || serviceType.equals(GenericService.class);

        final HttpInvokerProxyFactoryBean httpProxyFactoryBean = new HttpInvokerProxyFactoryBean();
        httpProxyFactoryBean.setRemoteInvocationFactory(new RemoteInvocationFactory() {
            @Override
            public RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
                RemoteInvocation invocation = new HttpRemoteInvocation(methodInvocation);
                if (isGeneric) {
                    invocation.addAttribute(Constants.GENERIC_KEY, generic);
                }
                return invocation;
            }
        });

        String key = url.toIdentityString();
        if (isGeneric) {
            key = key + "/" + Constants.GENERIC_KEY;
        }

        httpProxyFactoryBean.setServiceUrl(key);
        httpProxyFactoryBean.setServiceInterface(serviceType);
        // 创建执行器 SimpleHttpInvokerRequestExecutor 对象
        String client = url.getParameter(Constants.CLIENT_KEY);
        if (client == null || client.length() == 0 || "simple".equals(client)) {
            SimpleHttpInvokerRequestExecutor httpInvokerRequestExecutor = new SimpleHttpInvokerRequestExecutor() {
                @Override
                protected void prepareConnection(HttpURLConnection con,
                                                 int contentLength) throws IOException {
                    super.prepareConnection(con, contentLength);
                    con.setReadTimeout(url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
                    con.setConnectTimeout(url.getParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT));
                }
            };
            httpProxyFactoryBean.setHttpInvokerRequestExecutor(httpInvokerRequestExecutor);
        } else if ("commons".equals(client)) {
            //"simple"：创建执行器 SimpleHttpInvokerRequestExecutor 对象。
            //"commons"：创建执行器 HttpComponentsHttpInvokerRequestExecutor 对象。
            //两者的差异点在于使用的 HttpClient 不同，前者使用 JDK HttpClient ，后者使用 Apache HttpClient
            HttpComponentsHttpInvokerRequestExecutor httpInvokerRequestExecutor = new HttpComponentsHttpInvokerRequestExecutor();
            httpInvokerRequestExecutor.setReadTimeout(url.getParameter(Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT));
            httpInvokerRequestExecutor.setConnectTimeout(url.getParameter(Constants.CONNECT_TIMEOUT_KEY, Constants.DEFAULT_CONNECT_TIMEOUT));
            httpProxyFactoryBean.setHttpInvokerRequestExecutor(httpInvokerRequestExecutor);
        } else {
            throw new IllegalStateException("Unsupported http protocol client " + client + ", only supported: simple, commons");
        }
        httpProxyFactoryBean.afterPropertiesSet();
        return (T) httpProxyFactoryBean.getObject();
    }

    @Override
    protected int getErrorCode(Throwable e) {
        if (e instanceof RemoteAccessException) {
            e = e.getCause();
        }
        if (e != null) {
            Class<?> cls = e.getClass();
            if (SocketTimeoutException.class.equals(cls)) {
                return RpcException.TIMEOUT_EXCEPTION;
            } else if (IOException.class.isAssignableFrom(cls)) {
                return RpcException.NETWORK_EXCEPTION;
            } else if (ClassNotFoundException.class.isAssignableFrom(cls)) {
                return RpcException.SERIALIZATION_EXCEPTION;
            }
        }
        return super.getErrorCode(e);
    }

    /**
     * 负责处理Http请求
     */
    private class InternalHandler implements HttpHandler {

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            //取得请求的uri
            String uri = request.getRequestURI();
            //根据uri取出对应的exporter
            HttpInvokerServiceExporter skeleton = skeletonMap.get(uri);
            if (!request.getMethod().equalsIgnoreCase("POST")) {
                //必须是 POST 请求,否则返回500
                response.setStatus(500);
            } else {
                //记录远程客户端地址和端口
                RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());
                try {
                    //继续处理请求
                    skeleton.handleRequest(request, response);
                } catch (Throwable e) {
                    throw new ServletException(e);
                }
            }
        }

    }

}
