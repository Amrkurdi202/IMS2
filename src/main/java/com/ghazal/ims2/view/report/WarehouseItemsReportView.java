package com.ghazal.ims2.view.report;

import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.KeyValueCollectionLoader;
import io.jmix.flowui.view.*;

import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "warehouse-items-report", layout = MainView.class)
@ViewController("WarehouseItemsReportView")
@ViewDescriptor("warehouse-items-report-view.xml")
public class WarehouseItemsReportView extends StandardView {

    @ViewComponent
    private CollectionLoader<Warehouse> warehousesDl;

    @ViewComponent
    private EntityComboBox<Warehouse> warehouseField;

    @ViewComponent
    private TypedTextField<String> qField;

    @ViewComponent
    private KeyValueCollectionLoader reportDl;

    @Autowired
    private Notifications notifications;
    @Autowired
    private MessageBundle messageBundle;

    @Subscribe
    public void onInit(final InitEvent event) {
        warehousesDl.load();
    }

    @Subscribe("searchBtn")
    public void onSearchBtnClick(ClickEvent<Button> event) {
        loadReport();
    }

    @Subscribe("resetBtn")
    public void onResetBtnClick(ClickEvent<Button> event) {
        qField.clear();
        loadReport();
    }

    @Subscribe("warehouseField")
    public void onWarehouseFieldValueChange(
            com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent<
                    EntityComboBox<Warehouse>, Warehouse> event) {
        if (event.getValue() != null) {
            loadReport();
        }
    }

    private void loadReport() {
        Warehouse wh = warehouseField.getValue();
        if (wh == null) {
            notifications.create(messageBundle.getMessage("WarehouseItemsReportView.selectWarehouse"))
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }

        String q = qField.getValue();
        if (q == null) q = "";
        q = q.trim();

        reportDl.setParameter("warehouse", wh);
        reportDl.setParameter("q", q);
        reportDl.load();
    }
}
