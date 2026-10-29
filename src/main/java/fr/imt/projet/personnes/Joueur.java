package fr.imt.projet.personnes;

import java.util.Objects;

/**
 * Joueur : appartient à une seule équipe à la fois, amateur ou pro.
 * <p>
 * L'équipe n'est plus un simple nom (String) fixé librement : c'est une référence vers
 * l'{@link Equipe}, posée uniquement par {@link Equipe#recruter(Joueur)}. Il devient ainsi
 * impossible de placer un joueur dans deux équipes.
 */
public class Joueur extends Personne implements Classable {
    public static final int VICTOIRES_POUR_PASSER_PRO = 3;

    public enum Statut {
        AMATEUR,
        PRO
    }

    private Equipe equipe;
    private Statut statut;
    private int victoiresDepuisArrivee;
    private int classement;

    public Joueur(long id, String nom, String prenom, String pseudo, String genre, String telephone, Statut statut) {
        super(id, nom, prenom, pseudo, genre, telephone);
        this.statut = Objects.requireNonNull(statut, "statut ne peut pas être null");
    }

    /** L'équipe actuelle, ou {@code null} si le joueur est libre. */
    public synchronized Equipe getEquipe() {
        return equipe;
    }

    public synchronized Statut getStatut() {
        return statut;
    }

    public synchronized int getVictoiresDepuisArrivee() {
        return victoiresDepuisArrivee;
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

    /** Appelé uniquement par {@link Equipe#recruter(Joueur)}. */
    public synchronized void rejoindre(Equipe nouvelleEquipe) {
        if (equipe != null) {
            throw new IllegalStateException(getPseudo() + " joue déjà pour « " + equipe.getNom() + " »");
        }
        equipe = Objects.requireNonNull(nouvelleEquipe);
        victoiresDepuisArrivee = 0;
    }

    /** Appelé uniquement par {@link Equipe#liberer(Joueur)}. */
    public synchronized void quitter(Equipe ancienneEquipe) {
        if (equipe != ancienneEquipe) {
            throw new IllegalStateException(getPseudo() + " n'appartient pas à cette équipe");
        }
        equipe = null;
        victoiresDepuisArrivee = 0;
    }

    /** Un amateur passe pro quand son équipe a gagné 3 matchs depuis son arrivée. */
    public synchronized void enregistrerVictoireEquipe() {
        victoiresDepuisArrivee++;
        if (statut == Statut.AMATEUR && victoiresDepuisArrivee >= VICTOIRES_POUR_PASSER_PRO) {
            statut = Statut.PRO;
        }
    }
}
