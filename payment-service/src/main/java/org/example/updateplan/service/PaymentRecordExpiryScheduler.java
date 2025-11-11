package org.example.updateplan.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PaymentRecordExpiryScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentRecordExpiryScheduler.class);

    private final PayOSService payOSService;

    public PaymentRecordExpiryScheduler(PayOSService payOSService) {
        this.payOSService = payOSService;
    }

    @Scheduled(fixedDelayString = "${payos.payment-record.expiry-check-delay-ms:60000}")
    public void markExpiredPaymentsAsFailed() {
        int updated = payOSService.markExpiredRecordsAsFailed();
        if (updated > 0 && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Marked {} payment record(s) as FAILED due to expiration", updated);
        }
    }
}
