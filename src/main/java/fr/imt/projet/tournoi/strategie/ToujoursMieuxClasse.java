package fr.imt.projet.tournoi.strategie;

import fr.imt.projet.jeux.Equipe;
import fr.imt.projet.jeux.Match;
import fr.imt.projet.personne.Coach;

import java.util.Random;

public class ToujoursMieuxClasse implements StrategieVainqueur {
    @Override
    public Equipe designerVainqueur(Match match, Random random) {
        Equipe equipe1 = match.getEquipe1();
        Equipe equipe2 = match.getEquipe2();
        int classement1 = classement(equipe1.getCoach());
        int classement2 = classement(equipe2.getCoach());
        if (classement1 > classement2) {
            return equipe1;
        } else if (classement2 > classement1) {
            return equipe2;
        }
        return random.nextBoolean() ? equipe1 : equipe2;
    }

    private static int classement(Coach coach) {
        return (coach != null && coach.estClasse()) ? coach.getClassement() : 0;
    }
}

