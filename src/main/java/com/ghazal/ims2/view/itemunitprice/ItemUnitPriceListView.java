package com.ghazal.ims2.view.itemunitprice;

import com.ghazal.ims2.entity.ItemUnitPrice;
import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;

@Route(value = "itemUnitPrices", layout = MainView.class)
@ViewController(id = "ItemUnitPrice.list")
@ViewDescriptor(path = "item-unit-price-list-view.xml")
@LookupComponent("itemUnitPricesDataGrid")
public class ItemUnitPriceListView extends StandardListView<ItemUnitPrice> {

    @ViewComponent
    private CollectionLoader<ItemUnitPrice> itemUnitPricesDl;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        // IMPORTANT: set the parameter even if null, to avoid "argument not found"
        itemUnitPricesDl.setParameter("warehouse", null);
        itemUnitPricesDl.load();
    }

    public void setWarehouse(Warehouse warehouse) {
        itemUnitPricesDl.setParameter("warehouse", warehouse);
        itemUnitPricesDl.load();
    }
}
