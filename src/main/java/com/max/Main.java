    package com.max;

    import java.util.ArrayList;
    import java.util.List;
    import java.util.concurrent.locks.Lock;
    import java.util.concurrent.locks.ReentrantLock;

    public class Main {
        public static void main(String[] args) throws InterruptedException {
            // 使用一个长度为1的数组来模拟可变的 int 变量，初始值为1000
            int[] count = new int[]{1000};

            // 用于保存所有线程对象的列表
            List<Thread> list = new ArrayList<>();
            MyLock lock=new MyLock();
            // 创建100个线程，每个线程执行10次 count[0]--
            for (int i = 0; i < 100; i++) {

                list.add( new Thread(() -> {
                    //加锁来保证线程安全
                    lock.lock();
                    for (int j = 0; j < 10; j++) {
                        count[0]--;
                    }
                    lock.unlock();
                })); // 将线程添加到列表中
            }

            // 启动所有线程
            for (Thread t : list) {
                t.start();
            }

            // 等待所有线程执行完毕
            for (Thread t : list) {
                t.join();
            }

           //最终结果应该是0
            System.out.println(count[0]);
        }
    }
