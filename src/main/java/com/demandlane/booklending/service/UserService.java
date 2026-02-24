package com.demandlane.booklending.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.demandlane.booklending.dto.UserDto;
import com.demandlane.booklending.entity.User;
import com.demandlane.booklending.exception.ResourceNotFoundException;
import com.demandlane.booklending.mapper.UserMapper;
import com.demandlane.booklending.repository.UserRepository;
import com.demandlane.booklending.specification.SpecificationBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public Page<UserDto.Response> findAll(UserDto.Filter filter, Pageable pageable) {
        Specification<User> spec = SpecificationBuilder.fromFilter(filter, User.class);
        Page<User> users = userRepository.findAll(spec, pageable);
        return users.map(userMapper::toResponse);
    }

    public UserDto.Response findById(Long id) {
        User user = userRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toResponse(user);
    }

    public UserDto.Response save(UserDto.Request request) {
        User user = userMapper.toEntity(request);
        User saved = userRepository.save(user);
        return userMapper.toResponse(saved);
    }

    public UserDto.Response update(Long id, UserDto.Request request) {
        User existing = userRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        userMapper.updateEntity(existing, request);
        User updated = userRepository.save(existing);
        return userMapper.toResponse(updated);
    }

    public void delete(Long id) {
        User user = userRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.softDelete();
        userRepository.save(user);
    }

    public UserDto.Response findByEmail(String email) {
        User user = userRepository.findActiveByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toResponse(user);
    }
}
