package com.cmc.eventmanagement.controller;

import com.cmc.eventmanagement.repository.EventCategoryRepository;
import com.cmc.eventmanagement.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Danh mục dùng khi tạo sự kiện (công khai, chỉ đọc). */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MetaController {
    private final EventCategoryRepository categoryRepo;
    private final VenueRepository venueRepo;

    public record CategoryItem(Long id, String name) {}
    public record VenueItem(Long id, String name, String address, Integer maxCapacity) {}

    @GetMapping("/categories")
    public List<CategoryItem> categories() {
        return categoryRepo.findAll().stream().map(c -> new CategoryItem(c.getId(), c.getName())).toList();
    }

    @GetMapping("/venues")
    public List<VenueItem> venues() {
        return venueRepo.findAll().stream()
                .map(v -> new VenueItem(v.getId(), v.getName(), v.getAddress(), v.getMaxCapacity())).toList();
    }
}
