package com.ghazal.ims2.view.itemunitprice;

import com.ghazal.ims2.entity.ItemUnitPrice;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "itemUnitPrices", layout = MainView.class)
@ViewController(id = "ItemUnitPrice.list")
@ViewDescriptor(path = "item-unit-price-list-view.xml")
@LookupComponent("itemUnitPricesDataGrid")
@DialogMode(width = "64em")
public class ItemUnitPriceListView extends StandardListView<ItemUnitPrice> {
}