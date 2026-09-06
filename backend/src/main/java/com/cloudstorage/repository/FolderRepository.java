package com.cloudstorage.repository;

import com.cloudstorage.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FolderRepository extends JpaRepository<Folder, UUID> {

    List<Folder> findByOwnerIdAndParentIsNullAndTrashedFalse(
            UUID ownerId
    );

    List<Folder> findByOwnerIdAndParentIdAndTrashedFalse(
            UUID ownerId,
            UUID parentId
    );

    List<Folder> findByOwnerIdAndTrashedTrue(
            UUID ownerId
    );
}