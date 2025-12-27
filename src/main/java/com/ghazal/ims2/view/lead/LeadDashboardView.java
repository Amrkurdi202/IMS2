package com.ghazal.ims2.view.lead;

import com.ghazal.ims2.entity.AttendanceAction;
import com.ghazal.ims2.entity.AttendanceRecord;
import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.entity.Order;
import com.ghazal.ims2.entity.OrderItem;
import com.ghazal.ims2.entity.Project;
import com.ghazal.ims2.entity.User;
import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.service.AttendanceService;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.vaadin.flow.component.html.Div;

@Route(value = "lead-dashboard", layout = MainView.class)
@ViewController("LeadDashboard.view")
@ViewDescriptor("lead-dashboard-view.xml")
public class LeadDashboardView extends StandardView {

    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private Notifications notifications;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    // ---------- TOP: Attendance ----------

    @ViewComponent
    private CollectionContainer<AttendanceRecord> attendanceDc;

    // ---------- ITEMS FOR CARDS ----------

    @ViewComponent
    private CollectionContainer<Item> itemsDc;

    @ViewComponent
    private CollectionLoader<Item> itemsDl;

    // ---------- WAREHOUSES / PROJECTS ----------

    @ViewComponent
    private CollectionContainer<Warehouse> warehousesDc;

    @ViewComponent
    private CollectionLoader<Warehouse> warehousesDl;

    @ViewComponent
    private CollectionContainer<Project> projectsDc;

    @ViewComponent
    private CollectionLoader<Project> projectsDl;

    // ---------- UI COMPONENTS FOR CARDS & SEARCH ----------

    @ViewComponent
    private Div itemsCardsContainer;

    @ViewComponent
    private VerticalLayout customItemCard;

    @ViewComponent
    private TypedTextField<String> itemsSearchField;

    // ---------- BOTTOM: Quick items "cart" ----------

    @ViewComponent
    private JmixComboBox<Warehouse> warehouseCombo;

    @ViewComponent
    private JmixComboBox<Project> projectCombo;

    @ViewComponent
    private KeyValueCollectionContainer quickItemsDc;

    @ViewComponent
    private DataGrid<KeyValueEntity> quickItemsTable;

    @ViewComponent
    private TypedTextField<String> customItemNameField;

    @ViewComponent
    private Button addCustomItemBtn;

    // ----------------- lifecycle -----------------

    @Subscribe
    public void onInit(InitEvent event) {
        reloadAttendance();
        quickItemsDc.getMutableItems().clear();
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        // load items for cards
        itemsDl.load();

        // load warehouses & projects
        warehousesDl.load();
        projectsDl.load();

        // bind combos
        warehouseCombo.setItems(warehousesDc.getItems());
        warehouseCombo.setItemLabelGenerator(wh -> wh == null ? "" : wh.getName());

        projectCombo.setItems(projectsDc.getItems());
        projectCombo.setItemLabelGenerator(pr -> pr == null ? "" : pr.getName());

        // build initial cards
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

    @Subscribe("breakBtn")
    public void onBreakBtnClick(ClickEvent<Button> event) {
        handleAttendanceAction(AttendanceAction.BREAK, "Break started");
    }

    @Subscribe("breakEndBtn")
    public void onBreakEndBtnClick(ClickEvent<Button> event) {
        handleAttendanceAction(AttendanceAction.BREAK_END, "Break ended");
    }

    private void handleAttendanceAction(AttendanceAction action, String successMessage) {
        try {
            attendanceService.markAction(action);
            notifications.show(successMessage);
            reloadAttendance();
        } catch (IllegalStateException ex) {
            notifications.show(ex.getMessage());
        }
    }

    // ========== DYNAMIC CARDS FROM ITEMS ==========

    private static final int CARDS_PER_ROW = 2;   // better for mobile

    private void rebuildItemCards(String filter) {
        itemsCardsContainer.removeAll();

        // add custom card first (make it full row using CSS class)
        customItemCard.addClassName("custom-full");
        customItemCard.addClassName("item-card");
        itemsCardsContainer.add(customItemCard);

        String normalized = (filter == null) ? "" : filter.trim().toLowerCase();

        List<Item> filtered = itemsDc.getItems().stream()
                .filter(item -> {
                    if (normalized.isEmpty()) return true;
                    String name = item.getName();
                    return name != null && name.toLowerCase().contains(normalized);
                })
                .collect(Collectors.toList());

        for (Item item : filtered) {
            itemsCardsContainer.add(createItemCard(item));
        }
    }


    private VerticalLayout createItemCard(Item item) {
        VerticalLayout card = uiComponents.create(VerticalLayout.class);
        card.setWidthFull();
        card.addClassName("item-card");

        card.setSpacing(false);    // ✅ smaller

        card.addClassNames("border", "rounded-l", "shadow-s");

        // image from Item.image (byte[])
        Image img = uiComponents.create(Image.class);
        byte[] imageBytes = item.getImage();

        if (imageBytes != null && imageBytes.length > 0) {
            StreamResource resource = new StreamResource(
                    "item.png",
                    () -> new ByteArrayInputStream(imageBytes)
            );
            img.setSrc(resource);
            img.setWidth("100%");
            img.setHeight("70px");
            img.getStyle().set("object-fit", "cover");

        } else {
            img.setVisible(false);
        }

        Span title = uiComponents.create(Span.class);
        title.setText(item.getName() != null ? item.getName() : "(no name)");

        Span desc = uiComponents.create(Span.class);
        desc.setText("Item from catalog");

        Button addBtn = uiComponents.create(Button.class);
        addBtn.setText("+ Add");

        addBtn.getElement().getThemeList().add("small");

        addBtn.addClickListener(e -> addOrIncreaseItem(item.getName()));

        card.add(img, title, desc, addBtn);
        return card;
    }

    // ========== SEARCH ==========

    @Subscribe("itemsSearchField")
    public void onItemsSearchFieldValueChange(
            ComponentValueChangeEvent<TypedTextField<String>, String> event) {
        rebuildItemCards(event.getValue());
    }

    @Subscribe("itemsSearchBtn")
    public void onItemsSearchBtnClick(ClickEvent<Button> event) {
        rebuildItemCards(itemsSearchField.getValue());
    }

    // ========== HELPERS FOR TYPES ==========

    private int safeInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private String safeString(Object value) {
        return value != null ? value.toString() : null;
    }

    private void setQuantity(KeyValueEntity e, int qty) {
        e.setValue("quantity", String.valueOf(qty));
    }

    // ========== QUICK ITEMS TABLE LOGIC ==========

    private void addOrIncreaseItem(String itemName) {
        final String warehouseName =
                (warehouseCombo != null && warehouseCombo.getValue() != null)
                        ? warehouseCombo.getValue().getName()
                        : null;

        final String projectName =
                (projectCombo != null && projectCombo.getValue() != null)
                        ? projectCombo.getValue().getName()
                        : null;

        KeyValueEntity existing = quickItemsDc.getItems().stream()
                .filter(e ->
                        Objects.equals(itemName, safeString(e.getValue("item"))) &&
                                Objects.equals(warehouseName, safeString(e.getValue("warehouse"))) &&
                                Objects.equals(projectName, safeString(e.getValue("project"))))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            int qty = safeInt(existing.getValue("quantity"));
            setQuantity(existing, qty + 1);
            quickItemsDc.replaceItem(existing);
        } else {
            KeyValueEntity entity = new KeyValueEntity();
            entity.setValue("item", itemName);
            entity.setValue("warehouse", warehouseName);
            entity.setValue("project", projectName);
            setQuantity(entity, 1);
            quickItemsDc.getMutableItems().add(entity);
        }
    }

    // ---- custom item card ----

    @Subscribe("customItemNameField")
    public void onCustomItemNameFieldValueChange(
            ComponentValueChangeEvent<TypedTextField<String>, String> event) {
        String value = event.getValue();
        boolean hasText = value != null && !value.trim().isEmpty();
        addCustomItemBtn.setEnabled(hasText);
    }

    @Subscribe("addCustomItemBtn")
    public void onAddCustomItemBtnClick(ClickEvent<Button> event) {
        String name = customItemNameField.getValue();
        if (name == null || name.trim().isEmpty()) {
            notifications.show("Enter custom item name first.");
            return;
        }

        String trimmed = name.trim();
        addOrIncreaseItem(trimmed);

        customItemNameField.clear();
        addCustomItemBtn.setEnabled(false);
    }

    // ---- bottom +/-/delete buttons ----

    @Subscribe("increaseQtyBtn")
    public void onIncreaseQtyBtnClick(ClickEvent<Button> event) {
        KeyValueEntity selected = quickItemsTable.getSingleSelectedItem();
        if (selected == null) {
            notifications.show("Select a line first.");
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
            notifications.show("Select a line first.");
            return;
        }
        int qty = safeInt(selected.getValue("quantity"));
        setQuantity(selected, qty - 1);   // ✅ negatives allowed
        quickItemsDc.replaceItem(selected);
    }

    @Subscribe("deleteLineBtn")
    public void onDeleteLineBtnClick(ClickEvent<Button> event) {
        KeyValueEntity selected = quickItemsTable.getSingleSelectedItem();
        if (selected == null) {
            notifications.show("Select a line first.");
            return;
        }
        quickItemsDc.getMutableItems().remove(selected);
    }

    // ------- SAVE ORDER -------

    @Subscribe("saveOrderBtn")
    public void onSaveOrderBtnClick(ClickEvent<Button> event) {
        if (quickItemsDc.getItems().isEmpty()) {
            notifications.show("Add at least one item to the table.");
            return;
        }

        Warehouse warehouse = warehouseCombo.getValue();
        Project project = projectCombo.getValue();

        if (warehouse == null || project == null) {
            notifications.show("Select warehouse and project first.");
            return;
        }

        // create Order and set warehouse/project
        Order order = dataManager.create(Order.class);
        order.setWarehouse(warehouse);
        order.setProject(project);
        order.setItems(new java.util.ArrayList<>());

        User user = (User) currentAuthentication.getUser();
        order.setUser(user);

        for (KeyValueEntity row : quickItemsDc.getItems()) {
            String lineName = safeString(row.getValue("item"));
            int qtyInt = safeInt(row.getValue("quantity"));

            OrderItem line = dataManager.create(OrderItem.class);
            line.setQuantity(java.math.BigDecimal.valueOf(qtyInt));

            Item catalogItem = itemsDc.getItems().stream()
                    .filter(i -> Objects.equals(i.getName(), lineName))
                    .findFirst()
                    .orElse(null);

            if (catalogItem != null) {
                line.setItem(catalogItem);
                line.setCustomItem(false);
                line.setCustomName(null);
            } else {
                line.setCustomItem(true);
                line.setCustomName(lineName);
            }

            line.setOrder(order);
            order.getItems().add(line);
        }

        dataManager.save(order);
        notifications.show("Order saved successfully.");
        quickItemsDc.getMutableItems().clear();
    }


}
