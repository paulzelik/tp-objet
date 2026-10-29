package fr.imt.projet;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.imt.projet.rapports.JpaRapportRepository;
import fr.imt.projet.rapports.RapportTournoi;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RapportRepositoryTest {
    private JpaRapportRepository rapports;

    @BeforeEach
    void ouvrirBaseDeTest() {
        rapports = JpaRapportRepository.enMemoire("rapports" + System.nanoTime());
    }

    @AfterEach
    void fermerBaseDeTest() {
        rapports.close();
    }

    @Test
    void rapportsPersistesSontRetrouvesTousOuParJeu() {
        RapportTournoi tennis = rapport("Coupe Tennis", "Tennis", LocalDate.of(2026, 11, 1));
        RapportTournoi pong = rapport("Open Pong", "Pong", LocalDate.of(2026, 11, 2));
        rapports.sauvegarder(tennis);
        rapports.sauvegarder(pong);

        List<RapportTournoi> tous = rapports.trouverTous();
        List<RapportTournoi> tennisArchives = rapports.trouverParJeu("tEnNiS");
        assertEquals(2, tous.size());
        assertEquals("Coupe Tennis", tous.get(0).getNom());
        assertEquals("Open Pong", tous.get(1).getNom());
        assertEquals(1, tennisArchives.size());
        assertEquals("Tennis", tennisArchives.get(0).getJeu());
    }

    private static RapportTournoi rapport(String nom, String jeu, LocalDate date) {
        return new RapportTournoi(nom, date, jeu, 6, 6, "Equipe 1", true);
    }
}
