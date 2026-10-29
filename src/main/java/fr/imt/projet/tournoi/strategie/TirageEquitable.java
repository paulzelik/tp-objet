package fr.imt.projet.tournoi.strategie;

import fr.imt.projet.jeux.Equipe;
import fr.imt.projet.jeux.Match;

import java.util.Random;

public class TirageEquitable implements StrategieVainqueur {
    @Override
    public Equipe designerVainqueur(Match match, Random random) {
        return random.nextBoolean() ? match.getEquipe1() : match.getEquipe2();
    }
}

