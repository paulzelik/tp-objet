package fr.imt.projet.tournois;

import fr.imt.projet.rapports.RapportRepository;
import fr.imt.projet.rapports.RapportTournoi;
import fr.imt.projet.personnes.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Point d'entrée de la librairie : registre des équipes et arbitres, calendrier des tournois,
 * simulation en arrière-plan et archivage des rapports.
 * <p>
 * <b>Collections</b> :
 * <ul>
 *     <li>tournois planifiés : {@link PriorityQueue} (le plus proche en tête, ordre naturel de {@link Tournoi}) ;</li>
 *     <li>tournois en cours : {@link LinkedHashSet} (au plus 3, ordre de lancement) ;</li>
 *     <li>équipes : {@link HashMap} nom → équipe (accès direct par nom) ;</li>
 *     <li>classement : {@link TreeSet} de lignes figées, triées par points puis par nom.</li>
 * </ul>
 * <b>Synchronisation</b> : lancer un tournoi = « vérifier qu'il reste de la place et qu'il n'y a
 * pas de conflit, puis démarrer ». Ces étapes sont dans une méthode {@code synchronized} : deux
 * demandes simultanées ne peuvent jamais aboutir à plus de 3 tournois en cours.
 * <p>
 * Le gestionnaire est aussi un {@link ObservateurTournoi} : à chaque fin de tournoi, il range le
 * tournoi parmi les terminés, archive son rapport et met à jour les classements.
 */
public class GestionnaireTournois implements ObservateurTournoi, AutoCloseable {
    public static final int MAX_TOURNOIS_EN_COURS = 3;
    private static final long DELAI_ARRET_MILLIS = 2_000;

    private final PriorityQueue<Tournoi> planifies = new PriorityQueue<>();
    private final Set<Tournoi> enCours = new LinkedHashSet<>();
    private final List<Tournoi> termines = new ArrayList<>();
    private final Map<String, Equipe> equipesParNom = new HashMap<>();
    private final Map<Long, Arbitre> arbitres = new LinkedHashMap<>();
    private final List<ObservateurTournoi> observateurs = new CopyOnWriteArrayList<>();
    private final ExecutorService executor;
    private final long tempsParMatchMillis;
    private final double probabiliteIncident;
    private final StrategieVainqueur strategie;
    private final long graine;
    private final RapportRepository rapports;
    private boolean ferme;
    private long numeroSimulation;

    public GestionnaireTournois() {
        this(100, 0.10, System.nanoTime());
    }

    public GestionnaireTournois(long tempsParMatchMillis, double probabiliteIncident, long graine) {
        this(tempsParMatchMillis, probabiliteIncident, StrategieVainqueur.favoriserMieuxClassee(), graine, null);
    }

    /**
     * @param strategie façon de désigner les vainqueurs (patron Stratégie)
     * @param rapports  dépôt où archiver les rapports, ou {@code null} pour ne rien archiver
     */
    public GestionnaireTournois(long tempsParMatchMillis, double probabiliteIncident, StrategieVainqueur strategie,
                                long graine, RapportRepository rapports) {
        if (tempsParMatchMillis < 0) {
            throw new IllegalArgumentException("Le temps par match ne peut pas être négatif");
        }
        if (probabiliteIncident < 0.0 || probabiliteIncident > 1.0) {
            throw new IllegalArgumentException("La probabilité d'incident doit être entre 0 et 1");
        }
        this.tempsParMatchMillis = tempsParMatchMillis;
        this.probabiliteIncident = probabiliteIncident;
        this.strategie = Objects.requireNonNull(strategie, "strategie ne peut pas être null");
        this.graine = graine;
        this.rapports = rapports;
        AtomicInteger numeroThread = new AtomicInteger();
        this.executor = Executors.newFixedThreadPool(MAX_TOURNOIS_EN_COURS,
                tache -> new Thread(tache, "simulation-" + numeroThread.incrementAndGet()));
    }

    // ------------------------------------------------------------------ Registre

    /** Enregistre une équipe ; le nom est unique (accès direct par nom). */
    public synchronized Equipe ajouterEquipe(Equipe equipe) {
        Objects.requireNonNull(equipe, "equipe ne peut pas être null");
        Equipe existante = equipesParNom.putIfAbsent(equipe.getNom(), equipe);
        if (existante != null && existante != equipe) {
            throw new IllegalArgumentException("Une équipe « " + equipe.getNom() + " » existe déjà");
        }
        return equipe;
    }

    public synchronized Optional<Equipe> getEquipe(String nom) {
        return Optional.ofNullable(equipesParNom.get(nom));
    }

    public synchronized Arbitre ajouterArbitre(Arbitre arbitre) {
        Objects.requireNonNull(arbitre, "arbitre ne peut pas être null");
        arbitres.putIfAbsent(arbitre.getId(), arbitre);
        return arbitre;
    }

    /** Abonne un observateur aux tournois ajoutés ensuite (journal, affichage…). */
    public void ajouterObservateur(ObservateurTournoi observateur) {
        observateurs.add(Objects.requireNonNull(observateur, "observateur ne peut pas être null"));
    }

    // ------------------------------------------------------------------ Tournois

    /** Ajoute un tournoi planifié au calendrier (ses arbitres rejoignent le registre des arbitres). */
    public synchronized void ajouterTournoi(Tournoi tournoi) {
        verifierOuvert();
        Objects.requireNonNull(tournoi, "tournoi ne peut pas être null");
        if (tournoi.getStatut() != Tournoi.Statut.PLANIFIE) {
            throw new IllegalArgumentException("Seul un tournoi planifié peut être ajouté");
        }
        if (tousLesTournois().contains(tournoi)) {
            throw new IllegalArgumentException("Le tournoi est déjà enregistré");
        }
        tournoi.getArbitres().forEach(this::ajouterArbitre);
        tournoi.ajouterObservateur(this);
        observateurs.forEach(tournoi::ajouterObservateur);
        planifies.add(tournoi);
    }

    /** Démarre le prochain tournoi pour un déroulement manuel (matchs pilotés par l'appelant). */
    public synchronized Tournoi lancerProchainTournoi() {
        verifierOuvert();
        Tournoi prochain = prochainTournoiDemarrable();
        prochain.demarrer();
        planifies.poll();
        enCours.add(prochain);
        return prochain;
    }

    /** Démarre le prochain tournoi et le simule dans un thread du pool ; rend la main aussitôt. */
    public synchronized Future<RapportTournoi> simulerProchainTournoi() {
        Tournoi prochain = lancerProchainTournoi();
        return executor.submit(new SimulateurTournoi(
                prochain, tempsParMatchMillis, probabiliteIncident, strategie, graine + numeroSimulation++));
    }

    /** Le plus proche dans le temps, s'il peut démarrer : place libre et aucun conflit. */
    private Tournoi prochainTournoiDemarrable() {
        Tournoi prochain = planifies.peek();
        if (prochain == null) {
            throw new IllegalStateException("Aucun tournoi n'est planifié");
        }
        rangerLesTournoisTermines();
        if (enCours.size() >= MAX_TOURNOIS_EN_COURS) {
            throw new IllegalStateException("Trois tournois sont déjà en cours");
        }
        if (enCours.stream().anyMatch(prochain::estEnConflitAvec)) {
            throw new IllegalStateException(
                    "Une équipe, un coach, un joueur ou un arbitre est déjà engagé dans un tournoi en cours");
        }
        return prochain;
    }

    /** Fin d'un tournoi (thread de simulation) : archivage, rangement, classements. */
    @Override
    public void tournoiTermine(Tournoi tournoi, RapportTournoi rapport) {
        if (rapports != null) {
            rapports.sauvegarder(rapport); // hors verrou : l'écriture en base peut prendre du temps
        }
        synchronized (this) {
            rangerLesTournoisTermines();
            mettreAJourLesClassements();
        }
    }

    private void rangerLesTournoisTermines() {
        enCours.removeIf(t -> {
            boolean fini = t.getStatut() == Tournoi.Statut.TERMINE;
            if (fini && !termines.contains(t)) {
                termines.add(t);
            }
            return fini;
        });
    }

    /** Rang 1 = le plus de points (équipes) / de victoires en carrière (coachs). */
    private void mettreAJourLesClassements() {
        int rang = 1;
        for (LigneClassement ligne : getClassementEquipes()) {
            equipesParNom.get(ligne.equipe()).classer(rang++);
        }
        List<Coach> coachs = new ArrayList<>(equipesParNom.values().stream().map(Equipe::getCoach).toList());
        coachs.sort(Comparator.comparingInt(Coach::getTotalVictoiresEnCarriere).reversed()
                .thenComparing(Coach::getPseudo));
        for (int i = 0; i < coachs.size(); i++) {
            coachs.get(i).classer(i + 1);
        }
    }

    // ------------------------------------------------------------------ Listes demandées

    /** Tournois planifiés, le plus proche d'abord (copie triée : une PriorityQueue ne se parcourt pas dans l'ordre). */
    public synchronized List<Tournoi> getTournoisPlanifies() {
        List<Tournoi> tries = new ArrayList<>(planifies);
        tries.sort(Comparator.naturalOrder());
        return tries;
    }

    public synchronized List<Tournoi> getTournoisEnCours() {
        rangerLesTournoisTermines();
        return List.copyOf(enCours);
    }

    /** Tournois terminés, du plus gros au plus petit budget. */
    public synchronized List<Tournoi> getTournoisTermines() {
        rangerLesTournoisTermines();
        List<Tournoi> tries = new ArrayList<>(termines);
        tries.sort(Tournoi.PAR_BUDGET_DECROISSANT);
        return tries;
    }

    public synchronized List<Tournoi> getTournois() {
        return tousLesTournois();
    }

    /**
     * Classement des équipes, sans doublon, par points décroissants puis par nom.
     * On trie des lignes figées : si l'on triait les équipes elles-mêmes, leurs points pourraient
     * changer pendant une simulation et rendre le {@link TreeSet} incohérent.
     */
    public synchronized SortedSet<LigneClassement> getClassementEquipes() {
        SortedSet<LigneClassement> classement = new TreeSet<>();
        equipesParNom.values().forEach(e -> classement.add(new LigneClassement(e.getNom(), e.getNombreDePoints())));
        return classement;
    }

    /** Arbitres qui officient dans un tournoi en cours. */
    public synchronized List<Arbitre> getArbitresOccupes() {
        Set<Arbitre> occupes = arbitresEnService();
        return arbitres.values().stream().filter(occupes::contains).toList();
    }

    public synchronized List<Arbitre> getArbitresDisponibles() {
        Set<Arbitre> occupes = arbitresEnService();
        return arbitres.values().stream().filter(a -> !occupes.contains(a)).toList();
    }

    /** Tous les matchs d'une équipe, tournoi par tournoi (dans l'ordre des dates). */
    public synchronized List<Match> getMatchsDe(Equipe equipe) {
        List<Match> resultat = new ArrayList<>();
        tousLesTournois().stream().sorted().forEach(t -> resultat.addAll(t.getMatchsDe(equipe)));
        return resultat;
    }

    public SortedMap<Equipe, List<Match>> getMatchsParEquipeGagnante(Tournoi tournoi) {
        return tournoi.getMatchsParEquipeGagnante();
    }

    public List<RapportTournoi> getRapportsArchives() {
        return rapports == null ? List.of() : rapports.trouverTous();
    }

    public List<RapportTournoi> getRapportsArchives(String jeu) {
        return rapports == null ? List.of() : rapports.trouverParJeu(jeu);
    }

    private Set<Arbitre> arbitresEnService() {
        rangerLesTournoisTermines();
        Set<Arbitre> occupes = new LinkedHashSet<>();
        enCours.forEach(t -> occupes.addAll(t.getArbitres()));
        return occupes;
    }

    private List<Tournoi> tousLesTournois() {
        List<Tournoi> tous = new ArrayList<>(planifies);
        tous.addAll(enCours);
        tous.addAll(termines);
        return tous;
    }

    // ------------------------------------------------------------------ Arrêt propre

    /**
     * Arrêt propre : plus aucune nouvelle simulation, on laisse jusqu'à 2 s aux simulations en
     * cours, puis on les interrompt (leurs matchs restants sont annulés et leur rapport produit).
     * L'attente se fait hors verrou, pour que les fins de tournoi puissent encore être enregistrées.
     */
    @Override
    public void close() {
        synchronized (this) {
            if (ferme) {
                return;
            }
            ferme = true;
            executor.shutdown();
        }
        boolean interrompu = false;
        try {
            if (!executor.awaitTermination(DELAI_ARRET_MILLIS, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
                executor.awaitTermination(DELAI_ARRET_MILLIS, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            interrompu = true;
            executor.shutdownNow();
        }
        for (Tournoi tournoi : getTournoisEnCours()) {
            tournoi.terminerLesMatchsRestantsSurIncident(); // tournois pilotés à la main
        }
        if (rapports != null) {
            rapports.close();
        }
        if (interrompu) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean estFerme() {
        return executor.isTerminated();
    }

    private void verifierOuvert() {
        if (ferme) {
            throw new IllegalStateException("Le gestionnaire de tournois est fermé");
        }
    }

    /** Ligne figée du classement : nom de l'équipe et points au moment du calcul. */
    public record LigneClassement(String equipe, int points) implements Comparable<LigneClassement> {
        @Override
        public int compareTo(LigneClassement autre) {
            int parPoints = Integer.compare(autre.points, points);
            return parPoints != 0 ? parPoints : equipe.compareTo(autre.equipe);
        }
    }
}
