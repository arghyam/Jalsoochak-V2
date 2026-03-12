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
import org.arghyam.jalsoochak.scheme.repository.SchemeSubdivisionMappingCreateRecord;
import org.arghyam.jalsoochak.scheme.repository.SchemeVillageMappingCreateRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

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
            "village_id",
            "subdivision_id"
    );
    private static final List<String> MAPPING_HEADERS_V1 = List.of(
            "scheme_id",
            "village_id",
            "subdivision_id",
            "created_by",
            "updated_by"
    );
    private static final String VILLAGE_LEVEL = "Village";
    private static final String SUBDIVISION_LEVEL = "Sub-division";

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

    @Override
    public List<SchemeDTO> getAllSchemes() {
        String schemaName = requireTenantSchema();
        return schemeDbRepository.findAllSchemes(schemaName);
    }

    @Override
    @Transactional
    public SchemeUploadResponseDTO uploadSchemes(MultipartFile file, String authorizationHeader) {
        String schemaName = requireTenantSchema();
        int actorUserId = uploadAuthService.requireStateAdminUserId(schemaName, authorizationHeader);
        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        List<ParsedSchemeRow> parsedRows = parseRows(file, extension, List.of(SCHEME_HEADERS_V2, SCHEME_HEADERS_V1));
        List<SchemeCreateRecord> rowsToInsert = validateAndBuildRows(parsedRows, actorUserId);

        schemeDbRepository.insertSchemes(schemaName, rowsToInsert);

        return SchemeUploadResponseDTO.builder()
                .message("Schemes uploaded successfully")
                .totalRows(parsedRows.size())
                .uploadedRows(rowsToInsert.size())
                .build();
    }

    @Override
    @Transactional
    public SchemeUploadResponseDTO uploadSchemeMappings(MultipartFile file, String authorizationHeader) {
        String schemaName = requireTenantSchema();
        int actorUserId = uploadAuthService.requireStateAdminUserId(schemaName, authorizationHeader);
        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        List<ParsedSchemeRow> parsedRows = parseRows(file, extension, List.of(MAPPING_HEADERS_V2, MAPPING_HEADERS_V1));
        MappingUploadPayload payload = validateAndBuildMappingRows(schemaName, parsedRows, actorUserId);

        schemeDbRepository.insertVillageMappings(schemaName, payload.villageRows());
        schemeDbRepository.insertSubdivisionMappings(schemaName, payload.subdivisionRows());

        return SchemeUploadResponseDTO.builder()
                .message("Scheme mappings uploaded successfully")
                .totalRows(parsedRows.size())
                .uploadedRows(payload.villageRows().size())
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

    private List<ParsedSchemeRow> parseRows(MultipartFile file, String extension, List<List<String>> allowedHeaderVariants) {
        try {
            return "csv".equals(extension)
                    ? parseCsv(file, allowedHeaderVariants)
                    : parseXlsx(file, allowedHeaderVariants);
        } catch (IOException ex) {
            throw new FileValidationException(
                    "Failed to read uploaded file",
                    List.of(error(0, "file", "Unable to read file content"))
            );
        }
    }

    private List<ParsedSchemeRow> parseCsv(MultipartFile file, List<List<String>> allowedHeaderVariants) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder().setTrim(true).setIgnoreSurroundingSpaces(true).build().parse(reader)) {

            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                throw new FileValidationException(
                        "Uploaded file is empty",
                        List.of(error(0, "file", "Header row is missing"))
                );
            }

            List<String> headers = new ArrayList<>();
            records.getFirst().forEach(headers::add);
            List<String> activeHeaders = resolveHeaderVariant(headers, allowedHeaderVariants);

            List<ParsedSchemeRow> rows = new ArrayList<>();
            for (int i = 1; i < records.size(); i++) {
                CSVRecord record = records.get(i);
                rows.add(new ParsedSchemeRow(
                        (int) record.getRecordNumber(),
                        rowAsMap(indexedValues(record, activeHeaders), activeHeaders)
                ));
            }
            return rows;
        }
    }

    private List<ParsedSchemeRow> parseXlsx(MultipartFile file, List<List<String>> allowedHeaderVariants) throws IOException {
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

            List<String> activeHeaders = resolveHeaderVariant(readExcelHeaders(headerRow), allowedHeaderVariants);

            List<ParsedSchemeRow> rows = new ArrayList<>();
            for (int i = firstRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                rows.add(new ParsedSchemeRow(i + 1, rowAsMap(indexedValues(row, activeHeaders), activeHeaders)));
            }
            return rows;
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

    private List<SchemeCreateRecord> validateAndBuildRows(List<ParsedSchemeRow> rows, int actorUserId) {
        if (rows.isEmpty()) {
            throw new FileValidationException(
                    "No data rows found in uploaded file",
                    List.of(error(0, "file", "At least one data row is required"))
            );
        }

        List<SchemeUploadErrorDTO> errors = new ArrayList<>();
        List<SchemeCreateRecord> insertRows = new ArrayList<>();

        for (ParsedSchemeRow row : rows) {
            Map<String, String> values = row.values();

            requireField(values, row.rowNumber(), "state_scheme_id", errors);
            requireField(values, row.rowNumber(), "centre_scheme_id", errors);
            requireField(values, row.rowNumber(), "scheme_name", errors);
            requireField(values, row.rowNumber(), "fhtc_count", errors);
            requireField(values, row.rowNumber(), "planned_fhtc", errors);
            requireField(values, row.rowNumber(), "house_hold_count", errors);
            requireField(values, row.rowNumber(), "latitude", errors);
            requireField(values, row.rowNumber(), "longitude", errors);
            requireField(values, row.rowNumber(), "channel", errors);
            requireField(values, row.rowNumber(), "work_status", errors);
            requireField(values, row.rowNumber(), "operating_status", errors);

            Integer fhtcCount = parseInteger(values.get("fhtc_count"), row.rowNumber(), "fhtc_count", errors);
            Integer plannedFhtc = parseInteger(values.get("planned_fhtc"), row.rowNumber(), "planned_fhtc", errors);
            Integer houseHoldCount = parseInteger(values.get("house_hold_count"), row.rowNumber(), "house_hold_count", errors);
            Double latitude = parseDouble(values.get("latitude"), row.rowNumber(), "latitude", errors);
            Double longitude = parseDouble(values.get("longitude"), row.rowNumber(), "longitude", errors);
            Integer channel = parseEnum(values.get("channel"), row.rowNumber(), "channel", CHANNEL_MAP, "Bfm/Electric or 1/2", errors);
            Integer workStatus = parseEnum(values.get("work_status"), row.rowNumber(), "work_status", WORK_STATUS_MAP, "Ongoing, Completed, Not Started, Handed Over or 1/2/3/4", errors);
            Integer operatingStatus = parseEnum(values.get("operating_status"), row.rowNumber(), "operating_status", OPERATING_STATUS_MAP, "Operative, Non-Operative, Partially Operative or 1/2/3", errors);

            if (hasErrorsForRow(errors, row.rowNumber())) {
                continue;
            }

            insertRows.add(new SchemeCreateRecord(
                    UUID.randomUUID().toString(),
                    values.get("state_scheme_id"),
                    values.get("centre_scheme_id"),
                    values.get("scheme_name"),
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
        }

        if (!errors.isEmpty()) {
            throw new FileValidationException("Validation failed for uploaded file", errors);
        }

        return insertRows;
    }

    private MappingUploadPayload validateAndBuildMappingRows(String schemaName, List<ParsedSchemeRow> rows, int actorUserId) {
        if (rows.isEmpty()) {
            throw new FileValidationException(
                    "No data rows found in uploaded file",
                    List.of(error(0, "file", "At least one data row is required"))
            );
        }

        List<SchemeUploadErrorDTO> errors = new ArrayList<>();
        List<SchemeVillageMappingCreateRecord> villageRows = new ArrayList<>();
        List<SchemeSubdivisionMappingCreateRecord> subdivisionRows = new ArrayList<>();

        for (ParsedSchemeRow row : rows) {
            Map<String, String> values = row.values();

            requireField(values, row.rowNumber(), "scheme_id", errors);
            requireField(values, row.rowNumber(), "village_id", errors);
            requireField(values, row.rowNumber(), "subdivision_id", errors);

            Integer schemeId = parseInteger(values.get("scheme_id"), row.rowNumber(), "scheme_id", errors);
            Integer villageId = parseInteger(values.get("village_id"), row.rowNumber(), "village_id", errors);
            Integer subdivisionId = parseInteger(values.get("subdivision_id"), row.rowNumber(), "subdivision_id", errors);

            if (hasErrorsForRow(errors, row.rowNumber())) {
                continue;
            }

            if (!schemeDbRepository.existsSchemeById(schemaName, schemeId)) {
                errors.add(error(row.rowNumber(), "scheme_id", "scheme_id does not exist"));
                continue;
            }

            villageRows.add(new SchemeVillageMappingCreateRecord(
                    schemeId,
                    villageId,
                    VILLAGE_LEVEL,
                    actorUserId,
                    actorUserId
            ));
            subdivisionRows.add(new SchemeSubdivisionMappingCreateRecord(
                    schemeId,
                    subdivisionId,
                    SUBDIVISION_LEVEL,
                    actorUserId,
                    actorUserId
            ));
        }

        if (!errors.isEmpty()) {
            throw new FileValidationException("Validation failed for uploaded file", errors);
        }

        return new MappingUploadPayload(villageRows, subdivisionRows);
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

    private boolean hasErrorsForRow(List<SchemeUploadErrorDTO> errors, int rowNumber) {
        return errors.stream().anyMatch(error -> rowNumber == error.getRowNumber());
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

    private record ParsedSchemeRow(int rowNumber, Map<String, String> values) {
    }

    private record MappingUploadPayload(
            List<SchemeVillageMappingCreateRecord> villageRows,
            List<SchemeSubdivisionMappingCreateRecord> subdivisionRows
    ) {
    }
}
