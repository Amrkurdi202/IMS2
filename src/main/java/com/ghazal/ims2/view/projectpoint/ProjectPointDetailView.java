package com.ghazal.ims2.view.projectpoint;

import com.ghazal.ims2.entity.ProjectPoint;
import com.ghazal.ims2.entity.ProjectPointRepository;
import com.ghazal.ims2.view.AutoSaveDetailView;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;


@Route(value = "ProjectPoint/:id", layout = MainView.class)
@ViewController(id = "ProjectPoint.detail")
@ViewDescriptor(path = "project-point-detail-view.xml")
@EditedEntityContainer("ProjectPointDc")
public class ProjectPointDetailView extends AutoSaveDetailView<ProjectPoint> {
    
    @Autowired
    private ProjectPointRepository projectPointRepository;


    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    public void onSaveAndCloseButtonClick(final ClickEvent<JmixButton> event) {
        projectPointRepository.save(getEditedEntity());
    }
}
