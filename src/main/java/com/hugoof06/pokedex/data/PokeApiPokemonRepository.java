package com.hugoof06.pokedex.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hugoof06.pokedex.model.Generation;
import com.hugoof06.pokedex.model.Pokemon;
import com.hugoof06.pokedex.model.Stats;
import com.hugoof06.pokedex.model.Type;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class PokeApiPokemonRepository implements PokemonRepository {

    private static final String BASE = "https://pokeapi.co/api/v2";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Optional<Pokemon> findById(int id) {
        if (id <= 0) return Optional.empty();
        return fetchPokemon(String.valueOf(id));
    }

    @Override
    public Optional<Pokemon> findByName(String name) {
        if (name == null) return Optional.empty();
        String n = name.trim().toLowerCase(Locale.ROOT);
        if (n.isEmpty()) return Optional.empty();
        return fetchPokemon(n);
    }

    @Override
    public List<Pokemon> list(Generation gen, int offset, int limit) {
        if (offset < 0) offset = 0;
        if (limit < 1) limit = 20;

        int genId = generationToId(gen);

        JsonNode genJson = getJson("/generation/" + genId + "/");
        JsonNode species = genJson.get("pokemon_species");
        if (species == null || !species.isArray()) return List.of();

        // 1) construir lista (id, name) desde la URL
        record SpeciesRef(int id, String name) {}
        List<SpeciesRef> refs = new ArrayList<>();

        for (JsonNode sp : species) {
            String name = sp.get("name").asText();
            String url = sp.get("url").asText(); // .../pokemon-species/25/
            int id = extractTrailingInt(url);
            refs.add(new SpeciesRef(id, name));
        }

        // 2) ordenar por ID (Pokedex nacional dentro de la generación)
        refs.sort(Comparator.comparingInt(SpeciesRef::id));

        // 3) aplicar offset/limit ya sobre el orden correcto
        int end = Math.min(offset + limit, refs.size());
        if (offset >= end) return List.of();

        List<Pokemon> page = new ArrayList<>();
        for (int i = offset; i < end; i++) {
            SpeciesRef r = refs.get(i);
            fetchPokemon(r.name()).ifPresent(page::add);
        }

        // ya vienen “casi” ordenados, pero por seguridad:
        page.sort(Comparator.comparingInt(Pokemon::getId));
        return page;
    }


    @Override
    public List<Pokemon> findAll(Generation gen) {
        // OJO: esto descarga TODA la generación (Gen1=151 está OK; más adelante optimizamos).
        List<Pokemon> all = new ArrayList<>();
        int offset = 0;
        int limit = 50;

        while (true) {
            List<Pokemon> page = list(gen, offset, limit);
            if (page.isEmpty()) break;
            all.addAll(page);
            offset += limit;
        }
        return all;
    }

    @Override
    public List<Pokemon> findAll() {
        // Evita esto en API (demasiado grande). Si lo necesitas luego, lo planteamos con paginación global.
        return List.of();
    }

    @Override
    public List<Pokemon> findByType(Type type, Generation gen) {
        // Implementación simple por ahora: filtra en memoria sobre la generación.
        // (Luego lo optimizamos con el endpoint /type/{name}.)
        return findAll(gen).stream()
                .filter(p -> p.getTypes().contains(type))
                .toList();
    }

    @Override
    public List<Pokemon> listByType(Type type, Generation gen, int offset, int limit) {
        if (offset < 0) offset = 0;
        if (limit < 1) limit = 20;

        // /type/{name} devuelve una lista "pokemon" con referencias :contentReference[oaicite:1]{index=1}
        String typeName = type.name().toLowerCase(Locale.ROOT);
        JsonNode typeJson = getJson("/type/" + typeName + "/");
        JsonNode arr = typeJson.get("pokemon");
        if (arr == null || !arr.isArray()) return List.of();

        // Construimos refs (id, name) y ordenamos por id
        record Ref(int id, String name) {}
        List<Ref> refs = new ArrayList<>();
        for (JsonNode entry : arr) {
            JsonNode p = entry.get("pokemon");
            String name = p.get("name").asText();
            String url = p.get("url").asText();          // .../pokemon/25/
            int id = extractTrailingInt(url);
            refs.add(new Ref(id, name));
        }
        refs.sort(Comparator.comparingInt(Ref::id));

        // Ahora aplicamos offset/limit sobre los que sean de esa generación
        List<Pokemon> page = new ArrayList<>();
        int seenMatching = 0;

        for (Ref r : refs) {
            Optional<Pokemon> opt = fetchPokemon(r.name()); // esto ya cachea luego por el wrapper
            if (opt.isEmpty()) continue;

            Pokemon p = opt.get();
            if (p.getGeneration() != gen) continue;

            if (seenMatching < offset) {
                seenMatching++;
                continue;
            }

            page.add(p);
            seenMatching++;

            if (page.size() >= limit) break;
        }

        return page;
    }


    // ----------------- Internals -----------------

    private Optional<Pokemon> fetchPokemon(String idOrName) {
        try {
            JsonNode p = getJson("/pokemon/" + idOrName + "/"); // :contentReference[oaicite:3]{index=3}
            int id = p.get("id").asInt();

            String name = capitalize(p.get("name").asText());

            List<Type> types = new ArrayList<>();
            for (JsonNode t : p.get("types")) {
                String typeName = t.get("type").get("name").asText();
                types.add(toTypeEnum(typeName));
            }

            Stats stats = parseStats(p.get("stats"));

            // Generación: viene en /pokemon-species/{id} dentro del campo "generation" :contentReference[oaicite:4]{index=4}
            Generation gen = fetchGenerationForSpecies(id);

            return Optional.of(new Pokemon(id, name, gen, types, stats));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Generation fetchGenerationForSpecies(int id) {
        JsonNode s = getJson("/pokemon-species/" + id + "/"); // :contentReference[oaicite:5]{index=5}
        JsonNode g = s.get("generation");
        if (g == null) return Generation.GEN_1;

        // "url": ".../generation/4/" -> sacamos el número
        String url = g.get("url").asText();
        int genId = extractTrailingInt(url);
        return idToGeneration(genId);
    }

    private Stats parseStats(JsonNode statsArr) {
        int hp = 0, atk = 0, def = 0, spAtk = 0, spDef = 0, spd = 0;

        for (JsonNode s : statsArr) {
            String statName = s.get("stat").get("name").asText();
            int base = s.get("base_stat").asInt();

            switch (statName) {
                case "hp" -> hp = base;
                case "attack" -> atk = base;
                case "defense" -> def = base;
                case "special-attack" -> spAtk = base;
                case "special-defense" -> spDef = base;
                case "speed" -> spd = base;
            }
        }
        return new Stats(hp, atk, def, spAtk, spDef, spd);
    }

    private JsonNode getJson(String path) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(BASE + path))
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new RuntimeException("HTTP " + resp.statusCode() + " for " + path);
            }
            return mapper.readTree(resp.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Request failed for " + path, e);
        }
    }

    private static int generationToId(Generation g) {
        return switch (g) {
            case GEN_1 -> 1;
            case GEN_2 -> 2;
            case GEN_3 -> 3;
            case GEN_4 -> 4;
            case GEN_5 -> 5;
            case GEN_6 -> 6;
            case GEN_7 -> 7;
            case GEN_8 -> 8;
            case GEN_9 -> 9;
        };
    }

    private static Generation idToGeneration(int id) {
        return switch (id) {
            case 1 -> Generation.GEN_1;
            case 2 -> Generation.GEN_2;
            case 3 -> Generation.GEN_3;
            case 4 -> Generation.GEN_4;
            case 5 -> Generation.GEN_5;
            case 6 -> Generation.GEN_6;
            case 7 -> Generation.GEN_7;
            case 8 -> Generation.GEN_8;
            case 9 -> Generation.GEN_9;
            default -> Generation.GEN_1;
        };
    }

    private static int extractTrailingInt(String url) {
        // Ej: https://pokeapi.co/api/v2/generation/4/
        String u = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        int slash = u.lastIndexOf('/');
        return Integer.parseInt(u.substring(slash + 1));
    }

    private static Type toTypeEnum(String apiTypeName) {
        // "water" -> WATER, "fighting" -> FIGHTING
        // Si algún día hay guiones, esto lo soporta: "something-name" -> SOMETHING_NAME
        String s = apiTypeName.toUpperCase(Locale.ROOT).replace('-', '_');
        return Type.valueOf(s);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
