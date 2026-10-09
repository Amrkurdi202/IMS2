package com.ghazal.ims2.view.integrations;

import com.ghazal.ims2.service.AppSettings;
import com.ghazal.ims2.service.N8nSyncService;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Settings screen for the two external integrations:
 * n8n inventory-snapshot push and the read-only MCP endpoint.
 */
@Route(value = "admin/integrations", layout = MainView.class)
@ViewController(id = "IntegrationsView")
@ViewDescriptor("integrations-view.xml")
public class IntegrationsView extends StandardView {

    private static final DateTimeFormatter SYNC_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    @ViewComponent
    private TextField urlField;
    @ViewComponent
    private PasswordField secretField;
    @ViewComponent
    private Button saveSyncBtn;
    @ViewComponent
    private Button resyncBtn;
    @ViewComponent
    private Span configuredLabel;
    @ViewComponent
    private Span lastSyncLabel;
    @ViewComponent
    private Span pendingLabel;
    @ViewComponent
    private Span errorLabel;
    @ViewComponent
    private TextField mcpTokenField;
    @ViewComponent
    private Button regenBtn;
    @ViewComponent
    private TextField endpointField;

    @Autowired
    private AppSettings appSettings;
    @Autowired
    private N8nSyncService n8nSyncService;
    @Autowired
    private Notifications notifications;
    @Autowired
    private MessageBundle messageBundle;

    private final SecureRandom random = new SecureRandom();

    @Subscribe
    public void onInit(InitEvent event) {
        String storedUrl = appSettings.get(AppSettings.N8N_URL);
        if (storedUrl != null) {
            urlField.setValue(storedUrl);
        }
        String token = appSettings.get(AppSettings.MCP_TOKEN);
        if (token != null && !token.isEmpty()) {
            mcpTokenField.setValue(token);
        } else {
            mcpTokenField.setValue("");
        }
        endpointField.setValue(buildEndpointUrl());
        refreshStatus();
    }

    @Subscribe("saveSyncBtn")
    public void onSaveSyncBtnClick(ClickEvent<Button> event) {
        String url = trim(urlField.getValue());
        if (url.isEmpty()) {
            warn("n8n.err.notConfigured");
            return;
        }
        appSettings.set(AppSettings.N8N_URL, url);
        String secret = secretField.getValue();
        if (secret != null && !secret.trim().isEmpty()) {
            appSettings.set(AppSettings.N8N_SECRET, secret.trim());
        }
        secretField.clear();
        runSync("n8n.saveOk");
    }

    @Subscribe("resyncBtn")
    public void onResyncBtnClick(ClickEvent<Button> event) {
        runSync(null);
    }

    @Subscribe("regenBtn")
    public void onRegenBtnClick(ClickEvent<Button> event) {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(32);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        String token = sb.toString();
        appSettings.set(AppSettings.MCP_TOKEN, token);
        mcpTokenField.setValue(token);
        info("mcp.tokenSaved");
    }

    private void runSync(String successKey) {
        N8nSyncService.SyncResult result = n8nSyncService.sync();
        refreshStatus();
        if (result.ok()) {
            long revision = appSettings.getLong(AppSettings.N8N_REVISION, 0);
            String msg = messageBundle.getMessage("n8n.syncOk").replace("{0}", String.valueOf(revision));
            if (successKey != null) {
                msg = messageBundle.getMessage(successKey) + " — " + msg;
            }
            notifications.create(msg).withType(Notifications.Type.SUCCESS).show();
        } else {
            notifications.create(errorMessage(result))
                    .withType(Notifications.Type.ERROR).show();
        }
    }

    private void refreshStatus() {
        boolean configured = n8nSyncService.isConfigured();
        configuredLabel.setText(configured
                ? messageBundle.getMessage("n8n.configured")
                : messageBundle.getMessage("n8n.notConfigured"));

        String syncAt = appSettings.get(AppSettings.N8N_SYNC_AT);
        lastSyncLabel.setText(syncAt == null || syncAt.isEmpty()
                ? messageBundle.getMessage("n8n.lastSyncNever")
                : messageBundle.getMessage("n8n.lastSync").replace("{0}", formatTime(syncAt)));

        long revision = appSettings.getLong(AppSettings.N8N_REVISION, 0);
        long syncRevision = appSettings.getLong(AppSettings.N8N_SYNC_REVISION, 0);
        pendingLabel.setText(revision > syncRevision
                ? messageBundle.getMessage("n8n.pending")
                : messageBundle.getMessage("n8n.upToDate"));

        String syncError = appSettings.get(AppSettings.N8N_SYNC_ERROR);
        if (syncError == null || syncError.isEmpty()) {
            errorLabel.setText("");
        } else {
            errorLabel.setText(errorMessage(
                    new N8nSyncService.SyncResult(false, codeOf(syncError), detailOf(syncError))));
        }
    }

    private String errorMessage(N8nSyncService.SyncResult result) {
        String key = "n8n.err." + result.errorCode();
        String msg;
        try {
            msg = messageBundle.getMessage(key);
        } catch (IllegalArgumentException e) {
            msg = result.errorCode();
        }
        if (result.detail() != null && !result.detail().isEmpty()) {
            msg = msg + " (" + result.detail() + ")";
        }
        return msg;
    }

    private static String codeOf(String stored) {
        int i = stored.indexOf('|');
        return i < 0 ? stored : stored.substring(0, i);
    }

    private static String detailOf(String stored) {
        int i = stored.indexOf('|');
        return i < 0 ? "" : stored.substring(i + 1);
    }

    private String buildEndpointUrl() {
        try {
            VaadinServletRequest request = VaadinServletRequest.getCurrent();
            if (request == null) {
                return "/mcp";
            }
            URI full = URI.create(request.getRequestURL().toString());
            String context = request.getContextPath() == null ? "" : request.getContextPath();
            return full.getScheme() + "://" + full.getHost()
                    + (full.getPort() > 0 ? ":" + full.getPort() : "") + context + "/mcp";
        } catch (Exception e) {
            return "/mcp";
        }
    }

    private static String formatTime(String iso) {
        try {
            return SYNC_TIME_FORMAT.format(Instant.parse(iso));
        } catch (Exception e) {
            return iso;
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void warn(String key) {
        notifications.create(messageBundle.getMessage(key))
                .withType(Notifications.Type.WARNING).show();
    }

    private void info(String key) {
        notifications.create(messageBundle.getMessage(key))
                .withType(Notifications.Type.SUCCESS).show();
    }
}
