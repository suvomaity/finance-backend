package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.TransactionRequest;
import com.zorvyn.finance.entity.AuditLog;
import com.zorvyn.finance.entity.Transaction;
import com.zorvyn.finance.entity.User;
import com.zorvyn.finance.entity.enums.TransactionType;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.repository.AuditLogRepository;
import com.zorvyn.finance.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public Map<String, Object> create(TransactionRequest request, User currentUser) {
        Transaction transaction = Transaction.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory().trim())
                .date(request.getDate())
                .description(request.getDescription())
                .createdBy(currentUser)
                .build();

        transaction = transactionRepository.save(transaction);

        // log every create for audit trail - important for finance compliance
        logAudit("CREATE", transaction.getId(),
                "Created " + request.getType() + " of " + request.getAmount() + " [" + request.getCategory() + "]",
                currentUser.getEmail());

        return toResponse(transaction);
    }

    /**
     * Filtering uses JPA Specifications - way cleaner than the if-else chain I had before.
     * Previously had 7 separate repository methods for each filter combo, this is much better.
     */
    // old approach (removed):
    // if (hasType && hasCategory && hasDateRange) { return repo.findByTypeAndCategoryAndDateBetween(...) }
    // else if (hasType && hasCategory) { return repo.findByTypeAndCategory(...) }
    // ... this was getting ridiculous with every new filter
    public Page<Map<String, Object>> getAll(TransactionType type, String category,
                                             LocalDate startDate, LocalDate endDate,
                                             String keyword, int page, int size,
                                             String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // build specs dynamically - each filter is optional
        Specification<Transaction> spec = Specification.where(null);

        if (keyword != null && !keyword.isBlank()) {
            String term = "%" + keyword.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("description")), term),
                            cb.like(cb.lower(root.get("category")), term)
                    ));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (category != null && !category.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category));
        }
        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("date"), startDate));
        }
        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("date"), endDate));
        }

        return transactionRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public Map<String, Object> getById(String id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));
        return toResponse(transaction);
    }

    @Transactional
    public Map<String, Object> update(String id, TransactionRequest request, User currentUser) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));

        // snapshot before state for audit log
        String before = transaction.getType() + " " + transaction.getAmount() + " [" + transaction.getCategory() + "]";

        transaction.setAmount(request.getAmount());
        transaction.setType(request.getType());
        transaction.setCategory(request.getCategory().trim());
        transaction.setDate(request.getDate());
        transaction.setDescription(request.getDescription());
        transaction = transactionRepository.save(transaction);

        String after = request.getType() + " " + request.getAmount() + " [" + request.getCategory() + "]";
        logAudit("UPDATE", id, "Before: " + before + " → After: " + after, currentUser.getEmail());

        return toResponse(transaction);
    }

    @Transactional
    public void softDelete(String id, User currentUser) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));

        // soft delete - never hard delete financial records
        transaction.setDeleted(true);
        transaction.setDeletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        logAudit("DELETE", id,
                "Soft deleted " + transaction.getType() + " of " + transaction.getAmount(),
                currentUser.getEmail());
    }

    @Transactional
    public Map<String, Object> recover(String id, User currentUser) {
        // need native query here because @SQLRestriction filters out deleted records
        // spent a while debugging this - findById won't work for deleted records
        Transaction transaction = transactionRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id));

        if (!transaction.isDeleted()) {
            throw new IllegalArgumentException("Transaction is not deleted");
        }

        transaction.setDeleted(false);
        transaction.setDeletedAt(null);
        transactionRepository.save(transaction);

        logAudit("RECOVER", id,
                "Recovered " + transaction.getType() + " of " + transaction.getAmount(),
                currentUser.getEmail());

        return toResponse(transaction);
    }

    // simple CSV export - not using any library, just StringBuilder
    // TODO: maybe switch to OpenCSV if we need to handle edge cases with commas in descriptions
    public String exportCsv(TransactionType type, LocalDate startDate, LocalDate endDate) {
        Specification<Transaction> spec = Specification.where(null);
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("date"), startDate));
        }
        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("date"), endDate));
        }

        var transactions = transactionRepository.findAll(spec, Sort.by("date").descending());

        StringBuilder csv = new StringBuilder("id,amount,type,category,date,description\n");
        for (Transaction t : transactions) {
            csv.append(t.getId()).append(",")
               .append(t.getAmount()).append(",")
               .append(t.getType()).append(",")
               .append(t.getCategory()).append(",")
               .append(t.getDate()).append(",")
               .append(t.getDescription() != null ? "\"" + t.getDescription().replace("\"", "\"\"") + "\"" : "")
               .append("\n");
        }
        return csv.toString();
    }

    private void logAudit(String action, String entityId, String details, String userEmail) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .entityType("TRANSACTION")
                .entityId(entityId)
                .performedBy(userEmail)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }

    // TODO: could replace this with a proper TransactionResponse DTO later
    private Map<String, Object> toResponse(Transaction t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getId());
        map.put("amount", t.getAmount());
        map.put("type", t.getType());
        map.put("category", t.getCategory());
        map.put("date", t.getDate());
        map.put("description", t.getDescription());
        map.put("createdBy", t.getCreatedBy() != null ? t.getCreatedBy().getName() : null);
        map.put("createdAt", t.getCreatedAt());
        map.put("updatedAt", t.getUpdatedAt());
        return map;
    }
}
