package com.ghazal.ims2.view.order;

import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.entity.ItemUnitPrice;
import com.ghazal.ims2.entity.Order;
import com.ghazal.ims2.entity.OrderItem;
import com.ghazal.ims2.entity.Unit;
import com.ghazal.ims2.entity.User;
import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.view.AutoSaveDetailView;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.Metadata;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.valuepicker.EntityPicker;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Route(value = "orders/:id", layout = MainView.class)
@ViewController("ims_Order.detail")
@ViewDescriptor("order-detail-view.xml")
@EditedEntityContainer("orderDc")
public class OrderDetailView extends AutoSaveDetailView<Order> {

    @Autowired
    private Metadata metadata;

    @Autowired
    private Notifications notifications;

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Autowired
    private DataManager dataManager;

    @ViewComponent
    private CollectionContainer<OrderItem> itemsDc;

    @ViewComponent
    private DataGrid<OrderItem> itemsDataGrid;

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private MessageBundle messageBundle;

    @ViewComponent
    private EntityPicker<Warehouse> warehouseField;

    @ViewComponent
    private JmixButton addItemBtn;

    // bottom Qty +/- buttons
    @ViewComponent
    private JmixButton qtyMinusBtn;

    @ViewComponent
    private JmixButton qtyPlusBtn;

    // -----------------------------------------------------
    // 1) Auto-fill user when creating a NEW order
    // -----------------------------------------------------
    @Subscribe
    public void onInitEntity(InitEntityEvent<Order> event) {
        Object authUser = currentAuthentication.getUser();

        if (authUser instanceof User appUser) {
            event.getEntity().setUser(appUser);
            return;
        }

        String username = currentAuthentication.getUser().getUsername();
        dataManager.load(User.class)
                .query("select u from User u where u.username = :username")
                .parameter("username", username)
                .optional()
                .ifPresent(event.getEntity()::setUser);
    }

    // -----------------------------------------------------
    // 2) Add item (inline editor)
    // -----------------------------------------------------
    @Subscribe(id = "addItemBtn", subject = "clickListener")
    public void onAddItemBtnClick(ClickEvent<JmixButton> event) {

        if (warehouseField.getValue() == null) {
            notifications.show("Select warehouse first");
            return;
        }

        OrderItem line = metadata.create(OrderItem.class);
        line.setOrder(getEditedEntity());
        line.setQuantity(BigDecimal.ZERO);
        itemsDc.getMutableItems().add(line);

        addItemBtn.setText("Add +"); // will change after selecting ItemUnitPrice

        // start inline editing for the new row
        itemsDataGrid.getEditor().editItem(line);
    }

    // -----------------------------------------------------
    // 3) Qty +/- on selected row (no more reset, skip custom)
    // -----------------------------------------------------
    @Subscribe(id = "qtyMinusBtn", subject = "clickListener")
    public void onQtyMinusBtnClick(ClickEvent<JmixButton> event) {
        adjustSelectedQuantity(-1);
    }

    @Subscribe(id = "qtyPlusBtn", subject = "clickListener")
    public void onQtyPlusBtnClick(ClickEvent<JmixButton> event) {
        adjustSelectedQuantity(+1);
    }

    private void adjustSelectedQuantity(int delta) {
        if (itemsDataGrid.getEditor().isOpen()) {
            itemsDataGrid.getEditor().save();
        }

        OrderItem selected = itemsDataGrid.getSingleSelectedItem();
        if (selected == null) {
            notifications.show(messageBundle.getMessage("OrderDetailView.selectItemFirst"));
            return;
        }

        if (Boolean.TRUE.equals(selected.getCustomItem())) {
            notifications.show(messageBundle.getMessage("OrderDetailView.customItemInstruction"));
            return;
        }

        BigDecimal q = selected.getQuantity();
        if (q == null) q = BigDecimal.ZERO;

        q = q.add(BigDecimal.valueOf(delta));
        selected.setQuantity(q);

        itemsDataGrid.getDataProvider().refreshItem(selected);
    }

    // -----------------------------------------------------
    // 4) Custom Item checkbox editor
    // -----------------------------------------------------
    @Supply(to = "itemsDataGrid.customItem", subject = "editorComponent")
    private Component itemsDataGridCustomItemEditorComponent() {
        Checkbox checkbox = uiComponents.create(Checkbox.class);

        checkbox.addClickListener(event -> {
            if (!itemsDc.getMutableItems().isEmpty() && itemsDataGrid.getEditor().isOpen()) {

                OrderItem item = itemsDataGrid.getEditor().getItem();
                Boolean value = event.getSource().getValue();

                item.setCustomItem(value);

                if (Boolean.TRUE.equals(value)) {
                    // custom item => no catalog item, no unit, no stored unit price
                    item.setItem(null);
                    item.setUnit(null);
                    item.setUnitPriceAtOrder(null);
                    item.setCustomName("");
                    addItemBtn.setText("Add +");
                }
            }
        });

        return checkbox;
    }

    @Supply(to = "itemsDataGrid.customName", subject = "editorComponent")
    private Component itemsDataGridCustomNameEditorComponent() {
        TextField textField = uiComponents.create(TextField.class);

        if (!itemsDc.getMutableItems().isEmpty() && itemsDataGrid.getEditor().isOpen()) {
            OrderItem item = itemsDataGrid.getEditor().getItem();
            if (item.getCustomName() != null) {
                textField.setValue(item.getCustomName());
            }
        }

        textField.addValueChangeListener(event -> {
            if (!itemsDc.getMutableItems().isEmpty() && itemsDataGrid.getEditor().isOpen()) {
                OrderItem item = itemsDataGrid.getEditor().getItem();
                item.setCustomName(textField.getValue());
            }
        });

        return textField;
    }

    // -----------------------------------------------------
    // 5) ✅ Item-Unit combo: choose from ItemUnitPrice (Item - Unit)
    // -----------------------------------------------------
    @Supply(to = "itemsDataGrid.item", subject = "editorComponent")
    private Component itemsDataGridItemEditorComponent() {

        ComboBox<ItemUnitPrice> cb = uiComponents.create(ComboBox.class);
        cb.setClearButtonVisible(true);

        // Show "Item Name - Unit Name" without price
        cb.setItemLabelGenerator(iup ->
                iup.getItem().getName() + " - " + iup.getUnit().getName()
        );

        // Refresh items when combo box is attached (when editor opens)
        cb.addAttachListener(attachEvent -> {
            Warehouse wh = warehouseField.getValue();
            if (wh == null) {
                cb.setItems(Collections.emptyList());
                cb.setPlaceholder("Select warehouse first");
                cb.setReadOnly(true);
                return;
            }

            cb.setReadOnly(false);
            cb.setPlaceholder(null);

            // Load ItemUnitPrice for selected warehouse
            List<ItemUnitPrice> available = dataManager.load(ItemUnitPrice.class)
                    .query("select iup from ItemUnitPrice iup " +
                            "where iup.warehouse = :wh " +
                            "and iup.unit is not null " +
                            "order by iup.item.name, iup.unit.name")
                    .parameter("wh", wh)
                    .list();

            cb.setItems(available);

            // preselect current value (if any)
            if (itemsDataGrid.getEditor().isOpen()) {
                OrderItem line = itemsDataGrid.getEditor().getItem();
                Item currentItem = line.getItem();
                Unit currentUnit = line.getUnit();

                if (currentItem != null && currentUnit != null) {
                    available.stream()
                            .filter(iup -> iup.getItem().equals(currentItem) && iup.getUnit().equals(currentUnit))
                            .findFirst()
                            .ifPresent(cb::setValue);
                }
            }
        });

        cb.addValueChangeListener(e -> {
            if (!itemsDataGrid.getEditor().isOpen()) return;

            OrderItem line = itemsDataGrid.getEditor().getItem();
            ItemUnitPrice iup = e.getValue();

            if (iup == null) {
                line.setItem(null);
                line.setUnit(null);
                line.setUnitPriceAtOrder(null);
                addItemBtn.setText("Add +");
            } else {
                // Set both item and unit from ItemUnitPrice
                line.setItem(iup.getItem());
                line.setUnit(iup.getUnit());
                line.setUnitPriceAtOrder(iup.getPrice());
                addItemBtn.setText(iup.getUnit().getName() + " +");
            }

            itemsDataGrid.getDataProvider().refreshItem(line);
        });

        return cb;
    }

    // -----------------------------------------------------
    // 8) Save & close
    // -----------------------------------------------------
    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    public void onSaveAndCloseButtonClick(ClickEvent<JmixButton> event) {
        if (itemsDataGrid.getEditor().isOpen()) {
            itemsDataGrid.getEditor().save();
        }
        getViewData().getDataContext().save();
        closeWithSave();
    }
}
