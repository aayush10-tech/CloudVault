package com.cloudstorage.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="stars", uniqueConstraints=@UniqueConstraint(columnNames={"user_id","file_id"}))
public class Star {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(optional=false) private User user;
    @ManyToOne(optional=false) private FileEntity file;
    public UUID getId(){return id;} public User getUser(){return user;} public void setUser(User v){user=v;}
    public FileEntity getFile(){return file;} public void setFile(FileEntity v){file=v;}
}
