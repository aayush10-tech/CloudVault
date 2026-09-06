package com.cloudstorage.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "files")
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @JsonIgnore
    private String storageKey;

    @Column(nullable = false)
    private long size;

    private String contentType;

    @ManyToOne(optional = false)
    @JsonIgnore
    private User owner;

    @ManyToOne
    @JsonIgnore
    private Folder folder;

    @Column(nullable = false)
    private boolean trashed = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String v) {
        name = v;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String v) {
        storageKey = v;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long v) {
        size = v;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String v) {
        contentType = v;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User v) {
        owner = v;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder v) {
        folder = v;
    }

    public boolean isTrashed() {
        return trashed;
    }

    public void setTrashed(boolean v) {
        trashed = v;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        updatedAt = Instant.now();
    }
}