package by.java.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Thread.sleep;

public class BadLocks {
    final static Object obj1 = new Object();
    final static Object obj2 = new Object();

    static void main() {
        try (ExecutorService executorService = Executors.newFixedThreadPool(2)) {
            executorService.execute(() -> deadLock("One", obj1, obj2));
            executorService.execute(() -> deadLock("Two", obj2, obj1));
        }
    }

    /*
     * Поток One повиснет так как obj2 уже захвачен Вторым потоком, а Второй поток зависнет, так как монитор obj1 захвачен первым потоком.
     */
    public static void deadLock(String name, Object first, Object second )
    {
        try {
            synchronized (first) {
                System.out.println("Thread " + name + " Holding...");
                sleep(200);
                synchronized (second)
                {
                    System.out.println("Thread " + name + " Got it!");
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
