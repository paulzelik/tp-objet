package fr.imt.projet;

public class RapportTournoi {
    private final String nom;
    private final String jeu;
    private final int matchsJoues;
    private final String vainqueur;
    private final boolean reussi;

    RapportTournoi(String nom, String jeu, int matchsJoues, String vainqueur, boolean reussi) {
        this.nom = nom;
        this.jeu = jeu;
        this.matchsJoues = matchsJoues;
        this.vainqueur = vainqueur;
        this.reussi = reussi;
    }

    public String getNom() {
        return nom;
    }

    public String getJeu() {
        return jeu;
    }

    public int getMatchsJoues() {
        return matchsJoues;
    }

    public String getVainqueur() {
        return vainqueur;
    }

    public boolean estReussi() {
        return reussi;
    }
}
