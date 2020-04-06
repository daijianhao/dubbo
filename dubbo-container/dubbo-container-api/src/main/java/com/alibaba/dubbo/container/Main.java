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
package com.alibaba.dubbo.container;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main. (API, Static, ThreadSafe)
 * <p>
 * 启动程序，负责初始化 Container 服务容器
 */
public class Main {

    /**
     * Container 配置 KEY
     */
    public static final String CONTAINER_KEY = "dubbo.container";

    /**
     * ShutdownHook 是否开启配置 KEY
     */
    public static final String SHUTDOWN_HOOK_KEY = "dubbo.shutdown.hook";

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /**
     * Container 拓展点对应的 ExtensionLoader 对象
     */
    private static final ExtensionLoader<Container> loader = ExtensionLoader.getExtensionLoader(Container.class);

    private static final ReentrantLock LOCK = new ReentrantLock();

    private static final Condition STOP = LOCK.newCondition();

    public static void main(String[] args) {
        try {
            // 若 main 函数参数传入为空，从配置中加载
            if (args == null || args.length == 0) {
                String config = ConfigUtils.getProperty(CONTAINER_KEY, loader.getDefaultExtensionName());
                args = Constants.COMMA_SPLIT_PATTERN.split(config);
            }
            // 加载容器数组
            final List<Container> containers = new ArrayList<Container>();
            for (int i = 0; i < args.length; i++) {
                containers.add(loader.getExtension(args[i]));
            }
            logger.info("Use container type(" + Arrays.toString(args) + ") to run dubbo serivce.");

            // ShutdownHook 钩子
            if ("true".equals(System.getProperty(SHUTDOWN_HOOK_KEY))) {
                /**
                 * 有个疑惑，如果不开启 ShutdownHook ，那岂不是 Main 一直等待，JVM 无法结束了？🙂 答案实际是不会，JVM 正常退出时，
                 * 例如使用 kill pid 指定，只要 ShutdownHook 全部执行完成即可退出，无需 Main 函数执行完成。如果没有 ShutdownHook ，那就直接退出。
                 * 那么 Main 的等待唤醒有什么作用？如果123行不进行等待，Main 执行完成，就会触发 JVM 退出，
                 * 导致 Dubbo 服务退出。所以相当于，起到了 JVM 进程常驻的作用。
                 */
                Runtime.getRuntime().addShutdownHook(new Thread("dubbo-container-shutdown-hook") {
                    @Override
                    public void run() {
                        // 关闭容器
                        for (Container container : containers) {
                            try {
                                container.stop();
                                logger.info("Dubbo " + container.getClass().getSimpleName() + " stopped!");
                            } catch (Throwable t) {
                                logger.error(t.getMessage(), t);
                            }
                            try {
                                // 获得 ReentrantLock
                                LOCK.lock();
                                // 唤醒 Main 主线程的等待
                                STOP.signal();
                            } finally {
                                // 释放 ReentrantLock
                                LOCK.unlock();
                            }
                        }
                    }
                });
            }
            // 启动容器
            for (Container container : containers) {
                container.start();
                logger.info("Dubbo " + container.getClass().getSimpleName() + " started!");
            }
            // 输出提示，启动成功
            System.out.println(new SimpleDateFormat("[yyyy-MM-dd HH:mm:ss]").format(new Date()) + " Dubbo service server started!");
        } catch (RuntimeException e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
            System.exit(1);
        }
        try {
            // 获得 ReentrantLock
            LOCK.lock();
            // 释放锁，并且将自己沉睡，等待唤醒
            STOP.await();
        } catch (InterruptedException e) {
            logger.warn("Dubbo service server stopped, interrupted by other thread!", e);
        } finally {
            // 释放 ReentrantLock
            LOCK.unlock();
        }
    }

}
