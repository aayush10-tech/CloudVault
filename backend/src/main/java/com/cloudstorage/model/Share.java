package com.cloudstorage.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="shares")
public class Share {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false) private FileEntity file;
    @ManyToOne(optional=false) private User sharedWith;
    @Column(nullable=false) private String role;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    public UUID getId(){return id;} public FileEntity getFile(){return file;} public void setFile(FileEntity v){file=v;}
    public User getSharedWith(){return sharedWith;} public void setSharedWith(User v){sharedWith=v;}
    public String getRole(){return role;} public void setRole(String v){role=v;} public Instant getCreatedAt(){return createdAt;}
}
