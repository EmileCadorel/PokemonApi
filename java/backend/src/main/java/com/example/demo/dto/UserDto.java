package com.example.demo.dto;

public class UserDto {

    public record User (Integer id, String login, String password) {}
    
}
