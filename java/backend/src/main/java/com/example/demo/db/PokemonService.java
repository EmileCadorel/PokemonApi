package com.example.demo.db;

import org.springframework.stereotype.Service;
import java.util.List;
import com.example.demo.dto.PokemonDto;

@Service
public class PokemonService {

    private final PokemonRepository repo;

    public PokemonService(PokemonRepository repo) {
        this.repo = repo;
    }

    public List<String> suggest (String start) {
        return repo.suggest (start);
    }

    public Boolean exists (String name) {
        var pokemons = repo.find (name);
        return pokemons.size () != 0;
    }
}
