package org.arghyam.jalsoochak.user.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

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
        if ((totalPages == 0 && page != 0) || (totalPages > 0 && page >= totalPages)) {
            throw new IllegalArgumentException(
                    "page " + page + " is out of range; totalPages is " + totalPages);
        }
        return PageResponseDTO.<T>builder()
                .content(content)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .number(page)
                .size(size)
                .build();
    }
}
