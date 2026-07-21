package by.java;

import java.util.HashMap;
import java.util.Map;

public class CheckHashMapStuff {
    static void main() {

        Map<String,String> map = new HashMap<>();

        map.put("I have a common prefixDB", "zero");
        map.put("I have a common prefixCa", "one"); // как оно работает с одинаковыми хешами ?

        System.out.println(map); //generates 310265957 as hashcode
    }
}
