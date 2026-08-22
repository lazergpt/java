package by.java.concurrency;

import java.util.concurrent.*;

public class ExchangerExample {
    static void main() throws InterruptedException {
        Exchanger<String> exchanger = new Exchanger<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CompletableFuture.runAsync(() -> {
            try {
                String result = exchanger.exchange("INFORMATION");
                System.out.println("Received: " + result);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executor);


        CompletableFuture.runAsync(() -> {
            try {
                String result = exchanger.exchange(" NO INFORMATION");
                System.out.println("Received: " + result);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, executor);

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

    }
}
