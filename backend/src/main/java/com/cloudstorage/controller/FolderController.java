package com.cloudstorage.controller;

import com.cloudstorage.model.Folder;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.FolderRepository;
import com.cloudstorage.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/folders")
public class FolderController {

    private final FolderRepository folders;
    private final UserRepository users;

    public FolderController(
            FolderRepository folders,
            UserRepository users) {

        this.folders = folders;
        this.users = users;
    }

    private User me(
            org.springframework.security.core.Authentication authentication) {

        return (User) authentication.getPrincipal();
    }

    private Folder owned(
            UUID id,
            User user) {

        Folder folder = folders.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Folder not found"
                        ));

        if (!folder.getOwner().getId().equals(user.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied"
            );
        }

        return folder;
    }

    private Map<String, Object> response(Folder folder) {

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("id", folder.getId());
        result.put("name", folder.getName());

        result.put(
                "parentId",
                folder.getParent() == null
                        ? null
                        : folder.getParent().getId()
        );

        result.put("trashed", folder.isTrashed());
        result.put("createdAt", folder.getCreatedAt());

        return result;
    }

    @PostMapping
    public Map<String, Object> create(
            @RequestParam String name,
            @RequestParam(required = false) UUID parentId,
            org.springframework.security.core.Authentication authentication) {

        User user = me(authentication);

        String folderName = name == null
                ? ""
                : name.trim();

        if (folderName.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Folder name cannot be empty"
            );
        }

        Folder folder = new Folder();

        folder.setName(folderName);
        folder.setOwner(user);

        if (parentId != null) {

            Folder parent = owned(parentId, user);

            if (parent.isTrashed()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot create folder inside a trashed folder"
                );
            }

            folder.setParent(parent);
        }

        Folder saved = folders.save(folder);

        return response(saved);
    }

    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam(required = false) UUID parentId,
            org.springframework.security.core.Authentication authentication) {

        User user = me(authentication);

        List<Folder> folderList;

        if (parentId == null) {

            folderList =
                    folders.findByOwnerIdAndParentIsNullAndTrashedFalse(
                            user.getId()
                    );

        } else {

            Folder parent = owned(parentId, user);

            if (parent.isTrashed()) {

                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Folder not found"
                );
            }

            folderList =
                    folders.findByOwnerIdAndParentIdAndTrashedFalse(
                            user.getId(),
                            parentId
                    );
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Folder folder : folderList) {
            result.add(response(folder));
        }

        return result;
    }

    @PatchMapping("/{id}")
    public Map<String, Object> rename(
            @PathVariable UUID id,
            @RequestParam String name,
            org.springframework.security.core.Authentication authentication) {

        User user = me(authentication);

        Folder folder = owned(id, user);

        String folderName = name == null
                ? ""
                : name.trim();

        if (folderName.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Folder name cannot be empty"
            );
        }

        folder.setName(folderName);

        Folder saved = folders.save(folder);

        return response(saved);
    }

    @DeleteMapping("/{id}")
    public void trash(
            @PathVariable UUID id,
            org.springframework.security.core.Authentication authentication) {

        User user = me(authentication);

        Folder folder = owned(id, user);

        folder.setTrashed(true);

        folders.save(folder);
    }

    @PostMapping("/{id}/restore")
    public void restore(
            @PathVariable UUID id,
            org.springframework.security.core.Authentication authentication) {

        User user = me(authentication);
        Folder folder = owned(id, user);

        folder.setTrashed(false);
        folder.touch();
        folders.save(folder);
    }

    @DeleteMapping("/{id}/permanent")
    public void permanent(
            @PathVariable UUID id,
            org.springframework.security.core.Authentication authentication) {

        User user = me(authentication);
        Folder folder = owned(id, user);

        if (!folder.isTrashed()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only trashed folders can be permanently deleted"
            );
        }

        folders.delete(folder);
    }
}
