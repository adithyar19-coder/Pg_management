package com.pgmanagement.service;

import com.pgmanagement.entity.DirectMessage;
import com.pgmanagement.entity.RoomAssignment;
import com.pgmanagement.entity.User;
import com.pgmanagement.repository.DirectMessageRepository;
import com.pgmanagement.repository.RoomAssignmentRepository;
import com.pgmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Private 1-to-1 messaging service.
 *
 * <p>Access control: a tenant can DM their PG owner only. An owner can DM any tenant
 * currently assigned to one of their PGs. Other pairs are rejected.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final DirectMessageRepository messageRepo;
    private final UserRepository userRepo;
    private final RoomAssignmentRepository roomAssignmentRepo;

    /** Send a message after validating the sender/recipient relationship. */
    @Transactional
    public DirectMessage send(User sender, Long recipientId, String body) {
        if (body == null || body.isBlank()) throw new IllegalArgumentException("Message body is required");
        User recipient = userRepo.findById(recipientId)
                .orElseThrow(() -> new RuntimeException("Recipient not found"));
        if (sender.getId().equals(recipient.getId())) {
            throw new IllegalArgumentException("Cannot send a message to yourself");
        }
        if (!canChat(sender, recipient)) {
            throw new RuntimeException("You can only chat with " + (sender.getRole() == User.Role.TENANT
                    ? "your PG owner" : "tenants currently assigned to one of your PGs"));
        }
        return messageRepo.save(DirectMessage.builder()
                .sender(sender)
                .recipient(recipient)
                .body(body.trim())
                .isRead(false)
                .build());
    }

    /** Full chronological chat between two users + mark received messages as read. */
    @Transactional
    public List<DirectMessage> getConversation(User me, Long otherId) {
        User other = userRepo.findById(otherId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!canChat(me, other)) throw new RuntimeException("Not authorized to view this conversation");

        List<DirectMessage> messages = messageRepo.findConversationBetween(me, other);
        // Mark messages addressed to me as read
        for (DirectMessage m : messages) {
            if (m.getRecipient().getId().equals(me.getId()) && !Boolean.TRUE.equals(m.getIsRead())) {
                m.setIsRead(true);
                messageRepo.save(m);
            }
        }
        return messages;
    }

    /**
     * Conversation summaries: for each distinct partner the user has messaged,
     * return a small map with partner details + last message + unread count.
     */
    public List<Map<String, Object>> getConversations(User me) {
        List<DirectMessage> all = messageRepo.findAllInvolving(me);

        // Map partnerId → (lastMessage, unreadCount, partner)
        Map<Long, Map<String, Object>> byPartner = new LinkedHashMap<>();
        for (DirectMessage m : all) {
            User partner = m.getSender().getId().equals(me.getId()) ? m.getRecipient() : m.getSender();
            Long pid = partner.getId();
            Map<String, Object> entry = byPartner.computeIfAbsent(pid, k -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("partnerId", partner.getId());
                map.put("partnerName", partner.getName());
                map.put("partnerEmail", partner.getEmail());
                map.put("partnerRole", partner.getRole().name());
                map.put("lastMessage", m.getBody());
                map.put("lastMessageAt", m.getCreatedAt());
                map.put("lastFromMe", m.getSender().getId().equals(me.getId()));
                map.put("unreadCount", 0);
                return map;
            });
            // Count messages the user hasn't read
            if (!Boolean.TRUE.equals(m.getIsRead()) && m.getRecipient().getId().equals(me.getId())) {
                entry.put("unreadCount", (int) entry.get("unreadCount") + 1);
            }
        }
        return new ArrayList<>(byPartner.values());
    }

    /**
     * Eligible chat partners — tenants see only their owner; owners see all their
     * active tenants. Used by the "Start new conversation" picker in the UI.
     */
    public List<User> getEligiblePartners(User me) {
        if (me.getRole() == User.Role.TENANT) {
            return roomAssignmentRepo.findByTenantAndIsActive(me, true)
                    .map(a -> List.of(a.getRoom().getPg().getOwner()))
                    .orElse(List.of());
        }
        // Owner → list every active tenant across their PGs
        return roomAssignmentRepo.findByRoom_PgOwnerAndIsActive(me, true).stream()
                .map(RoomAssignment::getTenant)
                .distinct()
                .toList();
    }

    public long getUnreadCount(User me) {
        return messageRepo.countByRecipientAndIsRead(me, false);
    }

    /** True iff sender and recipient have a permitted tenant-owner relationship. */
    private boolean canChat(User a, User b) {
        // Identify (owner, tenant) pair
        User owner, tenant;
        if (a.getRole() == User.Role.OWNER && b.getRole() == User.Role.TENANT) { owner = a; tenant = b; }
        else if (a.getRole() == User.Role.TENANT && b.getRole() == User.Role.OWNER) { owner = b; tenant = a; }
        else return false;

        return roomAssignmentRepo.findByTenantAndIsActive(tenant, true)
                .map(asn -> asn.getRoom().getPg().getOwner().getId().equals(owner.getId()))
                .orElse(false);
    }
}
