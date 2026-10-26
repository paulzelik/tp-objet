package fr.imt.projet;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTest {
    @Test
    void printsGreeting() {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try (PrintStream testOutput = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(testOutput);
            Main.main(new String[0]);
        } finally {
            System.setOut(originalOutput);
        }

        assertEquals("Bonjour, Maven !", output.toString(StandardCharsets.UTF_8).trim());
    }
}