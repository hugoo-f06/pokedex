package com.hugoof06.pokedex.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hugoof06.pokedex.model.Generation;
import com.hugoof06.pokedex.model.Pokemon;
import com.hugoof06.pokedex.model.Type;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class JsonPokemonRepository implements PokemonRepository {

    private final List<Pokemon> all;

    public JsonPokemonRepository() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            InputStream is = this.getClass().getResourceAsStream("/data/pokemon_gen1.json");
            if (is == null) {
                throw new IllegalStateException("Cannot find /data/pokemon_gen1.json in resources");
            }

            this.all = mapper.readValue(is, new TypeReference<List<Pokemon>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to load pokemon data from JSON", e);
        }
    }

    @Override
    public List<Pokemon> findAll() {
        return all;
    }

    @Override
    public List<Pokemon> findAll(Generation gen) {
        return all.stream()
                .filter(p -> p.getGeneration() == gen)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Pokemon> findById(int id) {
        return all.stream()
                .filter(p -> p.getId() == id)
                .findFirst();
    }

    @Override
    public Optional<Pokemon> findByName(String name) {
        if (name == null) return Optional.empty();
        String target = name.trim().toLowerCase(Locale.ROOT);

        return all.stream()
                .filter(p -> p.getName().toLowerCase(Locale.ROOT).equals(target))
                .findFirst();
    }

    @Override
    public List<Pokemon> findByType(Type type, Generation gen) {
        return all.stream()
                .filter(p -> p.getGeneration() == gen)
                .filter(p -> p.getTypes().contains(type))
                .collect(Collectors.toList());
    }
}

