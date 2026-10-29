package fr.imt.projet.tournoi;

import fr.imt.projet.exceptions.RegleMetierException;
import fr.imt.projet.rapports.RapportTournoi;
import fr.imt.projet.tournoi.strategie.FavoriseMieuxClasse;
import fr.imt.projet.tournoi.strategie.StrategieVainqueur;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class GestionnaireTournois implements AutoCloseable {
    private static final int MAX_TOURNOIS_EN_COURS = 3;

    private final PriorityQueue<Tournoi> fileTournois = new PriorityQueue<>();
    private final List<Tournoi> tousLesTournois = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_TOURNOIS_EN_COURS);
    private final Semaphore semaphore = new Semaphore(MAX_TOURNOIS_EN_COURS);
    private final long tempsParMatchMillis;
    private final double probabiliteIncident;
    private final long graine;
    private final StrategieVainqueur strategieVainqueur;
    private boolean ferme;
    private long numeroSimulation;

    public GestionnaireTournois() {
        this(100, 0.10, System.nanoTime(), new FavoriseMieuxClasse());
    }

    public GestionnaireTournois(long tempsParMatchMillis, double probabiliteIncident, long graine) {
        this(tempsParMatchMillis, probabiliteIncident, graine, new FavoriseMieuxClasse());
    }

    public GestionnaireTournois(
            long tempsParMatchMillis,
            double probabiliteIncident,
            long graine,
            StrategieVainqueur strategieVainqueur) {
        if (tempsParMatchMillis < 0) {
            throw new IllegalArgumentException("Le temps par match ne peut pas être négatif");
        }
        if (probabiliteIncident < 0.0 || probabiliteIncident > 1.0) {
            throw new IllegalArgumentException("La probabilité d'incident doit être entre 0 et 1");
        }
        this.tempsParMatchMillis = tempsParMatchMillis;
        this.probabiliteIncident = probabiliteIncident;
        this.graine = graine;
        this.strategieVainqueur = Objects.requireNonNull(strategieVainqueur, "strategieVainqueur ne peut pas être null");
    }

    public synchronized List<Tournoi> getTournois() {
        return List.copyOf(tousLesTournois);
    }

    public synchronized List<Tournoi> getTournoisPlanifiesTries() {
        return fileTournois.stream().sorted().toList();
    }

    public synchronized void ajouterTournoi(Tournoi tournoi) {
        verifierOuvert();
        Objects.requireNonNull(tournoi, "tournoi ne peut pas être null");
        if (tousLesTournois.contains(tournoi)) {
            throw new RegleMetierException("Le tournoi est déjà enregistré");
        }
        tousLesTournois.add(tournoi);
        if (tournoi.getStatut() == Tournoi.Statut.PLANIFIE) {
            fileTournois.add(tournoi);
        }
    }

    public synchronized Tournoi lancerProchainTournoi() {
        verifierOuvert();
        if (!semaphore.tryAcquire()) {
            throw new IllegalStateException("Trois tournois sont déjà en cours");
        }
        try {
            Tournoi prochain = fileTournois.poll();
            if (prochain == null) {
                throw new IllegalStateException("Aucun tournoi n'est planifié");
            }
            verifierAbsenceDeConflit(prochain);
            prochain.demarrer();
            return prochain;
        } catch (Throwable t) {
            semaphore.release();
            throw t;
        }
    }

    public synchronized Future<RapportTournoi> simulerProchainTournoi() {
        verifierOuvert();
        if (!semaphore.tryAcquire()) {
            throw new IllegalStateException("Trois tournois sont déjà en cours");
        }
        Tournoi prochain;
        try {
            prochain = fileTournois.poll();
            if (prochain == null) {
                throw new IllegalStateException("Aucun tournoi n'est planifié");
            }
            verifierAbsenceDeConflit(prochain);
            prochain.demarrer();
        } catch (Throwable t) {
            semaphore.release();
            throw t;
        }

        Random random = new Random(graine + numeroSimulation++);
        return executor.submit(() -> {
            try {
                SimulateurTournoi simulateur = new SimulateurTournoi(
                        prochain, tempsParMatchMillis, probabiliteIncident, random, strategieVainqueur);
                return simulateur.call();
            } finally {
                semaphore.release();
            }
        });
    }

    private void verifierAbsenceDeConflit(Tournoi prochain) {
        tousLesTournois.stream()
                .filter(t -> t.getStatut() == Tournoi.Statut.EN_COURS)
                .filter(prochain::estEnConflitAvec)
                .findFirst()
                .ifPresent(t -> {
                    throw new IllegalStateException(
                            "Une équipe, un coach ou un joueur est déjà engagé dans un tournoi en cours");
                });
    }

    @Override
    public synchronized void close() {
        if (ferme) {
            return;
        }
        ferme = true;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void verifierOuvert() {
        if (ferme) {
            throw new IllegalStateException("Le gestionnaire de tournois est fermé");
        }
    }
}
