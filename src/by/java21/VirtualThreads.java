package by.java21;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class VirtualThreads {

    public static void main(String[] args) throws InterruptedException {
        System.out.println(callOneMillioneThreads());

        System.out.println("End of the programm");
    }

    public static int callOneMillioneThreads() throws InterruptedException {
        AtomicInteger a = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<>(1000);

        for (int i = 0; i < 1_000_000_0; i++) {
            threads.add(Thread.startVirtualThread(() -> {
                a.incrementAndGet();
            }));
        }

        // Ждём завершения всех потоков, иначе в строчке 34 можем вернуть неправильный результат пока потоки не успели отработать
        for (Thread t : threads) {
            t.join();    // блокирует текущий(вызывающий) поток пока поток t не завершит работу
        }



        return a.intValue();
    }
}
