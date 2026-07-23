package by.java12;

import java.util.concurrent.CompletableFuture;

public class ExceptionallyAsync {
    public void doSomeTask(int value) {
        System.out.println("Working with exceptionallyAsync without Executor==>");
        double data=100;
        CompletableFuture.supplyAsync(() -> {
            System.out.println("supplyAsync() executed by:" + Thread.currentThread().getName());
            Double ans = data / value;
            return ans;
        }).exceptionallyAsync(e -> {
            System.out.println("exceptionallyAsync executed by::" + Thread.currentThread().getName());
            System.out.println("Exception occured :" + e.getMessage());
            return Double.NaN;
        }).thenAcceptAsync(s -> {
            System.out.println("acceptAsync() executed by:" + Thread.currentThread().getName());
            System.out.println("Answer from AcceptAsync()=>" + s);

        });
    }

    static void main() {
        System.out.println("Main executed by::" + Thread.currentThread().getName());
        new ExceptionallyAsync().doSomeTask(0);
    }
}