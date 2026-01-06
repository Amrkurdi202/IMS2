package com.ghazal.ims2.view.attendance.worker;

import com.ghazal.ims2.entity.AttendanceAction;
import com.ghazal.ims2.entity.AttendanceRecord;
import com.ghazal.ims2.entity.Project;
import com.ghazal.ims2.service.AttendanceService;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.core.Messages;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@ViewController("AttendanceView")
@ViewDescriptor("attendance-view.xml")
@Route(value = "attendance", layout = MainView.class)
public class AttendanceView extends StandardView {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private Notifications notifications;

    @Autowired
    private Messages messages;

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

    @ViewComponent
    private EntityComboBox<Project> projectField;

    @Subscribe
    public void onInit(InitEvent event) {
        refreshData();
        updateProjectFieldState();

        // Add listener to enable/disable IN button based on project selection
        projectField.addValueChangeListener(e -> updateInButtonState());
    }

    // --- Button handlers: delegate rules to service -------------------------

    @Subscribe("inButton")
    public void onInButtonClick(ClickEvent<JmixButton> event) {
        Project selectedProject = projectField.getValue();
        handleAction(AttendanceAction.IN,
                messages.getMessage("com.ghazal.ims2.view.attendance/checkedIn"),
                selectedProject);
    }

    @Subscribe("outButton")
    public void onOutButtonClick(ClickEvent<JmixButton> event) {
        // Project will be auto-filled by service from the IN record
        handleAction(AttendanceAction.OUT,
                messages.getMessage("com.ghazal.ims2.view.attendance/checkedOut"),
                null);
    }

//    @Subscribe("breakButton")
//    public void onBreakButtonClick(ClickEvent<JmixButton> event) {
//        handleAction(AttendanceAction.BREAK, "Break started");
//    }
//
//    @Subscribe("breakEndButton")
//    public void onBreakEndButtonClick(ClickEvent<JmixButton> event) {
//        handleAction(AttendanceAction.BREAK_END, "Break ended");
//    }

    private void handleAction(AttendanceAction action, String successMessage, Project selectedProject) {
        try {
            attendanceService.markAction(action, selectedProject);
            notifications.show(successMessage);
            refreshData();
            updateProjectFieldState();
        } catch (IllegalStateException ex) {
            // message from service, e.g. "You must check OUT before checking IN again."
            notifications.show(ex.getMessage());
        }
    }

    /**
     * Updates project field state based on current attendance status:
     * - After IN: disable and keep selected project
     * - After OUT: enable and clear project (ready for next check-in)
     */
    private void updateProjectFieldState() {
        List<AttendanceRecord> records = attendanceService.getTodayRecords();

        long inCount = records.stream()
                .filter(r -> r.getAction() == AttendanceAction.IN)
                .count();
        long outCount = records.stream()
                .filter(r -> r.getAction() == AttendanceAction.OUT)
                .count();

        boolean isCheckedIn = (inCount != outCount);

        if (isCheckedIn) {
            // User is IN: disable project field (keep current value)
            projectField.setEnabled(false);
        } else {
            // User is OUT: enable project field and clear it for next check-in
            projectField.setEnabled(true);
            projectField.clear();
        }

        // Update IN button state
        updateInButtonState();
    }

    /**
     * Enables IN button only when project is selected and user is checked out
     */
    private void updateInButtonState() {
        List<AttendanceRecord> records = attendanceService.getTodayRecords();

        long inCount = records.stream()
                .filter(r -> r.getAction() == AttendanceAction.IN)
                .count();
        long outCount = records.stream()
                .filter(r -> r.getAction() == AttendanceAction.OUT)
                .count();

        boolean isCheckedOut = (inCount == outCount);
        boolean hasProjectSelected = projectField.getValue() != null;

        // Enable IN button only if user is checked out AND has selected a project
        inButton.setEnabled(isCheckedOut && hasProjectSelected);
    }

    // --- Load today's records into the table --------------------------------

    private void refreshData() {
        attendanceDc.setItems(attendanceService.getTodayRecords());
    }
}
