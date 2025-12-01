package com.ghazal.ims2.view.report;

import com.ghazal.ims2.entity.Project;
import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.service.report.ProjectCostReportResult;
import com.ghazal.ims2.service.report.ProjectCostReportService;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    private Label totalPointsLabel;

    @ViewComponent
    private Label pointsCostLabel;

    @ViewComponent
    private Label clientAmountLabel;

    @Autowired
    private ProjectCostReportService reportService;

    @Subscribe("generateBtn")
    public void onGenerateBtnClick(ClickEvent<Button> event) {
        Project project = projectField.getValue();
        LocalDate from = fromDateField.getValue();
        LocalDate to = toDateField.getValue();
        Warehouse warehouse = warehouseField.getValue();
        Double pointValDouble = pointValueField.getValue();

        if (project == null || from == null || to == null || pointValDouble == null) {
            Notification.show("Please fill project, dates, and point value",
                    3000, Notification.Position.MIDDLE);
            return;
        }

        BigDecimal pointValue = BigDecimal.valueOf(pointValDouble);

        ProjectCostReportResult result = reportService.calculateProjectCost(
                project, from, to, warehouse, pointValue
        );

        totalPointsLabel.setText("Total points: " + result.getTotalPoints());
        pointsCostLabel.setText("Points cost: " + result.getPointsCost());
        clientAmountLabel.setText("Client amount: " + result.getClientAmount());
    }
}
