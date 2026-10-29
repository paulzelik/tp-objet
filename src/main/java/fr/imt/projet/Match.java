package fr.imt.projet;

import java.util.Objects;

/**
 * Match entre deux équipes différentes sur un jeu, sous le contrôle d'un arbitre qualifié.
 * Il garde la trace des deux équipes, de l'arbitre et du vainqueur. Ses transitions sont
 * package-private : elles passent par le {@link Tournoi}, qui contrôle les arènes.
 */
public class Match {
    public enum Statut {
        PLANIFIE,
        EN_COURS,
        JOUE,
        INCIDENT_TECHNIQUE;

        public boolean estTermine() {
            return this == JOUE || this == INCIDENT_TECHNIQUE;
        }
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

    public boolean oppose(Equipe equipe) {
        return equipe == equipe1 || equipe == equipe2;
    }

    synchronized void demarrer() {
        if (statut != Statut.PLANIFIE) {
            throw new IllegalStateException("Le match n'est pas planifié");
        }
        arbitre.arbitrerMatch(jeu.getNom());
        statut = Statut.EN_COURS;
    }

    synchronized void declarerVainqueur(Equipe vainqueur) {
        Objects.requireNonNull(vainqueur, "vainqueur ne peut pas être null");
        if (statut != Statut.EN_COURS) {
            throw new IllegalStateException("Le match doit être en cours pour déclarer son vainqueur");
        }
        if (!oppose(vainqueur)) {
            throw new IllegalArgumentException("Le vainqueur doit être l'une des équipes du match");
        }
        vainqueur.enregistrerVictoire();
        this.vainqueur = vainqueur;
        statut = Statut.JOUE;
    }

    synchronized void signalerIncidentTechnique() {
        if (statut != Statut.EN_COURS) {
            throw new IllegalStateException("Le match doit être en cours pour signaler un incident");
        }
        statut = Statut.INCIDENT_TECHNIQUE;
    }

    @Override
    public synchronized String toString() {
        String resultat = switch (statut) {
            case JOUE -> "victoire de " + vainqueur.getNom();
            case INCIDENT_TECHNIQUE -> "annulé (incident technique)";
            case EN_COURS -> "en cours";
            case PLANIFIE -> "planifié";
        };
        return equipe1.getNom() + " vs " + equipe2.getNom() + " [" + arbitre.getPseudo() + "] → " + resultat;
    }
}
