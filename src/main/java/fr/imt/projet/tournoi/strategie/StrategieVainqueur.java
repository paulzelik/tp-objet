package fr.imt.projet.tournoi.strategie;

import fr.imt.projet.jeux.Equipe;
import fr.imt.projet.jeux.Match;

import java.util.Random;

public interface StrategieVainqueur {
    Equipe designerVainqueur(Match match, Random random);
}

