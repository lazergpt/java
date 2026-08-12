package by.java25;

public class ScopedValues {
    static void main() {
        handleRequest();
    }


    private static final ScopedValue<String> USER_CONTEXT = ScopedValue.newInstance();

    public static void handleRequest() {
        String user = "USER";
        ScopedValue.where(USER_CONTEXT, user).run(() -> {
            processRequest(); // внутри можно вызвать USER_CONTEXT.get()
        });
    }

    public static void processRequest()
    {
        System.out.println(USER_CONTEXT.get());
    }
}
