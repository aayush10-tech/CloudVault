package com.cloudstorage.repository;
import com.cloudstorage.model.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface FileRepository extends JpaRepository<FileEntity,UUID>{
 List<FileEntity> findByOwnerIdAndFolderIdAndTrashedFalse(UUID ownerId,UUID folderId);
 List<FileEntity> findByOwnerIdAndFolderIsNullAndTrashedFalse(UUID ownerId);
 List<FileEntity> findByOwnerIdAndTrashedTrue(UUID ownerId);
 @Query("select f from FileEntity f where f.owner.id=:uid and f.trashed=false and lower(f.name) like lower(concat('%',:q,'%'))")
 List<FileEntity> search(@Param("uid") UUID uid,@Param("q") String q);
}
