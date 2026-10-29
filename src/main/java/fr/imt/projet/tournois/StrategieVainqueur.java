package fr.imt.projet.tournois;

import fr.imt.projet.personnes.Classable;
import fr.imt.projet.personnes.Equipe;
import java.util.Random;

/**
 * Patron <b>Stratégie</b> : la façon de désigner le vainqueur d'un match simulé.
 * <p>
 * Le vainqueur peut être tiré équitablement, en favorisant l'équipe la mieux classée, ou être
 * toujours la mieux classée (tests déterministes). Le simulateur ne connaît que cette
 * interface : on change de stratégie sans toucher au match ni au simulateur.
 */
@FunctionalInterface
public interface StrategieVainqueur {
    /** Rang attribué à une équipe pas encore classée (derrière toutes les autres). */
    int RANG_NON_CLASSE = 10;

    /** @return {@code equipe1} ou {@code equipe2} */
    Equipe designerVainqueur(Equipe equipe1, Equipe equipe2, Random random);

    /** Pile ou face. */
    static StrategieVainqueur equitable() {
        return (equipe1, equipe2, random) -> random.nextBoolean() ? equipe1 : equipe2;
    }

    /**
     * Tirage pondéré par le classement : P(équipe 1) = rang2 / (rang1 + rang2).
     * Exemple : 1re contre 3e → 75 % pour la 1re. Deux équipes non classées : 50 %.
     */
    static StrategieVainqueur favoriserMieuxClassee() {
        return (equipe1, equipe2, random) ->
                random.nextDouble() < probabiliteVictoire(equipe1, equipe2) ? equipe1 : equipe2;
    }

    /** Déterministe : la mieux classée gagne toujours (à rang égal, la première). */
    static StrategieVainqueur toujoursMieuxClassee() {
        return (equipe1, equipe2, random) -> rang(equipe2) < rang(equipe1) ? equipe2 : equipe1;
    }

    static double probabiliteVictoire(Classable equipe1, Classable equipe2) {
        double rang1 = rang(equipe1);
        double rang2 = rang(equipe2);
        return rang2 / (rang1 + rang2);
    }

    private static int rang(Classable classable) {
        return classable.estClasse() ? classable.getClassement() : RANG_NON_CLASSE;
    }
}
