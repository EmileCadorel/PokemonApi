package com.example.demo.config;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final String secret = "secret";

    public String generateToken (String body) {
        return Jwts.builder()
            .setSubject(body)            
            .setIssuedAt(new Date())
            .setExpiration(Date.from(Instant.now().plus(Duration.ofHours(12))))
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact();
    }

    public String validate (String token) {
        try {
            return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
        }
    }
}
