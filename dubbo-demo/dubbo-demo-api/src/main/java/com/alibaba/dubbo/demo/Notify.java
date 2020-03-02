package com.alibaba.dubbo.demo;

public interface Notify {
    void onreturn(String msg, String name);

    void onthrow(Throwable ex, String name);
}