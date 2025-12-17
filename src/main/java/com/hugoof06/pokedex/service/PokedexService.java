package com.hugoof06.pokedex.service;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.hugoof06.pokedex.data.PokemonRepository;
import com.hugoof06.pokedex.model.Generation;
import com.hugoof06.pokedex.model.Pokemon;
import com.hugoof06.pokedex.model.Type;

public class PokedexService {

    private final PokemonRepository repo;

    public PokedexService(PokemonRepository repo) {
        this.repo = repo;
    }

    public List<Pokemon> list(Generation gen) {
        return repo.findAll(gen);
    }

    public List<Pokemon> list(Generation gen, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;

        int offset = (page - 1) * pageSize;
        return repo.list(gen, offset, pageSize);
    }

    public Optional<Pokemon> show(String idOrName) {
        if (idOrName == null) return Optional.empty();
        String s = idOrName.trim();
        if (s.isEmpty()) return Optional.empty();

        // Si es número -> buscar por id
        if (s.matches("\\d+")) {
            int id = Integer.parseInt(s);
            return repo.findById(id);
        }

        // Si no -> buscar por nombre
        return repo.findByName(s);
    }

    public List<Pokemon> byType(Generation gen, Type type) {
        return repo.findByType(type, gen);
    }

    public List<Pokemon> search(Generation gen, String text) {
        String q = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        return repo.findAll(gen).stream()
                .filter(p -> p.getName().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    public List<Pokemon> byType(Generation gen, Type type, int page, int pageSize) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;

        int offset = (page - 1) * pageSize;
        return repo.listByType(type, gen, offset, pageSize);
    }

}
