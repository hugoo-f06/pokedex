package com.hugoof06.pokedex.favorites;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FavoritesRepository {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path path;

    public FavoritesRepository() {
        String home = System.getProperty("user.home");
        this.path = Path.of(home, ".pokedex", "favorites.json");
    }

    public FavoritesData load() {
        try {
            if (!Files.exists(path)) {
                return new FavoritesData();
            }
            return mapper.readValue(path.toFile(), FavoritesData.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read favorites file: " + path, e);
        }
    }

    public void save(FavoritesData data) {
        try {
            Files.createDirectories(path.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write favorites file: " + path, e);
        }
    }

    public Path getPath() {
        return path;
    }
}
