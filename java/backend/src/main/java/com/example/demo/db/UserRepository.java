package com.example.demo.db;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.example.demo.dto.UserDto;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository (JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<UserDto.User> findAll() {
        return jdbc.query(
            "SELECT id, login, password FROM users",
            (rs, rowNum) -> new UserDto.User(
                rs.getInt("id"),
                rs.getString("login"),
                rs.getString("password")
            )
        );
    }

    public Optional<UserDto.User> findByLogin (String login) {
        var results = jdbc.query (
            "SELECT id, login, password FROM users where login = ?",
            (rs, rowNum) -> new UserDto.User(
                rs.getInt("id"),
                rs.getString("login"),
                rs.getString("password")
            ),
            login
        );
        
        return results.stream().findFirst();
    }

    public Boolean insert (UserDto.User user) {
        var results = jdbc.update("INSERT INTO users (login, password) VALUES (?, ?)",
                                  user.login (),
                                  user.password ());                        
        
        return results == 1;
    }

    public Boolean isLiked (UserDto.User user, Integer pokemonId) {
        var results = jdbc.query (
            "SELECT id FROM likes WHERE user_id=? and pokemon_id=?",
            (rs, rowNum) -> rs.getInt("id"),            
            user.id (),
            pokemonId
        );
        
        return results.stream().findFirst().isPresent ();
    }

    public Boolean like (UserDto.User user, Integer pokemonId) {
        if (!this.isLiked (user, pokemonId)) {
            var results = jdbc.update("INSERT INTO likes (user_id, pokemon_id) VALUES (?, ?)",
                                  user.id (),
                                  pokemonId);

            return results == 1;
        }

        return false;
    }


    public Boolean unlike (UserDto.User user, Integer pokemonId) {
        if (this.isLiked (user, pokemonId)) {
            var results = jdbc.update("DELETE from likes where user_id = ? and pokemon_id = ?",
                                  user.id (),
                                  pokemonId);

            return results == 1;
        }

        return false;
    }
}
