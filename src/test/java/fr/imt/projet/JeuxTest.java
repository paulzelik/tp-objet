package fr.imt.projet;

import fr.imt.projet.jeux.Jeux;
import fr.imt.projet.jeux.JeuxEquipe;
import fr.imt.projet.jeux.JeuxSolo;
import fr.imt.projet.personne.Joueur;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeuxTest {
    @Test
    void validatesGameConfigurationAndSoloPlayerSelection() {
        Jeux teamGame = new JeuxEquipe("Basketball", 4, 5);
        assertEquals("Basketball", teamGame.getNom());
        assertEquals(5, teamGame.getNombreJoueursParEquipe());
        assertThrows(IllegalArgumentException.class, () -> new JeuxEquipe("Basketball", 4, 2));
        assertThrows(IllegalArgumentException.class, () -> new JeuxSolo(" ", 3));

        Joueur participant = new Joueur(
                1, "Name", "First", "player", "X", "555", "Team", Joueur.Statut.AMATEUR);
        Joueur outsider = new Joueur(
                2, "Other", "First", "other", "X", "555", "Other", Joueur.Statut.AMATEUR);
        JeuxSolo soloGame = new JeuxSolo("Tennis", 3);
        assertEquals(participant, soloGame.selectionnerJoueur(List.of(participant), participant));
        assertThrows(
                IllegalArgumentException.class,
                () -> soloGame.selectionnerJoueur(List.of(participant), outsider));
        assertTrue(soloGame.getNombreManchesParMatch() > 0);
    }
}
