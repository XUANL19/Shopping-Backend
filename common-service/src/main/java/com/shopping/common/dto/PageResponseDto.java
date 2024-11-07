package com.shopping.common.dto;

import lombok.Data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import java.util.List;

@Data
public class PageResponseDto<T> {
    private int pageNumber;
    private int pageSize;
    private Long totalElements;  // Make it nullable
    private Integer totalPages;  // Make it nullable
    private boolean hasNext;
    private List<T> content;

    public static <T> PageResponseDto<T> fromPage(Page<T> page) {
        PageResponseDto<T> response = new PageResponseDto<>();
        response.setContent(page.getContent());
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setHasNext(page.hasNext());
        return response;
    }

    public static <T> PageResponseDto<T> fromSlice(Slice<T> slice) {
        PageResponseDto<T> response = new PageResponseDto<>();
        response.setContent(slice.getContent());
        response.setPageNumber(slice.getNumber());
        response.setPageSize(slice.getSize());
        response.setHasNext(slice.hasNext());
        // totalElements and totalPages will remain null for Slice
        return response;
    }
}