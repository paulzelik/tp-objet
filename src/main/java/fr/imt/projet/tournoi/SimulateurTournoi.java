package fr.imt.projet.tournoi;

import fr.imt.projet.jeux.Equipe;
import fr.imt.projet.jeux.Match;
import fr.imt.projet.personne.Coach;
import fr.imt.projet.rapports.RapportTournoi;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;

public class SimulateurTournoi implements Callable<RapportTournoi> {
    private final Tournoi tournoi;
    private final long tempsParMatchMillis;
    private final double probabiliteIncident;
    private final Random random;

    public SimulateurTournoi(Tournoi tournoi, long tempsParMatchMillis, double probabiliteIncident, long graine) {
        this.tournoi = Objects.requireNonNull(tournoi, "tournoi ne peut pas être null");
        if (tempsParMatchMillis < 0) {
            throw new IllegalArgumentException("Le temps par match ne peut pas être négatif");
        }
        if (probabiliteIncident < 0.0 || probabiliteIncident > 1.0) {
            throw new IllegalArgumentException("La probabilité d'incident doit être entre 0 et 1");
        }
        this.tempsParMatchMillis = tempsParMatchMillis;
        this.probabiliteIncident = probabiliteIncident;
        random = new Random(graine);
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
                    tournoi.declarerVainqueur(match, choisirVainqueur(match));
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

    private Equipe choisirVainqueur(Match match) {
        Equipe equipe1 = match.getEquipe1();
        Equipe equipe2 = match.getEquipe2();
        int classement1 = classement(equipe1.getCoach());
        int classement2 = classement(equipe2.getCoach());
        double avantageEquipe1 = 1.0 / (1.0 + Math.pow(10.0, (classement1 - classement2) / 400.0));
        return random.nextDouble() < avantageEquipe1 ? equipe1 : equipe2;
    }

    private static int classement(Coach coach) {
        return coach.estClasse() ? coach.getClassement() : 0;
    }
}
