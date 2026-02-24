package com.demandlane.booklending.service;

import com.demandlane.booklending.dto.LoanDto;
import com.demandlane.booklending.entity.Book;
import com.demandlane.booklending.entity.Loan;
import com.demandlane.booklending.entity.Role;
import com.demandlane.booklending.entity.User;
import com.demandlane.booklending.exception.ResourceNotFoundException;
import com.demandlane.booklending.mapper.LoanMapper;
import com.demandlane.booklending.repository.BookRepository;
import com.demandlane.booklending.repository.LoanRepository;
import com.demandlane.booklending.repository.UserRepository;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private LoanMapper loanMapper;

    @InjectMocks
    private LoanService loanService;

    private Loan loan;
    private User user;
    private Book book;
    private LoanDto.Request loanRequest;
    private LoanDto.Response loanResponse;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .name("John Doe")
                .email("john@example.com")
                .role(Role.MEMBER)
                .build();

        book = Book.builder()
                .id(2L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("9780132350884")
                .build();

        loan = Loan.builder()
                .id(1L)
                .user(user)
                .book(book)
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 15, 10, 0))
                .returnedAt(null)
                .build();

        loanRequest = LoanDto.Request.builder()
                .userId(1L)
                .bookId(2L)
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 15, 10, 0))
                .returnedAt(null)
                .build();

        loanResponse = LoanDto.Response.builder()
                .id(1L)
                .userId(1L)
                .userName("John Doe")
                .userEmail("john@example.com")
                .bookId(2L)
                .bookTitle("Clean Code")
                .bookAuthor("Robert C. Martin")
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 15, 10, 0))
                .returnedAt(null)
                .build();
    }

    @Test
    void shouldFindAllLoans() {
        // Given
        Page<Loan> loanPage = new PageImpl<>(List.of(loan));
        Pageable pageable = PageRequest.of(0, 10);
        LoanDto.Filter filter = new LoanDto.Filter();

        when(loanRepository.findAll(ArgumentMatchers.<Specification<Loan>>any(), any(Pageable.class))).thenReturn(loanPage);
        when(loanMapper.toResponse(any(Loan.class))).thenReturn(loanResponse);

        // When
        Page<LoanDto.Response> result = loanService.findAll(filter, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUserName()).isEqualTo("John Doe");
        assertThat(result.getContent().get(0).getBookTitle()).isEqualTo("Clean Code");

        verify(loanRepository).findAll(ArgumentMatchers.<Specification<Loan>>any(), eq(pageable));
        verify(loanMapper).toResponse(loan);
    }

    @Test
    void shouldFindAllLoansWithFilter() {
        // Given
        Page<Loan> loanPage = new PageImpl<>(List.of(loan));
        Pageable pageable = PageRequest.of(0, 10);
        LoanDto.Filter filter = new LoanDto.Filter(1L, 2L);

        when(loanRepository.findAll(ArgumentMatchers.<Specification<Loan>>any(), any(Pageable.class))).thenReturn(loanPage);
        when(loanMapper.toResponse(any(Loan.class))).thenReturn(loanResponse);

        // When
        Page<LoanDto.Response> result = loanService.findAll(filter, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(loanRepository).findAll(ArgumentMatchers.<Specification<Loan>>any(), eq(pageable));
        verify(loanMapper).toResponse(loan);
    }

    @Test
    void shouldFindLoanById() {
        // Given
        Authentication auth = mock(Authentication.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(auth).getAuthorities();

        when(loanRepository.findActiveById(anyLong())).thenReturn(Optional.of(loan));
        when(loanMapper.toResponse(any(Loan.class))).thenReturn(loanResponse);

        // When
        LoanDto.Response result = loanService.findById(1L, auth);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getBookId()).isEqualTo(2L);

        verify(loanRepository).findActiveById(1L);
        verify(loanMapper).toResponse(loan);
    }

    @Test
    void shouldFindLoanById_whenMemberOwnsLoan() {
        // Given - member accessing their own loan
        Authentication auth = mock(Authentication.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))).when(auth).getAuthorities();
        when(auth.getName()).thenReturn("john@example.com");

        when(loanRepository.findActiveById(anyLong())).thenReturn(Optional.of(loan));
        when(userRepository.findActiveByEmail("john@example.com")).thenReturn(Optional.of(user)); // user.id=1L matches loan.user.id=1L
        when(loanMapper.toResponse(any(Loan.class))).thenReturn(loanResponse);

        // When
        LoanDto.Response result = loanService.findById(1L, auth);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(userRepository).findActiveByEmail("john@example.com");
        verify(loanMapper).toResponse(loan);
    }

    @Test
    void shouldThrowAccessDenied_whenMemberAccessesOtherUsersLoan() {
        // Given - member trying to access another user's loan
        User otherMember = User.builder()
                .id(99L)
                .name("Other User")
                .email("other@example.com")
                .role(Role.MEMBER)
                .build();

        Authentication auth = mock(Authentication.class);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_MEMBER"))).when(auth).getAuthorities();
        when(auth.getName()).thenReturn("other@example.com");

        when(loanRepository.findActiveById(anyLong())).thenReturn(Optional.of(loan)); // loan.user.id=1L
        when(userRepository.findActiveByEmail("other@example.com")).thenReturn(Optional.of(otherMember)); // id=99L

        // When & Then
        assertThatThrownBy(() -> loanService.findById(1L, auth))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not authorized");

        verify(userRepository).findActiveByEmail("other@example.com");
        verify(loanMapper, never()).toResponse(any());
    }

    @Test
    void shouldThrowException_whenLoanNotFound() {
        // Given
        when(loanRepository.findActiveById(anyLong())).thenReturn(Optional.empty());
        Authentication auth = mock(Authentication.class);

        // When & Then
        assertThatThrownBy(() -> loanService.findById(999L, auth))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan not found");

        verify(loanRepository).findActiveById(999L);
        verify(loanMapper, never()).toResponse(any());
    }

    @Test
    void shouldSaveLoan() {
        // Given
        when(userRepository.findActiveById(anyLong())).thenReturn(Optional.of(user));
        when(bookRepository.findActiveById(anyLong())).thenReturn(Optional.of(book));
        when(loanMapper.toEntity(any(LoanDto.Request.class))).thenReturn(loan);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        when(loanMapper.toResponse(any(Loan.class))).thenReturn(loanResponse);

        // When
        LoanDto.Response result = loanService.save(loanRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserName()).isEqualTo("John Doe");
        assertThat(result.getBookTitle()).isEqualTo("Clean Code");

        verify(userRepository).findActiveById(1L);
        verify(bookRepository).findActiveById(2L);
        verify(loanMapper).toEntity(loanRequest);
        verify(loanRepository).save(loan);
        verify(loanMapper).toResponse(loan);
    }

    @Test
    void shouldThrowException_whenUserNotFoundDuringSave() {
        // Given
        when(userRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> loanService.save(loanRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findActiveById(1L);
        verify(bookRepository, never()).findActiveById(any());
        verify(loanRepository, never()).save(any());
    }

    @Test
    void shouldThrowException_whenBookNotFoundDuringSave() {
        // Given
        when(userRepository.findActiveById(anyLong())).thenReturn(Optional.of(user));
        when(bookRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> loanService.save(loanRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Book not found");

        verify(userRepository).findActiveById(1L);
        verify(bookRepository).findActiveById(2L);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void shouldUpdateLoan() {
        // Given
        LoanDto.Request updateRequest = LoanDto.Request.builder()
                .userId(1L)
                .bookId(2L)
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 20, 10, 0)) // Extended
                .returnedAt(LocalDateTime.of(2024, 1, 10, 10, 0)) // Returned
                .build();

        Loan updatedLoan = Loan.builder()
                .id(1L)
                .user(user)
                .book(book)
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 20, 10, 0))
                .returnedAt(LocalDateTime.of(2024, 1, 10, 10, 0))
                .build();

        LoanDto.Response updatedResponse = LoanDto.Response.builder()
                .id(1L)
                .userId(1L)
                .userName("John Doe")
                .userEmail("john@example.com")
                .bookId(2L)
                .bookTitle("Clean Code")
                .bookAuthor("Robert C. Martin")
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 20, 10, 0))
                .returnedAt(LocalDateTime.of(2024, 1, 10, 10, 0))
                .build();

        when(loanRepository.findActiveById(anyLong())).thenReturn(Optional.of(loan));
        doNothing().when(loanMapper).updateEntity(any(Loan.class), any(LoanDto.Request.class));
        when(loanRepository.save(any(Loan.class))).thenReturn(updatedLoan);
        when(loanMapper.toResponse(any(Loan.class))).thenReturn(updatedResponse);

        // When
        LoanDto.Response result = loanService.update(1L, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDueDate()).isEqualTo(LocalDateTime.of(2024, 1, 20, 10, 0));
        assertThat(result.getReturnedAt()).isEqualTo(LocalDateTime.of(2024, 1, 10, 10, 0));

        verify(loanRepository).findActiveById(1L);
        verify(loanMapper).updateEntity(loan, updateRequest);
        verify(loanRepository).save(loan);
        verify(loanMapper).toResponse(updatedLoan);
    }

    @Test
    void shouldThrowException_whenUpdatingNonExistentLoan() {
        // Given
        when(loanRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> loanService.update(999L, loanRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan not found");

        verify(loanRepository).findActiveById(999L);
        verify(loanMapper, never()).updateEntity(any(), any());
        verify(loanRepository, never()).save(any());
    }

    @Test
    void shouldSoftDeleteLoan() {
        // Given
        when(loanRepository.findActiveById(anyLong())).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        // When
        loanService.delete(1L);

        // Then
        verify(loanRepository).findActiveById(1L);
        verify(loanRepository).save(loan);
        assertThat(loan.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldThrowException_whenDeletingNonExistentLoan() {
        // Given
        when(loanRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> loanService.delete(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Loan not found");

        verify(loanRepository).findActiveById(999L);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void shouldUpdateLoanWithDifferentMember() {
        // Given
        User newMember = User.builder()
                .id(3L)
                .name("Jane Smith")
                .email("jane@example.com")
                .build();

        LoanDto.Request updateRequest = LoanDto.Request.builder()
                .userId(3L) // Different member
                .bookId(2L)
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 15, 10, 0))
                .build();

        when(loanRepository.findActiveById(anyLong())).thenReturn(Optional.of(loan));
        when(userRepository.findActiveById(3L)).thenReturn(Optional.of(newMember));
        doNothing().when(loanMapper).updateEntity(any(Loan.class), any(LoanDto.Request.class));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        when(loanMapper.toResponse(any(Loan.class))).thenReturn(loanResponse);

        // When
        loanService.update(1L, updateRequest);

        // Then
        verify(userRepository).findActiveById(3L);
        verify(loanRepository).save(loan);
    }
}
