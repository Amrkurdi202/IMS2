package com.ghazal.ims2.view.report;

import com.ghazal.ims2.entity.Project;
import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.service.report.ProjectCostReportResult;
import com.ghazal.ims2.service.report.ProjectCostReportService;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.kit.component.ComponentUtils;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

@Route(value = "project-cost-report", layout = MainView.class)
@ViewController("ProjectCostReportView")
@ViewDescriptor("project-cost-report-view.xml")
public class ProjectCostReportView extends StandardView {

    @ViewComponent
    private DatePicker fromDateField;

    @ViewComponent
    private DatePicker toDateField;

    @ViewComponent
    private EntityComboBox<Project> projectField;

    @ViewComponent
    private EntityComboBox<Warehouse> warehouseField;

    @ViewComponent
    private NumberField pointValueField;

    @ViewComponent
    private Span totalPointsLabel;

    @ViewComponent
    private Span pointsCostLabel;

    @ViewComponent
    private Span clientAmountLabel;

    @Autowired
    private ProjectCostReportService reportService;

    @Autowired
    private MessageBundle messageBundle;

    @Subscribe("generateBtn")
    public void onGenerateBtnClick(ClickEvent<Button> event) {
        Project project = projectField.getValue();
        LocalDate from = fromDateField.getValue();
        LocalDate to = toDateField.getValue();
        Warehouse warehouse = warehouseField.getValue();
        Double pointValDouble = pointValueField.getValue();

        if (project == null || from == null || to == null || pointValDouble == null) {
            Notification.show(messageBundle.getMessage("ProjectCostReportView.fillRequired"),
                    3000, Notification.Position.MIDDLE);
            return;
        }

        BigDecimal pointValue = BigDecimal.valueOf(pointValDouble);

        OffsetDateTime offsetFrom = from.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime offsetTo = to.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        ProjectCostReportResult result = reportService.calculateProjectCost(
                project, offsetFrom, offsetTo, warehouse, pointValue
        );

        totalPointsLabel.setText(messageBundle.getMessage("ProjectCostReportView.totalPoints") + ": " + result.getTotalPoints());
        pointsCostLabel.setText(messageBundle.getMessage("ProjectCostReportView.pointsCost") + ": " + result.getPointsCost());
        clientAmountLabel.setText(messageBundle.getMessage("ProjectCostReportView.clientAmount") + ": " + result.getClientAmount());
    }
}
