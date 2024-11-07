package com.shopping.item.constants;

public class ErrorMessages {
    public static final String ITEM_NOT_FOUND = "Item not found with UPC: %s";
    public static final String DUPLICATE_UPC = "Item with UPC %s already exists";
    public static final String INVALID_PURCHASE_LIMIT = "Purchase limit cannot be greater than inventory";
    public static final String NEGATIVE_PURCHASE_LIMIT = "Purchase limit cannot be less than 0";
    public static final String NEGATIVE_INVENTORY = "Inventory cannot be less than 0";
}