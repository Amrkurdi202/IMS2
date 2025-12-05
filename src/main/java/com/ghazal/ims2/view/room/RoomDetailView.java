package com.ghazal.ims2.view.room;

import com.ghazal.ims2.entity.ProjectPoint;
import com.ghazal.ims2.entity.Room;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.grid.CellFocusEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.Metadata;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

@Route(value = "rooms/:id", layout = MainView.class)
@ViewController("Room.detail")
@ViewDescriptor("room-detail-view.xml")
@EditedEntityContainer("roomDc")
public class RoomDetailView extends StandardDetailView<Room> {

    @Autowired
    private Metadata metadata;

    @ViewComponent
    private CollectionContainer<ProjectPoint> pointsDc;

    @ViewComponent
    private DataGrid<ProjectPoint> pointsDataGrid;

    // ---------- Save & close (use DataContext instead of repository) ---------

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    public void onSaveAndCloseButtonClick(ClickEvent<JmixButton> event) {
        if (pointsDataGrid.getEditor().isOpen()) {
            pointsDataGrid.getEditor().save();
        }
        // Let Jmix handle entity saving & validation via DataContext
        getViewData().getDataContext().save();
        closeWithSave();
    }

    // ---------- Add point ----------------------------------------------------

    @Subscribe(id = "addPointBtn", subject = "clickListener")
    public void onAddPointBtnClick(ClickEvent<JmixButton> event) {
        ProjectPoint newPoint = metadata.create(ProjectPoint.class);
        newPoint.setRoom(getEditedEntity());
        pointsDc.getMutableItems().add(newPoint);
        pointsDataGrid.getEditor().editItem(newPoint);
    }

    // ---------- Merge duplicate points --------------------------------------

    @Subscribe("pointsDataGrid")
    public void onPointsDataGridCellFocus(CellFocusEvent<ProjectPoint> event) {
        event.getItem().ifPresent(item -> {
            long count = pointsDc.getItems()
                    .stream()
                    .filter(it -> it.getPoint() == item.getPoint())
                    .count();
            if (count < 1)
                return;
            if (item.getPoint() != null && item.getQuantity() != null) {

                ProjectPoint duplicate = null;

                for (ProjectPoint existing : pointsDc.getItems()) {
                    if (existing != item
                            && existing.getPoint() != null
                            && existing.getPoint().equals(item.getPoint())) {

                        BigDecimal oldQty = existing.getQuantity() != null
                                ? existing.getQuantity()
                                : BigDecimal.ZERO;
                        BigDecimal newQty = item.getQuantity() != null
                                ? item.getQuantity()
                                : BigDecimal.ZERO;
                        item.setQuantity(oldQty.add(newQty));

                        duplicate = existing;
                        break;
                    }
                }

                if (duplicate != null) {
                    pointsDc.getMutableItems().remove(duplicate);
                }

                pointsDataGrid.getDataProvider().refreshAll();
            }
        });
    }
}
