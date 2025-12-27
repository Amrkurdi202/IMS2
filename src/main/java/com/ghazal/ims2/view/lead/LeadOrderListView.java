package com.ghazal.ims2.view.lead;

import com.ghazal.ims2.entity.Order;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "lead-orders", layout = MainView.class)
@ViewController("LeadOrder.list")
@ViewDescriptor("lead-order-list-view.xml")
@LookupComponent("ordersTable")
@DialogMode(width = "64em")
public class LeadOrderListView extends StandardListView<Order> {

    @Autowired
    private CurrentAuthentication currentAuthentication;

    @ViewComponent
    private CollectionLoader<Order> ordersDl;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        String username = currentAuthentication.getUser().getUsername();
        ordersDl.setParameter("currentUser", username);
        ordersDl.load();
    }
}
