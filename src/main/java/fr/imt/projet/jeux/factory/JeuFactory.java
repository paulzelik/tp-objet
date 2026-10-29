package fr.imt.projet.jeux.factory;

import fr.imt.projet.jeux.Jeux;
import fr.imt.projet.exceptions.RegleMetierException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class JeuFactory {
    private static final Map<String, Supplier<Jeux>> REGISTRE = new ConcurrentHashMap<>();

    public static void enregistrerJeu(String nom, Supplier<Jeux> supplier) {
        REGISTRE.put(nom.toLowerCase(), supplier);
    }

    public static Jeux creerJeu(String nom) {
        Supplier<Jeux> supplier = REGISTRE.get(nom.toLowerCase());
        if (supplier == null) {
            throw new RegleMetierException("Jeu non enregistré dans la factory : " + nom);
        }
        return supplier.get();
    }

    public static boolean estEnregistre(String nom) {
        return REGISTRE.containsKey(nom.toLowerCase());
    }
}

