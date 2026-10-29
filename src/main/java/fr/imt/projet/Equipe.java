package fr.imt.projet;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Équipe : un nom, un coach, des joueurs et un total de points (3 par victoire, pas de nul).
 * <p>
 * L'équipe garantit les règles de composition :
 * <ul>
 *     <li>un coach obligatoire, qui ne dirige qu'une équipe ;</li>
 *     <li>un joueur n'est que dans une équipe à la fois ;</li>
 *     <li>la composition est gelée pendant un tournoi en cours ;</li>
 *     <li>les points ne s'obtiennent que par une victoire en match (pas de setter public).</li>
 * </ul>
 * Effectif dans une {@link ArrayList} : petite taille, ordre d'inscription, accès par position.
 * L'équipe est {@link Classable} : son rang au classement général est mis à jour à chaque fin de tournoi.
 */
public final class Equipe implements Classable {
    public static final int POINTS_PAR_VICTOIRE = 3;

    private final String nom;
    private final List<Joueur> joueurs = new ArrayList<>();
    private Coach coach;
    private int nombreDePoints;
    private int classement;
    private Tournoi tournoiEnCours;

    public Equipe(String nom, Coach coach, Joueur... joueurs) {
        this.nom = Personne.texteValide(nom, "nom");
        Objects.requireNonNull(coach, "coach ne peut pas être null");
        coach.prendreEnCharge(this);
        this.coach = coach;
        for (Joueur joueur : joueurs) {
            recruter(joueur);
        }
    }

    public String getNom() {
        return nom;
    }

    public synchronized Coach getCoach() {
        return coach;
    }

    /** Copie non modifiable de l'effectif. */
    public synchronized List<Joueur> getJoueurs() {
        return List.copyOf(joueurs);
    }

    public synchronized int getNombreDePoints() {
        return nombreDePoints;
    }

    public synchronized boolean estEngagee() {
        return tournoiEnCours != null;
    }

    public synchronized void recruter(Joueur joueur) {
        Objects.requireNonNull(joueur, "un joueur ne peut pas être null");
        verifierModifiable();
        joueur.rejoindre(this);
        joueurs.add(joueur);
    }

    public synchronized void liberer(Joueur joueur) {
        verifierModifiable();
        if (!joueurs.remove(joueur)) {
            throw new IllegalArgumentException(joueur.getPseudo() + " ne fait pas partie de « " + nom + " »");
        }
        joueur.quitter(this);
    }

    public synchronized void changerCoach(Coach nouveauCoach) {
        Objects.requireNonNull(nouveauCoach, "coach ne peut pas être null");
        verifierModifiable();
        if (nouveauCoach != coach) {
            nouveauCoach.prendreEnCharge(this);
            coach.quitter(this);
            coach = nouveauCoach;
        }
    }

    @Override
    public synchronized int getClassement() {
        return classement;
    }

    @Override
    public synchronized void classer(int classement) {
        if (classement <= 0) {
            throw new IllegalArgumentException("Le classement doit être positif");
        }
        this.classement = classement;
    }

    /** Victoire en match : +3 points, une victoire de plus pour le coach et pour chaque joueur. */
    synchronized void enregistrerVictoire() {
        nombreDePoints = Math.addExact(nombreDePoints, POINTS_PAR_VICTOIRE);
        coach.enregistrerVictoireEquipe();
        for (Joueur joueur : joueurs) {
            joueur.enregistrerVictoireEquipe();
        }
    }

    synchronized void engager(Tournoi tournoi) {
        if (tournoiEnCours != null) {
            throw new IllegalStateException("« " + nom + " » dispute déjà « " + tournoiEnCours.getNom() + " »");
        }
        tournoiEnCours = tournoi;
    }

    synchronized void desengager(Tournoi tournoi) {
        if (tournoiEnCours == tournoi) {
            tournoiEnCours = null;
        }
    }

    private void verifierModifiable() {
        if (tournoiEnCours != null) {
            throw new IllegalStateException("Composition de « " + nom + " » gelée : tournoi « "
                    + tournoiEnCours.getNom() + " » en cours");
        }
    }

    @Override
    public String toString() {
        return nom;
    }
}
