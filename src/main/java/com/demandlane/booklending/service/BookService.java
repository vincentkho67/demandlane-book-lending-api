package com.demandlane.booklending.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.demandlane.booklending.dto.BookDto;
import com.demandlane.booklending.entity.Book;
import com.demandlane.booklending.exception.ResourceNotFoundException;
import com.demandlane.booklending.mapper.BookMapper;
import com.demandlane.booklending.repository.BookRepository;
import com.demandlane.booklending.specification.SpecificationBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    public Page<BookDto.Response> findAll(BookDto.Filter filter, Pageable pageable) {
        Specification<Book> spec = SpecificationBuilder.fromFilter(filter, Book.class);
        Page<Book> books = bookRepository.findAll(spec, pageable);
        return books.map(bookMapper::toResponse);
    }

    public BookDto.Response findById(Long id) {
        Book book = bookRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
        return bookMapper.toResponse(book);
    }

    public BookDto.Response save(BookDto.Request request) {
        Book book = bookMapper.toEntity(request);
        Book saved = bookRepository.save(book);
        return bookMapper.toResponse(saved);
    }

    public BookDto.Response update(Long id, BookDto.Request request) {
        Book existing = bookRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
        bookMapper.updateEntity(existing, request);
        Book updated = bookRepository.save(existing);
        return bookMapper.toResponse(updated);
    }

    public void delete(Long id) {
        Book book = bookRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
        book.softDelete();
        bookRepository.save(book);
    }
}
