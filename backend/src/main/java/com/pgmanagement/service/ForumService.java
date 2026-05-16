package com.pgmanagement.service;

import com.pgmanagement.entity.*;
import com.pgmanagement.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Community forum service. Per-PG posts with threaded replies.
 *
 * <ul>
 *   <li>OWNER: sees boards for all PGs they own, with optional pgId filter; can pin/delete any post in their PG.</li>
 *   <li>TENANT: sees only the board for the PG they're currently assigned to.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumPostRepository postRepo;
    private final ForumReplyRepository replyRepo;
    private final PGRepository pgRepo;
    private final RoomAssignmentRepository roomAssignmentRepo;

    /** Find the tenant's currently assigned PG, or throw. */
    private PG requireTenantPg(User tenant) {
        RoomAssignment a = roomAssignmentRepo.findByTenantAndIsActive(tenant, true)
                .orElseThrow(() -> new RuntimeException("You are not currently assigned to a PG; cannot use the community board."));
        return a.getRoom().getPg();
    }

    // ── List ─────────────────────────────────────────────
    public List<ForumPost> listForOwner(User owner, Long pgIdOrNull) {
        if (pgIdOrNull != null) {
            PG pg = pgRepo.findById(pgIdOrNull).orElseThrow(() -> new RuntimeException("PG not found"));
            if (!pg.getOwner().getId().equals(owner.getId())) throw new RuntimeException("Not authorized");
            return populateReplyCounts(postRepo.findByPgOrderByIsPinnedDescCreatedAtDesc(pg));
        }
        // All PGs the owner has → flatten
        List<PG> pgs = pgRepo.findByOwner(owner);
        return populateReplyCounts(pgs.stream()
                .flatMap(p -> postRepo.findByPgOrderByIsPinnedDescCreatedAtDesc(p).stream())
                .sorted((a, b) -> {
                    int p = Boolean.compare(Boolean.TRUE.equals(b.getIsPinned()), Boolean.TRUE.equals(a.getIsPinned()));
                    if (p != 0) return p;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .toList());
    }

    public List<ForumPost> listForTenant(User tenant) {
        PG pg = requireTenantPg(tenant);
        return populateReplyCounts(postRepo.findByPgOrderByIsPinnedDescCreatedAtDesc(pg));
    }

    private List<ForumPost> populateReplyCounts(List<ForumPost> posts) {
        if (posts.isEmpty()) return posts;
        List<ForumReply> replies = replyRepo.findByPostInOrderByCreatedAtAsc(posts);
        Map<Long, Long> counts = replies.stream()
                .collect(Collectors.groupingBy(r -> r.getPost().getId(), Collectors.counting()));
        for (ForumPost p : posts) p.setReplyCount(counts.getOrDefault(p.getId(), 0L).intValue());
        return posts;
    }

    // ── Post create / pin / delete ───────────────────────
    @Transactional
    public ForumPost createPost(User author, Long pgId, String title, String body) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required");

        PG pg;
        if (author.getRole() == User.Role.OWNER) {
            pg = pgRepo.findById(pgId).orElseThrow(() -> new RuntimeException("PG not found"));
            if (!pg.getOwner().getId().equals(author.getId())) throw new RuntimeException("Not authorized");
        } else {
            // Tenant must be assigned to the PG they're posting in.
            pg = requireTenantPg(author);
            if (pgId != null && !pgId.equals(pg.getId())) {
                throw new RuntimeException("You can only post in your own PG's board");
            }
        }
        return postRepo.save(ForumPost.builder()
                .pg(pg)
                .author(author)
                .title(title.trim())
                .body(body)
                .isPinned(false)
                .build());
    }

    @Transactional
    public ForumPost togglePin(Long postId, User owner) {
        ForumPost post = postRepo.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        if (!post.getPg().getOwner().getId().equals(owner.getId())) throw new RuntimeException("Not authorized");
        post.setIsPinned(!Boolean.TRUE.equals(post.getIsPinned()));
        return postRepo.save(post);
    }

    @Transactional
    public void deletePost(Long postId, User requester) {
        ForumPost post = postRepo.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        // Author or PG owner can delete
        boolean isOwner = post.getPg().getOwner().getId().equals(requester.getId());
        boolean isAuthor = post.getAuthor().getId().equals(requester.getId());
        if (!isOwner && !isAuthor) throw new RuntimeException("Not authorized");

        // Delete replies first (no cascade defined)
        replyRepo.deleteAll(replyRepo.findByPostOrderByCreatedAtAsc(post));
        postRepo.delete(post);
    }

    // ── Replies ──────────────────────────────────────────
    public List<ForumReply> listReplies(Long postId, User requester) {
        ForumPost post = postRepo.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        // Owner of that PG, or tenant assigned to that PG, can view
        checkPgAccess(requester, post.getPg());
        return replyRepo.findByPostOrderByCreatedAtAsc(post);
    }

    @Transactional
    public ForumReply addReply(Long postId, User author, String body) {
        if (body == null || body.isBlank()) throw new IllegalArgumentException("Reply body is required");
        ForumPost post = postRepo.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));
        checkPgAccess(author, post.getPg());
        return replyRepo.save(ForumReply.builder()
                .post(post)
                .author(author)
                .body(body.trim())
                .build());
    }

    @Transactional
    public void deleteReply(Long replyId, User requester) {
        ForumReply reply = replyRepo.findById(replyId).orElseThrow(() -> new RuntimeException("Reply not found"));
        boolean isOwner = reply.getPost().getPg().getOwner().getId().equals(requester.getId());
        boolean isAuthor = reply.getAuthor().getId().equals(requester.getId());
        if (!isOwner && !isAuthor) throw new RuntimeException("Not authorized");
        replyRepo.delete(reply);
    }

    private void checkPgAccess(User user, PG pg) {
        if (user.getRole() == User.Role.OWNER) {
            if (!pg.getOwner().getId().equals(user.getId())) throw new RuntimeException("Not authorized");
        } else {
            PG tenantPg = requireTenantPg(user);
            if (!tenantPg.getId().equals(pg.getId())) throw new RuntimeException("Not authorized");
        }
    }
}
