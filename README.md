# IMT Arena

Projet de TP de Conception Orientée Objet (CI1 FISA ITR) - Paul BETINELI et Badr BOURHIM.

Le programme gère des tournois e-sport : il crée les équipes, lance les tournois, les simule en parallèle et enregistre le rapport de chaque tournoi dans une base de données.

## Ce qu'il faut

* Java 25
* Maven

## Lancer le main

Dans le dossier du projet :

```
mvn compile exec:java
```

Le main crée 8 équipes et 3 tournois, en lance 2 en même temps, puis affiche les résultats et les rapports enregistrés dans la base.

## Lancer les tests

```
mvn test
```

Les tests sont dans `src/test/java` :

* `JeuxTest` : vérifie les jeux (nombre de joueurs, valeurs interdites)
* `RapportRepositoryTest` : vérifie que les rapports sont bien enregistrés et relus
* `TournoiTest` : joue un tournoi complet et vérifie qu'on ne dépasse jamais 3 tournois en cours

## Dossier de conception

Il se trouve dans `docs/dossier-conception.pdf`.

