package com.ghazal.ims2.view.item;

import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "items", layout = MainView.class)
@ViewController(id = "Item.list")
@ViewDescriptor(path = "item-list-view.xml")
@LookupComponent("itemsDataGrid")
@DialogMode(width = "64em")
public class ItemListView extends StandardListView<Item> {
}