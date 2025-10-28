package com.ghazal.ims2.view.project;

import com.ghazal.ims2.entity.Project;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "projects/:id", layout = MainView.class)
@ViewController(id = "Project.detail")
@ViewDescriptor(path = "project-detail-view.xml")
@EditedEntityContainer("projectDc")
public class ProjectDetailView extends StandardDetailView<Project> {

    @Autowired
    private UiComponents uiComponents;

    @ViewComponent("photoBox")
    private com.vaadin.flow.component.orderedlayout.VerticalLayout photoBox;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        showImage();
    }

    @Subscribe(id = "projectDc", target = Target.DATA_CONTAINER)
    public void onProjectChanged(InstanceContainer.ItemPropertyChangeEvent<Project> event) {
        if ("image".equals(event.getProperty())) {
            showImage();
        }
    }

    private void showImage() {
        photoBox.removeAll();
        Project project = getEditedEntity();
        byte[] photo = project.getImage();
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
