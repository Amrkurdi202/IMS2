package com.ghazal.ims2.view.attendance.worker;

import com.ghazal.ims2.entity.AttendanceAction;
import com.ghazal.ims2.entity.AttendanceRecord;
import com.ghazal.ims2.service.AttendanceService;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@ViewController("AttendanceView")
@ViewDescriptor("attendance-view.xml")
@Route(value = "attendance", layout = MainView.class)
public class AttendanceView extends StandardView {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private Notifications notifications;

    @ViewComponent
    private CollectionContainer<AttendanceRecord> attendanceDc;

    @ViewComponent
    private JmixButton inButton;

    @ViewComponent
    private JmixButton outButton;

    @ViewComponent
    private JmixButton breakButton;

    @ViewComponent
    private JmixButton breakEndButton;

    @Subscribe
    public void onInit(InitEvent event) {
        refreshData();
    }

    // --- Button handlers: delegate rules to service -------------------------

    @Subscribe("inButton")
    public void onInButtonClick(ClickEvent<JmixButton> event) {
        handleAction(AttendanceAction.IN, "Checked IN");
    }

    @Subscribe("outButton")
    public void onOutButtonClick(ClickEvent<JmixButton> event) {
        handleAction(AttendanceAction.OUT, "Checked OUT");
    }

    @Subscribe("breakButton")
    public void onBreakButtonClick(ClickEvent<JmixButton> event) {
        handleAction(AttendanceAction.BREAK, "Break started");
    }

    @Subscribe("breakEndButton")
    public void onBreakEndButtonClick(ClickEvent<JmixButton> event) {
        handleAction(AttendanceAction.BREAK_END, "Break ended");
    }

    private void handleAction(AttendanceAction action, String successMessage) {
        try {
            attendanceService.markAction(action);
            notifications.show(successMessage);
            refreshData();
        } catch (IllegalStateException ex) {
            // message from service, e.g. "You must check OUT before checking IN again."
            notifications.show(ex.getMessage());
        }
    }

    // --- Load today's records into the table --------------------------------

    private void refreshData() {
        attendanceDc.setItems(attendanceService.getTodayRecords());
    }
}
