package fr.imt.projet.jeux;

import fr.imt.projet.personne.Coach;
import fr.imt.projet.personne.Joueur;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Equipe {
    private String nom;
    private Coach coach;
    private List<Joueur> joueurs;
    private int nombreDePoints;

    public Equipe(String nom, Coach coach, List<Joueur> joueurs, int nombreDePoints) {
        setNom(nom);
        setCoach(coach);
        setJoueurs(joueurs);
        setNombreDePoints(nombreDePoints);
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        Objects.requireNonNull(nom, "nom ne peut pas être null");
        if (nom.isBlank()) {
            throw new IllegalArgumentException("nom ne peut pas être vide");
        }
        this.nom = nom;
    }

    public Coach getCoach() {
        return coach;
    }

    public void setCoach(Coach coach) {
        this.coach = Objects.requireNonNull(coach, "coach ne peut pas être null");
    }

    public List<Joueur> getJoueurs() {
        return new ArrayList<>(joueurs);
    }

    public void setJoueurs(List<Joueur> joueurs) {
        List<Joueur> copie = new ArrayList<>(Objects.requireNonNull(joueurs, "joueurs ne peut pas être null"));
        for (Joueur joueur : copie) {
            Objects.requireNonNull(joueur, "un joueur ne peut pas être null");
        }
        this.joueurs = copie;
    }

    public int getNombreDePoints() {
        return nombreDePoints;
    }

    public void setNombreDePoints(int nombreDePoints) {
        if (nombreDePoints < 0) {
            throw new IllegalArgumentException("Le nombre de points ne peut pas être négatif");
        }
        this.nombreDePoints = nombreDePoints;
    }

    public void ajouterPoints(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("Le nombre de points à ajouter ne peut pas être négatif");
        }
        nombreDePoints = Math.addExact(nombreDePoints, points);
    }
}
