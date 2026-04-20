package com.example.demo.dto;

public class LoginDto {
    public record Request(String login, String password) {}
    public record Response(String jwt, String username) {}
    public record RegisterResponse () {}
}
