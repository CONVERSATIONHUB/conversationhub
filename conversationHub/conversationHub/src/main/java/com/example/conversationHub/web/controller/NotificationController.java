package com.example.conversationHub.web.controller;

import com.example.conversationHub.service.NotificationService;
import com.example.conversationHub.web.dto.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ApiResponse mine(Authentication authentication) {
        return ApiResponse.ok("Notifications retrieved",
                notificationService.listMine(authentication.getName()));
    }

    @GetMapping("/unread-count")
    public ApiResponse unreadCount(Authentication authentication) {
        return ApiResponse.ok("Unread notification count",
                notificationService.unreadCount(authentication.getName()));
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse markRead(@PathVariable Long notificationId, Authentication authentication) {
        notificationService.markRead(notificationId, authentication.getName());
        return ApiResponse.ok("Notification marked as read", null);
    }

    @PostMapping("/read-all")
    public ApiResponse markAllRead(Authentication authentication) {
        int updated = notificationService.markAllRead(authentication.getName());
        return ApiResponse.ok("Notifications marked as read", updated);
    }
}
