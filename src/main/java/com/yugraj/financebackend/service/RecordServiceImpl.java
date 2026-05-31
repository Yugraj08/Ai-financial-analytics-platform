package com.yugraj.financebackend.service;

import com.yugraj.financebackend.dto.CategorySummaryDTO;
import com.yugraj.financebackend.dto.DashboardResponseDTO;
import com.yugraj.financebackend.dto.RecordRequestDTO;
import com.yugraj.financebackend.dto.RecordResponseDTO;
import com.yugraj.financebackend.model.Record;
import com.yugraj.financebackend.model.RecordType;
import com.yugraj.financebackend.model.Role;
import com.yugraj.financebackend.model.User;
import com.yugraj.financebackend.model.spec.RecordSpecification;
import com.yugraj.financebackend.repository.RecordRepository;
import com.yugraj.financebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecordServiceImpl implements RecordService {

    private final RecordRepository recordRepository;
    private final UserRepository userRepository;


    //CREATE RECORD
    @PreAuthorize("isAuthenticated()")
    @Override
    public RecordResponseDTO createRecord(Long userId, RecordRequestDTO request) {

        // 1. Fetch user FIRST
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));


        // 2. Create record
        Record record = Record.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .date(request.getDate())
                .note(request.getNote())
                .user(user)
                .build();

        recordRepository.save(record);

        return mapToDTO(record);
    }

    //GET ALL (basic - no filter)
    @Override
    public List<RecordResponseDTO> getAllRecords() {
        return recordRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    //DELETE
    @PreAuthorize("isAuthenticated()")
    @Override
    public void deleteRecord(Long recordId) {

        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found"));

        Long currentUserId = (Long) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String role = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        // ADMIN can delete anything
        if (role.equals("ROLE_ADMIN")) {
            recordRepository.delete(record);
            return;
        }

        // Others → only own records
        if (!record.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("Access denied: You can delete only your own records");
        }

        recordRepository.delete(record);
    }

    // UPDATE
    @PreAuthorize("isAuthenticated()")
    @Override
    public RecordResponseDTO updateRecord(Long recordId, RecordRequestDTO request) {

        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found"));

        Long currentUserId = (Long) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String role = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        if (!role.equals("ROLE_ADMIN") &&
                !record.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("Access denied: You can update only your own records");
        }

        record.setAmount(request.getAmount());
        record.setType(request.getType());
        record.setCategory(request.getCategory());
        record.setDate(request.getDate());
        record.setNote(request.getNote());

        recordRepository.save(record);

        return mapToDTO(record);
    }

    @PreAuthorize("isAuthenticated()")
    @Override
    public List<RecordResponseDTO> getAllRecords(
            int page,
            int size,
            String type,
            String category,
            String startDate,
            String endDate) {

        Pageable pageable = PageRequest.of(page, size);

        // ✅ Get current user
        Long currentUserId = (Long) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        String role = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        // safer initialization
        Specification<Record> spec = (root, query, cb) -> null;

        // 🔥 IMPORTANT: Apply ownership filter (for non-admin)
        if (!role.equals("ROLE_ADMIN")) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("user").get("id"), currentUserId)
            );
        }

        //Type filter
        if (type != null) {
            try {
                spec = spec.and(
                        RecordSpecification.hasType(
                                RecordType.valueOf(type.toUpperCase())
                        )
                );
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid record type");
            }
        }

        //Category filter
        if (category != null) {
            spec = spec.and(
                    RecordSpecification.hasCategory(category)
            );
        }

        //Date filter
        if (startDate != null && endDate != null) {
            spec = spec.and(
                    RecordSpecification.dateBetween(
                            LocalDate.parse(startDate),
                            LocalDate.parse(endDate)
                    )
            );
        }

        Page<Record> records = recordRepository.findAll(spec, pageable);

        return records.getContent()
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public DashboardResponseDTO getDashboard(Long userId) {

        Double income = recordRepository.getTotalIncome(userId);
        Double expense = recordRepository.getTotalExpense(userId);

        // handle nulls
        income = (income == null) ? 0.0 : income;
        expense = (expense == null) ? 0.0 : expense;

        Double balance = income - expense;

        return new DashboardResponseDTO(income, expense, balance);
    }

    @PreAuthorize("isAuthenticated()")
    public List<CategorySummaryDTO> getCategorySummary(Long userId) {
        return recordRepository.getCategorySummary(userId)
                .stream()
                .map(obj -> new CategorySummaryDTO(
                        (String) obj[0],
                        (Double) obj[1]
                ))
                .toList();
    }
    //MAPPER
    private RecordResponseDTO mapToDTO(Record record) {
        RecordResponseDTO dto = new RecordResponseDTO();
        dto.setId(record.getId());
        dto.setAmount(record.getAmount());
        dto.setType(record.getType());
        dto.setCategory(record.getCategory());
        dto.setDate(record.getDate());
        dto.setNote(record.getNote());
        dto.setUserId(record.getUser().getId());
        return dto;
    }
}