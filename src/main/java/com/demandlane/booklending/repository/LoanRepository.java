package com.demandlane.booklending.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.demandlane.booklending.entity.Loan;

public interface LoanRepository extends BaseRepository<Loan> {
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.user.id = :userId " +
           "AND l.returnedAt IS NULL AND l.deletedAt IS NULL")
    long countActiveLoans(@Param("userId") Long userId);

    @Query("SELECT COUNT(l) > 0 FROM Loan l WHERE l.user.id = :userId " +
           "AND l.returnedAt IS NULL AND l.dueDate < :now AND l.deletedAt IS NULL")
    boolean hasOverdueLoans(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
