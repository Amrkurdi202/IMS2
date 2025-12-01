package com.ghazal.ims2.view.order;

import com.ghazal.ims2.entity.Order;
import com.ghazal.ims2.entity.OrderItem;
import com.ghazal.ims2.view.AutoSaveDetailView;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.Metadata;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import com.ghazal.ims2.entity.User;
import io.jmix.core.DataManager;
import io.jmix.core.security.CurrentAuthentication;

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

    // bottom Qty +/- buttons
    @ViewComponent
    private JmixButton qtyMinusBtn;

    @ViewComponent
    private JmixButton qtyPlusBtn;


    @Subscribe
    public void onInitEntity(InitEntityEvent<Order> event) {
        // 1) If the authenticated user IS your app User entity, just cast it
        Object authUser = currentAuthentication.getUser();
        if (authUser instanceof User appUser) {
            event.getEntity().setUser(appUser);
            return;
        }

        // 2) Otherwise, fall back to DB lookup by username – but safely
        String username = currentAuthentication.getUser().getUsername();

        dataManager.load(User.class)
                .query("select u from User u where u.username = :username")
                .parameter("username", username)
                .optional()                      // <-- instead of .one()
                .ifPresent(event.getEntity()::setUser);
    }

    // ---------- Add item ----------

    @Subscribe(id = "addItemBtn", subject = "clickListener")
    public void onAddItemBtnClick(ClickEvent<JmixButton> event) {
        OrderItem line = metadata.create(OrderItem.class);
        line.setOrder(getEditedEntity());
        line.setQuantity(BigDecimal.ZERO);
        itemsDc.getMutableItems().add(line);

        // start inline editing for the new row
        itemsDataGrid.getEditor().editItem(line);
    }

    // ---------- Qty +/- on selected row ----------

    @Subscribe(id = "qtyMinusBtn", subject = "clickListener")
    public void onQtyMinusBtnClick(ClickEvent<JmixButton> event) {
        adjustSelectedQuantity(-1);
    }

    @Subscribe(id = "qtyPlusBtn", subject = "clickListener")
    public void onQtyPlusBtnClick(ClickEvent<JmixButton> event) {
        adjustSelectedQuantity(+1);
    }

    private void adjustSelectedQuantity(int delta) {
        OrderItem selected = itemsDataGrid.getSingleSelectedItem();
        if (selected == null) {
            notifications.show("Select an item first");
            return;
        }

        // Do NOT change custom items with these buttons
        if (Boolean.TRUE.equals(selected.getCustomItem())) {
            notifications.show("For custom items, change the Qty/Price value directly.");
            return;
        }

        BigDecimal q = selected.getQuantity();
        if (q == null) {
            q = BigDecimal.ZERO;
        }
        q = q.add(BigDecimal.valueOf(delta));   // can go negative (returns)
        selected.setQuantity(q);

        // refresh grid display
        itemsDataGrid.getDataProvider().refreshAll();
    }

    // ---------- Save & close ----------

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    public void onSaveAndCloseButtonClick(ClickEvent<JmixButton> event) {
        if (itemsDataGrid.getEditor().isOpen()) {
            itemsDataGrid.getEditor().save();
        }
        getViewData().getDataContext().save();
        closeWithSave();
    }

}