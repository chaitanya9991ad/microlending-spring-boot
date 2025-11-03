package com.example.microlending.service;


import com.example.microlending.entity.User;
import com.example.microlending.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository repo;

    public CustomUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = repo.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        // roles stored as comma-separated, e.g. "USER,ADMIN"
        var auths = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + u.getRole()));
        return new org.springframework.security.core.userdetails.User(u.getEmail(), u.getPassword(), auths);
    }
}
