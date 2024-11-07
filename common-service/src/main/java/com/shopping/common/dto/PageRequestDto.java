package com.shopping.common.dto;

import lombok.Data;

@Data
public class PageRequestDto {
    private int pageNumber = 0;
    private int pageSize = 20;

    public void validate() {
        if (pageSize <= 0) {
            pageSize = 20;
        } else if (pageSize > 100) { // maximum page size
            pageSize = 100;
        }
        if (pageNumber < 0) {
            pageNumber = 0;
        }
    }
}