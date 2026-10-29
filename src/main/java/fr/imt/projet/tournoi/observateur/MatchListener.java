package fr.imt.projet.tournoi.observateur;

import fr.imt.projet.jeux.Equipe;
import fr.imt.projet.jeux.Match;

public interface MatchListener {
    void onMatchTermine(Match match, Equipe vainqueur);

    void onIncident(Match match);
}

