package com.demandlane.booklending.controller;

import com.demandlane.booklending.dto.BookDto;
import com.demandlane.booklending.entity.Book;
import com.demandlane.booklending.repository.BookRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private ObjectMapper objectMapper;

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        objectMapper = new ObjectMapper();
        bookRepository.deleteAll();
    }

    // --- Authentication ---

    @Test
    void shouldReturn403WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isForbidden());
    }

    // --- GET all ---

    @Test
    @WithMockUser(roles = "MEMBER")
    void shouldGetAllBooksAsMember() throws Exception {
        // Given
        createTestBook("Clean Code", "Robert Martin", "9780132350884");

        // When & Then
        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("Clean Code"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAllBooksAsAdmin() throws Exception {
        // Given
        createTestBook("Clean Code", "Robert Martin", "9780132350884");

        // When & Then
        mockMvc.perform(get("/api/v1/books")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.data[0].author").value("Robert Martin"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void shouldGetAllBooksWithoutPaginationParams() throws Exception {
        // Given
        createTestBook("Clean Code", "Robert Martin", "9780132350884");

        // When & Then - should use defaults (page=0, size=10) without failing
        mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // --- Search / filter ---

    @Test
    @WithMockUser(roles = "MEMBER")
    void shouldFilterBooksByTitle() throws Exception {
        // Given
        createTestBook("Clean Code", "Robert Martin", "9780132350884");
        createTestBook("The Pragmatic Programmer", "Andrew Hunt", "9780135957059");

        // When & Then
        mockMvc.perform(get("/api/v1/books").param("title", "clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Clean Code"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void shouldFilterBooksByAuthor() throws Exception {
        // Given
        createTestBook("Clean Code", "Robert Martin", "9780132350884");
        createTestBook("The Pragmatic Programmer", "Andrew Hunt", "9780135957059");

        // When & Then
        mockMvc.perform(get("/api/v1/books").param("author", "martin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].author").value("Robert Martin"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void shouldFilterBooksByIsbn() throws Exception {
        // Given
        createTestBook("Clean Code", "Robert Martin", "9780132350884");
        createTestBook("The Pragmatic Programmer", "Andrew Hunt", "9780135957059");

        // When & Then
        mockMvc.perform(get("/api/v1/books").param("isbn", "9780132350884"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].isbn").value("9780132350884"));
    }

    // --- GET by ID ---

    @Test
    @WithMockUser(roles = "MEMBER")
    void shouldGetBookById() throws Exception {
        // Given
        Book book = createTestBook("Clean Code", "Robert Martin", "9780132350884");

        // When & Then
        mockMvc.perform(get("/api/v1/books/" + book.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(book.getId()))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.author").value("Robert Martin"))
                .andExpect(jsonPath("$.isbn").value("9780132350884"));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void shouldReturn404WhenBookNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/books/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Book not found with id: 999"));
    }

    // --- POST ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateBookAsAdmin() throws Exception {
        // Given
        BookDto.Request request = BookDto.Request.builder()
                .title("Clean Code")
                .author("Robert Martin")
                .isbn("9780132350884")
                .totalCopies(5L)
                .availableCopies(5L)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.author").value("Robert Martin"))
                .andExpect(jsonPath("$.isbn").value("9780132350884"))
                .andExpect(jsonPath("$.totalCopies").value(5))
                .andExpect(jsonPath("$.availableCopies").value(5));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void shouldReturn403WhenMemberTriesToCreate() throws Exception {
        // Given
        BookDto.Request request = BookDto.Request.builder()
                .title("Clean Code")
                .author("Robert Martin")
                .isbn("9780132350884")
                .totalCopies(5L)
                .availableCopies(5L)
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- PUT ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateBookAsAdmin() throws Exception {
        // Given
        Book book = createTestBook("Old Title", "Old Author", "1111111111");

        BookDto.Request request = BookDto.Request.builder()
                .title("Updated Title")
                .author("Updated Author")
                .isbn("1111111111")
                .totalCopies(10L)
                .availableCopies(8L)
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/books/" + book.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(book.getId()))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.author").value("Updated Author"))
                .andExpect(jsonPath("$.totalCopies").value(10))
                .andExpect(jsonPath("$.availableCopies").value(8));
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void shouldReturn403WhenMemberTriesToUpdate() throws Exception {
        // Given
        Book book = createTestBook("Clean Code", "Robert Martin", "9780132350884");

        BookDto.Request request = BookDto.Request.builder()
                .title("Updated Title")
                .author("Updated Author")
                .isbn("9780132350884")
                .totalCopies(10L)
                .availableCopies(8L)
                .build();

        // When & Then
        mockMvc.perform(put("/api/v1/books/" + book.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldPartiallyUpdateBookTitleOnly() throws Exception {
        // Given
        Book book = createTestBook("Original Title", "Original Author", "1234567890");

        // When - update only title field
        BookDto.Request partialRequest = BookDto.Request.builder()
                .title("Updated Title Only")
                .build(); // other fields are null

        // Then
        mockMvc.perform(put("/api/v1/books/" + book.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(book.getId()))
                .andExpect(jsonPath("$.title").value("Updated Title Only"))
                .andExpect(jsonPath("$.author").value("Original Author")) // Unchanged
                .andExpect(jsonPath("$.isbn").value("1234567890")) // Unchanged
                .andExpect(jsonPath("$.totalCopies").value(5)) // Unchanged
                .andExpect(jsonPath("$.availableCopies").value(5)); // Unchanged
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldPartiallyUpdateBookAvailableCopiesOnly() throws Exception {
        // Given
        Book book = createTestBook("Original Title", "Original Author", "1234567890");

        // When - update only availableCopies field
        BookDto.Request partialRequest = BookDto.Request.builder()
                .availableCopies(3L)
                .build(); // other fields are null

        // Then
        mockMvc.perform(put("/api/v1/books/" + book.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(partialRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(book.getId()))
                .andExpect(jsonPath("$.title").value("Original Title")) // Unchanged
                .andExpect(jsonPath("$.author").value("Original Author")) // Unchanged
                .andExpect(jsonPath("$.isbn").value("1234567890")) // Unchanged
                .andExpect(jsonPath("$.totalCopies").value(5)) // Unchanged
                .andExpect(jsonPath("$.availableCopies").value(3)); // Changed
    }

    // --- DELETE ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldSoftDeleteBookAsAdmin() throws Exception {
        // Given
        Book book = createTestBook("Clean Code", "Robert Martin", "9780132350884");

        // When & Then
        mockMvc.perform(delete("/api/v1/books/" + book.getId()))
                .andExpect(status().isNoContent());

        // Verify soft deleted
        Book deleted = bookRepository.findById(book.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(deleted.isDeleted()).isTrue();
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void shouldReturn403WhenMemberTriesToDelete() throws Exception {
        // Given
        Book book = createTestBook("Clean Code", "Robert Martin", "9780132350884");

        // When & Then
        mockMvc.perform(delete("/api/v1/books/" + book.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void shouldNotReturnSoftDeletedBook() throws Exception {
        // Given
        Book book = createTestBook("Clean Code", "Robert Martin", "9780132350884");
        book.softDelete();
        bookRepository.save(book);

        // When & Then
        mockMvc.perform(get("/api/v1/books/" + book.getId()))
                .andExpect(status().isNotFound());
    }

    // --- Helper ---

    private Book createTestBook(String title, String author, String isbn) {
        Book book = Book.builder()
                .title(title)
                .author(author)
                .isbn(isbn)
                .totalCopies(5L)
                .availableCopies(5L)
                .build();
        return bookRepository.save(book);
    }
}
