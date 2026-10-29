package fr.imt.projet;

import fr.imt.projet.exceptions.TournoiInvalideException;
import fr.imt.projet.jeux.Equipe;
import fr.imt.projet.jeux.Jeux;
import fr.imt.projet.jeux.JeuxSolo;
import fr.imt.projet.jeux.Match;
import fr.imt.projet.personne.Arbitre;
import fr.imt.projet.personne.Coach;
import fr.imt.projet.personne.Joueur;
import fr.imt.projet.rapports.JpaRapportRepository;
import fr.imt.projet.rapports.RapportTournoi;
import fr.imt.projet.tournoi.GestionnaireTournois;
import fr.imt.projet.tournoi.Tournoi;
import fr.imt.projet.tournoi.constructeur.TournoiBuilder;
import fr.imt.projet.tournoi.strategie.FavoriseMieuxClasse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournoiTest {
    private String gameName;
    private Jeux game;

    @BeforeEach
    void initialiser() {
        gameName = "Test-" + UUID.randomUUID();
        game = new JeuxSolo(gameName, 3);
    }

    @Test
    void completesAndAutomaticallyPersistsTournamentReport() {
        Tournoi tournament = new TournoiBuilder()
                .nom("Cup-" + UUID.randomUUID())
                .jeu(game)
                .date(LocalDate.of(2026, 11, 1))
                .budget(new BigDecimal("1000"))
                .arenes(2)
                .build();

        List<Equipe> teams = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            String teamName = "Team " + i;
            Coach coach = new Coach(i, "Coach " + i, "First", "coach" + i, "X", "555", teamName);
            Joueur player = new Joueur(
                    i, "Player " + i, "First", "player" + i, "X", "555", teamName, Joueur.Statut.AMATEUR);
            Equipe team = new Equipe(teamName, coach, List.of(player), 0);
            teams.add(team);
            tournament.inscrireEquipe(team);
        }
        Arbitre referee = new Arbitre(1, "Referee", "First", "ref", "X", "555");
        referee.qualifierPourJeu(game);
        tournament.inscrireArbitre(referee);

        try (GestionnaireTournois manager = new GestionnaireTournois(10, 0.0, 123L, new FavoriseMieuxClasse())) {
            manager.ajouterTournoi(tournament);
            manager.lancerProchainTournoi();

            for (Match match : tournament.getMatchs()) {
                tournament.demarrerMatch(match);
                tournament.declarerVainqueur(match, teams.get(0).equals(match.getEquipe1())
                        || teams.get(0).equals(match.getEquipe2()) ? teams.get(0) : match.getEquipe1());
            }
        }

        RapportTournoi report = tournament.getRapport();
        assertNotNull(report);
        assertEquals(6, report.getMatchsPlanifies());
        assertEquals(6, report.getMatchsJoues());
        assertEquals("Team 1", report.getVainqueur());
        assertTrue(report.estReussi());
        assertTrue(JpaRapportRepository.getInstance().trouverParJeu(gameName).stream()
                .anyMatch(saved -> saved.getNom().equals(tournament.getNom())
                        && saved.getDate().equals(tournament.getDate())));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void testTournoiBuilderArenesValides(int arenes) {
        Tournoi tournoi = new TournoiBuilder()
                .nom("Tournoi-Arenes-" + arenes)
                .jeu(game)
                .arenes(arenes)
                .build();
        assertEquals(arenes, tournoi.getNombreArenes());
    }

    @Test
    void testTournoiDemarrageSansAssezDEquipesLeveException() {
        Tournoi tournoi = new TournoiBuilder()
                .nom("TournoiIncomplet")
                .jeu(game)
                .build();
        assertThrows(TournoiInvalideException.class, tournoi::demarrer);
    }
}
