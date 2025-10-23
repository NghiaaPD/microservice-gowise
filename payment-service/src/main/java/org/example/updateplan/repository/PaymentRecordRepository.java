package org.example.updateplan.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.example.updateplan.model.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, UUID> {

    Optional<PaymentRecord> findByOrderCode(Long orderCode);

    List<PaymentRecord> findByExpiresAtBefore(OffsetDateTime threshold);
}
