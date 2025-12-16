package com.hugoof06.pokedex.cli;

import com.hugoof06.pokedex.data.*;

public class Main {
    public static void main(String[] args) {
        JsonPokemonRepository repo = new JsonPokemonRepository();
        repo.findAll().forEach(System.out::println);
    }
}
