package com.ghazal.ims2.view.item;

import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "items/:id", layout = MainView.class)
@ViewController(id = "Item.detail")
@ViewDescriptor(path = "item-detail-view.xml")
@EditedEntityContainer("itemDc")
public class ItemDetailView extends StandardDetailView<Item> {
}