package com.demandlane.booklending.exception;

/**
 * Exception thrown when a borrowing rule is violated.
 * For example: max active loans exceeded, overdue loans exist, book unavailable.
 */
public class BorrowingRuleViolationException extends RuntimeException {

    public BorrowingRuleViolationException(String message) {
        super(message);
    }

    public BorrowingRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
