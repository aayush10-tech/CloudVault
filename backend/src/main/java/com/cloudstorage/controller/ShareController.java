package com.cloudstorage.controller;

import com.cloudstorage.model.FileEntity;
import com.cloudstorage.model.PublicLink;
import com.cloudstorage.model.Share;
import com.cloudstorage.model.User;
import com.cloudstorage.repository.FileRepository;
import com.cloudstorage.repository.PublicLinkRepository;
import com.cloudstorage.repository.ShareRepository;
import com.cloudstorage.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ShareController {

    private final ShareRepository shares;
    private final FileRepository files;
    private final UserRepository users;
    private final PublicLinkRepository links;
    private final PasswordEncoder encoder;

    private final SecureRandom secureRandom = new SecureRandom();

    public ShareController(
            ShareRepository shares,
            FileRepository files,
            UserRepository users,
            PublicLinkRepository links,
            PasswordEncoder encoder
    ) {
        this.shares = shares;
        this.files = files;
        this.users = users;
        this.links = links;
        this.encoder = encoder;
    }

    // =========================================================
    // CURRENT USER
    // =========================================================

    private User me(
            org.springframework.security.core.Authentication authentication
    ) {
        return (User) authentication.getPrincipal();
    }

    // =========================================================
    // GET FILE
    // =========================================================

    private FileEntity getFile(UUID fileId) {

        return files.findById(fileId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "File not found"
                        )
                );
    }

    // =========================================================
    // CHECK OWNER
    // =========================================================

    private FileEntity ownedFile(
            UUID fileId,
            User user
    ) {

        FileEntity file = getFile(fileId);

        if (!file.getOwner().getId().equals(user.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the file owner can manage sharing"
            );
        }

        return file;
    }

    // =========================================================
    // CREATE / UPDATE USER SHARE
    // =========================================================

    @PostMapping("/shares")
    public Share share(
            @RequestParam UUID fileId,
            @RequestParam String email,
            @RequestParam String role,
            org.springframework.security.core.Authentication authentication
    ) {

        User owner = me(authentication);

        FileEntity file = ownedFile(fileId, owner);

        if (email == null || email.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Recipient email is required"
            );
        }

        String normalizedEmail =
                email.trim().toLowerCase();

        if (normalizedEmail.equals(
                owner.getEmail().toLowerCase()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot share a file with yourself"
            );
        }

        if (role == null || role.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role is required"
            );
        }

        String normalizedRole =
                role.trim().toUpperCase();

        if (!normalizedRole.equals("VIEWER")
                && !normalizedRole.equals("EDITOR")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role must be VIEWER or EDITOR"
            );
        }

        User target = users
                .findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Recipient user not found"
                        )
                );

        Share share = shares
                .findByFileIdAndSharedWithId(
                        fileId,
                        target.getId()
                )
                .orElseGet(Share::new);

        share.setFile(file);
        share.setSharedWith(target);
        share.setRole(normalizedRole);

        return shares.save(share);
    }

    // =========================================================
    // GET FILES SHARED WITH CURRENT USER
    // =========================================================

    @GetMapping("/shares")
    public List<Share> shared(
            org.springframework.security.core.Authentication authentication
    ) {

        User user = me(authentication);

        return shares.findBySharedWithId(
                user.getId()
        );
    }

    // =========================================================
    // GET ALL SHARES FOR A FILE
    // OWNER ONLY
    // =========================================================

    @GetMapping("/shares/file/{fileId}")
    public List<Share> fileShares(
            @PathVariable UUID fileId,
            org.springframework.security.core.Authentication authentication
    ) {

        User owner = me(authentication);

        ownedFile(fileId, owner);

        return shares.findByFileId(fileId);
    }

    // =========================================================
    // GET CURRENT USER'S ROLE
    // =========================================================

    @GetMapping("/shares/file/{fileId}/permission")
    public Map<String, String> permission(
            @PathVariable UUID fileId,
            org.springframework.security.core.Authentication authentication
    ) {

        User user = me(authentication);

        FileEntity file = getFile(fileId);

        if (file.getOwner().getId().equals(user.getId())) {

            return Map.of(
                    "role",
                    "OWNER"
            );
        }

        Share share = shares
                .findByFileIdAndSharedWithId(
                        fileId,
                        user.getId()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "You do not have access to this file"
                        )
                );

        return Map.of(
                "role",
                share.getRole()
        );
    }

    // =========================================================
    // REMOVE USER SHARE
    // OWNER ONLY
    // =========================================================

    @DeleteMapping("/shares/{fileId}/{userId}")
    public void removeShare(
            @PathVariable UUID fileId,
            @PathVariable UUID userId,
            org.springframework.security.core.Authentication authentication
    ) {

        User owner = me(authentication);

        ownedFile(fileId, owner);

        Share share = shares
                .findByFileIdAndSharedWithId(
                        fileId,
                        userId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Share not found"
                        )
                );

        shares.delete(share);
    }

    // =========================================================
    // CREATE PUBLIC SHARE LINK
    //
    // POST /api/public-links
    //
    // Required:
    // fileId
    //
    // Optional:
    // expiresInHours
    // password
    // =========================================================

    @PostMapping("/public-links")
    public Map<String, Object> createPublicLink(
            @RequestParam UUID fileId,
            @RequestParam(required = false) Long expiresInHours,
            @RequestParam(required = false) String password,
            org.springframework.security.core.Authentication authentication
    ) {

        User owner = me(authentication);

        FileEntity file = ownedFile(fileId, owner);

        // -----------------------------------------------------
        // Validate expiration
        // -----------------------------------------------------

        if (expiresInHours != null && expiresInHours <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Expiration must be greater than 0 hours"
            );
        }

        // -----------------------------------------------------
        // Create secure random token
        // -----------------------------------------------------

        String token = generateToken();

        // -----------------------------------------------------
        // Create link
        // -----------------------------------------------------

        PublicLink link = new PublicLink();

        link.setToken(token);
        link.setFile(file);

        // -----------------------------------------------------
        // Optional expiration
        // -----------------------------------------------------

        if (expiresInHours != null) {

            link.setExpiresAt(
                    Instant.now().plusSeconds(
                            expiresInHours * 60L * 60L
                    )
            );
        }

        // -----------------------------------------------------
        // Optional password
        // -----------------------------------------------------

        if (password != null && !password.isBlank()) {

            if (password.length() < 4) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Public link password must contain at least 4 characters"
                );
            }

            link.setPasswordHash(
                    encoder.encode(password)
            );
        }

        PublicLink saved = links.save(link);

        // -----------------------------------------------------
        // Return public URL
        // -----------------------------------------------------

        String publicUrl =
                "/api/public/" + saved.getToken();

        return Map.of(
                "id", saved.getId(),
                "token", saved.getToken(),
                "url", publicUrl,
                "fileId", file.getId(),
                "fileName", file.getName(),
                "expiresAt",
                saved.getExpiresAt() == null
                        ? ""
                        : saved.getExpiresAt().toString(),
                "protected",
                saved.getPasswordHash() != null
        );
    }

    // =========================================================
    // LIST PUBLIC LINKS FOR A FILE
    // OWNER ONLY
    // =========================================================

    @GetMapping("/public-links/file/{fileId}")
    public List<PublicLink> getPublicLinks(
            @PathVariable UUID fileId,
            org.springframework.security.core.Authentication authentication
    ) {

        User owner = me(authentication);

        FileEntity file = ownedFile(fileId, owner);

        return links.findByFileId(file.getId());
    }

    // =========================================================
    // DELETE PUBLIC LINK
    // OWNER ONLY
    // =========================================================

    @DeleteMapping("/public-links/{id}")
    public void deletePublicLink(
            @PathVariable UUID id,
            org.springframework.security.core.Authentication authentication
    ) {

        User owner = me(authentication);

        PublicLink link = links.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Public link not found"
                        )
                );

        if (!link.getFile().getOwner().getId().equals(owner.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the file owner can delete this public link"
            );
        }

        links.delete(link);
    }

    // =========================================================
    // TOKEN GENERATOR
    // =========================================================

    private String generateToken() {

        byte[] bytes = new byte[32];

        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}