package com.ghazal.ims2.service;

import com.ghazal.ims2.entity.Order;
import com.ghazal.ims2.entity.OrderItem;
import io.jmix.core.event.EntityChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bumps the n8n snapshot revision after any stock-affecting change is committed.
 * <p>
 * Runs AFTER_COMMIT and uses plain SQL (not DataManager): DataManager inside a commit
 * callback collides with Jmix's EclipseLink resource synchronization. The single
 * INSERT ... ON DUPLICATE KEY UPDATE statement is race-free; over-counting revisions
 * is harmless (n8n only needs monotonicity). A failure here must never affect the
 * user's save, so errors are only logged.
 */
@Component("ims2.SyncRevisionListener")
public class SyncRevisionListener {

    private static final Logger log = LoggerFactory.getLogger(SyncRevisionListener.class);

    private static final String BUMP_SQL =
            "INSERT INTO app_setting(SETTING_KEY, SETTING_VALUE) VALUES('n8n.revision', '1') "
                    + "ON DUPLICATE KEY UPDATE SETTING_VALUE = CAST(SETTING_VALUE AS UNSIGNED) + 1";

    private final JdbcTemplate jdbcTemplate;

    public SyncRevisionListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderChanged(EntityChangedEvent<Order> event) {
        bump();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderItemChanged(EntityChangedEvent<OrderItem> event) {
        bump();
    }

    private void bump() {
        try {
            jdbcTemplate.update(BUMP_SQL);
        } catch (Exception e) {
            log.warn("Could not bump n8n revision: {}", e.getMessage());
        }
    }
}
