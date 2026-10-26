package fr.imt.projet;

public class JeuxEquipe extends Jeux {
    public JeuxEquipe(String nom, int nombreManchesParMatch, int nombreJoueursParEquipe) {
        super(nom, nombreManchesParMatch, validerNombreJoueurs(nombreJoueursParEquipe));
    }

    private static int validerNombreJoueurs(int nombreJoueursParEquipe) {
        if (nombreJoueursParEquipe < 3 || nombreJoueursParEquipe > 5) {
            throw new IllegalArgumentException("Un jeu en équipe doit aligner de 3 à 5 joueurs");
        }
        return nombreJoueursParEquipe;
    }
}
