package com.hugoof06.pokedex.favorites;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class FavoritesService {

    private static final int MAX_FAVORITES = 20;

    private final FavoritesRepository repo;
    private FavoritesData data;

    public FavoritesService(FavoritesRepository repo) {
        this.repo = repo;
        this.data = repo.load();

        // Normaliza: sin duplicados, mantiene orden
        var set = new LinkedHashSet<>(data.favoriteIds);
        data.favoriteIds = new ArrayList<>(set);

        // Si excede, recorta
        if (data.favoriteIds.size() > MAX_FAVORITES) {
            data.favoriteIds = data.favoriteIds.subList(0, MAX_FAVORITES);
            repo.save(data);
        }
    }

    public List<Integer> listIds() {
        return List.copyOf(data.favoriteIds);
    }

    public boolean add(int id) {
        if (data.favoriteIds.contains(id)) return false;
        if (data.favoriteIds.size() >= MAX_FAVORITES) {
            throw new IllegalStateException("Favorites limit reached (" + MAX_FAVORITES + ")");
        }
        data.favoriteIds.add(id);
        repo.save(data);
        return true;
    }

    public boolean remove(int id) {
        boolean removed = data.favoriteIds.remove(Integer.valueOf(id));
        if (removed) repo.save(data);
        return removed;
    }

    public int capacity() {
        return MAX_FAVORITES;
    }

    public int size() {
        return data.favoriteIds.size();
    }

    public String fileLocation() {
        return repo.getPath().toString();
    }
}
