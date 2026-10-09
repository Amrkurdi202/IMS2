package com.ghazal.ims2.view.barcode;

import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.entity.ItemUnitPrice;
import com.ghazal.ims2.entity.Order;
import com.ghazal.ims2.entity.OrderItem;
import com.ghazal.ims2.entity.User;
import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.view.common.GeoLocator;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.textfield.JmixNumberField;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewDescriptor;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.StandardView;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Scan-to-take / scan-to-check-in for internal warehouses.
 * <p>
 * UX: open page → scanner burst auto-focuses/fills the barcode field →
 * warehouse auto-selects by GPS proximity when not chosen → every scan
 * auto-saves (hybrid quantity rules per mode).
 */
@Route(value = "barcode/scan", layout = MainView.class)
@ViewController(id = "BarcodeScanView")
@ViewDescriptor("scan-view.xml")
public class BarcodeScanView extends StandardView {

    /** Max distance (meters) for GPS auto-selection of a warehouse. */
    static final double GPS_RANGE_M = 500;
    /** Same barcode twice within this window is treated as one scan (client re-delivery guard). */
    static final long DUPLICATE_SCAN_WINDOW_MS = 700;

    enum Mode { TAKE, CHECK_IN }

    record ScanLine(String barcode, String itemName, String unit, BigDecimal qty, BigDecimal remaining) {}

    @ViewComponent
    private CollectionLoader<Warehouse> warehousesDl;
    @ViewComponent
    private EntityComboBox<Warehouse> warehouseField;
    @ViewComponent
    private TypedTextField<String> barcodeField;
    @ViewComponent
    private JmixNumberField qtyField;
    @ViewComponent
    private Span gpsTag;
    @ViewComponent
    private Span currentItemLabel;
    @ViewComponent
    private Span statusLabel;
    @ViewComponent
    private Button takeBtn;
    @ViewComponent
    private Button checkInBtn;
    @ViewComponent
    private com.vaadin.flow.component.orderedlayout.VerticalLayout linesHost;

    @Autowired
    private DataManager dataManager;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private Notifications notifications;
    @Autowired
    private io.jmix.flowui.view.MessageBundle messageBundle;

    private Mode mode = Mode.TAKE;

    // ---- session state ----
    private Order sessionOrder;
    private String currentBarcode;
    private Item currentItem;
    private ItemUnitPrice resolvedIup;
    private OrderItem currentOrderItem;
    private BigDecimal currentQty = BigDecimal.ONE;
    private final List<ScanLine> lines = new ArrayList<>();
    private Grid<ScanLine> linesGrid;

    private String pendingScanCode;
    private GeoLocator.GeoPoint lastGps;
    private String lastScanCode;
    private long lastScanTime;
    private boolean updatingUi;

    @Subscribe
    public void onInit(InitEvent event) {
        warehousesDl.load();

        linesGrid = new Grid<>();
        linesGrid.setWidthFull();
        linesGrid.setItems(lines);
        linesGrid.addColumn(ScanLine::itemName)
                .setHeader(messageBundle.getMessage("barcode.colItem"))
                .setFlexGrow(2);
        linesGrid.addColumn(ScanLine::unit)
                .setHeader(messageBundle.getMessage("barcode.colUnit"))
                .setAutoWidth(true);
        linesGrid.addColumn(ScanLine::barcode)
                .setHeader(messageBundle.getMessage("barcode.colBarcode"))
                .setAutoWidth(true);
        linesGrid.addColumn(l -> l.qty().toPlainString())
                .setHeader(messageBundle.getMessage("barcode.colQty"))
                .setAutoWidth(true);
        linesGrid.addColumn(l -> l.remaining() == null ? "" : l.remaining().toPlainString())
                .setHeader(messageBundle.getMessage("barcode.colRemaining"))
                .setAutoWidth(true);
        linesHost.add(linesGrid);

        takeBtn.addClickListener(e -> setMode(Mode.TAKE));
        checkInBtn.addClickListener(e -> setMode(Mode.CHECK_IN));

        warehouseField.addValueChangeListener(e -> {
            if (updatingUi) {
                return;
            }
            gpsTag.setVisible(false);
            endSession();
        });

        barcodeField.getElement().addEventListener("keydown", keyEvent -> {
            if (!"Enter".equals(keyEvent.getEventData().getString("event.key"))) {
                return;
            }
            String value = keyEvent.getEventData().getString("event.composedPath()[0].value");
            handleScan(value);
        })
                .addEventData("event.key")
                .addEventData("event.composedPath()[0].value");

        qtyField.addValueChangeListener(e -> {
            if (updatingUi || currentBarcode == null || e.getValue() == null) {
                return;
            }
            currentQty = BigDecimal.valueOf(e.getValue()).stripTrailingZeros();
            if (currentQty.compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
            if (commitLine()) {
                refreshLines();
                updateCurrentCard();
                setStatus(savedStatus(currentItem, currentQty));
            }
        });

        setMode(Mode.TAKE);

        // Pre-fill the warehouse by GPS as soon as the view opens.
        GeoLocator.request(this, "initGpsResult");
    }

    // ---- client-side plumbing ----

    @Override
    protected void onAttach(AttachEvent event) {
        super.onAttach(event);
        getElement().executeJs(
                "if(window.__imsScanH){document.removeEventListener('keydown',window.__imsScanH);}"
                        + "if(window.__imsScanStop){window.__imsScanStop();}"
                        + "var field=$0, view=$1, buf='', times=[], timer=null;"
                        + "var reset=function(){buf='';times=[];if(timer){clearTimeout(timer);timer=null;}};"
                        + "window.__imsScanStop=reset;"
                        + "var qualifies=function(){"
                        + "  if(buf.length<3||times.length<2)return false;"
                        + "  var sum=0;for(var i=1;i<times.length;i++){sum+=times[i]-times[i-1];}"
                        + "  return (sum/(times.length-1))<=40;"
                        + "};"
                        + "var deliver=function(code){"
                        + "  try{field.focus();}catch(err){}"
                        + "  view.$server['scanFromScanner'](code);"
                        + "};"
                        + "var fire=function(){"
                        + "  timer=null;"
                        + "  if(qualifies()){var c=buf;reset();deliver(c);}"
                        + "  else{reset();}"
                        + "};"
                        + "var arm=function(){if(timer)clearTimeout(timer);timer=setTimeout(fire,100);};"
                        + "window.__imsScanH=function(e){"
                        + "  if(e.key==='Enter'){"
                        + "    if(qualifies()){"
                        + "        e.preventDefault();"
                        + "        var code=buf;reset();"
                        + "        deliver(code);"
                        + "        return;"
                        + "      }"
                        + "    reset();return;"
                        + "  }"
                        + "  if(e.key.length!==1){return;}"
                        + "  times.push(performance.now());"
                        + "  buf+=e.key;"
                        + "  arm();"
                        + "};"
                        + "document.addEventListener('keydown',window.__imsScanH);",
                barcodeField.getElement(),
                getElement());
    }

    @Override
    protected void onDetach(DetachEvent event) {
        getElement().executeJs(
                "if(window.__imsScanH){document.removeEventListener('keydown',window.__imsScanH);"
                        + "window.__imsScanH=null;}"
                        + "if(window.__imsScanStop){window.__imsScanStop();window.__imsScanStop=null;}");
        super.onDetach(event);
    }

    @ClientCallable
    public void scanFromScanner(String code) {
        handleScan(code);
    }

    @ClientCallable
    public void scanGpsResult(String payload) {
        applyGpsIfUnset(payload);

        String code = pendingScanCode;
        pendingScanCode = null;

        if (code == null || code.isBlank()) {
            return;
        }
        if (warehouseField.getValue() == null) {
            setStatus(messageBundle.getMessage("barcode.selectWarehouse"));
            return;
        }
        processScan(code);
    }

    /** Called on view open so the warehouse is pre-filled by GPS before the first scan. */
    @ClientCallable
    public void initGpsResult(String payload) {
        applyGpsIfUnset(payload);
    }

    /** Selects the nearest internal warehouse (≤ GPS_RANGE_M) unless one is already chosen. */
    private void applyGpsIfUnset(String payload) {
        if (warehouseField.getValue() != null) {
            return;
        }
        GeoLocator.GeoPoint point = GeoLocator.parse(payload);
        if (point == null) {
            return;
        }
        lastGps = point;
        nearestWarehouse(point).ifPresent(w -> {
            updatingUi = true;
            try {
                warehouseField.setValue(w);
            } finally {
                updatingUi = false;
            }
            gpsTag.setVisible(true);
            notifications.create(messageBundle.getMessage("barcode.gpsSelected")
                            .replace("{0}", w.getName()))
                    .withType(Notifications.Type.SUCCESS)
                    .show();
        });
    }

    // ---- scan flow ----

    private void handleScan(String raw) {
        String code = raw == null ? "" : raw.trim();
        updatingUi = true;
        try {
            barcodeField.clear();
        } finally {
            updatingUi = false;
        }

        if (code.isEmpty()) {
            focusScanner();
            return;
        }

        long now = System.currentTimeMillis();
        if (code.equals(lastScanCode) && now - lastScanTime < DUPLICATE_SCAN_WINDOW_MS) {
            focusScanner();
            return;
        }
        lastScanCode = code;
        lastScanTime = now;

        if (warehouseField.getValue() == null) {
            pendingScanCode = code;
            setStatus(messageBundle.getMessage("barcode.gpsSearching"));
            GeoLocator.request(this, "scanGpsResult");
            return;
        }
        processScan(code);
    }

    private void processScan(String code) {
        try {
            processScanResolved(code);
        } catch (Exception e) {
            notifications.create(messageBundle.getMessage("barcode.saveFailed")
                            .replace("{0}", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()))
                    .withType(Notifications.Type.ERROR)
                    .show();
            setStatus(messageBundle.getMessage("barcode.saveFailedShort"));
            focusScanner();
        }
    }

    private void processScanResolved(String code) {
        Warehouse wh = warehouseField.getValue();
        Item item = null;
        ItemUnitPrice iup = null;

        // 1) barcode bound to a specific ItemUnitPrice (item x unit x warehouse)
        List<ItemUnitPrice> byIupCode = dataManager.load(ItemUnitPrice.class)
                .query("select p from ItemUnitPrice p join fetch p.item join fetch p.unit "
                        + "join fetch p.warehouse where p.barcode = :b")
                .parameter("b", code)
                .list();
        if (byIupCode.size() > 1) {
            notifications.create(messageBundle.getMessage("barcode.ambiguous"))
                    .withType(Notifications.Type.ERROR)
                    .show();
            focusScanner();
            return;
        }
        if (byIupCode.size() == 1) {
            ItemUnitPrice p = byIupCode.get(0);
            if (wh == null || !p.getWarehouse().getId().equals(wh.getId())) {
                notifications.create(messageBundle.getMessage("barcode.otherWarehouse")
                                .replace("{0}", p.getWarehouse().getName()))
                        .withType(Notifications.Type.ERROR)
                        .show();
                setStatus(messageBundle.getMessage("barcode.statusReady"));
                focusScanner();
                return;
            }
            item = p.getItem();
            iup = p;
        } else {
            // 2) item-level barcode fallback
            List<Item> found = dataManager.load(Item.class)
                    .query("select i from Item i where i.barcode = :b")
                    .parameter("b", code)
                    .list();

            if (found.isEmpty()) {
                notifications.create(messageBundle.getMessage("barcode.unknown")
                                .replace("{0}", code))
                        .withType(Notifications.Type.ERROR)
                        .show();
                setStatus(messageBundle.getMessage("barcode.statusReady"));
                focusScanner();
                return;
            }
            if (found.size() > 1) {
                notifications.create(messageBundle.getMessage("barcode.ambiguous"))
                        .withType(Notifications.Type.ERROR)
                        .show();
                focusScanner();
                return;
            }
            item = found.get(0);

            // prefer the unit/price config registered for the selected warehouse
            if (wh != null) {
                List<ItemUnitPrice> inWh = dataManager.load(ItemUnitPrice.class)
                        .query("select p from ItemUnitPrice p join fetch p.unit "
                                + "where p.item = :i and p.warehouse = :w")
                        .parameter("i", item)
                        .parameter("w", wh)
                        .list();
                if (inWh.size() == 1) {
                    iup = inWh.get(0);
                } else if (inWh.size() > 1) {
                    notifications.create(messageBundle.getMessage("barcode.ambiguousUnit")
                                    .replace("{0}", item.getName()))
                            .withType(Notifications.Type.ERROR)
                            .show();
                    setStatus(messageBundle.getMessage("barcode.statusReady"));
                    focusScanner();
                    return;
                }
            }
        }

        if (code.equals(currentBarcode)) {
            // same item keeps counting up; autosave
            resolvedIup = iup;
            currentQty = currentQty.add(BigDecimal.ONE);
            updatingUi = true;
            try {
                qtyField.setValue(currentQty.doubleValue());
            } finally {
                updatingUi = false;
            }
        } else {
            // barcode changed: previous line already saved at its current qty
            currentBarcode = code;
            currentItem = item;
            resolvedIup = iup;
            currentOrderItem = null;
            currentQty = BigDecimal.ONE;
            updatingUi = true;
            try {
                qtyField.setValue(1.0);
            } finally {
                updatingUi = false;
            }
        }

        if (!commitLine()) {
            focusScanner();
            return;
        }
        refreshLines();
        updateCurrentCard();
        setStatus(savedStatus(item, currentQty));
        focusScanner();
    }

    private boolean commitLine() {
        if (currentItem == null || currentBarcode == null || warehouseField.getValue() == null) {
            return false;
        }
        BigDecimal signed = mode == Mode.TAKE ? currentQty : currentQty.negate();

        try {
            if (sessionOrder == null) {
                Order order = dataManager.create(Order.class);
                order.setWarehouse(warehouseField.getValue());
                order.setUser((User) currentAuthentication.getUser());
                order.setItems(new ArrayList<>());
                dataManager.save(order);
                sessionOrder = order;
            }

            if (currentOrderItem == null) {
                OrderItem line = dataManager.create(OrderItem.class);
                line.setOrder(sessionOrder);
                line.setItem(currentItem);
                if (resolvedIup != null) {
                    line.setUnit(resolvedIup.getUnit());
                    line.setUnitPriceAtOrder(resolvedIup.getPrice());
                }
                line.setCustomItem(false);
                line.setCustomName(null);
                line.setQuantity(signed);
                dataManager.save(line);
                sessionOrder.getItems().add(line);
                currentOrderItem = line;
                return true;
            }

            currentOrderItem.setQuantity(signed);
            dataManager.save(currentOrderItem);
            return true;
        } catch (Exception e) {
            notifications.create(messageBundle.getMessage("barcode.saveFailed")
                            .replace("{0}", e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()))
                    .withType(Notifications.Type.ERROR)
                    .show();
            setStatus(messageBundle.getMessage("barcode.saveFailedShort"));
            return false;
        }
    }

    private void endSession() {
        sessionOrder = null;
        currentBarcode = null;
        currentItem = null;
        resolvedIup = null;
        currentOrderItem = null;
        currentQty = BigDecimal.ONE;
        updatingUi = true;
        try {
            qtyField.setValue(1.0);
        } finally {
            updatingUi = false;
        }
        updateCurrentCard();
    }

    private void setMode(Mode newMode) {
        mode = newMode;
        sessionOrder = null;
        currentBarcode = null;
        currentItem = null;
        resolvedIup = null;
        currentOrderItem = null;
        currentQty = BigDecimal.ONE;

        if (mode == Mode.TAKE) {
            takeBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
            checkInBtn.removeThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
        } else {
            checkInBtn.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
            takeBtn.removeThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
        }
        qtyField.setVisible(true);

        updatingUi = true;
        try {
            qtyField.setValue(1.0);
        } finally {
            updatingUi = false;
        }

        updateCurrentCard();
        setStatus(messageBundle.getMessage("barcode.statusReady"));
        focusScanner();
    }

    private Optional<Warehouse> nearestWarehouse(GeoLocator.GeoPoint point) {
        Warehouse best = null;
        double bestDist = GPS_RANGE_M + 1;
        for (Warehouse w : warehousesDl.getContainer().getItems()) {
            if (w.getLatitude() == null || w.getLongitude() == null) {
                continue;
            }
            double d = GeoLocator.distanceMeters(point.latitude(), point.longitude(),
                    w.getLatitude(), w.getLongitude());
            if (d <= GPS_RANGE_M && d < bestDist) {
                best = w;
                bestDist = d;
            }
        }
        return Optional.ofNullable(best);
    }

    private void refreshLines() {
        if (currentBarcode == null || currentItem == null) {
            return;
        }
        String itemLabel = currentItem.getName();
        String unitLabel = (resolvedIup != null && resolvedIup.getUnit() != null)
                ? resolvedIup.getUnit().getName() : "";
        BigDecimal shown = currentQty;
        BigDecimal remaining = computeRemaining(currentItem);
        boolean replaced = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).barcode().equals(currentBarcode)) {
                lines.set(i, new ScanLine(currentBarcode, itemLabel, unitLabel, shown, remaining));
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            lines.add(0, new ScanLine(currentBarcode, itemLabel, unitLabel, shown, remaining));
        }
        linesGrid.getDataProvider().refreshAll();
    }

    /** Stock for this item in the selected warehouse: 0 - SUM(order_item.quantity). */
    private BigDecimal computeRemaining(Item item) {
        if (item == null || warehouseField.getValue() == null) {
            return null;
        }
        try {
            List<OrderItem> rows = dataManager.load(OrderItem.class)
                    .query("select oi from ORDER_ITEM oi join oi.order o "
                            + "where oi.item = :i and o.warehouse = :w")
                    .parameter("i", item)
                    .parameter("w", warehouseField.getValue())
                    .list();
            BigDecimal sum = BigDecimal.ZERO;
            for (OrderItem row : rows) {
                if (row.getQuantity() != null) {
                    sum = sum.add(row.getQuantity());
                }
            }
            return sum.negate().stripTrailingZeros();
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(BarcodeScanView.class)
                    .warn("computeRemaining failed for item {}", item.getId(), e);
            return null;
        }
    }

    private String savedStatus(Item item, BigDecimal qty) {
        String text = messageBundle.getMessage("barcode.savedLine")
                .replace("{0}", item == null ? "" : item.getName())
                .replace("{1}", qty.toPlainString());
        BigDecimal rem = computeRemaining(item);
        if (rem != null) {
            text += " · " + messageBundle.getMessage("barcode.remaining")
                    .replace("{0}", rem.toPlainString());
        }
        return text;
    }

    private void updateCurrentCard() {
        if (currentItem == null) {
            currentItemLabel.setText(
                    messageBundle.getMessage("barcode.noCurrent"));
        } else {
            String text = currentItem.getName() + " × " + currentQty.toPlainString();
            BigDecimal rem = computeRemaining(currentItem);
            if (rem != null) {
                text += " · " + messageBundle.getMessage("barcode.remaining")
                        .replace("{0}", rem.toPlainString());
            }
            currentItemLabel.setText(text);
        }
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void focusScanner() {
        getElement().executeJs("$0.focus()", barcodeField.getElement());
    }
}