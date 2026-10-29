package fr.imt.projet;

import fr.imt.projet.jeux.Equipe;
import fr.imt.projet.personne.Personne;
import fr.imt.projet.rapports.RapportTournoi;
import fr.imt.projet.registre.RegistreEquipes;
import fr.imt.projet.registre.RegistrePersonnes;
import fr.imt.projet.tournoi.GestionnaireTournois;
import fr.imt.projet.tournoi.PlanificateurTournois;
import fr.imt.projet.tournoi.Tournoi;

import java.util.concurrent.Future;

public class Arena implements AutoCloseable {
    private final RegistrePersonnes registrePersonnes;
    private final RegistreEquipes registreEquipes;
    private final GestionnaireTournois gestionnaireTournois;
    private final PlanificateurTournois planificateurTournois;

    public Arena() {
        this(new GestionnaireTournois());
    }

    public Arena(GestionnaireTournois gestionnaireTournois) {
        this.registrePersonnes = new RegistrePersonnes();
        this.registreEquipes = new RegistreEquipes();
        this.gestionnaireTournois = gestionnaireTournois;
        this.planificateurTournois = new PlanificateurTournois(gestionnaireTournois);
    }

    public RegistrePersonnes getRegistrePersonnes() {
        return registrePersonnes;
    }

    public RegistreEquipes getRegistreEquipes() {
        return registreEquipes;
    }

    public PlanificateurTournois getPlanificateur() {
        return planificateurTournois;
    }

    public void enregistrerPersonne(Personne personne) {
        registrePersonnes.enregistrer(personne);
    }

    public void enregistrerEquipe(Equipe equipe) {
        registreEquipes.enregistrer(equipe);
    }

    public void planifierTournoi(Tournoi tournoi) {
        planificateurTournois.planifier(tournoi);
    }

    public Future<RapportTournoi> simulerProchainTournoi() {
        return planificateurTournois.simulerProchain();
    }

    @Override
    public void close() {
        gestionnaireTournois.close();
    }
}

