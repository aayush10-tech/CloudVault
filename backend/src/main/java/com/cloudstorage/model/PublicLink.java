package com.cloudstorage.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="link_shares")
public class PublicLink {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(nullable=false,unique=true) private String token;
    @ManyToOne(optional=false) private FileEntity file;
    private Instant expiresAt;
    @JsonIgnore private String passwordHash;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    public UUID getId(){return id;} public String getToken(){return token;} public void setToken(String v){token=v;}
    public FileEntity getFile(){return file;} public void setFile(FileEntity v){file=v;} public Instant getExpiresAt(){return expiresAt;}
    public void setExpiresAt(Instant v){expiresAt=v;} public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
}
