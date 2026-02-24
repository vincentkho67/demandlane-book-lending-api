package com.demandlane.booklending.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.demandlane.booklending.entity.Book;
import com.demandlane.booklending.entity.Loan;
import com.demandlane.booklending.entity.Role;
import com.demandlane.booklending.entity.User;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class LoanRepositoryIntegrationTest {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        // Clean up
        loanRepository.deleteAll();
        userRepository.deleteAll();
        bookRepository.deleteAll();

        // Create test user
        user = User.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("password123")
                .role(Role.MEMBER)
                .build();
        user = userRepository.save(user);

        // Create test book
        book = Book.builder()
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .totalCopies(5L)
                .availableCopies(5L)
                .build();
        book = bookRepository.save(book);
    }

    private Loan createTestLoan() {
        Loan loan = Loan.builder()
                .user(user)
                .book(book)
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 15, 10, 0))
                .returnedAt(null)
                .build();
        return loan;
    }

    @Test
    void shouldSaveAndFindLoan() {
        // Given
        Loan loan = createTestLoan();

        // When
        loanRepository.save(loan);
        Loan foundLoan = loanRepository.findById(loan.getId()).orElse(null);

        // Then
        assertNotNull(foundLoan);
        assertEquals(loan.getUser().getId(), foundLoan.getUser().getId());
        assertEquals(loan.getBook().getId(), foundLoan.getBook().getId());
        assertEquals(loan.getBorrowedAt(), foundLoan.getBorrowedAt());
        assertEquals(loan.getDueDate(), foundLoan.getDueDate());
        assertEquals(loan.getReturnedAt(), foundLoan.getReturnedAt());
    }

    @Test
    void shouldUpdateLoan() {
        // Given
        Loan loan = createTestLoan();
        loanRepository.save(loan);

        // When
        loan.setReturnedAt(LocalDateTime.of(2024, 1, 10, 15, 30));
        loanRepository.save(loan);
        Loan updatedLoan = loanRepository.findById(loan.getId()).orElse(null);

        // Then
        assertNotNull(updatedLoan);
        assertEquals(LocalDateTime.of(2024, 1, 10, 15, 30), updatedLoan.getReturnedAt());
    }

    @Test
    void shouldDeleteLoan() {
        // Given
        Loan loan = createTestLoan();
        loanRepository.save(loan);
        Long loanId = loan.getId();

        // When
        loanRepository.deleteById(loanId);

        // Then
        assertEquals(false, loanRepository.existsById(loanId));
    }

    @Test
    void shouldFindAllLoans() {
        // Given
        Loan loan1 = createTestLoan();
        Loan loan2 = createTestLoan();
        loanRepository.save(loan1);
        loanRepository.save(loan2);

        // When
        long count = loanRepository.count();

        // Then
        assertEquals(2, count);
    }

    @Test
    void shouldSaveLoanWithReturnedDate() {
        // Given
        Loan loan = createTestLoan();
        loan.setReturnedAt(LocalDateTime.of(2024, 1, 10, 15, 30));

        // When
        loanRepository.save(loan);
        Loan foundLoan = loanRepository.findById(loan.getId()).orElse(null);

        // Then
        assertNotNull(foundLoan);
        assertNotNull(foundLoan.getReturnedAt());
        assertEquals(LocalDateTime.of(2024, 1, 10, 15, 30), foundLoan.getReturnedAt());
    }

    @Test
    void shouldHandleMultipleLoansForSameUser() {
        // Given
        Book anotherBook = Book.builder()
                .title("The Pragmatic Programmer")
                .author("Andrew Hunt")
                .isbn("9780135957059")
                .totalCopies(3L)
                .availableCopies(3L)
                .build();
        anotherBook = bookRepository.save(anotherBook);

        Loan loan1 = createTestLoan();
        Loan loan2 = Loan.builder()
                .user(user)
                .book(anotherBook)
                .borrowedAt(LocalDateTime.of(2024, 2, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 2, 15, 10, 0))
                .returnedAt(null)
                .build();

        // When
        loanRepository.save(loan1);
        loanRepository.save(loan2);

        // Then
        long count = loanRepository.count();
        assertEquals(2, count);
    }
}
