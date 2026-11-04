
package com.universalfit.controller;

import com.universalfit.domain.enums.Category;
import com.universalfit.domain.model.Product;
import com.universalfit.repository.BrandRepository;
import com.universalfit.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    // GET: Browse active products
    @GetMapping
    public ResponseEntity<?> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) Category category) {

        if (page < 0 || size < 1 || size > 50) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid pagination parameters"));
        }

        if (brandId != null && !ObjectId.isValid(brandId)) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid brand ID"));
        }

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Product> products;

        if (brandId != null && category != null) {
            products = productRepository.findByActiveTrueAndBrandIdAndCategory(
                    new ObjectId(brandId), category, pageable);

        } else if (brandId != null) {
            products = productRepository.findByActiveTrueAndBrandId(
                    new ObjectId(brandId), pageable);

        } else if (category != null) {
            products = productRepository.findByActiveTrueAndCategory(
                    category, pageable);

        } else {
            products = productRepository.findByActiveTrue(pageable);
        }

        return ResponseEntity.ok(products);
    }

    // GET: Retrieve one product
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable String id) {

        if (!ObjectId.isValid(id)) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid product ID"));
        }

        return productRepository.findById(new ObjectId(id))
                .filter(Product::isActive)
                .filter(product -> brandRepository
                        .findById(product.getBrandId())
                        .map(brand -> brand.isActive())
                        .orElse(false))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
