package com.cloudstorage.repository;

import com.cloudstorage.model.PublicLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicLinkRepository extends JpaRepository<PublicLink, UUID> {

    Optional<PublicLink> findByToken(String token);

    List<PublicLink> findByFileId(UUID fileId);
}
