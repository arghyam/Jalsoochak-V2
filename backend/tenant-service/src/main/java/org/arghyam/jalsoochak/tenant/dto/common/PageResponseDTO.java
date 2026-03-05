package org.arghyam.jalsoochak.tenant.dto.common;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generic pagination response wrapper")
public class PageResponseDTO<T> {

    @Schema(description = "List of items in the current page")
    private List<T> content;

    @Schema(description = "Total number of elements across all pages", example = "100")
    private long totalElements;

    @Schema(description = "Total number of pages", example = "10")
    private int totalPages;

    @Schema(description = "Requested page size", example = "10")
    private int size;

    @Schema(description = "Current page number (0-indexed)", example = "0")
    private int number;

    /**
     * Helper method to create a PageResponseDTO.
     */
    public static <T> PageResponseDTO<T> of(List<T> content, long totalElements, int page, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be non-negative");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements must be non-negative");
        }
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int clampedPage = totalPages == 0 ? 0 : Math.min(page, totalPages - 1);
        return PageResponseDTO.<T>builder()
                .content(content)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .number(clampedPage)
                .size(size)
                .build();
    }
}
