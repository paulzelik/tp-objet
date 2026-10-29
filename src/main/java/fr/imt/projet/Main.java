package fr.imt.projet;

import fr.imt.projet.jeux.*;
import fr.imt.projet.personnes.*;
import fr.imt.projet.rapports.*;
import fr.imt.projet.tournois.*;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.LogManager;

/**
 * Programme de démonstration : une mini-saison IMT Arena de bout en bout.
 * C'est le seul endroit (avec les tests) où l'on écrit sur la console.
 */
public class Main {
    private static final long GRAINE = 2026L;
    private static final List<String> PRENOMS = List.of("Léa", "Hugo", "Inès", "Lucas", "Chloé", "Nathan",
            "Manon", "Yanis", "Camille", "Théo", "Sarah", "Adam", "Jade", "Louis", "Zoé", "Rayan");
    private static final List<String> NOMS = List.of("Martin", "Bernard", "Dubois", "Moreau", "Laurent",
            "Garcia", "Roux", "Fournier", "Girard", "Bonnet", "Lambert", "Fontaine");
    private static final List<String> PSEUDOS = List.of("Shadow", "Nova", "Pixel", "Blitz", "Viper", "Echo",
            "Ghost", "Raptor", "Kraken", "Zephyr", "Glitch", "Byte", "Comet", "Frost", "Onyx", "Spark");

    private static final Random HASARD = new Random(GRAINE);
    private static long prochainId = 1;

    public static void main(String[] args) throws Exception {
        configurerJournalisation();
        demo(JpaRapportRepository.fichier());
    }

    /** La démonstration, avec le dépôt de rapports fourni (fichier H2 pour le main, mémoire pour les tests). */
    static void demo(RapportRepository rapports) throws InterruptedException, ExecutionException, TimeoutException {
        System.out.println("=== IMT Arena — saison e-sport (ça sent le café froid) ===");

        // 1. Jeux (patron Fabrique) — un nouveau jeu ajouté sans modifier le code existant
        FabriqueJeux fabrique = FabriqueJeux.avecCatalogueParDefaut()
                .enregistrer("Pong", () -> new JeuxSolo("Pong", 1));
        Jeux rocketLeague = fabrique.creer("rocket league");
        Jeux streetFighter = fabrique.creer("Street Fighter 6");
        System.out.println("\n--- 1. Jeux disponibles : " + fabrique.getJeuxDisponibles());

        // 2. Équipes, joueurs, coachs, arbitres générés au hasard (graine fixe)
        System.out.println("\n--- 2. Équipes et arbitres");
        List<String> noms = List.of("Les Cafés Froids", "Kernel Panic", "404 Skill Not Found", "Ctrl Alt Defeat",
                "Null Pointer Esport", "Les Lag Masters", "Segfault Squad", "Blue Screen Gang");
        List<Equipe> equipes = new ArrayList<>();
        for (String nom : noms) {
            Equipe equipe = new Equipe(nom, nouveauCoach(), nouveauJoueur(), nouveauJoueur(), nouveauJoueur());
            equipes.add(equipe);
            System.out.println("  " + nom + " — coach " + equipe.getCoach().getPseudo() + " — "
                    + equipe.getJoueurs().stream().map(Joueur::getPseudo).toList());
        }
        Arbitre refRl1 = nouvelArbitre(rocketLeague);
        Arbitre refRl2 = nouvelArbitre(rocketLeague);
        Arbitre refSf = nouvelArbitre(streetFighter);
        Arbitre refPolyvalent = nouvelArbitre(rocketLeague, streetFighter);

        System.out.println("  Les objets protègent leurs règles :");
        essayer("un joueur dans deux équipes", () -> equipes.get(1).recruter(equipes.get(0).getJoueurs().get(0)));
        essayer("un tournoi à 5 arènes", () -> Tournoi.builder().nom("X").jeu(rocketLeague)
                .date(LocalDate.now()).budget(1).arenes(5).build());
        essayer("un arbitre non qualifié", () -> Tournoi.builder().nom("Y").jeu(rocketLeague)
                .date(LocalDate.now()).budget(1).arbitre(refSf).build());

        try (GestionnaireTournois gestionnaire = new GestionnaireTournois(
                120, 0.10, StrategieVainqueur.favoriserMieuxClassee(), GRAINE, rapports)) {
            gestionnaire.ajouterObservateur(new AffichageConsole());
            equipes.forEach(gestionnaire::ajouterEquipe);
            List.of(refRl1, refRl2, refSf, refPolyvalent).forEach(gestionnaire::ajouterArbitre);

            // 3. Planification (patron Monteur)
            Tournoi coupeHiver = Tournoi.builder().nom("Coupe d'Hiver").jeu(rocketLeague)
                    .date(LocalDate.of(2026, 12, 10)).budget(8_000).arenes(3)
                    .equipes(equipes.subList(0, 6)).arbitre(refRl1).arbitre(refPolyvalent).build();
            Tournoi openRentree = Tournoi.builder().nom("Open de Rentrée").jeu(rocketLeague)
                    .date(LocalDate.of(2026, 11, 5)).budget(3_000).arenes(2)
                    .equipes(equipes.subList(0, 4)).arbitre(refRl1).arbitre(refRl2).build();
            Tournoi sfNight = Tournoi.builder().nom("Street Fighter Night").jeu(streetFighter)
                    .date(LocalDate.of(2026, 11, 12)).budget(1_500).arenes(2)
                    .equipes(equipes.subList(4, 8)).arbitre(refSf).build();
            Tournoi pong = Tournoi.builder().nom("Pong Invitational").jeu(fabrique.creer("Pong"))
                    .date(LocalDate.of(2027, 1, 15)).budget(42).equipes(equipes.subList(2, 6)).build();
            for (Tournoi tournoi : List.of(coupeHiver, openRentree, sfNight, pong)) {
                gestionnaire.ajouterTournoi(tournoi);
            }
            System.out.println("\n--- 3. Tournois planifiés (le plus proche d'abord)");
            gestionnaire.getTournoisPlanifies().forEach(t -> System.out.println("  " + t));

            // 4. Simulation en arrière-plan, deux tournois en parallèle
            System.out.println("\n--- 4. Simulations (ExecutorService)");
            Future<RapportTournoi> rapportOpen = gestionnaire.simulerProchainTournoi();
            Future<RapportTournoi> rapportSf = gestionnaire.simulerProchainTournoi();
            essayer("lancer la Coupe d'Hiver en même temps (équipes déjà engagées)",
                    gestionnaire::simulerProchainTournoi);
            System.out.println("  Arbitres occupés : " + pseudos(gestionnaire.getArbitresOccupes())
                    + " / disponibles : " + pseudos(gestionnaire.getArbitresDisponibles()));
            while (!rapportOpen.isDone() || !rapportSf.isDone()) {
                for (Tournoi t : gestionnaire.getTournoisEnCours()) {
                    System.out.printf("  … %s : %s, %d/%d matchs terminés%n", t.getNom(), t.getStatut(),
                            t.getNombreMatchsTermines(), t.getMatchs().size());
                }
                Thread.sleep(150); // le programme principal reste utilisable pendant la simulation
            }
            rapportOpen.get(10, TimeUnit.SECONDS);
            rapportSf.get(10, TimeUnit.SECONDS);
            RapportTournoi rapportHiver = gestionnaire.simulerProchainTournoi().get(10, TimeUnit.SECONDS);
            System.out.println("  Rapport de la Coupe d'Hiver reçu : " + rapportHiver);

            // 5. Les listes demandées par le sujet
            System.out.println("\n--- 5. Listes");
            System.out.println("Tournois terminés (budget décroissant) :");
            gestionnaire.getTournoisTermines().forEach(t -> System.out.println("  " + t.getBudget() + " € — " + t.getNom()));
            System.out.println("Tournois encore planifiés :");
            gestionnaire.getTournoisPlanifies().forEach(t -> System.out.println("  " + t));
            System.out.println("Classement des équipes (points, puis nom) :");
            int rang = 1;
            for (GestionnaireTournois.LigneClassement ligne : gestionnaire.getClassementEquipes()) {
                System.out.printf("  %d. %-22s %3d pts%n", rang++, ligne.equipe(), ligne.points());
            }
            Equipe cafes = gestionnaire.getEquipe("Les Cafés Froids").orElseThrow();
            System.out.println("Matchs de « " + cafes.getNom() + " » :");
            gestionnaire.getMatchsDe(cafes).forEach(m -> System.out.println("  " + m));
            System.out.println("Matchs de la Coupe d'Hiver regroupés par équipe gagnante :");
            for (Map.Entry<Equipe, List<Match>> entree : gestionnaire.getMatchsParEquipeGagnante(coupeHiver).entrySet()) {
                System.out.println("  " + entree.getKey() + " : " + entree.getValue().size() + " victoire(s)");
            }
            System.out.println("Coachs, joueurs pros et arbitres :");
            for (Equipe equipe : equipes) {
                System.out.printf("  %-22s coach %s : %d victoire(s) ; pros : %s%n", equipe.getNom(),
                        equipe.getCoach().getPseudo(), equipe.getCoach().getTotalVictoiresEnCarriere(),
                        equipe.getJoueurs().stream().filter(j -> j.getStatut() == Joueur.Statut.PRO)
                                .map(Joueur::getPseudo).toList());
            }
            for (Arbitre arbitre : List.of(refRl1, refRl2, refSf, refPolyvalent)) {
                System.out.println("  " + arbitre.getPseudo() + " a arbitré " + arbitre.getMatchsArbitres() + " match(s)");
            }

            // 6. Persistance (JPA / H2)
            System.out.println("\n--- 6. Rapports archivés en base (y compris des lancements précédents)");
            gestionnaire.getRapportsArchives().forEach(r -> System.out.println("  #" + r.getId() + " " + r));
            System.out.println("  Rapports Rocket League : " + gestionnaire.getRapportsArchives("Rocket League").size());

            System.out.println("\n" + refPolyvalent.siffler());
        }
        System.out.println("Gestionnaire fermé proprement : aucun thread de simulation ne continue.");
    }

    /** Observateur d'affichage (patron Observateur) : vit dans le main, seul autorisé à écrire. */
    private static final class AffichageConsole implements ObservateurTournoi {
        @Override
        public void tournoiDemarre(Tournoi tournoi) {
            System.out.println("  > " + tournoi.getNom() + " démarre : " + tournoi.getMatchs().size() + " matchs");
        }

        @Override
        public void incidentTechnique(Tournoi tournoi, Match match) {
            System.out.println("  ! [" + Thread.currentThread().getName() + "] " + tournoi.getNom() + " : " + match);
        }

        @Override
        public void tournoiTermine(Tournoi tournoi, RapportTournoi rapport) {
            System.out.println("  # [" + Thread.currentThread().getName() + "] " + rapport);
        }
    }

    private static Joueur nouveauJoueur() {
        return new Joueur(prochainId++, piocher(NOMS), piocher(PRENOMS), piocher(PSEUDOS) + prochainId,
                genre(), telephone(), Joueur.Statut.AMATEUR);
    }

    private static Coach nouveauCoach() {
        return new Coach(prochainId++, piocher(NOMS), piocher(PRENOMS), "Coach" + piocher(PSEUDOS) + prochainId,
                genre(), telephone());
    }

    private static Arbitre nouvelArbitre(Jeux... jeux) {
        Arbitre arbitre = new Arbitre(prochainId++, piocher(NOMS), piocher(PRENOMS), "Ref" + piocher(PSEUDOS) + prochainId,
                genre(), telephone());
        for (Jeux jeu : jeux) {
            arbitre.qualifierPourJeu(jeu.getNom());
        }
        return arbitre;
    }

    private static String piocher(List<String> valeurs) {
        return valeurs.get(HASARD.nextInt(valeurs.size()));
    }

    private static String genre() {
        return HASARD.nextBoolean() ? "F" : "M";
    }

    private static String telephone() {
        return "06 %02d %02d %02d %02d".formatted(HASARD.nextInt(100), HASARD.nextInt(100),
                HASARD.nextInt(100), HASARD.nextInt(100));
    }

    private static void essayer(String description, Runnable action) {
        try {
            action.run();
            System.out.println("  ✗ accepté (inattendu) : " + description);
        } catch (RuntimeException e) {
            System.out.println("  ✓ refusé : " + description + " — " + e.getMessage());
        }
    }

    private static List<String> pseudos(List<Arbitre> arbitres) {
        return arbitres.stream().map(Arbitre::getPseudo).toList();
    }

    private static void configurerJournalisation() {
        try (InputStream config = Main.class.getResourceAsStream("/logging.properties")) {
            if (config != null) {
                LogManager.getLogManager().readConfiguration(config);
            }
        } catch (IOException e) {
            System.err.println("Configuration de journalisation ignorée : " + e.getMessage());
        }
    }
}
