package by.java17;

import java.util.Optional;

public class SwitchPatternMatching {

    static void main() {
        switchPatternMatching(null); // если в switch есть обработчик под null, тогда выполнится он, иначе будет NPE
        switchPatternMatching(Optional.empty());
        switchPatternMatching(19);
        switchPatternMatching(19L);
        switchPatternMatching("19");

    }

    private static void switchPatternMatching(Object a)
    {
        switch (a) {
            case Integer b -> System.out.println("Integer - " + b);
            case Long c when c == 19L -> System.out.println("Long guarded - " + c); // guarded pattern. java проверяет условия сверху в низ, т.е. желательно чтобы сверху был более специфичный
            case Long c -> System.out.println("Long - " + c);
            case String d -> System.out.println("String - " + d);
            case null -> System.out.println("NULL");
            default -> System.out.println("Unknown type");
        }
    }
}