package com.shopping.common.security;

import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

public class SecurityUtils {
    public static UUID getCurrentUserId() {
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return UUID.fromString(userId);
    }

    public static String getCurrentUserEmail() {
        return (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
    }
}