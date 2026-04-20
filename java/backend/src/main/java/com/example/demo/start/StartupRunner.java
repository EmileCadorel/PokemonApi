package com.example.demo.start;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.example.demo.dto.PokemonDto;


@Slf4j
@Component
public class StartupRunner implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final RestClient rest;

    public StartupRunner (RestClient rest, JdbcTemplate jdbc) {
        this.rest = rest;
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        this.createPokemonTable ();
        this.createUserTable ();
        this.createLikesTable ();
        this.insertPokemons ();        
    }

    private void createUserTable () {
        var rq = "CREATE TABLE IF NOT EXISTS users (id INT NOT NULL AUTO_INCREMENT, login VARCHAR (255) NOT NULL, password VARCHAR (255) NOT NULL, PRIMARY KEY (id))";
        jdbc.execute (rq);
    }

    private void createPokemonTable () {
        var drop = "DROP TABLE pokemon_names";
        jdbc.execute (drop);
        
        var rq = "CREATE TABLE IF NOT EXISTS pokemon_names (name VARCHAR (255) NOT NULL, PRIMARY KEY (name))";
        jdbc.execute (rq);        
    }

    private void createLikesTable () {
        var rq = "CREATE TABLE IF NOT EXISTS likes (id INT NOT NULL AUTO_INCREMENT, user_id INT NOT NULL, pokemon_id INT NOT NULL, PRIMARY KEY (id), FOREIGN KEY (user_id) REFERENCES users(id))";
        jdbc.execute (rq);        
    }

    private void insertPokemons () {
        var response = rest.get()
            .uri("https://pokeapi.co/api/v2/pokemon?limit=2000")
            .retrieve()
            .body(PokemonDto.PokemonList.class);
        
        String sql = "INSERT INTO pokemon_names (name) VALUES (?)";
        jdbc.batchUpdate(sql, response.results (), response.results ().size(), (ps, pok) -> {
                ps.setString(1, pok.name ());
            });
        
    }
    
}
