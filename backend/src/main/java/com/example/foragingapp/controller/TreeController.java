package com.example.foragingapp.controller;

import com.example.foragingapp.model.Tree;
import com.example.foragingapp.repository.TreeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/trees")
@CrossOrigin(origins = "*")
public class TreeController {

    @Autowired
    private TreeRepository treeRepository;

    @GetMapping("/list")
    public List<Tree> getAllTrees() {
        return treeRepository.findAll();
    }

    @PostMapping("/add")
    public Tree addTree(@RequestParam("treeName") String treeName,
                        @RequestParam("latitude") String latitude,
                        @RequestParam("longitude") String longitude,
                        @RequestParam(value = "treeImage", required = false) MultipartFile treeImage) throws IOException {

        Tree tree = new Tree();
        tree.setName(treeName);
        tree.setLocation(latitude + ", " + longitude);

        if (treeImage != null && !treeImage.isEmpty()) {
            String filename = UUID.randomUUID().toString() + "_" + treeImage.getOriginalFilename();
            String uploadDir = System.getProperty("user.dir") + "/uploads";
            File uploadPath = new File(uploadDir);
            if (!uploadPath.exists()) {
                uploadPath.mkdirs();
            }
            File destination = new File(uploadPath, filename);
            treeImage.transferTo(destination);
            tree.setImageUrl("/" + uploadDir + "/" + filename); // e.g., "/uploads/abc.jpg"
        } else {
            tree.setImageUrl(""); // Optional: empty or placeholder
        }

        return treeRepository.save(tree);
    }
}
