package fr.imt.projet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Patron <b>Monteur (Builder)</b> : construire un tournoi en vérifiant qu'il est valide.
 * <p>
 * Un tournoi a 5 attributs obligatoires et des équipes/arbitres : le monteur offre une écriture
 * lisible et regroupe toutes les vérifications dans {@link #build()}, qui signale toutes les
 * erreurs d'un coup au lieu de s'arrêter à la première.
 *
 * <pre>{@code
 * Tournoi t = Tournoi.builder().nom("Coupe d'Hiver").jeu(rocketLeague)
 *         .date(LocalDate.of(2026, 12, 10)).budget(8000).arenes(3)
 *         .equipes(equipes).arbitre(arbitre).build();
 * }</pre>
 */
public class TournoiBuilder {
    private String nom;
    private Jeux jeu;
    private LocalDate date;
    private BigDecimal budget;
    private int arenes = 1;
    private final List<Equipe> equipes = new ArrayList<>();
    private final List<Arbitre> arbitres = new ArrayList<>();

    TournoiBuilder() {
    }

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

    public TournoiBuilder budget(long euros) {
        this.budget = BigDecimal.valueOf(euros);
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

    public TournoiBuilder equipe(Equipe equipe) {
        equipes.add(equipe);
        return this;
    }

    public TournoiBuilder equipes(Collection<Equipe> autres) {
        equipes.addAll(autres);
        return this;
    }

    public TournoiBuilder arbitre(Arbitre arbitre) {
        arbitres.add(arbitre);
        return this;
    }

    /** @throws IllegalArgumentException listant toutes les erreurs */
    public Tournoi build() {
        List<String> erreurs = new ArrayList<>();
        if (nom == null || nom.isBlank()) {
            erreurs.add("nom obligatoire");
        }
        if (jeu == null) {
            erreurs.add("jeu obligatoire");
        }
        if (date == null) {
            erreurs.add("date obligatoire");
        }
        if (budget == null || budget.signum() < 0) {
            erreurs.add("budget obligatoire et positif");
        }
        if (arenes < 1 || arenes > 4) {
            erreurs.add("de 1 à 4 arènes");
        }
        for (Arbitre arbitre : arbitres) {
            if (jeu != null && !arbitre.estQualifiePour(jeu.getNom())) {
                erreurs.add(arbitre.getPseudo() + " n'est pas qualifié pour " + jeu.getNom());
            }
        }
        if (!erreurs.isEmpty()) {
            throw new IllegalArgumentException("Tournoi invalide : " + String.join(", ", erreurs));
        }
        Tournoi tournoi = new Tournoi(nom, jeu, date, budget, arenes);
        equipes.forEach(tournoi::inscrireEquipe);
        arbitres.forEach(tournoi::inscrireArbitre);
        return tournoi;
    }
}
