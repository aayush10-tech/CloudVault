package com.cloudstorage.repository;

import com.cloudstorage.model.Star;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StarRepository extends JpaRepository<Star, UUID> {

    boolean existsByUserIdAndFileId(UUID userId, UUID fileId);

    Optional<Star> findByUserIdAndFileId(UUID userId, UUID fileId);

    List<Star> findByUserId(UUID userId);
}