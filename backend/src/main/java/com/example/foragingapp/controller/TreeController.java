package backend.src.main.java.com.example.foragingapp.controller;

import com.example.foragingapp.model.Tree;
import com.example.foragingapp.repository.TreeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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
    public Tree addTree(
            @RequestParam("treeName") String treeName,
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude,
            @RequestParam(value = "treeImage", required = false) MultipartFile treeImage
    ) throws IOException {

        Tree tree = new Tree();
        tree.setTreeName(treeName);
        tree.setLatitude(latitude);
        tree.setLongitude(longitude);

        // Save the image as bytes if provided
        if (treeImage != null && !treeImage.isEmpty()) {
            tree.setImage(treeImage.getBytes());
        }

        return treeRepository.save(tree);
    }
}

