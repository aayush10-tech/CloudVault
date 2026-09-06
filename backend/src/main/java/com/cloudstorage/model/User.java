package com.cloudstorage.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="users")
public class User {
    @Id @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;
    @Column(nullable=false, unique=true) private String email;
    @Column(nullable=false) @JsonIgnore private String passwordHash;
    @Column(nullable=false) private String name;
    @Column(nullable=false) private String role = "USER";
    @Column(nullable=false) private Instant createdAt = Instant.now();

    public UUID getId(){return id;} public String getEmail(){return email;}
    public void setEmail(String v){email=v;} public String getPasswordHash(){return passwordHash;}
    public void setPasswordHash(String v){passwordHash=v;} public String getName(){return name;}
    public void setName(String v){name=v;} public String getRole(){return role;}
    public Instant getCreatedAt(){return createdAt;}
}
