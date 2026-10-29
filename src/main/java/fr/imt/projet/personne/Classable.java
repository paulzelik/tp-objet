package fr.imt.projet.personne;

public interface Classable {
    int getClassement();

    void classer(int classement);

    default int getPoints() {
        return getClassement();
    }

    default boolean estClasse() {
        return getClassement() > 0;
    }
}
