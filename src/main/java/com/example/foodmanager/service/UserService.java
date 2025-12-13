package com.example.foodmanager.service;

import com.example.foodmanager.model.User;
import com.example.foodmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. 入力されたメールアドレスでユーザーを探す
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return org.springframework.security.core.userdetails.User
            // 2. ▼▼▼ 修正: ここを getEmail() から getUsername() に変更 ▼▼▼
            // これにより、画面表示や auth.getName() が「ユーザーネーム」になります
            .withUsername(user.getEmail()) 
            .password(user.getPassword())
            .roles(user.getRole().replace("ROLE_", ""))
            .build();
    }

    @Transactional
    public User registerUser(String username, String email, String password) {

        
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }
}