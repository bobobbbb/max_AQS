package com.max;

import java.util.concurrent.atomic.AtomicBoolean;

public class MyLock {
    // 使用 AtomicBoolean 实现一个自旋锁，初始为未加锁状态（false）
    AtomicBoolean flag = new AtomicBoolean(false);

    // 获取锁的方法
    public void lock() {
        while (true) {
            // 如果当前为未加锁状态（false），尝试将其设置为加锁状态（true）
            // compareAndSet 是原子操作，避免多线程竞争导致的问题
            if (flag.compareAndSet(false, true)) {
                return; // 加锁成功，返回
            }
            // 否则继续自旋尝试获取锁
        }
    }

    // 释放锁的方法
    public void unlock() {
        while (true) {
            // 如果当前为加锁状态（true），尝试将其设置为未加锁状态（false）
            if (flag.compareAndSet(true, false)) {
                return; // 解锁成功，返回
            }
            // 理论上应该不会出现 CAS 失败，但为了保险起见仍使用循环自旋
        }
    }
}
