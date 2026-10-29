package fr.imt.projet.tournoi;

import fr.imt.projet.jeux.Equipe;
import fr.imt.projet.jeux.Jeux;
import fr.imt.projet.jeux.Match;
import fr.imt.projet.personne.Arbitre;
import fr.imt.projet.personne.Joueur;
import fr.imt.projet.rapports.JpaRapportRepository;
import fr.imt.projet.rapports.RapportTournoi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Tournoi {
    public enum Statut {
        PLANIFIE,
        EN_COURS,
        TERMINE
    }

    private final String nom;
    private final Jeux jeu;
    private final LocalDate date;
    private final BigDecimal budget;
    private final int nombreArenes;
    private final List<Equipe> equipes = new ArrayList<>();
    private final List<Arbitre> arbitres = new ArrayList<>();
    private final List<Match> matchs = new ArrayList<>();
    private Statut statut = Statut.PLANIFIE;
    private RapportTournoi rapport;

    public Tournoi(String nom, Jeux jeu, LocalDate date, BigDecimal budget, int nombreArenes) {
        this.nom = nomValide(nom);
        this.jeu = Objects.requireNonNull(jeu, "jeu ne peut pas être null");
        this.date = Objects.requireNonNull(date, "date ne peut pas être null");
        this.budget = Objects.requireNonNull(budget, "budget ne peut pas être null");
        if (budget.signum() < 0) {
            throw new IllegalArgumentException("Le budget ne peut pas être négatif");
        }
        if (nombreArenes < 1 || nombreArenes > 4) {
            throw new IllegalArgumentException("Un tournoi doit avoir de 1 à 4 arènes");
        }
        this.nombreArenes = nombreArenes;
    }

    public String getNom() {
        return nom;
    }

    public Jeux getJeu() {
        return jeu;
    }

    public LocalDate getDate() {
        return date;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public int getNombreArenes() {
        return nombreArenes;
    }

    public synchronized Statut getStatut() {
        return statut;
    }

    public synchronized List<Equipe> getEquipes() {
        return List.copyOf(equipes);
    }

    public synchronized List<Arbitre> getArbitres() {
        return List.copyOf(arbitres);
    }

    public synchronized List<Match> getMatchs() {
        return List.copyOf(matchs);
    }

    public synchronized RapportTournoi getRapport() {
        return rapport;
    }

    public synchronized void inscrireEquipe(Equipe equipe) {
        verifierPlanifie();
        Objects.requireNonNull(equipe, "equipe ne peut pas être null");
        if (equipes.contains(equipe)) {
            throw new IllegalArgumentException("L'équipe est déjà inscrite");
        }
        equipes.add(equipe);
    }

    public synchronized void inscrireArbitre(Arbitre arbitre) {
        verifierPlanifie();
        Objects.requireNonNull(arbitre, "arbitre ne peut pas être null");
        if (arbitres.contains(arbitre)) {
            throw new IllegalArgumentException("L'arbitre est déjà inscrit");
        }
        arbitres.add(arbitre);
    }

    public synchronized void demarrerMatch(Match match) {
        verifierEnCours();
        verifierMatchInscrit(match);
        long matchsEnCours = matchs.stream().filter(m -> m.getStatut() == Match.Statut.EN_COURS).count();
        if (matchsEnCours >= nombreArenes) {
            throw new IllegalStateException("Toutes les arènes sont actuellement occupées");
        }
        match.demarrer();
    }

    public synchronized void declarerVainqueur(Match match, Equipe vainqueur) {
        verifierEnCours();
        verifierMatchInscrit(match);
        match.declarerVainqueur(vainqueur);
        terminerSiTousLesMatchsSontTraites();
    }

    public synchronized void signalerIncidentTechnique(Match match) {
        verifierEnCours();
        verifierMatchInscrit(match);
        match.signalerIncidentTechnique();
        terminerSiTousLesMatchsSontTraites();
    }

    synchronized void demarrer() {
        verifierPlanifie();
        validerConditionsDeDemarrage();
        genererMatchs();
        statut = Statut.EN_COURS;
    }

    synchronized boolean estEnConflitAvec(Tournoi autre) {
        for (Equipe equipe : equipes) {
            for (Equipe equipeActive : autre.equipes) {
                if (equipe == equipeActive
                        || equipe.getCoach() == equipeActive.getCoach()
                        || contientIdentite(equipe.getJoueurs(), equipeActive.getJoueurs())) {
                    return true;
                }
            }
        }
        return false;
    }

    synchronized void terminerLesMatchsRestantsSurIncident() {
        if (statut == Statut.TERMINE) {
            return;
        }
        verifierEnCours();
        for (Match match : matchs) {
            if (match.getStatut() == Match.Statut.PLANIFIE) {
                match.demarrer();
                match.signalerIncidentTechnique();
            } else if (match.getStatut() == Match.Statut.EN_COURS) {
                match.signalerIncidentTechnique();
            }
        }
        terminerSiTousLesMatchsSontTraites();
    }

    private void validerConditionsDeDemarrage() {
        if (equipes.size() < 4) {
            throw new IllegalStateException("Un tournoi doit compter au moins 4 équipes");
        }
        Map<Joueur, Boolean> joueursInscrits = new IdentityHashMap<>();
        for (Equipe equipe : equipes) {
            if (equipe.getCoach() == null) {
                throw new IllegalStateException("Chaque équipe doit avoir un coach");
            }
            List<Joueur> joueurs = equipe.getJoueurs();
            if (joueurs.size() != jeu.getNombreJoueursParEquipe()) {
                throw new IllegalStateException(
                        "Chaque équipe doit avoir exactement " + jeu.getNombreJoueursParEquipe() + " joueurs");
            }
            for (Joueur joueur : joueurs) {
                Objects.requireNonNull(joueur, "Une équipe ne peut pas contenir de joueur null");
                if (joueursInscrits.put(joueur, Boolean.TRUE) != null) {
                    throw new IllegalStateException("Un joueur ne peut représenter qu'une seule équipe");
                }
            }
        }
        if (arbitres.stream().noneMatch(a -> a.estQualifiePour(jeu.getNom()))) {
            throw new IllegalStateException("Au moins un arbitre doit être qualifié pour le jeu");
        }
    }

    private void genererMatchs() {
        List<Arbitre> arbitresQualifies = arbitres.stream()
                .filter(a -> a.estQualifiePour(jeu.getNom()))
                .toList();
        int indexArbitre = 0;
        for (int i = 0; i < equipes.size(); i++) {
            for (int j = i + 1; j < equipes.size(); j++) {
                Arbitre arbitre = arbitresQualifies.get(indexArbitre++ % arbitresQualifies.size());
                matchs.add(new Match(equipes.get(i), equipes.get(j), jeu, arbitre));
            }
        }
    }

    private void terminerSiTousLesMatchsSontTraites() {
        if (matchs.stream().anyMatch(m -> m.getStatut() == Match.Statut.PLANIFIE
                || m.getStatut() == Match.Statut.EN_COURS)) {
            return;
        }

        Equipe equipeVainqueur = determinerVainqueur();
        String vainqueur = equipeVainqueur == null ? null : equipeVainqueur.getNom();
        int matchsJoues = (int) matchs.stream().filter(m -> m.getStatut() == Match.Statut.JOUE).count();
        boolean reussi = matchsJoues * 10 >= matchs.size() * 7;
        rapport = new RapportTournoi(nom, date, jeu.getNom(), matchs.size(), matchsJoues, vainqueur, reussi);
        JpaRapportRepository.getInstance().sauvegarder(rapport);
        statut = Statut.TERMINE;
    }

    private Equipe determinerVainqueur() {
        int meilleurScore = equipes.stream().mapToInt(this::pointsDuTournoi).max().orElse(0);
        List<Equipe> candidates = equipes.stream()
                .filter(equipe -> pointsDuTournoi(equipe) == meilleurScore)
                .toList();
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        Equipe vainqueurFaceAFace = null;
        int meilleurScoreFaceAFace = -1;
        boolean egaliteFaceAFace = false;
        for (Equipe candidate : candidates) {
            int scoreFaceAFace = candidates.stream()
                    .filter(autre -> autre != candidate)
                    .mapToInt(autre -> pointsFaceAFace(candidate, autre))
                    .sum();
            if (scoreFaceAFace > meilleurScoreFaceAFace) {
                vainqueurFaceAFace = candidate;
                meilleurScoreFaceAFace = scoreFaceAFace;
                egaliteFaceAFace = false;
            } else if (scoreFaceAFace == meilleurScoreFaceAFace) {
                egaliteFaceAFace = true;
            }
        }
        return egaliteFaceAFace ? null : vainqueurFaceAFace;
    }

    private int pointsDuTournoi(Equipe equipe) {
        return (int) matchs.stream().filter(m -> m.getVainqueur() == equipe).count() * 3;
    }

    private int pointsFaceAFace(Equipe equipe, Equipe autre) {
        return (int) matchs.stream()
                .filter(m -> (m.getEquipe1() == equipe && m.getEquipe2() == autre)
                        || (m.getEquipe1() == autre && m.getEquipe2() == equipe))
                .filter(m -> m.getVainqueur() == equipe)
                .count() * 3;
    }

    private void verifierMatchInscrit(Match match) {
        Objects.requireNonNull(match, "match ne peut pas être null");
        if (!matchs.contains(match)) {
            throw new IllegalArgumentException("Le match n'appartient pas à ce tournoi");
        }
    }

    private void verifierPlanifie() {
        if (statut != Statut.PLANIFIE) {
            throw new IllegalStateException("Le tournoi n'est plus planifié");
        }
    }

    private void verifierEnCours() {
        if (statut != Statut.EN_COURS) {
            throw new IllegalStateException("Le tournoi n'est pas en cours");
        }
    }

    private static boolean contientIdentite(List<Joueur> joueurs, List<Joueur> autresJoueurs) {
        for (Joueur joueur : joueurs) {
            for (Joueur autreJoueur : autresJoueurs) {
                if (joueur == autreJoueur) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String nomValide(String nom) {
        Objects.requireNonNull(nom, "nom ne peut pas être null");
        if (nom.isBlank()) {
            throw new IllegalArgumentException("nom ne peut pas être vide");
        }
        return nom;
    }
}
