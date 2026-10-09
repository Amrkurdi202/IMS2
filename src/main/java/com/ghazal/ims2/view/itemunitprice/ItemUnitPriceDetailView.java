package com.ghazal.ims2.view.itemunitprice;

import com.ghazal.ims2.entity.Item;
import com.ghazal.ims2.entity.ItemUnitPrice;
import com.ghazal.ims2.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.view.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.security.SecureRandom;
import java.util.regex.Pattern;

@Route(value = "itemUnitPrices/:id", layout = MainView.class)
@ViewController("ItemUnitPrice.detail")
@ViewDescriptor("item-unit-price-detail-view.xml")
@EditedEntityContainer("itemUnitPriceDc")
public class ItemUnitPriceDetailView extends StandardDetailView<ItemUnitPrice> {

    private static final Logger log = LoggerFactory.getLogger(ItemUnitPriceDetailView.class);
    private static final Pattern BARCODE_FORMAT = Pattern.compile("^[0-9A-Za-z\\-]{1,100}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private DataManager dataManager;

    @Autowired
    private Notifications notifications;

    @Autowired
    private MessageBundle messageBundle;

    @Subscribe
    public void onBeforeSave(BeforeSaveEvent event) {
        ItemUnitPrice entity = getEditedEntity();

        String error = ensureBarcode();
        if (error != null) {
            notifications.create(messageBundle.getMessage("itemUnitPrice.barcodeInvalid")
                            .replace("{0}", error))
                    .withType(Notifications.Type.ERROR)
                    .show();
            event.preventSave();
            return;
        }

        log.info("=== BEFORE SAVE ===");
        log.info("ID: {}", entity.getId());
        log.info("Item: {}", entity.getItem() != null ? entity.getItem().getName() : "null");
        log.info("Unit: {}", entity.getUnit() != null ? entity.getUnit().getName() : "null");
        log.info("Price: {}", entity.getPrice());
        log.info("Warehouse: {}", entity.getWarehouse() != null ? entity.getWarehouse().getName() : "null");

        // Force save using DataManager to ensure changes are persisted
        try {
            ItemUnitPrice saved = dataManager.save(entity);
            log.info("Explicitly saved with DataManager - new price: {}", saved.getPrice());

            // Clear the data context to mark as no unsaved changes
            getViewData().getDataContext().clear();

            // Prevent default save since we already saved
            event.preventSave();

            // Manually close the view after successful save
            close(StandardOutcome.DISCARD);
        } catch (Exception e) {
            log.error("Error during explicit save", e);
            throw e;
        }
    }

    /**
     * @return null when OK, otherwise a reason key fragment
     */
    private String ensureBarcode() {
        ItemUnitPrice entity = getEditedEntity();
        String barcode = entity.getBarcode() == null ? "" : entity.getBarcode().trim();

        if (barcode.isEmpty()) {
            entity.setBarcode(generateUniqueBarcode());
            return null;
        }
        if (!BARCODE_FORMAT.matcher(barcode).matches()) {
            return "format";
        }
        entity.setBarcode(barcode);

        Long id = entity.getId();
        for (ItemUnitPrice other : dataManager.load(ItemUnitPrice.class)
                .query("select p from ItemUnitPrice p").list()) {
            if (barcode.equals(other.getBarcode())
                    && (id == null || !id.equals(other.getId()))) {
                return "duplicate";
            }
        }
        for (Item other : dataManager.load(Item.class).query("select i from Item i").list()) {
            if (barcode.equals(other.getBarcode())) {
                return "duplicate";
            }
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
            boolean iupTaken = dataManager.load(ItemUnitPrice.class)
                    .query("select p from ItemUnitPrice p where p.barcode = :b")
                    .parameter("b", candidate).list().size() > 0;
            boolean itemTaken = dataManager.load(Item.class)
                    .query("select i from Item i where i.barcode = :b")
                    .parameter("b", candidate).list().size() > 0;
            if (!iupTaken && !itemTaken) {
                return candidate;
            }
        }
        return null;
    }

    @Subscribe
    public void onAfterSave(AfterSaveEvent event) {
        log.info("=== AFTER SAVE - Success ===");
        notifications.create("Saved successfully!").show();
    }

    @Subscribe
    public void onValidation(ValidationEvent event) {
        ValidationErrors errors = event.getErrors();
        if (!errors.isEmpty()) {
            log.error("=== VALIDATION ERRORS ===");
            log.error("{}", errors);
        }
    }
}