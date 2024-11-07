package com.shopping.item.controller;

import com.shopping.item.dto.*;
import com.shopping.common.dto.PageResponseDto;
import com.shopping.common.dto.PageRequestDto;
import com.shopping.common.dto.ApiResponseDto;
import com.shopping.item.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @GetMapping
    public ResponseEntity<ApiResponseDto<PageResponseDto<ItemDto>>> getAllItems(
            @RequestParam(required = false, defaultValue = "0") Integer pageNumber,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageNumber(pageNumber);
        pageRequest.setPageSize(pageSize);
        PageResponseDto<ItemDto> items = itemService.getAllItems(pageRequest);
        return ResponseEntity.ok(ApiResponseDto.success("Items retrieved successfully", items));
    }

    @GetMapping("/upc/{upc}")
    public ResponseEntity<ApiResponseDto<ItemDto>> getItemByUpc(@PathVariable Long upc) {
        ItemDto item = itemService.getItemByUpc(upc);
        return ResponseEntity.ok(ApiResponseDto.success("Item retrieved successfully", item));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponseDto<PageResponseDto<ItemDto>>> searchByName(
            @RequestParam(required = true) String name,
            @RequestParam(required = false, defaultValue = "0") Integer pageNumber,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageNumber(pageNumber);
        pageRequest.setPageSize(pageSize);
        PageResponseDto<ItemDto> items = itemService.getItemsByName(name, pageRequest);
        return ResponseEntity.ok(ApiResponseDto.success("Items retrieved successfully", items));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponseDto<PageResponseDto<ItemDto>>> getByCategory(
            @PathVariable String category,
            @RequestParam(required = false, defaultValue = "0") Integer pageNumber,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize) {
        PageRequestDto pageRequest = new PageRequestDto();
        pageRequest.setPageNumber(pageNumber);
        pageRequest.setPageSize(pageSize);
        PageResponseDto<ItemDto> items = itemService.getItemsByCategory(category, pageRequest);
        return ResponseEntity.ok(ApiResponseDto.success("Items retrieved successfully", items));
    }

    @PostMapping
    public ResponseEntity<ApiResponseDto<ItemDto>> createItem(@Valid @RequestBody ItemDto itemDto) {
        ItemDto createdItem = itemService.createItem(itemDto);
        return new ResponseEntity<>(
                ApiResponseDto.created("Item created successfully", createdItem),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/upc/{upc}")
    public ResponseEntity<ApiResponseDto<ItemDto>> updateItem(
            @PathVariable Long upc,
            @Valid @RequestBody ItemUpdateDto updateDto) {
        ItemDto updatedItem = itemService.updateItem(upc, updateDto);
        return ResponseEntity.ok(ApiResponseDto.success("Item updated successfully", updatedItem));
    }

    @DeleteMapping("/upc/{upc}")
    public ResponseEntity<ApiResponseDto<Void>> deleteItem(@PathVariable Long upc) {
        itemService.deleteItem(upc);
        return ResponseEntity.ok(ApiResponseDto.success("Item deleted successfully"));
    }
}