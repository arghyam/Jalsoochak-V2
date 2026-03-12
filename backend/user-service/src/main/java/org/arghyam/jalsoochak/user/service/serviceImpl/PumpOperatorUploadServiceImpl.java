package org.arghyam.jalsoochak.user.service.serviceImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.arghyam.jalsoochak.user.auth.UploadAuthService;
import org.arghyam.jalsoochak.user.config.TenantContext;
import org.arghyam.jalsoochak.user.dto.response.PumpOperatorUploadResponseDTO;
import org.arghyam.jalsoochak.user.dto.response.UploadErrorDTO;
import org.arghyam.jalsoochak.user.exceptions.BadRequestException;
import org.arghyam.jalsoochak.user.repository.TenantUserRecord;
import org.arghyam.jalsoochak.user.repository.UserCommonRepository;
import org.arghyam.jalsoochak.user.repository.UserSchemeMappingCreateRow;
import org.arghyam.jalsoochak.user.repository.UserTenantRepository;
import org.arghyam.jalsoochak.user.repository.UserUploadRepository;
import org.arghyam.jalsoochak.user.service.PumpOperatorUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PumpOperatorUploadServiceImpl implements PumpOperatorUploadService {

    private static final String REQUIRED_TARGET_USER_TYPE = "pump_operator";

    // Strict header contract (order and spelling must match exactly).
    private static final List<String> EXPECTED_HEADERS = List.of(
            "first_name",
            "last_name",
            "full_name",
            "phone_number",
            "alternate_number",
            "person_type_id",
            "state_scheme_id",
            "center_scheme_id"
    );

    private static final DataFormatter DATA_FORMATTER = new DataFormatter(Locale.ROOT);

    private final UploadAuthService uploadAuthService;
    private final UserTenantRepository userTenantRepository;
    private final UserUploadRepository userUploadRepository;
    private final UserCommonRepository userCommonRepository;

    @Override
    @Transactional
    public PumpOperatorUploadResponseDTO uploadPumpOperatorMappings(MultipartFile file, String authorizationHeader) {
        String schemaName = requireTenantSchema();
        int actorUserId = uploadAuthService.requireStateAdminUserId(schemaName, authorizationHeader);
        validateFile(file);

        TenantUserRecord actor = userTenantRepository.findUserById(schemaName, (long) actorUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found for token"));

        Integer pumpOperatorTypeId = userCommonRepository.findUserTypeIdByName("PUMP_OPERATOR")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Missing required user type in common_schema.user_type_master_table: " + REQUIRED_TARGET_USER_TYPE
                ));

        String extension = extractExtension(file.getOriginalFilename());
        List<ParsedRow> parsedRows = parseRows(file, extension);

        ValidationResult result = validateAndBuildMappings(schemaName, parsedRows, actor, pumpOperatorTypeId);
        userUploadRepository.insertUserSchemeMappings(schemaName, result.rowsToInsert(), actorUserId);

        return PumpOperatorUploadResponseDTO.builder()
                .message("Pump operator upload processed successfully")
                .totalRows(parsedRows.size())
                .uploadedRows(result.rowsToInsert().size())
                .skippedRows(result.skippedRows())
                .build();
    }

    private String requireTenantSchema() {
        String schemaName = TenantContext.getSchema();
        if (schemaName == null || schemaName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tenant could not be resolved. Ensure X-Tenant-Code header is set."
            );
        }
        return schemaName;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty", List.of(
                    UploadErrorDTO.builder().row(0).field("file").message("Please upload a non-empty CSV or Excel file").build()
            ));
        }
        String extension = extractExtension(file.getOriginalFilename());
        if (!"csv".equals(extension) && !"xlsx".equals(extension) && !"xls".equals(extension)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only .csv, .xlsx and .xls files are supported");
        }
    }

    private List<ParsedRow> parseRows(MultipartFile file, String extension) {
        try {
            return "csv".equals(extension) ? parseCsv(file) : parseXlsx(file);
        } catch (IOException ex) {
            throw new BadRequestException("Failed to read uploaded file", List.of(
                    UploadErrorDTO.builder().row(0).field("file").message("Unable to read file content").build()
            ));
        }
    }

    private List<ParsedRow> parseCsv(MultipartFile file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder().setTrim(true).setIgnoreSurroundingSpaces(true).build().parse(reader)) {

            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                throw new BadRequestException("Uploaded file is empty", List.of(
                        UploadErrorDTO.builder().row(0).field("file").message("Header row is missing").build()
                ));
            }

            List<String> rawHeaders = new ArrayList<>();
            records.getFirst().forEach(rawHeaders::add);
            requireExactHeaders(1, rawHeaders);
            List<String> activeHeaders = EXPECTED_HEADERS;

            List<ParsedRow> rows = new ArrayList<>();
            for (int i = 1; i < records.size(); i++) {
                CSVRecord record = records.get(i);
                List<String> values = new ArrayList<>();
                for (int c = 0; c < activeHeaders.size(); c++) {
                    values.add(c < record.size() ? normalizeValue(record.get(c)) : "");
                }
                Map<String, String> map = toMap(activeHeaders, values);
                if (isAllBlank(map)) {
                    continue;
                }
                rows.add(new ParsedRow((int) record.getRecordNumber(), map));
            }
            return rows;
        }
    }

    private List<ParsedRow> parseXlsx(MultipartFile file) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new BadRequestException("Uploaded file is empty", List.of(
                        UploadErrorDTO.builder().row(0).field("file").message("Worksheet is missing").build()
                ));
            }

            int firstRowIndex = sheet.getFirstRowNum();
            Row headerRow = sheet.getRow(firstRowIndex);
            if (headerRow == null) {
                throw new BadRequestException("Invalid headers", List.of(
                        UploadErrorDTO.builder().row(1).field("header").message("Header row is missing").build()
                ));
            }

            List<String> rawHeaders = new ArrayList<>();
            int headerSize = Math.max(headerRow.getLastCellNum(), (short) 0);
            for (int i = 0; i < headerSize; i++) {
                rawHeaders.add(DATA_FORMATTER.formatCellValue(headerRow.getCell(i)));
            }
            requireExactHeaders(firstRowIndex + 1, rawHeaders);
            List<String> activeHeaders = EXPECTED_HEADERS;

            List<ParsedRow> rows = new ArrayList<>();
            for (int i = firstRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                List<String> values = new ArrayList<>();
                for (int c = 0; c < activeHeaders.size(); c++) {
                    values.add(row == null ? "" : normalizeValue(DATA_FORMATTER.formatCellValue(row.getCell(c))));
                }
                Map<String, String> map = toMap(activeHeaders, values);
                if (isAllBlank(map)) {
                    continue;
                }
                rows.add(new ParsedRow(i + 1, map));
            }
            return rows;
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Failed to read uploaded file", List.of(
                    UploadErrorDTO.builder().row(0).field("file").message("Unable to read file content").build()
            ));
        }
    }

    private ValidationResult validateAndBuildMappings(
            String schemaName,
            List<ParsedRow> rows,
            TenantUserRecord actor,
            int pumpOperatorTypeId
    ) {
        if (rows.isEmpty()) {
            throw new BadRequestException("No data rows found in uploaded file", List.of(
                    UploadErrorDTO.builder().row(0).field("file").message("At least one data row is required").build()
            ));
        }

        List<UploadErrorDTO> errors = new ArrayList<>();
        List<UserSchemeMappingCreateRow> insertRows = new ArrayList<>();
        int skipped = 0;
        Set<String> seenPhones = new HashSet<>();

        for (ParsedRow row : rows) {
            Map<String, String> v = row.values();

            String firstName = normalizeValue(v.get("first_name"));
            String lastName = normalizeValue(v.get("last_name"));
            String fullName = normalizeValue(v.get("full_name"));
            String phone = normalizeValue(v.get("phone_number"));
            String alternateNumber = normalizeValue(v.get("alternate_number"));
            String personTypeInFile = normalizeHeader(v.get("person_type_id"));
            String stateSchemeId = normalizeValue(v.get("state_scheme_id"));
            String centreSchemeId = normalizeValue(v.get("center_scheme_id"));

            if (firstName.isBlank()) {
                errors.add(err(row.rowNumber(), "first_name", "First name is required"));
                continue;
            }
            if (lastName.isBlank()) {
                errors.add(err(row.rowNumber(), "last_name", "Last name is required"));
                continue;
            }
            if (fullName.isBlank()) {
                errors.add(err(row.rowNumber(), "full_name", "Full name is required"));
                continue;
            }

            if (phone.isBlank()) {
                errors.add(err(row.rowNumber(), "phone_number", "Phone number is required"));
                continue;
            }
            if (phone.startsWith("+")) {
                errors.add(err(row.rowNumber(), "phone_number", "Phone number must not start with '+'"));
                continue;
            }
            if (!phone.matches("^9\\d{9}$")) {
                errors.add(err(row.rowNumber(), "phone_number", "Phone number must be a valid 10-digit Indian number starting with 9"));
                continue;
            }
            if (!seenPhones.add(phone)) {
                errors.add(err(row.rowNumber(), "phone_number", "Duplicate phone_number in uploaded file"));
                continue;
            }
            if (!alternateNumber.isBlank()) {
                if (alternateNumber.startsWith("+")) {
                    errors.add(err(row.rowNumber(), "alternate_number", "Alternate number must not start with '+'"));
                    continue;
                }
                if (!alternateNumber.matches("^9\\d{9}$")) {
                    errors.add(err(row.rowNumber(), "alternate_number", "Alternate number must be a valid 10-digit Indian number starting with 9"));
                    continue;
                }
            }

            if (personTypeInFile.isBlank()) {
                errors.add(err(row.rowNumber(), "person_type_id", "person_type_id is required"));
                continue;
            }
            if (!REQUIRED_TARGET_USER_TYPE.equals(personTypeInFile)) {
                errors.add(err(row.rowNumber(), "person_type_id", "person_type_id must be " + REQUIRED_TARGET_USER_TYPE));
                continue;
            }

            if (stateSchemeId.isBlank()) {
                errors.add(err(row.rowNumber(), "state_scheme_id", "state_scheme_id is required"));
                continue;
            }
            if (centreSchemeId.isBlank()) {
                errors.add(err(row.rowNumber(), "center_scheme_id", "center_scheme_id is required"));
                continue;
            }

            // Phone must not exist previously in DB.
            TenantUserRecord existing = userTenantRepository.findUserByPhone(schemaName, phone).orElse(null);
            if (existing != null) {
                errors.add(err(row.rowNumber(), "phone_number", "Phone number already exists in database"));
                continue;
            }

            CreatedUser created = createPumpOperatorUser(schemaName, actor, pumpOperatorTypeId, phone, fullName, firstName, lastName);
            TenantUserRecord user = new TenantUserRecord(
                    created.userId(),
                    actor.tenantId(),
                    phone,
                    created.email(),
                    (long) pumpOperatorTypeId,
                    "PUMP_OPERATOR",
                    created.title()
            );

            Integer schemeId = userUploadRepository.findSchemeId(schemaName, blankToNull(stateSchemeId), blankToNull(centreSchemeId));
            if (schemeId == null) {
                errors.add(err(row.rowNumber(), "scheme", "Invalid state_scheme_id/center_scheme_id (scheme not found)"));
                continue;
            }

            if (userUploadRepository.existsUserSchemeMapping(schemaName, user.id(), schemeId)) {
                skipped++;
                continue;
            }
            insertRows.add(new UserSchemeMappingCreateRow(user.id(), schemeId));
        }

        if (!errors.isEmpty()) {
            throw new BadRequestException("Validation failed for uploaded file", errors);
        }

        return new ValidationResult(insertRows, skipped);
    }

    private CreatedUser createPumpOperatorUser(
            String schemaName,
            TenantUserRecord actor,
            int pumpOperatorTypeId,
            String phone,
            String fullName,
            String firstName,
            String lastName
    ) {
        if (actor == null || actor.tenantId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to resolve tenant_id for uploader");
        }
        if (actor.id() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to resolve uploader user_id");
        }

        String email = generatedEmailForPhone(phone);
        if (userTenantRepository.findUserByEmail(schemaName, email).isPresent()) {
            email = "po_" + phone + "_" + UUID.randomUUID() + "@pump-operator.local";
        }

        String title = !normalizeValue(fullName).isBlank()
                ? normalizeValue(fullName)
                : (normalizeValue(firstName) + " " + normalizeValue(lastName)).trim();
        if (title.isBlank()) {
            title = "Pump Operator " + phone;
        }

        String uuid = UUID.randomUUID().toString();
        Long createdId = userTenantRepository.createUser(
                schemaName,
                uuid,
                actor.tenantId(),
                title,
                email,
                pumpOperatorTypeId,
                phone,
                "CSV_ONBOARDED",
                actor.id()
        );
        if (createdId == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create pump operator user");
        }
        return new CreatedUser(createdId, email, title);
    }

    private String generatedEmailForPhone(String phone) {
        return "po_" + phone + "@pump-operator.local";
    }

    private record CreatedUser(long userId, String email, String title) {}

    private UploadErrorDTO err(int row, String field, String message) {
        return UploadErrorDTO.builder().row(row).field(field).message(message).build();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String blankToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isBlank() ? null : t;
    }

    private boolean isPumpOperator(String cName) {
        if (cName == null) {
            return false;
        }
        return REQUIRED_TARGET_USER_TYPE.equals(normalizeHeader(cName));
    }

    private static boolean isAllBlank(Map<String, String> map) {
        for (String v : map.values()) {
            if (v != null && !v.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private Map<String, String> toMap(List<String> headers, List<String> values) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            map.put(headers.get(i), i < values.size() ? normalizeValue(values.get(i)) : "");
        }
        return map;
    }

    private void requireExactHeaders(int rowNum, List<String> rawHeaders) {
        List<String> actual = rawHeaders.stream()
                .map(v -> v == null ? "" : v.trim())
                .toList();

        String expected = String.join(",", EXPECTED_HEADERS);
        if (actual.size() != EXPECTED_HEADERS.size()) {
            throw new BadRequestException("Invalid headers", List.of(
                    UploadErrorDTO.builder()
                            .row(rowNum)
                            .field("header")
                            .message("Header row must be exactly: " + expected)
                            .build()
            ));
        }

        for (int i = 0; i < EXPECTED_HEADERS.size(); i++) {
            if (!EXPECTED_HEADERS.get(i).equals(actual.get(i))) {
                throw new BadRequestException("Invalid headers", List.of(
                        UploadErrorDTO.builder()
                                .row(rowNum)
                                .field("header")
                                .message("Header row must be exactly: " + expected)
                                .build()
                ));
            }
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            return "";
        }
        return filename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeHeader(String value) {
        if (value == null) {
            return "";
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        return v.replace(' ', '_');
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    private record ParsedRow(int rowNumber, Map<String, String> values) {
    }

    private record ValidationResult(List<UserSchemeMappingCreateRow> rowsToInsert, int skippedRows) {
    }
}
