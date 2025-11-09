package com.ghazal.ims2.view.item;

import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.entity.ItemRepository;
import com.ghazal.ims2.view.AutoSaveDetailView;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.ByteArrayInputStream;

@Route(value = "items/:id", layout = MainView.class)
@ViewController(id = "Item.detail")
@ViewDescriptor(path = "item-detail-view.xml")
@EditedEntityContainer("itemDc")
public class ItemDetailView extends AutoSaveDetailView<Item> {
    @ViewComponent
    private JmixImage<Object> image;
    @Autowired
    private ItemRepository itemRepository;

    @Subscribe("imageField")
    public void onImageFieldFileUploadSucceeded(final FileUploadSucceededEvent<FileUploadField> event) {
        image.setSrc(
                new StreamResource(
                        event.getFileName(),
                        () -> new ByteArrayInputStream(event.getSource().getValue())
                )
        );
    }

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    public void onSaveAndCloseButtonClick(final ClickEvent<JmixButton> event) {
        itemRepository.save(getEditedEntity());
    }


}
