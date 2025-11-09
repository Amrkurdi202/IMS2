package com.ghazal.ims2.view.order;

import com.ghazal.ims2.entity.Order;
import com.ghazal.ims2.entity.OrderItem;
import com.ghazal.ims2.view.AutoSaveDetailView;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.grid.CellFocusEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.Metadata;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Objects;

@Route(value = "orders/:id", layout = MainView.class)
@ViewController(id = "ims_Order.detail")
@ViewDescriptor(path = "order-detail-view.xml")
@EditedEntityContainer("orderDc")
public class OrderDetailView extends AutoSaveDetailView<Order> {

    @Autowired
    private Metadata metadata;

    @ViewComponent
    private CollectionContainer<OrderItem> itemsDc;

    @ViewComponent
    private DataGrid<OrderItem> itemsDataGrid;

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    public void onSaveAndCloseButtonClick(final ClickEvent<JmixButton> event) {
        itemsDataGrid.getEditor().save(); // flush editor
        getViewData().getDataContext().save(); // commit all containers
        closeWithSave();
    }

    @Subscribe(id = "addPointBtn", subject = "clickListener")
    public void onAddPointBtnClick(final ClickEvent<JmixButton> event) {
        OrderItem orderItem = metadata.create(OrderItem.class);
        orderItem.setOrder(getEditedEntity());
        itemsDc.getMutableItems().add(orderItem);
        itemsDataGrid.getEditor().editItem(orderItem);
    }

    @Subscribe("itemsDataGrid")
    public void onItemsDataGridCellFocus(final CellFocusEvent<OrderItem> event) {
        event.getItem().ifPresent(item -> {
            if (item.getItem() == null || item.getQuantity() == null)
                return;

            OrderItem duplicate = null;

            for (OrderItem existing : itemsDc.getItems()) {
                if (existing != item && existing.getItem() != null
                        && existing.getItem().equals(item.getItem())) {

                    BigDecimal oldQty = existing.getQuantity() != null ? existing.getQuantity() : BigDecimal.ZERO;
                    BigDecimal newQty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                    item.setQuantity(oldQty.add(newQty));

                    duplicate = existing;
                    break;
                }
            }

            if (duplicate != null) {
                itemsDc.getMutableItems().remove(duplicate);
            }

            itemsDataGrid.getDataProvider().refreshAll();
        });
    }

}
