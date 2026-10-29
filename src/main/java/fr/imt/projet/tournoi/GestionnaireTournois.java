package fr.imt.projet.tournoi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import fr.imt.projet.rapports.RapportTournoi;

public class GestionnaireTournois implements AutoCloseable {
    private static final int MAX_TOURNOIS_EN_COURS = 3;

    private final List<Tournoi> tournois = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(MAX_TOURNOIS_EN_COURS);
    private final long tempsParMatchMillis;
    private final double probabiliteIncident;
    private final long graine;
    private boolean ferme;
    private long numeroSimulation;

    public GestionnaireTournois() {
        this(100, 0.10, System.nanoTime());
    }

    public GestionnaireTournois(long tempsParMatchMillis, double probabiliteIncident, long graine) {
        if (tempsParMatchMillis < 0) {
            throw new IllegalArgumentException("Le temps par match ne peut pas être négatif");
        }
        if (probabiliteIncident < 0.0 || probabiliteIncident > 1.0) {
            throw new IllegalArgumentException("La probabilité d'incident doit être entre 0 et 1");
        }
        this.tempsParMatchMillis = tempsParMatchMillis;
        this.probabiliteIncident = probabiliteIncident;
        this.graine = graine;
    }

    public synchronized List<Tournoi> getTournois() {
        return List.copyOf(tournois);
    }

    public synchronized void ajouterTournoi(Tournoi tournoi) {
        verifierOuvert();
        Objects.requireNonNull(tournoi, "tournoi ne peut pas être null");
        if (tournois.contains(tournoi)) {
            throw new IllegalArgumentException("Le tournoi est déjà enregistré");
        }
        tournois.add(tournoi);
    }

    public synchronized Tournoi lancerProchainTournoi() {
        verifierOuvert();
        verifierCapacite();
        Tournoi prochain = prochainTournoi();
        verifierAbsenceDeConflit(prochain);
        prochain.demarrer();
        return prochain;
    }

    public synchronized Future<RapportTournoi> simulerProchainTournoi() {
        verifierOuvert();
        verifierCapacite();
        Tournoi prochain = prochainTournoi();
        verifierAbsenceDeConflit(prochain);
        prochain.demarrer();
        return executor.submit(new SimulateurTournoi(
                prochain, tempsParMatchMillis, probabiliteIncident, graine + numeroSimulation++));
    }

    private void verifierCapacite() {
        long tournoisEnCours = tournois.stream()
                .filter(t -> t.getStatut() == Tournoi.Statut.EN_COURS)
                .count();
        if (tournoisEnCours >= MAX_TOURNOIS_EN_COURS) {
            throw new IllegalStateException("Trois tournois sont déjà en cours");
        }
    }

    private Tournoi prochainTournoi() {
        return tournois.stream()
                .filter(t -> t.getStatut() == Tournoi.Statut.PLANIFIE)
                .min(Comparator.comparing(Tournoi::getDate))
                .orElseThrow(() -> new IllegalStateException("Aucun tournoi n'est planifié"));
    }

    private void verifierAbsenceDeConflit(Tournoi prochain) {
        tournois.stream()
                .filter(t -> t.getStatut() == Tournoi.Statut.EN_COURS)
                .filter(prochain::estEnConflitAvec)
                .findFirst()
                .ifPresent(t -> {
                    throw new IllegalStateException(
                            "Une équipe, un coach ou un joueur est déjà engagé dans un tournoi en cours");
                });
    }

    @Override
    public void close() {
        synchronized (this) {
            if (ferme) {
                return;
            }
            ferme = true;
            executor.shutdownNow();
            for (Tournoi tournoi : tournois) {
                if (tournoi.getStatut() == Tournoi.Statut.EN_COURS) {
                    tournoi.terminerLesMatchsRestantsSurIncident();
                }
            }
        }

        boolean interrompu = false;
        while (!executor.isTerminated()) {
            try {
                if (!executor.awaitTermination(1, TimeUnit.DAYS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                interrompu = true;
                executor.shutdownNow();
            }
        }
        if (interrompu) {
            Thread.currentThread().interrupt();
        }
    }

    private void verifierOuvert() {
        if (ferme) {
            throw new IllegalStateException("Le gestionnaire de tournois est fermé");
        }
    }
}
