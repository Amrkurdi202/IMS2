package com.ghazal.ims2.view.warehouse;

import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;

@Route(value = "warehouses", layout = MainView.class)
@ViewController(id = "Warehouse.list")
@ViewDescriptor(path = "warehouse-list-view.xml")
@LookupComponent("warehousesDataGrid")
@DialogMode(width = "64em")
public class WarehouseListView extends StandardListView<Warehouse> {

    @ViewComponent
    private DataGrid<Warehouse> warehousesDataGrid;

    @ViewComponent
    private CollectionLoader<Warehouse> warehousesDl;

    @Autowired
    private UiComponents uiComponents;

    @Autowired
    private DialogWindows dialogWindows;

    @Autowired
    private Notifications notifications;

    @Subscribe
    public void onInit(InitEvent event) {

        // TYPE column (Internal / External)
        warehousesDataGrid.getColumnByKey("type")
                .setRenderer(new ComponentRenderer<>(warehouse -> {
                    boolean internal = warehouse != null && Boolean.TRUE.equals(warehouse.getInternal());
                    return new Span(internal ? "Internal" : "External");
                }));

        // PHOTO column
        warehousesDataGrid.getColumnByKey("photo")
                .setRenderer(new ComponentRenderer<>(warehouse -> {
                    byte[] photo = warehouse != null ? warehouse.getImage() : null;

                    if (photo != null && photo.length > 0) {
                        JmixImage<?> img = uiComponents.create(JmixImage.class);
                        img.setWidth("40px");
                        img.setHeight("40px");
                        img.setSrc(new StreamResource("preview.jpg", () -> new ByteArrayInputStream(photo)));
                        return img;
                    }
                    return new Span("");
                }));
    }

    @Subscribe("editButton")
    public void onEditButtonClick(ClickEvent<Button> event) {
        Warehouse selected = warehousesDataGrid.getSingleSelectedItem();
        if (selected == null) {
            notifications.show("Select a warehouse first");
            return;
        }

        dialogWindows.detail(this, Warehouse.class)
                .editEntity(selected)
                .withAfterCloseListener(afterClose -> warehousesDl.load())
                .open();
    }

    @Subscribe("createButton")
    public void onCreateButtonClick(ClickEvent<Button> event) {
        dialogWindows.detail(this, Warehouse.class)
                .newEntity()
                .withAfterCloseListener(afterClose -> warehousesDl.load())
                .open();
    }
}
