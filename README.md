# IMT Arena — modèle de tournois

Projet Java qui modélise les personnes, les équipes, les matchs et les tournois d'une compétition.
Les règles métier sont portées par les classes du package `fr.imt.projet`.

> Le modèle métier est implémenté, mais `Main` n'exécute pas encore de scénario de tournoi :
> pour le moment, l'application affiche uniquement `Bonjour, Maven !`.

## Prérequis

- JDK 25 ou plus récent
- Apache Maven 3.9 ou plus récent

## Compiler, tester et lancer

À la racine du projet :

```powershell
mvn test
mvn package
java -cp target/classes fr.imt.projet.Main
```

Les tests utilisent JUnit 5. Ils couvrent notamment les règles des jeux et des matchs, le calendrier des
tournois, les conditions de démarrage, les arènes, les incidents techniques et le rapport final.

## Organisation du code

Les sources Java se trouvent dans `src/main/java/fr/imt/projet` et les tests dans
`src/test/java/fr/imt/projet`.

### Personnes et capacités

- `Personne` est la classe de base d'`Arbitre`, de `Coach` et de `Joueur`. Elle contient les informations
  communes : identifiant, nom, prénom, pseudo, genre et téléphone.
- `Classable` représente la capacité à recevoir un classement. `Coach` et `Joueur` l'implémentent.
- `QualifiablePourJeu` représente la capacité à être qualifié pour un jeu. `Arbitre` l'implémente.
- Un arbitre conserve les noms des jeux pour lesquels il est qualifié. Il ne peut arbitrer un match que
  pour un jeu correspondant à une qualification ; le compteur de matchs arbitrés augmente au démarrage
  du match.
- Un joueur amateur devient professionnel après trois victoires de son équipe depuis son arrivée. Un
  changement d'équipe remet son compteur de victoires à zéro.
- Le coach conserve le nombre de victoires de son équipe en carrière.

### Jeux

`Jeux` décrit un jeu avec son nom, son nombre de manches et son nombre de joueurs par équipe.

- `JeuxEquipe` représente un jeu par équipe et accepte de 3 à 5 joueurs par équipe.
- `JeuxSolo` représente un jeu individuel (un joueur par équipe) et vérifie que le joueur sélectionné
  appartient bien à l'équipe.
- `JeuxIndividuel` est une spécialisation de `JeuxSolo`.

### Équipes et matchs

`Equipe` regroupe un nom, un coach, un tableau de joueurs et un total de points. Les noms ne peuvent pas
être nuls ou vides, les joueurs sont copiés à l'entrée et à la sortie, et les points ne peuvent pas être
négatifs.

`Match` oppose deux équipes distinctes pour un jeu, sous la responsabilité d'un arbitre. Son statut suit
le cycle `PLANIFIE` → `EN_COURS` → `JOUE` ou `INCIDENT_TECHNIQUE`.

- Le match ne peut démarrer qu'une fois ; le démarrage vérifie la qualification de l'arbitre.
- Une victoire doit être attribuée à l'une des deux équipes et rapporte 3 points. Elle incrémente aussi
  les victoires du coach et des joueurs de l'équipe gagnante.
- Il n'existe pas de résultat nul. Un match interrompu par un incident technique ne donne pas de points.

### Tournois

`Tournoi` contient un nom, un jeu, une date, un budget (en `BigDecimal`), de 1 à 4 arènes, les équipes
inscrites, les arbitres, les matchs et le statut `PLANIFIE`, `EN_COURS` ou `TERMINE`.

Les inscriptions sont possibles tant que le tournoi est planifié. Pour le démarrer, il faut :

1. Au moins 4 équipes.
2. Un coach pour chaque équipe et le nombre exact de joueurs demandé par le jeu.
3. Des joueurs distincts dans les équipes inscrites.
4. Au moins un arbitre qualifié pour le jeu du tournoi.

Au démarrage, chaque paire d'équipes reçoit exactement un match. Le nombre de matchs est donc `n × (n − 1) / 2`
(par exemple, 15 matchs pour 6 équipes). Les arbitres qualifiés sont affectés à tour de rôle.

Les matchs sont ensuite démarrés explicitement avec `demarrerMatch`. Le nombre de matchs simultanément en cours
ne peut dépasser le nombre d'arènes. Chaque match doit être déclaré joué avec un vainqueur, ou terminé avec un
incident technique. Quand tous les matchs sont traités, le tournoi se termine automatiquement et produit un
`RapportTournoi` avec le nom du tournoi, le jeu, le nombre de matchs joués, le vainqueur et le résultat de
réussite. Le tournoi est réussi si au moins 70 % des matchs ont été joués.

Le vainqueur est l'équipe qui totalise le plus de points gagnés dans ce tournoi. En cas d'égalité, les résultats
des confrontations directes entre les équipes à égalité servent au départage. Si l'égalité persiste, le rapport
ne désigne pas de vainqueur (`getVainqueur()` renvoie `null`).

`GestionnaireTournois` conserve les tournois planifiés et en cours. Les méthodes
`lancerProchainTournoi()` et `simulerProchainTournoi()` choisissent le tournoi planifié à la date la plus proche,
refusent le démarrage s'il y a déjà 3 tournois en cours, et empêchent qu'une équipe, un coach ou un joueur
participe simultanément à deux tournois en cours.

`lancerProchainTournoi()` prépare un tournoi pour un déroulement contrôlé manuellement. Pour lancer la simulation
automatique dans un thread du pool, utiliser `simulerProchainTournoi()`. La simulation attend
`(nombre de matchs × temps par match) / nombre d'arènes` millisecondes, puis tire au sort les incidents (10 % par
défaut) et les vainqueurs. Un meilleur classement du coach augmente la probabilité de victoire ; en l'absence de
classement, les équipes ont les mêmes chances. Aucun jeu n'est effectivement joué.

Le résultat est un `Future<RapportTournoi>` : l'état du tournoi peut être consulté avec `getStatut()`, et l'appelant
peut attendre le rapport pendant une durée maximale avec `Future.get(délai, unité)`. Le rapport est également
accessible via `getRapport()` une fois le tournoi terminé. Les accès synchronisés publient l'état et le rapport
uniquement à des étapes cohérentes.

Le gestionnaire accepte un temps par match, une probabilité d'incident et une graine aléatoire pour rendre les
simulations reproductibles :

```java
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

try (GestionnaireTournois gestionnaire = new GestionnaireTournois(100, 0.10, 42)) {
    gestionnaire.ajouterTournoi(tournoi);
    Future<RapportTournoi> resultat = gestionnaire.simulerProchainTournoi();
    RapportTournoi rapport = resultat.get(5, TimeUnit.SECONDS);
}
```

Le `try` utilise `GestionnaireTournois` comme `AutoCloseable`. Sa fermeture arrête le pool et attend la fin de ses
threads. Si une simulation est interrompue à la fermeture, ses matchs non terminés sont clôturés comme incidents
techniques afin de produire le rapport final.

## Parcours d'utilisation

1. Créer le jeu, les équipes, les coachs et les joueurs.
2. Créer un tournoi avec son budget, sa date et son nombre d'arènes.
3. Inscrire les équipes et au moins un arbitre qualifié pour ce jeu.
4. Enregistrer le tournoi dans un `GestionnaireTournois`, puis choisir l'un des modes :
   `lancerProchainTournoi()` pour le piloter manuellement, ou `simulerProchainTournoi()` pour lancer la simulation
   asynchrone.
5. En mode manuel, pour chaque match du calendrier, appeler `demarrerMatch(match)`, puis soit
   `declarerVainqueur(match, equipe)`, soit `signalerIncidentTechnique(match)`.
6. En mode simulation, consulter `getStatut()` pendant l'exécution et attendre le `Future` avec un délai maximal.
   Une fois terminé, lire le rapport avec `getRapport()` ou via le résultat du `Future`.

## Tests

Les tests JUnit se trouvent dans `src/test/java/fr/imt/projet`. Pour ne lancer qu'une classe de tests :

```powershell
mvn -Dtest=TournoiTest test
```
