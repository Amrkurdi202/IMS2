package com.ghazal.ims2.view.order;

import com.ghazal.ims2.entity.Order;
import com.ghazal.ims2.entity.OrderItem;
import com.ghazal.ims2.entity.User;
import com.ghazal.ims2.view.AutoSaveDetailView;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.Metadata;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;

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

    @Supply(to = "itemsDataGrid.customItem", subject = "editorComponent")
    private Component itemsDataGridCustomItemEditorComponent() {
        Checkbox checkbox = uiComponents.create(Checkbox.class);
        checkbox.addClickListener(event -> {
            System.out.println("hi");
            if(!itemsDc.getMutableItems().isEmpty()) {
                OrderItem item = itemsDataGrid.getEditor().getItem();
                Boolean value = event.getSource().getValue();
                item.setCustomItem(value);
                if(value){
                    item.setItem(null);
                }
                else
                    item.setCustomName("");
            }
        });
        return checkbox;
    }

    @Supply(to = "itemsDataGrid.customName", subject = "editorComponent")
    private Component itemsDataGridCustomNameEditorComponent() {
        TextField textField = uiComponents.create(TextField.class);
        if(!itemsDc.getMutableItems().isEmpty()) {
            OrderItem item = itemsDataGrid.getEditor().getItem();
            textField.setValue(item.getCustomName());
        }
        textField.addValueChangeListener(event -> {
            if(!itemsDc.getMutableItems().isEmpty()) {
                OrderItem item = itemsDataGrid.getEditor().getItem();
                item.setCustomName(textField.getValue());
            }
        });
        return textField;
    }
    
    

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

        // Case 1: security user IS your app User entity
        if (authUser instanceof User appUser) {
            event.getEntity().setUser(appUser);
            return;
        }

        // Case 2: find User by username, but safely (no exception)
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
        OrderItem line = metadata.create(OrderItem.class);
        line.setOrder(getEditedEntity());
        line.setQuantity(BigDecimal.ZERO);
        itemsDc.getMutableItems().add(line);

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
        // Save any open editor row first so we don't lose typed values
        if (itemsDataGrid.getEditor().isOpen()) {
            itemsDataGrid.getEditor().save();
        }

        OrderItem selected = itemsDataGrid.getSingleSelectedItem();
        if (selected == null) {
            notifications.show(messageBundle.getMessage("OrderDetailView.selectItemFirst"));
            return;
        }

        // 🚫 Do not use +/- for custom items
        if (Boolean.TRUE.equals(selected.getCustomItem())) {
            notifications.show(messageBundle.getMessage("OrderDetailView.customItemInstruction"));
            return;
        }

        BigDecimal q = selected.getQuantity();
        if (q == null) {
            q = BigDecimal.ZERO;
        }
        q = q.add(BigDecimal.valueOf(delta));   // can go negative (returns)
        selected.setQuantity(q);

        // Refresh only this row
        itemsDataGrid.getDataProvider().refreshItem(selected);
    }

    // -----------------------------------------------------
    // 4) Save & close
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
