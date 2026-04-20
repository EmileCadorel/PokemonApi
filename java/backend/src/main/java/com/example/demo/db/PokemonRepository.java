package com.example.demo.db;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.example.demo.dto.PokemonDto;


@Repository
public class PokemonRepository {

    private final JdbcTemplate jdbc;

    public PokemonRepository (JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> suggest (String start) {
        String sql = "SELECT name FROM pokemon_names WHERE name LIKE ? ORDER BY name LIMIT 5";                    
        return jdbc.query (sql,
                           ((rs, rowNum) -> rs.getString("name")),                            
                           start + "%");                           
    }

    public List<String> find (String name) {
        String sql = "SELECT name FROM pokemon_names WHERE name = ?";                    
        return jdbc.query (sql,
                           ((rs, rowNum) -> rs.getString("name")),                            
                           name);        
    }
    
}
