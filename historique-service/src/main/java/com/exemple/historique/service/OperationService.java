package com.exemple.historique.service;

import com.exemple.historique.model.Operation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OperationService {

    // Pas de "static" : Spring ne crée qu'un seul OperationService
    private final Map<Integer, Operation> operations = new ConcurrentHashMap<>();
    private final AtomicInteger compteur = new AtomicInteger(0);

    public Operation ajouter(Operation op) {
        int id = compteur.incrementAndGet();
        op.setId(id);
        operations.put(id, op);
        return op;
    }

    public List<Operation> lister() {
        return new ArrayList<>(operations.values());
    }

    public Operation lire(int id) {
        return operations.get(id); // null si l'identifiant n'existe pas
    }

    public boolean existe(int id) {
        return operations.containsKey(id);
    }

    public void remplacer(int id, Operation op) {
        op.setId(id);
        operations.put(id, op);
    }

    public Operation supprimer(int id) {
        return operations.remove(id); // null si l'identifiant n'existait pas
    }
}