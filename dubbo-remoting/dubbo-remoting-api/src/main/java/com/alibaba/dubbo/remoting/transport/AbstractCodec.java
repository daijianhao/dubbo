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
package com.alibaba.dubbo.remoting.transport;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.serialize.Serialization;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.Codec2;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * AbstractCodec
 *
 * 实现 Codec2 接口，提供如下公用方法：
 *
 * #checkPayload(channel, size) 静态方法，校验消息长度。
 * #getSerialization(channel) 方法，获得 Serialization 对象。
 * #isClientSide(channel) 方法，是否为客户端侧的通道。
 * #isServerSide(channel) 方法，是否为服务端侧的通道。
 *
 * 编解码器的实现，通过继承的方式，获得更多的功能。每一个 Codec2 类实现对不同消息的编解码。通过协议头来判断，具体使用哪个编解码逻辑
 */
public abstract class AbstractCodec implements Codec2 {

    private static final Logger logger = LoggerFactory.getLogger(AbstractCodec.class);

    protected static void checkPayload(Channel channel, long size) throws IOException {
        int payload = Constants.DEFAULT_PAYLOAD;
        if (channel != null && channel.getUrl() != null) {
            payload = channel.getUrl().getParameter(Constants.PAYLOAD_KEY, Constants.DEFAULT_PAYLOAD);
        }
        if (payload > 0 && size > payload) {
            ExceedPayloadLimitException e = new ExceedPayloadLimitException("Data length too large: " + size + ", max payload: " + payload + ", channel: " + channel);
            logger.error(e);
            throw e;
        }
    }

    protected Serialization getSerialization(Channel channel) {
        return CodecSupport.getSerialization(channel.getUrl());
    }

    protected boolean isClientSide(Channel channel) {
        String side = (String) channel.getAttribute(Constants.SIDE_KEY);
        if ("client".equals(side)) {
            return true;
        } else if ("server".equals(side)) {
            return false;
        } else {
            InetSocketAddress address = channel.getRemoteAddress();
            URL url = channel.getUrl();
            boolean client = url.getPort() == address.getPort()
                    && NetUtils.filterLocalHost(url.getIp()).equals(
                    NetUtils.filterLocalHost(address.getAddress()
                            .getHostAddress()));
            channel.setAttribute(Constants.SIDE_KEY, client ? "client"
                    : "server");
            return client;
        }
    }

    protected boolean isServerSide(Channel channel) {
        return !isClientSide(channel);
    }

}
