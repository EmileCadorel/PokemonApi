package com.example.demo.start;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.io.InputStream;
import java.io.*;

import java.nio.charset.StandardCharsets; 
import org.yaml.snakeyaml.Yaml;

import com.example.demo.dto.PokemonDto;


@Slf4j
@Component
public class StartupRunner implements CommandLineRunner {

    private final JdbcTemplate jdbc;
    private final RestClient rest;
    private final PasswordEncoder encoder;

    public StartupRunner (RestClient rest, JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.rest = rest;
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        this.createPokemonTable ();
        this.createUserTable ();
        this.createLikesTable ();
        this.insertPokemons ();        
    }

    private void createUserTable () {
        try {
            var drop1 = "DROP TABLE likes";
            jdbc.execute (drop1);
            
            var drop = "DROP TABLE users";
            jdbc.execute (drop);
        } catch (Exception e) {
            System.out.println (e);
        }
        
        var rq = "CREATE TABLE IF NOT EXISTS users (id INT NOT NULL AUTO_INCREMENT, login VARCHAR (255) NOT NULL, password VARCHAR (255) NOT NULL, PRIMARY KEY (id))";
        jdbc.execute (rq);


        var inputStream = getClass ().getClassLoader ().getResourceAsStream ("/users.yml");
        if (inputStream != null) {
            var yaml = new Yaml();
            Map<String, String> data = yaml.load(inputStream);
            List<Map.Entry<String, String>> list = data.entrySet()
                .stream()
                .collect(Collectors.toList());
            
            String sql = "INSERT INTO users (login, password) VALUES (?, ?)";
            jdbc.batchUpdate(sql, list, list.size(), (ps, user) -> {
                    ps.setString(1, user.getKey ());
                    ps.setString(2, this.encoder.encode (user.getValue ()));
                });
        }
    }

    private void createPokemonTable () {
        try {        
            var drop = "DROP TABLE pokemon_names";
            jdbc.execute (drop);
        } catch (Exception e) {            
        }
        
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
