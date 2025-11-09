package com.ghazal.ims2.view.project;

import com.ghazal.ims2.entity.Project;
import com.ghazal.ims2.entity.ProjectRepository;
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

@Route(value = "projects/:id", layout = MainView.class)
@ViewController(id = "Project.detail")
@ViewDescriptor(path = "project-detail-view.xml")
@EditedEntityContainer("projectDc")
public class ProjectDetailView extends AutoSaveDetailView<Project> {
    
    @ViewComponent
    private JmixImage<Object> image;
    
    @Autowired
    private ProjectRepository projectRepository;

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
        projectRepository.save(getEditedEntity());
    }
}
