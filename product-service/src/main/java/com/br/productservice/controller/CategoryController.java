package com.br.productservice.controller;


import com.br.productservice.enums.RoleEnum;
import com.br.productservice.service.CategoryService;
import com.br.productservice.service.dto.CategoryResponse;
import com.br.productservice.service.dto.CreateCategoryDTO;
import com.br.productservice.service.dto.UpdateCategoryDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * List all categories in the database
     * @return list of CategoryResponse
     */
    @GetMapping()
    public ResponseEntity<List<CategoryResponse>> listAllCategories() {
        log.info("REST - Request to list all categories");
        // TODO - If its admin it will list all including inactive ones
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.listAll(true));
    }

    /**
     * List details from one category by its id
     * @param id Category id
     * @return ResponseEntity of CategoryResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> listOneCategory(@PathVariable UUID id) {
        log.info("REST - Request to detail one category by its id");
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.findById(id));
    }

    @PostMapping()
    public ResponseEntity<?> create(@RequestHeader("X-User-Role") String role, @Valid @RequestBody CreateCategoryDTO categoryDTO) {
        log.info("REST - Request to create a new category");
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createNewCategory(categoryDTO));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@RequestHeader("X-User-Role") String role, @PathVariable UUID id, @RequestBody UpdateCategoryDTO categoryDTO) {
        log.info("REST - Request to update category");
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        return ResponseEntity.status(HttpStatus.OK).body(categoryService.updateCategory(id, categoryDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deactivate(@RequestHeader("X-User-Role") String role, @PathVariable UUID id) {
        log.info("REST - Request to deactivate category");
        if (!RoleEnum.ADMIN.name().equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        categoryService.deactivateCategory(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
