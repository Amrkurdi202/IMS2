package com.ghazal.ims2.view.warehouse;

import com.ghazal.ims2.entity.Warehouse;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "warehouses/:id", layout = MainView.class)
@ViewController(id = "Warehouse.detail")
@ViewDescriptor(path = "warehouse-detail-view.xml")
@EditedEntityContainer("warehouseDc")
public class WarehouseDetailView extends StandardDetailView<Warehouse> {
}