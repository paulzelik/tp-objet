package fr.imt.projet.rapports;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "tournament_reports")
public class RapportTournoi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_name", nullable = false)
    private String nom;

    @Column(name = "tournament_date", nullable = false)
    private LocalDate date;

    @Column(name = "game_name", nullable = false)
    private String jeu;

    @Column(name = "planned_matches", nullable = false)
    private int matchsPlanifies;

    @Column(name = "played_matches", nullable = false)
    private int matchsJoues;

    @Column(name = "winner_team_name")
    private String vainqueur;

    @Column(nullable = false)
    private boolean reussi;

    protected RapportTournoi() {
        
    }

    public RapportTournoi(
            String nom,
            LocalDate date,
            String jeu,
            int matchsPlanifies,
            int matchsJoues,
            String vainqueur,
            boolean reussi) {
        this.nom = texteValide(nom, "nom");
        this.date = Objects.requireNonNull(date, "date ne peut pas être null");
        this.jeu = texteValide(jeu, "jeu");
        if (matchsPlanifies < 0 || matchsJoues < 0 || matchsJoues > matchsPlanifies) {
            throw new IllegalArgumentException("Le nombre de matchs joués doit être compris entre 0 et le nombre planifié");
        }
        this.matchsPlanifies = matchsPlanifies;
        this.matchsJoues = matchsJoues;
        this.vainqueur = vainqueur;
        this.reussi = reussi;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getJeu() {
        return jeu;
    }

    public int getMatchsPlanifies() {
        return matchsPlanifies;
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

    private static String texteValide(String valeur, String champ) {
        Objects.requireNonNull(valeur, champ + " ne peut pas être null");
        if (valeur.isBlank()) {
            throw new IllegalArgumentException(champ + " ne peut pas être vide");
        }
        return valeur;
    }
}
