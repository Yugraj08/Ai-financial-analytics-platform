package com.yugraj.financebackend.controller;

import com.yugraj.financebackend.dto.DashboardResponseDTO;
import com.yugraj.financebackend.dto.RecordRequestDTO;
import com.yugraj.financebackend.dto.RecordResponseDTO;
import com.yugraj.financebackend.service.RecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;

@RestController
@RequestMapping("/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;


    //Helper method (clean way)
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getPrincipal();
    }

    //Dashboard endpoint
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/dashboard")
    public DashboardResponseDTO getDashboard() {
        return recordService.getDashboard(getCurrentUserId());
    }

    //Create record
    @PostMapping
    public RecordResponseDTO createRecord(
           @Valid @RequestBody RecordRequestDTO request ) {

        return recordService.createRecord(getCurrentUserId(), request);
    }


    //Delete record
    @DeleteMapping("/{id}")
    public String deleteRecord(@PathVariable Long id) {
        recordService.deleteRecord(id);
        return "Record deleted successfully";
    }

    //Update by id
    @PutMapping("/{id}")
    public RecordResponseDTO updateRecord(
            @PathVariable Long id,
            @Valid @RequestBody RecordRequestDTO request) {

        return recordService.updateRecord(id, request);
    }

    //Get ALL (Filter + Pagination)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<RecordResponseDTO> getAllRecords(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        return recordService.getAllRecords(page, size, type, category, startDate, endDate);
    }
}