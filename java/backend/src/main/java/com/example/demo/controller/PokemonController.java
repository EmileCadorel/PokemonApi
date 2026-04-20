package com.example.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
    
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.example.demo.dto.PokemonDto;
import com.example.demo.db.PokemonService;

@Slf4j
@RestController
public class PokemonController {
    
    private final RestClient rest;
    private final ObjectMapper objectMapper;
    private final PokemonService dbService;
    
    public PokemonController (RestClient rest, ObjectMapper mapper, PokemonService service) {
        this.rest = rest;
        this.objectMapper = mapper;
        this.dbService = service;
    }

    /*!
     * ====================================================================================================
     * ====================================================================================================
     * =====================================          ROUTES          =====================================
     * ====================================================================================================
     * ====================================================================================================
     */
    
    @GetMapping("/api/pokemon-info/{name}")
    public PokemonDto.Pokemon pokemonInfo (@PathVariable String name) {
        // It's faster to lookup the pokemon in the DB first
        if (!this.dbService.exists (name)) {            
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pokemon not found");
        }
        
        // And if it exists lookup in the pokeapi
        var url = String.format ("https://pokeapi.co/api/v2/pokemon/%s", name);

        try {
            Map<String, Object> map = this.rest.get()
                .uri(url)
                .retrieve()           
                .body(Map.class);

            return this.extract (map);
        } catch (HttpClientErrorException.NotFound err) {
            // Shouldn't happen, but maybe there's some name in DB that aren't pokemon anymore..
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pokemon not found");            
        }
    }

    @GetMapping("/api/pokemon-suggest/{start}")
    public PokemonDto.SuggestList suggestList (@PathVariable String start) {        
        var lst = this.dbService.suggest (start);

        return new PokemonDto.SuggestList (lst);
    }

    /*!
     * ====================================================================================================
     * ====================================================================================================
     * ====================================          PRIVATES          ====================================
     * ====================================================================================================
     * ====================================================================================================
     */

    PokemonDto.Pokemon extract (Map<String, Object> map) {        
        Integer id = this.objectMapper.convertValue(map.get("id"), Integer.class);
        String name = this.objectMapper.convertValue(map.get("name"), String.class);
        Integer weight = this.objectMapper.convertValue(map.get("weight"), Integer.class);
                
        var sprites =  this.objectMapper.convertValue(map.get ("sprites"), Map.class);        
        String sprite = this.objectMapper.convertValue(sprites.get("front_default"), String.class);

        var types = (List<Map <String, Object> >) (map.get ("types"));        
        var list = new ArrayList<String> ();
        for (var type : types) {
            var tmp = this.objectMapper.convertValue(type.get("type"), Map.class);            
            list.add (this.objectMapper.convertValue(tmp.get("name"), String.class));
        }
                
        return new PokemonDto.Pokemon (id, name, weight, sprite, list);
    }

    
}
