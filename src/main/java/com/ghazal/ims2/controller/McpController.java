package com.ghazal.ims2.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ghazal.ims2.service.AppSettings;
import com.ghazal.ims2.service.InventoryTools;
import io.jmix.core.security.SystemAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal stateless MCP (Model Context Protocol) server over Streamable HTTP.
 * JSON-RPC 2.0, tools capability only, read-only inventory tools (InventoryTools).
 * Authentication: Authorization: Bearer &lt;token&gt; where token lives in app_setting (mcp.token).
 */
@RestController
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);
    private static final String PROTOCOL_VERSION = "2025-06-18";

    // JSON-RPC error codes
    private static final int PARSE_ERROR = -32700;
    private static final int INVALID_REQUEST = -32600;
    private static final int METHOD_NOT_FOUND = -32601;
    private static final int INVALID_PARAMS = -32602;
    private static final int INTERNAL_ERROR = -32603;

    private final AppSettings appSettings;
    private final InventoryTools inventoryTools;
    private final SystemAuthenticator systemAuthenticator;
    private final ObjectMapper mapper = new ObjectMapper();

    public McpController(AppSettings appSettings, InventoryTools inventoryTools,
                         SystemAuthenticator systemAuthenticator) {
        this.appSettings = appSettings;
        this.inventoryTools = inventoryTools;
        this.systemAuthenticator = systemAuthenticator;
    }

    @PostMapping(value = "/mcp", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handle(@RequestBody String body,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        // External AI clients are not Vaadin sessions: run under the 'system' user so that
        // Jmix DataManager queries work (read-only tools; no user permissions involved).
        return systemAuthenticator.withSystem(() -> doHandle(body, authorization));
    }

    private ResponseEntity<String> doHandle(String body, String authorization) {
        if (!isAuthorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"error\":\"invalid or missing bearer token\"}");
        }

        JsonNode payload;
        try {
            payload = mapper.readTree(body);
        } catch (Exception e) {
            return jsonRpcResponse(null, errorObject(PARSE_ERROR, "Parse error", null));
        }

        if (payload.isArray()) {
            ArrayNode out = mapper.createArrayNode();
            for (JsonNode node : payload) {
                out.add(handleSingle(node));
            }
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(out.toString());
        }

        JsonNode single = handleSingle(payload);
        if (single == null) {
            // notification — accepted with no body
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(single.toString());
    }

    private JsonNode handleSingle(JsonNode req) {
        if (req == null || !req.has("method")) {
            return errorResponse(req, INVALID_REQUEST, "Invalid Request", null);
        }
        String method = req.path("method").asText();
        boolean isNotification = !req.has("id");

        switch (method) {
            case "initialize": {
                ObjectNode result = mapper.createObjectNode();
                String requested = req.path("params").path("protocolVersion").asText("");
                result.put("protocolVersion", requested.isEmpty() ? PROTOCOL_VERSION : requested);
                result.putObject("capabilities").putObject("tools");
                ObjectNode serverInfo = result.putObject("serverInfo");
                serverInfo.put("name", "ims2");
                serverInfo.put("version", "1.0.0");
                return resultOrNotification(req, result, isNotification);
            }
            case "notifications/initialized":
            case "notifications/cancelled":
                return null; // accepted, no body
            case "ping":
                return resultOrNotification(req, mapper.createObjectNode(), isNotification);
            case "tools/list": {
                ObjectNode result = mapper.createObjectNode();
                ArrayNode tools = result.putArray("tools");
                for (InventoryTools.ToolDescriptor d : inventoryTools.descriptors()) {
                    ObjectNode t = tools.addObject();
                    t.put("name", d.name());
                    t.put("description", d.description());
                    t.set("inputSchema", d.inputSchema());
                    t.set("annotations", d.annotations());
                }
                return resultOrNotification(req, result, isNotification);
            }
            case "tools/call": {
                JsonNode params = req.path("params");
                String name = params.path("name").asText("");
                Map<String, Object> args = new LinkedHashMap<>();
                JsonNode rawArgs = params.path("arguments");
                if (rawArgs.isObject()) {
                    rawArgs.fields().forEachRemaining(e -> args.put(e.getKey(),
                            mapper.convertValue(e.getValue(), Object.class)));
                }
                if (name.isEmpty()) {
                    return errorResponse(req, INVALID_PARAMS, "tool name is required", null);
                }
                ObjectNode result = mapper.createObjectNode();
                ArrayNode content = result.putArray("content");
                try {
                    String text = inventoryTools.call(name, args);
                    ObjectNode item = content.addObject();
                    item.put("type", "text");
                    item.put("text", text);
                    result.put("isError", false);
                } catch (InventoryTools.ToolException e) {
                    ObjectNode item = content.addObject();
                    item.put("type", "text");
                    item.put("text", e.getMessage());
                    result.put("isError", true);
                } catch (Exception e) {
                    log.error("MCP tool {} failed", name, e);
                    return errorResponse(req, INTERNAL_ERROR, "Tool execution failed", null);
                }
                return resultOrNotification(req, result, isNotification);
            }
            default:
                if (isNotification) {
                    return null;
                }
                return errorResponse(req, METHOD_NOT_FOUND, "Method not found: " + method, null);
        }
    }

    private JsonNode resultOrNotification(JsonNode req, ObjectNode result, boolean isNotification) {
        if (isNotification) {
            return null;
        }
        return successResponse(req, result);
    }

    private JsonNode successResponse(JsonNode req, ObjectNode result) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", req != null && req.has("id") ? req.get("id") : null);
        response.set("result", result);
        return response;
    }

    private JsonNode errorResponse(JsonNode req, int code, String message, Object data) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", req != null && req.has("id") ? req.get("id") : null);
        response.set("error", errorObject(code, message, data));
        return response;
    }

    private ObjectNode errorObject(int code, String message, Object data) {
        ObjectNode error = mapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        if (data != null) {
            error.set("data", mapper.valueToTree(data));
        }
        return error;
    }

    private ResponseEntity<String> jsonRpcResponse(JsonNode id, ObjectNode error) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("error", error);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(response.toString());
    }

    private boolean isAuthorized(String authorization) {
        String stored = appSettings.get(AppSettings.MCP_TOKEN);
        if (stored == null || stored.isEmpty()) {
            return false;
        }
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }
        byte[] provided = authorization.substring("Bearer ".length()).trim()
                .getBytes(StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(provided, stored.getBytes(StandardCharsets.UTF_8));
    }
}
