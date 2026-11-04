
package com.universalfit.controller;

import com.universalfit.domain.enums.Category;
import com.universalfit.domain.model.Brand;
import com.universalfit.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandRepository brandRepository;

    // GET: List active brands with pagination
    @GetMapping
    public ResponseEntity<?> getBrands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Category category) {

        if (page < 0 || size < 1 || size > 50) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid pagination parameters"));
        }

        PageRequest pageable = PageRequest.of(
                page, size, Sort.by(Sort.Direction.ASC, "name"));

        Page<Brand> brands;

        if (search != null && !search.isBlank()) {
            brands = brandRepository.findByActiveTrueAndNameContainingIgnoreCase(
                    search.trim(), pageable);
        } else if (category != null) {
            brands = brandRepository.findByActiveTrueAndCategoriesContaining(
                    category, pageable);
        } else {
            brands = brandRepository.findByActiveTrue(pageable);
        }

        return ResponseEntity.ok(brands);
    }

    // GET: Get one active brand by slug
    @GetMapping("/{slug}")
    public ResponseEntity<?> getBrandBySlug(@PathVariable String slug) {

        return brandRepository.findBySlug(slug)
                .filter(Brand::isActive)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
