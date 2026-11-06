package com.cmc.eventmanagement.service;

import com.cmc.eventmanagement.dto.request.CategoryRequest;
import com.cmc.eventmanagement.dto.request.VenueRequest;
import com.cmc.eventmanagement.dto.response.CategoryResponse;
import com.cmc.eventmanagement.dto.response.VenueResponse;
import com.cmc.eventmanagement.entity.EventCategory;
import com.cmc.eventmanagement.entity.Venue;
import com.cmc.eventmanagement.exception.ConflictException;
import com.cmc.eventmanagement.exception.NotFoundException;
import com.cmc.eventmanagement.repository.EventCategoryRepository;
import com.cmc.eventmanagement.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** FR14, FR15: danh mục loại sự kiện và địa điểm. */
@Service
@RequiredArgsConstructor
public class CatalogService {
    private final EventCategoryRepository categoryRepo;
    private final VenueRepository venueRepo;

    @Transactional(readOnly = true)
    public List<CategoryResponse> categories() {
        return categoryRepo.findAll().stream().map(c -> new CategoryResponse(c.getId(), c.getName())).toList();
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest req) {
        if (categoryRepo.existsByNameIgnoreCase(req.name())) throw new ConflictException("Loại sự kiện đã tồn tại");
        EventCategory c = categoryRepo.save(new EventCategory(req.name().trim()));
        return new CategoryResponse(c.getId(), c.getName());
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest req) {
        EventCategory c = categoryRepo.findById(id).orElseThrow(() -> new NotFoundException("Loại sự kiện không tồn tại"));
        c.setName(req.name().trim());
        return new CategoryResponse(c.getId(), c.getName());
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepo.existsById(id)) throw new NotFoundException("Loại sự kiện không tồn tại");
        try {
            categoryRepo.deleteById(id);
            categoryRepo.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Loại sự kiện đang được sử dụng, không thể xóa");
        }
    }

    @Transactional(readOnly = true)
    public List<VenueResponse> venues() {
        return venueRepo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public VenueResponse createVenue(VenueRequest req) {
        return toResponse(venueRepo.save(new Venue(req.name().trim(), req.address(), req.maxCapacity())));
    }

    @Transactional
    public VenueResponse updateVenue(Long id, VenueRequest req) {
        Venue v = venueRepo.findById(id).orElseThrow(() -> new NotFoundException("Địa điểm không tồn tại"));
        v.setName(req.name().trim());
        v.setAddress(req.address());
        v.setMaxCapacity(req.maxCapacity());
        return toResponse(v);
    }

    @Transactional
    public void deleteVenue(Long id) {
        if (!venueRepo.existsById(id)) throw new NotFoundException("Địa điểm không tồn tại");
        try {
            venueRepo.deleteById(id);
            venueRepo.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Địa điểm đang được sử dụng, không thể xóa");
        }
    }

    private VenueResponse toResponse(Venue v) {
        return new VenueResponse(v.getId(), v.getName(), v.getAddress(), v.getMaxCapacity());
    }
}
