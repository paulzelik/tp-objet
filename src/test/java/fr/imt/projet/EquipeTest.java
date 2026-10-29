package fr.imt.projet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Les règles de composition sont garanties par les objets eux-mêmes. */
class EquipeTest {
    private Joueur joueur;
    private Equipe alpha;
    private Equipe beta;

    @BeforeEach
    void preparer() {
        joueur = joueur(1);
        alpha = new Equipe("Alpha", coach(10), joueur);
        beta = new Equipe("Beta", coach(11));
    }

    @Test
    void aPlayerBelongsToOneTeamAtATime() {
        assertThrows(IllegalStateException.class, () -> beta.recruter(joueur));
        assertSame(alpha, joueur.getEquipe());

        alpha.liberer(joueur);
        assertNull(joueur.getEquipe());
        beta.recruter(joueur);
        assertSame(beta, joueur.getEquipe());
    }

    @Test
    void aCoachLeadsOneTeamAtATime() {
        Coach coach = alpha.getCoach();
        assertThrows(IllegalStateException.class, () -> new Equipe("Gamma", coach));

        Coach nouveau = coach(12);
        alpha.changerCoach(nouveau);
        assertNull(coach.getEquipe());
        assertSame(alpha, nouveau.getEquipe());
    }

    @Test
    void theRosterIsReadOnlyFromOutside() {
        assertThrows(UnsupportedOperationException.class, () -> alpha.getJoueurs().clear());
        assertEquals(1, alpha.getJoueurs().size());
    }

    @ParameterizedTest(name = "après {0} victoire(s) : {1}")
    @CsvSource({"0, AMATEUR", "2, AMATEUR", "3, PRO", "5, PRO"})
    void anAmateurBecomesProAfterThreeWinsOfHisTeam(int victoires, Joueur.Statut attendu) {
        for (int i = 0; i < victoires; i++) {
            alpha.enregistrerVictoire();
        }
        assertEquals(attendu, joueur.getStatut());
        assertEquals(victoires * 3, alpha.getNombreDePoints());
    }

    @Test
    void changingTeamResetsTheWinCounter() {
        alpha.enregistrerVictoire();
        alpha.enregistrerVictoire();
        alpha.liberer(joueur);
        beta.recruter(joueur);
        beta.enregistrerVictoire();
        assertEquals(Joueur.Statut.AMATEUR, joueur.getStatut());
        assertEquals(1, joueur.getVictoiresDepuisArrivee());
    }

    static Joueur joueur(long id) {
        return new Joueur(id, "Nom", "Prenom", "pseudo" + id, "F", "0600000000", Joueur.Statut.AMATEUR);
    }

    static Coach coach(long id) {
        return new Coach(id, "Coach", "Prenom", "coach" + id, "M", "0600000000");
    }
}
