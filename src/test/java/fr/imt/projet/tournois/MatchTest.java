package fr.imt.projet.tournois;

import fr.imt.projet.jeux.Jeux;
import fr.imt.projet.jeux.JeuxSolo;
import fr.imt.projet.personnes.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchTest {
    @Test
    void recordsMatchAndAwardsThreePointsToTheWinner() {
        Equipe equipe1 = creerEquipe("Equipe 1");
        Equipe equipe2 = creerEquipe("Equipe 2");
        Arbitre arbitre = creerArbitre("Tennis");
        Jeux jeu = new JeuxSolo("Tennis", 3);

        Match match = new Match(equipe1, equipe2, jeu, arbitre);

        assertSame(equipe1, match.getEquipe1());
        assertSame(equipe2, match.getEquipe2());
        assertSame(jeu, match.getJeu());
        assertSame(arbitre, match.getArbitre());
        assertNull(match.getVainqueur());
        assertEquals(0, arbitre.getMatchsArbitres());

        match.demarrer();
        assertEquals(1, arbitre.getMatchsArbitres());
        match.declarerVainqueur(equipe2);

        assertSame(equipe2, match.getVainqueur());
        assertEquals(0, equipe1.getNombreDePoints());
        assertEquals(3, equipe2.getNombreDePoints());
        assertEquals(1, equipe2.getCoach().getTotalVictoiresEnCarriere());
    }

    @Test
    void rejectsSameTeamAndUnqualifiedReferee() {
        Equipe equipe = creerEquipe("Equipe");
        Arbitre arbitre = creerArbitre("Football");

        assertThrows(
                IllegalArgumentException.class,
                () -> new Match(equipe, equipe, new JeuxSolo("Tennis", 3), arbitre));
        Match matchAvecArbitreNonQualifie =
                new Match(equipe, creerEquipe("Autre équipe"), new JeuxSolo("Tennis", 3), arbitre);
        assertThrows(
                IllegalArgumentException.class,
                matchAvecArbitreNonQualifie::demarrer);
        assertEquals(0, arbitre.getMatchsArbitres());
    }

    @Test
    void rejectsWinnerOutsideMatchAndDoesNotAllowDrawOrSecondWinner() {
        Equipe equipe1 = creerEquipe("Equipe 1");
        Equipe equipe2 = creerEquipe("Equipe 2");
        Match match = new Match(
                equipe1, equipe2, new JeuxSolo("Tennis", 3), creerArbitre("Tennis"));

        match.demarrer();
        assertThrows(IllegalArgumentException.class, () -> match.declarerVainqueur(creerEquipe("Autre")));
        assertThrows(NullPointerException.class, () -> match.declarerVainqueur(null));

        match.declarerVainqueur(equipe1);

        assertThrows(IllegalStateException.class, () -> match.declarerVainqueur(equipe2));
        assertEquals(3, equipe1.getNombreDePoints());
        assertEquals(0, equipe2.getNombreDePoints());
    }

    @ParameterizedTest(name = "{0} équipes, {1} arène(s)")
    @CsvSource({"4, 1", "4, 4", "6, 3", "8, 4"})
    void simulationWavesRespectArenasAndNeverPutATeamTwice(int nombreEquipes, int arenes) {
        Jeux jeu = new JeuxSolo("Tekken", 3);
        List<Equipe> equipes = new ArrayList<>();
        List<Match> matchs = new ArrayList<>();
        Arbitre arbitre = new Arbitre(1, "A", "B", "ref", "F", "0600000000");
        arbitre.qualifierPourJeu(jeu.getNom());
        for (int i = 0; i < nombreEquipes; i++) {
            equipes.add(new Equipe("E" + i,
                    new Coach(i, "Coach", "B", "coach" + i, "F", "0600000000")));
        }
        for (int i = 0; i < nombreEquipes; i++) {
            for (int j = i + 1; j < nombreEquipes; j++) {
                matchs.add(new Match(equipes.get(i), equipes.get(j), jeu, arbitre));
            }
        }

        List<List<Match>> vagues = SimulateurTournoi.planifierVagues(matchs, arenes);

        assertEquals(matchs.size(), vagues.stream().mapToInt(List::size).sum());
        for (List<Match> vague : vagues) {
            assertTrue(vague.size() <= arenes);
            long equipesDistinctes = vague.stream()
                    .flatMap(m -> java.util.stream.Stream.of(m.getEquipe1(), m.getEquipe2()))
                    .distinct().count();
            assertEquals(vague.size() * 2L, equipesDistinctes);
        }
    }

    private static Equipe creerEquipe(String nom) {
        Coach coach = new Coach(1, "Coach", "Prenom", "pseudo", "genre", "telephone");
        return new Equipe(nom, coach);
    }

    private static Arbitre creerArbitre(String jeu) {
        Arbitre arbitre = new Arbitre(1, "Arbitre", "Prenom", "pseudo", "genre", "telephone");
        arbitre.qualifierPourJeu(jeu);
        return arbitre;
    }
}
