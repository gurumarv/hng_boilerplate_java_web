package hng_java_boilerplate.waitlist.controller;

import hng_java_boilerplate.waitlist.entity.WaitlistPage;
import hng_java_boilerplate.waitlist.service.WaitlistPageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/waitlist/pages")
@Tag(name = "Waitlist Pages")
@PreAuthorize("hasRole('ADMIN')")
public class WaitlistPageController {

    private final WaitlistPageService waitlistPageService;

    public WaitlistPageController(WaitlistPageService waitlistPageService) {
        this.waitlistPageService = waitlistPageService;
    }

    @PostMapping
    public ResponseEntity<?> createWaitlistPage(@RequestBody WaitlistPage waitlistPage) throws IOException, SQLException {
        WaitlistPage savedPage = waitlistPageService.createWaitlistPage(waitlistPage);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Waitlist page created successfully");
        response.put("id", savedPage.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<?> getWaitlistPages(Pageable pageable) {
        Page<WaitlistPage> waitlistPages = waitlistPageService.getWaitlistPages(pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("pages", waitlistPages.getContent());
        response.put("currentPage", waitlistPages.getNumber());
        response.put("totalItems", waitlistPages.getTotalElements());
        response.put("totalPages", waitlistPages.getTotalPages());
        response.put("status_code", HttpStatus.OK.value());
        response.put("message", "Retrieved waitlist pages successfully");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchWaitlistPages(@RequestParam String keyword, Pageable pageable) {
        Page<WaitlistPage> waitlistPages = waitlistPageService.searchWaitlistPages(keyword, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("pages", waitlistPages.getContent());
        response.put("currentPage", waitlistPages.getNumber());
        response.put("totalItems", waitlistPages.getTotalElements());
        response.put("totalPages", waitlistPages.getTotalPages());
        response.put("status_code", HttpStatus.OK.value());
        response.put("message", "Search completed successfully");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<?> toggleWaitlistPageActive(@PathVariable UUID id) {
        WaitlistPage updatedPage = waitlistPageService.toggleWaitlistPageActive(id);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Waitlist page active status toggled successfully");
        response.put("id", updatedPage.getId());
        response.put("active", updatedPage.isActive());

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}