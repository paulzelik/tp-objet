package fr.imt.projet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Builder, stratégies, observateurs, listes demandées par le sujet et persistance JPA. */
class GestionnaireListesTest {
    private final Jeux jeu = new JeuxSolo("Tekken", 3);
    private JpaRapportRepository rapports;
    private GestionnaireTournois gestionnaire;
    private long id = 100;

    @BeforeEach
    void ouvrir() {
        rapports = JpaRapportRepository.enMemoire("listes" + System.nanoTime());
        gestionnaire = new GestionnaireTournois(5, 0.0, StrategieVainqueur.toujoursMieuxClassee(), 1, rapports);
    }

    @AfterEach
    void fermer() {
        gestionnaire.close(); // ferme aussi le dépôt
    }

    private Tournoi planifier(String nom, LocalDate date, long budget) {
        List<Equipe> equipes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Equipe equipe = new Equipe(nom + " " + i, EquipeTest.coach(id++), EquipeTest.joueur(id++));
            equipes.add(gestionnaire.ajouterEquipe(equipe));
        }
        Arbitre arbitre = new Arbitre(id++, "Arbitre", "Prenom", "ref" + id, "F", "0600000000");
        arbitre.qualifierPourJeu(jeu.getNom());
        Tournoi tournoi = Tournoi.builder().nom(nom).jeu(jeu).date(date).budget(budget).arenes(2)
                .equipes(equipes).arbitre(arbitre).build();
        gestionnaire.ajouterTournoi(tournoi);
        return tournoi;
    }

    @Test
    void theBuilderReportsAllErrorsAtOnce() {
        IllegalArgumentException erreur = assertThrows(IllegalArgumentException.class,
                () -> Tournoi.builder().arenes(9).build());
        assertTrue(erreur.getMessage().contains("nom"));
        assertTrue(erreur.getMessage().contains("jeu"));
        assertTrue(erreur.getMessage().contains("arènes"));
    }

    @Test
    void plannedTournamentsAreListedNearestFirst() {
        Tournoi decembre = planifier("Décembre", LocalDate.of(2026, 12, 1), 100);
        Tournoi novembre = planifier("Novembre", LocalDate.of(2026, 11, 1), 100);
        Tournoi janvier = planifier("Janvier", LocalDate.of(2027, 1, 1), 100);

        assertEquals(List.of(novembre, decembre, janvier), gestionnaire.getTournoisPlanifies());
    }

    @Test
    @Timeout(10)
    void finishedTournamentsAreListedByDecreasingBudgetAndReportsAreArchived() throws Exception {
        Tournoi petit = planifier("Petit", LocalDate.of(2026, 11, 1), 100);
        Tournoi gros = planifier("Gros", LocalDate.of(2026, 11, 2), 9_000);

        gestionnaire.simulerProchainTournoi().get(5, TimeUnit.SECONDS);
        gestionnaire.simulerProchainTournoi().get(5, TimeUnit.SECONDS);

        assertEquals(List.of(gros, petit), gestionnaire.getTournoisTermines());
        assertEquals(2, gestionnaire.getRapportsArchives().size());
        assertEquals(2, gestionnaire.getRapportsArchives("tekken").size());
        assertTrue(gestionnaire.getRapportsArchives("Valorant").isEmpty());
    }

    @Test
    @Timeout(10)
    void rankingIsSortedByPointsThenNameAndTeamsGetTheirRank() throws Exception {
        Tournoi tournoi = planifier("Open", LocalDate.of(2026, 11, 1), 100);
        gestionnaire.simulerProchainTournoi().get(5, TimeUnit.SECONDS);

        List<GestionnaireTournois.LigneClassement> classement = new ArrayList<>(gestionnaire.getClassementEquipes());
        for (int i = 1; i < classement.size(); i++) {
            assertTrue(classement.get(i - 1).compareTo(classement.get(i)) < 0);
        }
        Equipe premiere = gestionnaire.getEquipe(classement.get(0).equipe()).orElseThrow();
        assertEquals(1, premiere.getClassement());
        assertEquals(tournoi.getRapport().getVainqueur(), premiere.getNom());
    }

    @Test
    void matchesAreGroupedByWinningTeam() {
        Tournoi tournoi = planifier("Open", LocalDate.of(2026, 11, 1), 100);
        gestionnaire.lancerProchainTournoi();
        for (Match match : tournoi.getMatchs()) {
            tournoi.demarrerMatch(match);
            tournoi.declarerVainqueur(match, match.getEquipe1());
        }

        SortedMap<Equipe, List<Match>> parVainqueur = tournoi.getMatchsParEquipeGagnante();
        assertEquals(6, parVainqueur.values().stream().mapToInt(List::size).sum());
        parVainqueur.forEach((equipe, matchs) -> matchs.forEach(m -> assertSame(equipe, m.getVainqueur())));
        assertEquals(3, gestionnaire.getMatchsDe(tournoi.getEquipes().get(0)).size());
    }

    @Test
    void refereesAreOccupiedDuringATournamentThenAvailable() {
        Tournoi tournoi = planifier("Open", LocalDate.of(2026, 11, 1), 100);
        Arbitre arbitre = tournoi.getArbitres().get(0);
        assertEquals(List.of(arbitre), gestionnaire.getArbitresDisponibles());

        gestionnaire.lancerProchainTournoi();
        assertEquals(List.of(arbitre), gestionnaire.getArbitresOccupes());
        assertTrue(gestionnaire.getArbitresDisponibles().isEmpty());
    }

    @Test
    void aTeamCannotPlayTwoMatchesAtTheSameTime() {
        Tournoi tournoi = planifier("Open", LocalDate.of(2026, 11, 1), 100);
        gestionnaire.lancerProchainTournoi();
        List<Match> matchs = tournoi.getMatchs();
        tournoi.demarrerMatch(matchs.get(0));
        Match memeEquipe = matchs.stream().filter(m -> m != matchs.get(0) && m.oppose(matchs.get(0).getEquipe1()))
                .findFirst().orElseThrow();
        assertThrows(IllegalStateException.class, () -> tournoi.demarrerMatch(memeEquipe));
    }

    @Test
    void observersAreNotifiedAndAFaultyObserverDoesNotStopTheTournament() {
        Tournoi tournoi = planifier("Open", LocalDate.of(2026, 11, 1), 100);
        List<String> evenements = new ArrayList<>();
        tournoi.ajouterObservateur(new ObservateurTournoi() {
            @Override
            public void matchTermine(Tournoi t, Match m) {
                evenements.add("match");
            }

            @Override
            public void incidentTechnique(Tournoi t, Match m) {
                evenements.add("incident");
            }

            @Override
            public void tournoiTermine(Tournoi t, RapportTournoi r) {
                evenements.add("fin");
            }
        });
        tournoi.ajouterObservateur(new ObservateurTournoi() {
            @Override
            public void matchTermine(Tournoi t, Match m) {
                throw new IllegalStateException("observateur défaillant");
            }
        });
        gestionnaire.lancerProchainTournoi();
        List<Match> matchs = tournoi.getMatchs();
        for (int i = 0; i < matchs.size(); i++) {
            tournoi.demarrerMatch(matchs.get(i));
            if (i == 0) {
                tournoi.signalerIncidentTechnique(matchs.get(i));
            } else {
                tournoi.declarerVainqueur(matchs.get(i), matchs.get(i).getEquipe2());
            }
        }
        assertEquals(List.of("incident", "match", "match", "match", "match", "match", "fin"), evenements);
        assertEquals(Tournoi.Statut.TERMINE, tournoi.getStatut());
    }

    @ParameterizedTest(name = "rang {0} contre rang {1} → {2}")
    @CsvSource({"1, 3, 0.75", "3, 1, 0.25", "2, 2, 0.5", "0, 0, 0.5"})
    void theWeightedDrawFavoursTheBestRankedTeam(int rang1, int rang2, double probabilite) {
        Equipe equipe1 = new Equipe("Un", EquipeTest.coach(1));
        Equipe equipe2 = new Equipe("Deux", EquipeTest.coach(2));
        if (rang1 > 0) {
            equipe1.classer(rang1);
        }
        if (rang2 > 0) {
            equipe2.classer(rang2);
        }
        assertEquals(probabilite, StrategieVainqueur.probabiliteVictoire(equipe1, equipe2), 1e-9);
    }

    @Test
    void theDeterministicStrategyAlwaysPicksTheBestRankedTeam() {
        Equipe premiere = new Equipe("Première", EquipeTest.coach(1));
        Equipe derniere = new Equipe("Dernière", EquipeTest.coach(2));
        premiere.classer(1);
        derniere.classer(8);
        assertSame(premiere, StrategieVainqueur.toujoursMieuxClassee().designerVainqueur(derniere, premiere, new Random()));
    }

    @ParameterizedTest(name = "{0} équipes, {1} arène(s)")
    @CsvSource({"4, 1", "4, 4", "6, 3", "8, 4"})
    void simulationWavesRespectArenasAndNeverPutATeamTwice(int nombreEquipes, int arenes) {
        List<Equipe> equipes = new ArrayList<>();
        List<Match> matchs = new ArrayList<>();
        Arbitre arbitre = new Arbitre(1, "A", "B", "ref", "F", "0600000000");
        arbitre.qualifierPourJeu(jeu.getNom());
        for (int i = 0; i < nombreEquipes; i++) {
            equipes.add(new Equipe("E" + i, EquipeTest.coach(i)));
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

    @Test
    void aTeamNameIsUnique() {
        gestionnaire.ajouterEquipe(new Equipe("Kernel Panic", EquipeTest.coach(1)));
        assertThrows(IllegalArgumentException.class,
                () -> gestionnaire.ajouterEquipe(new Equipe("Kernel Panic", EquipeTest.coach(2))));
        assertTrue(gestionnaire.getEquipe("Kernel Panic").isPresent());
        assertFalse(gestionnaire.getEquipe("Inconnue").isPresent());
        assertEquals(Set.of(), Set.copyOf(gestionnaire.getTournoisEnCours()));
    }
}
