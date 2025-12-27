package com.ghazal.ims2.view.warehouse;

import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.view.*;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;

@Route(value = "warehouses", layout = MainView.class)
@ViewController(id = "Warehouse.list")
@ViewDescriptor(path = "warehouse-list-view.xml")
@LookupComponent("warehousesDataGrid")
public class WarehouseListView extends StandardListView<Warehouse> {

    @ViewComponent
    private DataGrid<Warehouse> warehousesDataGrid;

    @Autowired
    private UiComponents uiComponents;

    @Subscribe
    public void onInit(InitEvent event) {

        warehousesDataGrid.getColumnByKey("type")
                .setRenderer(new ComponentRenderer<>(warehouse -> {
                    boolean internal = warehouse != null && Boolean.TRUE.equals(warehouse.getInternal());
                    return new Span(internal ? "Internal" : "External");
                }));

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
}
