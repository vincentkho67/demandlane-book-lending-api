package com.demandlane.booklending.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import com.demandlane.booklending.entity.BaseEntity;

@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity> extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {

    @Query("SELECT e FROM #{#entityName} e WHERE e.deletedAt IS NULL")
    Page<T> findAllActive(Pageable pageable);

    @Query("SELECT e FROM #{#entityName} e WHERE e.id = ?1 AND e.deletedAt IS NULL")
    Optional<T> findActiveById(Long id);
}
