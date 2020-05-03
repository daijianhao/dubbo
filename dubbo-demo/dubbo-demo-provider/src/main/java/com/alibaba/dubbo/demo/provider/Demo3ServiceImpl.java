package com.alibaba.dubbo.demo.provider;

import com.alibaba.dubbo.demo.Demo2Service;
import com.alibaba.dubbo.demo.Demo3Service;

/**
 * @author djh
 * @date 2020/3/2
 */
public class Demo3ServiceImpl implements Demo3Service {
    @Override
    public String sayHi(String name) {
        return "Hi " + name;
    }
}
