package fr.imt.projet.personne;

import fr.imt.projet.jeux.Jeux;
import java.util.Set;

public interface QualifiablePourJeu {
    Set<String> getJeuxQualifies();

    void qualifierPourJeu(String jeu);

    default boolean estQualifiePour(String jeu) {
        return getJeuxQualifies().contains(jeu);
    }

    default boolean estQualifiePour(Jeux jeu) {
        return jeu != null && estQualifiePour(jeu.getNom());
    }
}
