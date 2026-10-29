package fr.imt.projet.rapports;

import java.util.List;

/**
 * Accès aux rapports archivés (patron <b>Dépôt / Repository</b>).
 * Le reste du code ne dépend que de cette interface, pas de JPA.
 */
public interface RapportRepository extends AutoCloseable {
    void sauvegarder(RapportTournoi rapport);

    /** Tous les rapports, par date puis nom. */
    List<RapportTournoi> trouverTous();

    /** Les rapports d'un jeu donné (casse ignorée). */
    List<RapportTournoi> trouverParJeu(String jeu);

    @Override
    void close();
}
