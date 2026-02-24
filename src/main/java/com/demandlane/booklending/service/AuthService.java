package com.demandlane.booklending.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demandlane.booklending.dto.AuthDto;
import com.demandlane.booklending.dto.UserDto;
import com.demandlane.booklending.entity.Role;
import com.demandlane.booklending.entity.User;
import com.demandlane.booklending.exception.ResourceNotFoundException;
import com.demandlane.booklending.mapper.UserMapper;
import com.demandlane.booklending.repository.UserRepository;
import com.demandlane.booklending.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public UserDto.Response register(UserDto.Request request) {
        if (userRepository.findActiveByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("User with email " + request.getEmail() + " already exists");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.MEMBER);

        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findActiveByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalStateException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());

        return AuthDto.LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
