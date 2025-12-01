package com.ghazal.ims2.view.attendance.attendancerecord;

import com.ghazal.ims2.entity.AttendanceAction;
import com.ghazal.ims2.entity.AttendanceDay;
import com.ghazal.ims2.entity.AttendanceRecord;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.grid.editor.EditorSaveEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Route(value = "attendance-records", layout = MainView.class)
@ViewController("AttendanceRecord.list")
@ViewDescriptor("attendance-record-list-view.xml")
@LookupComponent("attendanceRecordsDataGrid")
public class AttendanceRecordListView extends StandardListView<AttendanceRecord>
        implements BeforeEnterObserver {

    @Autowired
    private DataManager dataManager;

    @Autowired
    private Notifications notifications;

    @Autowired
    private Dialogs dialogs;

    @ViewComponent
    private DataGrid<AttendanceRecord> attendanceRecordsDataGrid;

    @ViewComponent
    private CollectionLoader<AttendanceRecord> attendanceRecordsDl;

    @ViewComponent
    private CollectionContainer<AttendanceRecord> attendanceRecordsDc;

    private AttendanceDay currentDay;

    // used by the leave-guard
    private boolean hasUnsavedChanges = false;

    // -------------------------------------------------
    // Load records for a specific AttendanceDay (dayId)
    // -------------------------------------------------
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        hasUnsavedChanges = false;   // fresh entry

        List<String> values = event.getLocation()
                .getQueryParameters()
                .getParameters()
                .getOrDefault("dayId", Collections.emptyList());

        if (values.isEmpty()) {
            return;
        }

        try {
            UUID dayId = UUID.fromString(values.get(0));

            attendanceRecordsDl.setParameter("dayId", dayId);
            attendanceRecordsDl.load();

            currentDay = dataManager.load(AttendanceDay.class)
                    .id(dayId)
                    .one();
        } catch (IllegalArgumentException ex) {
            // invalid UUID → ignore
        }
    }

    // -------------------------------------------------
    // 1) CREATE inline: mark unsaved
    // -------------------------------------------------
    @Subscribe("createButton")
    public void onCreateButtonClick(ClickEvent<JmixButton> event) {
        if (currentDay == null) {
            notifications.show("No attendance day selected.");
            return;
        }

        AttendanceRecord record = dataManager.create(AttendanceRecord.class);
        record.setDay(currentDay);

        // optional defaults
        record.setAction(AttendanceAction.IN);
        record.setTime(LocalTime.now());

        attendanceRecordsDc.getMutableItems().add(record);

        // start editing this new row inline
        attendanceRecordsDataGrid.getEditor().editItem(record);

        hasUnsavedChanges = true;
    }

    // -------------------------------------------------
    // 2) EDIT inline: closing editor marks unsaved
    // -------------------------------------------------
    @Install(to = "attendanceRecordsDataGrid.@editor", subject = "saveListener")
    private void attendanceRecordsEditorSave(EditorSaveEvent<AttendanceRecord> event) {
        // user edited a row and closed the editor → mark as dirty (not yet saved to DB)
        hasUnsavedChanges = true;
        // we do NOT save to DB here; only the global "Save changes" button does that
    }

    // -------------------------------------------------
    // 3) SAVE button: persist all & clear dirty flag
    // -------------------------------------------------
    @Subscribe("saveButton")
    public void onSaveButtonClick(ClickEvent<JmixButton> event) {
        saveChanges();
    }

    private void saveChanges() {
        if (attendanceRecordsDc.getItems().isEmpty()) {
            notifications.show("No records to save");
            return;
        }

        for (AttendanceRecord record : attendanceRecordsDc.getItems()) {
            dataManager.save(record);
        }

        attendanceRecordsDl.load();
        hasUnsavedChanges = false;
        notifications.show("Changes saved");
    }
}
