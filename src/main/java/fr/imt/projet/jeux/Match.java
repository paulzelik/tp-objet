package fr.imt.projet.jeux;

import fr.imt.projet.personne.Arbitre;
import fr.imt.projet.personne.Joueur;
import java.util.Objects;

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

    public Match(Equipe equipe1, Equipe equipe2, Jeux jeu, Arbitre arbitre) {
        this.equipe1 = Objects.requireNonNull(equipe1, "equipe1 ne peut pas être null");
        this.equipe2 = Objects.requireNonNull(equipe2, "equipe2 ne peut pas être null");
        if (equipe1 == equipe2) {
            throw new IllegalArgumentException("Un match doit opposer deux équipes différentes");
        }
        this.jeu = Objects.requireNonNull(jeu, "jeu ne peut pas être null");
        this.arbitre = Objects.requireNonNull(arbitre, "arbitre ne peut pas être null");
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
            throw new IllegalStateException("Le match n'est pas planifié");
        }
        arbitre.arbitrerMatch(jeu.getNom());
        statut = Statut.EN_COURS;
    }

    public synchronized void declarerVainqueur(Equipe vainqueur) {
        Objects.requireNonNull(vainqueur, "vainqueur ne peut pas être null");
        if (statut != Statut.EN_COURS) {
            throw new IllegalStateException("Le match doit être en cours pour déclarer son vainqueur");
        }
        if (vainqueur != equipe1 && vainqueur != equipe2) {
            throw new IllegalArgumentException("Le vainqueur doit être l'une des équipes du match");
        }

        vainqueur.ajouterPoints(POINTS_PAR_VICTOIRE);
        vainqueur.getCoach().enregistrerVictoireEquipe();
        for (Joueur joueur : vainqueur.getJoueurs()) {
            joueur.enregistrerVictoireEquipe();
        }
        this.vainqueur = vainqueur;
        statut = Statut.JOUE;
    }

    public synchronized void signalerIncidentTechnique() {
        if (statut != Statut.EN_COURS) {
            throw new IllegalStateException("Le match doit être en cours pour signaler un incident");
        }
        statut = Statut.INCIDENT_TECHNIQUE;
    }
}
