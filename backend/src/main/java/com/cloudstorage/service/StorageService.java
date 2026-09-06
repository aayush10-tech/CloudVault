package com.cloudstorage.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;

@Service
public class StorageService {

    private final Path localRoot;
    private final String type;
    private final String supabaseUrl;
    private final String bucket;
    private final String serviceKey;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public StorageService(
            @Value("${app.storage.root:./storage}") String root,
            @Value("${app.storage.type:local}") String type,
            @Value("${app.storage.supabase.url:}") String supabaseUrl,
            @Value("${app.storage.supabase.bucket:cloudvault}") String bucket,
            @Value("${app.storage.supabase.service-key:}") String serviceKey
    ) {
        this.localRoot = Paths.get(root).toAbsolutePath().normalize();
        this.type = type == null ? "local" : type.trim().toLowerCase();
        this.supabaseUrl = trimTrailingSlash(supabaseUrl);
        this.bucket = bucket;
        this.serviceKey = serviceKey;

        if ("local".equals(this.type)) {
            try {
                Files.createDirectories(this.localRoot);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create local storage directory", e);
            }
        }

        if ("supabase".equals(this.type)
                && (this.supabaseUrl.isBlank() || this.bucket.isBlank() || this.serviceKey.isBlank())) {
            throw new IllegalStateException(
                    "Supabase storage requires SUPABASE_URL, SUPABASE_STORAGE_BUCKET and SUPABASE_SERVICE_ROLE_KEY"
            );
        }
    }

    public String save(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String safeName = Path.of(original).getFileName().toString();
        String key = UUID.randomUUID() + "-" + safeName;

        if ("supabase".equals(type)) {
            uploadToSupabase(key, file);
        } else {
            Files.copy(
                    file.getInputStream(),
                    localRoot.resolve(key),
                    StandardCopyOption.REPLACE_EXISTING
            );
        }

        return key;
    }

    public InputStream open(String key) throws IOException {
        String safeKey = safeKey(key);

        if ("supabase".equals(type)) {
            HttpRequest request = authenticatedRequest(
                    objectUrl(safeKey)
            ).GET().build();

            HttpResponse<InputStream> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Storage download interrupted", e);
            }

            if (response.statusCode() / 100 != 2) {
                try (InputStream ignored = response.body()) {
                    throw new IOException("Supabase storage download failed: HTTP " + response.statusCode());
                }
            }

            return response.body();
        }

        return Files.newInputStream(localRoot.resolve(safeKey));
    }

    public void delete(String key) throws IOException {
        String safeKey = safeKey(key);

        if ("supabase".equals(type)) {
            HttpRequest request = authenticatedRequest(
                    objectUrl(safeKey)
            ).DELETE().build();

            HttpResponse<String> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Storage delete interrupted", e);
            }

            if (response.statusCode() / 100 != 2 && response.statusCode() != 404) {
                throw new IOException(
                        "Supabase storage delete failed: HTTP " + response.statusCode()
                );
            }
            return;
        }

        Files.deleteIfExists(localRoot.resolve(safeKey));
    }

    private void uploadToSupabase(String key, MultipartFile file) throws IOException {
        HttpRequest.Builder builder = authenticatedRequest(objectUrl(key))
                .header("Content-Type",
                        file.getContentType() == null
                                ? "application/octet-stream"
                                : file.getContentType())
                .header("x-upsert", "false")
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> {
                    try {
                        return file.getInputStream();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));

        HttpResponse<String> response;
        try {
            response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Supabase storage upload interrupted", e);
        }

        if (response.statusCode() / 100 != 2) {
            throw new IOException(
                    "Supabase storage upload failed: HTTP "
                            + response.statusCode()
                            + " "
                            + response.body()
            );
        }
    }

    private HttpRequest.Builder authenticatedRequest(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + serviceKey)
                .header("apikey", serviceKey);
    }

    private String objectUrl(String key) {
        return supabaseUrl
                + "/storage/v1/object/"
                + encode(bucket)
                + "/"
                + encode(key);
    }

    private String safeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Invalid storage key");
        }

        String safe = Path.of(key).getFileName().toString();

        if (!safe.equals(key)) {
            throw new IllegalArgumentException("Invalid storage key");
        }

        return safe;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private String trimTrailingSlash(String value) {
        if (value == null) return "";
        return value.replaceAll("/+$", "");
    }
}
