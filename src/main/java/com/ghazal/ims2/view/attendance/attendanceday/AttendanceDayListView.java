package com.ghazal.ims2.view.attendance.attendanceday;

import com.ghazal.ims2.entity.AttendanceDay;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "attendance-days", layout = MainView.class)
@ViewController("AttendanceDay.list")
@ViewDescriptor("attendance-day-list-view.xml")
@LookupComponent("attendanceDaysDataGrid")
public class AttendanceDayListView extends StandardListView<AttendanceDay> {

    @Autowired
    private Notifications notifications;

    @ViewComponent
    private DataGrid<AttendanceDay> attendanceDaysDataGrid;

    /**
     * Only this button will open the records screen.
     * Double-click on the grid row will just select it.
     */
    @Subscribe("recordsButton")
    public void onRecordsButtonClick(ClickEvent<JmixButton> event) {
        AttendanceDay selected = attendanceDaysDataGrid.getSingleSelectedItem();
        if (selected == null) {
            notifications.show("Please select a day first");
            return;
        }

        UI.getCurrent().navigate("attendance-records?dayId=" + selected.getId());
    }
}
