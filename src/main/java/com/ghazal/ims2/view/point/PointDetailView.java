package com.ghazal.ims2.view.point;

import com.ghazal.ims2.entity.Point;
import com.ghazal.ims2.entity.PointRepository;
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

@Route(value = "points/:id", layout = MainView.class)
@ViewController(id = "Point.detail")
@ViewDescriptor(path = "point-detail-view.xml")
@EditedEntityContainer("pointDc")
public class PointDetailView extends AutoSaveDetailView<Point> {

    @ViewComponent
    private JmixImage<Object> image;

    @Autowired
    private PointRepository pointRepository;

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
        pointRepository.save(getEditedEntity());
        closeWithSave();
    }
}
