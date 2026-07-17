package by.java10;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamingTestJava10 {
    static void main() {
        createUnmodifiableMap();
    }

    static void createUnmodifiableMap() {
        Employee e1 = new Employee(101, "First");
        Employee e2 = new Employee(101, "Second");
        Map<Integer, Object> empMap = Stream.of(e1, e2)
                .collect(Collectors.toMap(i -> i.id(), i -> i, (x, y) -> "Result of merge: " + x + ", " + y));
//        Map<Integer, Object> empMap2 = Stream.of(e1, e2)
//                .collect(Collectors.toMap(i -> 0, i -> i));
//Exception in thread "main" java.lang.IllegalStateException: Duplicate key 0 (attempted merging values Employee[id=101, name=First] and Employee[id=101, name=Second])
        Optional.empty().ifPresentOrElse(i -> {}, () -> System.out.println("test"));
        Optional.empty().orElseThrow();
        System.out.println(empMap);
    }
}

record Employee(int id, String name){}
