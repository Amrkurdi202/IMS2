package com.ghazal.ims2.view.report;

import com.ghazal.ims2.entity.OrderItem;
import com.ghazal.ims2.entity.Project;
import com.ghazal.ims2.entity.Unit;
import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.service.report.ProjectCostReportPdfService;
import com.ghazal.ims2.service.report.ProjectCostReportResult;
import com.ghazal.ims2.service.report.ProjectCostReportService;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.MessageBundle;
import io.jmix.flowui.view.StandardView;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

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

    @ViewComponent
    private DataGrid<OrderItem> itemsGrid;

    @ViewComponent
    private CollectionContainer<OrderItem> orderItemsDc;

    @ViewComponent
    private Button exportPdfBtn;

    @Autowired
    private ProjectCostReportService reportService;

    @Autowired
    private ProjectCostReportPdfService pdfService;

    @Autowired
    private MessageBundle messageBundle;

    private ProjectCostReportResult lastResult;
    private Project lastProject;
    private OffsetDateTime lastFrom;
    private OffsetDateTime lastTo;
    private Warehouse lastWarehouse;
    private BigDecimal lastPointValue;

    private StreamRegistration pdfRegistration;

    @Subscribe
    public void onInit(InitEvent event) {
        itemsGrid.removeAllColumns();

        itemsGrid.addColumn(this::getDisplayName)
                .setHeader(messageBundle.getMessage("ProjectCostReportView.itemName"))
                .setAutoWidth(true);

        itemsGrid.addColumn(this::getUnitName)
                .setHeader(messageBundle.getMessage("ProjectCostReportView.unit"))
                .setAutoWidth(true);

        itemsGrid.addColumn(OrderItem::getQuantity)
                .setHeader(messageBundle.getMessage("ProjectCostReportView.quantity"))
                .setAutoWidth(true);

        itemsGrid.addColumn(OrderItem::getUnitPriceAtOrder)
                .setHeader(messageBundle.getMessage("ProjectCostReportView.price"))
                .setAutoWidth(true);

        exportPdfBtn.setEnabled(false);
        orderItemsDc.setItems(Collections.emptyList());
    }

    @Subscribe
    public void onDetach(DetachEvent event) {
        if (pdfRegistration != null) {
            pdfRegistration.unregister();
            pdfRegistration = null;
        }
    }

    @Subscribe("generateBtn")
    public void onGenerateBtnClick(ClickEvent<Button> event) {
        Project project = projectField.getValue();
        LocalDate from = fromDateField.getValue();
        LocalDate to = toDateField.getValue();
        Warehouse warehouse = warehouseField.getValue();
        Double pointValDouble = pointValueField.getValue();

        if (project == null || from == null || to == null || pointValDouble == null) {
            Notification.show(messageBundle.getMessage("ProjectCostReportView.fillRequired"), 3000, Notification.Position.MIDDLE);
            exportPdfBtn.setEnabled(false);
            orderItemsDc.setItems(Collections.emptyList());
            lastResult = null;
            return;
        }

        BigDecimal pointValue = BigDecimal.valueOf(pointValDouble);

        OffsetDateTime offsetFrom = from.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        OffsetDateTime offsetTo = to.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();

        ProjectCostReportResult result = reportService.calculateProjectCost(project, offsetFrom, offsetTo, warehouse, pointValue);

        BigDecimal totalPoints = result.totalPoints() != null ? result.totalPoints() : BigDecimal.ZERO;
        BigDecimal pointsCost = result.pointsCost() != null ? result.pointsCost() : BigDecimal.ZERO;
        BigDecimal clientAmount = result.clientAmount() != null ? result.clientAmount() : BigDecimal.ZERO;

        totalPointsLabel.setText(messageBundle.getMessage("ProjectCostReportView.totalPoints") + ": " + totalPoints);
        pointsCostLabel.setText(messageBundle.getMessage("ProjectCostReportView.pointsCost") + ": " + pointsCost);
        clientAmountLabel.setText(messageBundle.getMessage("ProjectCostReportView.clientAmount") + ": " + clientAmount);

        List<OrderItem> items = result.orderItems() != null ? result.orderItems() : Collections.emptyList();
        orderItemsDc.setItems(items);

        lastResult = result;
        lastProject = project;
        lastFrom = offsetFrom;
        lastTo = offsetTo;
        lastWarehouse = warehouse;
        lastPointValue = pointValue;

        exportPdfBtn.setEnabled(!items.isEmpty());
    }

    @Subscribe("exportPdfBtn")
    public void onExportPdfBtnClick(ClickEvent<Button> event) {
        if (lastResult == null || lastProject == null || lastFrom == null || lastTo == null || lastPointValue == null) {
            Notification.show(messageBundle.getMessage("ProjectCostReportView.generateFirst"), 3000, Notification.Position.MIDDLE);
            return;
        }

        List<OrderItem> items = lastResult.orderItems() != null ? lastResult.orderItems() : Collections.emptyList();

        try {
            byte[] pdf = pdfService.generateProjectCostPdf(
                    lastProject,
                    lastFrom,
                    lastTo,
                    lastWarehouse,
                    lastPointValue,
                    lastResult,
                    items
            );

            StreamResource resource = new StreamResource("project-cost-report.pdf", () -> new ByteArrayInputStream(pdf));

            if (pdfRegistration != null) {
                pdfRegistration.unregister();
                pdfRegistration = null;
            }

            pdfRegistration = VaadinSession.getCurrent().getResourceRegistry().registerResource(resource);

            String url = pdfRegistration.getResourceUri().toString();
            UI.getCurrent().getPage().open(url, "_blank");
        } catch (Exception e) {
            Notification.show(messageBundle.getMessage("ProjectCostReportView.exportFailed"), 4000, Notification.Position.MIDDLE);
        }
    }

    private String getDisplayName(OrderItem oi) {
        if (oi == null) {
            return "";
        }
        String customName = oi.getCustomName();
        if (customName != null && !customName.isBlank()) {
            return customName;
        }
        if (oi.getItem() != null) {
            return oi.getItem().getName();
        }
        return "";
    }

    private String getUnitName(OrderItem oi) {
        if (oi == null) {
            return "";
        }
        Unit unit = oi.getUnit();
        if (unit == null) {
            return "";
        }
        return unit.getName();
    }
}
