package com.hugoof06.pokedex.model;

import java.util.List;
import java.util.Objects;

public class Pokemon {

    private final int id;
    private final String name;
    private final Generation generation;
    private final List<Type> types;
    private final Stats stats;

    public Pokemon(int id, String name, Generation generation, List<Type> types, Stats stats) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Pokemon name cannot be null or empty");
        }
        if (types == null || types.isEmpty()) {
            throw new IllegalArgumentException("Pokemon must have at least one type");
        }
        if (stats == null) {
            throw new IllegalArgumentException("Pokemon stats cannot be null");
        }

        this.id = id;
        this.name = name;
        this.generation = generation;
        this.types = List.copyOf(types); // evita modificaciones externas
        this.stats = stats;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Generation getGeneration() {
        return generation;
    }

    public List<Type> getTypes() {
        return types;
    }

    public Stats getStats() {
        return stats;
    }

    @Override
    public String toString() {
        return String.format(
            "#%03d %s %s",
            id,
            name,
            types
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pokemon)) return false;
        Pokemon pokemon = (Pokemon) o;
        return id == pokemon.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

