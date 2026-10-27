package fr.imt.projet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournoiTest {
    @Test
    void createsRoundRobinScheduleAndAutomaticallyProducesSuccessfulReport() {
        Jeux jeu = new JeuxSolo("Tennis", 3);
        Tournoi tournoi = creerTournoi("Coupe", jeu, LocalDate.of(2026, 11, 1), 4);
        List<Equipe> equipes = inscrireQuatreEquipes(tournoi);
        Arbitre arbitre = inscrireArbitre(tournoi, jeu);

        GestionnaireTournois gestionnaire = new GestionnaireTournois();
        gestionnaire.ajouterTournoi(tournoi);
        assertSame(tournoi, gestionnaire.lancerProchainTournoi());
        assertEquals(Tournoi.Statut.EN_COURS, tournoi.getStatut());
        assertEquals(6, tournoi.getMatchs().size());

        List<Match> matchs = tournoi.getMatchs();
        for (int i = 0; i < 4; i++) {
            tournoi.demarrerMatch(matchs.get(i));
        }
        assertThrows(IllegalStateException.class, () -> tournoi.demarrerMatch(matchs.get(4)));

        tournoi.declarerVainqueur(matchs.get(0), equipes.get(0));
        tournoi.declarerVainqueur(matchs.get(1), equipes.get(0));
        tournoi.declarerVainqueur(matchs.get(2), equipes.get(0));
        tournoi.declarerVainqueur(matchs.get(3), equipes.get(1));
        tournoi.demarrerMatch(matchs.get(4));
        tournoi.demarrerMatch(matchs.get(5));
        tournoi.declarerVainqueur(matchs.get(4), equipes.get(1));
        tournoi.declarerVainqueur(matchs.get(5), equipes.get(2));

        assertEquals(Tournoi.Statut.TERMINE, tournoi.getStatut());
        assertEquals(9, equipes.get(0).getNombreDePoints());
        assertEquals(3, equipes.get(0).getCoach().getTotalVictoiresEnCarriere());
        assertEquals(Joueur.Statut.PRO, equipes.get(0).getJoueurs()[0].getStatut());
        assertEquals(6, arbitre.getMatchsArbitres());
        assertEquals("Coupe", tournoi.getRapport().getNom());
        assertEquals("Tennis", tournoi.getRapport().getJeu());
        assertEquals(6, tournoi.getRapport().getMatchsJoues());
        assertEquals("Equipe 1", tournoi.getRapport().getVainqueur());
        assertTrue(tournoi.getRapport().estReussi());
    }

    @Test
    void reportsSuccessWhenAtLeastSeventyPercentOfMatchesArePlayed() {
        Jeux jeu = new JeuxSolo("Tennis", 3);
        Tournoi tournoi = creerTournoi("Coupe", jeu, LocalDate.of(2026, 11, 1), 4);
        List<Equipe> equipes = inscrireQuatreEquipes(tournoi);
        inscrireArbitre(tournoi, jeu);
        GestionnaireTournois gestionnaire = new GestionnaireTournois();
        gestionnaire.ajouterTournoi(tournoi);
        gestionnaire.lancerProchainTournoi();

        for (int i = 0; i < tournoi.getMatchs().size(); i++) {
            Match match = tournoi.getMatchs().get(i);
            tournoi.demarrerMatch(match);
            if (i < 5) {
                tournoi.declarerVainqueur(match, equipes.get(0) == match.getEquipe1()
                        || equipes.get(0) == match.getEquipe2() ? equipes.get(0) : match.getEquipe1());
            } else {
                tournoi.signalerIncidentTechnique(match);
            }
        }

        assertEquals(Tournoi.Statut.TERMINE, tournoi.getStatut());
        assertTrue(tournoi.getRapport().estReussi());
        assertEquals(5, tournoi.getRapport().getMatchsJoues());
    }

    @Test
    void reportsFailureWhenFewerThanSeventyPercentOfMatchesArePlayed() {
        Jeux jeu = new JeuxSolo("Tennis", 3);
        Tournoi tournoi = creerTournoi("Coupe", jeu, LocalDate.of(2026, 11, 1), 4);
        List<Equipe> equipes = inscrireQuatreEquipes(tournoi);
        inscrireArbitre(tournoi, jeu);
        GestionnaireTournois gestionnaire = new GestionnaireTournois();
        gestionnaire.ajouterTournoi(tournoi);
        gestionnaire.lancerProchainTournoi();

        for (int i = 0; i < tournoi.getMatchs().size(); i++) {
            Match match = tournoi.getMatchs().get(i);
            tournoi.demarrerMatch(match);
            if (i < 4) {
                Equipe vainqueur = equipes.get(0) == match.getEquipe1() || equipes.get(0) == match.getEquipe2()
                        ? equipes.get(0)
                        : match.getEquipe1();
                tournoi.declarerVainqueur(match, vainqueur);
            } else {
                tournoi.signalerIncidentTechnique(match);
            }
        }

        assertFalse(tournoi.getRapport().estReussi());
        assertEquals(4, tournoi.getRapport().getMatchsJoues());
    }

    @Test
    void startsTheNearestTournamentAndLimitsConcurrentTournamentsToThree() {
        Jeux jeu = new JeuxSolo("Tennis", 3);
        GestionnaireTournois gestionnaire = new GestionnaireTournois();
        Tournoi plusTard = tournoiValide("Plus tard", jeu, LocalDate.of(2026, 12, 1));
        Tournoi lePlusProche = tournoiValide("Le plus proche", jeu, LocalDate.of(2026, 11, 1));
        Tournoi troisieme = tournoiValide("Troisième", jeu, LocalDate.of(2026, 11, 2));
        Tournoi quatrieme = tournoiValide("Quatrième", jeu, LocalDate.of(2026, 11, 3));
        gestionnaire.ajouterTournoi(plusTard);
        gestionnaire.ajouterTournoi(lePlusProche);
        gestionnaire.ajouterTournoi(troisieme);
        gestionnaire.ajouterTournoi(quatrieme);

        assertSame(lePlusProche, gestionnaire.lancerProchainTournoi());
        assertSame(troisieme, gestionnaire.lancerProchainTournoi());
        assertSame(quatrieme, gestionnaire.lancerProchainTournoi());
        assertThrows(IllegalStateException.class, gestionnaire::lancerProchainTournoi);
        assertEquals(Tournoi.Statut.PLANIFIE, plusTard.getStatut());
    }

    @Test
    void refusesToStartTournamentWithTeamAlreadyActiveElsewhere() {
        Jeux jeu = new JeuxSolo("Tennis", 3);
        Equipe equipePartagee = creerEquipe("Partagée");
        Tournoi premier = tournoiValide("Premier", jeu, LocalDate.of(2026, 11, 1));
        inscrireEquipe(premier, equipePartagee);
        Tournoi second = tournoiValide("Second", jeu, LocalDate.of(2026, 11, 2));
        inscrireEquipe(second, equipePartagee);
        GestionnaireTournois gestionnaire = new GestionnaireTournois();
        gestionnaire.ajouterTournoi(premier);
        gestionnaire.ajouterTournoi(second);

        gestionnaire.lancerProchainTournoi();
        assertThrows(IllegalStateException.class, gestionnaire::lancerProchainTournoi);
        assertEquals(Tournoi.Statut.PLANIFIE, second.getStatut());
    }

    @Test
    void requiresFourTeamsAndCorrectRosterBeforeStarting() {
        Jeux jeu = new JeuxEquipe("Basketball", 3, 3);
        Tournoi tournoi = creerTournoi("Coupe", jeu, LocalDate.of(2026, 11, 1), 1);
        tournoi.inscrireEquipe(creerEquipe("Equipe 1"));
        inscrireArbitre(tournoi, jeu);
        GestionnaireTournois gestionnaire = new GestionnaireTournois();
        gestionnaire.ajouterTournoi(tournoi);

        assertThrows(IllegalStateException.class, gestionnaire::lancerProchainTournoi);
        assertEquals(Tournoi.Statut.PLANIFIE, tournoi.getStatut());
        assertThrows(
                IllegalArgumentException.class,
                () -> creerTournoi("Invalide", jeu, LocalDate.now(), 5));
    }

    @Test
    void refusesToStartWhenARegisteredTeamDoesNotHaveTheRequiredRosterSize() {
        Jeux jeu = new JeuxEquipe("Basketball", 3, 3);
        Tournoi tournoi = creerTournoi("Coupe", jeu, LocalDate.of(2026, 11, 1), 2);
        inscrireQuatreEquipes(tournoi);
        inscrireArbitre(tournoi, jeu);
        GestionnaireTournois gestionnaire = new GestionnaireTournois();
        gestionnaire.ajouterTournoi(tournoi);

        assertThrows(IllegalStateException.class, gestionnaire::lancerProchainTournoi);
        assertEquals(Tournoi.Statut.PLANIFIE, tournoi.getStatut());
    }

    @Test
    void simulatesTournamentAsynchronouslyAndPublishesReportAfterCompletion() throws Exception {
        Jeux jeu = new JeuxSolo("Tennis", 3);
        Tournoi tournoi = tournoiValide("Simulation", jeu, LocalDate.of(2026, 11, 1));

        try (GestionnaireTournois gestionnaire = new GestionnaireTournois(100, 0.0, 42)) {
            gestionnaire.ajouterTournoi(tournoi);
            Future<RapportTournoi> resultat = gestionnaire.simulerProchainTournoi();

            assertEquals(Tournoi.Statut.EN_COURS, tournoi.getStatut());
            assertThrows(TimeoutException.class, () -> resultat.get(1, TimeUnit.MILLISECONDS));

            RapportTournoi rapport = resultat.get(5, TimeUnit.SECONDS);

            assertEquals(Tournoi.Statut.TERMINE, tournoi.getStatut());
            assertSame(rapport, tournoi.getRapport());
            assertEquals(6, rapport.getMatchsJoues());
            assertTrue(rapport.estReussi());
            assertTrue(tournoi.getMatchs().stream().allMatch(m -> m.getStatut() == Match.Statut.JOUE));
        }
    }

    @Test
    void concurrentSimulationRequestsNeverExceedThreeRunningTournaments() throws Exception {
        Jeux jeu = new JeuxSolo("Tennis", 3);
        ExecutorService lanceurs = Executors.newFixedThreadPool(4);
        CountDownLatch depart = new CountDownLatch(1);
        try (GestionnaireTournois gestionnaire = new GestionnaireTournois(300, 0.0, 7)) {
            for (int i = 0; i < 4; i++) {
                gestionnaire.ajouterTournoi(
                        tournoiValide("Tournoi " + i, jeu, LocalDate.of(2026, 11, i + 1)));
            }

            List<Future<Future<RapportTournoi>>> demandes = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                demandes.add(lanceurs.submit(() -> {
                    depart.await();
                    return gestionnaire.simulerProchainTournoi();
                }));
            }
            depart.countDown();

            List<Future<RapportTournoi>> simulations = new ArrayList<>();
            int demandesRefusees = 0;
            for (Future<Future<RapportTournoi>> demande : demandes) {
                try {
                    simulations.add(demande.get(5, TimeUnit.SECONDS));
                } catch (ExecutionException e) {
                    assertTrue(e.getCause() instanceof IllegalStateException);
                    demandesRefusees++;
                }
            }

            assertEquals(3, simulations.size());
            assertEquals(1, demandesRefusees);
            for (Future<RapportTournoi> simulation : simulations) {
                assertTrue(simulation.get(5, TimeUnit.SECONDS).estReussi());
            }
        } finally {
            lanceurs.shutdownNow();
        }
    }

    @Test
    void interruptionFinishesRemainingMatchesAndClosesSimulationPool() throws Exception {
        Jeux jeu = new JeuxSolo("Tennis", 3);
        Tournoi tournoi = tournoiValide("Interrompu", jeu, LocalDate.of(2026, 11, 1));
        GestionnaireTournois gestionnaire = new GestionnaireTournois(60_000, 0.0, 99);
        gestionnaire.ajouterTournoi(tournoi);
        Future<RapportTournoi> resultat = gestionnaire.simulerProchainTournoi();

        gestionnaire.close();

        assertTrue(resultat.isDone());
        assertEquals(Tournoi.Statut.TERMINE, tournoi.getStatut());
        assertEquals(0, tournoi.getRapport().getMatchsJoues());
        assertFalse(tournoi.getRapport().estReussi());
        assertThrows(IllegalStateException.class, gestionnaire::simulerProchainTournoi);
    }

    private static Tournoi tournoiValide(String nom, Jeux jeu, LocalDate date) {
        Tournoi tournoi = creerTournoi(nom, jeu, date, 4);
        inscrireQuatreEquipes(tournoi);
        inscrireArbitre(tournoi, jeu);
        return tournoi;
    }

    private static Tournoi creerTournoi(String nom, Jeux jeu, LocalDate date, int arenes) {
        return new Tournoi(nom, jeu, date, new BigDecimal("1000.00"), arenes);
    }

    private static List<Equipe> inscrireQuatreEquipes(Tournoi tournoi) {
        List<Equipe> equipes = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Equipe equipe = creerEquipe("Equipe " + i);
            tournoi.inscrireEquipe(equipe);
            equipes.add(equipe);
        }
        return equipes;
    }

    private static void inscrireEquipe(Tournoi tournoi, Equipe equipe) {
        tournoi.inscrireEquipe(equipe);
        for (int i = 1; i <= 3; i++) {
            tournoi.inscrireEquipe(creerEquipe("Supplementaire " + i));
        }
    }

    private static Equipe creerEquipe(String nom) {
        Coach coach = new Coach(1, "Coach " + nom, "Prenom", "pseudo", "genre", "telephone", nom);
        Joueur joueur = new Joueur(
                1, "Joueur " + nom, "Prenom", "pseudo", "genre", "telephone", nom, Joueur.Statut.AMATEUR);
        return new Equipe(nom, coach, new Joueur[] {joueur}, 0);
    }

    private static Arbitre inscrireArbitre(Tournoi tournoi, Jeux jeu) {
        Arbitre arbitre = new Arbitre(1, "Arbitre", "Prenom", "pseudo", "genre", "telephone");
        arbitre.qualifierPourJeu(jeu.getNom());
        tournoi.inscrireArbitre(arbitre);
        return arbitre;
    }
}
