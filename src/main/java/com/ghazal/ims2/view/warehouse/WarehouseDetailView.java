package com.ghazal.ims2.view.warehouse;

import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "warehouses/:id", layout = MainView.class)
@ViewController(id = "Warehouse.detail")
@ViewDescriptor(path = "warehouse-detail-view.xml")
@EditedEntityContainer("warehouseDc")
public class WarehouseDetailView extends StandardDetailView<Warehouse> {

    @Autowired
    private UiComponents uiComponents;

    @ViewComponent("photoBox")
    private com.vaadin.flow.component.orderedlayout.VerticalLayout photoBox;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        showImage();
    }

    @Subscribe(id = "warehouseDc", target = Target.DATA_CONTAINER)
    public void onWarehouseChanged(InstanceContainer.ItemPropertyChangeEvent<Warehouse> event) {
        if ("image".equals(event.getProperty())) {
            showImage();
        }
    }

    private void showImage() {
        photoBox.removeAll();
        Warehouse warehouse = getEditedEntity();
        byte[] photo = warehouse.getImage();
        if (photo != null && photo.length > 0) {
            JmixImage image = uiComponents.create(JmixImage.class);
            image.setWidth("300px");
            image.setHeight("300px");
            image.setSrc(new StreamResource(
                    "preview.jpg",
                    () -> new java.io.ByteArrayInputStream(photo)
            ));
            photoBox.add(image);
        } else {
            photoBox.add(new Span("No image"));
        }
    }
}