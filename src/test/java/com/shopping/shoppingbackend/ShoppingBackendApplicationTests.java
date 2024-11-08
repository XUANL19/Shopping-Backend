package com.shopping.shoppingbackend;

import org.junit.platform.suite.api.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Suite
@SuiteDisplayName("Shopping Backend All Service Layer Tests")
@SelectPackages({
		"com.shopping.account",
		"com.shopping.item",
		"com.shopping.order",
		"com.shopping.payment"
})
@IncludeClassNamePatterns({
		".*Service.*Test"
})
public class ShoppingBackendApplicationTests {

	@Test
	void contextLoads() {
		assertTrue(true, "Test Suite loaded successfully");
	}
}