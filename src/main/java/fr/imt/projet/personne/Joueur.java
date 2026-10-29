package fr.imt.projet.personne;

import fr.imt.projet.exceptions.RegleMetierException;
import fr.imt.projet.jeux.Equipe;

import java.util.Objects;

public class Joueur extends Personne implements Classable {
    public enum Statut {
        AMATEUR,
        PRO
    }

    private String equipe;
    private Equipe equipeObjet;
    private Statut statut;
    private int victoiresDepuisArrivee;
    private int classement;

    public Joueur(
            long id,
            String nom,
            String prenom,
            String pseudo,
            String genre,
            String telephone,
            String equipe,
            Statut statut) {
        super(id, nom, prenom, pseudo, genre, telephone);
        this.equipe = nomValide(equipe, "equipe");
        this.statut = Objects.requireNonNull(statut, "statut ne peut pas être null");
    }

    public Joueur(
            long id,
            String nom,
            String prenom,
            String pseudo,
            String genre,
            String telephone,
            Statut statut) {
        super(id, nom, prenom, pseudo, genre, telephone);
        this.statut = Objects.requireNonNull(statut, "statut ne peut pas être null");
    }

    public String getEquipe() {
        return equipe;
    }

    public Equipe getEquipeObjet() {
        return equipeObjet;
    }

    public Statut getStatut() {
        return statut;
    }

    public int getVictoiresDepuisArrivee() {
        return victoiresDepuisArrivee;
    }

    @Override
    public int getClassement() {
        return classement;
    }

    @Override
    public void classer(int classement) {
        if (classement <= 0) {
            throw new RegleMetierException("Le classement doit être positif");
        }
        this.classement = classement;
    }

    public void changerEquipe(String nouvelleEquipe) {
        this.equipe = nomValide(nouvelleEquipe, "equipe");
        this.victoiresDepuisArrivee = 0;
    }

    public void assignerEquipe(Equipe nouvelleEquipe) {
        this.equipeObjet = nouvelleEquipe;
        this.equipe = nouvelleEquipe != null ? nouvelleEquipe.getNom() : null;
        this.victoiresDepuisArrivee = 0;
    }

    public void quitterEquipe() {
        this.equipeObjet = null;
        this.equipe = null;
        this.victoiresDepuisArrivee = 0;
    }

    public void enregistrerVictoireEquipe() {
        victoiresDepuisArrivee++;
        if (statut == Statut.AMATEUR && victoiresDepuisArrivee >= 3) {
            statut = Statut.PRO;
        }
    }

    private static String nomValide(String valeur, String champ) {
        Objects.requireNonNull(valeur, champ + " ne peut pas être null");
        if (valeur.isBlank()) {
            throw new RegleMetierException(champ + " ne peut pas être vide");
        }
        return valeur;
    }
}
