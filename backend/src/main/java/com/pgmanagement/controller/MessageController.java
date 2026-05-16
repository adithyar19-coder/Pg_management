package com.pgmanagement.controller;

import com.pgmanagement.dto.ApiResponse;
import com.pgmanagement.entity.DirectMessage;
import com.pgmanagement.entity.User;
import com.pgmanagement.repository.UserRepository;
import com.pgmanagement.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final UserRepository userRepository;

    private User getUser(UserDetails ud) {
        return userRepository.findByEmail(ud.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /** Conversation summary list (sidebar of chat page). */
    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> conversations(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", messageService.getConversations(getUser(ud))));
    }

    /** Eligible new-chat partners (owner ↔ tenants under their PGs, tenant ↔ their owner). */
    @GetMapping("/partners")
    public ResponseEntity<ApiResponse<List<User>>> partners(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", messageService.getEligiblePartners(getUser(ud))));
    }

    /** Full message history with a specific user (auto-marks received messages as read). */
    @GetMapping("/with/{userId}")
    public ResponseEntity<ApiResponse<List<DirectMessage>>> conversation(@PathVariable Long userId,
                                                                          @AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", messageService.getConversation(getUser(ud), userId)));
    }

    /** Send a message. */
    @PostMapping
    public ResponseEntity<ApiResponse<DirectMessage>> send(@AuthenticationPrincipal UserDetails ud,
                                                            @RequestBody Map<String, Object> body) {
        Object ridObj = body.get("recipientId");
        if (ridObj == null) throw new IllegalArgumentException("recipientId is required");
        Long recipientId = Long.valueOf(ridObj.toString());
        String text = body.get("body") != null ? body.get("body").toString() : "";
        return ResponseEntity.ok(ApiResponse.success("Sent",
                messageService.send(getUser(ud), recipientId, text)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(@AuthenticationPrincipal UserDetails ud) {
        return ResponseEntity.ok(ApiResponse.success("OK", messageService.getUnreadCount(getUser(ud))));
    }
}
