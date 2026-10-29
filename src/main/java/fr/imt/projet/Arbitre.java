package fr.imt.projet;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Arbitre : ne joue jamais, qualifié pour un ou plusieurs jeux, compte ses matchs arbitrés.
 * <p>
 * Jeux qualifiés : {@link TreeSet} (pas de doublon, liste triée à l'affichage). Un
 * {@code EnumSet} imposerait une énumération figée des jeux, contraire à l'ajout de nouveaux
 * jeux sans modifier l'existant.
 */
public class Arbitre extends Personne implements QualifiablePourJeu {
    /** Easter egg : les arbitres ne servent à rien, mais ils sifflent quand même. */
    private static final List<String> COUPS_DE_SIFFLET = List.of(
            "Priiiiit !",
            "Prrrt prrrt ! (personne n'a rien fait, c'est pour le principe)",
            "Fwiiiiit ! Carton jaune pour le café froid.");

    private final SortedSet<String> jeuxQualifies = new TreeSet<>();
    private final AtomicInteger matchsArbitres = new AtomicInteger();

    public Arbitre(long id, String nom, String prenom, String pseudo, String genre, String telephone) {
        super(id, nom, prenom, pseudo, genre, telephone);
    }

    @Override
    public synchronized Set<String> getJeuxQualifies() {
        return Collections.unmodifiableSortedSet(new TreeSet<>(jeuxQualifies));
    }

    public int getMatchsArbitres() {
        return matchsArbitres.get();
    }

    @Override
    public synchronized void qualifierPourJeu(String jeu) {
        jeuxQualifies.add(texteValide(jeu, "jeu"));
    }

    /** Comptabilise un match ; l'arbitre doit être qualifié pour son jeu. */
    void arbitrerMatch(String jeu) {
        String jeuValide = texteValide(jeu, "jeu");
        if (!estQualifiePour(jeuValide)) {
            throw new IllegalArgumentException("L'arbitre n'est pas qualifié pour le jeu : " + jeuValide);
        }
        matchsArbitres.incrementAndGet();
    }

    public String siffler() {
        return getPseudo() + " : " + COUPS_DE_SIFFLET.get(getMatchsArbitres() % COUPS_DE_SIFFLET.size());
    }
}
