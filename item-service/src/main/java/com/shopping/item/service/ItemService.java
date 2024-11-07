package com.shopping.item.service;

import com.shopping.item.constants.ErrorMessages;
import com.shopping.item.dao.ItemRepository;
import com.shopping.item.dto.*;
import com.shopping.common.dto.PageRequestDto;
import com.shopping.common.dto.PageResponseDto;
import com.shopping.item.entity.Item;
import com.shopping.item.exception.DuplicateUpcException;
import com.shopping.item.exception.ItemNotFoundException;
import com.shopping.item.exception.InvalidItemDataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;

    public PageResponseDto<ItemDto> getAllItems(PageRequestDto pageRequest) {
        pageRequest.validate();
        Page<Item> itemPage = itemRepository.findAll(
                PageRequest.of(
                        pageRequest.getPageNumber(),
                        pageRequest.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "upc")
                )
        );
        return createPageResponse(itemPage);
    }

    public ItemDto getItemByUpc(Long upc) {
        Item item = itemRepository.findByUpc(upc)
                .orElseThrow(() -> new ItemNotFoundException(String.format(ErrorMessages.ITEM_NOT_FOUND, upc)));
        return mapToDto(item);
    }

    public PageResponseDto<ItemDto> getItemsByName(String name, PageRequestDto pageRequest) {
        pageRequest.validate();
        Page<Item> itemPage = itemRepository.findByItemNameContainingIgnoreCase(
                name,
                PageRequest.of(
                        pageRequest.getPageNumber(),
                        pageRequest.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "upc")
                )
        );
        return createPageResponse(itemPage);
    }

    public PageResponseDto<ItemDto> getItemsByCategory(String category, PageRequestDto pageRequest) {
        pageRequest.validate();
        Page<Item> itemPage = itemRepository.findByCategory(
                category,
                PageRequest.of(
                        pageRequest.getPageNumber(),
                        pageRequest.getPageSize(),
                        Sort.by(Sort.Direction.ASC, "upc")
                )
        );
        return createPageResponse(itemPage);
    }

    @Transactional
    public ItemDto createItem(ItemDto itemDto) {
        if (itemRepository.existsByUpc(itemDto.getUpc())) {
            throw new DuplicateUpcException(String.format(ErrorMessages.DUPLICATE_UPC, itemDto.getUpc()));
        }

        validatePurchaseAndInventoryCnt(itemDto);
        Item item = mapToEntity(itemDto);
        Item savedItem = itemRepository.save(item);
        return mapToDto(savedItem);
    }

    @Transactional
    public ItemDto updateItem(Long upc, ItemUpdateDto updateDto) {
        Item item = itemRepository.findByUpc(upc)
                .orElseThrow(() -> new ItemNotFoundException(String.format(ErrorMessages.ITEM_NOT_FOUND, upc)));

        validateItemUpdate(item, updateDto);
        updateItemFields(item, updateDto);
        Item updatedItem = itemRepository.save(item);
        return mapToDto(updatedItem);
    }

    @Transactional
    public void deleteItem(Long upc) {
        Item item = itemRepository.findByUpc(upc)
                .orElseThrow(() -> new ItemNotFoundException(String.format(ErrorMessages.ITEM_NOT_FOUND, upc)));
        itemRepository.delete(item);
    }

    @Transactional
    public void processOrderPaid(OrderEventDto orderEvent) {
        for (OrderItemDto orderItem : orderEvent.getItems()) {
            Item item = itemRepository.findByUpc(orderItem.getUpc())
                    .orElseThrow(() -> new ItemNotFoundException(
                            String.format(ErrorMessages.ITEM_NOT_FOUND, orderItem.getUpc())));

            // Calculate new inventory
            int newInventory = item.getInventory() - orderItem.getPurchaseCount();
            if (newInventory < 0) {
                throw new InvalidItemDataException(ErrorMessages.NEGATIVE_INVENTORY);
            }

            // Update inventory
            item.setInventory(newInventory);

            // If current purchaseLimit is greater than new inventory, update it
            if (item.getPurchaseLimit() > newInventory) {
                item.setPurchaseLimit(newInventory);
            }

            itemRepository.save(item);
        }
    }

    private void validatePurchaseAndInventoryCnt(ItemDto itemDto) {
        if (itemDto.getInventory() < 0) {
            throw new InvalidItemDataException(ErrorMessages.NEGATIVE_INVENTORY);
        }
        if (itemDto.getPurchaseLimit() < 0) {
            throw new InvalidItemDataException(ErrorMessages.NEGATIVE_PURCHASE_LIMIT);
        }
        if (itemDto.getPurchaseLimit() > itemDto.getInventory()) {
            throw new InvalidItemDataException(ErrorMessages.INVALID_PURCHASE_LIMIT);
        }
    }

    private void validateItemUpdate(Item item, ItemUpdateDto updateDto) {
        Integer newInventory = updateDto.getInventory();
        Integer newPurchaseLimit = updateDto.getPurchaseLimit();

        // If updating inventory
        if (newInventory != null && newInventory < 0) {
            throw new InvalidItemDataException(ErrorMessages.NEGATIVE_INVENTORY);
        }

        // If updating purchase limit
        if (newPurchaseLimit != null) {
            int inventoryToCompare = newInventory != null ? newInventory : item.getInventory();
            if (newPurchaseLimit > inventoryToCompare) {
                throw new InvalidItemDataException(String.format(ErrorMessages.INVALID_PURCHASE_LIMIT));
            }
        }

        // If only updating inventory, check against existing purchase limit
        if (newInventory != null && newPurchaseLimit == null && newInventory < item.getPurchaseLimit()) {
            throw new InvalidItemDataException(ErrorMessages.INVALID_PURCHASE_LIMIT);
        }
    }

    private PageResponseDto<ItemDto> createPageResponse(Page<Item> itemPage) {
        PageResponseDto<ItemDto> response = new PageResponseDto<>();
        response.setPageNumber(itemPage.getNumber());
        response.setPageSize(itemPage.getSize());
        response.setTotalElements(itemPage.getTotalElements());
        response.setTotalPages(itemPage.getTotalPages());
        response.setHasNext(itemPage.hasNext());
        response.setContent(itemPage.getContent().stream().map(this::mapToDto).toList());
        return response;
    }

    private ItemDto mapToDto(Item item) {
        ItemDto dto = new ItemDto();
        dto.setUpc(item.getUpc());
        dto.setItemName(item.getItemName());
        dto.setPrice(item.getPrice());
        dto.setPurchaseLimit(item.getPurchaseLimit());
        dto.setCategory(item.getCategory());
        dto.setInventory(item.getInventory());
        return dto;
    }

    private Item mapToEntity(ItemDto dto) {
        Item item = new Item();
        item.setUpc(dto.getUpc());
        item.setItemName(dto.getItemName());
        item.setPrice(dto.getPrice());
        item.setPurchaseLimit(dto.getPurchaseLimit());
        item.setCategory(dto.getCategory());
        item.setInventory(dto.getInventory());
        return item;
    }

    private void updateItemFields(Item item, ItemUpdateDto updateDto) {
        if (updateDto.getItemName() != null) {
            item.setItemName(updateDto.getItemName());
        }
        if (updateDto.getPrice() != null) {
            item.setPrice(updateDto.getPrice());
        }
        if (updateDto.getPurchaseLimit() != null) {
            item.setPurchaseLimit(updateDto.getPurchaseLimit());
        }
        if (updateDto.getCategory() != null) {
            item.setCategory(updateDto.getCategory());
        }
        if (updateDto.getInventory() != null) {
            item.setInventory(updateDto.getInventory());
        }
    }
}