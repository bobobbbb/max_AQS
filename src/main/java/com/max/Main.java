package com.max;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int[] count=new int[]{1000};
        List<Thread> list=new ArrayList<>();
        for(int i=0;i<100;i++){
            Thread thread = new Thread(()->{
                for(int j=0;j<10;j++){
                    count[0]--;
                }
            });
            list.add(thread);
        }
        for(Thread t:list){
            t.start();
        }
        for(Thread t:list){
            t.join();
        }
        System.out.println(count[0]);
    }
}
