package com.demandlane.booklending.controller;

import java.security.Principal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.demandlane.booklending.controller.api.UserApi;
import com.demandlane.booklending.dto.PageResponse;
import com.demandlane.booklending.dto.UserDto;
import com.demandlane.booklending.service.UserService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @Override
    public ResponseEntity<UserDto.Response> me(Principal principal) {
        return ResponseEntity.ok(userService.findByEmail(principal.getName()));
    }

    @Override
    public ResponseEntity<PageResponse<UserDto.Response>> findAll(
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            UserDto.Filter filter) {
        return ResponseEntity.ok(PageResponse.of(userService.findAll(filter, pageable)));
    }

    @Override
    public ResponseEntity<UserDto.Response> findById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @Override
    public ResponseEntity<UserDto.Response> create(@RequestBody UserDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.save(request));
    }

    @Override
    public ResponseEntity<UserDto.Response> update(@PathVariable Long id, @RequestBody UserDto.Request request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
