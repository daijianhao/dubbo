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
package com.alibaba.dubbo.remoting.exchange.support;

import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.exchange.ExchangeChannel;
import com.alibaba.dubbo.remoting.exchange.ExchangeHandler;
import com.alibaba.dubbo.remoting.telnet.TelnetHandler;
import com.alibaba.dubbo.remoting.telnet.support.TelnetHandlerAdapter;
import com.alibaba.dubbo.remoting.transport.ChannelHandlerDispatcher;

/**
 * ExchangeHandlerDispatcher
 * 实现 ExchangeHandler 接口，信息交换处理器调度器实现类
 *
 * 通过 ExchangeHandlerDispatcher ，将 ReplierDispatcher + ChannelHandlerDispatcher + TelnetHandler 三者结合在一起
 */
public class ExchangeHandlerDispatcher implements ExchangeHandler {

    /**
     * 回复者调度器
     */
    private final ReplierDispatcher replierDispatcher;

    /**
     * 通道处理器集合
     */
    private final ChannelHandlerDispatcher handlerDispatcher;

    /**
     * Telnet 命令处理器
     */
    private final TelnetHandler telnetHandler;

    public ExchangeHandlerDispatcher() {
        replierDispatcher = new ReplierDispatcher();
        handlerDispatcher = new ChannelHandlerDispatcher();
        telnetHandler = new TelnetHandlerAdapter();
    }

    public ExchangeHandlerDispatcher(Replier<?> replier) {
        replierDispatcher = new ReplierDispatcher(replier);
        handlerDispatcher = new ChannelHandlerDispatcher();
        telnetHandler = new TelnetHandlerAdapter();
    }

    public ExchangeHandlerDispatcher(ChannelHandler... handlers) {
        replierDispatcher = new ReplierDispatcher();
        handlerDispatcher = new ChannelHandlerDispatcher(handlers);
        telnetHandler = new TelnetHandlerAdapter();
    }

    public ExchangeHandlerDispatcher(Replier<?> replier, ChannelHandler... handlers) {
        replierDispatcher = new ReplierDispatcher(replier);
        handlerDispatcher = new ChannelHandlerDispatcher(handlers);
        telnetHandler = new TelnetHandlerAdapter();
    }

    public ExchangeHandlerDispatcher addChannelHandler(ChannelHandler handler) {
        handlerDispatcher.addChannelHandler(handler);
        return this;
    }

    public ExchangeHandlerDispatcher removeChannelHandler(ChannelHandler handler) {
        handlerDispatcher.removeChannelHandler(handler);
        return this;
    }

    public <T> ExchangeHandlerDispatcher addReplier(Class<T> type, Replier<T> replier) {
        replierDispatcher.addReplier(type, replier);
        return this;
    }

    public <T> ExchangeHandlerDispatcher removeReplier(Class<T> type) {
        replierDispatcher.removeReplier(type);
        return this;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object reply(ExchangeChannel channel, Object request) throws RemotingException {
        return ((Replier) replierDispatcher).reply(channel, request);
    }

    @Override
    public void connected(Channel channel) {
        handlerDispatcher.connected(channel);
    }

    @Override
    public void disconnected(Channel channel) {
        handlerDispatcher.disconnected(channel);
    }

    @Override
    public void sent(Channel channel, Object message) {
        handlerDispatcher.sent(channel, message);
    }

    @Override
    public void received(Channel channel, Object message) {
        handlerDispatcher.received(channel, message);
    }

    @Override
    public void caught(Channel channel, Throwable exception) {
        handlerDispatcher.caught(channel, exception);
    }

    @Override
    public String telnet(Channel channel, String message) throws RemotingException {
        return telnetHandler.telnet(channel, message);
    }

}
