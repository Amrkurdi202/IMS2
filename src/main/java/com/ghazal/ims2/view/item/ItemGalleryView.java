package com.ghazal.ims2.view.item;

import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import io.jmix.flowui.component.grid.DataGrid;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "item-gallery", layout = MainView.class)
@ViewController("ItemGalleryView")
@ViewDescriptor("item-gallery-view.xml")
public class ItemGalleryView extends StandardView {

    @ViewComponent
    private CollectionContainer<Item> itemsDc;

    @ViewComponent
    private DataGrid<Item> itemsGrid;

    // expose selected item so caller (OrderDetailView) can read it
    public Item getSelectedItem() {
        Item selected = itemsGrid.getSingleSelectedItem();
        if (selected == null && !itemsDc.getItems().isEmpty()) {
            // fallback: first row if nothing explicitly selected
            return itemsDc.getItems().get(0);
        }
        return selected;
    }

    @Subscribe("selectBtn")
    public void onSelectBtnClick(ClickEvent<JmixButton> event) {
        // close dialog with "SELECT" outcome
        close(StandardOutcome.SELECT);
    }

    @Subscribe("closeBtn")
    public void onCloseBtnClick(ClickEvent<JmixButton> event) {
        close(StandardOutcome.CLOSE);
    }
}
