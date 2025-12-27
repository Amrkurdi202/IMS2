package com.ghazal.ims2.view.report;

import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.entity.ItemUnitPrice;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import com.ghazal.ims2.view.main.MainView;

@Route(value = "admin/item-warehouses-report", layout = MainView.class)
@ViewController("ItemWarehousesReportView")
@ViewDescriptor("item-warehouses-report-view.xml")
public class ItemWarehousesReportView extends StandardView {

    @ViewComponent
    private EntityComboBox<Item> itemField;

    @ViewComponent
    private TypedTextField<String> qField;

    @ViewComponent
    private CollectionLoader<Item> itemsDl;

    @ViewComponent
    private CollectionLoader<ItemUnitPrice> itemUnitPricesDl;

    @ViewComponent
    private CollectionContainer<ItemUnitPrice> itemUnitPricesDc;

    @ViewComponent("grid")
    private io.jmix.flowui.component.grid.DataGrid<ItemUnitPrice> grid;

    private final Notifications notifications;

    public ItemWarehousesReportView(Notifications notifications) {
        this.notifications = notifications;
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        itemsDl.load();
        itemUnitPricesDc.getMutableItems().clear();

        // renderer for Type column (Internal / External)
        grid.getColumnByKey("type").setRenderer(new ComponentRenderer<>(iup -> {
            boolean internal = iup != null
                    && iup.getWarehouse() != null
                    && Boolean.TRUE.equals(iup.getWarehouse().getInternal());
            return new Span(internal ? "Internal" : "External");
        }));
    }

    @Subscribe("searchBtn")
    public void onSearchBtnClick(ClickEvent<Button> event) {
        loadGrid();
    }

    @Subscribe("resetBtn")
    public void onResetBtnClick(ClickEvent<Button> event) {
        itemField.clear();
        qField.clear();
        itemUnitPricesDc.getMutableItems().clear();
    }

    private void loadGrid() {
        Item item = itemField.getValue();
        if (item == null) {
            notifications.create("Please select an item.")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        String q = qField.getValue();
        itemUnitPricesDl.setParameter("item", item);
        itemUnitPricesDl.setParameter("q", q);
        itemUnitPricesDl.load();
    }
}
