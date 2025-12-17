package com.hugoof06.pokedex.data;

import com.hugoof06.pokedex.cache.FilePokemonCache;
import com.hugoof06.pokedex.model.Generation;
import com.hugoof06.pokedex.model.Pokemon;
import com.hugoof06.pokedex.model.Type;

import java.util.List;
import java.util.Optional;

public class CachedPokemonRepository implements PokemonRepository {

    private final PokemonRepository source;
    private final FilePokemonCache cache;

    public CachedPokemonRepository(PokemonRepository source, FilePokemonCache cache) {
        this.source = source;
        this.cache = cache;
    }

    @Override
    public Optional<Pokemon> findById(int id) {
        // 1) cache
        Optional<Pokemon> cached = cache.getById(id);
        if (cached.isPresent()) return cached;

        // 2) source
        Optional<Pokemon> fromSource = source.findById(id);
        fromSource.ifPresent(cache::put);
        return fromSource;
    }

    @Override
    public Optional<Pokemon> findByName(String name) {
        // Buscar por nombre depende de la fuente: lo delegamos
        Optional<Pokemon> p = source.findByName(name);
        p.ifPresent(cache::put);
        return p;
    }

    @Override
    public List<Pokemon> findAll() {
        List<Pokemon> list = source.findAll();
        list.forEach(cache::put);
        return list;
    }

    @Override
    public List<Pokemon> findAll(Generation gen) {
        List<Pokemon> list = source.findAll(gen);
        list.forEach(cache::put);
        return list;
    }

    @Override
    public List<Pokemon> findByType(Type type, Generation gen) {
        List<Pokemon> list = source.findByType(type, gen);
        list.forEach(cache::put);
        return list;
    }

    @Override
    public List<Pokemon> list(Generation gen, int offset, int limit) {
        List<Pokemon> page = source.list(gen, offset, limit);
        page.forEach(cache::put);
        return page;
    }
}
