package fr.imt.projet;

import java.util.Set;

public interface QualifiablePourJeu {
    Set<String> getJeuxQualifies();

    void qualifierPourJeu(String jeu);

    default boolean estQualifiePour(String jeu) {
        return getJeuxQualifies().contains(jeu);
    }
}
