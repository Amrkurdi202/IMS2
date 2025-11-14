package com.ghazal.ims2.view.attendance;

import com.ghazal.ims2.entity.AttendanceAction;
import com.ghazal.ims2.entity.AttendanceRecord;
import com.ghazal.ims2.service.AttendanceService;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@ViewController("ims2_AttendanceView")
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
    private CollectionLoader<AttendanceRecord> attendanceDl;

    @Subscribe("inButton")
    public void onInButtonClick(ClickEvent<JmixButton> event) {
        attendanceService.markAction(AttendanceAction.IN);
        refreshData();
        notifications.show("Checked IN");
    }

    @Subscribe("outButton")
    public void onOutButtonClick(ClickEvent<JmixButton> event) {
        attendanceService.markAction(AttendanceAction.OUT);
        refreshData();
        notifications.show("Checked OUT");
    }

    @Subscribe("breakButton")
    public void onBreakButtonClick(ClickEvent<JmixButton> event) {
        attendanceService.markAction(AttendanceAction.BREAK);
        refreshData();
        notifications.show("Break started");
    }

    @Subscribe("breakEndButton")
    public void onBreakEndButtonClick(ClickEvent<JmixButton> event) {
        attendanceService.markAction(AttendanceAction.BREAK_END);
        refreshData();
        notifications.show("Break ended");
    }

    private void refreshData() {
        attendanceDl.load();
    }
}
