package com.afitnerd.tnra.service;

import com.afitnerd.tnra.model.User;

import java.time.LocalDate;

/**
 * Generates a CSV export of a user's posts. Includes the post body fields
 * (intro, personal/family/work best/worst), timestamps, and one column per
 * stat the user has ever recorded a value for. Stat columns are headered by
 * each stat's label (emoji prefixed when present) and ordered by display
 * order.
 *
 * <p>Posts come back from JPA already decrypted via the existing
 * {@code EncryptedStringConverter} / {@code EncryptedIntegerConverter} — no
 * extra decryption work is needed here. Plaintext only leaves the server
 * when the user explicitly clicks Download.
 */
public interface PostDataExportService {

    /**
     * Export the user's posts, optionally bounded by {@code post.start} date.
     *
     * @param user the user whose posts to export
     * @param from inclusive lower bound on {@code post.start} (null = no lower bound)
     * @param to   inclusive upper bound on {@code post.start} (null = no upper bound)
     * @return CSV bytes, UTF-8 with BOM (so Excel detects encoding correctly)
     */
    byte[] exportToCsv(User user, LocalDate from, LocalDate to);
}
