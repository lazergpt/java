package by.java11;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;


public class ReflectionApiDemo {

    public static void main(String[] args) {
        Car car=new Car("Diesel");
        Car.Engine engine=car.new Engine();
        System.out.println("Host name for Engine class:-"+hostName(engine));
        if(checkNestMate(engine))
            System.out.println("Engine is nestmate of Car");
        else
            System.out.println("Engine is not nestmate of Car");
        Set<String> nestMembers=findNestMembers(engine);
        System.out.println("Nestmembers of Engine are: ==>");
        int counter=0;
        for(String member:nestMembers)
            System.out.println(++counter+ ". "+member+" ");
    }
    public static String hostName(Object o)
    {
        return o.getClass().getNestHost().getName();

    }

    public static boolean checkNestMate(Object o) {
        return o.getClass().isNestmateOf(Car.class);
       // return Car.class.isNestmateOf(o.getClass()); // результат тот же что и у строчки выше
    }

    public static Set<String> findNestMembers(Object o){
        return Arrays.stream(o.getClass().getNestMembers()).map(Class::getName)
                .collect(Collectors.toSet());
    }

}

class Car {
    private static String carType;
    public Car(String type) {
        carType = type;
    }


    class Engine {
        String engineType;
        void setEngine() {
            if(carType.equals("disel"))
                this.engineType="customized";
            else
                this.engineType="standard";
        }

        String getEngineType() {
            return this.engineType;
        }
    }

    class SteeringWheel {


    }

}
