package com.hugoof06.pokedex.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hugoof06.pokedex.model.Pokemon;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class FilePokemonCache {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path cacheDir;

    public FilePokemonCache() {
        String home = System.getProperty("user.home");
        this.cacheDir = Path.of(home, ".pokedex", "cache");
    }

    private Path fileForId(int id) {
        return cacheDir.resolve("pokemon_" + id + ".json");
    }

    public Optional<Pokemon> getById(int id) {
        try {
            Path f = fileForId(id);
            if (!Files.exists(f)) return Optional.empty();
            return Optional.of(mapper.readValue(f.toFile(), Pokemon.class));
        } catch (IOException e) {
            // Si el archivo está corrupto, mejor ignorar y tratarlo como "no cacheado"
            return Optional.empty();
        }
    }

    public void put(Pokemon p) {
        try {
            Files.createDirectories(cacheDir);
            Path f = fileForId(p.getId());
            mapper.writerWithDefaultPrettyPrinter().writeValue(f.toFile(), p);
        } catch (IOException e) {
            // Caché es best-effort: si falla, no rompemos la app
        }
    }

    public String location() {
        return cacheDir.toString();
    }

    public int countCachedPokemons() {
        if (!Files.exists(cacheDir)) return 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cacheDir, "pokemon_*.json")) {
            int count = 0;
            for (Path ignored : stream) count++;
            return count;
        } catch (IOException e) {
            return 0;
        }
    }

    public int clear() {
        if (!Files.exists(cacheDir)) return 0;

        int removed = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(cacheDir, "pokemon_*.json")) {
            for (Path p : stream) {
                Files.deleteIfExists(p);
                removed++;
            }
        } catch (IOException e) {
            // best-effort
        }
        return removed;
    }

}
