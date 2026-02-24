package com.demandlane.booklending.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration properties for library settings.
 *
 * Properties:
 * - library.max-active-loans: Maximum number of active loans per member (default: 5)
 * - library.loan-duration-days: Loan duration in days (default: 14)
 */
@Component
@ConfigurationProperties(prefix = "library")
@Data
public class LibraryProperties {

    /**
     * Maximum number of active loans a member can have at once.
     */
    private int maxActiveLoans = 5;

    /**
     * Number of days a book can be borrowed for.
     */
    private int loanDurationDays = 14;
}
