package by.java;

import java.util.Comparator;
import java.util.TreeMap;

public class TreeMapWithNullAndComparator {
    static void main() {
        Comparator<String> nullFirstComparator = (s1, s2) -> {
            if (s1 == null && s2 == null) return 0;
            if (s1 == null) return -1; // null всегда меньше любого не-null объекта
            if (s2 == null) return 1;  // любой не-null объект больше null
            return s1.compareTo(s2);   // для остальных - естественный порядок
        };

     // или второй вариант библиотечный   Comparator<String> nullFirstComparator = Comparator.nullsFirst(Comparator.naturalOrder());


        // 2. Инициализируем TreeMap с нашим кастомным компаратором
        var map = new TreeMap<String, String>(nullFirstComparator);
        map.put(null, "qwertty");
        map.put(null, "abc");

        System.out.println(map.size());
        map.forEach((k, v) -> System.out.println("Key: " + k +" Value: " + v) );
    }
}
