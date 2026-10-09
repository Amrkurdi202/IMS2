package com.ghazal.ims2.view.item;

import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.entity.ItemRepository;
import com.ghazal.ims2.view.AutoSaveDetailView;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.image.JmixImage;
import io.jmix.flowui.component.upload.FileUploadField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.upload.event.FileUploadSucceededEvent;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.ByteArrayInputStream;
import java.security.SecureRandom;
import java.util.regex.Pattern;

@Route(value = "items/:id", layout = MainView.class)
@ViewController(id = "Item.detail")
@ViewDescriptor(path = "item-detail-view.xml")
@EditedEntityContainer("itemDc")
public class ItemDetailView extends AutoSaveDetailView<Item> {

    private static final Pattern BARCODE_FORMAT = Pattern.compile("^[0-9A-Za-z\\-]{1,100}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    @ViewComponent
    private JmixImage<Object> image;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private Notifications notifications;

    @Autowired
    private MessageBundle messageBundle;

    @Subscribe("imageField")
    public void onImageFieldFileUploadSucceeded(final FileUploadSucceededEvent<FileUploadField> event) {
        image.setSrc(
                new StreamResource(
                        event.getFileName(),
                        () -> new ByteArrayInputStream(event.getSource().getValue())
                )
        );
    }

    @Subscribe
    public void onBeforeSave(BeforeSaveEvent event) {
        String error = ensureBarcode();
        if (error != null) {
            notifications.create(messageBundle.getMessage("item.barcodeInvalid")
                            .replace("{0}", error))
                    .withType(Notifications.Type.ERROR)
                    .show();
            event.preventSave();
        }
    }

    @Subscribe(id = "saveAndCloseButton", subject = "clickListener")
    public void onSaveAndCloseButtonClick(final ClickEvent<JmixButton> event) {
        String error = ensureBarcode();
        if (error != null) {
            notifications.create(messageBundle.getMessage("item.barcodeInvalid")
                            .replace("{0}", error))
                    .withType(Notifications.Type.ERROR)
                    .show();
            return;
        }
        itemRepository.save(getEditedEntity());
    }

    /**
     * @return null when OK, otherwise a reason string
     */
    private String ensureBarcode() {
        Item item = getEditedEntity();
        String barcode = item.getBarcode() == null ? "" : item.getBarcode().trim();

        if (barcode.isEmpty()) {
            item.setBarcode(generateUniqueBarcode());
            return null;
        }

        if (!BARCODE_FORMAT.matcher(barcode).matches()) {
            return "format";
        }
        item.setBarcode(barcode);

        Long id = item.getId();
        boolean taken = false;
        for (Item other : itemRepository.findAll()) {
            if (barcode.equals(other.getBarcode()) && (id == null || !id.equals(other.getId()))) {
                taken = true;
                break;
            }
        }
        if (taken) {
            return "duplicate";
        }
        return null;
    }

    private String generateUniqueBarcode() {
        for (int attempt = 0; attempt < 50; attempt++) {
            StringBuilder sb = new StringBuilder("2");
            for (int i = 0; i < 12; i++) {
                sb.append(RANDOM.nextInt(10));
            }
            String candidate = sb.toString();
            boolean exists = false;
            for (Item other : itemRepository.findAll()) {
                if (candidate.equals(other.getBarcode())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                return candidate;
            }
        }
        return null;
    }
}