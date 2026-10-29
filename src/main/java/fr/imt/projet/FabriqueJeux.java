package fr.imt.projet;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Patron <b>Fabrique</b> : créer un jeu à partir de son simple nom.
 * <p>
 * Sans fabrique, chaque appelant devrait connaître la classe et les paramètres de chaque jeu
 * ({@code new JeuxEquipe("Valorant", 3, 5)}) et ce code serait répété partout. Le catalogue
 * associe un nom à une recette ; {@link #enregistrer(String, Supplier)} permet d'ajouter un jeu
 * (même d'un nouveau type) sans modifier cette classe.
 * <p>
 * {@link TreeMap} avec {@link String#CASE_INSENSITIVE_ORDER} : « valorant » = « Valorant »,
 * et le catalogue s'affiche trié. Un même nom renvoie toujours la même instance.
 */
public class FabriqueJeux {
    private final Map<String, Supplier<? extends Jeux>> catalogue = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final Map<String, Jeux> instances = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    /** Fabrique pré-remplie avec les jeux habituels de l'association. */
    public static FabriqueJeux avecCatalogueParDefaut() {
        return new FabriqueJeux()
                .enregistrer("Rocket League", () -> new JeuxEquipe("Rocket League", 5, 3))
                .enregistrer("Valorant", () -> new JeuxEquipe("Valorant", 3, 5))
                .enregistrer("League of Legends", () -> new JeuxEquipe("League of Legends", 3, 5))
                .enregistrer("Street Fighter 6", () -> new JeuxSolo("Street Fighter 6", 3))
                .enregistrer("Tekken 8", () -> new JeuxSolo("Tekken 8", 3))
                .enregistrer("Hearthstone", () -> new JeuxSolo("Hearthstone", 5));
    }

    public synchronized FabriqueJeux enregistrer(String nom, Supplier<? extends Jeux> recette) {
        String cle = Personne.texteValide(nom, "nom du jeu");
        catalogue.put(cle, Objects.requireNonNull(recette, "recette ne peut pas être null"));
        instances.remove(cle);
        return this;
    }

    /** @throws IllegalArgumentException si le jeu n'est pas au catalogue */
    public synchronized Jeux creer(String nom) {
        String cle = Personne.texteValide(nom, "nom du jeu");
        Supplier<? extends Jeux> recette = catalogue.get(cle);
        if (recette == null) {
            throw new IllegalArgumentException("Jeu inconnu : " + cle + ". Jeux disponibles : " + catalogue.keySet());
        }
        return instances.computeIfAbsent(cle, k -> recette.get());
    }

    public synchronized SortedSet<String> getJeuxDisponibles() {
        SortedSet<String> noms = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        noms.addAll(catalogue.keySet());
        return Collections.unmodifiableSortedSet(noms);
    }
}
