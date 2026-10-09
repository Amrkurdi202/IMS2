package com.ghazal.ims2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.entity.ItemUnitPrice;
import com.ghazal.ims2.entity.Warehouse;
import io.jmix.core.DataManager;
import io.jmix.core.entity.KeyValueEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only inventory tools exposed over MCP. All queries are strictly read-only —
 * taking/checking in stock stays in the UI (BarcodeScanView).
 */
@Component("ims2.InventoryTools")
public class InventoryTools {

    /** Thrown when tool arguments are invalid or data is ambiguous; surfaced as isError:true. */
    public static class ToolException extends RuntimeException {
        public ToolException(String message) {
            super(message);
        }
    }

    private final DataManager dataManager;
    private final N8nSyncService n8nSyncService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InventoryTools(DataManager dataManager, N8nSyncService n8nSyncService) {
        this.dataManager = dataManager;
        this.n8nSyncService = n8nSyncService;
    }

    public record ToolDescriptor(String name, String description, ObjectNode inputSchema, ObjectNode annotations) {
    }

    public List<ToolDescriptor> descriptors() {
        List<ToolDescriptor> tools = new ArrayList<>();

        ObjectNode searchSchema = schema();
        ObjectNode searchProps = searchSchema.putObject("properties");
        searchProps.putObject("query").put("type", "string")
                .put("description", "Substring of item name or barcode.");
        searchProps.putObject("warehouse").put("type", "string")
                .put("description", "Optional warehouse name filter (substring).");
        searchSchema.putArray("required").add("query");
        tools.add(new ToolDescriptor("search_inventory",
                "Read inventory stock by item name or barcode. Does not change stock.",
                searchSchema, readOnlyAnnotations()));

        ObjectNode listSchema = schema();
        listSchema.putObject("properties")
                .putObject("warehouse").put("type", "string")
                .put("description", "Warehouse name (exact or unique substring).");
        listSchema.putArray("required").add("warehouse");
        tools.add(new ToolDescriptor("list_warehouse_stock",
                "List all stock lines of one warehouse, including whether it is internal and its GPS coordinates.",
                listSchema, readOnlyAnnotations()));

        ObjectNode itemSchema = schema();
        itemSchema.putObject("properties")
                .putObject("barcode").put("type", "string")
                .put("description", "Item barcode or unit-price (item/unit/warehouse) barcode.");
        itemSchema.putArray("required").add("barcode");
        tools.add(new ToolDescriptor("get_item_stock",
                "Resolve a scanned barcode and return the stock of that item per warehouse, plus unit and price when the barcode is a unit barcode.",
                itemSchema, readOnlyAnnotations()));

        return tools;
    }

    /**
     * Executes a tool. Returns the JSON text payload; throws ToolException on tool-level errors.
     */
    public String call(String name, Map<String, Object> args) throws Exception {
        return switch (name) {
            case "search_inventory" -> searchInventory(args);
            case "list_warehouse_stock" -> listWarehouseStock(args);
            case "get_item_stock" -> getItemStock(args);
            default -> throw new ToolException("Unknown tool: " + name);
        };
    }

    // ------------------------------------------------------------------ tools

    private String searchInventory(Map<String, Object> args) throws Exception {
        String query = str(args.get("query"));
        if (query.isEmpty()) {
            throw new ToolException("query is required");
        }
        String warehouseFilter = str(args.get("warehouse"));

        Long warehouseId = null;
        if (!warehouseFilter.isEmpty()) {
            warehouseId = resolveWarehouseId(warehouseFilter);
        }

        // Stock rows for matching items (grouped per item/unit/warehouse).
        List<KeyValueEntity> stockRows = n8nSyncService.loadStockRows(warehouseId, query);
        Map<Long, List<KeyValueEntity>> rowsByItem = new LinkedHashMap<>();
        for (KeyValueEntity row : stockRows) {
            Number id = row.getValue("itemId");
            if (id != null) {
                rowsByItem.computeIfAbsent(id.longValue(), k -> new ArrayList<>()).add(row);
            }
        }

        // The item catalog too, so items that never moved still show up with stock 0.
        List<Item> items = dataManager.load(Item.class)
                .query("select i from Item i where lower(i.name) like :q or lower(i.barcode) like :q "
                        + "order by i.name")
                .parameter("q", "%" + query.toLowerCase() + "%")
                .list();

        List<Object> results = new ArrayList<>();
        for (Item item : items) {
            List<KeyValueEntity> rows = rowsByItem.remove(item.getId());
            if (rows == null || rows.isEmpty()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("barcode", item.getBarcode() != null ? item.getBarcode() : "");
                m.put("name", item.getName());
                m.put("unit", "");
                m.put("warehouse", "");
                m.put("stock", 0);
                results.add(m);
                continue;
            }
            for (KeyValueEntity row : rows) {
                results.add(toStockMap(row));
            }
        }
        // Stock rows whose item name changed since the movements (still matching stock query).
        for (List<KeyValueEntity> rows : rowsByItem.values()) {
            for (KeyValueEntity row : rows) {
                results.add(toStockMap(row));
            }
        }
        return objectMapper.writeValueAsString(Map.of(
                "query", query,
                "count", results.size(),
                "results", results));
    }

    private static Map<String, Object> toStockMap(KeyValueEntity row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("barcode", str(row.getValue("itemBarcode")));
        m.put("name", str(row.getValue("itemName")));
        m.put("unit", str(row.getValue("unitName")));
        m.put("warehouse", str(row.getValue("warehouseName")));
        m.put("stock", row.getValue("stockQty"));
        return m;
    }

    private String listWarehouseStock(Map<String, Object> args) throws Exception {
        String name = str(args.get("warehouse"));
        if (name.isEmpty()) {
            throw new ToolException("warehouse is required");
        }
        Warehouse warehouse = resolveWarehouse(name);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("warehouse", warehouse.getName());
        out.put("internal", Boolean.TRUE.equals(warehouse.getInternal()));
        out.put("latitude", warehouse.getLatitude());
        out.put("longitude", warehouse.getLongitude());

        List<Object> lines = new ArrayList<>();
        for (KeyValueEntity row : n8nSyncService.loadStockRows(warehouse.getId(), null)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("barcode", str(row.getValue("itemBarcode")));
            m.put("name", str(row.getValue("itemName")));
            m.put("unit", str(row.getValue("unitName")));
            m.put("stock", row.getValue("stockQty"));
            lines.add(m);
        }
        out.put("count", lines.size());
        out.put("lines", lines);
        return objectMapper.writeValueAsString(out);
    }

    private String getItemStock(Map<String, Object> args) throws Exception {
        String barcode = str(args.get("barcode"));
        if (barcode.isEmpty()) {
            throw new ToolException("barcode is required");
        }

        // Same resolution order as BarcodeScanView: unit barcode first, then item barcode.
        ItemUnitPrice iup = dataManager.load(ItemUnitPrice.class)
                .query("select p from ItemUnitPrice p where p.barcode = :b")
                .parameter("b", barcode)
                .optional()
                .orElse(null);
        if (iup == null) {
            List<Item> items = dataManager.load(Item.class)
                    .query("select i from Item i where i.barcode = :b")
                    .parameter("b", barcode)
                    .list();
            if (items.isEmpty()) {
                throw new ToolException("Unknown barcode: " + barcode);
            }
            if (items.size() > 1) {
                throw new ToolException("Barcode matches multiple items: " + barcode);
            }
            return itemStockJson(items.get(0), null, null);
        }
        return itemStockJson(iup.getItem(), iup.getUnit() != null ? iup.getUnit().getName() : null, iup);
    }

    private String itemStockJson(Item item, String unitHint, ItemUnitPrice iup) throws Exception {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("item", item.getName());
        out.put("item_barcode", item.getBarcode() != null ? item.getBarcode() : "");
        out.put("unit", unitHint != null ? unitHint : "");
        if (iup != null) {
            out.put("iup_barcode", iup.getBarcode() != null ? iup.getBarcode() : "");
            out.put("price", iup.getPrice());
            out.put("iup_warehouse", iup.getWarehouse() != null ? iup.getWarehouse().getName() : "");
        }

        List<Object> perWarehouse = new ArrayList<>();
        for (KeyValueEntity row : n8nSyncService.loadStockRows(null, null)) {
            Object itemId = row.getValue("itemId");
            if (!(itemId instanceof Number n) || n.longValue() != item.getId()) {
                continue;
            }
            if (unitHint != null && !unitHint.equals(str(row.getValue("unitName")))) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("warehouse", str(row.getValue("warehouseName")));
            m.put("unit", str(row.getValue("unitName")));
            m.put("stock", row.getValue("stockQty"));
            perWarehouse.add(m);
        }
        out.put("stock", perWarehouse);
        return objectMapper.writeValueAsString(out);
    }

    // ------------------------------------------------------------------ helpers

    private Long resolveWarehouseId(String name) {
        return resolveWarehouse(name).getId();
    }

    private Warehouse resolveWarehouse(String name) {
        List<Warehouse> exact = dataManager.load(Warehouse.class)
                .query("select w from Warehouse w where lower(w.name) = :n")
                .parameter("n", name.toLowerCase())
                .list();
        if (exact.size() == 1) {
            return exact.get(0);
        }
        List<Warehouse> partial = dataManager.load(Warehouse.class)
                .query("select w from Warehouse w where lower(w.name) like :n order by w.name")
                .parameter("n", "%" + name.toLowerCase() + "%")
                .list();
        if (partial.isEmpty()) {
            throw new ToolException("No warehouse matches: " + name);
        }
        if (partial.size() > 1) {
            throw new ToolException("Ambiguous warehouse name '" + name + "'; matches: "
                    + partial.stream().map(Warehouse::getName).reduce((a, b) -> a + ", " + b).orElse(""));
        }
        return partial.get(0);
    }

    private static ObjectNode schema() {
        ObjectNode n = objectMapperStatic().createObjectNode();
        n.put("type", "object");
        n.put("additionalProperties", false);
        return n;
    }

    private static ObjectNode readOnlyAnnotations() {
        ObjectNode n = objectMapperStatic().createObjectNode();
        n.put("readOnlyHint", true);
        n.put("destructiveHint", false);
        return n;
    }

    private static final ObjectMapper STATIC_MAPPER = new ObjectMapper();

    private static ObjectMapper objectMapperStatic() {
        return STATIC_MAPPER;
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
