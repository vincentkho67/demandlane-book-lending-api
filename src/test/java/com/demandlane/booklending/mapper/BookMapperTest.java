package com.demandlane.booklending.mapper;

import com.demandlane.booklending.dto.BookDto;
import com.demandlane.booklending.entity.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BookMapperTest {

    @Autowired
    private BookMapper bookMapper;

    @Test
    void shouldMapEntityToResponse() {
        // Given
        Book book = Book.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .totalCopies(10L)
                .availableCopies(8L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        BookDto.Response response = bookMapper.toResponse(book);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Clean Code");
        assertThat(response.getAuthor()).isEqualTo("Robert C. Martin");
        assertThat(response.getIsbn()).isEqualTo("9780132350884");
        assertThat(response.getTotalCopies()).isEqualTo(10L);
        assertThat(response.getAvailableCopies()).isEqualTo(8L);
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldMapRequestToEntity() {
        // Given
        BookDto.Request request = BookDto.Request.builder()
                .title("The Pragmatic Programmer")
                .author("Andrew Hunt")
                .isbn("9780135957059")
                .totalCopies(5L)
                .availableCopies(5L)
                .build();

        // When
        Book book = bookMapper.toEntity(request);

        // Then
        assertThat(book).isNotNull();
        assertThat(book.getTitle()).isEqualTo("The Pragmatic Programmer");
        assertThat(book.getAuthor()).isEqualTo("Andrew Hunt");
        assertThat(book.getIsbn()).isEqualTo("9780135957059");
        assertThat(book.getTotalCopies()).isEqualTo(5L);
        assertThat(book.getAvailableCopies()).isEqualTo(5L);
        assertThat(book.getId()).isNull(); // Should not map ID
        assertThat(book.getCreatedAt()).isNull(); // Should not map createdAt
        assertThat(book.getUpdatedAt()).isNull(); // Should not map updatedAt
    }

    @Test
    void shouldUpdateEntityFromRequest() {
        // Given
        Book existingBook = Book.builder()
                .id(1L)
                .title("Old Title")
                .author("Old Author")
                .isbn("1111111111")
                .totalCopies(3L)
                .availableCopies(2L)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        BookDto.Request request = BookDto.Request.builder()
                .title("Updated Title")
                .author("Updated Author")
                .isbn("1111111111")
                .totalCopies(10L)
                .availableCopies(7L)
                .build();

        // When
        bookMapper.updateEntity(existingBook, request);

        // Then
        assertThat(existingBook.getId()).isEqualTo(1L); // ID should not change
        assertThat(existingBook.getTitle()).isEqualTo("Updated Title");
        assertThat(existingBook.getAuthor()).isEqualTo("Updated Author");
        assertThat(existingBook.getIsbn()).isEqualTo("1111111111");
        assertThat(existingBook.getTotalCopies()).isEqualTo(10L);
        assertThat(existingBook.getAvailableCopies()).isEqualTo(7L);
    }

    @Test
    void shouldHandleNullEntityGracefully() {
        // When
        BookDto.Response response = bookMapper.toResponse(null);

        // Then
        assertThat(response).isNull();
    }

    @Test
    void shouldHandleNullRequestGracefully() {
        // When
        Book book = bookMapper.toEntity(null);

        // Then
        assertThat(book).isNull();
    }

    @Test
    void shouldMapEntityWithNullOptionalFields() {
        // Given
        Book book = Book.builder()
                .id(1L)
                .title("Book Without Copies")
                .author("Test Author")
                .isbn("0000000000")
                .totalCopies(null)
                .availableCopies(null)
                .build();

        // When
        BookDto.Response response = bookMapper.toResponse(book);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Book Without Copies");
        assertThat(response.getTotalCopies()).isNull();
        assertThat(response.getAvailableCopies()).isNull();
    }

    @Test
    void shouldPartiallyUpdateEntityIgnoringNullValues() {
        // Given - existing book with all fields populated
        Book existingBook = Book.builder()
                .id(1L)
                .title("Original Title")
                .author("Original Author")
                .isbn("1234567890")
                .totalCopies(10L)
                .availableCopies(5L)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        // When - update with only title field (other fields are null)
        BookDto.Request partialRequest = BookDto.Request.builder()
                .title("Updated Title Only")
                .author(null)
                .isbn(null)
                .totalCopies(null)
                .availableCopies(null)
                .build();

        bookMapper.updateEntity(existingBook, partialRequest);

        // Then - only title should be updated, other fields should remain unchanged
        assertThat(existingBook.getId()).isEqualTo(1L);
        assertThat(existingBook.getTitle()).isEqualTo("Updated Title Only"); // Changed
        assertThat(existingBook.getAuthor()).isEqualTo("Original Author"); // Unchanged
        assertThat(existingBook.getIsbn()).isEqualTo("1234567890"); // Unchanged
        assertThat(existingBook.getTotalCopies()).isEqualTo(10L); // Unchanged
        assertThat(existingBook.getAvailableCopies()).isEqualTo(5L); // Unchanged
    }

    @Test
    void shouldPartiallyUpdateOnlyAvailableCopies() {
        // Given
        Book existingBook = Book.builder()
                .id(1L)
                .title("Original Title")
                .author("Original Author")
                .isbn("1234567890")
                .totalCopies(10L)
                .availableCopies(5L)
                .build();

        // When - update only availableCopies
        BookDto.Request partialRequest = BookDto.Request.builder()
                .title(null)
                .author(null)
                .isbn(null)
                .totalCopies(null)
                .availableCopies(3L)
                .build();

        bookMapper.updateEntity(existingBook, partialRequest);

        // Then
        assertThat(existingBook.getTitle()).isEqualTo("Original Title"); // Unchanged
        assertThat(existingBook.getAuthor()).isEqualTo("Original Author"); // Unchanged
        assertThat(existingBook.getIsbn()).isEqualTo("1234567890"); // Unchanged
        assertThat(existingBook.getTotalCopies()).isEqualTo(10L); // Unchanged
        assertThat(existingBook.getAvailableCopies()).isEqualTo(3L); // Changed
    }
}
