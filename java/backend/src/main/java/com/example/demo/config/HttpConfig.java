package com.example.demo.config;

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


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.demo.config.JwtService;

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
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter(this.jwtService (), this.objectMapper ());
    }

    @Bean
    public AuthEntryPointJwt authentrypointjwt () {
        return new AuthEntryPointJwt ();
    }

    @Bean
    public JwtService jwtService() {
        return new JwtService ();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {        
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable()) // Disable CORS (or configure if needed)
            .exceptionHandling(exceptionHandling ->
                               exceptionHandling.authenticationEntryPoint(this.authentrypointjwt ())
                               )
            .sessionManagement(sessionManagement ->
                               sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                               )
            .authorizeHttpRequests(auth -> auth
                                   .requestMatchers("/", "/index.html", "/static/**", "/assets/**", "/api/").permitAll()
                                   .requestMatchers("/api/login", "/api/register").permitAll()                                                                      
                                   .anyRequest().authenticated()
                                   )
            .formLogin(form -> form.disable());

        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);
       

        return http.build();
    }
}
