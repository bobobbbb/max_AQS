package com.max;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class MyLock {
    // 锁状态：0 表示未加锁，1 表示已加锁；支持可重入
    AtomicInteger state = new AtomicInteger(0);

    // 当前持有锁的线程
    Thread own = null;

    // 虚拟头节点（不代表真实线程），用于构建等待队列的链表结构
    AtomicReference<Node> head = new AtomicReference<>(new Node());

    // 队列的尾节点，初始指向头节点
    AtomicReference<Node> tail = new AtomicReference<>(head.get());

    /**
     * 获取锁的主方法：
     * - 无竞争时直接加锁成功；
     * - 否则将当前线程加入等待队列并自旋等待；
     * - 成为头节点时尝试抢锁。
     */
    public void lock() {
        // 快速路径：尝试获取锁
        if (state.get() == 0) {
            if (state.compareAndSet(0, 1)) {
                System.out.println(Thread.currentThread().getName() + " 直接拿到了锁");
                own = Thread.currentThread();
                return;
            }
        } else {
            // 支持可重入锁
            if (own == Thread.currentThread()) {
                state.incrementAndGet();
                return;
            }
        }

        // 构造当前线程对应的节点
        Node curr = new Node();
        curr.thread = Thread.currentThread();

        // 将节点加入等待队列（尾插法，保证顺序）
        while (true) {
            Node currTail = tail.get();
            if (tail.compareAndSet(currTail, curr)) {
                System.out.println(Thread.currentThread().getName() + " 加入到了链表尾部");
                curr.prev = currTail;     // 设置前驱节点
                currTail.next = curr;     // 设置前一个尾节点的后继
                break; // 加入成功，退出循环
            }
            // CAS失败则重试
        }

        // 自旋等待获取锁
        while (true) {
            // 当前线程是队头的下一个，尝试抢锁
            if (curr.prev == head.get() && state.compareAndSet(0, 1)) {
                own = Thread.currentThread();  // 设置锁持有者
                head.set(curr);                // 将当前节点设为头节点
                curr.prev.next = null;         // 清理旧头节点的引用，帮助GC
                curr.prev = null;              // 断链
                System.out.println(Thread.currentThread().getName() + " 被唤醒后拿到了锁");
                return;
            }
            // 不是轮到当前线程，挂起自己
            LockSupport.park();
        }
    }

    /**
     * 解锁方法：
     * - 检查是否为持锁线程；
     * - 可重入情况则递减计数；
     * - 非重入最后一次解锁后，唤醒下一个等待线程。
     */
    public void unlock() {
        // 非锁持有线程调用会抛异常
        if (Thread.currentThread() != this.own) {
            throw new IllegalStateException("当前线程没有锁，不能解锁");
        }

        int i = state.get();
        // 可重入支持：只减计数，不释放锁
        if (i > 1) {
            state.set(i - 1);
            return;
        }

        // 获取当前队列头节点和下一个节点
        Node headNode = head.get();
        Node next = headNode.next;

        // 释放锁状态
        state.set(0);

        // 如果存在后继线程，唤醒它
        if (next != null) {
            System.out.println(Thread.currentThread().getName() + " 唤醒了 " + next.thread.getName());
            LockSupport.unpark(next.thread);
        }
    }

    /**
     * 队列节点类：
     * - 用于构建等待线程的链表；
     * - 每个节点包含线程引用及双向链表指针。
     */
    class Node {
        Node prev;       // 前驱节点
        Node next;       // 后继节点
        Thread thread;   // 当前节点绑定的线程
    }
}
