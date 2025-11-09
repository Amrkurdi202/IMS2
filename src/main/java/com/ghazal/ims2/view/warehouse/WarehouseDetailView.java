package com.ghazal.ims2.view.warehouse;

import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.entity.WarehouseRepository;
import com.ghazal.ims2.view.AutoSaveDetailView;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;

@Route(value = "warehouses/:id", layout = MainView.class)
@ViewController(id = "Warehouse.detail")
@ViewDescriptor(path = "warehouse-detail-view.xml")
@EditedEntityContainer("warehouseDc")
public class WarehouseDetailView extends AutoSaveDetailView<Warehouse> {
    
    @ViewComponent
    private JmixImage<Object> image;
    
    @Autowired
    private WarehouseRepository warehouseRepository;

    @Subscribe("imageField")
    public void onImageFieldFileUploadSucceeded(final FileUploadSucceededEvent<FileUploadField> event) {
        image.setSrc(
                new StreamResource(
                        event.getFileName(),
                        () -> new ByteArrayInputStream(event.getSource().getValue())
                )
        );
    }

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    public void onSaveAndCloseButtonClick(final ClickEvent<JmixButton> event) {
        warehouseRepository.save(getEditedEntity());
    }
}
