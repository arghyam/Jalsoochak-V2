package org.arghyam.jalsoochak.scheme.service;

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
import org.arghyam.jalsoochak.scheme.auth.UploadAuthService;
import org.arghyam.jalsoochak.scheme.config.TenantContext;
import org.arghyam.jalsoochak.scheme.dto.SchemeDTO;
import org.arghyam.jalsoochak.scheme.dto.SchemeUploadErrorDTO;
import org.arghyam.jalsoochak.scheme.dto.SchemeUploadResponseDTO;
import org.arghyam.jalsoochak.scheme.exception.FileValidationException;
import org.arghyam.jalsoochak.scheme.exception.UnsupportedFileTypeException;
import org.arghyam.jalsoochak.scheme.repository.SchemeCreateRecord;
import org.arghyam.jalsoochak.scheme.repository.SchemeDbRepository;
import org.arghyam.jalsoochak.scheme.repository.SchemeLgdMappingCreateRecord;
import org.arghyam.jalsoochak.scheme.repository.SchemeSubdivisionMappingCreateRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchemeServiceImpl implements SchemeService {

    private static final int MAX_VALIDATION_ERRORS = 1000;
    private static final int CHUNK_SIZE = 1000;

    private static final List<String> SCHEME_HEADERS_V2 = List.of(
            "state_scheme_id",
            "centre_scheme_id",
            "scheme_name",
            "fhtc_count",
            "planned_fhtc",
            "house_hold_count",
            "latitude",
            "longitude",
            "channel",
            "work_status",
            "operating_status"
    );
    private static final List<String> SCHEME_HEADERS_V1 = List.of(
            "state_scheme_id",
            "centre_scheme_id",
            "scheme_name",
            "fhtc_count",
            "planned_fhtc",
            "house_hold_count",
            "latitude",
            "longitude",
            "channel",
            "work_status",
            "operating_status",
            "created_by",
            "updated_by"
    );

    private static final List<String> MAPPING_HEADERS_V2 = List.of(
            "scheme_id",
            "parent_lgd_id",
            "parent_lgd_level"
    );
    private static final List<String> MAPPING_HEADERS_V3 = List.of(
            "scheme_id",
            "parent_lgd_id",
            "parent_lgd_level",
            "parent_department_id",
            "parent_department_level"
    );

    private static final Map<String, Integer> CHANNEL_MAP = Map.of(
            "1", 1,
            "bfm", 1,
            "2", 2,
            "electric", 2
    );

    private static final Map<String, Integer> WORK_STATUS_MAP = Map.of(
            "1", 1,
            "ongoing", 1,
            "2", 2,
            "completed", 2,
            "3", 3,
            "not started", 3,
            "4", 4,
            "handed over", 4
    );

    private static final Map<String, Integer> OPERATING_STATUS_MAP = Map.of(
            "1", 1,
            "operative", 1,
            "2", 2,
            "non-operative", 2,
            "3", 3,
            "partially operative", 3
    );

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("csv", "xlsx");
    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    private final SchemeDbRepository schemeDbRepository;
    private final UploadAuthService uploadAuthService;
    private final SchemeUploadChunkProcessor chunkProcessor;

    @Override
    public List<SchemeDTO> getAllSchemes() {
        String schemaName = requireTenantSchema();
        return schemeDbRepository.findAllSchemes(schemaName);
    }

    @Override
    public SchemeUploadResponseDTO uploadSchemes(MultipartFile file, String authorizationHeader) {
        String schemaName = requireTenantSchema();
        int actorUserId = uploadAuthService.requireStateAdminUserId(schemaName, authorizationHeader);
        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        List<String> activeHeaders = resolveHeaders(file, extension, List.of(SCHEME_HEADERS_V2, SCHEME_HEADERS_V1));

        int totalRows = validateSchemes(file, extension, activeHeaders);
        int uploadedRows = processSchemes(schemaName, file, extension, activeHeaders, actorUserId);

        return SchemeUploadResponseDTO.builder()
                .message("Schemes uploaded successfully")
                .totalRows(totalRows)
                .uploadedRows(uploadedRows)
                .build();
    }

    @Override
    public SchemeUploadResponseDTO uploadSchemeMappings(MultipartFile file, String authorizationHeader) {
        String schemaName = requireTenantSchema();
        int actorUserId = uploadAuthService.requireStateAdminUserId(schemaName, authorizationHeader);
        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        List<String> activeHeaders = resolveHeaders(file, extension, List.of(MAPPING_HEADERS_V3, MAPPING_HEADERS_V2));
        boolean includeDepartment = activeHeaders.contains("parent_department_id");

        int totalRows = validateMappings(schemaName, file, extension, activeHeaders, includeDepartment);
        int uploadedRows = processMappings(schemaName, file, extension, activeHeaders, includeDepartment, actorUserId);

        return SchemeUploadResponseDTO.builder()
                .message("Scheme mappings uploaded successfully")
                .totalRows(totalRows)
                .uploadedRows(uploadedRows)
                .build();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException(
                    "Uploaded file is empty",
                    List.of(error(0, "file", "Please upload a non-empty CSV or XLSX file"))
            );
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new UnsupportedFileTypeException("Only .csv and .xlsx files are supported");
        }
    }

    private List<String> resolveHeaders(MultipartFile file, String extension, List<List<String>> allowedHeaderVariants) {
        try {
            if ("csv".equals(extension)) {
                return resolveCsvHeaders(file, allowedHeaderVariants);
            }
            return resolveXlsxHeaders(file, allowedHeaderVariants);
        } catch (IOException ex) {
            throw new FileValidationException(
                    "Failed to read uploaded file",
                    List.of(error(0, "file", "Unable to read file content"))
            );
        }
    }

    private List<String> resolveCsvHeaders(MultipartFile file, List<List<String>> allowedHeaderVariants) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder().setTrim(true).setIgnoreSurroundingSpaces(true).build().parse(reader)) {

            var it = parser.iterator();
            if (!it.hasNext()) {
                throw new FileValidationException(
                        "Uploaded file is empty",
                        List.of(error(0, "file", "Header row is missing"))
                );
            }

            List<String> headers = new ArrayList<>();
            CSVRecord header = it.next();
            header.forEach(headers::add);
            return resolveHeaderVariant(headers, allowedHeaderVariants);
        }
    }

    private List<String> resolveXlsxHeaders(MultipartFile file, List<List<String>> allowedHeaderVariants) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new FileValidationException(
                        "Uploaded file is empty",
                        List.of(error(0, "file", "Worksheet is missing"))
                );
            }

            int firstRowIndex = sheet.getFirstRowNum();
            Row headerRow = sheet.getRow(firstRowIndex);
            if (headerRow == null) {
                throw new FileValidationException(
                        "Invalid headers",
                        List.of(error(1, "header", "Header row is missing"))
                );
            }

            return resolveHeaderVariant(readExcelHeaders(headerRow), allowedHeaderVariants);
        }
    }

    private List<String> readExcelHeaders(Row headerRow) {
        int size = Math.max(headerRow.getLastCellNum(), (short) 0);
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            headers.add(getExcelValue(headerRow, i));
        }
        return headers;
    }

    private List<String> indexedValues(CSVRecord record, List<String> headers) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            values.add(i < record.size() ? normalize(record.get(i)) : "");
        }
        return values;
    }

    private List<String> indexedValues(Row row, List<String> headers) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            values.add(getExcelValue(row, i));
        }
        return values;
    }

    private Map<String, String> rowAsMap(List<String> values, List<String> headers) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            map.put(headers.get(i), normalize(values.get(i)));
        }
        return map;
    }

    private String getExcelValue(Row row, int index) {
        if (row == null || row.getCell(index) == null) {
            return "";
        }
        return normalize(DATA_FORMATTER.formatCellValue(row.getCell(index)));
    }

    private List<String> resolveHeaderVariant(List<String> rawHeaders, List<List<String>> allowedHeaderVariants) {
        List<String> normalized = rawHeaders.stream().map(this::normalize).toList();
        for (List<String> variant : allowedHeaderVariants) {
            if (normalized.equals(variant)) {
                return variant;
            }
        }

        String expected = allowedHeaderVariants.stream()
                .map(v -> String.join(",", v))
                .reduce((a, b) -> a + " OR " + b)
                .orElse("");

        throw new FileValidationException(
                "Invalid headers",
                List.of(error(1, "header", "Header row must be exactly: " + expected))
        );
    }

    private int validateSchemes(MultipartFile file, String extension, List<String> activeHeaders) {
        List<SchemeUploadErrorDTO> errors = new ArrayList<>();
        final int[] total = {0};

        try {
            streamRows(file, extension, activeHeaders, (rowNumber, values) -> {
                if (isAllBlank(values)) {
                    return;
                }
                total[0]++;

                int before = errors.size();

                requireField(values, rowNumber, "state_scheme_id", errors);
                requireField(values, rowNumber, "centre_scheme_id", errors);
                requireField(values, rowNumber, "scheme_name", errors);
                requireField(values, rowNumber, "fhtc_count", errors);
                requireField(values, rowNumber, "planned_fhtc", errors);
                requireField(values, rowNumber, "house_hold_count", errors);
                requireField(values, rowNumber, "latitude", errors);
                requireField(values, rowNumber, "longitude", errors);
                requireField(values, rowNumber, "channel", errors);
                requireField(values, rowNumber, "work_status", errors);
                requireField(values, rowNumber, "operating_status", errors);

                parseInteger(values.get("fhtc_count"), rowNumber, "fhtc_count", errors);
                parseInteger(values.get("planned_fhtc"), rowNumber, "planned_fhtc", errors);
                parseInteger(values.get("house_hold_count"), rowNumber, "house_hold_count", errors);
                parseDouble(values.get("latitude"), rowNumber, "latitude", errors);
                parseDouble(values.get("longitude"), rowNumber, "longitude", errors);
                parseEnum(values.get("channel"), rowNumber, "channel", CHANNEL_MAP, "Bfm/Electric or 1/2", errors);
                parseEnum(values.get("work_status"), rowNumber, "work_status", WORK_STATUS_MAP, "Ongoing, Completed, Not Started, Handed Over or 1/2/3/4", errors);
                parseEnum(values.get("operating_status"), rowNumber, "operating_status", OPERATING_STATUS_MAP, "Operative, Non-Operative, Partially Operative or 1/2/3", errors);

                if (errors.size() > before && errors.size() >= MAX_VALIDATION_ERRORS) {
                    errors.add(error(rowNumber, "file", "Too many validation errors; showing first " + MAX_VALIDATION_ERRORS));
                    throw new TooManyErrorsException();
                }
            });
        } catch (TooManyErrorsException ignored) {
            // stop early (errors already captured)
        } catch (IOException ex) {
            throw new FileValidationException(
                    "Failed to read uploaded file",
                    List.of(error(0, "file", "Unable to read file content"))
            );
        }

        if (!errors.isEmpty()) {
            throw new FileValidationException("Validation failed for uploaded file", errors);
        }
        if (total[0] == 0) {
            throw new FileValidationException(
                    "No data rows found in uploaded file",
                    List.of(error(0, "file", "At least one data row is required"))
            );
        }
        return total[0];
    }

    private int processSchemes(String schemaName, MultipartFile file, String extension, List<String> activeHeaders, int actorUserId) {
        List<SchemeCreateRecord> chunk = new ArrayList<>(CHUNK_SIZE);
        final int[] uploaded = {0};

        try {
            streamRows(file, extension, activeHeaders, (rowNumber, values) -> {
                if (isAllBlank(values)) {
                    return;
                }

                // Validation already ran. Keep processing tight and avoid allocating error objects.
                Integer fhtcCount = Integer.parseInt(normalize(values.get("fhtc_count")));
                Integer plannedFhtc = Integer.parseInt(normalize(values.get("planned_fhtc")));
                Integer houseHoldCount = Integer.parseInt(normalize(values.get("house_hold_count")));
                Double latitude = Double.parseDouble(normalize(values.get("latitude")));
                Double longitude = Double.parseDouble(normalize(values.get("longitude")));
                Integer channel = CHANNEL_MAP.get(normalize(values.get("channel")).toLowerCase());
                Integer workStatus = WORK_STATUS_MAP.get(normalize(values.get("work_status")).toLowerCase());
                Integer operatingStatus = OPERATING_STATUS_MAP.get(normalize(values.get("operating_status")).toLowerCase());

                chunk.add(new SchemeCreateRecord(
                        UUID.randomUUID().toString(),
                        normalize(values.get("state_scheme_id")),
                        normalize(values.get("centre_scheme_id")),
                        normalize(values.get("scheme_name")),
                        fhtcCount,
                        plannedFhtc,
                        houseHoldCount,
                        latitude,
                        longitude,
                        channel,
                        workStatus,
                        operatingStatus,
                        actorUserId,
                        actorUserId
                ));

                if (chunk.size() >= CHUNK_SIZE) {
                    // Insert and clear.
                    // Use a copy so the chunk processor can safely retain the list if needed.
                    uploaded[0] += chunkProcessor.insertSchemesChunk(schemaName, new ArrayList<>(chunk));
                    chunk.clear();
                }
            });
        } catch (IOException ex) {
            throw new FileValidationException(
                    "Failed to read uploaded file",
                    List.of(error(0, "file", "Unable to read file content"))
            );
        }

        if (!chunk.isEmpty()) {
            uploaded[0] += chunkProcessor.insertSchemesChunk(schemaName, chunk);
        }
        return uploaded[0];
    }

    private int validateMappings(String schemaName, MultipartFile file, String extension, List<String> activeHeaders, boolean includeDepartment) {
        List<SchemeUploadErrorDTO> errors = new ArrayList<>();
        List<MappingRow> chunk = new ArrayList<>(CHUNK_SIZE);
        final int[] total = {0};

        try {
            streamRows(file, extension, activeHeaders, (rowNumber, values) -> {
                if (isAllBlank(values)) {
                    return;
                }
                total[0]++;

                int before = errors.size();

                requireField(values, rowNumber, "scheme_id", errors);
                requireField(values, rowNumber, "parent_lgd_id", errors);
                requireField(values, rowNumber, "parent_lgd_level", errors);
                if (includeDepartment) {
                    requireField(values, rowNumber, "parent_department_id", errors);
                    requireField(values, rowNumber, "parent_department_level", errors);
                }

                Integer schemeId = parseInteger(values.get("scheme_id"), rowNumber, "scheme_id", errors);
                Integer parentLgdId = parseInteger(values.get("parent_lgd_id"), rowNumber, "parent_lgd_id", errors);
                Integer parentLgdLevel = parseInteger(values.get("parent_lgd_level"), rowNumber, "parent_lgd_level", errors);
                Integer parentDepartmentId = includeDepartment
                        ? parseInteger(values.get("parent_department_id"), rowNumber, "parent_department_id", errors)
                        : null;
                String parentDepartmentLevel = includeDepartment ? normalize(values.get("parent_department_level")) : "";

                boolean rowHasErrors = errors.size() != before;
                if (!rowHasErrors) {
                    if (parentLgdLevel == null || parentLgdLevel < 1 || parentLgdLevel > 6) {
                        errors.add(error(rowNumber, "parent_lgd_level", "parent_lgd_level must be between 1 and 6"));
                        rowHasErrors = true;
                    }
                    if (includeDepartment && parentDepartmentLevel.isBlank()) {
                        errors.add(error(rowNumber, "parent_department_level", "parent_department_level is required"));
                        rowHasErrors = true;
                    }
                }

                if (!rowHasErrors) {
                    chunk.add(new MappingRow(rowNumber, schemeId, parentLgdId, parentLgdLevel, parentDepartmentId, parentDepartmentLevel));
                }

                if (chunk.size() >= CHUNK_SIZE) {
                    validateMappingChunk(schemaName, chunk, includeDepartment, errors);
                    chunk.clear();
                }

                if (errors.size() > before && errors.size() >= MAX_VALIDATION_ERRORS) {
                    errors.add(error(rowNumber, "file", "Too many validation errors; showing first " + MAX_VALIDATION_ERRORS));
                    throw new TooManyErrorsException();
                }
            });
        } catch (TooManyErrorsException ignored) {
            // stop early (errors already captured)
        } catch (IOException ex) {
            throw new FileValidationException(
                    "Failed to read uploaded file",
                    List.of(error(0, "file", "Unable to read file content"))
            );
        }

        if (!chunk.isEmpty()) {
            validateMappingChunk(schemaName, chunk, includeDepartment, errors);
        }

        if (!errors.isEmpty()) {
            throw new FileValidationException("Validation failed for uploaded file", errors);
        }
        if (total[0] == 0) {
            throw new FileValidationException(
                    "No data rows found in uploaded file",
                    List.of(error(0, "file", "At least one data row is required"))
            );
        }
        return total[0];
    }

    private void validateMappingChunk(
            String schemaName,
            List<MappingRow> rows,
            boolean includeDepartment,
            List<SchemeUploadErrorDTO> errors
    ) {
        List<Integer> schemeIds = new ArrayList<>(rows.size());
        List<Integer> lgdIds = new ArrayList<>(rows.size());
        List<Integer> deptIds = includeDepartment ? new ArrayList<>(rows.size()) : List.of();

        for (MappingRow r : rows) {
            schemeIds.add(r.schemeId());
            lgdIds.add(r.parentLgdId());
            if (includeDepartment) {
                deptIds.add(r.parentDepartmentId());
            }
        }

        Set<Integer> existingSchemes = schemeDbRepository.findExistingSchemeIds(schemaName, schemeIds);
        Set<Integer> existingLgds = schemeDbRepository.findExistingLgdLocationIds(schemaName, lgdIds);
        Set<Integer> existingDepts = includeDepartment
                ? schemeDbRepository.findExistingDepartmentLocationIds(schemaName, deptIds)
                : Set.of();

        for (MappingRow r : rows) {
            if (!existingSchemes.contains(r.schemeId())) {
                errors.add(error(r.rowNumber(), "scheme_id", "scheme_id does not exist"));
                continue;
            }
            if (!existingLgds.contains(r.parentLgdId())) {
                errors.add(error(r.rowNumber(), "parent_lgd_id", "parent_lgd_id does not exist"));
                continue;
            }
            if (includeDepartment && (r.parentDepartmentId() == null || !existingDepts.contains(r.parentDepartmentId()))) {
                errors.add(error(r.rowNumber(), "parent_department_id", "parent_department_id does not exist"));
            }
        }
    }

    private int processMappings(
            String schemaName,
            MultipartFile file,
            String extension,
            List<String> activeHeaders,
            boolean includeDepartment,
            int actorUserId
    ) {
        List<MappingRow> chunk = new ArrayList<>(CHUNK_SIZE);
        final int[] uploaded = {0};

        try {
            streamRows(file, extension, activeHeaders, (rowNumber, values) -> {
                if (isAllBlank(values)) {
                    return;
                }

                Integer schemeId = Integer.parseInt(normalize(values.get("scheme_id")));
                Integer parentLgdId = Integer.parseInt(normalize(values.get("parent_lgd_id")));
                Integer parentLgdLevel = Integer.parseInt(normalize(values.get("parent_lgd_level")));
                Integer parentDepartmentId = includeDepartment
                        ? Integer.parseInt(normalize(values.get("parent_department_id")))
                        : null;
                String parentDepartmentLevel = includeDepartment ? normalize(values.get("parent_department_level")) : "";

                chunk.add(new MappingRow(rowNumber, schemeId, parentLgdId, parentLgdLevel, parentDepartmentId, parentDepartmentLevel));
                if (chunk.size() >= CHUNK_SIZE) {
                    insertMappingChunk(schemaName, chunk, includeDepartment, actorUserId);
                    uploaded[0] += chunk.size();
                    chunk.clear();
                }
            });
        } catch (IOException ex) {
            throw new FileValidationException(
                    "Failed to read uploaded file",
                    List.of(error(0, "file", "Unable to read file content"))
            );
        }

        if (!chunk.isEmpty()) {
            insertMappingChunk(schemaName, chunk, includeDepartment, actorUserId);
            uploaded[0] += chunk.size();
        }
        return uploaded[0];
    }

    private void insertMappingChunk(String schemaName, List<MappingRow> rows, boolean includeDepartment, int actorUserId) {
        List<SchemeLgdMappingCreateRecord> lgd = new ArrayList<>(rows.size());
        List<SchemeSubdivisionMappingCreateRecord> dept = includeDepartment ? new ArrayList<>(rows.size()) : List.of();

        for (MappingRow r : rows) {
            lgd.add(new SchemeLgdMappingCreateRecord(
                    r.schemeId(),
                    r.parentLgdId(),
                    r.parentLgdLevel(),
                    actorUserId,
                    actorUserId
            ));
            if (includeDepartment) {
                dept.add(new SchemeSubdivisionMappingCreateRecord(
                        r.schemeId(),
                        r.parentDepartmentId(),
                        r.parentDepartmentLevel(),
                        actorUserId,
                        actorUserId
                ));
            }
        }

        chunkProcessor.insertMappingsChunk(schemaName, lgd, dept);
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

    private void requireField(Map<String, String> values, int rowNumber, String field, List<SchemeUploadErrorDTO> errors) {
        if (normalize(values.get(field)).isBlank()) {
            errors.add(error(rowNumber, field, field + " is required"));
        }
    }

    private Integer parseInteger(String value, int rowNumber, String field, List<SchemeUploadErrorDTO> errors) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            errors.add(error(rowNumber, field, field + " must be a valid integer"));
            return null;
        }
    }

    private Double parseDouble(String value, int rowNumber, String field, List<SchemeUploadErrorDTO> errors) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ex) {
            errors.add(error(rowNumber, field, field + " must be a valid decimal number"));
            return null;
        }
    }

    private Integer parseEnum(
            String value,
            int rowNumber,
            String field,
            Map<String, Integer> mapping,
            String expected,
            List<SchemeUploadErrorDTO> errors
    ) {
        String normalized = normalize(value).toLowerCase();
        if (normalized.isBlank()) {
            return null;
        }

        Integer mappedValue = mapping.get(normalized);
        if (mappedValue == null) {
            errors.add(error(rowNumber, field, "Invalid " + field + ". Expected: " + expected));
        }
        return mappedValue;
    }

    private SchemeUploadErrorDTO error(int rowNumber, String field, String message) {
        return SchemeUploadErrorDTO.builder()
                .rowNumber(rowNumber)
                .field(field)
                .message(message)
                .build();
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isAllBlank(Map<String, String> values) {
        for (String v : values.values()) {
            if (v != null && !v.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private void streamRows(MultipartFile file, String extension, List<String> activeHeaders, RowConsumer consumer) throws IOException {
        if ("csv".equals(extension)) {
            streamCsv(file, activeHeaders, consumer);
            return;
        }
        streamXlsx(file, activeHeaders, consumer);
    }

    private void streamCsv(MultipartFile file, List<String> activeHeaders, RowConsumer consumer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder().setTrim(true).setIgnoreSurroundingSpaces(true).build().parse(reader)) {

            var it = parser.iterator();
            if (!it.hasNext()) {
                return;
            }
            it.next(); // header

            while (it.hasNext()) {
                CSVRecord record = it.next();
                Map<String, String> values = rowAsMap(indexedValues(record, activeHeaders), activeHeaders);
                consumer.accept((int) record.getRecordNumber(), values);
            }
        }
    }

    private void streamXlsx(MultipartFile file, List<String> activeHeaders, RowConsumer consumer) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return;
            }

            int firstRowIndex = sheet.getFirstRowNum();
            for (int i = firstRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                Map<String, String> values = rowAsMap(indexedValues(row, activeHeaders), activeHeaders);
                consumer.accept(i + 1, values);
            }
        }
    }

    private record MappingRow(
            int rowNumber,
            Integer schemeId,
            Integer parentLgdId,
            Integer parentLgdLevel,
            Integer parentDepartmentId,
            String parentDepartmentLevel
    ) {
    }

    @FunctionalInterface
    private interface RowConsumer {
        void accept(int rowNumber, Map<String, String> values);
    }

    private static final class TooManyErrorsException extends RuntimeException {
    }
}
