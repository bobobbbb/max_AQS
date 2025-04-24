package com.max;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

public class MyLock {
    // 锁状态标志，使用AtomicBoolean保证原子性操作
    // false表示锁未被占用，true表示锁已被占用
    AtomicBoolean flag = new AtomicBoolean(false);

    // 记录当前持有锁的线程，用于检查解锁时的线程合法性
    Thread own = null;

    // 等待队列的头节点（虚拟节点，不存储实际线程）
    // 使用AtomicReference保证对头节点的原子操作
    AtomicReference<Node> head = new AtomicReference<>(new Node());

    // 等待队列的尾节点，初始时指向头节点
    // 使用AtomicReference保证对尾节点的原子操作
    AtomicReference<Node> tail = new AtomicReference<>(head.get());

    /**
     * 获取锁的方法
     * 1. 首先尝试快速获取锁（无竞争情况）
     * 2. 如果失败，则创建节点加入等待队列
     * 3. 在队列中自旋等待直到成为队列首节点并成功获取锁
     */
    public void lock() {
//        // 尝试快速获取锁（无竞争情况）
//        if (flag.compareAndSet(false, true)) {
//            System.out.println(Thread.currentThread().getName()+"直接拿到了锁");
//            own = Thread.currentThread();
//            return; // 加锁成功，直接返回
//        }

        // 创建代表当前线程的节点
        Node curr = new Node();
        curr.thread = Thread.currentThread();

        // 将当前节点加入等待队列尾部（使用CAS保证线程安全）
        while (true) {
            Node currTail = tail.get();
            // 尝试将当前节点设置为新的尾节点
            if (tail.compareAndSet(currTail, curr)) {
                System.out.println(Thread.currentThread().getName()+"加入到了链表尾部");
                // 设置前驱后继关系
                curr.prev = currTail;     // 当前节点的前驱指向旧尾节点
                currTail.next = curr;     // 旧尾节点的后继指向当前节点
                break;                    // 成功加入队列后退出循环
            }
            // CAS失败说明有其他线程修改了tail，需要重试
        }

        // 在队列中等待获取锁
        while(true) {
            // 检查是否轮到当前线程（前驱节点是头节点）
            // 并且尝试获取锁
            if (curr.prev == head.get() && flag.compareAndSet(false, true)) {
                // 获取锁成功后的处理
                own = Thread.currentThread();  // 记录锁持有者
                head.set(curr);              // 将当前节点设为新的头节点
                curr.prev.next = null;        // 断开与前头节点的连接
                curr.prev = null;            // 清除前驱引用
                System.out.println(Thread.currentThread().getName()+"被唤醒后拿到了锁");
                return;                      // 成功获取锁，退出方法
            }
            // 如果还不该当前线程获取锁，则挂起当前线程
            LockSupport.park();
        }
    }

    /**
     * 释放锁的方法
     * 1. 检查调用线程是否是锁的持有者
     * 2. 释放锁状态
     * 3. 唤醒等待队列中的下一个线程（如果存在）
     */
    public void unlock() {
        // 检查当前线程是否是锁的持有者
        if (Thread.currentThread() != this.own) {
            throw new IllegalStateException("当前线程没有锁，不能解锁");
        }

        // 获取当前头节点和其后继节点
        Node headNode = head.get();
        Node next = headNode.next;

        // 释放锁状态
        flag.set(false);

        // 如果存在等待线程，则唤醒队列中的下一个线程
        if (next != null) {
            System.out.println(Thread.currentThread().getName()+"唤醒了"+next.thread.getName());
            LockSupport.unpark(next.thread);
        }
    }

    /**
     * 内部节点类，用于构建等待队列
     * 每个节点代表一个等待获取锁的线程
     */
    class Node {
        Node prev;       // 前驱节点
        Node next;       // 后继节点
        Thread thread;   // 节点关联的线程
    }
}
