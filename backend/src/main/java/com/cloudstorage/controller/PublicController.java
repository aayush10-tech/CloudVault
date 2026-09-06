package com.cloudstorage.controller;

import com.cloudstorage.model.FileEntity;
import com.cloudstorage.model.PublicLink;
import com.cloudstorage.repository.PublicLinkRepository;
import com.cloudstorage.service.StorageService;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final PublicLinkRepository links;
    private final StorageService storage;
    private final PasswordEncoder encoder;

    public PublicController(
            PublicLinkRepository links,
            StorageService storage,
            PasswordEncoder encoder
    ) {
        this.links = links;
        this.storage = storage;
        this.encoder = encoder;
    }

    // =========================================================
    // PUBLIC LINK INFORMATION
    // =========================================================

    @GetMapping("/{token}")
    public ResponseEntity<?> info(
            @PathVariable String token
    ) {

        PublicLink link = findValidLink(token);

        FileEntity file = link.getFile();

        if (file == null || file.isTrashed()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File no longer exists");
        }

        return ResponseEntity.ok(
                Map.of(
                        "name",
                        file.getName(),

                        "size",
                        file.getSize(),

                        "contentType",
                        Objects.toString(
                                file.getContentType(),
                                "application/octet-stream"
                        ),

                        "protected",
                        link.getPasswordHash() != null,

                        "expiresAt",
                        link.getExpiresAt() == null
                                ? ""
                                : link.getExpiresAt().toString()
                )
        );
    }

    // =========================================================
    // PUBLIC DOWNLOAD
    // =========================================================

    @GetMapping("/{token}/download")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable String token,
            @RequestParam(required = false) String password
    ) throws IOException {

        PublicLink link = findValidLink(token);

        // -----------------------------------------------------
        // Check password
        // -----------------------------------------------------

        if (link.getPasswordHash() != null) {

            if (password == null
                    || password.isBlank()
                    || !encoder.matches(
                            password,
                            link.getPasswordHash()
                    )) {

                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Password required or incorrect password"
                );
            }
        }

        // -----------------------------------------------------
        // Get file
        // -----------------------------------------------------

        FileEntity file = link.getFile();

        String contentType =
                Objects.toString(
                        file.getContentType(),
                        "application/octet-stream"
                );

        String safeFileName =
                file.getName()
                        .replace("\"", "")
                        .replace("\r", "")
                        .replace("\n", "");

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                contentType
                        )
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" +
                                safeFileName +
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
    // FIND + VALIDATE LINK
    // =========================================================

    private PublicLink findValidLink(
            String token
    ) {

        if (token == null || token.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Public link not found"
            );
        }

        PublicLink link =
                links.findByToken(token)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Public link not found"
                                )
                        );

        // -----------------------------------------------------
        // Expiration
        // -----------------------------------------------------

        if (link.getExpiresAt() != null
                && !link.getExpiresAt().isAfter(Instant.now())) {

            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Link expired"
            );
        }

        // -----------------------------------------------------
        // Make sure linked file still exists
        // -----------------------------------------------------

        if (link.getFile() == null) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "File no longer exists"
            );
        }

        return link;
    }
}