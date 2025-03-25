package com.example.foragingapp.controller;

import com.example.foragingapp.model.Tree;
import com.example.foragingapp.repository.TreeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID; 

@CrossOrigin(origins = "http://localhost:5500")
@RestController
@RequestMapping("/api/trees")
public class TreeController {

    @Autowired
    private TreeRepository treeRepository;

    @GetMapping("/list")
    public List<Tree> getAllTrees() {
        return treeRepository.findAll();
    }

    @PostMapping("/add")
    public ResponseEntity<String> addTree(
            @RequestParam("name") String name,
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude,
            @RequestParam("image") MultipartFile imageFile
    ) {
        try {
            // Optional: Save image to local folder (for now)
            String uploadDir = "uploads/";
            File uploadFolder = new File(uploadDir);
            if (!uploadFolder.exists()) uploadFolder.mkdirs();

            String imageName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            File destination = new File(uploadDir + imageName);
            imageFile.transferTo(destination);

            // Save tree info (imageName is optional for now)
            Tree tree = new Tree();
            tree.setName(name);
            tree.setLatitude(latitude);
            tree.setLongitude(longitude);
            tree.setImagePath(imageName); // Only if you added this field to Tree

            treeRepository.save(tree);
            return ResponseEntity.ok("Tree added successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving tree");
        }
    }
}
