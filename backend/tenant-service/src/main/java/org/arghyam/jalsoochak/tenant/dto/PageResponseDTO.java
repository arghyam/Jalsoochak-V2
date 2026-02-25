package org.arghyam.jalsoochak.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    @Schema(description = "Number of elements in the current page", example = "10")
    private int size;

    @Schema(description = "Current page number (0-indexed)", example = "0")
    private int number;

    /**
     * Helper method to create a PageResponseDTO.
     */
    public static <T> PageResponseDTO<T> of(List<T> content, long totalElements, int page, int size) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return PageResponseDTO.<T>builder()
                .content(content)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .number(page)
                .size(size)
                .build();
    }
}
