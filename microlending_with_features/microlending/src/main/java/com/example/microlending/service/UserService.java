package com.example.microlending.service;


import com.example.microlending.entity.User;
import com.example.microlending.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository repo;
    public UserService(UserRepository repo){ this.repo = repo; }

    public User register(User u){
        u.setVerified(false);
        return repo.save(u);
    }
    public Optional<User> findByEmail(String email){ return repo.findByEmail(email); }
    public Optional<User> findById(Long id){ return repo.findById(id); }
    public User save(User u){ return repo.save(u); }
}

