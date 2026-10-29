package fr.imt.projet.jeux;

import fr.imt.projet.exceptions.RegleMetierException;
import fr.imt.projet.personne.Arbitre;
import fr.imt.projet.personne.Joueur;
import fr.imt.projet.tournoi.observateur.MatchListener;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class Match {
    private static final int POINTS_PAR_VICTOIRE = 3;

    public enum Statut {
        PLANIFIE,
        EN_COURS,
        JOUE,
        INCIDENT_TECHNIQUE
    }

    private final Equipe equipe1;
    private final Equipe equipe2;
    private final Jeux jeu;
    private final Arbitre arbitre;
    private Equipe vainqueur;
    private Statut statut = Statut.PLANIFIE;
    private final List<MatchListener> listeners = new CopyOnWriteArrayList<>();

    public Match(Equipe equipe1, Equipe equipe2, Jeux jeu, Arbitre arbitre) {
        this.equipe1 = Objects.requireNonNull(equipe1, "equipe1 ne peut pas être null");
        this.equipe2 = Objects.requireNonNull(equipe2, "equipe2 ne peut pas être null");
        if (equipe1.equals(equipe2)) {
            throw new RegleMetierException("Un match doit opposer deux équipes différentes");
        }
        this.jeu = Objects.requireNonNull(jeu, "jeu ne peut pas être null");
        this.arbitre = Objects.requireNonNull(arbitre, "arbitre ne peut pas être null");
        if (!arbitre.estQualifiePour(jeu)) {
            throw new RegleMetierException("L'arbitre n'est pas qualifié pour le jeu " + jeu.getNom());
        }
    }

    public void addMatchListener(MatchListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeMatchListener(MatchListener listener) {
        listeners.remove(listener);
    }

    public Equipe getEquipe1() {
        return equipe1;
    }

    public Equipe getEquipe2() {
        return equipe2;
    }

    public Jeux getJeu() {
        return jeu;
    }

    public Arbitre getArbitre() {
        return arbitre;
    }

    public synchronized Equipe getVainqueur() {
        return vainqueur;
    }

    public synchronized Statut getStatut() {
        return statut;
    }

    public synchronized void demarrer() {
        if (statut != Statut.PLANIFIE) {
            throw new RegleMetierException("Le match n'est pas planifié");
        }
        arbitre.arbitrerMatch(jeu);
        statut = Statut.EN_COURS;
    }

    public synchronized void declarerVainqueur(Equipe vainqueur) {
        Objects.requireNonNull(vainqueur, "vainqueur ne peut pas être null");
        if (statut != Statut.EN_COURS) {
            throw new RegleMetierException("Le match doit être en cours pour déclarer son vainqueur");
        }
        if (!vainqueur.equals(equipe1) && !vainqueur.equals(equipe2)) {
            throw new RegleMetierException("Le vainqueur doit être l'une des deux équipes du match");
        }
        this.vainqueur = vainqueur;
        this.statut = Statut.JOUE;
        vainqueur.ajouterPoints(POINTS_PAR_VICTOIRE);
        if (vainqueur.getCoach() != null) {
            vainqueur.getCoach().enregistrerVictoireEquipe();
        }
        for (Joueur joueur : vainqueur.getJoueurs()) {
            joueur.enregistrerVictoireEquipe();
        }
        notifierMatchTermine(vainqueur);
    }

    public synchronized void signalerIncidentTechnique() {
        if (statut != Statut.EN_COURS) {
            throw new RegleMetierException("Le match doit être en cours pour signaler un incident technique");
        }
        this.statut = Statut.INCIDENT_TECHNIQUE;
        notifierIncident();
    }

    private void notifierMatchTermine(Equipe vainqueur) {
        for (MatchListener listener : listeners) {
            listener.onMatchTermine(this, vainqueur);
        }
    }

    private void notifierIncident() {
        for (MatchListener listener : listeners) {
            listener.onIncident(this);
        }
    }
}
