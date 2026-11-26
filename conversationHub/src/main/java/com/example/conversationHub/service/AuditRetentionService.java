package com.example.conversationHub.service;

import com.example.conversationHub.domain.AuditLog;
import com.example.conversationHub.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Applies the documented audit retention period (FR-12.3, NFR-5.6). */
@Service
public class AuditRetentionService {

    private static final Logger log = LoggerFactory.getLogger(AuditRetentionService.class);

    private final AuditLogRepository auditLogRepository;
    private final long retentionDays;

    public AuditRetentionService(AuditLogRepository auditLogRepository,
                                 @Value("${app.audit.retention-days:365}") long retentionDays) {
        this.auditLogRepository = auditLogRepository;
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedDelayString = "${app.audit.cleanup-scan-ms:86400000}")
    @Transactional
    public void deleteExpiredAuditLogs() {
        if (retentionDays <= 0) {
            return;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        List<AuditLog> expired = auditLogRepository.findAll().stream()
                .filter(entry -> entry.getPerformedAt() != null && entry.getPerformedAt().isBefore(cutoff))
                .toList();
        if (!expired.isEmpty()) {
            auditLogRepository.deleteAllInBatch(expired);
            log.info("Deleted {} audit records older than {}", expired.size(), cutoff);
        }
    }
}
