package com.ghazal.ims2.view.report;

import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.KeyValueCollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

@Route(value = "attendance-days-report", layout = MainView.class)
@ViewController("AttendanceDaysReport")
@ViewDescriptor("attendance-days-report-view.xml")
public class AttendanceDaysReportView extends StandardView {

    @ViewComponent
    private TypedDatePicker<LocalDate> fromDateField;

    @ViewComponent
    private TypedDatePicker<LocalDate> toDateField;


    @ViewComponent
    private KeyValueCollectionLoader reportDl;

    @Autowired
    private Notifications notifications;

    @Subscribe("generateBtn")
    public void onGenerateBtnClick(ClickEvent<JmixButton> event) {
        LocalDate from = fromDateField.getValue();
        LocalDate to = toDateField.getValue();

        if (from == null || to == null) {
            notifications.show("Please select both From date and To date");
            return;
        }

        if (to.isBefore(from)) {
            notifications.show("To date cannot be before From date");
            return;
        }

        reportDl.setParameter("fromDate", from);
        reportDl.setParameter("toDate", to);
        reportDl.load();
    }
}
