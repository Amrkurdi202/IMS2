package com.ghazal.ims2.view.order;

import com.ghazal.ims2.entity.Order;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.view.*;

@Route(value = "orders/:id", layout = MainView.class)
@ViewController(id = "Order.detail")
@ViewDescriptor(path = "order-detail-view.xml")
@EditedEntityContainer("orderDc")
public class OrderDetailView extends StandardDetailView<Order> {
}