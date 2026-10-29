package fr.imt.projet;

import fr.imt.projet.jeux.Equipe;
import fr.imt.projet.jeux.Jeux;
import fr.imt.projet.jeux.JeuxSolo;
import fr.imt.projet.jeux.factory.JeuFactory;
import fr.imt.projet.personne.Arbitre;
import fr.imt.projet.personne.Coach;
import fr.imt.projet.personne.Joueur;
import fr.imt.projet.rapports.RapportTournoi;
import fr.imt.projet.tournoi.GestionnaireTournois;
import fr.imt.projet.tournoi.Tournoi;
import fr.imt.projet.tournoi.constructeur.TournoiBuilder;
import fr.imt.projet.tournoi.strategie.FavoriseMieuxClasse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        JeuFactory.enregistrerJeu("Tennis", () -> new JeuxSolo("Tennis", 3));
        Jeux jeu = JeuFactory.creerJeu("Tennis");

        TournoiBuilder builder = new TournoiBuilder()
                .nom("Coupe IMT Arena")
                .jeu(jeu)
                .date(LocalDate.now())
                .budget(new BigDecimal("500.00"))
                .arenes(2);

        for (int i = 1; i <= 4; i++) {
            builder.ajouterEquipe(creerEquipe(i));
        }

        Arbitre arbitre = new Arbitre(100, "Martin", "Alex", "arbitre", "X", "0600000000");
        arbitre.qualifierPourJeu(jeu);
        builder.ajouterArbitre(arbitre);

        Tournoi tournoi = builder.build();

        try (Arena arena = new Arena(new GestionnaireTournois(50, 0.0, 42, new FavoriseMieuxClasse()))) {
            arena.planifierTournoi(tournoi);
            RapportTournoi rapport = arena.simulerProchainTournoi().get();

            System.out.println("Tournoi : " + rapport.getNom());
            System.out.println("Jeu : " + rapport.getJeu());
            System.out.println("Matchs joués : " + rapport.getMatchsJoues()
                    + " / " + rapport.getMatchsPlanifies());
            System.out.println("Vainqueur : "
                    + (rapport.getVainqueur() == null ? "Égalité" : rapport.getVainqueur()));
            System.out.println("Tournoi réussi : " + (rapport.estReussi() ? "oui" : "non"));
        }
    }

    private static Equipe creerEquipe(int numero) {
        String nomEquipe = "Équipe " + numero;
        Coach coach = new Coach(
                numero, "Coach" + numero, "Alex", "coach" + numero, "X", "060000000" + numero, nomEquipe);
        Joueur joueur = new Joueur(
                numero, "Joueur" + numero, "Sam", "joueur" + numero, "X",
                "061000000" + numero, nomEquipe, Joueur.Statut.AMATEUR);
        return new Equipe(nomEquipe, coach, List.of(joueur), 0);
    }
}
