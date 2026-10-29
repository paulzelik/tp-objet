package fr.imt.projet.tournois;

import fr.imt.projet.rapports.RapportTournoi;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import fr.imt.projet.personnes.Equipe;

/**
 * Simulation d'un tournoi, exécutée dans un thread de l'{@code ExecutorService} du gestionnaire.
 * <ol>
 *     <li>Les matchs sont répartis en <b>vagues</b> : au plus un match par arène, et jamais deux
 *     matchs simultanés pour une même équipe.</li>
 *     <li>Durée totale = (nombre de matchs × temps par match) ÷ nombre d'arènes, répartie
 *     également entre les vagues (15 matchs × 100 ms ÷ 3 arènes = 500 ms).</li>
 *     <li>À la fin de chaque vague, chaque match subit le tirage d'un incident technique
 *     (match annulé, sans point), sinon la {@link StrategieVainqueur} désigne le vainqueur.</li>
 * </ol>
 * Le {@link Random} est propre à cette simulation et créé avec une graine : à graine égale,
 * résultats identiques. Interrompue, la simulation annule les matchs restants et clôture le
 * tournoi, qui produit tout de même son rapport.
 */
public class SimulateurTournoi implements Callable<RapportTournoi> {
    private final Tournoi tournoi;
    private final long tempsParMatchMillis;
    private final double probabiliteIncident;
    private final StrategieVainqueur strategie;
    private final Random random;

    public SimulateurTournoi(Tournoi tournoi, long tempsParMatchMillis, double probabiliteIncident, long graine) {
        this(tournoi, tempsParMatchMillis, probabiliteIncident, StrategieVainqueur.favoriserMieuxClassee(), graine);
    }

    public SimulateurTournoi(Tournoi tournoi, long tempsParMatchMillis, double probabiliteIncident,
                             StrategieVainqueur strategie, long graine) {
        this.tournoi = Objects.requireNonNull(tournoi, "tournoi ne peut pas être null");
        if (tempsParMatchMillis < 0) {
            throw new IllegalArgumentException("Le temps par match ne peut pas être négatif");
        }
        if (probabiliteIncident < 0.0 || probabiliteIncident > 1.0) {
            throw new IllegalArgumentException("La probabilité d'incident doit être entre 0 et 1");
        }
        this.tempsParMatchMillis = tempsParMatchMillis;
        this.probabiliteIncident = probabiliteIncident;
        this.strategie = Objects.requireNonNull(strategie, "strategie ne peut pas être null");
        random = new Random(graine);
    }

    @Override
    public RapportTournoi call() {
        List<List<Match>> vagues = planifierVagues(tournoi.getMatchs(), tournoi.getNombreArenes());
        long pauseParVagueNanos = vagues.isEmpty() ? 0
                : TimeUnit.MILLISECONDS.toNanos(tournoi.getDureeSimulationMillis(tempsParMatchMillis)) / vagues.size();
        try {
            for (List<Match> vague : vagues) {
                vague.forEach(tournoi::demarrerMatch);
                TimeUnit.NANOSECONDS.sleep(pauseParVagueNanos);
                vague.forEach(this::conclure);
            }
        } catch (InterruptedException interruption) {
            Thread.currentThread().interrupt(); // on restaure le drapeau d'interruption
        } catch (IllegalStateException e) {
            // Le tournoi a pu être clôturé de force par la fermeture du gestionnaire.
            if (tournoi.getStatut() != Tournoi.Statut.TERMINE) {
                throw e;
            }
        } finally {
            // Quoi qu'il arrive, le tournoi ne reste jamais bloqué EN_COURS.
            tournoi.terminerLesMatchsRestantsSurIncident();
        }
        return tournoi.getRapport();
    }

    private void conclure(Match match) {
        if (random.nextDouble() < probabiliteIncident) {
            tournoi.signalerIncidentTechnique(match);
        } else {
            tournoi.declarerVainqueur(match,
                    strategie.designerVainqueur(match.getEquipe1(), match.getEquipe2(), random));
        }
    }

    /**
     * Répartit les matchs en vagues jouables en même temps : au plus {@code arenes} matchs, sans
     * équipe en double. {@link LinkedList} : les matchs retenus sont retirés pendant le parcours
     * ({@link Iterator#remove()} en temps constant).
     */
    static List<List<Match>> planifierVagues(List<Match> matchs, int arenes) {
        List<Match> restants = new LinkedList<>(matchs);
        List<List<Match>> vagues = new ArrayList<>();
        while (!restants.isEmpty()) {
            List<Match> vague = new ArrayList<>();
            Set<Equipe> occupees = new HashSet<>();
            Iterator<Match> it = restants.iterator();
            while (it.hasNext() && vague.size() < arenes) {
                Match match = it.next();
                if (!occupees.contains(match.getEquipe1()) && !occupees.contains(match.getEquipe2())) {
                    vague.add(match);
                    occupees.add(match.getEquipe1());
                    occupees.add(match.getEquipe2());
                    it.remove();
                }
            }
            vagues.add(vague);
        }
        return vagues;
    }
}
