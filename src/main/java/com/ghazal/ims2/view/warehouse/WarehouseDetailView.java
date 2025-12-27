package com.ghazal.ims2.view.warehouse;

import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.entity.WarehouseRepository;
import com.ghazal.ims2.view.AutoSaveDetailView;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.jmix.flowui.component.combobox.JmixComboBox;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.view.*;

import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;

@Route(value = "warehouses/:id", layout = MainView.class)
@ViewController("Warehouse.detail")
@ViewDescriptor("warehouse-detail-view.xml")
@EditedEntityContainer("warehouseDc")
public class WarehouseDetailView extends AutoSaveDetailView<Warehouse> {

    @ViewComponent
    private JmixImage<Object> image;

    @ViewComponent
    private JmixComboBox<Boolean> internalField;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Subscribe
    public void onInit(InitEvent event) {
        internalField.setItems(Boolean.TRUE, Boolean.FALSE);
        internalField.setItemLabelGenerator(v -> Boolean.TRUE.equals(v) ? "Internal" : "External");
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        Warehouse w = getEditedEntity();
        if (w.getInternal() == null) {
            w.setInternal(Boolean.FALSE);
        }
    }

    @Subscribe("imageField")
    public void onImageFieldFileUploadSucceeded(FileUploadSucceededEvent<FileUploadField> event) {
        byte[] bytes = event.getSource().getValue();
        getEditedEntity().setImage(bytes);

        if (bytes != null && bytes.length > 0) {
            image.setSrc(new StreamResource(
                    event.getFileName(),
                    () -> new ByteArrayInputStream(bytes)
            ));
        }
    }

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    public void onSaveAndCloseButtonClick(ClickEvent<JmixButton> event) {
        warehouseRepository.save(getEditedEntity());
        closeWithSave();
    }
}
