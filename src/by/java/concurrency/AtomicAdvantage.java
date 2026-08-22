package by.java.concurrency;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicAdvantage {

    private static long regular;
    private static volatile long vol;
    private static Long obj = 0L;
    private static volatile Long volObj = 0L;
    private static AtomicLong atomicLong = new AtomicLong(0);

    static void main() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        CompletableFuture<Void>[] threads = new CompletableFuture[20];

        for (int i = 0; i < 20; i++) {
            threads[i] = (CompletableFuture.runAsync(() -> {
                for (int j = 0; j < 1_000_000; j++){
                    ++regular;
                    ++vol;
                    obj = ++obj;
                    volObj = ++volObj;
                    atomicLong.incrementAndGet();
                }
            }, executorService));
        }

        CompletableFuture.allOf(threads).join();

        System.out.println(regular);
        System.out.println(vol);
        System.out.println(obj);
        System.out.println(volObj);
        System.out.println(atomicLong);

        executorService.shutdown();
        executorService.awaitTermination(50, TimeUnit.SECONDS);

    }

}
