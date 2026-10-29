package fr.imt.projet;

import java.util.Objects;

/**
 * Coach : dirige une seule équipe à la fois et cumule les victoires de ses équipes en carrière.
 * L'équipe est posée uniquement par {@link Equipe} (création ou changement de coach).
 */
public class Coach extends Personne implements Classable {
    private Equipe equipe;
    private int totalVictoiresEnCarriere;
    private int classement;

    public Coach(long id, String nom, String prenom, String pseudo, String genre, String telephone) {
        super(id, nom, prenom, pseudo, genre, telephone);
    }

    /** L'équipe dirigée, ou {@code null} si le coach est libre. */
    public synchronized Equipe getEquipe() {
        return equipe;
    }

    public synchronized int getTotalVictoiresEnCarriere() {
        return totalVictoiresEnCarriere;
    }

    @Override
    public synchronized int getClassement() {
        return classement;
    }

    @Override
    public synchronized void classer(int classement) {
        if (classement <= 0) {
            throw new IllegalArgumentException("Le classement doit être positif");
        }
        this.classement = classement;
    }

    synchronized void prendreEnCharge(Equipe nouvelleEquipe) {
        if (equipe != null) {
            throw new IllegalStateException(getPseudo() + " coache déjà « " + equipe.getNom() + " »");
        }
        equipe = Objects.requireNonNull(nouvelleEquipe);
    }

    synchronized void quitter(Equipe ancienneEquipe) {
        if (equipe != ancienneEquipe) {
            throw new IllegalStateException(getPseudo() + " ne coache pas cette équipe");
        }
        equipe = null;
    }

    synchronized void enregistrerVictoireEquipe() {
        totalVictoiresEnCarriere++;
    }
}
