package com.hugoof06.pokedex.cli;

import java.util.Scanner;
import java.util.Comparator;
import java.util.stream.Collectors;

import com.hugoof06.pokedex.data.JsonPokemonRepository;
//import com.hugoof06.pokedex.data.JsonPokemonRepository;
import com.hugoof06.pokedex.data.PokeApiPokemonRepository;
import com.hugoof06.pokedex.data.PokemonRepository;
import com.hugoof06.pokedex.model.Generation;
import com.hugoof06.pokedex.model.Pokemon;
import com.hugoof06.pokedex.model.Type;
import com.hugoof06.pokedex.service.PokedexService;
import com.hugoof06.pokedex.favorites.*;

public class Main {

    public static void main(String[] args) {
        String source = "api"; // default
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--source") && i + 1 < args.length) {
                source = args[i + 1].toLowerCase();
            }
        }

        PokemonRepository sourceRepo;
        if (source.equals("json")) {
            sourceRepo = new JsonPokemonRepository();
            System.out.println("Source: JSON (local)");
        } else {
            sourceRepo = new PokeApiPokemonRepository();
            System.out.println("Source: PokeAPI");
        }

        var cache = new com.hugoof06.pokedex.cache.FilePokemonCache();
        var repo = new com.hugoof06.pokedex.data.CachedPokemonRepository(sourceRepo, cache);
        var service = new PokedexService(repo);

        var favRepo = new FavoritesRepository();
        var favService = new FavoritesService(favRepo);


        Generation currentGen = Generation.GEN_1;

        System.out.println("Pokedex CLI - type 'help'");

        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;

                if (line.equalsIgnoreCase("exit")) {
                    break;
                }

                if (line.equalsIgnoreCase("help")) {
                    System.out.println("Commands:");
                    System.out.println("  help");
                    System.out.println("  gen <n>        (example: gen 1)");
                    System.out.println("  list <n>");
                    System.out.println("  show <id|name> (example: show 25 / show pikachu)");
                    System.out.println("  type <TYPE> [page]  (example: type FIRE 2)");
                    System.out.println("  search <text> [page]  (example: search char 2)");
                    System.out.println("  fav list");
                    System.out.println("  fav add <id>   (example: fav add 4)");
                    System.out.println("  fav rm <id>    (example: fav rm 4)");
                    System.out.println("  cache info");
                    System.out.println("  cache clear");
                    System.out.println("  exit");
                    continue;
                }

                if (line.toLowerCase().startsWith("gen ")) {
                    String n = line.substring(4).trim();
                    currentGen = switch (n) {
                        case "1" -> Generation.GEN_1;
                        case "2" -> Generation.GEN_2;
                        case "3" -> Generation.GEN_3;
                        case "4" -> Generation.GEN_4;
                        case "5" -> Generation.GEN_5;
                        case "6" -> Generation.GEN_6;
                        case "7" -> Generation.GEN_7;
                        case "8" -> Generation.GEN_8;
                        case "9" -> Generation.GEN_9;
                        default -> currentGen;
                    };
                    System.out.println("Current generation: " + currentGen);
                    continue;
                }

                if (line.toLowerCase().startsWith("list")) {
                    int page = 1;
                    int pageSize = 20;

                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            page = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            System.out.println("Usage: list [page]");
                            continue;
                        }
                    }

                    var results = service.list(currentGen, page, pageSize);

                    if (results.isEmpty()) {
                        System.out.println("No results (page " + page + ")");
                    } else {
                        System.out.println("Page " + page + " (Gen " + currentGen + ")");
                        results.forEach(System.out::println);
                    }
                    continue;
                }


                if (line.toLowerCase().startsWith("show ")) {
                    String arg = line.substring(5).trim();

                    service.show(arg).ifPresentOrElse(
                        p -> {
                            System.out.println("-----");
                            System.out.println("#" + String.format("%03d", p.getId()) + " " + p.getName());
                            System.out.println("Gen: " + p.getGeneration());
                            System.out.println("Types: " + p.getTypes());
                            System.out.println("Stats: " + p.getStats());
                            System.out.println("-----");
                        },
                        () -> System.out.println("Not found: " + arg)
                    );
                    continue;
                }

                if (line.toLowerCase().startsWith("type ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length < 2) {
                        System.out.println("Usage: type <TYPE> [page]");
                        continue;
                    }

                    String typeArg = parts[1];
                    int page = 1;
                    int pageSize = 20;

                    if (parts.length >= 3) {
                        try {
                            page = Integer.parseInt(parts[2]);
                        } catch (NumberFormatException e) {
                            System.out.println("Usage: type <TYPE> [page]");
                            continue;
                        }
                    }

                    try {
                        Type type = Type.valueOf(typeArg.toUpperCase());
                        var results = service.byType(currentGen, type, page, pageSize);

                        if (results.isEmpty()) {
                            System.out.println("No results (type " + type + ", page " + page + ")");
                        } else {
                            System.out.println("Type " + type + " - Gen " + currentGen + " - Page " + page);
                            results.forEach(System.out::println);
                        }
                    } catch (IllegalArgumentException e) {
                        System.out.println("Unknown type: " + typeArg);
                    }
                    continue;
                }


                if (line.toLowerCase().startsWith("search ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length < 2) {
                        System.out.println("Usage: search <text> [page]");
                        continue;
                    }

                    String text = parts[1];
                    int page = 1;
                    int pageSize = 20;

                    if (parts.length >= 3) {
                        try {
                            page = Integer.parseInt(parts[2]);
                        } catch (NumberFormatException e) {
                            System.out.println("Usage: search <text> [page]");
                            continue;
                        }
                    }

                    var results = service.searchNames(currentGen, text, page, pageSize);

                    if (results.isEmpty()) {
                        System.out.println("No results (\"" + text + "\", Gen " + currentGen + ", page " + page + ")");
                    } else {
                        System.out.println("Search \"" + text + "\" - Gen " + currentGen + " - Page " + page);
                        results.forEach(name -> System.out.println("- " + name));
                        System.out.println("Tip: use 'show <name>' to see full details.");
                    }
                    continue;
                }


                if (line.toLowerCase().equals("fav list")) {
                    var favIds = favService.listIds();

                    // Resolvemos IDs -> Optional<Pokemon>
                    var resolved = favIds.stream()
                            .map(id -> service.show(String.valueOf(id)) // Optional<Pokemon>
                                    .map(p -> new FavResolved(id, p))
                                    .orElseGet(() -> new FavResolved(id, null)))
                            .collect(Collectors.toList());

                    // Separar cargados / no cargados
                    var loaded = resolved.stream()
                            .filter(r -> r.pokemon != null)
                            .map(r -> r.pokemon)
                            .sorted(Comparator.comparing(Pokemon::getGeneration)
                                    .thenComparingInt(Pokemon::getId))
                            .toList();

                    var notLoaded = resolved.stream()
                            .filter(r -> r.pokemon == null)
                            .map(r -> r.id)
                            .sorted()
                            .toList();

                    System.out.println("Favorites (" + favIds.size() + "/" + favService.capacity() + ")");
                    System.out.println("File: " + favService.fileLocation());

                    if (favIds.isEmpty()) {
                        System.out.println("(empty)");
                        continue;
                    }

                    if (!loaded.isEmpty()) {
                        System.out.println("\n--- Loaded ---");
                        for (Pokemon p : loaded) {
                            System.out.println("-----");
                            System.out.println("#" + String.format("%03d", p.getId()) + " " + p.getName());
                            System.out.println("Gen: " + p.getGeneration());
                            System.out.println("Types: " + p.getTypes());
                            System.out.println("Stats: " + p.getStats());
                        }
                    }

                    if (!notLoaded.isEmpty()) {
                        System.out.println("\n--- Not loaded yet ---");
                        for (int id : notLoaded) {
                            System.out.println("#" + String.format("%03d", id) + " (NOT LOADED)");
                        }
                    }

                    System.out.println("-----");
                    continue;
                }

                if (line.toLowerCase().startsWith("fav add ")) {
                    String arg = line.substring(8).trim();
                    try {
                        int id = Integer.parseInt(arg);

                        boolean added = favService.add(id);
                        System.out.println(
                                added
                                        ? "Added to favorites: " + id
                                        : "Already in favorites: " + id
                        );

                    } catch (NumberFormatException e) {
                        System.out.println("Usage: fav add <id>");
                    } catch (IllegalStateException e) {
                        System.out.println(e.getMessage());
                    }
                    continue;
                }

                if (line.toLowerCase().startsWith("fav rm ")) {
                    String arg = line.substring(7).trim();
                    try {
                        int id = Integer.parseInt(arg);
                        boolean removed = favService.remove(id);
                        System.out.println(removed ? "Removed from favorites: " + id : "Not in favorites: " + id);
                    } catch (NumberFormatException e) {
                        System.out.println("Usage: fav rm <id>");
                    }
                    continue;
                }

                if (line.toLowerCase().equals("cache info")) {
                    System.out.println("Cache directory: " + cache.location());
                    System.out.println("Cached pokemons: " + cache.countCachedPokemons());
                    continue;
                }

                if (line.toLowerCase().equals("cache clear")) {
                    int removed = cache.clear();
                    System.out.println("Cache cleared (" + removed + " files removed)");
                    continue;
                }


                System.out.println("Unknown command. Type 'help'.");
            }
        }
    }

    private static class FavResolved {
        final int id;
        final Pokemon pokemon; // null si no está cargado

        FavResolved(int id, Pokemon pokemon) {
            this.id = id;
            this.pokemon = pokemon;
        }
    }
}
