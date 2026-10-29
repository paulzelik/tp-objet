package fr.imt.projet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * Rapport de fin de tournoi : seul objet persisté (entité JPA, base H2 fichier).
 * <p>
 * Créé automatiquement par le {@link Tournoi} à sa clôture, puis archivé par le
 * {@link GestionnaireTournois} (observateur de fin de tournoi). Aucun setter : une fois créé,
 * le rapport ne change plus, il peut donc être lu sans risque depuis n'importe quel thread.
 */
@Entity
@Table(name = "RAPPORT_TOURNOI")
@NamedQuery(name = RapportTournoi.TOUS,
        query = "SELECT r FROM RapportTournoi r ORDER BY r.date, r.nom")
@NamedQuery(name = RapportTournoi.PAR_JEU,
        query = "SELECT r FROM RapportTournoi r WHERE LOWER(r.jeu) = LOWER(:jeu) ORDER BY r.date, r.nom")
public class RapportTournoi {
    static final String TOUS = "RapportTournoi.tous";
    static final String PAR_JEU = "RapportTournoi.parJeu";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NOM_TOURNOI", nullable = false)
    private String nom;

    @Column(name = "DATE_TOURNOI", nullable = false)
    private LocalDate date;

    @Column(name = "JEU", nullable = false)
    private String jeu;

    @Column(name = "MATCHS_PREVUS", nullable = false)
    private int matchsPrevus;

    @Column(name = "MATCHS_JOUES", nullable = false)
    private int matchsJoues;

    @Column(name = "EQUIPE_VAINQUEUR")
    private String vainqueur;

    @Column(name = "REUSSI", nullable = false)
    private boolean reussi;

    /** Exigé par JPA. */
    protected RapportTournoi() {
    }

    RapportTournoi(String nom, LocalDate date, String jeu, int matchsPrevus, int matchsJoues,
                   String vainqueur, boolean reussi) {
        this.nom = nom;
        this.date = date;
        this.jeu = jeu;
        this.matchsPrevus = matchsPrevus;
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

    public int getMatchsPrevus() {
        return matchsPrevus;
    }

    public int getMatchsJoues() {
        return matchsJoues;
    }

    /** Nom de l'équipe vainqueur, ou {@code null} s'il n'y en a pas. */
    public String getVainqueur() {
        return vainqueur;
    }

    public boolean estReussi() {
        return reussi;
    }

    @Override
    public String toString() {
        return "%s (%s, %s) : %d/%d matchs joués, vainqueur : %s, %s".formatted(
                nom, jeu, date, matchsJoues, matchsPrevus, vainqueur == null ? "aucun" : vainqueur,
                reussi ? "RÉUSSI" : "RATÉ");
    }
}
