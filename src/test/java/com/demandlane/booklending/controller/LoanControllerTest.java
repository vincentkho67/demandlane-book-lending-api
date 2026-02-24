package com.demandlane.booklending.controller;

import com.demandlane.booklending.dto.LoanDto;
import com.demandlane.booklending.entity.Book;
import com.demandlane.booklending.entity.Loan;
import com.demandlane.booklending.entity.Role;
import com.demandlane.booklending.entity.User;
import com.demandlane.booklending.repository.BookRepository;
import com.demandlane.booklending.repository.LoanRepository;
import com.demandlane.booklending.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoanControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper;

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
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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

    // --- Authentication ---

    @Test
    void shouldReturn403WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/loans"))
                .andExpect(status().isForbidden());
    }


    // --- GET all ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAllLoansAsAdmin() throws Exception {
        // Given
        createTestLoan(user, book);

        // When & Then
        mockMvc.perform(get("/api/v1/loans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].userName").value("John Doe"))
                .andExpect(jsonPath("$.data[0].bookTitle").value("Clean Code"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAllLoansWithPagination() throws Exception {
        // Given
        createTestLoan(user, book);

        // When & Then
        mockMvc.perform(get("/api/v1/loans")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    // --- GET self (owned loans) ---

    @Test
    @WithMockUser(username = "john@example.com", roles = "MEMBER")
    void shouldGetOwnedLoansAsMember() throws Exception {
        // Given
        createTestLoan(user, book);

        // When & Then
        mockMvc.perform(get("/api/v1/loans/self"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].userName").value("John Doe"))
                .andExpect(jsonPath("$.data[0].bookTitle").value("Clean Code"));
    }

    @Test
    @WithMockUser(username = "john@example.com", roles = "ADMIN")
    void shouldGetOwnedLoansAsAdmin() throws Exception {
        // Given
        createTestLoan(user, book);

        // When & Then
        mockMvc.perform(get("/api/v1/loans/self"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].userName").value("John Doe"));
    }

    @Test
    @WithMockUser(username = "john@example.com", roles = "MEMBER")
    void shouldGetOwnedLoansWithPagination() throws Exception {
        // Given
        createTestLoan(user, book);

        // When & Then
        mockMvc.perform(get("/api/v1/loans/self")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @WithMockUser(username = "other@example.com", roles = "MEMBER")
    void shouldNotGetOtherUsersLoans() throws Exception {
        // Given - Create another user
        User otherUser = User.builder()
                .name("Other User")
                .email("other@example.com")
                .password("password123")
                .role(Role.MEMBER)
                .build();
        otherUser = userRepository.save(otherUser);

        // Create loan for john@example.com
        createTestLoan(user, book);

        // When & Then - other@example.com should see 0 loans
        mockMvc.perform(get("/api/v1/loans/self"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "john@example.com", roles = "MEMBER")
    void shouldFilterOwnedLoansByBookId() throws Exception {
        // Given
        createTestLoan(user, book);

        // When & Then
        mockMvc.perform(get("/api/v1/loans/self")
                        .param("bookId", book.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].bookId").value(book.getId()));
    }

    // --- Search / filter ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldFilterLoansByUserId() throws Exception {
        // Given
        createTestLoan(user, book);

        // When & Then
        mockMvc.perform(get("/api/v1/loans").param("userId", user.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].userId").value(user.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldFilterLoansByBookId() throws Exception {
        // Given
        createTestLoan(user, book);

        // When & Then
        mockMvc.perform(get("/api/v1/loans").param("bookId", book.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].bookId").value(book.getId()));
    }

    // --- GET by ID ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetLoanById() throws Exception {
        // Given
        Loan loan = createTestLoan(user, book);

        // When & Then
        mockMvc.perform(get("/api/v1/loans/" + loan.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loan.getId()))
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.bookId").value(book.getId()))
                .andExpect(jsonPath("$.userName").value("John Doe"))
                .andExpect(jsonPath("$.bookTitle").value("Clean Code"));
    }

    @Test
    @WithMockUser(username = "john@example.com", roles = "MEMBER")
    void shouldGetOwnLoanByIdAsMember() throws Exception {
        // Given - john is the loan owner
        Loan loan = createTestLoan(user, book);

        // When & Then
        mockMvc.perform(get("/api/v1/loans/" + loan.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loan.getId()))
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.userName").value("John Doe"));
    }

    @Test
    @WithMockUser(username = "other@example.com", roles = "MEMBER")
    void shouldReturn403WhenMemberViewsOtherUsersLoan() throws Exception {
        // Given - other@example.com exists in DB but is NOT the loan owner
        User otherUser = User.builder()
                .name("Other User")
                .email("other@example.com")
                .password("password123")
                .role(Role.MEMBER)
                .build();
        userRepository.save(otherUser);

        Loan loan = createTestLoan(user, book); // owned by john@example.com

        // When & Then
        mockMvc.perform(get("/api/v1/loans/" + loan.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not authorized to view this loan"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn404WhenLoanNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/loans/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Loan not found with id: 999"));
    }


    // --- POST ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateLoanAsAdmin() throws Exception {
        // Given
        LoanDto.Request request = LoanDto.Request.builder()
                .userId(user.getId())
                .bookId(book.getId())
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 15, 10, 0))
                .returnedAt(null)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.bookId").value(book.getId()))
                .andExpect(jsonPath("$.userName").value("John Doe"))
                .andExpect(jsonPath("$.bookTitle").value("Clean Code"));
    }


    // --- PUT ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateLoanAsAdmin() throws Exception {
        // Given
        Loan loan = createTestLoan(user, book);

        LoanDto.Request request = LoanDto.Request.builder()
                .userId(user.getId())
                .bookId(book.getId())
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 20, 10, 0)) // Extended
                .returnedAt(LocalDateTime.of(2024, 1, 10, 15, 30)) // Returned
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/loans/" + loan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loan.getId()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldPartiallyUpdateLoanDueDateOnly() throws Exception {
        // Given
        Loan loan = createTestLoan(user, book);

        // When - update only dueDate field
        LoanDto.Request partialRequest = LoanDto.Request.builder()
                .dueDate(LocalDateTime.of(2024, 1, 25, 10, 0)) // Extended
                .build(); // other fields are null

        // Then
        mockMvc.perform(put("/api/v1/loans/" + loan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loan.getId()))
                .andExpect(jsonPath("$.borrowedAt").value("2024-01-01T10:00:00")) // Unchanged
                .andExpect(jsonPath("$.dueDate").value("2024-01-25T10:00:00")) // Changed
                .andExpect(jsonPath("$.returnedAt").doesNotExist()); // Unchanged (null)
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldPartiallyUpdateLoanReturnedAtOnly() throws Exception {
        // Given
        Loan loan = createTestLoan(user, book);

        // When - update only returnedAt field
        LoanDto.Request partialRequest = LoanDto.Request.builder()
                .returnedAt(LocalDateTime.of(2024, 1, 10, 14, 30))
                .build(); // other fields are null

        // Then
        mockMvc.perform(put("/api/v1/loans/" + loan.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loan.getId()))
                .andExpect(jsonPath("$.borrowedAt").value("2024-01-01T10:00:00")) // Unchanged
                .andExpect(jsonPath("$.dueDate").value("2024-01-15T10:00:00")) // Unchanged
                .andExpect(jsonPath("$.returnedAt").value("2024-01-10T14:30:00")); // Changed
    }


    // --- DELETE ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldSoftDeleteLoanAsAdmin() throws Exception {
        // Given
        Loan loan = createTestLoan(user, book);

        // When & Then
        mockMvc.perform(delete("/api/v1/loans/" + loan.getId()))
                .andExpect(status().isNoContent());

        // Verify soft deleted
        Loan deleted = loanRepository.findById(loan.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(deleted.isDeleted()).isTrue();
    }


    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldNotReturnSoftDeletedLoan() throws Exception {
        // Given
        Loan loan = createTestLoan(user, book);
        loan.softDelete();
        loanRepository.save(loan);

        // When & Then
        mockMvc.perform(get("/api/v1/loans/" + loan.getId()))
                .andExpect(status().isNotFound());
    }

    // --- BORROW ENDPOINT ---

    @Test
    @WithMockUser(username = "john@example.com", roles = "MEMBER")
    void shouldBorrowBookSuccessfully() throws Exception {
        // Given
        LoanDto.BorrowRequest request = new LoanDto.BorrowRequest(book.getId());

        // When & Then
        mockMvc.perform(post("/api/v1/loans/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.bookId").value(book.getId()))
                .andExpect(jsonPath("$.borrowedAt").exists())
                .andExpect(jsonPath("$.dueDate").exists())
                .andExpect(jsonPath("$.returnedAt").doesNotExist());

        // Verify available copies decreased
        Book updatedBook = bookRepository.findById(book.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updatedBook.getAvailableCopies()).isEqualTo(4L);
    }

    @Test
    @WithMockUser(username = "john@example.com", roles = "MEMBER")
    void shouldRejectBorrowWhenMaxActiveLoansReached() throws Exception {
        // Given - Create 5 active loans (max-active-loans=5 in config)
        for (int i = 0; i < 5; i++) {
            Book newBook = Book.builder()
                    .title("Book " + i)
                    .author("Author " + i)
                    .isbn("ISBN" + i)
                    .totalCopies(10L)
                    .availableCopies(10L)
                    .build();
            newBook = bookRepository.save(newBook);
            createTestLoan(user, newBook);
        }

        // When & Then
        LoanDto.BorrowRequest request = new LoanDto.BorrowRequest(book.getId());
        mockMvc.perform(post("/api/v1/loans/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You have reached the maximum number of active loans (5). Please return a book before borrowing another."));
    }

    @Test
    @WithMockUser(username = "john@example.com", roles = "MEMBER")
    void shouldRejectBorrowWhenUserHasOverdueLoans() throws Exception {
        // Given - Create an overdue loan
        Loan overdueLoan = Loan.builder()
                .user(user)
                .book(book)
                .borrowedAt(LocalDateTime.now().minusDays(30))
                .dueDate(LocalDateTime.now().minusDays(1)) // Overdue
                .returnedAt(null)
                .build();
        loanRepository.save(overdueLoan);

        // Create another book to borrow
        Book newBook = Book.builder()
                .title("New Book")
                .author("New Author")
                .isbn("NEW123")
                .totalCopies(5L)
                .availableCopies(5L)
                .build();
        newBook = bookRepository.save(newBook);

        // When & Then
        LoanDto.BorrowRequest request = new LoanDto.BorrowRequest(newBook.getId());
        mockMvc.perform(post("/api/v1/loans/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You have overdue loans. Please return them before borrowing more books."));
    }

    @Test
    @WithMockUser(username = "john@example.com", roles = "MEMBER")
    void shouldRejectBorrowWhenNoAvailableCopies() throws Exception {
        // Given - Set available copies to 0
        book.setAvailableCopies(0L);
        bookRepository.save(book);

        // When & Then
        LoanDto.BorrowRequest request = new LoanDto.BorrowRequest(book.getId());
        mockMvc.perform(post("/api/v1/loans/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The book 'Clean Code' has no available copies at this time."));
    }

    @Test
    @WithMockUser(username = "john@example.com", roles = "MEMBER")
    void shouldRejectBorrowWhenBookNotFound() throws Exception {
        // When & Then
        LoanDto.BorrowRequest request = new LoanDto.BorrowRequest(999L);
        mockMvc.perform(post("/api/v1/loans/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found with id: 999"));
    }

    // --- RETURN ENDPOINT ---

    @Test
    @WithMockUser(username = "john@example.com", roles = "MEMBER")
    void shouldReturnBookSuccessfully() throws Exception {
        // Given
        book.setAvailableCopies(4L); // Simulate one copy borrowed
        bookRepository.save(book);
        Loan loan = createTestLoan(user, book);

        // When & Then
        mockMvc.perform(put("/api/v1/loans/return/" + loan.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(loan.getId()))
                .andExpect(jsonPath("$.returnedAt").exists());

        // Verify available copies increased
        Book updatedBook = bookRepository.findById(book.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updatedBook.getAvailableCopies()).isEqualTo(5L);
    }

    @Test
    @WithMockUser(username = "john@example.com", roles = "MEMBER")
    void shouldRejectReturnWhenAlreadyReturned() throws Exception {
        // Given - Create a loan that's already returned
        Loan loan = Loan.builder()
                .user(user)
                .book(book)
                .borrowedAt(LocalDateTime.now().minusDays(10))
                .dueDate(LocalDateTime.now().plusDays(4))
                .returnedAt(LocalDateTime.now().minusDays(1)) // Already returned
                .build();
        loan = loanRepository.save(loan);

        // When & Then
        mockMvc.perform(put("/api/v1/loans/return/" + loan.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This loan has already been returned"));
    }

    @Test
    @WithMockUser(username = "john@example.com", roles = "MEMBER")
    void shouldRejectReturnWhenLoanNotFound() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/v1/loans/return/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Loan not found with id: 999"));
    }

    @Test
    @WithMockUser(username = "admin@example.com", roles = "ADMIN")
    void shouldAllowAdminToBorrowBook() throws Exception {
        // Given - Create admin user
        User admin = User.builder()
                .name("Admin User")
                .email("admin@example.com")
                .password("admin123")
                .role(Role.ADMIN)
                .build();
        admin = userRepository.save(admin);

        // When & Then
        LoanDto.BorrowRequest request = new LoanDto.BorrowRequest(book.getId());
        mockMvc.perform(post("/api/v1/loans/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(admin.getId()));
    }

    // --- Helper ---

    private Loan createTestLoan(User user, Book book) {
        Loan loan = Loan.builder()
                .user(user)
                .book(book)
                .borrowedAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .dueDate(LocalDateTime.of(2024, 1, 15, 10, 0))
                .returnedAt(null)
                .build();
        return loanRepository.save(loan);
    }
}
