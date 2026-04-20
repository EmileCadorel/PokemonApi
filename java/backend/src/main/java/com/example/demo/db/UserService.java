package com.example.demo.db;

import org.springframework.stereotype.Service;
import java.util.List;
import com.example.demo.dto.UserDto;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public List<UserDto.User> findAll() {
        return repo.findAll();
    }

    public Optional<UserDto.User> findByLogin (String login) {
        return repo.findByLogin (login);
    }

    public Boolean exists (String login) {
        return repo.findByLogin (login).isPresent ();        
    }

    public Boolean insert (UserDto.User user) {
        return repo.insert (user);        
    }

    public Boolean like (UserDto.User user, Integer id) {
        return repo.like (user, id);
    }

    public Boolean unlike (UserDto.User user, Integer id) {
        return repo.unlike (user, id);
    }

    public Boolean isLiked (UserDto.User user, Integer id) {
        return repo.isLiked (user, id);
    }
    
}
