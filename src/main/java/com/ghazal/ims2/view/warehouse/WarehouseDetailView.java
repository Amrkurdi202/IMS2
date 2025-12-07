package com.ghazal.ims2.view.warehouse;

import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;

import java.io.ByteArrayInputStream;

@Route(value = "warehouses/:id", layout = MainView.class)
@ViewController("Warehouse.detail")
@ViewDescriptor("warehouse-detail-view.xml")
@EditedEntityContainer("warehouseDc")
public class WarehouseDetailView extends StandardDetailView<Warehouse> {

    @ViewComponent
    private JmixImage<Object> image;

    @Subscribe("imageField")
    public void onImageFieldFileUploadSucceeded(FileUploadSucceededEvent<FileUploadField> event) {
        byte[] bytes = event.getSource().getValue();
        if (bytes != null && bytes.length > 0) {
            image.setSrc(new StreamResource(
                    event.getFileName(),
                    () -> new ByteArrayInputStream(bytes)
            ));
        } else {
            image.setSrc((String) null);
        }
    }
}
