package by.java12;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CollectorTeeingDemo {

    public static void main(String[] args) {
        List<Employee> empList = Arrays.asList(
                new Employee(101, "John", 5049),
                new Employee(102, "Smith", 7000),
                new Employee(103, "William", 9000),
                new Employee(104, "Derek", 2000),
                new Employee(105, "Steve", 5000)
        );

        HashMap<String, Double> empSummary = empList.stream().collect(
                Collectors.teeing(
                        Collectors.averagingDouble(Employee::getEmpSalary),
                        Collectors.counting(),
                        (e1, e2) -> {
                            HashMap<String,
                                    Double> map = new HashMap<String,
                                    Double>();
                            map.put("Total Number of Employees, ", e2.doubleValue());
                            map.put("Average Salary of Employees, ", e1.doubleValue());
                            return map;
                        }
                ));

        for (Map.Entry<String, Double> entry : empSummary.entrySet()) {
            System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue().intValue());
        }
    }
}

class Employee {
    private int empId;
    private String empName;
    private double empSalary;

    public Employee(int empId, String empName, double empSalary) {
        this.empId = empId;
        this.empName = empName;
        this.empSalary = empSalary;
    }

    public int getEmpId() {
        return empId;
    }

    public void setEmpId(int empId) {
        this.empId = empId;
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName;
    }

    public double getEmpSalary() {
        return empSalary;
    }

    public void setEmpSalary(double empSalary) {
        this.empSalary = empSalary;
    }


}
