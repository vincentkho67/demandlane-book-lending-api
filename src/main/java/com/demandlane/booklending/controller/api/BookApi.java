package com.demandlane.booklending.controller.api;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.demandlane.booklending.dto.BookDto;
import com.demandlane.booklending.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Books", description = "Book catalog management (Read: All authenticated users, Write: Admin only)")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/books")
public interface BookApi {

    @Operation(
        summary = "Get all books",
        description = "Retrieve a paginated list of books with optional filtering by title, author, or ISBN"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Books retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @GetMapping
    ResponseEntity<PageResponse<BookDto.Response>> findAll(
            @ParameterObject
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @Parameter(description = "Filter by title, author, or ISBN")
            BookDto.Filter filter);

    @Operation(
        summary = "Get book by ID",
        description = "Retrieve a specific book by its ID with availability information"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Book found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
        @ApiResponse(responseCode = "404", description = "Book not found", content = @Content)
    })
    @GetMapping("/{id}")
    ResponseEntity<BookDto.Response> findById(
            @Parameter(description = "Book ID", example = "1")
            @PathVariable Long id);

    @Operation(
        summary = "Add new book (Admin only)",
        description = "Add a new book to the catalog with initial copy counts"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Book created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content)
    })
    @PostMapping
    ResponseEntity<BookDto.Response> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Book details", required = true)
            @RequestBody BookDto.Request request);

    @Operation(
        summary = "Update book (Admin only)",
        description = "Update book information. Supports partial updates - only provided fields will be updated."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Book updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "Book not found", content = @Content)
    })
    @PutMapping("/{id}")
    ResponseEntity<BookDto.Response> update(
            @Parameter(description = "Book ID", example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated book details (partial updates supported)", required = true)
            @RequestBody BookDto.Request request);

    @Operation(
        summary = "Delete book (Admin only)",
        description = "Soft delete a book. The book is marked as deleted but not removed from the database."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Book deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "Book not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Book ID", example = "1")
            @PathVariable Long id);
}
