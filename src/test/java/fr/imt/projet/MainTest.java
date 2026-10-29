package fr.imt.projet;

import fr.imt.projet.rapports.JpaRapportRepository;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {
    @Test
    @Timeout(30)
    void demoRunsAFullSeasonAndArchivesTheReports() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;

        try (PrintStream testOutput = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setOut(testOutput);
            Main.demo(JpaRapportRepository.enMemoire("demo"));
        } finally {
            System.setOut(originalOutput);
        }

        String sortie = output.toString(StandardCharsets.UTF_8);
        assertTrue(sortie.contains("Tournois planifiés (le plus proche d'abord)"));
        assertTrue(sortie.contains("refusé : lancer la Coupe d'Hiver en même temps"));
        assertTrue(sortie.contains("#3 Coupe d'Hiver"), "trois rapports archivés en base");
        assertTrue(sortie.contains("Gestionnaire fermé proprement"));
    }
}
