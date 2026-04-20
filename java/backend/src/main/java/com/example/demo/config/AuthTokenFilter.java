package com.example.demo.config;

import com.example.demo.dto.UserDto;
import com.example.demo.config.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.*;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.authentication.TestingAuthenticationToken;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    
    private JwtService jwtUtils;    
    private ObjectMapper mapper;

    public AuthTokenFilter (JwtService service, ObjectMapper mapper) {
        this.jwtUtils = service;
        this.mapper = mapper;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)                                    
        throws ServletException, IOException
    {
        try {
            String jwt = this.parseJwt(request);            
            if (jwt != null) {
                var content = this.jwtUtils.validate (jwt);
                if (content.isPresent ()) {
                    String value = content.get ();
                    var user = this.mapper.readValue (value, UserDto.User.class);
                    var authentication =
                        new TestingAuthenticationToken(user.login (), user.password (), "ROLE_USER");
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));                    
                                                            
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            System.out.println("Cannot set user authentication: " + e);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        
        return null;
    }
    
}
