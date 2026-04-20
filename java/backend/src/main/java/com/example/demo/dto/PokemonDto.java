package com.example.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class PokemonDto {
    public record Pokemon(Integer id, String name, Integer weight, String sprite, ArrayList<String> type) {}

    public record PokemonList(List<PokemonEntry> results) {}
    public record PokemonEntry(String name, String url) {}

    public record SuggestList (List <String> names) {}
}
