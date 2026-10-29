package fr.imt.projet.rapports;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class JpaRapportRepository implements AutoCloseable {
    private static final String PERSISTENCE_UNIT = "arena-reports";
    private static final Pattern DATABASE_NAME = Pattern.compile("[A-Za-z0-9_-]+");

    private final EntityManagerFactory entityManagerFactory;
    private boolean closed;

    public JpaRapportRepository() {
        this(creerFactoryPersistante());
    }

    private JpaRapportRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public static JpaRapportRepository getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public static JpaRapportRepository enMemoire(String nomBase) {
        Objects.requireNonNull(nomBase, "nomBase ne peut pas être null");
        if (!DATABASE_NAME.matcher(nomBase).matches()) {
            throw new IllegalArgumentException("Le nom de base de données est invalide");
        }
        EntityManagerFactory factory = Persistence.createEntityManagerFactory(
                PERSISTENCE_UNIT,
                Map.of(
                        "jakarta.persistence.jdbc.url", "jdbc:h2:mem:" + nomBase + ";DB_CLOSE_DELAY=-1",
                        "jakarta.persistence.jdbc.driver", "org.h2.Driver",
                        "jakarta.persistence.jdbc.user", "sa",
                        "jakarta.persistence.jdbc.password", "",
                        "hibernate.hbm2ddl.auto", "create-drop"));
        return new JpaRapportRepository(factory);
    }

    public synchronized RapportTournoi sauvegarder(RapportTournoi rapport) {
        verifierOuvert();
        Objects.requireNonNull(rapport, "rapport ne peut pas être null");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            entityManager.getTransaction().begin();
            entityManager.persist(rapport);
            entityManager.getTransaction().commit();
            return rapport;
        } catch (RuntimeException exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    public synchronized List<RapportTournoi> trouverTous() {
        verifierOuvert();
        try (EntityManagerResource resource = new EntityManagerResource(entityManagerFactory.createEntityManager())) {
            return resource.entityManager()
                    .createQuery("select r from RapportTournoi r order by r.id", RapportTournoi.class)
                    .getResultList();
        }
    }

    public synchronized List<RapportTournoi> trouverParJeu(String jeu) {
        verifierOuvert();
        Objects.requireNonNull(jeu, "jeu ne peut pas être null");
        String jeuNormalise = jeu.trim().toLowerCase(Locale.ROOT);
        try (EntityManagerResource resource = new EntityManagerResource(entityManagerFactory.createEntityManager())) {
            return resource.entityManager()
                    .createQuery(
                            "select r from RapportTournoi r where lower(r.jeu) = :jeu order by r.id",
                            RapportTournoi.class)
                    .setParameter("jeu", jeuNormalise)
                    .getResultList();
        }
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            entityManagerFactory.close();
            closed = true;
        }
    }

    private void verifierOuvert() {
        if (closed) {
            throw new IllegalStateException("Le dépôt de rapports est fermé");
        }
    }

    private static final class InstanceHolder {
        private static final JpaRapportRepository INSTANCE = new JpaRapportRepository();
    }

    private static EntityManagerFactory creerFactoryPersistante() {
        try {
            Files.createDirectories(Path.of("data"));
        } catch (IOException exception) {
            throw new PersistenceException("Impossible de créer le répertoire de la base H2", exception);
        }
        return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
    }

    private record EntityManagerResource(EntityManager entityManager) implements AutoCloseable {
        @Override
        public void close() {
            entityManager.close();
        }
    }
}
