package com.demandlane.booklending.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.demandlane.booklending.controller.api.BookApi;
import com.demandlane.booklending.dto.BookDto;
import com.demandlane.booklending.dto.PageResponse;
import com.demandlane.booklending.service.BookService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class BookController implements BookApi {

    private final BookService bookService;

    @Override
    public ResponseEntity<PageResponse<BookDto.Response>> findAll(
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            BookDto.Filter filter) {
        return ResponseEntity.ok(PageResponse.of(bookService.findAll(filter, pageable)));
    }

    @Override
    public ResponseEntity<BookDto.Response> findById(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.findById(id));
    }

    @Override
    public ResponseEntity<BookDto.Response> create(@RequestBody BookDto.Request request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.save(request));
    }

    @Override
    public ResponseEntity<BookDto.Response> update(@PathVariable Long id, @RequestBody BookDto.Request request) {
        return ResponseEntity.ok(bookService.update(id, request));
    }

    @Override
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
