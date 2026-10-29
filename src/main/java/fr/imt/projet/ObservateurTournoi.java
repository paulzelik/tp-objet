package fr.imt.projet;

/**
 * Patron <b>Observateur</b> : être prévenu de ce qui se passe dans un tournoi.
 * <p>
 * Quand un match se termine ou qu'un incident survient, plusieurs éléments doivent réagir
 * (gestionnaire, journal, affichage…) sans que le match ni le tournoi ne les connaissent : le
 * tournoi ne manipule que cette interface. Méthodes vides par défaut : chaque observateur ne
 * redéfinit que ce qui l'intéresse.
 * <p>
 * Les notifications partent du thread de simulation, <em>après</em> libération du verrou du
 * tournoi (pas d'interblocage). Une exception d'un observateur n'interrompt jamais le tournoi.
 */
public interface ObservateurTournoi {
    default void tournoiDemarre(Tournoi tournoi) {
    }

    default void matchTermine(Tournoi tournoi, Match match) {
    }

    default void incidentTechnique(Tournoi tournoi, Match match) {
    }

    default void tournoiTermine(Tournoi tournoi, RapportTournoi rapport) {
    }
}
