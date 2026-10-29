package fr.imt.projet;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Tournoi : chaque équipe inscrite rencontre chaque autre équipe une fois.
 * <p>
 * Toutes les lectures et écritures d'état sont {@code synchronized} : le thread principal ne
 * voit jamais un état intermédiaire pendant qu'un thread de simulation fait avancer le tournoi.
 * Les observateurs sont prévenus <em>hors</em> du verrou.
 * <p>
 * Ordre naturel ({@link Comparable}) : date puis nom, utilisé par la file de priorité des
 * tournois planifiés du {@link GestionnaireTournois}.
 */
public class Tournoi implements Comparable<Tournoi> {
    public enum Statut {
        PLANIFIE,
        EN_COURS,
        TERMINE
    }

    public static final int MIN_EQUIPES = 4;
    /** Tournois terminés : du plus gros au plus petit budget. */
    public static final Comparator<Tournoi> PAR_BUDGET_DECROISSANT =
            Comparator.comparing(Tournoi::getBudget).reversed().thenComparing(Comparator.naturalOrder());

    private static final Logger LOGGER = Logger.getLogger(Tournoi.class.getName());

    private final String nom;
    private final Jeux jeu;
    private final LocalDate date;
    private final BigDecimal budget;
    private final int nombreArenes;
    private final List<Equipe> equipes = new ArrayList<>();
    private final List<Arbitre> arbitres = new ArrayList<>();
    private final List<Match> matchs = new ArrayList<>();
    private final List<ObservateurTournoi> observateurs = new CopyOnWriteArrayList<>();
    private Statut statut = Statut.PLANIFIE;
    private RapportTournoi rapport;

    public Tournoi(String nom, Jeux jeu, LocalDate date, BigDecimal budget, int nombreArenes) {
        this.nom = Personne.texteValide(nom, "nom");
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

    /** Patron Monteur : construire un tournoi pas à pas, validé en une fois. */
    public static TournoiBuilder builder() {
        return new TournoiBuilder();
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

    public synchronized List<Match> getMatchsDe(Equipe equipe) {
        return matchs.stream().filter(m -> m.oppose(equipe)).toList();
    }

    public synchronized long getNombreMatchsTermines() {
        return matchs.stream().filter(m -> m.getStatut().estTermine()).count();
    }

    public synchronized RapportTournoi getRapport() {
        return rapport;
    }

    /** n × (n − 1) / 2 matchs pour n équipes. */
    public synchronized int getNombreMatchsPrevus() {
        return equipes.size() * (equipes.size() - 1) / 2;
    }

    /** Durée de simulation : (nombre de matchs × temps par match) ÷ nombre d'arènes. */
    public long getDureeSimulationMillis(long tempsParMatchMillis) {
        return Math.multiplyExact((long) getNombreMatchsPrevus(), tempsParMatchMillis) / nombreArenes;
    }

    /**
     * Matchs joués regroupés par équipe gagnante : {@link TreeMap} triée par nom d'équipe.
     * Les matchs annulés (incident technique) n'ont pas de vainqueur et n'y figurent pas.
     */
    public synchronized SortedMap<Equipe, List<Match>> getMatchsParEquipeGagnante() {
        return matchs.stream()
                .filter(m -> m.getStatut() == Match.Statut.JOUE)
                .collect(Collectors.groupingBy(Match::getVainqueur,
                        () -> new TreeMap<>(Comparator.comparing(Equipe::getNom)),
                        Collectors.toList()));
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

    public void ajouterObservateur(ObservateurTournoi observateur) {
        Objects.requireNonNull(observateur, "observateur ne peut pas être null");
        if (!observateurs.contains(observateur)) {
            observateurs.add(observateur);
        }
    }

    /**
     * Fait entrer un match dans une arène : refusé si toutes les arènes sont occupées ou si
     * l'une des deux équipes joue déjà dans une autre arène.
     */
    public synchronized void demarrerMatch(Match match) {
        verifierEnCours();
        verifierMatchInscrit(match);
        List<Match> enCours = matchs.stream().filter(m -> m.getStatut() == Match.Statut.EN_COURS).toList();
        if (enCours.size() >= nombreArenes) {
            throw new IllegalStateException("Toutes les arènes sont actuellement occupées");
        }
        if (enCours.stream().anyMatch(m -> m.oppose(match.getEquipe1()) || m.oppose(match.getEquipe2()))) {
            throw new IllegalStateException("Une équipe ne peut pas jouer deux matchs en même temps");
        }
        match.demarrer();
    }

    public void declarerVainqueur(Match match, Equipe vainqueur) {
        boolean termine;
        synchronized (this) {
            verifierEnCours();
            verifierMatchInscrit(match);
            match.declarerVainqueur(vainqueur);
            termine = terminerSiTousLesMatchsSontTraites();
        }
        notifier(o -> o.matchTermine(this, match));
        notifierFinSi(termine);
    }

    public void signalerIncidentTechnique(Match match) {
        boolean termine;
        synchronized (this) {
            verifierEnCours();
            verifierMatchInscrit(match);
            match.signalerIncidentTechnique();
            termine = terminerSiTousLesMatchsSontTraites();
        }
        notifier(o -> o.incidentTechnique(this, match));
        notifierFinSi(termine);
    }

    /** Démarrage : réservé au {@link GestionnaireTournois} (package-private). */
    void demarrer() {
        synchronized (this) {
            verifierPlanifie();
            validerConditionsDeDemarrage();
            equipes.forEach(e -> e.engager(this));
            genererMatchs();
            statut = Statut.EN_COURS;
        }
        notifier(o -> o.tournoiDemarre(this));
    }

    /** Conflit si une équipe, un coach, un joueur ou un arbitre est commun aux deux tournois. */
    boolean estEnConflitAvec(Tournoi autre) {
        Set<Object> engages = new HashSet<>();
        for (Equipe equipe : autre.getEquipes()) {
            engages.add(equipe);
            engages.add(equipe.getCoach());
            engages.addAll(equipe.getJoueurs());
        }
        engages.addAll(autre.getArbitres());
        for (Equipe equipe : getEquipes()) {
            if (engages.contains(equipe) || engages.contains(equipe.getCoach())
                    || equipe.getJoueurs().stream().anyMatch(engages::contains)) {
                return true;
            }
        }
        return getArbitres().stream().anyMatch(engages::contains);
    }

    /** Arrêt d'urgence : les matchs non terminés sont annulés, puis le tournoi se clôture normalement. */
    void terminerLesMatchsRestantsSurIncident() {
        List<Match> annules = new ArrayList<>();
        boolean termine;
        synchronized (this) {
            if (statut != Statut.EN_COURS) {
                return;
            }
            for (Match match : matchs) {
                if (match.getStatut() == Match.Statut.PLANIFIE) {
                    match.demarrer();
                }
                if (match.getStatut() == Match.Statut.EN_COURS) {
                    match.signalerIncidentTechnique();
                    annules.add(match);
                }
            }
            termine = terminerSiTousLesMatchsSontTraites();
        }
        annules.forEach(m -> notifier(o -> o.incidentTechnique(this, m)));
        notifierFinSi(termine);
    }

    private void validerConditionsDeDemarrage() {
        if (equipes.size() < MIN_EQUIPES) {
            throw new IllegalStateException("Un tournoi doit compter au moins " + MIN_EQUIPES + " équipes");
        }
        for (Equipe equipe : equipes) {
            // Jeu solo : l'équipe a plusieurs joueurs et en désigne un ; il faut donc « au moins » n joueurs.
            if (equipe.getJoueurs().size() < jeu.getNombreJoueursParEquipe()) {
                throw new IllegalStateException("« " + equipe.getNom() + " » doit avoir au moins "
                        + jeu.getNombreJoueursParEquipe() + " joueur(s) pour " + jeu.getNom());
            }
            if (equipe.estEngagee()) {
                throw new IllegalStateException("« " + equipe.getNom() + " » dispute déjà un tournoi en cours");
            }
        }
        if (arbitres.stream().noneMatch(a -> a.estQualifiePour(jeu.getNom()))) {
            throw new IllegalStateException("Au moins un arbitre doit être qualifié pour le jeu");
        }
    }

    /** Chaque paire d'équipes reçoit un match ; les arbitres qualifiés sont affectés à tour de rôle. */
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

    /** Clôture automatique : rapport + libération des équipes, en une seule section critique. */
    private boolean terminerSiTousLesMatchsSontTraites() {
        if (statut != Statut.EN_COURS || matchs.stream().anyMatch(m -> !m.getStatut().estTermine())) {
            return false;
        }
        Equipe equipeVainqueur = determinerVainqueur();
        int matchsJoues = (int) matchs.stream().filter(m -> m.getStatut() == Match.Statut.JOUE).count();
        boolean reussi = matchsJoues * 10 >= matchs.size() * 7;
        rapport = new RapportTournoi(nom, date, jeu.getNom(), matchs.size(), matchsJoues,
                equipeVainqueur == null ? null : equipeVainqueur.getNom(), reussi);
        equipes.forEach(e -> e.desengager(this));
        statut = Statut.TERMINE;
        return true;
    }

    /**
     * Vainqueur : le plus de points dans ce tournoi ; en cas d'égalité, les confrontations
     * directes départagent ; si l'égalité persiste, pas de vainqueur.
     */
    private Equipe determinerVainqueur() {
        int meilleurScore = equipes.stream().mapToInt(this::pointsDuTournoi).max().orElse(0);
        if (meilleurScore == 0) {
            return null;
        }
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
        return (int) matchs.stream().filter(m -> m.getVainqueur() == equipe).count() * Equipe.POINTS_PAR_VICTOIRE;
    }

    private int pointsFaceAFace(Equipe equipe, Equipe autre) {
        return (int) matchs.stream()
                .filter(m -> m.oppose(equipe) && m.oppose(autre))
                .filter(m -> m.getVainqueur() == equipe)
                .count() * Equipe.POINTS_PAR_VICTOIRE;
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

    private void notifierFinSi(boolean termine) {
        if (termine) {
            RapportTournoi rapportFinal = getRapport();
            notifier(o -> o.tournoiTermine(this, rapportFinal));
        }
    }

    /** Un observateur défaillant ne doit jamais faire planter le tournoi (surtout en finale). */
    private void notifier(Consumer<ObservateurTournoi> evenement) {
        for (ObservateurTournoi observateur : observateurs) {
            try {
                evenement.accept(observateur);
            } catch (RuntimeException e) {
                LOGGER.log(Level.WARNING, "Observateur en erreur sur le tournoi " + nom, e);
            }
        }
    }

    @Override
    public int compareTo(Tournoi autre) {
        int parDate = date.compareTo(autre.date);
        return parDate != 0 ? parDate : nom.compareTo(autre.nom);
    }

    @Override
    public String toString() {
        return "%s — %s, %s, budget %s €, %d arène(s)".formatted(nom, jeu.getNom(), date, budget, nombreArenes);
    }
}
