package fr.imt.projet;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Dépôt JPA (Hibernate) des rapports, dans une base H2.
 * <p>
 * L'{@link EntityManagerFactory} est partagée (elle est thread-safe) ; un {@link EntityManager}
 * ne l'est pas, on en ouvre donc un par opération. C'est nécessaire car les rapports sont
 * sauvegardés depuis les threads de simulation.
 */
public class JpaRapportRepository implements RapportRepository {
    public static final String UNITE_PERSISTANCE = "imt-arena";

    private final EntityManagerFactory fabrique;

    /** Base H2 fichier {@code jdbc:h2:./data/arena} (voir {@code persistence.xml}). */
    public static JpaRapportRepository fichier() {
        return new JpaRapportRepository(Map.of());
    }

    /** Base H2 en mémoire, recréée à chaque fois (tests). */
    public static JpaRapportRepository enMemoire(String nomBase) {
        return new JpaRapportRepository(Map.of(
                "jakarta.persistence.jdbc.url", "jdbc:h2:mem:" + nomBase + ";DB_CLOSE_DELAY=-1",
                "hibernate.hbm2ddl.auto", "create-drop"));
    }

    private JpaRapportRepository(Map<String, String> proprietes) {
        fabrique = Persistence.createEntityManagerFactory(UNITE_PERSISTANCE, proprietes);
    }

    @Override
    public void sauvegarder(RapportTournoi rapport) {
        Objects.requireNonNull(rapport, "rapport ne peut pas être null");
        try (EntityManager em = fabrique.createEntityManager()) {
            EntityTransaction transaction = em.getTransaction();
            try {
                transaction.begin();
                em.persist(rapport);
                transaction.commit();
            } catch (RuntimeException e) {
                if (transaction.isActive()) {
                    transaction.rollback();
                }
                throw e;
            }
        }
    }

    @Override
    public List<RapportTournoi> trouverTous() {
        try (EntityManager em = fabrique.createEntityManager()) {
            return List.copyOf(em.createNamedQuery(RapportTournoi.TOUS, RapportTournoi.class).getResultList());
        }
    }

    @Override
    public List<RapportTournoi> trouverParJeu(String jeu) {
        try (EntityManager em = fabrique.createEntityManager()) {
            return List.copyOf(em.createNamedQuery(RapportTournoi.PAR_JEU, RapportTournoi.class)
                    .setParameter("jeu", jeu)
                    .getResultList());
        }
    }

    @Override
    public void close() {
        if (fabrique.isOpen()) {
            fabrique.close();
        }
    }
}
