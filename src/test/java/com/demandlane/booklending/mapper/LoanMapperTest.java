package com.demandlane.booklending.mapper;

import com.demandlane.booklending.dto.LoanDto;
import com.demandlane.booklending.entity.Book;
import com.demandlane.booklending.entity.Loan;
import com.demandlane.booklending.entity.Role;
import com.demandlane.booklending.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class LoanMapperTest {

    @Autowired
    private LoanMapper loanMapper;

    @Test
    void shouldMapEntityToResponse() {
        // Given
        User user = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .role(Role.MEMBER)
                .build();

        Book book = Book.builder()
                .id(2L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .build();

        Loan loan = Loan.builder()
                .id(1L)
                .user(user)
                .book(book)
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 15, 10, 0))
                .returnedAt(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // When
        LoanDto.Response response = loanMapper.toResponse(loan);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getUserName()).isEqualTo("John Doe");
        assertThat(response.getUserEmail()).isEqualTo("john@example.com");
        assertThat(response.getBookId()).isEqualTo(2L);
        assertThat(response.getBookTitle()).isEqualTo("Clean Code");
        assertThat(response.getBookAuthor()).isEqualTo("Robert C. Martin");
        assertThat(response.getBorrowedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0));
        assertThat(response.getDueDate()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 0));
        assertThat(response.getReturnedAt()).isNull();
        assertThat(response.getCreatedAt()).isNotNull();
        assertThat(response.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldMapRequestToEntity() {
        // Given
        LoanDto.Request request = LoanDto.Request.builder()
                .userId(1L)
                .bookId(2L)
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 15, 10, 0))
                .returnedAt(null)
                .build();

        // When
        Loan loan = loanMapper.toEntity(request);

        // Then
        assertThat(loan).isNotNull();
        assertThat(loan.getBorrowedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0));
        assertThat(loan.getDueDate()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 0));
        assertThat(loan.getReturnedAt()).isNull();
        assertThat(loan.getId()).isNull(); // Should not map ID
        assertThat(loan.getUser()).isNull(); // Should not map user (handled in service)
        assertThat(loan.getBook()).isNull(); // Should not map book (handled in service)
        assertThat(loan.getCreatedAt()).isNull(); // Should not map createdAt
        assertThat(loan.getUpdatedAt()).isNull(); // Should not map updatedAt
    }

    @Test
    void shouldUpdateEntityFromRequest() {
        // Given
        User user = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .build();

        Book book = Book.builder()
                .id(2L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .build();

        Loan existingLoan = Loan.builder()
                .id(1L)
                .user(user)
                .book(book)
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 15, 10, 0))
                .returnedAt(null)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        LoanDto.Request request = LoanDto.Request.builder()
                .userId(1L)
                .bookId(2L)
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 20, 10, 0)) // Extended due date
                .returnedAt(LocalDateTime.of(2024, 1, 10, 10, 0)) // Book returned
                .build();

        // When
        loanMapper.updateEntity(existingLoan, request);

        // Then
        assertThat(existingLoan.getId()).isEqualTo(1L); // ID should not change
        assertThat(existingLoan.getUser()).isEqualTo(user); // User should not change
        assertThat(existingLoan.getBook()).isEqualTo(book); // Book should not change
        assertThat(existingLoan.getDueDate()).isEqualTo(LocalDateTime.of(2024, 1, 20, 10, 0));
        assertThat(existingLoan.getReturnedAt()).isEqualTo(LocalDateTime.of(2024, 1, 10, 10, 0));
    }

    @Test
    void shouldHandleNullEntityGracefully() {
        // When
        LoanDto.Response response = loanMapper.toResponse(null);

        // Then
        assertThat(response).isNull();
    }

    @Test
    void shouldHandleNullRequestGracefully() {
        // When
        Loan loan = loanMapper.toEntity(null);

        // Then
        assertThat(loan).isNull();
    }

    @Test
    void shouldMapEntityWithReturnedLoan() {
        // Given
        User user = User.builder()
                .id(1L)
                .name("Jane Smith")
                .email("jane@example.com")
                .role(Role.MEMBER)
                .build();

        Book book = Book.builder()
                .id(2L)
                .title("The Pragmatic Programmer")
                .author("Andrew Hunt")
                .isbn("9780135957059")
                .build();

        Loan loan = Loan.builder()
                .id(1L)
                .user(user)
                .book(book)
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 15, 10, 0))
                .returnedAt(LocalDateTime.of(2024, 1, 10, 15, 30)) // Returned
                .build();

        // When
        LoanDto.Response response = loanMapper.toResponse(loan);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getReturnedAt()).isEqualTo(LocalDateTime.of(2024, 1, 10, 15, 30));
        assertThat(response.getUserName()).isEqualTo("Jane Smith");
        assertThat(response.getBookTitle()).isEqualTo("The Pragmatic Programmer");
    }

    @Test
    void shouldPartiallyUpdateEntityIgnoringNullValues() {
        // Given - existing loan with all fields populated
        User user = User.builder().id(1L).name("John Doe").build();
        Book book = Book.builder().id(2L).title("Clean Code").build();

        Loan existingLoan = Loan.builder()
                .id(1L)
                .user(user)
                .book(book)
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 15, 10, 0))
                .returnedAt(null)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        // When - update with only dueDate field (other fields are null)
        LoanDto.Request partialRequest = LoanDto.Request.builder()
                .userId(null)
                .bookId(null)
                .borrowedAt(null)
                .dueDate(LocalDateTime.of(2024, 1, 20, 10, 0)) // Extend due date
                .returnedAt(null)
                .build();

        loanMapper.updateEntity(existingLoan, partialRequest);

        // Then - only dueDate should be updated, other fields should remain unchanged
        assertThat(existingLoan.getId()).isEqualTo(1L);
        assertThat(existingLoan.getUser()).isEqualTo(user); // Unchanged
        assertThat(existingLoan.getBook()).isEqualTo(book); // Unchanged
        assertThat(existingLoan.getBorrowedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0)); // Unchanged
        assertThat(existingLoan.getDueDate()).isEqualTo(LocalDateTime.of(2024, 1, 20, 10, 0)); // Changed
        assertThat(existingLoan.getReturnedAt()).isNull(); // Unchanged
    }

    @Test
    void shouldPartiallyUpdateOnlyReturnedAt() {
        // Given
        User user = User.builder().id(1L).name("John Doe").build();
        Book book = Book.builder().id(2L).title("Clean Code").build();

        Loan existingLoan = Loan.builder()
                .id(1L)
                .user(user)
                .book(book)
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 15, 10, 0))
                .returnedAt(null)
                .build();

        // When - update only returnedAt to mark as returned
        LoanDto.Request partialRequest = LoanDto.Request.builder()
                .userId(null)
                .bookId(null)
                .borrowedAt(null)
                .dueDate(null)
                .returnedAt(LocalDateTime.of(2024, 1, 10, 14, 30))
                .build();

        loanMapper.updateEntity(existingLoan, partialRequest);

        // Then
        assertThat(existingLoan.getUser()).isEqualTo(user); // Unchanged
        assertThat(existingLoan.getBook()).isEqualTo(book); // Unchanged
        assertThat(existingLoan.getBorrowedAt()).isEqualTo(LocalDateTime.of(2024, 1, 1, 10, 0)); // Unchanged
        assertThat(existingLoan.getDueDate()).isEqualTo(LocalDateTime.of(2024, 1, 15, 10, 0)); // Unchanged
        assertThat(existingLoan.getReturnedAt()).isEqualTo(LocalDateTime.of(2024, 1, 10, 14, 30)); // Changed
    }
}
