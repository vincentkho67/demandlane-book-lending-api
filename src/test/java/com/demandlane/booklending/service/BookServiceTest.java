package com.demandlane.booklending.service;

import com.demandlane.booklending.dto.BookDto;
import com.demandlane.booklending.entity.Book;
import com.demandlane.booklending.exception.ResourceNotFoundException;
import com.demandlane.booklending.mapper.BookMapper;
import com.demandlane.booklending.repository.BookRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookService bookService;

    private Book book;
    private BookDto.Request bookRequest;
    private BookDto.Response bookResponse;

    @BeforeEach
    void setUp() {
        book = Book.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .totalCopies(10L)
                .availableCopies(8L)
                .build();

        bookRequest = BookDto.Request.builder()
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .totalCopies(10L)
                .availableCopies(8L)
                .build();

        bookResponse = BookDto.Response.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .totalCopies(10L)
                .availableCopies(8L)
                .build();
    }

    @Test
    void shouldFindAllBooks() {
        // Given
        Page<Book> bookPage = new PageImpl<>(List.of(book));
        Pageable pageable = PageRequest.of(0, 10);
        BookDto.Filter filter = new BookDto.Filter();

        when(bookRepository.findAll(ArgumentMatchers.<Specification<Book>>any(), any(Pageable.class))).thenReturn(bookPage);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(bookResponse);

        // When
        Page<BookDto.Response> result = bookService.findAll(filter, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Clean Code");
        assertThat(result.getContent().get(0).getAuthor()).isEqualTo("Robert C. Martin");

        verify(bookRepository).findAll(ArgumentMatchers.<Specification<Book>>any(), eq(pageable));
        verify(bookMapper).toResponse(book);
    }

    @Test
    void shouldFindAllBooksWithFilter() {
        // Given
        Page<Book> bookPage = new PageImpl<>(List.of(book));
        Pageable pageable = PageRequest.of(0, 10);
        BookDto.Filter filter = new BookDto.Filter("Clean Code", "Robert C. Martin", "9780132350884");

        when(bookRepository.findAll(ArgumentMatchers.<Specification<Book>>any(), any(Pageable.class))).thenReturn(bookPage);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(bookResponse);

        // When
        Page<BookDto.Response> result = bookService.findAll(filter, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(bookRepository).findAll(ArgumentMatchers.<Specification<Book>>any(), eq(pageable));
        verify(bookMapper).toResponse(book);
    }

    @Test
    void shouldFindBookById() {
        // Given
        when(bookRepository.findActiveById(anyLong())).thenReturn(Optional.of(book));
        when(bookMapper.toResponse(any(Book.class))).thenReturn(bookResponse);

        // When
        BookDto.Response result = bookService.findById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(result.getIsbn()).isEqualTo("9780132350884");

        verify(bookRepository).findActiveById(1L);
        verify(bookMapper).toResponse(book);
    }

    @Test
    void shouldThrowException_whenBookNotFound() {
        // Given
        when(bookRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");

        verify(bookRepository).findActiveById(999L);
        verify(bookMapper, never()).toResponse(any());
    }

    @Test
    void shouldSaveBook() {
        // Given
        when(bookMapper.toEntity(any(BookDto.Request.class))).thenReturn(book);
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(bookResponse);

        // When
        BookDto.Response result = bookService.save(bookRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(result.getIsbn()).isEqualTo("9780132350884");

        verify(bookMapper).toEntity(bookRequest);
        verify(bookRepository).save(book);
        verify(bookMapper).toResponse(book);
    }

    @Test
    void shouldUpdateBook() {
        // Given
        BookDto.Request updateRequest = BookDto.Request.builder()
                .title("Updated Title")
                .author("Updated Author")
                .isbn("9780132350884")
                .totalCopies(15L)
                .availableCopies(12L)
                .build();

        Book updatedBook = Book.builder()
                .id(1L)
                .title("Updated Title")
                .author("Updated Author")
                .isbn("9780132350884")
                .totalCopies(15L)
                .availableCopies(12L)
                .build();

        BookDto.Response updatedResponse = BookDto.Response.builder()
                .id(1L)
                .title("Updated Title")
                .author("Updated Author")
                .isbn("9780132350884")
                .totalCopies(15L)
                .availableCopies(12L)
                .build();

        when(bookRepository.findActiveById(anyLong())).thenReturn(Optional.of(book));
        doNothing().when(bookMapper).updateEntity(any(Book.class), any(BookDto.Request.class));
        when(bookRepository.save(any(Book.class))).thenReturn(updatedBook);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(updatedResponse);

        // When
        BookDto.Response result = bookService.update(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Updated Title");
        assertThat(result.getAuthor()).isEqualTo("Updated Author");
        assertThat(result.getTotalCopies()).isEqualTo(15L);
        assertThat(result.getAvailableCopies()).isEqualTo(12L);

        verify(bookRepository).findActiveById(1L);
        verify(bookMapper).updateEntity(book, updateRequest);
        verify(bookRepository).save(book);
        verify(bookMapper).toResponse(updatedBook);
    }

    @Test
    void shouldThrowException_whenUpdatingNonExistentBook() {
        // Given
        when(bookRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookService.update(999L, bookRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");

        verify(bookRepository).findActiveById(999L);
        verify(bookMapper, never()).updateEntity(any(), any());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void shouldSoftDeleteBook() {
        // Given
        when(bookRepository.findActiveById(anyLong())).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);

        // When
        bookService.delete(1L);

        // Then
        verify(bookRepository).findActiveById(1L);
        verify(bookRepository).save(book);
        assertThat(book.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldThrowException_whenDeletingNonExistentBook() {
        // Given
        when(bookRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");

        verify(bookRepository).findActiveById(999L);
        verify(bookRepository, never()).save(any());
    }

    @Test
    void shouldUpdateAvailableCopiesWhenBookIsBorrowed() {
        // Given
        BookDto.Request updateRequest = BookDto.Request.builder()
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .totalCopies(10L)
                .availableCopies(7L) // One copy borrowed
                .build();

        Book updatedBook = Book.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .totalCopies(10L)
                .availableCopies(7L)
                .build();

        BookDto.Response updatedResponse = BookDto.Response.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .totalCopies(10L)
                .availableCopies(7L)
                .build();

        when(bookRepository.findActiveById(anyLong())).thenReturn(Optional.of(book));
        doNothing().when(bookMapper).updateEntity(any(Book.class), any(BookDto.Request.class));
        when(bookRepository.save(any(Book.class))).thenReturn(updatedBook);
        when(bookMapper.toResponse(any(Book.class))).thenReturn(updatedResponse);

        // When
        BookDto.Response result = bookService.update(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAvailableCopies()).isEqualTo(7L);
        assertThat(result.getTotalCopies()).isEqualTo(10L);

        verify(bookRepository).findActiveById(1L);
        verify(bookMapper).updateEntity(book, updateRequest);
        verify(bookRepository).save(book);
    }
}
