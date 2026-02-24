package com.demandlane.booklending.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demandlane.booklending.config.LibraryProperties;
import com.demandlane.booklending.dto.LoanDto;
import com.demandlane.booklending.entity.Book;
import com.demandlane.booklending.entity.Loan;
import com.demandlane.booklending.entity.User;
import com.demandlane.booklending.exception.BorrowingRuleViolationException;
import com.demandlane.booklending.exception.ResourceNotFoundException;
import com.demandlane.booklending.mapper.LoanMapper;
import com.demandlane.booklending.repository.BookRepository;
import com.demandlane.booklending.repository.LoanRepository;
import com.demandlane.booklending.repository.UserRepository;
import com.demandlane.booklending.specification.SpecificationBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanService.class);

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final LoanMapper loanMapper;
    private final LibraryProperties libraryProperties;

    public Page<LoanDto.Response> findAll(LoanDto.Filter filter, Pageable pageable) {
        Specification<Loan> spec = SpecificationBuilder.fromFilter(filter, Loan.class);
        Page<Loan> loans = loanRepository.findAll(spec, pageable);
        return loans.map(loanMapper::toResponse);
    }

    public LoanDto.Response findById(Long id, Authentication authentication) {
        Loan loan = loanRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + id));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            User requester = userRepository.findActiveByEmail(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            if (!loan.getUser().getId().equals(requester.getId())) {
                throw new AccessDeniedException("You are not authorized to view this loan");
            }
        }

        return loanMapper.toResponse(loan);
    }

    public LoanDto.Response save(LoanDto.Request request) {
        User user = userRepository.findActiveById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));
        Book book = bookRepository.findActiveById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + request.getBookId()));

        Loan loan = loanMapper.toEntity(request);
        loan.setUser(user);
        loan.setBook(book);

        Loan saved = loanRepository.save(loan);
        return loanMapper.toResponse(saved);
    }

    public LoanDto.Response update(Long id, LoanDto.Request request) {
        Loan existing = loanRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + id));

        if (request.getUserId() != null && !request.getUserId().equals(existing.getUser().getId())) {
            User user = userRepository.findActiveById(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));
            existing.setUser(user);
        }

        if (request.getBookId() != null && !request.getBookId().equals(existing.getBook().getId())) {
            Book book = bookRepository.findActiveById(request.getBookId())
                    .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + request.getBookId()));
            existing.setBook(book);
        }

        loanMapper.updateEntity(existing, request);

        Loan updated = loanRepository.save(existing);
        return loanMapper.toResponse(updated);
    }

    public void delete(Long id) {
        Loan loan = loanRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + id));
        loan.softDelete();
        loanRepository.save(loan);
    }

    /**
     * Borrow a book for a user with automatic borrowing rules validation.
     *
     * @param userId The user borrowing the book
     * @param bookId The book to borrow
     * @return The created loan
     * @throws BorrowingRuleViolationException if any borrowing rule is violated
     */
    public LoanDto.Response borrowBook(Long userId, Long bookId) {
        log.info("Processing borrow request for user {} and book {}", userId, bookId);

        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        Book book = bookRepository.findActiveById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        validateBorrowing(user, book);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueDate = now.plusDays(libraryProperties.getLoanDurationDays());

        Loan loan = Loan.builder()
                .user(user)
                .book(book)
                .borrowedAt(now)
                .dueDate(dueDate)
                .build();

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        Loan saved = loanRepository.save(loan);
        log.info("Loan created successfully with id {} for user {} and book {}", saved.getId(), userId, bookId);

        return loanMapper.toResponse(saved);
    }

    /**
     * Return a borrowed book.
     *
     * @param loanId The loan to return
     * @return The updated loan
     */
    public LoanDto.Response returnBook(Long loanId) {
        log.info("Processing return request for loan {}", loanId);

        Loan loan = loanRepository.findActiveById(loanId)
            .orElseThrow(() -> new ResourceNotFoundException("Loan not found with id: " + loanId));

        if (loan.getReturnedAt() != null) {
            log.warn("Loan {} has already been returned", loanId);
            throw new BorrowingRuleViolationException("This loan has already been returned");
        }

        loan.setReturnedAt(LocalDateTime.now());

        Book book = loan.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        Loan updated = loanRepository.save(loan);
        log.info("Loan {} returned successfully", loanId);

        return loanMapper.toResponse(updated);
    }

    /**
     * Validates whether a user can borrow a book.
     */
    private void validateBorrowing(User user, Book book) {
        log.debug("Validating borrowing rules for user {} and book {}", user.getId(), book.getId());

        long activeLoans = loanRepository.countActiveLoans(user.getId());
        if (activeLoans >= libraryProperties.getMaxActiveLoans()) {
            log.warn("User {} has {} active loans, exceeding max of {}",
                     user.getId(), activeLoans, libraryProperties.getMaxActiveLoans());
            throw new BorrowingRuleViolationException(
                String.format("You have reached the maximum number of active loans (%d). " +
                              "Please return a book before borrowing another.",
                              libraryProperties.getMaxActiveLoans())
            );
        }

        if (loanRepository.hasOverdueLoans(user.getId(), LocalDateTime.now())) {
            log.warn("User {} has overdue loans", user.getId());
            throw new BorrowingRuleViolationException(
                "You have overdue loans. Please return them before borrowing more books."
            );
        }

        if (book.getAvailableCopies() <= 0) {
            log.warn("Book {} has no available copies", book.getId());
            throw new BorrowingRuleViolationException(
                String.format("The book '%s' has no available copies at this time.", book.getTitle())
            );
        }

        log.debug("All borrowing rules passed for user {} and book {}", user.getId(), book.getId());
    }
}
