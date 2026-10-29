package fr.imt.projet;

import fr.imt.projet.exceptions.RegleMetierException;
import fr.imt.projet.jeux.Jeux;
import fr.imt.projet.jeux.JeuxEquipe;
import fr.imt.projet.jeux.JeuxSolo;
import fr.imt.projet.jeux.factory.JeuFactory;
import fr.imt.projet.personne.Joueur;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JeuxTest {
    private Joueur participant;
    private Joueur outsider;

    @BeforeEach
    void setUp() {
        participant = new Joueur(1, "Name", "First", "player", "X", "555", "Team", Joueur.Statut.AMATEUR);
        outsider = new Joueur(2, "Other", "First", "other", "X", "555", "Other", Joueur.Statut.AMATEUR);
    }

    @Test
    void testValidationConfigurationJeuEtFactory() {
        JeuFactory.enregistrerJeu("Basketball", () -> new JeuxEquipe("Basketball", 4, 5));
        Jeux teamGame = JeuFactory.creerJeu("Basketball");

        assertEquals("Basketball", teamGame.getNom());
        assertEquals(5, teamGame.getNombreJoueursParEquipe());
        assertThrows(RegleMetierException.class, () -> new JeuxEquipe("Basketball", 4, 2));
        assertThrows(RegleMetierException.class, () -> new JeuxSolo(" ", 3));

        JeuxSolo soloGame = new JeuxSolo("Tennis", 3);
        assertEquals(participant, soloGame.selectionnerJoueur(List.of(participant), participant));
        assertThrows(
                RegleMetierException.class,
                () -> soloGame.selectionnerJoueur(List.of(participant), outsider));
        assertTrue(soloGame.getNombreManchesParMatch() > 0);
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5})
    void testJeuxEquipeNombreJoueursValides(int nbJoueurs) {
        JeuxEquipe jeu = new JeuxEquipe("Volley" + nbJoueurs, 3, nbJoueurs);
        assertNotNull(jeu);
        assertEquals(nbJoueurs, jeu.getNombreJoueursParEquipe());
    }
}
