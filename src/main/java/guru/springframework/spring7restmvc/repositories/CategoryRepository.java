package guru.springframework.spring7restmvc.repositories;

import guru.springframework.spring7restmvc.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
}
