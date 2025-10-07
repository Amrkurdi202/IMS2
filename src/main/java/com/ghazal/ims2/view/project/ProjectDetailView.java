package com.ghazal.ims2.view.project;

import com.ghazal.ims2.entity.Project;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "projects/:id", layout = MainView.class)
@ViewController(id = "Project.detail")
@ViewDescriptor(path = "project-detail-view.xml")
@EditedEntityContainer("projectDc")
public class ProjectDetailView extends StandardDetailView<Project> {
}