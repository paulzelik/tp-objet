package fr.imt.projet.registre;

import fr.imt.projet.exceptions.RegleMetierException;
import fr.imt.projet.personne.Personne;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class RegistrePersonnes {
    private final Map<Long, Personne> personnesParId = new ConcurrentHashMap<>();

    public void enregistrer(Personne personne) {
        Objects.requireNonNull(personne, "personne ne peut pas être null");
        if (personnesParId.containsKey(personne.getId())) {
            throw new RegleMetierException("Une personne avec l'id " + personne.getId() + " existe déjà");
        }
        personnesParId.put(personne.getId(), personne);
    }

    public Personne trouverParId(long id) {
        return personnesParId.get(id);
    }

    public Collection<Personne> getToutesLesPersonnes() {
        return Collections.unmodifiableCollection(personnesParId.values());
    }
}

