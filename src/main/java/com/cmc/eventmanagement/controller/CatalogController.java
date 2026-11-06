package com.cmc.eventmanagement.controller;

import com.cmc.eventmanagement.dto.response.CategoryResponse;
import com.cmc.eventmanagement.dto.response.VenueResponse;
import com.cmc.eventmanagement.service.CatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Danh mục công khai để frontend đổ vào dropdown. */
@RestController
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService catalogService;

    @GetMapping("/api/categories")
    public List<CategoryResponse> categories() { return catalogService.categories(); }

    @GetMapping("/api/venues")
    public List<VenueResponse> venues() { return catalogService.venues(); }
}
