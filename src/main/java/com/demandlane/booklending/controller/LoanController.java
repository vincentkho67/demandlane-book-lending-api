package com.demandlane.booklending.controller;

import java.security.Principal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.demandlane.booklending.controller.api.LoanApi;
import com.demandlane.booklending.dto.LoanDto;
import com.demandlane.booklending.dto.PageResponse;
import com.demandlane.booklending.service.LoanService;
import com.demandlane.booklending.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequiredArgsConstructor
public class LoanController implements LoanApi {

    private final LoanService loanService;
    private final UserService userService;

    @Override
    public ResponseEntity<PageResponse<LoanDto.Response>> findAll(
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            LoanDto.Filter filter) {
        return ResponseEntity.ok(PageResponse.of(loanService.findAll(filter, pageable)));
    }

    @Override
    public ResponseEntity<PageResponse<LoanDto.Response>> findAllOwned(
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            LoanDto.Filter filter,
            Principal principal) {
        filter.setUserId(userService.findByEmail(principal.getName()).getId());
        return ResponseEntity.ok(PageResponse.of(loanService.findAll(filter, pageable)));
    }

    @Override
    public ResponseEntity<LoanDto.Response> findById(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok(loanService.findById(id, auth));
    }

    @Override
    public ResponseEntity<LoanDto.Response> create(@RequestBody LoanDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.save(request));
    }

    @Override
    public ResponseEntity<LoanDto.Response> update(@PathVariable Long id, @RequestBody LoanDto.Request request) {
        return ResponseEntity.ok(loanService.update(id, request));
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        loanService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<LoanDto.Response> borrowBook(@RequestBody LoanDto.BorrowRequest request, Principal principal) {
        Long userId = userService.findByEmail(principal.getName()).getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.borrowBook(userId, request.getBookId()));
    }

    @Override
    public ResponseEntity<LoanDto.Response> returnBook(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.returnBook(id));
    }
}
