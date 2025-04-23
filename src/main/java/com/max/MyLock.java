package com.max;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MyLock {
    // 使用 AtomicBoolean 实现一个基本的自旋锁，初始为未加锁状态（false）
    AtomicBoolean flag = new AtomicBoolean(false);

    // 记录当前持有锁的线程
    Thread own = null;

    // 队列头节点，用于构建等待队列（类似 CLH 或 MCS 锁）
    AtomicReference<Node> head = new AtomicReference<>(new Node());

    // 队列尾节点，初始指向 head
    AtomicReference<Node> tail = new AtomicReference<>(head.get());

    // 获取锁的方法
    public void lock() {
        // 尝试直接获取锁，如果成功，设置当前线程为持有者，返回
        if (flag.compareAndSet(false, true)) {
            own = Thread.currentThread();
            return; // 加锁成功，返回
        }

        // 如果加锁失败，构建等待节点
        Node curr = new Node();
        curr.thread = Thread.currentThread();

        // 将当前节点加入等待队列（尾插）
        while (true) {
            Node currTail = tail.get();
            // 再次获取 tail 进行比较并尝试插入
            if (tail.compareAndSet(currTail, tail.get())) {
                curr.prev = currTail;     // 当前节点的前驱指向旧尾节点
                currTail.next = curr;     // 旧尾节点的 next 指向当前节点
                break;                    // 成功加入队列后退出循环
            }
            // 如果 CAS 失败，说明 tail 被其他线程更新了，重新尝试
        }

        // 注：当前代码加入队列后未实现阻塞等待和唤醒逻辑，因此并不完整
    }

    // 释放锁的方法
    public void unlock() {
        // 如果当前线程不是持有锁的线程，抛出异常
        if (Thread.currentThread() != this.own) {
            throw new IllegalStateException("当前线程没有锁，不能解锁");
        }

        // 尝试将加锁状态设为未加锁（false）
        while (true) {
            if (flag.compareAndSet(true, false)) {
                return; // 解锁成功
            }
            // 如果 CAS 失败，自旋重试（理论上不会发生）
        }
    }

    // 队列节点类，用于构建等待线程的链表结构
    class Node {
        Node prev;       // 前一个节点
        Node next;       // 后一个节点
        Thread thread;   // 当前节点对应的线程
    }
}
