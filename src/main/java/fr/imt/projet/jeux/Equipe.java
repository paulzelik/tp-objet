package fr.imt.projet.jeux;

import fr.imt.projet.exceptions.RegleMetierException;
import fr.imt.projet.personne.Classable;
import fr.imt.projet.personne.Coach;
import fr.imt.projet.personne.Joueur;
import fr.imt.projet.personne.Qualifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Equipe implements Classable, Qualifiable {
    private String nom;
    private Coach coach;
    private final List<Joueur> joueurs = new ArrayList<>();
    private int nombreDePoints;

    public Equipe(String nom) {
        setNom(nom);
    }

    public Equipe(String nom, Coach coach, List<Joueur> joueurs, int nombreDePoints) {
        setNom(nom);
        setCoach(coach);
        if (joueurs != null) {
            for (Joueur j : joueurs) {
                ajouterJoueur(j);
            }
        }
        setNombreDePoints(nombreDePoints);
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        Objects.requireNonNull(nom, "nom ne peut pas être null");
        if (nom.isBlank()) {
            throw new RegleMetierException("nom ne peut pas être vide");
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
        return Collections.unmodifiableList(joueurs);
    }

    public void setJoueurs(List<Joueur> nouveauxJoueurs) {
        Objects.requireNonNull(nouveauxJoueurs, "joueurs ne peut pas être null");
        for (Joueur j : new ArrayList<>(this.joueurs)) {
            retirerJoueur(j);
        }
        for (Joueur j : nouveauxJoueurs) {
            ajouterJoueur(j);
        }
    }

    public void ajouterJoueur(Joueur joueur) {
        Objects.requireNonNull(joueur, "un joueur ne peut pas être null");
        if (joueur.getEquipeObjet() != null && joueur.getEquipeObjet() != this) {
            throw new RegleMetierException("Le joueur appartient déjà à une équipe : " + joueur.getEquipeObjet().getNom());
        }
        if (joueurs.contains(joueur)) {
            return;
        }
        joueurs.add(joueur);
        joueur.assignerEquipe(this);
    }

    public void retirerJoueur(Joueur joueur) {
        if (joueurs.remove(joueur)) {
            joueur.quitterEquipe();
        }
    }

    public int getNombreDePoints() {
        return nombreDePoints;
    }

    @Override
    public int getPoints() {
        return nombreDePoints;
    }

    @Override
    public int getClassement() {
        return nombreDePoints;
    }

    @Override
    public void classer(int classement) {
        setNombreDePoints(classement);
    }

    public void setNombreDePoints(int nombreDePoints) {
        if (nombreDePoints < 0) {
            throw new RegleMetierException("Le nombre de points ne peut pas être négatif");
        }
        this.nombreDePoints = nombreDePoints;
    }

    public void ajouterPoints(int points) {
        if (points < 0) {
            throw new RegleMetierException("Le nombre de points à ajouter ne peut pas être négatif");
        }
        nombreDePoints = Math.addExact(nombreDePoints, points);
    }

    @Override
    public boolean estQualifiePour(Jeux jeu) {
        if (jeu == null || coach == null) {
            return false;
        }
        return joueurs.size() == jeu.getNombreJoueursParEquipe();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Equipe other)) return false;
        return Objects.equals(nom, other.nom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nom);
    }

    @Override
    public String toString() {
        return nom;
    }
}
