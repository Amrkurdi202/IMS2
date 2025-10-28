package com.ghazal.ims2.view.warehouse;

import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "warehouses", layout = MainView.class)
@ViewController(id = "Warehouse.list")
@ViewDescriptor(path = "warehouse-list-view.xml")
@LookupComponent("warehousesDataGrid")
@DialogMode(width = "64em")
public class WarehouseListView extends StandardListView<Warehouse> {
    @ViewComponent("warehousesDataGrid")
    private DataGrid<Warehouse> warehouseDataGrid;

    @Autowired
    private UiComponents uiComponents;

    @Subscribe
    public void onInit(InitEvent event) {
        warehouseDataGrid.addComponentColumn(warehouse -> {
                    byte[] photo = warehouse.getImage();
                    if (photo != null && photo.length > 0) {
                        JmixImage image = uiComponents.create(JmixImage.class);
                        image.setWidth("40px");
                        image.setHeight("40px");
                        StreamResource streamResource = new StreamResource(
                                "preview.jpg",
                                () -> new java.io.ByteArrayInputStream(photo)
                        );
                        image.setSrc(streamResource);
                        return image;
                    } else {
                        return new Span();
                    }
                })
                .setHeader("Photo")
                .setAutoWidth(true)
                .setFlexGrow(0);
    }
}