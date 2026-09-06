package com.cloudstorage.repository;

import com.cloudstorage.model.Share;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShareRepository
        extends JpaRepository<Share, UUID> {

    Optional<Share> findByFileIdAndSharedWithId(
            UUID fileId,
            UUID userId
    );

    List<Share> findBySharedWithId(
            UUID userId
    );

    List<Share> findByFileId(
            UUID fileId
    );
}