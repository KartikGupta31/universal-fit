
package com.universalfit.controller;

import com.universalfit.domain.enums.Category;
import com.universalfit.domain.enums.MeasurementBasis;
import com.universalfit.domain.enums.SizingSystem;
import com.universalfit.domain.model.SizeChart;
import com.universalfit.dto.SizeChartResponse;
import com.universalfit.repository.BrandRepository;
import com.universalfit.repository.SizeChartRepository;

import lombok.RequiredArgsConstructor;

import org.bson.types.ObjectId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/size-charts")
@RequiredArgsConstructor
public class SizeChartController {

    private final SizeChartRepository sizeChartRepository;
    private final BrandRepository brandRepository;

    // GET: Retrieve charts for a brand and category
    @GetMapping
    public ResponseEntity<?> getSizeCharts(
            @RequestParam String brandId,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) SizingSystem sizingSystem,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) MeasurementBasis measurementBasis) {

        if (!ObjectId.isValid(brandId)) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid brand ID"));
        }

        ObjectId brandObjectId = new ObjectId(brandId);

        if (brandRepository.findById(brandObjectId)
                .filter(brand -> brand.isActive())
                .isEmpty()) {

            return ResponseEntity.notFound().build();
        }

        List<SizeChart> charts;

        if (category != null
                && sizingSystem != null
                && region != null
                && measurementBasis != null) {

            charts =
                    sizeChartRepository
                            .findByBrandIdAndCategoryAndSizingSystemAndRegionAndMeasurementBasisAndActiveTrue(
                                    brandObjectId,
                                    category,
                                    sizingSystem,
                                    region,
                                    measurementBasis
                            );

        } else if (category != null) {

            charts =
                    sizeChartRepository
                            .findByBrandIdAndCategoryAndActiveTrue(
                                    brandObjectId,
                                    category
                            );

        } else {

            charts =
                    sizeChartRepository
                            .findByBrandIdAndActiveTrue(brandObjectId);
        }

        return ResponseEntity.ok(charts.stream().map(SizeChartResponse::from).toList());
    }

    // GET: Retrieve a single size chart
    @GetMapping("/{id}")
    public ResponseEntity<?> getSizeChartById(
            @PathVariable String id) {

        if (!ObjectId.isValid(id)) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Invalid size chart ID"));
        }

        return sizeChartRepository.findById(new ObjectId(id))
                .filter(SizeChart::isActive)
                .filter(chart -> brandRepository
                        .findById(chart.getBrandId())
                        .map(brand -> brand.isActive())
                        .orElse(false))
                .<ResponseEntity<?>>map(chart -> ResponseEntity.ok(SizeChartResponse.from(chart)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
