package by.java14;

public class NewSwitch {
    static void main() {
        switchWithAndVariable(2);
        System.out.println(switchNew(3));
    }

    private static void switchWithAndVariable(int a) {
        switch (a) {
            case 1:
                int b = 1;
                System.out.println(b);
                break;
            case 2:
                b = 2;
                System.out.println(b);
        }
    }

    private static int switchNew(int a) {
        return switch (a) {
            case 1 -> {
                yield 1;
            }
            case 2 -> {
                yield 2;
            }
            default -> 3;
        };
    }
}
