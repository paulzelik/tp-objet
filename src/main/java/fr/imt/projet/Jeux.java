package fr.imt.projet;

import java.util.Objects;

/**
 * Un jeu : un nom, un nombre de manches par match et un nombre de joueurs alignés par équipe.
 * Classe abstraite : on crée toujours un type de jeu précis ({@link JeuxSolo}, {@link JeuxEquipe}).
 * Un nouveau type de jeu s'ajoute par une nouvelle sous-classe, sans modifier l'existant.
 */
public abstract class Jeux {
    private final String nom;
    private final int nombreManchesParMatch;
    private final int nombreJoueursParEquipe;

    protected Jeux(String nom, int nombreManchesParMatch, int nombreJoueursParEquipe) {
        this.nom = nomValide(nom);
        if (nombreManchesParMatch <= 0) {
            throw new IllegalArgumentException("Le nombre de manches doit être positif");
        }
        if (nombreJoueursParEquipe <= 0) {
            throw new IllegalArgumentException("Le nombre de joueurs par équipe doit être positif");
        }
        this.nombreManchesParMatch = nombreManchesParMatch;
        this.nombreJoueursParEquipe = nombreJoueursParEquipe;
    }

    public String getNom() {
        return nom;
    }

    public int getNombreManchesParMatch() {
        return nombreManchesParMatch;
    }

    public int getNombreJoueursParEquipe() {
        return nombreJoueursParEquipe;
    }

    @Override
    public String toString() {
        return nom + " (" + nombreJoueursParEquipe + " joueur(s) par équipe, " + nombreManchesParMatch + " manche(s))";
    }

    private static String nomValide(String nom) {
        Objects.requireNonNull(nom, "nom ne peut pas être null");
        if (nom.isBlank()) {
            throw new IllegalArgumentException("nom ne peut pas être vide");
        }
        return nom;
    }
}
