package com.example.foragingapp.repository;

import com.example.foragingapp.model.Tree;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TreeRepository extends JpaRepository<Tree, UUID> {
}
