package com.ghazal.ims2.view.lead;

import com.ghazal.ims2.entity.*;
import com.ghazal.ims2.service.AttendanceService;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.jmix.core.DataManager;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.KeyValueCollectionContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "lead-dashboard", layout = MainView.class)
@ViewController("LeadDashboard.view")
@ViewDescriptor("lead-dashboard-view.xml")
public class LeadDashboardView extends StandardView {

    @Autowired private AttendanceService attendanceService;
    @Autowired private Notifications notifications;
    @Autowired private UiComponents uiComponents;
    @Autowired private DataManager dataManager;
    @Autowired private CurrentAuthentication currentAuthentication;
    @Autowired private MessageBundle messageBundle;

    // ---------- TOP: Attendance ----------
    @ViewComponent private CollectionContainer<AttendanceRecord> attendanceDc;

    // ---------- Warehouses / Projects ----------
    @ViewComponent private CollectionContainer<Warehouse> warehousesDc;
    @ViewComponent private CollectionLoader<Warehouse> warehousesDl;

    @ViewComponent private CollectionContainer<Project> projectsDc;
    @ViewComponent private CollectionLoader<Project> projectsDl;

    // ---------- UI ----------
    @ViewComponent private Div itemsCardsContainer;
    @ViewComponent private VerticalLayout customItemCard;
    @ViewComponent private TypedTextField<String> itemsSearchField;

    // ---------- Bottom cart ----------
    @ViewComponent private JmixComboBox<Warehouse> warehouseCombo;
    @ViewComponent private JmixComboBox<Project> projectCombo;

    @ViewComponent private KeyValueCollectionContainer quickItemsDc;
    @ViewComponent private DataGrid<KeyValueEntity> quickItemsTable;

    @ViewComponent private TypedTextField<String> customItemNameField;
    @ViewComponent private Button addCustomItemBtn;

    // ✅ Build cards from ItemUnitPrice only
    private final Map<Long, List<ItemUnitPrice>> unitPricesByItemId = new HashMap<>();
    private final Map<Long, Item> itemsById = new LinkedHashMap<>();

    // ----------------- lifecycle -----------------

    @Subscribe
    public void onInit(InitEvent event) {
        reloadAttendance();
        quickItemsDc.getMutableItems().clear();
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        warehousesDl.load();
        projectsDl.load();

        warehouseCombo.setItems(warehousesDc.getItems());
        warehouseCombo.setItemLabelGenerator(w -> w == null ? "" : w.getName());

        projectCombo.setItems(projectsDc.getItems());
        projectCombo.setItemLabelGenerator(p -> p == null ? "" : p.getName());

        // ✅ no default selection
        warehouseCombo.clear();
        projectCombo.clear();

        // show only custom card until user selects warehouse
        unitPricesByItemId.clear();
        itemsById.clear();
        rebuildItemCards(null);
    }



    private void reloadAttendance() {
        attendanceDc.setItems(attendanceService.getTodayRecords());
    }

    // ------------- Attendance buttons ------------

    @Subscribe("inBtn")
    public void onInBtnClick(ClickEvent<Button> event) {
        handleAttendanceAction(AttendanceAction.IN, "Checked IN");
    }

    @Subscribe("outBtn")
    public void onOutBtnClick(ClickEvent<Button> event) {
        handleAttendanceAction(AttendanceAction.OUT, "Checked OUT");
    }

//    @Subscribe("breakBtn")
//    public void onBreakBtnClick(ClickEvent<Button> event) {
//        handleAttendanceAction(AttendanceAction.BREAK, "Break started");
//    }
//
//    @Subscribe("breakEndBtn")
//    public void onBreakEndBtnClick(ClickEvent<Button> event) {
//        handleAttendanceAction(AttendanceAction.BREAK_END, "Break ended");
//    }

    private void handleAttendanceAction(AttendanceAction action, String successMessage) {
        try {
            attendanceService.markAction(action);
            notifications.show(successMessage);
            reloadAttendance();
        } catch (IllegalStateException ex) {
            notifications.show(ex.getMessage());
        }
    }

    // ================== Warehouse changed ==================

    @Subscribe("warehouseCombo")
    public void onWarehouseComboValueChange(ComponentValueChangeEvent<JmixComboBox<Warehouse>, Warehouse> event) {
        quickItemsDc.getMutableItems().clear();

        if (event.getValue() == null) {
            unitPricesByItemId.clear();
            itemsById.clear();
            rebuildItemCards(itemsSearchField.getValue());
            return;
        }

        refreshDefsForWarehouse();
        rebuildItemCards(itemsSearchField.getValue());
    }

    private void refreshDefsForWarehouse() {
        unitPricesByItemId.clear();
        itemsById.clear();

        Warehouse wh = warehouseCombo.getValue();
        if (wh == null || wh.getId() == null) return;

        List<ItemUnitPrice> defs = dataManager.load(ItemUnitPrice.class)
                .query("select iup from ItemUnitPrice iup " +
                        "join fetch iup.item " +
                        "join fetch iup.unit " +
                        "where iup.warehouse.id = :whId")
                .parameter("whId", wh.getId())
                .list();

        for (ItemUnitPrice iup : defs) {
            if (iup.getItem() == null || iup.getUnit() == null) continue;
            Long itemId = iup.getItem().getId();
            if (itemId == null) continue;

            unitPricesByItemId.computeIfAbsent(itemId, k -> new ArrayList<>()).add(iup);
            itemsById.putIfAbsent(itemId, iup.getItem());
        }
    }

    // ================== Cards ==================

    private void rebuildItemCards(String filter) {
        itemsCardsContainer.removeAll();

        // always show custom card
        itemsCardsContainer.add(customItemCard);

        // no warehouse => only custom card
        if (warehouseCombo.getValue() == null) return;

        String normalized = filter == null ? "" : filter.trim().toLowerCase();

        List<Item> filtered = itemsById.values().stream()
                .filter(it -> {
                    if (normalized.isEmpty()) return true;
                    String name = it.getName();
                    return name != null && name.toLowerCase().contains(normalized);
                })
                .sorted(Comparator.comparing(i -> i.getName() == null ? "" : i.getName()))
                .collect(Collectors.toList());

        for (Item item : filtered) {
            itemsCardsContainer.add(createItemCard(item));
        }
    }

    private VerticalLayout createItemCard(Item item) {
        VerticalLayout card = uiComponents.create(VerticalLayout.class);
        card.setWidthFull();
        card.addClassName("item-card");
        card.setSpacing(false);
        card.addClassNames("border", "rounded-l", "shadow-s");

        Image img = uiComponents.create(Image.class);
        byte[] imageBytes = item.getImage();
        if (imageBytes != null && imageBytes.length > 0) {
            StreamResource resource = new StreamResource("item.png", () -> new ByteArrayInputStream(imageBytes));
            img.setSrc(resource);
            img.setWidth("100%");
            img.setHeight("70px");
            img.getStyle().set("object-fit", "cover");
        } else {
            img.setVisible(false);
        }

        Span title = uiComponents.create(Span.class);
        title.setText(item.getName() != null ? item.getName() : "(no name)");

        HorizontalLayout buttonsRow = uiComponents.create(HorizontalLayout.class);
        buttonsRow.setSpacing(true);

        List<ItemUnitPrice> defs = unitPricesByItemId.getOrDefault(item.getId(), List.of());
        for (ItemUnitPrice iup : defs) {
            String unitName = (iup.getUnit() != null && iup.getUnit().getName() != null)
                    ? iup.getUnit().getName()
                    : "Unit";

            Button unitBtn = uiComponents.create(Button.class);
            unitBtn.setText(unitName + " +");
            unitBtn.getElement().getThemeList().add("small");
            unitBtn.addClickListener(e -> addOrIncreaseCatalogItem(iup));
            buttonsRow.add(unitBtn);
        }

        card.add(img, title, buttonsRow);
        return card;
    }

    // Search
    @Subscribe("itemsSearchField")
    public void onItemsSearchFieldValueChange(ComponentValueChangeEvent<TypedTextField<String>, String> event) {
        rebuildItemCards(event.getValue());
    }

    @Subscribe("itemsSearchBtn")
    public void onItemsSearchBtnClick(ClickEvent<Button> event) {
        rebuildItemCards(itemsSearchField.getValue());
    }

    // ========== cart helpers ==========

    private int safeInt(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private String safeString(Object value) {
        return value != null ? value.toString() : null;
    }

    private void setQuantity(KeyValueEntity e, int qty) {
        e.setValue("quantity", String.valueOf(qty));
    }

    // add catalog row
    private void addOrIncreaseCatalogItem(ItemUnitPrice iup) {
        String itemName = (iup.getItem() != null) ? iup.getItem().getName() : "(no name)";
        String unitName = (iup.getUnit() != null) ? iup.getUnit().getName() : null;
        String iupId = (iup.getId() != null) ? iup.getId().toString() : null;
        addOrIncreaseRow(itemName, unitName, iupId);
    }

    private void addOrIncreaseCustomItem(String name) {
        addOrIncreaseRow(name, null, null);
    }

    private void addOrIncreaseRow(String itemName, String unitName, String itemUnitPriceId) {
        final String warehouseName = (warehouseCombo.getValue() != null) ? warehouseCombo.getValue().getName() : null;
        final String projectName = (projectCombo.getValue() != null) ? projectCombo.getValue().getName() : null;

        KeyValueEntity existing = quickItemsDc.getItems().stream()
                .filter(e ->
                        Objects.equals(itemName, safeString(e.getValue("item"))) &&
                                Objects.equals(unitName, safeString(e.getValue("unit"))) &&
                                Objects.equals(itemUnitPriceId, safeString(e.getValue("itemUnitPriceId"))) &&
                                Objects.equals(warehouseName, safeString(e.getValue("warehouse"))) &&
                                Objects.equals(projectName, safeString(e.getValue("project")))
                )
                .findFirst()
                .orElse(null);

        if (existing != null) {
            int qty = safeInt(existing.getValue("quantity"));
            setQuantity(existing, qty + 1);
            quickItemsDc.replaceItem(existing);
        } else {
            KeyValueEntity entity = new KeyValueEntity();
            entity.setValue("item", itemName);
            entity.setValue("unit", unitName);
            entity.setValue("itemUnitPriceId", itemUnitPriceId);
            entity.setValue("warehouse", warehouseName);
            entity.setValue("project", projectName);
            setQuantity(entity, 1);
            quickItemsDc.getMutableItems().add(entity);
        }
    }

    // custom item
    @Subscribe("customItemNameField")
    public void onCustomItemNameFieldValueChange(ComponentValueChangeEvent<TypedTextField<String>, String> event) {
        String value = event.getValue();
        addCustomItemBtn.setEnabled(value != null && !value.trim().isEmpty());
    }

    @Subscribe("addCustomItemBtn")
    public void onAddCustomItemBtnClick(ClickEvent<Button> event) {
        if (warehouseCombo.getValue() == null || projectCombo.getValue() == null) {
            notifications.show(messageBundle.getMessage("LeadDashboardView.selectWarehouseAndProject"));
            return;
        }
        String name = customItemNameField.getValue();
        if (name == null || name.trim().isEmpty()) {
            notifications.show(messageBundle.getMessage("LeadDashboardView.enterCustomItemName"));
            return;
        }
        addOrIncreaseCustomItem(name.trim());
        customItemNameField.clear();
        addCustomItemBtn.setEnabled(false);
    }

    // ---- bottom +/-/delete buttons ----

    @Subscribe("increaseQtyBtn")
    public void onIncreaseQtyBtnClick(ClickEvent<Button> event) {
        KeyValueEntity selected = quickItemsTable.getSingleSelectedItem();
        if (selected == null) {
            notifications.show(messageBundle.getMessage("LeadDashboardView.selectLineFirst"));
            return;
        }
        int qty = safeInt(selected.getValue("quantity"));
        setQuantity(selected, qty + 1);
        quickItemsDc.replaceItem(selected);
    }

    @Subscribe("decreaseQtyBtn")
    public void onDecreaseQtyBtnClick(ClickEvent<Button> event) {
        KeyValueEntity selected = quickItemsTable.getSingleSelectedItem();
        if (selected == null) {
            notifications.show(messageBundle.getMessage("LeadDashboardView.selectLineFirst"));
            return;
        }
        int qty = safeInt(selected.getValue("quantity"));
        int newQty = Math.max(1, qty - 1);
        setQuantity(selected, newQty);
        quickItemsDc.replaceItem(selected);
    }

    @Subscribe("deleteLineBtn")
    public void onDeleteLineBtnClick(ClickEvent<Button> event) {
        KeyValueEntity selected = quickItemsTable.getSingleSelectedItem();
        if (selected == null) {
            notifications.show(messageBundle.getMessage("LeadDashboardView.selectLineFirst"));
            return;
        }
        quickItemsDc.getMutableItems().remove(selected);
    }

    // ------- SAVE ORDER -------

    @Subscribe("saveOrderBtn")
    public void onSaveOrderBtnClick(ClickEvent<Button> event) {
        if (quickItemsDc.getItems().isEmpty()) {
            notifications.show(messageBundle.getMessage("LeadDashboardView.addAtLeastOneItem"));
            return;
        }

        Warehouse warehouse = warehouseCombo.getValue();
        Project project = projectCombo.getValue();

        if (warehouse == null || project == null) {
            notifications.show(messageBundle.getMessage("LeadDashboardView.selectWarehouseAndProject"));
            return;
        }

        Order order = dataManager.create(Order.class);
        order.setWarehouse(warehouse);
        order.setProject(project);
        order.setItems(new ArrayList<>());

        User user = (User) currentAuthentication.getUser();
        order.setUser(user);

        for (KeyValueEntity row : quickItemsDc.getItems()) {
            String lineName = safeString(row.getValue("item"));
            String iupIdStr  = safeString(row.getValue("itemUnitPriceId"));
            int qtyInt = Math.max(1, safeInt(row.getValue("quantity")));

            OrderItem line = dataManager.create(OrderItem.class);
            line.setQuantity(BigDecimal.valueOf(qtyInt));
            line.setOrder(order);

            if (iupIdStr != null && !iupIdStr.isBlank()) {
                try {
                    Long iupId = Long.parseLong(iupIdStr);

                    ItemUnitPrice iup = dataManager.load(ItemUnitPrice.class)
                            .id(iupId)
                            .optional()
                            .orElse(null);

                    if (iup != null && iup.getItem() != null) {
                        line.setItem(iup.getItem());
                        line.setCustomItem(false);
                        line.setCustomName(null);
                        line.setUnitPriceAtOrder(iup.getPrice()); // ✅ your setter exists
                    } else {
                        line.setCustomItem(true);
                        line.setCustomName(lineName);
                    }
                } catch (NumberFormatException ex) {
                    line.setCustomItem(true);
                    line.setCustomName(lineName);
                }
            } else {
                line.setCustomItem(true);
                line.setCustomName(lineName);
            }

            order.getItems().add(line);
        }

        dataManager.save(order);
        notifications.show(messageBundle.getMessage("LeadDashboardView.orderSavedSuccessfully"));
        quickItemsDc.getMutableItems().clear();
    }
}
