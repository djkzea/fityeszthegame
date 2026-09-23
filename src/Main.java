import java.io.IOException;
import java.util.Scanner;

/**
 * Belepesi pont: nyelvvalaszto, majd a jatek.
 * Entry point: language selector, then the game
 */
public class Main {
    public static void main(String[] args) throws IOException {
        Scanner input = new Scanner(System.in);
        Lang.select(input);
        fityesz1_0.jatek(input);
    }
}
