package fr.imt.projet.personne;

import java.util.Objects;

public class Coach extends Personne implements Classable {
    private String equipe;
    private int totalVictoiresEnCarriere;
    private int classement;

    public Coach(
            long id,
            String nom,
            String prenom,
            String pseudo,
            String genre,
            String telephone,
            String equipe) {
        super(id, nom, prenom, pseudo, genre, telephone);
        this.equipe = nomValide(equipe);
    }

    public String getEquipe() {
        return equipe;
    }

    public int getTotalVictoiresEnCarriere() {
        return totalVictoiresEnCarriere;
    }

    @Override
    public int getClassement() {
        return classement;
    }

    @Override
    public void classer(int classement) {
        if (classement <= 0) {
            throw new IllegalArgumentException("Le classement doit être positif");
        }
        this.classement = classement;
    }

    public void changerEquipe(String equipe) {
        this.equipe = nomValide(equipe);
    }

    public void enregistrerVictoireEquipe() {
        totalVictoiresEnCarriere++;
    }

    private static String nomValide(String equipe) {
        Objects.requireNonNull(equipe, "equipe ne peut pas être null");
        if (equipe.isBlank()) {
            throw new IllegalArgumentException("equipe ne peut pas être vide");
        }
        return equipe;
    }
}
