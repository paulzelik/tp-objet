package fr.imt.projet.tournoi.constructeur;

import fr.imt.projet.exceptions.TournoiInvalideException;
import fr.imt.projet.jeux.Equipe;
import fr.imt.projet.jeux.Jeux;
import fr.imt.projet.personne.Arbitre;
import fr.imt.projet.tournoi.Tournoi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TournoiBuilder {
    private String nom;
    private Jeux jeu;
    private LocalDate date;
    private BigDecimal budget;
    private int arenes = 1;
    private final List<Equipe> equipes = new ArrayList<>();
    private final List<Arbitre> arbitres = new ArrayList<>();

    public TournoiBuilder nom(String nom) {
        this.nom = nom;
        return this;
    }

    public TournoiBuilder jeu(Jeux jeu) {
        this.jeu = jeu;
        return this;
    }

    public TournoiBuilder date(LocalDate date) {
        this.date = date;
        return this;
    }

    public TournoiBuilder budget(BigDecimal budget) {
        this.budget = budget;
        return this;
    }

    public TournoiBuilder arenes(int arenes) {
        this.arenes = arenes;
        return this;
    }

    public TournoiBuilder ajouterEquipe(Equipe equipe) {
        if (equipe != null) {
            this.equipes.add(equipe);
        }
        return this;
    }

    public TournoiBuilder ajouterArbitre(Arbitre arbitre) {
        if (arbitre != null) {
            this.arbitres.add(arbitre);
        }
        return this;
    }

    public Tournoi build() {
        if (nom == null || nom.isBlank()) {
            throw new TournoiInvalideException("Le nom du tournoi ne peut pas être vide");
        }
        if (jeu == null) {
            throw new TournoiInvalideException("Le jeu doit être renseigné");
        }
        if (date == null) {
            this.date = LocalDate.now();
        }
        if (budget == null) {
            this.budget = BigDecimal.ZERO;
        }
        if (arenes < 1 || arenes > 4) {
            throw new TournoiInvalideException("Les arènes doivent être comprises entre 1 et 4");
        }

        Tournoi tournoi = new Tournoi(nom, jeu, date, budget, arenes);
        for (Equipe e : equipes) {
            tournoi.inscrireEquipe(e);
        }
        for (Arbitre a : arbitres) {
            tournoi.inscrireArbitre(a);
        }
        return tournoi;
    }
}

