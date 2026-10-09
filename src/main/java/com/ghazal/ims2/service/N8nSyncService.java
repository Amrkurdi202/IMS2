package com.ghazal.ims2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jmix.core.DataManager;
import io.jmix.core.ValueLoadContext;
import io.jmix.core.entity.KeyValueEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pushes a full inventory snapshot to an external n8n webhook (Ghazal-handoff contract,
 * re-implemented for IMS2). The app is the source of truth: a failed sync never rolls
 * back stock; retries resend the same revision so n8n's revision guard makes them idempotent.
 *
 * <p>Payload: {event:"inventory.snapshot", source:"ims2", revision, generated_at, warehouses:[...], products:[...]}
 * Expected ack: {ok:true, revision:&lt;same revision&gt;} with HTTP 2xx.
 */
@Component("ims2.N8nSyncService")
public class N8nSyncService {

    private static final Logger log = LoggerFactory.getLogger(N8nSyncService.class);

    public static final String HEADER_TOKEN = "X-IMS-Token";
    /** io.jmix.data.PersistenceHints.SOFT_DELETION — see loadStockRows. */
    static final String SOFT_DELETION_HINT = "jmix.softDeletion";
    /** Localhost may use http for development webhooks; everything else must be https. */
    private static final java.util.regex.Pattern SECRET_PATTERN =
            java.util.regex.Pattern.compile("^[\\x21-\\x7e]{20,300}$");

    public record SyncResult(boolean ok, String errorCode, String detail) {
        public static SyncResult success() {
            return new SyncResult(true, null, null);
        }
    }

    private final DataManager dataManager;
    private final AppSettings appSettings;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public N8nSyncService(DataManager dataManager, AppSettings appSettings) {
        this.dataManager = dataManager;
        this.appSettings = appSettings;
    }

    public boolean isConfigured() {
        String url = appSettings.get(AppSettings.N8N_URL);
        String secret = appSettings.get(AppSettings.N8N_SECRET);
        return url != null && !url.isEmpty() && secret != null && !secret.isEmpty();
    }

    /**
     * Builds the snapshot payload with the current revision.
     */
    public ObjectNode buildSnapshot() {
        long revision = appSettings.getLong(AppSettings.N8N_REVISION, 0);

        ObjectNode snapshot = objectMapper.createObjectNode();
        snapshot.put("event", "inventory.snapshot");
        snapshot.put("source", "ims2");
        snapshot.put("revision", revision);
        snapshot.put("generated_at", Instant.now().toString());

        ArrayNode warehouses = snapshot.putArray("warehouses");
        for (com.ghazal.ims2.entity.Warehouse w : dataManager.load(com.ghazal.ims2.entity.Warehouse.class)
                .query("select w from Warehouse w order by w.name")
                .list()) {
            ObjectNode n = warehouses.addObject();
            n.put("id", w.getUuid() != null ? w.getUuid().toString() : String.valueOf(w.getId()));
            n.put("name", w.getName());
            n.put("internal", Boolean.TRUE.equals(w.getInternal()));
            n.put("latitude", w.getLatitude());
            n.put("longitude", w.getLongitude());
        }

        Map<String, ObjectNode> iupIndex = loadIupIndex();
        ArrayNode products = snapshot.putArray("products");
        for (KeyValueEntity row : loadStockRows(null, null)) {
            ObjectNode p = products.addObject();
            Number itemId = row.getValue("itemId");
            String unitName = str(row.getValue("unitName"));
            Number warehouseId = row.getValue("warehouseId");
            p.put("item_id", itemId != null ? itemId.longValue() : -1);
            p.put("name", str(row.getValue("itemName")));
            p.put("barcode", str(row.getValue("itemBarcode")));
            p.put("unit", unitName);
            p.put("warehouse_id", warehouseId != null ? warehouseId.longValue() : -1);
            p.put("warehouse_name", str(row.getValue("warehouseName")));
            BigDecimal stock = row.getValue("stockQty");
            p.put("stock", stock != null ? stock : BigDecimal.ZERO);
            ObjectNode iup = iupIndex.get(iupKey(itemId, unitName, warehouseId));
            if (iup != null) {
                p.put("iup_barcode", iup.path("barcode").asText(""));
                p.put("price", iup.path("price").asText(""));
            }
        }
        return snapshot;
    }

    /**
     * Validates configuration, pushes the snapshot, verifies the ack and records status.
     * Never touches stock rows.
     */
    public SyncResult sync() {
        String url = appSettings.get(AppSettings.N8N_URL);
        String secret = appSettings.get(AppSettings.N8N_SECRET);

        if (url == null || url.isEmpty() || secret == null || secret.isEmpty()) {
            return fail("notConfigured", null);
        }
        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (IllegalArgumentException e) {
            return fail("badUrl", e.getMessage());
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost();
        boolean local = host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1");
        if ((!scheme.equals("https") && !(scheme.equals("http") && local))
                || host.isEmpty() || uri.getUserInfo() != null) {
            return fail("badUrl", url);
        }
        if (!SECRET_PATTERN.matcher(secret).matches()) {
            return fail("badSecret", null);
        }

        ObjectNode snapshot = buildSnapshot();
        long revision = snapshot.path("revision").asLong();
        String body;
        try {
            body = objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            return fail("response", e.getMessage());
        }

        HttpResponse<String> response;
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header(HEADER_TOKEN, secret)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.net.http.HttpTimeoutException e) {
            // The request may have landed; n8n's revision guard makes a retry safe.
            log.warn("n8n sync timeout, revision {}", revision);
            return fail("timeout", null);
        } catch (java.io.IOException e) {
            return fail("network", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fail("network", "interrupted");
        }

        int status = response.statusCode();
        if (status >= 300 && status < 400) {
            return fail("http3xx", "HTTP " + status);
        }
        if (status == 401 || status == 403) {
            return fail("http" + status, null);
        }
        if (status == 404) {
            return fail("http404", null);
        }
        if (status < 200 || status >= 300) {
            return fail("httpOther", "HTTP " + status);
        }

        JsonNode ack;
        try {
            ack = objectMapper.readTree(response.body());
        } catch (Exception e) {
            return fail("ackMismatch", "invalid JSON");
        }
        if (ack == null || !ack.path("ok").asBoolean(false)
                || ack.path("revision").asLong(-1) != revision) {
            return fail("ackMismatch", ack == null ? "empty" : ack.toString());
        }

        appSettings.set(AppSettings.N8N_SYNC_REVISION, String.valueOf(revision));
        appSettings.set(AppSettings.N8N_SYNC_AT, Instant.now().toString());
        appSettings.set(AppSettings.N8N_SYNC_ERROR, "");
        return SyncResult.success();
    }

    private SyncResult fail(String code, String detail) {
        appSettings.set(AppSettings.N8N_SYNC_ERROR, code + (detail != null && !detail.isEmpty() ? "|" + detail : ""));
        log.warn("n8n sync failed: {} {}", code, detail);
        return new SyncResult(false, code, detail);
    }

    /**
     * Stock rows grouped per (item, unit, warehouse) with the IMS2 sign convention:
     * stock = -SUM(order_item.quantity). Optionally filtered by warehouse id / search text.
     *
     * <p>Jmix value queries need explicit .property() declarations (mapped positionally to
     * the select clause). The unit is LEFT-joined explicitly: EclipseLink inner-joins to-one
     * paths, which would silently drop every order item that has no unit (39 rows in
     * production data). Soft deletion is OFF for this query so the outer join stays valid;
     * soft-deleted items/orders/units are excluded by explicit predicates instead.
     */
    public List<KeyValueEntity> loadStockRows(Long warehouseId, String query) {
        StringBuilder jpql = new StringBuilder(
                "select oi.item.id as itemId, oi.item.name as itemName, oi.item.barcode as itemBarcode, "
                        + "u.name as unitName, o.warehouse.id as warehouseId, "
                        + "o.warehouse.name as warehouseName, "
                        + "0 - sum(coalesce(oi.quantity, 0)) as stockQty "
                        + "from ORDER_ITEM oi join oi.order o "
                        + "left join oi.unit u "
                        + "where oi.item is not null "
                        + "and oi.item.deletedDate is null "
                        + "and o.deletedDate is null "
                        + "and (u.id is null or u.deletedDate is null) ");
        if (warehouseId != null) {
            jpql.append("and o.warehouse.id = :wh ");
        }
        if (query != null && !query.isEmpty()) {
            jpql.append("and (lower(oi.item.name) like :q or lower(oi.item.barcode) like :q) ");
        }
        jpql.append("group by oi.item.id, oi.item.name, oi.item.barcode, u.name, "
                + "o.warehouse.id, o.warehouse.name order by oi.item.name");

        ValueLoadContext.Query q = ValueLoadContext.createQuery(jpql.toString());
        if (warehouseId != null) {
            q.setParameter("wh", warehouseId);
        }
        if (query != null && !query.isEmpty()) {
            q.setParameter("q", "%" + query.toLowerCase() + "%");
        }
        ValueLoadContext context = ValueLoadContext.create()
                .setQuery(q)
                .addProperty("itemId")
                .addProperty("itemName")
                .addProperty("itemBarcode")
                .addProperty("unitName")
                .addProperty("warehouseId")
                .addProperty("warehouseName")
                .addProperty("stockQty")
                .setHint(SOFT_DELETION_HINT, Boolean.FALSE);
        return dataManager.loadValues(context);
    }

    /**
     * IUP barcode/price index keyed by itemId|unitName|warehouseId (blank barcode skipped).
     * IUP's item/unit/warehouse are non-nullable so inner joins keep soft deletion safe here.
     */
    public Map<String, ObjectNode> loadIupIndex() {
        Map<String, ObjectNode> index = new HashMap<>();
        ValueLoadContext context = ValueLoadContext.create()
                .setQuery(ValueLoadContext.createQuery(
                        "select p.item.id as itemId, p.unit.name as unitName, p.warehouse.id as warehouseId, "
                                + "p.barcode as barcode, p.price as price from ItemUnitPrice p"))
                .addProperty("itemId")
                .addProperty("unitName")
                .addProperty("warehouseId")
                .addProperty("barcode")
                .addProperty("price");
        for (KeyValueEntity row : dataManager.loadValues(context)) {
            String barcode = str(row.getValue("barcode"));
            if (barcode.isEmpty()) {
                continue;
            }
            ObjectNode n = objectMapper.createObjectNode();
            n.put("barcode", barcode);
            BigDecimal price = row.getValue("price");
            n.put("price", price != null ? price.toPlainString() : "");
            index.put(iupKey(row.getValue("itemId"), str(row.getValue("unitName")), row.getValue("warehouseId")), n);
        }
        return index;
    }

    public static String iupKey(Object itemId, String unitName, Object warehouseId) {
        long i = itemId instanceof Number n ? n.longValue() : -1;
        long w = warehouseId instanceof Number n ? n.longValue() : -1;
        return i + "|" + (unitName == null ? "" : unitName) + "|" + w;
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
