package by.java11;

public class StringApiDemo {

    public static void main(String[] args) {

        System.out.println("Stripping the String==>");
        String originalString="\u2001 \t BPB Publications \t \u2002 ";
        System.out.println("Stripped String="+originalString.strip());
        System.out.println("Trimmed String ="+(originalString.trim())+"\n");


        System.out.println("Stripping leading and training spaces =>");
        System.out.println("Leading Strip  ="+originalString.stripLeading());
        System.out.println("Trailing Strip ="+originalString.stripTrailing()+"\n");

        System.out.println("Checking the blank String=>");
        String blankString="\u2001";
        System.out.println("Is String blank ="+blankString.isBlank()+"\n");

        System.out.println("Generating lines from the String using lines() ==>");
        String text="This book contains recipes for Java.\nIt covers the concept from Java 9 to 18";
        text.lines().forEach((line)->System.out.println(line));
        System.out.println("\n");

        System.out.println("Repeating the String using repeat()==>");
        String bookName="Java 9+ Recipes!";
        System.out.println(bookName.repeat(5));
    }

}
