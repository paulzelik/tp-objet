package fr.imt.projet;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JeuxTest {
    @Test
    void individualGameStoresNameAndRounds() {
        Jeux jeu = new JeuxSolo("Tennis", 3);

        assertEquals("Tennis", jeu.getNom());
        assertEquals(3, jeu.getNombreManchesParMatch());
        assertEquals(1, jeu.getNombreJoueursParEquipe());
    }

    @Test
    void teamGameStoresNameAndRounds() {
        Jeux jeu = new JeuxEquipe("Basketball", 4, 5);

        assertEquals("Basketball", jeu.getNom());
        assertEquals(4, jeu.getNombreManchesParMatch());
        assertEquals(5, jeu.getNombreJoueursParEquipe());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new JeuxSolo(" ", 3));
    }

    @Test
    void rejectsNonPositiveRoundCount() {
        assertThrows(IllegalArgumentException.class, () -> new JeuxEquipe("Basketball", 0, 3));
    }

    @Test
    void rejectsTeamSizeOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class, () -> new JeuxEquipe("Basketball", 4, 2));
        assertThrows(IllegalArgumentException.class, () -> new JeuxEquipe("Basketball", 4, 6));
    }

    @Test
    void soloGameSelectsOnlyAPlayerFromTheTeam() {
        Joueur joueur = new Joueur(
                1, "Nom", "Prenom", "pseudo", "genre", "telephone", "Equipe", Joueur.Statut.AMATEUR);
        Joueur autreJoueur = new Joueur(
                2, "Autre", "Prenom", "pseudo2", "genre", "telephone", "Equipe", Joueur.Statut.AMATEUR);

        assertEquals(joueur, new JeuxSolo("Tennis", 3).selectionnerJoueur(List.of(joueur, autreJoueur), joueur));
        assertThrows(
                IllegalArgumentException.class,
                () -> new JeuxSolo("Tennis", 3).selectionnerJoueur(List.of(joueur), autreJoueur));
    }

    @Test
    void newGameTypesCanExtendTheBaseClassWithoutChangingExistingTypes() {
        Jeux jeu = new Jeux("Jeu personnalisé", 2, 2) { };

        assertEquals("Jeu personnalisé", jeu.getNom());
        assertEquals(2, jeu.getNombreJoueursParEquipe());
    }

    @Test
    void playerAndCoachShareTheClassableCapability() {
        Classable joueur = new Joueur(
                1, "Nom", "Prenom", "pseudo", "genre", "telephone", "Equipe", Joueur.Statut.AMATEUR);
        Classable coach = new Coach(2, "Coach", "Prenom", "pseudo", "genre", "telephone", "Equipe");

        assertFalse(joueur.estClasse());
        joueur.classer(1);
        coach.classer(2);
        assertTrue(joueur.estClasse());
        assertEquals(1, joueur.getClassement());
        assertEquals(2, coach.getClassement());
        assertThrows(IllegalArgumentException.class, () -> coach.classer(0));
    }

    @Test
    void refereeExposesQualificationAsACapability() {
        QualifiablePourJeu arbitre =
                new Arbitre(1, "Nom", "Prenom", "pseudo", "genre", "telephone");

        assertFalse(arbitre.estQualifiePour("Tennis"));
        arbitre.qualifierPourJeu("Tennis");
        assertTrue(arbitre.estQualifiePour("Tennis"));
    }
}
