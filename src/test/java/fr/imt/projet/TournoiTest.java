package fr.imt.projet;

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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournoiTest {
    @Test
    void completesAndAutomaticallyPersistsTournamentReport() {
        String gameName = "Test-" + UUID.randomUUID();
        Jeux game = new JeuxSolo(gameName, 3);
        Tournoi tournament = new Tournoi(
                "Cup-" + UUID.randomUUID(), game, LocalDate.of(2026, 11, 1), new BigDecimal("1000"), 2);
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
        referee.qualifierPourJeu(gameName);
        tournament.inscrireArbitre(referee);

        try (GestionnaireTournois manager = new GestionnaireTournois()) {
            manager.ajouterTournoi(tournament);
            manager.lancerProchainTournoi();

            for (Match match : tournament.getMatchs()) {
                tournament.demarrerMatch(match);
                tournament.declarerVainqueur(match, teams.get(0) == match.getEquipe1()
                        || teams.get(0) == match.getEquipe2() ? teams.get(0) : match.getEquipe1());
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
}
