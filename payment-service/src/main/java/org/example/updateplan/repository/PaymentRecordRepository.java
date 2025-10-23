package org.example.updateplan.repository;

import java.util.Optional;

import org.example.updateplan.model.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {

    Optional<PaymentRecord> findByOrderCode(Long orderCode);
}

