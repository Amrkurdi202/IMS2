package com.ghazal.ims2.view.itemunitprice;

import com.ghazal.ims2.entity.ItemUnitPrice;
import com.ghazal.ims2.entity.ItemUnitPriceRepository;
import com.ghazal.ims2.view.AutoSaveDetailView;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "itemUnitPrices/:id", layout = MainView.class)
@ViewController(id = "ItemUnitPrice.detail")
@ViewDescriptor(path = "item-unit-price-detail-view.xml")
@EditedEntityContainer("itemUnitPriceDc")
public class ItemUnitPriceDetailView extends AutoSaveDetailView<ItemUnitPrice> {
    
    @Autowired
    private ItemUnitPriceRepository itemUnitPriceRepository;

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    public void onSaveAndCloseButtonClick(final ClickEvent<JmixButton> event) {
        itemUnitPriceRepository.save(getEditedEntity());
    }
}
