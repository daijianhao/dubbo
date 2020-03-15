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

import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;

/**
 * ExchangeHandler. (API, Prototype, ThreadSafe)
 */
public interface ExchangeHandler extends ChannelHandler, TelnetHandler {

    /**
     * 注意，返回的是请求结果。正如我们在上文看到的，将请求结果，设置到 Response.mResult 属性中。
     * ExchangeHandler 是一个非常关键的接口。为什么这么说呢，点击 DubboProtocol. requestHandler ！胖友，领悟到了么？
     */
    Object reply(ExchangeChannel channel, Object request) throws RemotingException;

}