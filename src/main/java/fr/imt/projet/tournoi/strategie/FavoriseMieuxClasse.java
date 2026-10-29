package fr.imt.projet.tournoi.strategie;

import fr.imt.projet.jeux.Equipe;
import fr.imt.projet.jeux.Match;
import fr.imt.projet.personne.Coach;

import java.util.Random;

public class FavoriseMieuxClasse implements StrategieVainqueur {
    @Override
    public Equipe designerVainqueur(Match match, Random random) {
        Equipe equipe1 = match.getEquipe1();
        Equipe equipe2 = match.getEquipe2();
        int classement1 = classement(equipe1.getCoach());
        int classement2 = classement(equipe2.getCoach());
        double avantageEquipe1 = 1.0 / (1.0 + Math.pow(10.0, (classement1 - classement2) / 400.0));
        return random.nextDouble() < avantageEquipe1 ? equipe1 : equipe2;
    }

    private static int classement(Coach coach) {
        return (coach != null && coach.estClasse()) ? coach.getClassement() : 0;
    }
}

