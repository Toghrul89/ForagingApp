package com.example.foragingapp.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "trees")
public class Tree {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;
    private String location;
    private String imageUrl; // Store image path

    public Tree() {}

    public Tree(String name, String location, String imageUrl) {
        this.name = name;
        this.location = location;
        this.imageUrl = imageUrl;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getLocation() { return location; }
    public String getImageUrl() { return imageUrl; }

    public void setName(String name) { this.name = name; }
    public void setLocation(String location) { this.location = location; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
