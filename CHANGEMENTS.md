# Ce qui a changé par rapport à notre version

Le point de départ est notre dépôt `tp-objet` : mêmes classes et mêmes noms de méthodes. Les
hiérarchies de personnes et de jeux sont maintenant regroupées dans les packages dédiés
`fr.imt.projet.personnes` et `fr.imt.projet.jeux`. Ce document liste **tout** ce qui a été modifié
ou ajouté, et pourquoi.

Il y a 58 tests au total (contre 23 avant), tous réussis.

## 1. Ce qui manquait par rapport au sujet

| Exigence du sujet | Avant | Maintenant |
|---|---|---|
| Persistance JPA/H2 des rapports | Absente | `RapportTournoi` est une `@Entity`, sauvegardée dans `jdbc:h2:./data/arena` par `JpaRapportRepository`. On retrouve tous les rapports, ou ceux d'un jeu donné. |
| Rapport : id, date, matchs prévus | Absents | Ajoutés au rapport |
| Pattern pour désigner le vainqueur | Formule codée en dur dans le simulateur | **Stratégie** : `StrategieVainqueur` (équitable, favorise le mieux classé, toujours le mieux classé) |
| Pattern pour prévenir plusieurs éléments | Absent | **Observateur** : `ObservateurTournoi`. Le tournoi prévient le gestionnaire, l'affichage, etc. |
| Créer un jeu à partir d'un nom | Absent | **Fabrique** : `FabriqueJeux.creer("Rocket League")` |
| Construire un tournoi en le validant | Constructeur seul | **Monteur** : `Tournoi.builder()…build()`. Le constructeur existe toujours. |
| Tournois planifiés, le plus proche d'abord | `List` + recherche du minimum | `PriorityQueue<Tournoi>`, et `Tournoi` est `Comparable` (date puis nom) |
| Accès direct à une équipe par son nom | Absent | `HashMap<String, Equipe>` dans le gestionnaire : `getEquipe(nom)` |
| Classement sans doublon, par points puis par nom | Absent | `getClassementEquipes()` renvoie un `TreeSet` de lignes figées |
| Tournois terminés, du plus gros budget au plus petit | Absent | `getTournoisTermines()` avec le `Comparator` `Tournoi.PAR_BUDGET_DECROISSANT` |
| Arbitres disponibles et occupés | Absent | `getArbitresDisponibles()` / `getArbitresOccupes()` |
| Matchs d'une équipe | Absent | `getMatchsDe(equipe)` |
| Matchs regroupés par équipe gagnante | Absent | `Tournoi.getMatchsParEquipeGagnante()` renvoie une `TreeMap` |
| Programme principal avec une démo | `Bonjour, Maven !` | Mini-saison complète : 8 équipes générées avec un `Random` à graine fixe, 4 tournois, 2 simulés en parallèle |

## 2. Règles qui pouvaient être contournées

| Problème | Correction |
|---|---|
| L'équipe d'un joueur ou d'un coach était un simple `String` modifiable. Un même joueur pouvait donc être dans deux équipes. | `Joueur.getEquipe()` et `Coach.getEquipe()` renvoient l'objet `Equipe`. Seule `Equipe.recruter()` / `liberer()` / `changerCoach()` peut le modifier, et elle vérifie. |
| `Personne` avait des setters publics (`setNom`, `setId`…). | Attributs `final`, plus de setters. |
| `Equipe.setNombreDePoints()` était public, et le constructeur acceptait des points de départ. | Les points ne s'obtiennent qu'en gagnant un match. Le constructeur devient `new Equipe(nom, coach, joueurs...)`. |
| On pouvait changer la composition d'une équipe pendant un tournoi. | La composition est gelée tant que l'équipe dispute un tournoi en cours. |
| Une équipe pouvait jouer deux matchs en même temps dans deux arènes. | `Tournoi.demarrerMatch()` le refuse. |
| Jeu solo : il fallait **exactement** 1 joueur dans l'équipe. | Il en faut **au moins** 1 : l'équipe a plusieurs joueurs et en désigne un, comme le dit le sujet. |
| Un arbitre déjà occupé pouvait officier dans un autre tournoi en cours. | Le conflit est aussi vérifié pour les arbitres. |

## 3. Simulation et threads

- **Simulation par vagues** : avant, le simulateur attendait toute la durée, puis jouait tous les
  matchs d'un coup. Maintenant, les matchs se jouent par vagues. Une vague contient au plus un match
  par arène, sans qu'une équipe y figure deux fois. Chaque match est tiré au sort à la fin de sa
  vague, et la durée totale respecte toujours la formule matchs × temps ÷ arènes.
- **Arrêt propre** : avant, `close()` interrompait tout immédiatement. Maintenant, la fermeture se
  fait en trois temps : `shutdown()`, puis jusqu'à 2 secondes d'attente, puis `shutdownNow()`. Les
  tournois interrompus produisent quand même leur rapport.
- **Fin de tournoi** : le gestionnaire est un observateur. À chaque fin de tournoi, il range le
  tournoi parmi les terminés, archive le rapport et met à jour les classements. Le rang 1 revient
  à l'équipe qui a le plus de points ; celui des coachs dépend de leurs victoires en carrière.
  C'est ce classement qu'utilise la stratégie « favoriser la mieux classée ».
- Les threads s'appellent `simulation-1`, `simulation-2`…, ce qui permet de voir le parallélisme
  dans la démo.
- `Joueur`, `Coach` et `Equipe` ont des accesseurs `synchronized`, car ils sont lus par le thread
  principal pendant qu'un thread de simulation les modifie. Le compteur de l'arbitre est un
  `AtomicInteger`.
- Un observateur qui lève une exception est journalisé et n'interrompt jamais le tournoi.

## 4. Petites modifications

- `JeuxIndividuel` a été **supprimée**. Elle héritait de `JeuxSolo` sans rien ajouter : c'était de
  l'héritage « par plaisir », ce que le sujet déconseille.
- Les jeux d'un arbitre sont rangés dans un `TreeSet` (liste triée) au lieu d'un `HashSet`.
- `Match.Statut.estTermine()` a été ajoutée, ainsi que des `toString()` lisibles pour `Jeux`, `Match`,
  `Tournoi` et `RapportTournoi`.
- Dans le `pom.xml` :
  - Java 21 au lieu de 25 (fonctionne aussi avec un JDK 25) ;
  - ajout de Hibernate et de H2 ;
  - ajout de `exec-maven-plugin` pour lancer la démo ;
  - Hibernate rendu silencieux pendant les tests.

## 5. Les tests

- Nos tests existants ont été **conservés**. Ils ont seulement été adaptés aux nouveaux
  constructeurs, par exemple `new Equipe(nom, coach, joueur)` au lieu de `new Equipe(nom, coach, new Joueur[]{joueur}, 0)`.
- Le premier test de `TournoiTest` joue ses matchs un par un, car une équipe ne peut plus jouer
  deux matchs en même temps.
- `MainTest` exécute maintenant la démo complète, avec une base en mémoire.
- Deux fichiers de tests sont nouveaux :
  - `EquipeTest` : un joueur ou un coach dans une seule équipe, passage pro (test paramétré),
    `@BeforeEach` ;
  - `GestionnaireListesTest` : builder, toutes les listes demandées, stratégies (tests paramétrés),
    observateurs, vagues de simulation, persistance JPA (`@BeforeEach` / `@AfterEach`).
