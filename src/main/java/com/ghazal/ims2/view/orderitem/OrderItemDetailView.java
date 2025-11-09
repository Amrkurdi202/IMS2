package com.ghazal.ims2.view.orderitem;

import com.ghazal.ims2.entity.OrderItem;
import com.ghazal.ims2.entity.OrderItemRepository;
import com.ghazal.ims2.view.AutoSaveDetailView;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.EditedEntityContainer;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.Socket;


@Route(value = "OrderItem/:id", layout = MainView.class)
@ViewController(id = "ORDER_ITEM.detail")
@ViewDescriptor(path = "/com/ghazal/ims2/view/orderitem/order-item-detail-view.xml")
@EditedEntityContainer("OrderItemDc")
public class OrderItemDetailView extends AutoSaveDetailView<OrderItem> {
    
    @Autowired
    private OrderItemRepository projectPointRepository;


    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    public void onSaveAndCloseButtonClick(final ClickEvent<JmixButton> event) {
        projectPointRepository.save(getEditedEntity());
    }


}
