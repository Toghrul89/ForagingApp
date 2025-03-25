package com.example.foragingapp.controller;

import com.example.foragingapp.model.Tree;
import com.example.foragingapp.repository.TreeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public Tree addTree(@RequestBody Tree tree) {
        return treeRepository.save(tree);
    }
}

