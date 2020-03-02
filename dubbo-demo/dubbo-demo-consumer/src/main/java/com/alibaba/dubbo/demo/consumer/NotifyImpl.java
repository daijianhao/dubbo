package com.alibaba.dubbo.demo.consumer;

import com.alibaba.dubbo.demo.Notify;

class NotifyImpl implements Notify {
    @Override
    public void onreturn(String msg, String name) {
        System.out.println("onreturn:" + msg);
    }

    @Override
    public void onthrow(Throwable ex, String name) {
        System.out.println(ex + ":" + name);
    }
}