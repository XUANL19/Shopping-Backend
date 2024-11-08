package com.shopping.item.service;

import com.shopping.common.dto.PageRequestDto;
import com.shopping.common.dto.PageResponseDto;
import com.shopping.item.constants.ErrorMessages;
import com.shopping.item.dao.ItemRepository;
import com.shopping.item.dto.ItemDto;
import com.shopping.item.dto.ItemUpdateDto;
import com.shopping.item.dto.OrderEventDto;
import com.shopping.item.dto.OrderItemDto;
import com.shopping.item.entity.Item;
import com.shopping.item.exception.DuplicateUpcException;
import com.shopping.item.exception.InvalidItemDataException;
import com.shopping.item.exception.ItemNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item testItem;
    private ItemDto testItemDto;
    private PageRequestDto pageRequestDto;

    private static final Long TEST_UPC = 1234567890L;

    @BeforeEach
    void setUp() {
        // Setup test Item
        testItem = new Item();
        testItem.setUpc(TEST_UPC);
        testItem.setItemName("Test Item");
        testItem.setPrice(99.99);
        testItem.setPurchaseLimit(10);
        testItem.setCategory("Test Category");
        testItem.setInventory(20);

        // Setup test ItemDto
        testItemDto = new ItemDto();
        testItemDto.setUpc(TEST_UPC);
        testItemDto.setItemName("Test Item");
        testItemDto.setPrice(99.99);
        testItemDto.setPurchaseLimit(10);
        testItemDto.setCategory("Test Category");
        testItemDto.setInventory(20);

        // Setup PageRequestDto
        pageRequestDto = new PageRequestDto();
        pageRequestDto.setPageNumber(0);
        pageRequestDto.setPageSize(10);
    }

    @Test
    void getAllItems_ShouldReturnPageResponse() {
        // Arrange
        List<Item> items = Arrays.asList(testItem);
        Page<Item> itemPage = new PageImpl<>(items);
        when(itemRepository.findAll(any(PageRequest.class))).thenReturn(itemPage);

        // Act
        PageResponseDto<ItemDto> result = itemService.getAllItems(pageRequestDto);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(TEST_UPC, result.getContent().get(0).getUpc());
        verify(itemRepository).findAll(any(PageRequest.class));
    }

    @Test
    void getItemByUpc_WhenItemExists_ShouldReturnItem() {
        // Arrange
        when(itemRepository.findByUpc(TEST_UPC)).thenReturn(Optional.of(testItem));

        // Act
        ItemDto result = itemService.getItemByUpc(TEST_UPC);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_UPC, result.getUpc());
        verify(itemRepository).findByUpc(TEST_UPC);
    }

    @Test
    void getItemByUpc_WhenItemDoesNotExist_ShouldThrowException() {
        // Arrange
        when(itemRepository.findByUpc(TEST_UPC)).thenReturn(Optional.empty());

        // Act & Assert
        ItemNotFoundException exception = assertThrows(ItemNotFoundException.class,
                () -> itemService.getItemByUpc(TEST_UPC));
        assertEquals(String.format(ErrorMessages.ITEM_NOT_FOUND, TEST_UPC), exception.getMessage());
        verify(itemRepository).findByUpc(TEST_UPC);
    }

    @Test
    void createItem_WhenUpcNotExists_ShouldCreateItem() {
        // Arrange
        when(itemRepository.existsByUpc(TEST_UPC)).thenReturn(false);
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        // Act
        ItemDto result = itemService.createItem(testItemDto);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_UPC, result.getUpc());
        verify(itemRepository).existsByUpc(TEST_UPC);
        verify(itemRepository).save(any(Item.class));
    }

    @Test
    void createItem_WhenUpcExists_ShouldThrowException() {
        // Arrange
        when(itemRepository.existsByUpc(TEST_UPC)).thenReturn(true);

        // Act & Assert
        DuplicateUpcException exception = assertThrows(DuplicateUpcException.class,
                () -> itemService.createItem(testItemDto));
        assertEquals(String.format(ErrorMessages.DUPLICATE_UPC, TEST_UPC), exception.getMessage());
        verify(itemRepository).existsByUpc(TEST_UPC);
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void processOrderPaid_ShouldUpdateInventory() {
        // Arrange
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setUpc(TEST_UPC);
        orderItemDto.setPurchaseCount(5);

        OrderEventDto orderEventDto = new OrderEventDto();
        orderEventDto.setItems(List.of(orderItemDto));

        when(itemRepository.findByUpc(TEST_UPC)).thenReturn(Optional.of(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        // Act
        itemService.processOrderPaid(orderEventDto);

        // Assert
        assertEquals(15, testItem.getInventory()); // 20 - 5
        verify(itemRepository).findByUpc(TEST_UPC);
        verify(itemRepository).save(testItem);
    }

    @Test
    void processOrderPaid_WhenInsufficientInventory_ShouldThrowException() {
        // Arrange
        OrderItemDto orderItemDto = new OrderItemDto();
        orderItemDto.setUpc(TEST_UPC);
        orderItemDto.setPurchaseCount(25); // More than available inventory

        OrderEventDto orderEventDto = new OrderEventDto();
        orderEventDto.setItems(List.of(orderItemDto));

        when(itemRepository.findByUpc(TEST_UPC)).thenReturn(Optional.of(testItem));

        // Act & Assert
        assertThrows(InvalidItemDataException.class,
                () -> itemService.processOrderPaid(orderEventDto));
        verify(itemRepository).findByUpc(TEST_UPC);
        verify(itemRepository, never()).save(any(Item.class));
    }

    @Test
    void updateItem_ShouldUpdateFields() {
        // Arrange
        ItemUpdateDto updateDto = new ItemUpdateDto();
        updateDto.setItemName("Updated Name");
        updateDto.setPrice(199.99);

        when(itemRepository.findByUpc(TEST_UPC)).thenReturn(Optional.of(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        // Act
        ItemDto result = itemService.updateItem(TEST_UPC, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Name", result.getItemName());
        assertEquals(199.99, result.getPrice());
        verify(itemRepository).findByUpc(TEST_UPC);
        verify(itemRepository).save(any(Item.class));
    }
}