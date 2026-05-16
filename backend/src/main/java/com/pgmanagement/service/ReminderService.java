package com.pgmanagement.service;

import com.pgmanagement.entity.Notification;
import com.pgmanagement.entity.RentRecord;
import com.pgmanagement.entity.User;
import com.pgmanagement.repository.NotificationRepository;
import com.pgmanagement.repository.RentRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Background job that pushes rent-due reminders into the existing notification
 * system. Runs daily at 9:00 AM (cron) and can be triggered manually by the owner
 * via REST endpoint.
 *
 * <p>Logic per run:
 * <ol>
 *   <li>Find every {@code PENDING} rent record where {@code dueDate} is within 3 days from today
 *       (this includes records already overdue).</li>
 *   <li>For each, send a {@link Notification} to the tenant AND the PG owner.</li>
 *   <li>Skip records that already had a reminder pushed in the last 24 hours
 *       (so daily re-runs don't spam).</li>
 *   <li>Records that are past due date get marked {@code OVERDUE} as a side-effect.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderService {

    /** Send reminders for rent due within this many days (e.g. 2 = "due in <=2 days or already overdue"). */
    private static final int LOOKAHEAD_DAYS = 2;
    /** Skip records reminded within this window (prevents spam if job is run repeatedly). */
    private static final int COOLDOWN_HOURS = 20;

    private final RentRecordRepository rentRecordRepository;
    private final NotificationRepository notificationRepository;

    /** Daily 9:00 AM auto-run. */
    @Scheduled(cron = "0 0 9 * * *")
    public void scheduledRun() {
        log.info("[ReminderService] Scheduled rent-reminder run starting…");
        int n = sendAllDueReminders();
        log.info("[ReminderService] Scheduled run sent {} reminders.", n);
    }

    /**
     * Send reminders for every tenant whose rent is due within the lookahead window
     * (across all owners). Returns count of records actually reminded.
     */
    @Transactional
    public int sendAllDueReminders() {
        LocalDate cutoff = LocalDate.now().plusDays(LOOKAHEAD_DAYS);
        List<RentRecord> due = rentRecordRepository
                .findByStatusAndDueDateLessThanEqual(RentRecord.RentStatus.PENDING, cutoff);
        return sendReminders(due);
    }

    /**
     * Manual trigger by owner — only reminds tenants in their PGs.
     */
    @Transactional
    public int sendRemindersForOwner(User owner) {
        List<RentRecord> pending = rentRecordRepository
                .findByRoom_PgOwnerAndStatus(owner, RentRecord.RentStatus.PENDING);
        LocalDate cutoff = LocalDate.now().plusDays(LOOKAHEAD_DAYS);
        List<RentRecord> due = pending.stream()
                .filter(r -> r.getDueDate() != null && !r.getDueDate().isAfter(cutoff))
                .toList();
        return sendReminders(due);
    }

    private int sendReminders(List<RentRecord> due) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cooldownCutoff = now.minusHours(COOLDOWN_HOURS);
        int sent = 0;

        for (RentRecord r : due) {
            // Cooldown: skip if reminded within last COOLDOWN_HOURS
            if (r.getReminderSentAt() != null && r.getReminderSentAt().isAfter(cooldownCutoff)) {
                continue;
            }

            // Mark OVERDUE if past due
            if (r.getDueDate() != null && r.getDueDate().isBefore(LocalDate.now())
                    && r.getStatus() == RentRecord.RentStatus.PENDING) {
                r.setStatus(RentRecord.RentStatus.OVERDUE);
            }

            User tenant = r.getTenant();
            User owner = r.getRoom().getPg().getOwner();
            String month = r.getMonth();
            String amount = r.getAmount() != null ? r.getAmount().toPlainString() : "?";
            String dueStr = r.getDueDate() != null ? r.getDueDate().toString() : "soon";
            boolean overdue = r.getDueDate() != null && r.getDueDate().isBefore(LocalDate.now());

            // Tenant notification
            notificationRepository.save(Notification.builder()
                    .user(tenant)
                    .message((overdue ? "OVERDUE: " : "Reminder: ") + "Rent for " + month
                            + " of ₹" + amount + " is due " + (overdue ? "since " : "on ") + dueStr)
                    .type("RENT_REMINDER")
                    .build());

            // Owner notification (light-touch summary)
            notificationRepository.save(Notification.builder()
                    .user(owner)
                    .message(tenant.getName() + " has " + (overdue ? "OVERDUE" : "due") + " rent for " + month + " (₹" + amount + ")")
                    .type("RENT_REMINDER")
                    .build());

            r.setReminderSentAt(now);
            rentRecordRepository.save(r);
            sent++;
        }
        return sent;
    }
}
