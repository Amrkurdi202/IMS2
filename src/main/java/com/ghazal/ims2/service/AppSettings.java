package com.ghazal.ims2.service;

import com.ghazal.ims2.entity.AppSetting;
import io.jmix.core.DataManager;
import org.springframework.stereotype.Component;

/**
 * Upsert helpers for the app_setting key/value table.
 */
@Component("ims2.AppSettings")
public class AppSettings {

    public static final String N8N_URL = "n8n.webhook_url";
    public static final String N8N_SECRET = "n8n.secret";
    public static final String N8N_REVISION = "n8n.revision";
    public static final String N8N_SYNC_REVISION = "n8n.sync_revision";
    public static final String N8N_SYNC_AT = "n8n.sync_at";
    public static final String N8N_SYNC_ERROR = "n8n.sync_error";
    public static final String MCP_TOKEN = "mcp.token";

    private final DataManager dataManager;

    public AppSettings(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    public String get(String key) {
        AppSetting setting = find(key);
        return setting != null ? setting.getValue() : null;
    }

    public String get(String key, String defaultValue) {
        String value = get(key);
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    public long getLong(String key, long defaultValue) {
        try {
            String value = get(key);
            return value == null || value.isEmpty() ? defaultValue : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void set(String key, String value) {
        AppSetting setting = find(key);
        if (setting == null) {
            setting = dataManager.create(AppSetting.class);
            setting.setKey(key);
            setting.setValue(value);
            dataManager.save(setting);
        } else if (value == null ? setting.getValue() != null : !value.equals(setting.getValue())) {
            setting.setValue(value);
            dataManager.save(setting);
        }
    }

    /**
     * Atomically-enough increments the value stored under key (monotonic counter used for n8n revision).
     * Over-counting is harmless; going backwards is not.
     */
    public long increment(String key) {
        long next = getLong(key, 0) + 1;
        set(key, String.valueOf(next));
        return next;
    }

    private AppSetting find(String key) {
        return dataManager.load(AppSetting.class)
                .query("select s from AppSetting s where s.key = :k")
                .parameter("k", key)
                .optional()
                .orElse(null);
    }
}
