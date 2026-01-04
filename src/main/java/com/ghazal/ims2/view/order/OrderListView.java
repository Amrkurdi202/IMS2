package com.ghazal.ims2.view.order;

import com.ghazal.ims2.entity.Order;
import com.ghazal.ims2.service.OrderReportService;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Route(value = "orders", layout = MainView.class)
@ViewController(id = "ims_order.list")
@ViewDescriptor(path = "order-list-view.xml")
@LookupComponent("ordersDataGrid")
@DialogMode(width = "64em")
public class OrderListView extends StandardListView<Order> {

    @Autowired
    private OrderReportService orderReportService;

    @Autowired
    private Notifications notifications;

    @Autowired
    private Downloader downloader;

    @ViewComponent
    private DataGrid<Order> ordersDataGrid;

    @Subscribe("exportPdfBtn")
    public void onExportPdfBtnClick(ClickEvent<JmixButton> event) {
        Order selectedOrder = ordersDataGrid.getSingleSelectedItem();

        if (selectedOrder == null) {
            notifications.create("Please select an order to export")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        try {
            // Generate PDF
            byte[] pdfBytes = orderReportService.generateOrderPdf(selectedOrder);

            // Create filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("Order_%s_%s.pdf", selectedOrder.getId(), timestamp);

            // Download PDF using Vaadin downloader
            downloader.download(pdfBytes, filename);

            notifications.create("PDF exported successfully")
                    .withType(Notifications.Type.SUCCESS)
                    .show();

        } catch (Exception e) {
            notifications.create("Error generating PDF: " + e.getMessage())
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }
}