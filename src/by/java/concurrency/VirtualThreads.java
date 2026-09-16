package by.java.concurrency;

public class VirtualThreads {

    static void main() {

    }

    private void createVirtualThreadWithoutImmediateStart() {
        var unstartedThread = Thread.ofVirtual()
                .unstarted(() -> System.out.println("Hello world!"));
// Запустить поток позже, когда потребуется
        unstartedThread.start();
    }

    private void createVirtualThreadViaBuilderApi() throws InterruptedException {
        var startedThread = Thread.ofVirtual()
                .start(() -> System.out.println("Hello world!"));
        startedThread.join();
    }
}
