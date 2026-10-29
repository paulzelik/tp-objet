package fr.imt.projet.personnes;

public interface Classable {
    int getClassement();

    void classer(int classement);

    default boolean estClasse() {
        return getClassement() > 0;
    }
}
