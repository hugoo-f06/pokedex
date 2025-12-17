package com.hugoof06.pokedex.cli;

import java.util.Scanner;

import com.hugoof06.pokedex.data.JsonPokemonRepository;
import com.hugoof06.pokedex.model.Generation;
import com.hugoof06.pokedex.service.PokedexService;

public class Main {

    public static void main(String[] args) {
        var repo = new JsonPokemonRepository();
        var service = new PokedexService(repo);

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
                    System.out.println("  type <TYPE>    (example: type FIRE)");
                    System.out.println("  search <text>  (example: search char)");
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
                    String arg = line.substring(5).trim();

                    try {
                        var type = com.hugoof06.pokedex.model.Type.valueOf(arg.toUpperCase());
                        service.byType(currentGen, type).forEach(System.out::println);
                    } catch (IllegalArgumentException e) {
                        System.out.println("Unknown type: " + arg);
                    }
                    continue;
                }

                if (line.toLowerCase().startsWith("search ")) {
                    String arg = line.substring(7).trim();

                    if (arg.isEmpty()) {
                        System.out.println("Usage: search <text>");
                    } else {
                        service.search(currentGen, arg).forEach(System.out::println);
                    }
                    continue;
                }

                System.out.println("Unknown command. Type 'help'.");
            }
        }
    }
}
