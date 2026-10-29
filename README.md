# IMT Arena — modèle de tournois

Projet Java qui modélise les personnes, les équipes, les matchs et les tournois d'une compétition
e-sport. Il simule les tournois en arrière-plan et archive leurs rapports dans une base H2.
Les personnes et leurs capacités sont regroupées dans `fr.imt.projet.personnes`, tandis que les
jeux et leurs variantes sont dans `fr.imt.projet.jeux`. Les autres classes métier restent dans
`fr.imt.projet`.

La liste détaillée des améliorations apportées à la première version se trouve dans
[CHANGEMENTS.md](CHANGEMENTS.md).

## Prérequis

- JDK 21 ou plus récent (testé avec 21, compatible 25)
- Apache Maven 3.9 ou plus récent

## Lancer la démonstration et les tests

À la racine du projet :

```bash
mvn compile exec:java
```

```bash
mvn test
```

La démonstration (`Main`) déroule une mini-saison complète :

1. Création des jeux par la fabrique, dont un jeu « Pong » ajouté pendant l'exécution.
2. Génération de 8 équipes de 3 joueurs et de 4 arbitres (données aléatoires à graine fixe).
   Le programme montre aussi des refus : joueur dans deux équipes, 5 arènes, arbitre non qualifié.
3. Planification de 4 tournois avec le builder, affichés du plus proche au plus lointain.
4. Simulation de deux tournois **en parallèle** (threads `simulation-1` et `simulation-2`).
   La Coupe d'Hiver est refusée parce que ses équipes jouent déjà. Le programme suit l'état des
   tournois en direct, puis récupère les rapports avec un délai maximal (`Future.get`).
5. Affichage des listes demandées : tournois planifiés et terminés, classement, matchs d'une équipe,
   matchs regroupés par vainqueur, coachs, joueurs pros, arbitres.
6. Relecture des rapports archivés dans la base H2 `data/arena.mv.db`. La base est conservée d'un
   lancement à l'autre ; il suffit de supprimer `data/` pour repartir de zéro.

Les 58 tests JUnit 5 sont répartis en 6 classes : `JeuxTest`, `EquipeTest`, `MatchTest`,
`TournoiTest`, `GestionnaireListesTest` et `MainTest`. Pour n'en lancer qu'une :

```bash
mvn -Dtest=TournoiTest test
```

## Organisation du code

### Personnes et capacités

- `Personne` (abstraite) contient l'identité commune à `Joueur`, `Coach` et `Arbitre`. Ses attributs
  sont immuables.
- `Classable` est la capacité à recevoir un classement. `Joueur`, `Coach` et `Equipe` l'implémentent.
- `QualifiablePourJeu` est la capacité à être qualifié pour un jeu. `Arbitre` l'implémente ; ses jeux
  sont rangés dans un `TreeSet`.
- Un joueur n'appartient qu'à une équipe à la fois, et un coach ne dirige qu'une équipe. C'est
  l'`Equipe` qui affecte les joueurs et le coach (`recruter`, `liberer`, `changerCoach`) et qui le
  vérifie.
- Un joueur amateur devient pro après trois victoires de son équipe depuis son arrivée.

### Jeux

`Jeux` (abstraite) a deux sous-classes : `JeuxSolo` (un joueur désigné dans l'équipe) et
`JeuxEquipe` (3 à 5 joueurs). `FabriqueJeux` crée un jeu à partir de son nom
(patron **Fabrique**). Un nouveau type de jeu s'ajoute avec une nouvelle sous-classe, sans modifier
l'existant.

### Équipes, matchs et tournois

Les classes de calendrier, matchs, stratégie de victoire et simulation sont regroupées dans `fr.imt.projet.tournois`.
Les entités et dépôts de rapports se trouvent dans `fr.imt.projet.rapports`.

- `Equipe` : un nom, un coach, des joueurs et des points (3 par victoire). Sa composition est gelée
  pendant un tournoi.
- `Match` : deux équipes différentes et un arbitre qualifié. Son cycle de vie est
  `PLANIFIE` → `EN_COURS` → `JOUE` ou `INCIDENT_TECHNIQUE`.
- `Tournoi` : de 1 à 4 arènes, au moins 4 équipes et un arbitre qualifié pour démarrer. Chaque
  équipe rencontre chaque autre une fois. Une arène accueille un match à la fois, et une équipe ne
  joue jamais deux matchs en même temps. La clôture est automatique et produit le rapport ; le
  tournoi est réussi si au moins 70 % des matchs ont été joués.
- `TournoiBuilder` (patron **Monteur**) : `Tournoi.builder()…build()` valide tout et signale toutes
  les erreurs d'un coup.
- `ObservateurTournoi` (patron **Observateur**) : le tournoi prévient ses abonnés quand un match se
  termine, quand un incident survient et quand le tournoi se termine.

### Gestionnaire, simulation, persistance

- `GestionnaireTournois` est le point d'entrée :
  - tournois planifiés dans une `PriorityQueue` (le plus proche d'abord) ;
  - équipes dans une `HashMap` (accès par nom) ;
  - au plus 3 tournois en cours, garanti par une méthode `synchronized` même avec des demandes
    simultanées ;
  - pool de 3 threads (`ExecutorService`) ;
  - listes demandées et arrêt propre (`AutoCloseable`).
- `SimulateurTournoi` (`Callable`) joue les matchs par vagues pendant
  (matchs × temps par match) ÷ arènes. Il tire au sort les incidents (10 %) et confie le choix du
  vainqueur à une `StrategieVainqueur` (patron **Stratégie**).
- `RapportTournoi` est une `@Entity` JPA. `JpaRapportRepository` (Hibernate + H2) sauvegarde les
  rapports et les relit, tous ou par jeu.

## Exemple d'utilisation

```java
try (GestionnaireTournois gestionnaire = new GestionnaireTournois(
        100, 0.10, StrategieVainqueur.favoriserMieuxClassee(), 42, JpaRapportRepository.fichier())) {
    gestionnaire.ajouterTournoi(Tournoi.builder()
            .nom("Coupe d'Hiver").jeu(FabriqueJeux.avecCatalogueParDefaut().creer("Rocket League"))
            .date(LocalDate.of(2026, 12, 10)).budget(8000).arenes(3)
            .equipes(equipes).arbitre(arbitre).build());
    Future<RapportTournoi> resultat = gestionnaire.simulerProchainTournoi();
    RapportTournoi rapport = resultat.get(5, TimeUnit.SECONDS);
}
```
