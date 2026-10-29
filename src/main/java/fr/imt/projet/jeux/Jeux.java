package fr.imt.projet.jeux;

import fr.imt.projet.exceptions.RegleMetierException;
import java.util.Objects;

public abstract class Jeux {
    private final String nom;
    private final int nombreManchesParMatch;
    private final int nombreJoueursParEquipe;

    protected Jeux(String nom, int nombreManchesParMatch, int nombreJoueursParEquipe) {
        this.nom = nomValide(nom);
        if (nombreManchesParMatch <= 0) {
            throw new RegleMetierException("Le nombre de manches doit être positif");
        }
        if (nombreJoueursParEquipe <= 0) {
            throw new RegleMetierException("Le nombre de joueurs par équipe doit être positif");
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

    public int nbJoueursRequis() {
        return getNombreJoueursParEquipe();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Jeux other)) return false;
        return Objects.equals(nom, other.nom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nom);
    }

    @Override
    public String toString() {
        return nom;
    }

    private static String nomValide(String nom) {
        Objects.requireNonNull(nom, "nom ne peut pas être null");
        if (nom.isBlank()) {
            throw new RegleMetierException("nom ne peut pas être vide");
        }
        return nom;
    }
}
