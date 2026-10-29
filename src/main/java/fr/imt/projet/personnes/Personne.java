package fr.imt.projet.personnes;

import java.util.Objects;

/**
 * Identité commune aux joueurs, coachs et arbitres.
 * <p>
 * Les attributs d'identité sont {@code final} et sans setter : une fois créée, une personne
 * ne peut pas être modifiée depuis l'extérieur (encapsulation). Seuls les états propres à
 * chaque rôle (équipe, statut, compteurs) évoluent, et uniquement via les objets du modèle.
 */
public abstract class Personne {
    private final long id;
    private final String nom;
    private final String prenom;
    private final String pseudo;
    private final String genre;
    private final String telephone;

    protected Personne(long id, String nom, String prenom, String pseudo, String genre, String telephone) {
        this.id = id;
        this.nom = texteValide(nom, "nom");
        this.prenom = texteValide(prenom, "prenom");
        this.pseudo = texteValide(pseudo, "pseudo");
        this.genre = texteValide(genre, "genre");
        this.telephone = texteValide(telephone, "telephone");
    }

    public long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public String getPseudo() {
        return pseudo;
    }

    public String getGenre() {
        return genre;
    }

    public String getTelephone() {
        return telephone;
    }

    @Override
    public String toString() {
        return prenom + " « " + pseudo + " » " + nom;
    }

    public static String texteValide(String valeur, String champ) {
        Objects.requireNonNull(valeur, champ + " ne peut pas être null");
        if (valeur.isBlank()) {
            throw new IllegalArgumentException(champ + " ne peut pas être vide");
        }
        return valeur;
    }
}
