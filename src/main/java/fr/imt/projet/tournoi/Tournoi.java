package fr.imt.projet.tournoi;

import fr.imt.projet.exceptions.RegleMetierException;
import fr.imt.projet.exceptions.TournoiInvalideException;
import fr.imt.projet.jeux.Equipe;
import fr.imt.projet.jeux.Jeux;
import fr.imt.projet.jeux.Match;
import fr.imt.projet.personne.Arbitre;
import fr.imt.projet.personne.Joueur;
import fr.imt.projet.rapports.JpaRapportRepository;
import fr.imt.projet.rapports.RapportTournoi;
import fr.imt.projet.tournoi.observateur.MatchListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class Tournoi implements Comparable<Tournoi> {
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
    private final AtomicReference<Statut> statut = new AtomicReference<>(Statut.PLANIFIE);
    private final AtomicReference<RapportTournoi> rapport = new AtomicReference<>(null);
    private final List<MatchListener> listeners = new CopyOnWriteArrayList<>();

    public Tournoi(String nom, Jeux jeu, LocalDate date, BigDecimal budget, int nombreArenes) {
        this.nom = nomValide(nom);
        this.jeu = Objects.requireNonNull(jeu, "jeu ne peut pas être null");
        this.date = Objects.requireNonNull(date, "date ne peut pas être null");
        this.budget = Objects.requireNonNull(budget, "budget ne peut pas être null");
        if (budget.signum() < 0) {
            throw new RegleMetierException("Le budget ne peut pas être négatif");
        }
        if (nombreArenes < 1 || nombreArenes > 4) {
            throw new RegleMetierException("Un tournoi doit avoir de 1 à 4 arènes");
        }
        this.nombreArenes = nombreArenes;
    }

    public void addMatchListener(MatchListener listener) {
        if (listener != null) {
            listeners.add(listener);
            for (Match m : matchs) {
                m.addMatchListener(listener);
            }
        }
    }

    public void removeMatchListener(MatchListener listener) {
        listeners.remove(listener);
        for (Match m : matchs) {
            m.removeMatchListener(listener);
        }
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

    public Statut getStatut() {
        return statut.get();
    }

    public synchronized List<Equipe> getEquipes() {
        return Collections.unmodifiableList(new ArrayList<>(equipes));
    }

    public synchronized List<Arbitre> getArbitres() {
        return Collections.unmodifiableList(new ArrayList<>(arbitres));
    }

    public synchronized List<Match> getMatchs() {
        return Collections.unmodifiableList(new ArrayList<>(matchs));
    }

    public RapportTournoi getRapport() {
        return rapport.get();
    }

    public synchronized void inscrireEquipe(Equipe equipe) {
        verifierPlanifie();
        Objects.requireNonNull(equipe, "equipe ne peut pas être null");
        if (equipes.contains(equipe)) {
            throw new RegleMetierException("L'équipe est déjà inscrite");
        }
        equipes.add(equipe);
    }

    public synchronized void inscrireArbitre(Arbitre arbitre) {
        verifierPlanifie();
        Objects.requireNonNull(arbitre, "arbitre ne peut pas être null");
        if (arbitres.contains(arbitre)) {
            throw new RegleMetierException("L'arbitre est déjà inscrit");
        }
        arbitres.add(arbitre);
    }

    public synchronized void demarrer() {
        verifierPlanifie();
        verifierConditionsDemarrage();
        genererMatchs();
        statut.set(Statut.EN_COURS);
    }

    public synchronized void demarrerMatch(Match match) {
        verifierEnCours();
        Objects.requireNonNull(match, "match ne peut pas être null");
        if (!matchs.contains(match)) {
            throw new RegleMetierException("Ce match n'appartient pas au tournoi");
        }
        long matchsEnCours = matchs.stream()
                .filter(m -> m.getStatut() == Match.Statut.EN_COURS)
                .count();
        if (matchsEnCours >= nombreArenes) {
            throw new RegleMetierException("Toutes les arènes sont occupées");
        }
        match.demarrer();
    }

    public synchronized void declarerVainqueur(Match match, Equipe vainqueur) {
        verifierEnCours();
        Objects.requireNonNull(match, "match ne peut pas être null");
        if (!matchs.contains(match)) {
            throw new RegleMetierException("Ce match n'appartient pas au tournoi");
        }
        match.declarerVainqueur(vainqueur);
        verifierClotureAutomatique();
    }

    public synchronized void signalerIncidentTechnique(Match match) {
        verifierEnCours();
        Objects.requireNonNull(match, "match ne peut pas être null");
        if (!matchs.contains(match)) {
            throw new RegleMetierException("Ce match n'appartient pas au tournoi");
        }
        match.signalerIncidentTechnique();
        verifierClotureAutomatique();
    }

    public synchronized void terminerLesMatchsRestantsSurIncident() {
        if (statut.get() != Statut.EN_COURS) {
            return;
        }
        for (Match match : matchs) {
            if (match.getStatut() == Match.Statut.PLANIFIE) {
                match.demarrer();
                match.signalerIncidentTechnique();
            } else if (match.getStatut() == Match.Statut.EN_COURS) {
                match.signalerIncidentTechnique();
            }
        }
        cloturer();
    }

    public synchronized boolean estEnConflitAvec(Tournoi autre) {
        if (autre == null) return false;
        for (Equipe e : equipes) {
            if (autre.equipes.contains(e)) return true;
            if (e.getCoach() != null) {
                for (Equipe autreE : autre.equipes) {
                    if (e.getCoach().equals(autreE.getCoach())) return true;
                }
            }
            for (Joueur j : e.getJoueurs()) {
                for (Equipe autreE : autre.equipes) {
                    if (autreE.getJoueurs().contains(j)) return true;
                }
            }
        }
        return false;
    }

    public synchronized TreeSet<Equipe> getClassementEquipes() {
        Comparator<Equipe> comparator = Comparator
                .comparingInt(Equipe::getPoints).reversed()
                .thenComparing(Equipe::getNom);
        TreeSet<Equipe> classement = new TreeSet<>(comparator);
        classement.addAll(equipes);
        return classement;
    }

    public synchronized Map<Equipe, List<Match>> getMatchsGroupesParVainqueur() {
        return matchs.stream()
                .filter(m -> m.getStatut() == Match.Statut.JOUE && m.getVainqueur() != null)
                .collect(Collectors.groupingBy(Match::getVainqueur));
    }

    @Override
    public int compareTo(Tournoi autre) {
        int dateCmp = this.date.compareTo(autre.date);
        if (dateCmp != 0) return dateCmp;
        return this.nom.compareTo(autre.nom);
    }

    private void verifierConditionsDemarrage() {
        if (equipes.size() < 4) {
            throw new TournoiInvalideException("Un tournoi nécessite au moins 4 équipes");
        }
        List<Arbitre> arbitresQualifies = arbitres.stream()
                .filter(a -> a.estQualifiePour(jeu))
                .toList();
        if (arbitresQualifies.isEmpty()) {
            throw new TournoiInvalideException("Aucun arbitre qualifié pour le jeu : " + jeu.getNom());
        }
        int joueursRequis = jeu.getNombreJoueursParEquipe();
        Set<Joueur> tousLesJoueurs = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Equipe equipe : equipes) {
            if (equipe.getCoach() == null) {
                throw new TournoiInvalideException("Chaque équipe doit avoir un coach : " + equipe.getNom());
            }
            if (equipe.getJoueurs().size() != joueursRequis) {
                throw new TournoiInvalideException("L'équipe " + equipe.getNom() + " doit avoir exactement "
                        + joueursRequis + " joueurs");
            }
            for (Joueur joueur : equipe.getJoueurs()) {
                if (!tousLesJoueurs.add(joueur)) {
                    throw new TournoiInvalideException("Un joueur est présent dans plusieurs équipes : " + joueur.getNom());
                }
            }
        }
    }

    private void genererMatchs() {
        matchs.clear();
        List<Arbitre> arbitresQualifies = arbitres.stream()
                .filter(a -> a.estQualifiePour(jeu))
                .toList();
        int indexArbitre = 0;
        for (int i = 0; i < equipes.size(); i++) {
            for (int j = i + 1; j < equipes.size(); j++) {
                Arbitre arbitre = arbitresQualifies.get(indexArbitre % arbitresQualifies.size());
                indexArbitre++;
                Match m = new Match(equipes.get(i), equipes.get(j), jeu, arbitre);
                for (MatchListener listener : listeners) {
                    m.addMatchListener(listener);
                }
                matchs.add(m);
            }
        }
    }

    private void verifierClotureAutomatique() {
        boolean tousTermines = !matchs.isEmpty() && matchs.stream()
                .allMatch(m -> m.getStatut() == Match.Statut.JOUE || m.getStatut() == Match.Statut.INCIDENT_TECHNIQUE);
        if (tousTermines) {
            cloturer();
        }
    }

    private void cloturer() {
        statut.set(Statut.TERMINE);
        int matchsPlanifies = matchs.size();
        long matchsJoues = matchs.stream().filter(m -> m.getStatut() == Match.Statut.JOUE).count();
        boolean reussi = matchsPlanifies > 0 && ((double) matchsJoues / matchsPlanifies) >= 0.70;

        Equipe vainqueur = determinerVainqueurTournoi();
        String nomVainqueur = vainqueur == null ? null : vainqueur.getNom();

        RapportTournoi nouveauRapport = new RapportTournoi(
                nom, date, jeu.getNom(), matchsPlanifies, (int) matchsJoues, nomVainqueur, reussi);
        rapport.set(nouveauRapport);
        JpaRapportRepository.getInstance().sauvegarder(nouveauRapport);
    }

    private Equipe determinerVainqueurTournoi() {
        Map<Equipe, Integer> pointsDuTournoi = new IdentityHashMap<>();
        for (Equipe e : equipes) {
            pointsDuTournoi.put(e, 0);
        }
        for (Match m : matchs) {
            if (m.getStatut() == Match.Statut.JOUE && m.getVainqueur() != null) {
                pointsDuTournoi.put(m.getVainqueur(), pointsDuTournoi.get(m.getVainqueur()) + 3);
            }
        }
        int maxPoints = pointsDuTournoi.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<Equipe> exAequo = pointsDuTournoi.entrySet().stream()
                .filter(entry -> entry.getValue() == maxPoints)
                .map(Map.Entry::getKey)
                .toList();

        if (exAequo.isEmpty()) return null;
        if (exAequo.size() == 1) return exAequo.get(0);

        Map<Equipe, Integer> pointsDirects = new IdentityHashMap<>();
        for (Equipe e : exAequo) pointsDirects.put(e, 0);

        for (Match m : matchs) {
            if (m.getStatut() == Match.Statut.JOUE && m.getVainqueur() != null) {
                if (exAequo.contains(m.getEquipe1()) && exAequo.contains(m.getEquipe2())) {
                    pointsDirects.put(m.getVainqueur(), pointsDirects.get(m.getVainqueur()) + 3);
                }
            }
        }
        int maxDirects = pointsDirects.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<Equipe> vainqueursFinaux = pointsDirects.entrySet().stream()
                .filter(entry -> entry.getValue() == maxDirects)
                .map(Map.Entry::getKey)
                .toList();

        return vainqueursFinaux.size() == 1 ? vainqueursFinaux.get(0) : null;
    }

    private void verifierPlanifie() {
        if (statut.get() != Statut.PLANIFIE) {
            throw new RegleMetierException("Le tournoi n'est plus en phase de planification");
        }
    }

    private void verifierEnCours() {
        if (statut.get() != Statut.EN_COURS) {
            throw new RegleMetierException("Le tournoi doit être en cours");
        }
    }

    private static String nomValide(String nom) {
        Objects.requireNonNull(nom, "nom ne peut pas être null");
        if (nom.isBlank()) {
            throw new RegleMetierException("nom ne peut pas être vide");
        }
        return nom;
    }
}
