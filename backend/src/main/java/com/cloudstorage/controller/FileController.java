package com.cloudstorage.controller;

import com.cloudstorage.model.FileEntity;
import com.cloudstorage.model.Folder;
import com.cloudstorage.model.Share;
import com.cloudstorage.model.Star;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.FileRepository;
import com.cloudstorage.repository.FolderRepository;
import com.cloudstorage.repository.ShareRepository;
import com.cloudstorage.repository.StarRepository;
import com.cloudstorage.service.StorageService;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileRepository files;
    private final FolderRepository folders;
    private final StorageService storage;
    private final StarRepository stars;
    private final ShareRepository shares;

    public FileController(
            FileRepository f,
            FolderRepository fo,
            StorageService s,
            StarRepository st,
            ShareRepository sh) {

        files = f;
        folders = fo;
        storage = s;
        stars = st;
        shares = sh;
    }

    // =========================================================
    // CURRENT USER
    // =========================================================

    private User me(
            org.springframework.security.core.Authentication authentication) {

        return (User) authentication.getPrincipal();
    }

    // =========================================================
    // GET FILE
    // =========================================================

    private FileEntity getFile(UUID id) {

        return files.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "File not found"
                        ));
    }

    // =========================================================
    // GET SHARE FOR USER
    // =========================================================

    private Share getShare(
            FileEntity file,
            User user) {

        return shares.findByFileIdAndSharedWithId(
                file.getId(),
                user.getId()
        ).orElse(null);
    }

    // =========================================================
    // CHECK READ ACCESS
    //
    // OWNER  -> allowed
    // EDITOR -> allowed
    // VIEWER -> allowed
    // OTHER  -> denied
    // =========================================================

    private FileEntity readable(
            UUID id,
            User user) {

        FileEntity file = getFile(id);

        // Owner always has access
        if (file.getOwner().getId().equals(user.getId())) {
            return file;
        }

        // Check explicit share
        Share share = getShare(file, user);

        if (share == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have access to this file"
            );
        }

        return file;
    }

    // =========================================================
    // CHECK EDIT ACCESS
    //
    // OWNER  -> allowed
    // EDITOR -> allowed
    // VIEWER -> denied
    // =========================================================

    private FileEntity editable(
            UUID id,
            User user) {

        FileEntity file = getFile(id);

        // Owner
        if (file.getOwner().getId().equals(user.getId())) {
            return file;
        }

        Share share = getShare(file, user);

        if (share == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not have access to this file"
            );
        }

        if (!"EDITOR".equalsIgnoreCase(share.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Viewer permission does not allow this action"
            );
        }

        return file;
    }

    // =========================================================
    // OWNER ONLY
    //
    // Used for permanent deletion and sharing-sensitive actions
    // =========================================================

    private FileEntity owned(
            UUID id,
            User user) {

        FileEntity file = getFile(id);

        if (!file.getOwner().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the file owner can perform this action"
            );
        }

        return file;
    }

    // =========================================================
    // LIST FILES
    // =========================================================

    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam(required = false) UUID folderId,
            @RequestParam(required = false) String q,
            org.springframework.security.core.Authentication authentication) {

        User user = me(authentication);

        List<FileEntity> fileList;

        if (q != null && !q.isBlank()) {

            fileList = files.search(
                    user.getId(),
                    q
            );

        } else if (folderId == null) {

            fileList =
                    files.findByOwnerIdAndFolderIsNullAndTrashedFalse(
                            user.getId()
                    );

        } else {

            fileList =
                    files.findByOwnerIdAndFolderIdAndTrashedFalse(
                            user.getId(),
                            folderId
                    );
        }

        List<Map<String, Object>> result =
                new ArrayList<>();

        for (FileEntity file : fileList) {

            Map<String, Object> item =
                    new LinkedHashMap<>();

            item.put(
                    "id",
                    file.getId()
            );

            item.put(
                    "name",
                    file.getName()
            );

            item.put(
                    "size",
                    file.getSize()
            );

            item.put(
                    "contentType",
                    Objects.toString(
                            file.getContentType(),
                            ""
                    )
            );

            item.put(
                    "folderId",
                    file.getFolder() == null
                            ? null
                            : file.getFolder().getId()
            );

            item.put(
                    "updatedAt",
                    file.getUpdatedAt()
            );

            item.put(
                    "starred",
                    stars.existsByUserIdAndFileId(
                            user.getId(),
                            file.getId()
                    )
            );

            result.add(item);
        }

        return result;
    }

    // =========================================================
    // UPLOAD
    //
    // Only authenticated owner uploads into own drive.
    // =========================================================

    @PostMapping("/upload")
    public Map<String, Object> upload(
            @RequestParam MultipartFile file,
            @RequestParam(required = false) UUID folderId,
            org.springframework.security.core.Authentication authentication)
            throws IOException {

        User user = me(authentication);

        FileEntity entity =
                new FileEntity();

        entity.setOwner(user);

        String originalName =
                file.getOriginalFilename();

        entity.setName(
                Path.of(
                        originalName == null
                                ? "file"
                                : originalName
                ).getFileName().toString()
        );

        entity.setSize(
                file.getSize()
        );

        entity.setContentType(
                file.getContentType()
        );

        entity.setStorageKey(
                storage.save(file)
        );

        if (folderId != null) {

            Folder folder =
                    folders.findById(folderId)
                            .orElseThrow(() ->
                                    new ResponseStatusException(
                                            HttpStatus.NOT_FOUND,
                                            "Folder not found"
                                    ));

            if (!folder.getOwner()
                    .getId()
                    .equals(user.getId())) {

                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You do not have access to this folder"
                );
            }

            entity.setFolder(folder);
        }

        files.save(entity);

        Map<String, Object> result =
                new LinkedHashMap<>();

        result.put(
                "id",
                entity.getId()
        );

        result.put(
                "name",
                entity.getName()
        );

        return result;
    }

    // =========================================================
    // DOWNLOAD
    //
    // Owner / Editor / Viewer
    // =========================================================

    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable UUID id,
            org.springframework.security.core.Authentication authentication)
            throws IOException {

        FileEntity file =
                readable(
                        id,
                        me(authentication)
                );

        String contentType =
                Objects.toString(
                        file.getContentType(),
                        "application/octet-stream"
                );

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                contentType
                        )
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                file.getName()
                                        .replace("\"", "") +
                                "\""
                )
                .body(
                        new InputStreamResource(
                                storage.open(
                                        file.getStorageKey()
                                )
                        )
                );
    }

    // =========================================================
    // MOVE TO TRASH
    //
    // Owner / Editor
    // Viewer -> denied
    // =========================================================

    @DeleteMapping("/{id}")
    public void trash(
            @PathVariable UUID id,
            org.springframework.security.core.Authentication authentication) {

        FileEntity file =
                editable(
                        id,
                        me(authentication)
                );

        file.setTrashed(true);
        file.touch();

        files.save(file);
    }

    // =========================================================
    // RESTORE
    //
    // Owner / Editor
    // =========================================================

    @PostMapping("/{id}/restore")
    public void restore(
            @PathVariable UUID id,
            org.springframework.security.core.Authentication authentication) {

        FileEntity file =
                editable(
                        id,
                        me(authentication)
                );

        file.setTrashed(false);
        file.touch();

        files.save(file);
    }

    // =========================================================
    // PERMANENT DELETE
    //
    // OWNER ONLY
    // =========================================================

    @DeleteMapping("/{id}/permanent")
    public void permanent(
            @PathVariable UUID id,
            org.springframework.security.core.Authentication authentication)
            throws IOException {

        FileEntity file =
                owned(
                        id,
                        me(authentication)
                );

        storage.delete(
                file.getStorageKey()
        );

        files.delete(file);
    }

    // =========================================================
    // RENAME FILE
    // Owner / Editor
    // =========================================================

    @PatchMapping("/{id}")
    public Map<String, Object> rename(
            @PathVariable UUID id,
            @RequestParam String name,
            org.springframework.security.core.Authentication authentication) {

        FileEntity file = editable(id, me(authentication));

        String cleanName = name == null ? "" : name.trim();

        if (cleanName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File name cannot be empty"
            );
        }

        String safeName = Path.of(cleanName).getFileName().toString();
        if (!safeName.equals(cleanName)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid file name"
            );
        }

        file.setName(safeName);
        file.touch();
        files.save(file);

        return Map.of(
                "id", file.getId(),
                "name", file.getName()
        );
    }

    // =========================================================
    // MOVE FILE
    // Owner / Editor
    // =========================================================

    @PatchMapping("/{id}/move")
    public Map<String, Object> move(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID folderId,
            org.springframework.security.core.Authentication authentication) {

        User user = me(authentication);
        FileEntity file = editable(id, user);

        if (folderId == null) {
            file.setFolder(null);
        } else {
            Folder target = folders.findById(folderId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Destination folder not found"
                            ));

            if (!target.getOwner().getId().equals(user.getId())
                    || target.isTrashed()) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You cannot move the file into that folder"
                );
            }

            file.setFolder(target);
        }

        file.touch();
        files.save(file);

        return Map.of(
                "id", file.getId(),
                "folderId",
                file.getFolder() == null ? "" : file.getFolder().getId()
        );
    }

    // =========================================================
    // STAR
    //
    // Personal action.
    // User can star a file they can read.
    // =========================================================

    @PostMapping("/{id}/star")
    public Map<String, Boolean> star(
            @PathVariable UUID id,
            org.springframework.security.core.Authentication authentication) {

        User user =
                me(authentication);

        FileEntity file =
                readable(
                        id,
                        user
                );

        if (stars.existsByUserIdAndFileId(
                user.getId(),
                id
        )) {

            stars.delete(
                    stars.findByUserIdAndFileId(
                            user.getId(),
                            id
                    ).get()
            );

            return Map.of(
                    "starred",
                    false
            );
        }

        Star star =
                new Star();

        star.setUser(user);
        star.setFile(file);

        stars.save(star);

        return Map.of(
                "starred",
                true
        );
    }

    // =========================================================
    // TRASH LIST
    //
    // Owner's trash
    // =========================================================

    @GetMapping("/trash")
    public List<FileEntity> trash(
            org.springframework.security.core.Authentication authentication) {

        return files.findByOwnerIdAndTrashedTrue(
                me(authentication).getId()
        );
    }

    // =========================================================
    // STARRED LIST
    //
    // Personal starred files
    // =========================================================

    @GetMapping("/starred")
    public List<FileEntity> starred(
            org.springframework.security.core.Authentication authentication) {

        return stars.findByUserId(
                        me(authentication).getId()
                )
                .stream()
                .map(Star::getFile)
                .filter(file -> !file.isTrashed())
                .toList();
    }
}