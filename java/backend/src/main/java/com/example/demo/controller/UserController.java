package com.example.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

import com.example.demo.dto.UserDto;
import com.example.demo.dto.LoginDto;
import com.example.demo.db.UserService;
import com.example.demo.config.JwtService;

@Slf4j
@RestController
public class UserController {
        
    private final ObjectMapper objectMapper;
    private final UserService dbService;
    private final JwtService jwt;
    private final PasswordEncoder encoder;
    
    public UserController (ObjectMapper mapper, UserService db, PasswordEncoder encoder, JwtService jwt) {        
        this.objectMapper = mapper;
        this.dbService = db;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /*!
     * ====================================================================================================
     * ====================================================================================================
     * =====================================          ROUTES          =====================================
     * ====================================================================================================
     * ====================================================================================================
     */

    @PostMapping("/api/login")
    public LoginDto.Response login(@RequestBody LoginDto.Request request) {
        var login = request.login();
        var password = request.password();

        // Validate user
        var user = this.dbService.findByLogin (login)
            .orElseThrow(() -> {
                    log.info ("Not found");
                    return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"); });
                
        
        // Compare raw password with stored hash
        if (!this.encoder.matches(password, user.password ())) {
            log.info ("Wrong pass {}/{}", password, user.password ());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        try {
            var jsonUser = this.objectMapper.writeValueAsString (new UserDto.User (user.id (), user.login (), ""));        
            // Generate JWT
            var token = this.jwt.generateToken (jsonUser);
            return new LoginDto.Response(token, login);
        } catch (JsonProcessingException err) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Invalid credentials");
        }
    }

    @PostMapping("/api/register")
    public LoginDto.RegisterResponse register(@RequestBody LoginDto.Request request) {
        var login = request.login();
        var password = request.password();

        // Validate user
        if (this.dbService.exists (login)) {            
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login already exists;");
        }

        var user = new UserDto.User (0, login, this.encoder.encode (password));
        if (this.dbService.insert (user)) {
            return new LoginDto.RegisterResponse ();
        }

        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to insert in DB");        
    }
    
    
}
