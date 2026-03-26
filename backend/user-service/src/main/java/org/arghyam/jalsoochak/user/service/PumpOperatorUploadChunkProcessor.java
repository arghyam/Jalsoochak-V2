package org.arghyam.jalsoochak.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.arghyam.jalsoochak.user.event.UserEventPublisher;
import org.arghyam.jalsoochak.user.repository.TenantUserRecord;
import org.arghyam.jalsoochak.user.repository.UserSchemeMappingCreateRow;
import org.arghyam.jalsoochak.user.repository.UserTenantRepository;
import org.arghyam.jalsoochak.user.repository.UserUploadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Processes uploads in small transactions so very large CSVs don't create a single massive transaction
 * or require materializing everything in memory.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PumpOperatorUploadChunkProcessor {

    private final UserTenantRepository userTenantRepository;
    private final UserUploadRepository userUploadRepository;
    private final UserEventPublisher userEventPublisher;

    public record UploadRow(
            int rowNumber,
            String firstName,
            String lastName,
            String fullName,
            String phone,
            String personType,
            String stateSchemeId
    ) {}

    public record ChunkResult(int uploadedRows, int skippedRows) {}

    @Transactional
    public ChunkResult processChunk(
            String schemaName,
            String tenantCode,
            TenantUserRecord actor,
            int pumpOperatorTypeId,
            int preferredLanguageId,
            int actorUserId,
            List<UploadRow> rows,
            Map<String, Integer> schemeIdCache
    ) {
        if (rows == null || rows.isEmpty()) {
            return new ChunkResult(0, 0);
        }

        List<UserSchemeMappingCreateRow> insertRows = new ArrayList<>(rows.size());
        List<String> phonesForInsertRows = new ArrayList<>(rows.size());
        int uploaded = 0;
        int skipped = 0;

        for (UploadRow row : rows) {
            try {
                Integer schemeId = resolveSchemeId(schemaName, row.stateSchemeId(), schemeIdCache);
                if (schemeId == null || schemeId < 0) {
                    skipped++;
                    continue;
                }

                TenantUserRecord user = userTenantRepository.findUserByPhone(schemaName, row.phone()).orElse(null);
                Long userId;
                if (user == null) {
                    String title = !row.fullName().isBlank()
                            ? row.fullName()
                            : (row.firstName() + " " + row.lastName()).trim();
                    if (title.isBlank()) {
                        title = "Pump Operator " + row.phone();
                    }

                    userId = userTenantRepository.createUser(
                            schemaName,
                            java.util.UUID.randomUUID().toString(),
                            actor.tenantId(),
                            title,
                            uniqueEmail(schemaName, row.phone()),
                            pumpOperatorTypeId,
                            row.phone(),
                            "CSV_ONBOARDED",
                            actor.id()
                    );
                    if (userId == null) {
                        skipped++;
                        continue;
                    }
                    userTenantRepository.updateUserLanguageId(schemaName, userId, preferredLanguageId);
                } else {
                    // If an existing user isn't a pump operator, we skip to avoid mutating unrelated user types.
                    if (user.cName() == null || !user.cName().equalsIgnoreCase("PUMP_OPERATOR")) {
                        skipped++;
                        continue;
                    }
                    userId = user.id();
                    userTenantRepository.updateUserLanguageId(schemaName, userId, preferredLanguageId);
                }

                // Idempotent insert (ON CONFLICT DO NOTHING) will safely handle re-uploads + concurrent chunks.
                insertRows.add(new UserSchemeMappingCreateRow(userId, schemeId));
                phonesForInsertRows.add(row.phone());

                uploaded++;
            } catch (Exception ex) {
                // Best-effort: one bad row must not abort the entire batch upload.
                skipped++;
                log.warn("[pump-operator-upload] row_failed row={} err={}", row.rowNumber(), ex.getMessage());
                log.debug("[pump-operator-upload] row_failed row={} phone={} err={}",
                        row.rowNumber(), row.phone(), ex.getMessage());
            }
        }

        int[] insertCounts = userUploadRepository.insertUserSchemeMappings(schemaName, insertRows, actorUserId);
        Set<String> phonesToNotify = new LinkedHashSet<>();
        int inserted = 0;
        int n = Math.min(insertCounts.length, phonesForInsertRows.size());
        for (int i = 0; i < n; i++) {
            if (insertCounts[i] > 0) {
                inserted++;
                phonesToNotify.add(phonesForInsertRows.get(i));
            }
        }

        if (!phonesToNotify.isEmpty()) {
            userEventPublisher.publishPumpOperatorOnboardedAfterCommit(
                    tenantCode,
                    actor.tenantId(),
                    String.valueOf(preferredLanguageId),
                    new ArrayList<>(phonesToNotify)
            );
        }

        log.info("[pump-operator-upload] chunk_processed rows={} uploaded={} skipped={} mappings_inserted={}",
                rows.size(), uploaded, skipped, inserted);

        return new ChunkResult(uploaded, skipped);
    }

    private Integer resolveSchemeId(
            String schemaName,
            String stateSchemeId,
            Map<String, Integer> schemeIdCache
    ) {
        String schemeKey = stateSchemeId;
        Integer schemeId = schemeIdCache.get(schemeKey);
        if (schemeId != null) {
            return schemeId;
        }
        Integer found = userUploadRepository.findSchemeId(schemaName, blankToNull(stateSchemeId), null);
        schemeId = found != null ? found : -1;
        schemeIdCache.put(schemeKey, schemeId);
        return schemeId;
    }

    private static String generatedEmailForPhone(String phone) {
        return "po_" + phone + "@pump-operator.local";
    }

    private String uniqueEmail(String schemaName, String phone) {
        String email = generatedEmailForPhone(phone);
        // Extremely unlikely for new users (email derives from phone), but keep the same safety as the old flow.
        if (userTenantRepository.findUserByEmail(schemaName, email).isPresent()) {
            return "po_" + phone + "_" + java.util.UUID.randomUUID() + "@pump-operator.local";
        }
        return email;
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isBlank() ? null : t;
    }
}
