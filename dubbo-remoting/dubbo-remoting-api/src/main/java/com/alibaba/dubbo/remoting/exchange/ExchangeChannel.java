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
package com.alibaba.dubbo.remoting.exchange;

import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.RemotingException;

/**
 * ExchangeChannel. (API/SPI, Prototype, ThreadSafe)
 * <p>
 * 在一次 RPC 调用，每个请求( Request )，是关注对应的响应( Response )。那么 transport 层 提供的网络传输 功能，是无法满足 RPC 的诉求的。因此，exchange 层，在其 Message 之上，构造了Request-Response 的模型。
 * <p>
 * 实现上，也非常简单，将 Message 分成 Request 和 Response 两种类型，并增加编号属性，将 Request 和 Response 能够一一映射。
 * <p>
 * 实际上，RPC 调用，会有更多特性的需求：1）异步处理返回结果；2）内置事件；3）等等。因此，Request 和 Response 上会有类似编号的系统字段。
 * <p>
 * 一条消息，我们分成两段：
 * <p>
 * 协议头( Header ) ： 系统字段，例如编号等。
 * 内容( Body ) ：具体请求的参数和响应的结果等。
 * <p>
 * <p>
 * 继承 Channel 接口，信息交换通道接口
 */
public interface ExchangeChannel extends Channel {

    /**
     * send request.
     * <p>
     * // 发送请求
     *
     * @param request
     * @return response future
     * @throws RemotingException
     */
    ResponseFuture request(Object request) throws RemotingException;

    /**
     * send request.
     * <p>
     * // 发送请求
     *
     * @param request
     * @param timeout
     * @return response future
     * @throws RemotingException
     */
    ResponseFuture request(Object request, int timeout) throws RemotingException;

    /**
     * get message handler.
     * 获得信息交换处理器
     *
     * @return message handler
     */

    ExchangeHandler getExchangeHandler();

    /**
     * graceful close.
     * 优雅关闭
     *
     * @param timeout
     */
    @Override
    void close(int timeout);

}
