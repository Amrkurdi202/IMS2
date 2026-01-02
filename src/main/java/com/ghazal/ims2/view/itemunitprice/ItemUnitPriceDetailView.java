package com.ghazal.ims2.view.itemunitprice;

import com.ghazal.ims2.entity.ItemUnitPrice;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

@Route(value = "itemUnitPrices/:id", layout = MainView.class)
@ViewController("ItemUnitPrice.detail")
@ViewDescriptor("item-unit-price-detail-view.xml")
@EditedEntityContainer("itemUnitPriceDc")
public class ItemUnitPriceDetailView extends StandardDetailView<ItemUnitPrice> {
}
