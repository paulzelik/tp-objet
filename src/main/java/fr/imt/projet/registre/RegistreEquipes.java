package fr.imt.projet.registre;

import fr.imt.projet.exceptions.RegleMetierException;
import fr.imt.projet.jeux.Equipe;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class RegistreEquipes {
    private final Map<String, Equipe> equipesParNom = new ConcurrentHashMap<>();

    public void enregistrer(Equipe equipe) {
        Objects.requireNonNull(equipe, "equipe ne peut pas être null");
        if (equipesParNom.containsKey(equipe.getNom().toLowerCase())) {
            throw new RegleMetierException("Une équipe avec le nom " + equipe.getNom() + " existe déjà");
        }
        equipesParNom.put(equipe.getNom().toLowerCase(), equipe);
    }

    public Equipe trouverParNom(String nom) {
        if (nom == null) return null;
        return equipesParNom.get(nom.toLowerCase());
    }

    public Collection<Equipe> getToutesLesEquipes() {
        return Collections.unmodifiableCollection(equipesParNom.values());
    }
}

