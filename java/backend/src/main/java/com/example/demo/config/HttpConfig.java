package com.example.demo.utils;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.client.RestClient;

@Configuration
@EnableWebSecurity
public class HttpConfig {

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                                   .requestMatchers("/", "/index.html", "/static/**", "/assets/**").permitAll()
                                   .requestMatchers("/api/login").permitAll()
                                   .anyRequest().authenticated()
                                   )
            .formLogin(form -> form.disable());

        return http.build();
    }
}
