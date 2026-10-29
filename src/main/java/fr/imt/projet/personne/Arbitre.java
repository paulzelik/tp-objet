package fr.imt.projet.personne;

import fr.imt.projet.exceptions.RegleMetierException;
import fr.imt.projet.jeux.Jeux;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Arbitre extends Personne implements QualifiablePourJeu, Qualifiable {
    private final Set<String> jeuxQualifies = new HashSet<>();
    private final Set<Jeux> jeuxQualifiesObjets = new HashSet<>();
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

    public Set<Jeux> getJeuxQualifiesObjets() {
        return Collections.unmodifiableSet(jeuxQualifiesObjets);
    }

    public int getMatchsArbitres() {
        return matchsArbitres;
    }

    @Override
    public void qualifierPourJeu(String jeu) {
        jeuxQualifies.add(nomValide(jeu));
    }

    public void qualifierPourJeu(Jeux jeu) {
        Objects.requireNonNull(jeu, "jeu ne peut pas être null");
        jeuxQualifiesObjets.add(jeu);
        jeuxQualifies.add(jeu.getNom());
    }

    @Override
    public boolean estQualifiePour(Jeux jeu) {
        if (jeu == null) {
            return false;
        }
        return jeuxQualifiesObjets.contains(jeu) || jeuxQualifies.contains(jeu.getNom());
    }

    public void arbitrerMatch(String jeu) {
        String jeuValide = nomValide(jeu);
        if (!estQualifiePour(jeuValide)) {
            throw new RegleMetierException("L'arbitre n'est pas qualifié pour le jeu : " + jeuValide);
        }
        matchsArbitres++;
    }

    public void arbitrerMatch(Jeux jeu) {
        Objects.requireNonNull(jeu, "jeu ne peut pas être null");
        if (!estQualifiePour(jeu)) {
            throw new RegleMetierException("L'arbitre n'est pas qualifié pour le jeu : " + jeu.getNom());
        }
        matchsArbitres++;
    }

    private static String nomValide(String valeur) {
        Objects.requireNonNull(valeur, "jeu ne peut pas être null");
        if (valeur.isBlank()) {
            throw new RegleMetierException("jeu ne peut pas être vide");
        }
        return valeur;
    }
}
