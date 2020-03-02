package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.Demo2Service;

/**
 * @author djh
 * @date 2020/3/2
 */
public class Demo2ServiceImpl implements Demo2Service {
    @Override
    public String sayHi(String name) {
        return "Hi " + name;
    }
}
