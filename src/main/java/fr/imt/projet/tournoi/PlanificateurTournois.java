package fr.imt.projet.tournoi;

import fr.imt.projet.rapports.RapportTournoi;
import fr.imt.projet.tournoi.strategie.StrategieVainqueur;

import java.util.List;
import java.util.concurrent.Future;

public class PlanificateurTournois {
    private final GestionnaireTournois gestionnaireTournois;

    public PlanificateurTournois(GestionnaireTournois gestionnaireTournois) {
        this.gestionnaireTournois = gestionnaireTournois;
    }

    public void planifier(Tournoi tournoi) {
        gestionnaireTournois.ajouterTournoi(tournoi);
    }

    public Tournoi lancerProchain() {
        return gestionnaireTournois.lancerProchainTournoi();
    }

    public Future<RapportTournoi> simulerProchain() {
        return gestionnaireTournois.simulerProchainTournoi();
    }

    public List<Tournoi> getTournoisPlanifies() {
        return gestionnaireTournois.getTournoisPlanifiesTries();
    }
}

