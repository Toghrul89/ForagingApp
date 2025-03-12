package com.example.foragingapp.controller;

import com.example.foragingapp.model.Tree;
import com.example.foragingapp.repository.TreeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trees")
public class TreeController {

    private static final String UPLOAD_DIR = "uploads/";

    @Autowired
    private TreeRepository treeRepository;

    @PostMapping("/add")
    public ResponseEntity<String> addTree(@RequestParam("name") String name,
                                          @RequestParam("location") String location,
                                          @RequestParam("image") MultipartFile file) {
        try {
            // Ensure upload directory exists
            Files.createDirectories(Paths.get(UPLOAD_DIR));

            // Save image to server
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Save tree details to the database
            Tree tree = new Tree(name, location, "/uploads/" + fileName);
            treeRepository.save(tree);

            return ResponseEntity.ok("Tree added successfully!");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading image.");
        }
    }

    @GetMapping("/list")
    public List<Tree> getAllTrees() {
        return treeRepository.findAll();
    }
}
