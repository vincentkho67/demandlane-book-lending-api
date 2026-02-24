package com.demandlane.booklending.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.demandlane.booklending.controller.api.AuthApi;
import com.demandlane.booklending.dto.AuthDto;
import com.demandlane.booklending.dto.UserDto;
import com.demandlane.booklending.service.AuthService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public ResponseEntity<UserDto.Response> register(@RequestBody UserDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Override
    public ResponseEntity<AuthDto.LoginResponse> login(@RequestBody AuthDto.LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
