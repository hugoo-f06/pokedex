package com.hugoof06.pokedex.model;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Pokemon {

    private final int id;
    private final String name;
    private final Generation generation;
    private final List<Type> types;
    private final Stats stats;
    private String spriteUrl;

    @JsonCreator
    public Pokemon(
            @JsonProperty("id") int id,
            @JsonProperty("name") String name,
            @JsonProperty("generation") Generation generation,
            @JsonProperty("types") List<Type> types,
            @JsonProperty("stats") Stats stats,
            @JsonProperty("spriteUrl") String spriteUrl
    ) {
        this.id = id;
        this.name = name;
        this.generation = generation;
        this.types = List.copyOf(types);
        this.stats = stats;
        this.spriteUrl = spriteUrl; // puede ser null
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

    public String getSpriteUrl() {
        return spriteUrl;
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

