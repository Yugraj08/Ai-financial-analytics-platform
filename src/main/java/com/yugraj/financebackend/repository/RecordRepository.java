package com.yugraj.financebackend.repository;

import com.yugraj.financebackend.model.Record;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface RecordRepository extends JpaRepository<Record, Long>,
        JpaSpecificationExecutor<Record> {

    /**
     * Fetch user's records ordered by date desc (for AI context).
     */
    List<Record> findByUserIdOrderByDateDesc(Long userId, Pageable pageable);

    @Query("SELECT SUM(r.amount) FROM Record r WHERE r.type = 'INCOME' AND r.user.id = :userId")
    Double getTotalIncome(@Param("userId") Long userId);

    @Query("SELECT SUM(r.amount) FROM Record r WHERE r.type = 'EXPENSE' AND r.user.id = :userId")
    Double getTotalExpense(@Param("userId") Long userId);

    @Query("SELECT r.category, SUM(r.amount) FROM Record r WHERE r.user.id = :userId GROUP BY r.category")
    List<Object[]> getCategorySummary(@Param("userId") Long userId);
}