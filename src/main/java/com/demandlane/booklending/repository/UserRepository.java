package com.demandlane.booklending.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.demandlane.booklending.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends BaseRepository<User> {

    @Query("SELECT u FROM User u WHERE u.email = ?1 AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(String email);
}
