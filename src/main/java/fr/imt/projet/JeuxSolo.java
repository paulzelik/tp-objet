package fr.imt.projet;

import java.util.List;
import java.util.Objects;

public class JeuxSolo extends Jeux {
    public JeuxSolo(String nom, int nombreManchesParMatch) {
        super(nom, nombreManchesParMatch, 1);
    }

    public Joueur selectionnerJoueur(List<Joueur> joueursEquipe, Joueur joueur) {
        Objects.requireNonNull(joueursEquipe, "joueursEquipe ne peut pas être null");
        Objects.requireNonNull(joueur, "joueur ne peut pas être null");
        if (!joueursEquipe.contains(joueur)) {
            throw new IllegalArgumentException("Le joueur sélectionné doit appartenir à l'équipe");
        }
        return joueur;
    }
}
