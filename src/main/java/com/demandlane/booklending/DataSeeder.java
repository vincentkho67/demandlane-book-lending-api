package com.demandlane.booklending;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.demandlane.booklending.entity.Book;
import com.demandlane.booklending.entity.Role;
import com.demandlane.booklending.entity.User;
import com.demandlane.booklending.repository.BookRepository;
import com.demandlane.booklending.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void seedData() {
        seedUsers();
        seedBooks();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }

        String password = passwordEncoder.encode("password123");

        // Create Admin user
        User admin = User.builder()
                .name("Admin User")
                .email("admin@demandlane.com")
                .password(password)
                .role(Role.ADMIN)
                .build();
        userRepository.save(admin);

        // Create regular member users
        String[] memberNames = {
                "John Doe",
                "Jane Smith",
                "Bob Johnson",
                "Alice Williams",
                "Charlie Brown"
        };

        String[] memberEmails = {
                "a@demandlane.com",
                "b@demandlane.com",
                "c@demandlane.com",
                "d@demandlane.com",
                "e@demandlane.com"
        };

        for (int i = 0; i < memberNames.length; i++) {
            User user = User.builder()
                    .name(memberNames[i])
                    .email(memberEmails[i])
                    .password(password)
                    .role(Role.MEMBER)
                    .build();
            userRepository.save(user);
        }

        System.out.println("Database seeded with 6 test users. All users have password: password123");
    }

    private void seedBooks() {
        if (bookRepository.count() > 0) {
            return;
        }

        // Seed 10 books
        String[][] books = {
                {"The Great Gatsby",           "F. Scott Fitzgerald", "978-0743273565"},
                {"To Kill a Mockingbird",       "Harper Lee",          "978-0061935466"},
                {"1984",                        "George Orwell",       "978-0451524935"},
                {"Pride and Prejudice",         "Jane Austen",         "978-0141439518"},
                {"The Catcher in the Rye",      "J.D. Salinger",       "978-0316769174"},
                {"Brave New World",             "Aldous Huxley",       "978-0060850524"},
                {"The Hobbit",                  "J.R.R. Tolkien",      "978-0547928227"},
                {"Harry Potter and the Sorcerer's Stone", "J.K. Rowling", "978-0439708180"},
                {"The Da Vinci Code",           "Dan Brown",           "978-0307474278"},
                {"The Alchemist",               "Paulo Coelho",        "978-0062315007"}
        };

        for (String[] bookData : books) {
            Book book = Book.builder()
                    .title(bookData[0])
                    .author(bookData[1])
                    .isbn(bookData[2])
                    .totalCopies(5L)
                    .availableCopies(5L)
                    .build();
            bookRepository.save(book);
        }

        System.out.println("Database seeded with 10 books.");
    }
}
