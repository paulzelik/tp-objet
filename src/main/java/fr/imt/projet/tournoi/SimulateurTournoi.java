package fr.imt.projet.tournoi;

import fr.imt.projet.jeux.Equipe;
import fr.imt.projet.jeux.Match;
import fr.imt.projet.rapports.RapportTournoi;
import fr.imt.projet.tournoi.strategie.FavoriseMieuxClasse;
import fr.imt.projet.tournoi.strategie.StrategieVainqueur;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;

public class SimulateurTournoi implements Callable<RapportTournoi> {
    private final Tournoi tournoi;
    private final long tempsParMatchMillis;
    private final double probabiliteIncident;
    private final Random random;
    private final StrategieVainqueur strategieVainqueur;

    public SimulateurTournoi(Tournoi tournoi, long tempsParMatchMillis, double probabiliteIncident, long graine) {
        this(tournoi, tempsParMatchMillis, probabiliteIncident, new Random(graine), new FavoriseMieuxClasse());
    }

    public SimulateurTournoi(
            Tournoi tournoi,
            long tempsParMatchMillis,
            double probabiliteIncident,
            Random random,
            StrategieVainqueur strategieVainqueur) {
        this.tournoi = Objects.requireNonNull(tournoi, "tournoi ne peut pas être null");
        if (tempsParMatchMillis < 0) {
            throw new IllegalArgumentException("Le temps par match ne peut pas être négatif");
        }
        if (probabiliteIncident < 0.0 || probabiliteIncident > 1.0) {
            throw new IllegalArgumentException("La probabilité d'incident doit être entre 0 et 1");
        }
        this.tempsParMatchMillis = tempsParMatchMillis;
        this.probabiliteIncident = probabiliteIncident;
        this.random = Objects.requireNonNull(random, "random ne peut pas être null");
        this.strategieVainqueur = Objects.requireNonNull(strategieVainqueur, "strategieVainqueur ne peut pas être null");
    }

    @Override
    public RapportTournoi call() throws InterruptedException {
        List<Match> matchs = tournoi.getMatchs();
        try {
            long dureeMillis = Math.multiplyExact((long) matchs.size(), tempsParMatchMillis)
                    / tournoi.getNombreArenes();
            Thread.sleep(dureeMillis);
        } catch (InterruptedException interruption) {
            tournoi.terminerLesMatchsRestantsSurIncident();
            Thread.currentThread().interrupt();
            throw interruption;
        } catch (RuntimeException erreur) {
            tournoi.terminerLesMatchsRestantsSurIncident();
            throw erreur;
        }

        for (Match match : matchs) {
            if (tournoi.getStatut() == Tournoi.Statut.TERMINE) {
                return tournoi.getRapport();
            }
            try {
                tournoi.demarrerMatch(match);
                if (random.nextDouble() < probabiliteIncident) {
                    tournoi.signalerIncidentTechnique(match);
                } else {
                    Equipe gagnant = strategieVainqueur.designerVainqueur(match, random);
                    tournoi.declarerVainqueur(match, gagnant);
                }
            } catch (IllegalStateException e) {
                if (tournoi.getStatut() == Tournoi.Statut.TERMINE) {
                    return tournoi.getRapport();
                }
                throw e;
            }
        }
        return tournoi.getRapport();
    }
}
