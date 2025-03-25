package com.example.foragingapp.controller;

import com.example.foragingapp.model.Tree;
import com.example.foragingapp.repository.TreeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trees")
@CrossOrigin(origins = "http://localhost:5500")  // Allow requests from frontend
public class TreeController {

    @Autowired
    private TreeRepository treeRepository;

    // Get all submitted trees
    @GetMapping("/list")
    public List<Tree> getAllTrees() {
        return treeRepository.findAll();
    }

    // Add a new tree with image
    @PostMapping("/add")
    public ResponseEntity<String> addTree(
            @RequestParam("name") String name,
            @RequestParam("location") String location,
            @RequestParam(value = "image", required = false) MultipartFile image
    ) {
        try {
            String imageUrl = "";

            // Handle image upload if provided
            if (image != null && !image.isEmpty()) {
                String uploadDir = "uploads/";
                File uploadFolder = new File(uploadDir);
                if (!uploadFolder.exists()) uploadFolder.mkdirs();

                String imageName = UUID.randomUUID() + "_" + image.getOriginalFilename();
                File destination = new File(uploadDir + imageName);
                image.transferTo(destination);

                imageUrl = "/uploads/" + imageName; // store relative path
            }

            // Save tree info
            Tree tree = new Tree();
            tree.setName(name);
            tree.setLocation(location);
            tree.setImageUrl(imageUrl);

            treeRepository.save(tree);
            return ResponseEntity.ok("Tree added successfully");

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to save tree or upload image");
        }
    }
}
