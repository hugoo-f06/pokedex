package com.hugoof06.pokedex.data;

import java.util.List;
import java.util.Optional;

import com.hugoof06.pokedex.model.Generation;
import com.hugoof06.pokedex.model.Pokemon;
import com.hugoof06.pokedex.model.Type;

public interface PokemonRepository {

    List<Pokemon> findAll();

    List<Pokemon> findAll(Generation gen);

    List<Pokemon> list(Generation gen, int offset, int limit);

    Optional<Pokemon> findById(int id);

    Optional<Pokemon> findByName(String name);

    List<Pokemon> findByType(Type type, Generation gen);
}

