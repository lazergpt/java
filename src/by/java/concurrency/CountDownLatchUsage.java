package by.java.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CountDownLatchUsage {
    public static final Integer N = 10;

    // пример старта всех потоков одновременно
    static void main() throws InterruptedException {
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(N);
        try (ExecutorService executorService = Executors.newFixedThreadPool(N)) {
            for (int i = 0; i < N; i++) {
                executorService.submit(() -> {
                    try {
                        startSignal.await();          // ждём общего старта
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    doWork();
                    doneSignal.countDown();       // сообщаем, что закончили
                });
            }

            startSignal.countDown();              // все стартуют одновременно
            doneSignal.await();
        }

    }

    static void doWork()
    {
        System.out.println("Doing work");
    }

}

