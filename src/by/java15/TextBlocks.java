package by.java15;

public class TextBlocks {
    static void main() {
        String textBlock = """
                        
                         asdasd
                        static void main() {
                        \\\\\\
                        \\ttext
                        String testString = "12345!@#$%^&*qwerty1234677";
                        System.out.println(moveSpecCharactersToTheEnd(testString));
                        System.out.println(moveSpecCharactersToTheEnd1(testString));
                        System.out.println(moveSpecCharactersToTheEnd2(testString));
                    }""";

        //test();
        System.out.println(textBlock);
        System.out.println(textBlock.translateEscapes());
    }

}
