package fr.imt.projet.personne;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Arbitre extends Personne implements QualifiablePourJeu {
    private final Set<String> jeuxQualifies = new HashSet<>();
    private int matchsArbitres;

    public Arbitre(
            long id,
            String nom,
            String prenom,
            String pseudo,
            String genre,
            String telephone) {
        super(id, nom, prenom, pseudo, genre, telephone);
    }

    @Override
    public Set<String> getJeuxQualifies() {
        return Collections.unmodifiableSet(jeuxQualifies);
    }

    public int getMatchsArbitres() {
        return matchsArbitres;
    }

    @Override
    public void qualifierPourJeu(String jeu) {
        jeuxQualifies.add(nomValide(jeu));
    }

    public void arbitrerMatch(String jeu) {
        String jeuValide = nomValide(jeu);
        if (!estQualifiePour(jeuValide)) {
            throw new IllegalArgumentException("L'arbitre n'est pas qualifié pour le jeu : " + jeuValide);
        }
        matchsArbitres++;
    }

    private static String nomValide(String valeur) {
        Objects.requireNonNull(valeur, "jeu ne peut pas être null");
        if (valeur.isBlank()) {
            throw new IllegalArgumentException("jeu ne peut pas être vide");
        }
        return valeur;
    }
}
