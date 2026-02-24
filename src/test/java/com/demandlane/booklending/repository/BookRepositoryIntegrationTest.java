package com.demandlane.booklending.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.demandlane.booklending.entity.Book;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BookRepositoryIntegrationTest {

    @Autowired
    private BookRepository bookRepository;
    private final String TEST_TITLE = "Test Book";
    private final String TEST_AUTHOR = "Test Author";
    private final String TEST_ISBN = "1234567890";
    private final Long TEST_AVAILABLE_COPIES = 10L;
    private final Long TEST_TOTAL_COPIES = 10L;
    
    private Book createTestBook() {
        Book book = new Book();
        book.setTitle(TEST_TITLE);
        book.setAuthor(TEST_AUTHOR);
        book.setIsbn(TEST_ISBN);
        book.setAvailableCopies(TEST_AVAILABLE_COPIES);
        book.setTotalCopies(TEST_TOTAL_COPIES);
        return book;
    }
    
    @Test
    void shouldSaveAndFindBook() {
        // Given
        Book book = createTestBook();

        // When
        bookRepository.save(book);
        Book foundBook = bookRepository.findById(book.getId()).orElse(null);

        // Then
        assertNotNull(foundBook);
        assertEquals(book.getTitle(), foundBook.getTitle());
        assertEquals(book.getAuthor(), foundBook.getAuthor());
        assertEquals(book.getAvailableCopies(), foundBook.getAvailableCopies());
        assertEquals(book.getTotalCopies(), foundBook.getTotalCopies());
    }


    @Test
    void shouldUpdateBook() {
        // Given
        Book book = createTestBook();
        bookRepository.save(book);

        // When
        book.setTitle("Updated Title");
        bookRepository.save(book);
        Book updatedBook = bookRepository.findById(book.getId()).orElse(null);

        // Then
        assertNotNull(updatedBook);
        assertEquals("Updated Title", updatedBook.getTitle());
    }

    @Test
    void shouldDeleteBook() {
        // Given
        Book book = createTestBook();
        bookRepository.save(book);
        Long bookId = book.getId();

        // When
        bookRepository.deleteById(bookId);

        // Then
        assertEquals(false, bookRepository.existsById(bookId));
    }

    @Test
    void shouldFindAllBooks() {
        bookRepository.deleteAll(); // Clear existing data from seed
        // Given
        Book book1 = createTestBook();
        Book book2 = createTestBook();
        book2.setIsbn(TEST_ISBN + "2");
        bookRepository.save(book1);
        bookRepository.save(book2);

        // When
        long count = bookRepository.count();

        // Then
        assertEquals(2, count);
    }
}
