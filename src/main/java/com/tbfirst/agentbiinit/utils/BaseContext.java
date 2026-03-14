package com.tbfirst.agentbiinit.utils;

/**
  * 基于 ThreadLocal 实现的当前线程上下文，用于存储当前登录用户 id
 */
public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

}
