package com.ghazal.ims2.view.project;

import com.ghazal.ims2.entity.Project;
import com.ghazal.ims2.entity.Room;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.jmix.core.EntityStates;
import io.jmix.flowui.action.list.CreateAction;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import io.jmix.flowui.kit.action.Action;

@Route(value = "projects/:id", layout = MainView.class)
@ViewController("Project.detail")
@ViewDescriptor("project-detail-view.xml")
@EditedEntityContainer("projectDc")
public class ProjectDetailView extends StandardDetailView<Project> {

    @ViewComponent
    private JmixImage<Object> image;

    @Autowired
    private EntityStates entityStates;

    // Rooms UI
    @ViewComponent
    private DataGrid<Room> roomsDataGrid;

    @ViewComponent
    private JmixButton roomsCreateBtn;

    @ViewComponent
    private JmixButton roomsEditBtn;

    @ViewComponent
    private JmixButton roomsRemoveBtn;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        updateRoomsAvailability();
        setupRoomCreateInitializer();
    }

    private void updateRoomsAvailability() {
        // New project => not persisted => no id => disable room actions
        boolean persisted = getEditedEntity() != null && !entityStates.isNew(getEditedEntity());

        roomsCreateBtn.setEnabled(persisted);
        roomsEditBtn.setEnabled(persisted);
        roomsRemoveBtn.setEnabled(persisted);

        roomsDataGrid.getAction("create").setEnabled(persisted);
        roomsDataGrid.getAction("edit").setEnabled(persisted);
        roomsDataGrid.getAction("remove").setEnabled(persisted);

        // optional (strong disable): disable the whole grid too
        roomsDataGrid.setEnabled(persisted);
    }

    private void setupRoomCreateInitializer() {
        Action action = roomsDataGrid.getAction("create");

        if (action instanceof CreateAction<?> createAction) {
            createAction.setInitializer(entity -> {
                Room room = (Room) entity;
                room.setProject(getEditedEntity());
            });
        }
    }

    /**
     * When the user uploads an image, immediately show a preview.
     */
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
