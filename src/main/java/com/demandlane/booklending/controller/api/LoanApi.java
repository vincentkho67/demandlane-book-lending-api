package com.demandlane.booklending.controller.api;

import java.security.Principal;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.demandlane.booklending.dto.LoanDto;
import com.demandlane.booklending.dto.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Loans", description = "Loan tracking and book borrowing operations")
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/loans")
public interface LoanApi {

    @Operation(
        summary = "Get all loans (Admin only)",
        description = "Retrieve a paginated list of all loans with optional filtering by user or book"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Loans retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content)
    })
    @GetMapping
    ResponseEntity<PageResponse<LoanDto.Response>> findAll(
            @ParameterObject
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @Parameter(description = "Filter by userId or bookId")
            LoanDto.Filter filter);

    @Operation(
        summary = "Get my loans",
        description = "Retrieve your own loan history (both active and returned loans)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Your loans retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    @GetMapping("/self")
    ResponseEntity<PageResponse<LoanDto.Response>> findAllOwned(
            @ParameterObject
            @PageableDefault(size = 10, sort = "id") Pageable pageable,
            @Parameter(hidden = true)
            LoanDto.Filter filter,
            Principal principal);

    @Operation(
        summary = "Get loan by ID",
        description = "Admin can view any loan. Members can only view their own loans - returns 403 if accessing another user's loan."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Loan found"),
        @ApiResponse(responseCode = "403", description = "Access denied - not your loan", content = @Content),
        @ApiResponse(responseCode = "404", description = "Loan not found", content = @Content)
    })
    @GetMapping("/{id}")
    ResponseEntity<LoanDto.Response> findById(
            @Parameter(description = "Loan ID", example = "1")
            @PathVariable Long id);

    @Operation(
        summary = "Create loan manually (Admin only)",
        description = "Create a loan record manually. For regular borrowing, use /borrow endpoint instead."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Loan created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content)
    })
    @PostMapping
    ResponseEntity<LoanDto.Response> create(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Loan details", required = true)
            @RequestBody LoanDto.Request request);

    @Operation(
        summary = "Update loan (Admin only)",
        description = "Update loan information. Supports partial updates."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Loan updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "Loan not found", content = @Content)
    })
    @PutMapping("/{id}")
    ResponseEntity<LoanDto.Response> update(
            @Parameter(description = "Loan ID", example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Updated loan details (partial updates supported)", required = true)
            @RequestBody LoanDto.Request request);

    @Operation(
        summary = "Delete loan (Admin only)",
        description = "Soft delete a loan record"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Loan deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - Admin role required", content = @Content),
        @ApiResponse(responseCode = "404", description = "Loan not found", content = @Content)
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Loan ID", example = "1")
            @PathVariable Long id);

    @Operation(
        summary = "Borrow a book",
        description = """
            Borrow a book with automatic validation of borrowing rules:
            - Maximum active loans limit (default: 5)
            - No overdue loans
            - Book availability

            Due date is automatically calculated (default: 14 days from now)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Book borrowed successfully"),
        @ApiResponse(responseCode = "400", description = "Borrowing rule violated (max loans, overdue, unavailable)", content = @Content),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
        @ApiResponse(responseCode = "404", description = "Book not found", content = @Content)
    })
    @PostMapping("/borrow")
    ResponseEntity<LoanDto.Response> borrowBook(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Book to borrow (only bookId required)", required = true)
            @RequestBody LoanDto.BorrowRequest request,
            Principal principal);

    @Operation(
        summary = "Return a book",
        description = "Mark a borrowed book as returned. This increments the available copies count."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Book returned successfully"),
        @ApiResponse(responseCode = "400", description = "Book already returned", content = @Content),
        @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
        @ApiResponse(responseCode = "404", description = "Loan not found", content = @Content)
    })
    @PutMapping("/return/{id}")
    ResponseEntity<LoanDto.Response> returnBook(
            @Parameter(description = "Loan ID to return", example = "1")
            @PathVariable Long id);
}
