package com.ghazal.ims2.view.warehouse;

import com.ghazal.ims2.entity.*;
import com.ghazal.ims2.service.report.WarehouseStockPdfService;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.entity.KeyValueEntity;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.download.Downloader;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.KeyValueCollectionContainer;
import io.jmix.flowui.model.KeyValueCollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "admin/warehouse-stock", layout = MainView.class)
@ViewController("WarehouseStockView")
@ViewDescriptor("warehouse-stock-view.xml")
public class WarehouseStockView extends StandardView {

    @Autowired private DataManager dataManager;
    @Autowired private Notifications notifications;
    @Autowired private MessageBundle messageBundle;
    @Autowired private CurrentAuthentication currentAuthentication;
    @Autowired private WarehouseStockPdfService pdfService;
    @Autowired private Downloader downloader;

    @ViewComponent private CollectionLoader<Warehouse> warehousesDl;
    @ViewComponent private EntityComboBox<Warehouse> warehouseField;
    @ViewComponent private TypedTextField<String> qField;
    @ViewComponent private KeyValueCollectionLoader reportDl;
    @ViewComponent private KeyValueCollectionContainer reportDc;

    @Subscribe
    public void onInit(InitEvent event) {
        warehousesDl.load();
        qField.addValueChangeListener(e -> loadReport());
    }

    @Subscribe("warehouseField")
    public void onWarehouseFieldValueChange(
            com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent<
                    EntityComboBox<Warehouse>, Warehouse> event) {
        loadReport();
    }

    @Subscribe("exportPdfBtn")
    public void onExportPdfBtnClick(ClickEvent<Button> event) {
        Warehouse warehouse = warehouseField.getValue();
        if (warehouse == null) {
            warn("WarehouseStockView.selectWarehouseFirst");
            return;
        }
        if (reportDc.getItems().isEmpty()) {
            warn("WarehouseStockView.noDataToExport");
            return;
        }
        try {
            byte[] pdf = pdfService.generatePdf(warehouse, buildRows());
            String filename = "warehouse-stock-" + warehouse.getName() + "-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            downloader.download(pdf, filename);
        } catch (Exception e) {
            notifications.create(messageBundle.getMessage("WarehouseStockView.exportFailed") + ": " + e.getMessage())
                    .withType(Notifications.Type.ERROR).show();
        }
    }

    @Subscribe("addStockBtn")
    public void onAddStockBtnClick(ClickEvent<Button> event) {
        Warehouse warehouse = warehouseField.getValue();
        if (warehouse == null) {
            warn("WarehouseStockView.selectWarehouseFirst");
            return;
        }
        openAddStockDialog(warehouse);
    }

    private void loadReport() {
        Warehouse wh = warehouseField.getValue();
        if (wh == null) return;
        String q = qField.getValue();
        reportDl.setParameter("warehouse", wh);
        reportDl.setParameter("q", q != null ? q.trim() : "");
        reportDl.load();
    }

    private List<WarehouseStockPdfService.StockRow> buildRows() {
        List<WarehouseStockPdfService.StockRow> rows = new ArrayList<>();
        for (KeyValueEntity kve : reportDc.getItems()) {
            String itemName = kve.getValue("itemName");
            String unitName = kve.getValue("unitName");
            BigDecimal stockQty = kve.getValue("stockQty");
            rows.add(new WarehouseStockPdfService.StockRow(
                    itemName != null ? itemName : "",
                    unitName != null ? unitName : "",
                    stockQty != null ? stockQty : BigDecimal.ZERO
            ));
        }
        return rows;
    }

    private void openAddStockDialog(Warehouse warehouse) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(messageBundle.getMessage("WarehouseStockView.addStockTitle")
                + " — " + warehouse.getName());
        dialog.setWidth("380px");

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        ComboBox<Item> itemCombo = new ComboBox<>(messageBundle.getMessage("WarehouseStockView.itemLabel"));
        itemCombo.setItems(dataManager.load(Item.class)
                .query("select i from Item i order by i.name").list());
        itemCombo.setItemLabelGenerator(Item::getName);
        itemCombo.setWidth("100%");

        ComboBox<Unit> unitCombo = new ComboBox<>(messageBundle.getMessage("WarehouseStockView.unitLabel"));
        unitCombo.setItems(dataManager.load(Unit.class)
                .query("select u from Unit u order by u.name").list());
        unitCombo.setItemLabelGenerator(Unit::getName);
        unitCombo.setWidth("100%");

        NumberField qtyField = new NumberField(messageBundle.getMessage("WarehouseStockView.qtyLabel"));
        qtyField.setMin(0.001);
        qtyField.setStep(1);
        qtyField.setWidth("100%");

        layout.add(itemCombo, unitCombo, qtyField);
        dialog.add(layout);

        Button cancelBtn = new Button(messageBundle.getMessage("WarehouseStockView.btnClose"),
                e -> dialog.close());

        Button saveBtn = new Button(messageBundle.getMessage("WarehouseStockView.btnSave"), e -> {
            if (itemCombo.getValue() == null || unitCombo.getValue() == null
                    || qtyField.getValue() == null || qtyField.getValue() <= 0) {
                warn("WarehouseStockView.fillAllFields");
                return;
            }
            saveStock(warehouse, itemCombo.getValue(), unitCombo.getValue(),
                    BigDecimal.valueOf(qtyField.getValue()));
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancelBtn, saveBtn);
        dialog.open();
    }

    private void saveStock(Warehouse warehouse, Item item, Unit unit, BigDecimal qty) {
        try {
            Order order = dataManager.create(Order.class);
            order.setWarehouse(warehouse);
            order.setUser((User) currentAuthentication.getUser());

            OrderItem orderItem = dataManager.create(OrderItem.class);
            orderItem.setItem(item);
            orderItem.setUnit(unit);
            orderItem.setQuantity(qty.negate());
            orderItem.setCustomItem(false);
            orderItem.setOrder(order);

            List<OrderItem> items = new ArrayList<>();
            items.add(orderItem);
            order.setItems(items);

            dataManager.save(new SaveContext().saving(order, orderItem));

            notifications.create(messageBundle.getMessage("WarehouseStockView.stockAdded"))
                    .withType(Notifications.Type.SUCCESS).show();
            loadReport();
        } catch (Exception e) {
            notifications.create(messageBundle.getMessage("WarehouseStockView.saveFailed")
                    + ": " + e.getMessage())
                    .withType(Notifications.Type.ERROR).show();
        }
    }

    private void warn(String msgKey) {
        notifications.create(messageBundle.getMessage(msgKey))
                .withType(Notifications.Type.WARNING).show();
    }
}
