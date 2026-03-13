package org.arghyam.jalsoochak.user.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponseDTO<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int size;
    private int number;

    public static <T> PageResponseDTO<T> of(List<T> content, long totalElements, int page, int size) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return PageResponseDTO.<T>builder()
                .content(content)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .number(page)
                .size(size)
                .build();
    }
}
